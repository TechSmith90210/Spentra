package com.spentra.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.spentra.backend.model.dto.ai.QuickAddRequest;
import com.spentra.backend.model.dto.ai.TransactionDraftResponse;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.service.GeminiService;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private AiController aiController;

    @Test
    void parseText_returnsDraft() {
        QuickAddRequest request = new QuickAddRequest();
        request.setPrompt("Spent 45 on dinner yesterday");

        TransactionDraftResponse draft = new TransactionDraftResponse(
                "Dinner",
                45.0,
                TransactionType.EXPENSE,
                UUID.randomUUID(),
                "Food",
                LocalDate.of(2026, 8, 14),
                "HIGH");

        when(geminiService.parseText(anyString())).thenReturn(draft);

        ResponseEntity<TransactionDraftResponse> response = aiController.parseText(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Dinner", response.getBody().getTitle());
        assertEquals(45.0, response.getBody().getAmount());
        assertEquals(TransactionType.EXPENSE, response.getBody().getType());
    }
}
