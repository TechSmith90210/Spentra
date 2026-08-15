package com.spentra.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spentra.backend.model.dto.ai.TransactionDraftResponse;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.service.GeminiService;

@ExtendWith(MockitoExtension.class)
class AiReceiptControllerTest {

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private AiController aiController;

    @Test
    void parseReceipt_returnsDraft() {
        MultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", new byte[] { 1, 2 });
        TransactionDraftResponse draft = new TransactionDraftResponse(
                "Nando's",
                45.0,
                TransactionType.EXPENSE,
                UUID.randomUUID(),
                "Food",
                LocalDate.of(2026, 8, 14),
                "HIGH");

        when(geminiService.parseReceipt(file)).thenReturn(draft);

        ResponseEntity<TransactionDraftResponse> response = aiController.parseReceipt(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Nando's", response.getBody().getTitle());
        assertEquals(45.0, response.getBody().getAmount());
        assertEquals(TransactionType.EXPENSE, response.getBody().getType());
    }
}
