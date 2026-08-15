package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.ai.TransactionDraftResponse;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.repository.AiSummaryRepository;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private AiSummaryRepository aiSummaryRepository;

    @Mock
    private UserService userService;

    @Mock
    private RestTemplate restTemplate;

    private GeminiService geminiService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(categoryRepository, expenseRepository, aiSummaryRepository, userService, restTemplate);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
    }

    @Test
    void parseText_success_resolvesCategoryAndParsesDraft() {
        when(userService.getCurrentUser()).thenReturn(user);

        Category food = new Category();
        food.setId(UUID.randomUUID());
        food.setName("Food & Dining");
        food.setUser(null);

        when(categoryRepository.findByUserIdOrUserIsNull(userId)).thenReturn(List.of(food));

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"title\\":\\"Dinner\\",\\"amount\\":-45,\\"type\\":\\"EXPENSE\\",\\"categoryName\\":\\"Food\\",\\"transactionDate\\":\\"2026-08-14\\",\\"confidence\\":\\"HIGH\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(geminiResponse));

        ReflectionTestUtils.setField(geminiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiService, "apiUrl", "https://example.com");
        ReflectionTestUtils.setField(geminiService, "model", "gemini-1.5-flash");

        TransactionDraftResponse draft = geminiService.parseText("Spent 45 on dinner yesterday");

        assertEquals("Dinner", draft.getTitle());
        assertEquals(45.0, draft.getAmount());
        assertEquals("Food", draft.getCategoryName());
        assertNotNull(draft.getCategoryId());
        assertEquals(LocalDate.of(2026, 8, 14), draft.getTransactionDate());
    }

    @Test
    void parseText_missingApiKey_throwsServiceUnavailable() {
        ReflectionTestUtils.setField(geminiService, "apiKey", "");
        assertThrows(ApiRequestException.class, () -> geminiService.parseText("Spent 45 on dinner yesterday"));
    }

    @Test
    void parseReceipt_success_parsesDraft() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user);

        Category food = new Category();
        food.setId(UUID.randomUUID());
        food.setName("Food & Dining");
        food.setUser(null);
        when(categoryRepository.findByUserIdOrUserIsNull(userId)).thenReturn(List.of(food));

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"title\\":\\"Nando's\\",\\"amount\\":\\\"45.00\\\",\\"type\\":\\"EXPENSE\\",\\"categoryName\\":\\"Food\\",\\"transactionDate\\":\\"2026-08-14\\\",\\"confidence\\":\\"HIGH\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok(geminiResponse));

        ReflectionTestUtils.setField(geminiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiService, "apiUrl", "https://example.com");
        ReflectionTestUtils.setField(geminiService, "model", "gemini-1.5-flash");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                new byte[] { 1, 2, 3, 4 });

        TransactionDraftResponse draft = geminiService.parseReceipt(file);

        assertEquals("Nando's", draft.getTitle());
        assertEquals(45.0, draft.getAmount());
        assertEquals("Food", draft.getCategoryName());
        assertNotNull(draft.getCategoryId());
    }

    @Test
    void parseReceipt_oversizedFile_throwsBadRequest() {
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-key");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                new byte[10 * 1024 * 1024 + 1]);

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> geminiService.parseReceipt(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void parseReceipt_invalidMimeType_throwsBadRequest() {
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-key");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.txt",
                "text/plain",
                "hello".getBytes());

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> geminiService.parseReceipt(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
