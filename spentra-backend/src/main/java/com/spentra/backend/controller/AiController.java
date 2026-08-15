package com.spentra.backend.controller;

import com.spentra.backend.model.dto.ai.QuickAddRequest;
import com.spentra.backend.model.dto.ai.AiSummaryResponse;
import com.spentra.backend.model.dto.ai.TransactionDraftResponse;
import com.spentra.backend.service.GeminiService;

import java.time.YearMonth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiService geminiService;

    @PostMapping("/parse-text")
    public ResponseEntity<TransactionDraftResponse> parseText(@Valid @RequestBody QuickAddRequest request) {
        return ResponseEntity.ok(geminiService.parseText(request.getPrompt()));
    }

    @PostMapping("/parse-receipt")
    public ResponseEntity<TransactionDraftResponse> parseReceipt(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(geminiService.parseReceipt(file));
    }

    @GetMapping("/insights")
    public ResponseEntity<AiSummaryResponse> getInsights(
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "currency", defaultValue = "INR") String currency) {
        String targetMonth = month != null ? month : YearMonth.now().toString();
        return ResponseEntity.ok(geminiService.getOrGenerateInsights(targetMonth, currency));
    }

    @PostMapping("/insights/refresh")
    public ResponseEntity<AiSummaryResponse> refreshInsights(
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "currency", defaultValue = "INR") String currency) {
        String targetMonth = month != null ? month : YearMonth.now().toString();
        return ResponseEntity.ok(geminiService.refreshInsights(targetMonth, currency));
    }
}
