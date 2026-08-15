package com.spentra.backend.model.dto.ai;

import java.time.LocalDate;
import java.util.UUID;

import com.spentra.backend.model.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionDraftResponse {
    private String title;
    private Double amount;
    private TransactionType type;
    private UUID categoryId;
    private String categoryName;
    private LocalDate transactionDate;
    private String confidence;
}
