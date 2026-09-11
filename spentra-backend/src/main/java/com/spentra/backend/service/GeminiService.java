package com.spentra.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.ai.AiSummaryResponse;
import com.spentra.backend.model.dto.ai.TransactionDraftResponse;
import com.spentra.backend.model.entity.AiSummary;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.AiSummaryRepository;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private static final long MAX_RECEIPT_SIZE_BYTES = 2 * 1024 * 1024L;
    private static final Semaphore RECEIPT_PROCESSING_SLOT = new Semaphore(1);

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final AiSummaryRepository aiSummaryRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.model}")
    private String model;

    public TransactionDraftResponse parseText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini text parse requested but GEMINI_API_KEY is missing");
            throw new ApiRequestException("AI service temporarily unavailable.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        User currentUser = userService.getCurrentUser();
        List<Category> categories = categoryRepository.findByUserIdOrUserIsNull(currentUser.getId());
        log.info("Gemini text parse request started userId={} promptLength={} categoryCount={}",
                currentUser.getId(), prompt != null ? prompt.length() : 0, categories.size());
        String systemPrompt = buildSystemPrompt(categories);

        try {
            String body = """
                    {
                      "contents": [
                        {
                          "role": "user",
                          "parts": [{ "text": "%s" }]
                        }
                      ],
                      "systemInstruction": {
                        "parts": [{ "text": "%s" }]
                      },
                      "generationConfig": {
                        "responseMimeType": "application/json"
                      }
                    }
                    """.formatted(escapeJson(prompt), escapeJson(systemPrompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String url = "%s/%s:generateContent?key=%s".formatted(apiUrl, model, apiKey);
            String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            TransactionDraftResponse draft = parseDraft(response, categories);
            log.info("Gemini text parse success userId={} title={} amount={} categoryId={} confidence={}",
                    currentUser.getId(), draft.getTitle(), draft.getAmount(), draft.getCategoryId(), draft.getConfidence());
            return draft;
        } catch (ApiRequestException e) {
            log.warn("Gemini text parse failed userId={} status={} message={}",
                    currentUser.getId(), e.getStatus(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Gemini text parse error userId={} message={}, falling back to local rule-based parser",
                    currentUser.getId(), e.getMessage());
            return parseTextLocally(prompt, categories);
        }
    }

    public TransactionDraftResponse parseReceipt(MultipartFile file) {
        validateReceiptFile(file);

        if (!RECEIPT_PROCESSING_SLOT.tryAcquire()) {
            throw new ApiRequestException(
                    "Receipt processing is busy. Please try again shortly.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        try {
            return parseReceiptWithSlot(file);
        } finally {
            RECEIPT_PROCESSING_SLOT.release();
        }
    }

    private TransactionDraftResponse parseReceiptWithSlot(MultipartFile file) {

        User currentUser = userService.getCurrentUser();
        List<Category> categories = categoryRepository.findByUserIdOrUserIsNull(currentUser.getId());
        log.info("Gemini receipt parse request started userId={} fileName={} contentType={} sizeBytes={} categoryCount={}",
                currentUser.getId(), file.getOriginalFilename(), file.getContentType(), file.getSize(), categories.size());

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini receipt parse requested but GEMINI_API_KEY is missing, falling back to local receipt parser");
            return parseReceiptLocally(file, categories);
        }

        String systemPrompt = buildReceiptPrompt(categories);

        try {
            String mimeType = file.getContentType();
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String body = """
                    {
                      "contents": [
                        {
                          "role": "user",
                          "parts": [
                            {
                              "inlineData": {
                                "mimeType": "%s",
                                "data": "%s"
                              }
                            }
                          ]
                        }
                      ],
                      "systemInstruction": {
                        "parts": [{ "text": "%s" }]
                      },
                      "generationConfig": {
                        "responseMimeType": "application/json"
                      }
                    }
                    """.formatted(escapeJson(mimeType), base64, escapeJson(systemPrompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String url = "%s/%s:generateContent?key=%s".formatted(apiUrl, model, apiKey);
            String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            TransactionDraftResponse draft = parseDraft(response, categories);
            log.info("Gemini receipt parse success userId={} title={} amount={} categoryId={} confidence={}",
                    currentUser.getId(), draft.getTitle(), draft.getAmount(), draft.getCategoryId(), draft.getConfidence());
            return draft;
        } catch (ApiRequestException e) {
            log.warn("Gemini receipt parse failed userId={} status={} message={}",
                    currentUser.getId(), e.getStatus(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Gemini receipt parse error userId={} message={}, falling back to local receipt parser",
                    currentUser.getId(), e.getMessage());
            return parseReceiptLocally(file, categories);
        }
    }

    private TransactionDraftResponse parseReceiptLocally(MultipartFile file, List<Category> categories) {
        String filename = file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "Receipt";
        String cleanName = filename.replaceAll("(?i)\\.(jpg|jpeg|png|webp|heic|pdf)$", "")
                .replaceAll("[_-]+", " ")
                .trim();

        double amount = 0.0;
        java.util.regex.Pattern numPattern = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)");
        java.util.regex.Matcher nm = numPattern.matcher(cleanName);
        if (nm.find()) {
            try {
                amount = Double.parseDouble(nm.group(1));
            } catch (Exception ignored) {}
        }

        String lower = cleanName.toLowerCase(Locale.ROOT);
        Category matchedCategory = null;
        String matchedCatName = null;
        for (Category cat : categories) {
            if (cat.getName() != null && lower.contains(cat.getName().toLowerCase(Locale.ROOT))) {
                matchedCategory = cat;
                matchedCatName = cat.getName();
                break;
            }
        }
        if (matchedCategory == null) {
            if (lower.contains("food") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("cafe") || lower.contains("coffee") || lower.contains("restaurant")) {
                matchedCategory = resolveCategory("Food", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : "Food";
            } else if (lower.contains("grocery") || lower.contains("groceries") || lower.contains("mart") || lower.contains("market") || lower.contains("supermarket")) {
                matchedCategory = resolveCategory("Groceries", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : "Groceries";
            } else {
                matchedCategory = resolveCategory("Shopping", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : (categories.isEmpty() ? "General" : categories.get(0).getName());
            }
        }

        String title = cleanName.length() > 30 || cleanName.matches("^[0-9a-zA-Z]{12,}.*") || cleanName.isBlank()
                ? "Receipt Purchase"
                : (Character.toUpperCase(cleanName.charAt(0)) + cleanName.substring(1));

        return new TransactionDraftResponse(
                title,
                amount,
                TransactionType.EXPENSE,
                matchedCategory != null ? matchedCategory.getId() : null,
                matchedCatName,
                LocalDate.now(),
                amount > 0 ? "MEDIUM" : "LOW"
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public AiSummaryResponse getOrGenerateInsights(String yearMonth, String currencyCode) {
        UUID userId = userService.getCurrentUser().getId();
        YearMonth month = parseMonth(yearMonth);
        log.info("AI insights request started userId={} month={} currency={}", userId, month, currencyCode);
        return aiSummaryRepository.findFirstByUserIdAndYearMonthOrderByGeneratedAtDesc(userId, month)
                .map(summary -> {
                    log.info("AI insights cache hit userId={} month={} summaryId={}", userId, month, summary.getId());
                    return toResponse(summary);
                })
                .orElseGet(() -> {
                    log.info("AI insights cache miss userId={} month={}", userId, month);
                    return generateInsights(userId, month, currencyCode);
                });
    }

    @org.springframework.transaction.annotation.Transactional
    public AiSummaryResponse refreshInsights(String yearMonth, String currencyCode) {
        UUID userId = userService.getCurrentUser().getId();
        YearMonth month = parseMonth(yearMonth);
        log.info("AI insights refresh requested userId={} month={} currency={}", userId, month, currencyCode);
        return generateInsights(userId, month, currencyCode);
    }

    private String buildSystemPrompt(List<Category> categories) {
        String categoryList = categories.stream()
                .map(Category::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Uncategorized");

        return "You are a transaction parser for Spentra. Return only valid JSON with "
                + "title, amount, type, categoryName, transactionDate, confidence. "
                + "Category list: [" + categoryList + "]. "
                + "If the user says today or yesterday, map to the correct date.";
    }

    private String buildReceiptPrompt(List<Category> categories) {
        String categoryList = categories.stream()
                .map(Category::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("Uncategorized");

        return "You are a receipt parser for Spentra. Analyze the provided receipt image and return only valid JSON with "
                + "title, amount, type, categoryName, transactionDate, confidence. "
                + "Use the total amount, type must be EXPENSE, category list: [" + categoryList + "], "
                + "and if the date is unreadable use today's date.";
    }

    private AiSummaryResponse generateInsights(UUID userId, YearMonth month, String currencyCode) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<Expense> expenses = expenseRepository.findByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.EXPENSE, start, end);

        if (expenses.isEmpty()) {
            log.info("AI insights empty-state userId={} month={}", userId, month);
            return new AiSummaryResponse(null, month.toString(), "Not enough data yet. Add some transactions and check back!", 0.0, null, LocalDateTime.now());
        }

        double totalSpent = expenses.stream().mapToDouble(Expense::getAmount).sum();
        Map<String, Double> byCategory = expenses.stream().collect(
                Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().getName() : "Uncategorized",
                        Collectors.summingDouble(Expense::getAmount)));

        String topCategory = byCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Uncategorized");

        double topAmount = byCategory.getOrDefault(topCategory, 0.0);
        int topPct = totalSpent > 0 ? (int) Math.round((topAmount / totalSpent) * 100) : 0;
        String prompt = buildInsightsPrompt(month, currencyCode, totalSpent, byCategory, expenses.size());
        log.info("AI insights generation prepared userId={} month={} totalSpent={} topCategory={} txCount={}",
                userId, month, totalSpent, topCategory, expenses.size());
        String summary = requestGeminiSummary(prompt);
        if (summary == null || summary.isBlank()) {
            log.warn("AI insights Gemini summary unavailable userId={} month={} falling back to local summary", userId, month);
            summary = String.format(
                    "You spent %s%.2f this month. %s led your spending with %d%% of total spend. " +
                    "Spending was concentrated in a few categories. Try capping %s next month to save more.",
                    currencySymbol(currencyCode), totalSpent, topCategory, topPct, topCategory);
        }

        AiSummary summaryEntity = new AiSummary();
        summaryEntity.setUser(userService.getCurrentUser());
        summaryEntity.setYearMonth(month);
        summaryEntity.setSummaryText(summary);
        summaryEntity.setTotalSpent(totalSpent);
        summaryEntity.setTopCategory(topCategory);
        summaryEntity.setGeneratedAt(LocalDateTime.now());

        aiSummaryRepository.deleteByUserIdAndYearMonth(userId, month);
        AiSummary saved = aiSummaryRepository.save(summaryEntity);
        log.info("AI insights saved userId={} month={} summaryId={}", userId, month, saved.getId());
        return toResponse(saved);
    }

    private AiSummaryResponse toResponse(AiSummary summary) {
        return new AiSummaryResponse(
                summary.getId(),
                summary.getYearMonth().toString(),
                summary.getSummaryText(),
                summary.getTotalSpent(),
                summary.getTopCategory(),
                summary.getGeneratedAt()
        );
    }

    private String buildInsightsPrompt(YearMonth month, String currencyCode, double totalSpent,
            Map<String, Double> byCategory, int txCount) {
        String breakdown = byCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> String.format("%s: %s%.2f", entry.getKey(), currencySymbol(currencyCode), entry.getValue()))
                .collect(Collectors.joining(", "));

        return """
                You are a financial advisor AI for a personal finance app called Spentra.
                Analyze the following monthly spending data and provide exactly 4 concise sentences.
                User's spending for %s:
                - Total spent: %s%.2f
                - Breakdown by category: %s
                - Total transactions: %d
                Return ONLY 4 plain text sentences with no markdown, no bullets, no headers, no numbering.
                Sentence 1: Total spending summary for the month.
                Sentence 2: Top spending category and its share of total spend.
                Sentence 3: A notable trend or observation.
                Sentence 4: One specific, actionable savings tip based on the data.
                Keep the total response under 500 characters. Be specific to the numbers. No generic advice.
                """.formatted(month, currencySymbol(currencyCode), totalSpent, breakdown, txCount);
    }

    private String requestGeminiSummary(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI insights Gemini request skipped because GEMINI_API_KEY is missing");
            return null;
        }

        try {
            String body = """
                    {
                      "contents": [
                        {
                          "role": "user",
                          "parts": [{ "text": "%s" }]
                        }
                      ],
                      "generationConfig": {
                        "temperature": 0.4,
                        "maxOutputTokens": 256
                      }
                    }
                    """.formatted(escapeJson(prompt));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String url = "%s/%s:generateContent?key=%s".formatted(apiUrl, model, apiKey);
            String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            return parseGeminiText(response);
        } catch (Exception e) {
            log.error("AI insights Gemini request failed message={}", e.getMessage(), e);
            return null;
        }
    }

    private YearMonth parseMonth(String yearMonth) {
        return (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
    }

    private String currencySymbol(String currencyCode) {
        if ("USD".equalsIgnoreCase(currencyCode)) return "$";
        if ("EUR".equalsIgnoreCase(currencyCode)) return "€";
        if ("GBP".equalsIgnoreCase(currencyCode)) return "£";
        return "₹";
    }

    private String parseGeminiText(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        JsonElement textElement = root.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text");
        return textElement != null && textElement.isJsonPrimitive() ? textElement.getAsString() : null;
    }

    private TransactionDraftResponse parseDraft(String response, List<Category> categories) throws Exception {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Empty AI response");
        }

        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        JsonElement textElement = root.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text");
        String rawJson = textElement != null && textElement.isJsonPrimitive()
                ? textElement.getAsString()
                : root.toString();
        JsonObject draft = JsonParser.parseString(rawJson).getAsJsonObject();

        String categoryName = draft.has("categoryName") && !draft.get("categoryName").isJsonNull()
                ? draft.get("categoryName").getAsString()
                : null;
        Category matchedCategory = resolveCategory(categoryName, categories);
        LocalDate transactionDate = LocalDate.parse(
                draft.has("transactionDate") ? draft.get("transactionDate").getAsString() : LocalDate.now().toString());
        TransactionType type = parseTransactionType(draft.has("type") ? draft.get("type").getAsString() : "EXPENSE");

        return new TransactionDraftResponse(
                draft.has("title") ? draft.get("title").getAsString() : "",
                Math.abs(draft.has("amount") ? draft.get("amount").getAsDouble() : 0.0),
                type,
                matchedCategory != null ? matchedCategory.getId() : null,
                categoryName,
                transactionDate,
                draft.has("confidence") ? draft.get("confidence").getAsString() : "LOW"
        );
    }

    private void validateReceiptFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Receipt validation failed: missing file");
            throw new ApiRequestException("Image file is required.", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_RECEIPT_SIZE_BYTES) {
            log.warn("Receipt validation failed: file too large sizeBytes={}", file.getSize());
            throw new ApiRequestException("Image file size must be under 2 MB.", HttpStatus.BAD_REQUEST);
        }

        String mimeType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean allowed = mimeType.equals("image/jpeg")
                || mimeType.equals("image/jpg")
                || mimeType.equals("image/png")
                || mimeType.equals("image/webp")
                || mimeType.equals("image/heic");
        if (!allowed) {
            log.warn("Receipt validation failed: unsupported mimeType={} fileName={}", mimeType, file.getOriginalFilename());
            throw new ApiRequestException("Unsupported image format. Use JPEG, PNG, WebP, or HEIC.", HttpStatus.BAD_REQUEST);
        }
    }

    private Category resolveCategory(String categoryName, List<Category> categories) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String needle = categoryName.toLowerCase(Locale.ROOT);
        return categories.stream()
                .filter(category -> category.getName() != null
                        && category.getName().toLowerCase(Locale.ROOT).contains(needle))
                .findFirst()
                .orElse(null);
    }

    private TransactionType parseTransactionType(String value) {
        try {
            return TransactionType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return TransactionType.EXPENSE;
        }
    }

    private TransactionDraftResponse parseTextLocally(String prompt, List<Category> categories) {
        if (prompt == null || prompt.isBlank()) {
            return new TransactionDraftResponse("", 0.0, TransactionType.EXPENSE, null, null, LocalDate.now(), "LOW");
        }

        String text = prompt.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        // Date detection
        LocalDate txDate = LocalDate.now();
        if (lower.contains("yesterday")) {
            txDate = txDate.minusDays(1);
        }

        // Type detection
        TransactionType type = TransactionType.EXPENSE;
        if (lower.contains("salary") || lower.contains("credited") || lower.contains("received")
                || lower.contains("refund") || lower.contains("cashback") || lower.contains("income") || lower.contains("got paid")) {
            type = TransactionType.CREDIT;
        }

        // Amount detection: look for patterns like "for 539rs", "539 rs", "₹539", "$50", "539"
        double amount = 0.0;
        java.util.regex.Pattern explicitPattern = java.util.regex.Pattern.compile(
                "(?:for|spent|paid|cost|of)?\\s*(?:rs\\.?|inr|₹|\\$|€|£)\\s*([0-9]+(?:\\.[0-9]{1,2})?)|([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|rupees)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher explicitMatcher = explicitPattern.matcher(text);
        if (explicitMatcher.find()) {
            String val = explicitMatcher.group(1) != null ? explicitMatcher.group(1) : explicitMatcher.group(2);
            amount = Double.parseDouble(val);
        } else {
            java.util.regex.Pattern numPattern = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)");
            java.util.regex.Matcher nm = numPattern.matcher(text);
            while (nm.find()) {
                double val = Double.parseDouble(nm.group(1));
                if (val > amount) {
                    amount = val;
                }
            }
        }

        // Category matching
        Category matchedCategory = null;
        String matchedCatName = null;
        for (Category cat : categories) {
            if (cat.getName() != null && lower.contains(cat.getName().toLowerCase(Locale.ROOT))) {
                matchedCategory = cat;
                matchedCatName = cat.getName();
                break;
            }
        }
        if (matchedCategory == null) {
            if (lower.contains("dinner") || lower.contains("lunch") || lower.contains("food") || lower.contains("coffee") || lower.contains("restaurant") || lower.contains("eat")) {
                matchedCategory = resolveCategory("Food", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : "Food";
            } else if (lower.contains("cab") || lower.contains("uber") || lower.contains("ola") || lower.contains("metro") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("rapido")) {
                matchedCategory = resolveCategory("Transport", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : "Transport";
            } else if (lower.contains("groceries") || lower.contains("packet") || lower.contains("packets") || lower.contains("milk") || lower.contains("litter") || lower.contains("mart") || lower.contains("cat")) {
                matchedCategory = resolveCategory("Groceries", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : "Groceries";
            } else if (lower.contains("movie") || lower.contains("netflix") || lower.contains("game")) {
                matchedCategory = resolveCategory("Entertainment", categories);
                matchedCatName = matchedCategory != null ? matchedCategory.getName() : "Entertainment";
            }
        }

        // Clean title
        String cleanTitle = text;
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(today|yesterday|tomorrow)\\b", "");
        cleanTitle = cleanTitle.replaceAll("(?i)\\b(for|spent|paid)\\s*(?:rs\\.?|inr|₹|\\$|€|£)?\\s*[0-9]+(?:\\.[0-9]+)?\\s*(?:rs\\.?|inr|rupees)?\\b", "");
        cleanTitle = cleanTitle.replaceAll("(?i)(?:rs\\.?|inr|₹|\\$|€|£)\\s*[0-9]+(?:\\.[0-9]+)?", "");
        cleanTitle = cleanTitle.replaceAll("(?i)[0-9]+(?:\\.[0-9]+)?\\s*(?:rs\\.?|inr|rupees)", "");
        cleanTitle = cleanTitle.replaceAll("(?i)^(bought|purchased|paid for|paid|spent on)\\s+", "");
        cleanTitle = cleanTitle.replaceAll("\\s+", " ").trim();
        if (cleanTitle.isBlank()) {
            cleanTitle = prompt.trim();
        }
        cleanTitle = cleanTitle.substring(0, 1).toUpperCase(Locale.ROOT) + (cleanTitle.length() > 1 ? cleanTitle.substring(1) : "");

        return new TransactionDraftResponse(
                cleanTitle,
                amount,
                type,
                matchedCategory != null ? matchedCategory.getId() : null,
                matchedCatName,
                txDate,
                amount > 0 ? "HIGH" : "MEDIUM"
        );
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
