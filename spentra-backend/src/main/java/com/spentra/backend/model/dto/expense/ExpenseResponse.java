package com.spentra.backend.model.dto.expense;

import java.time.LocalDate;
import java.util.UUID;

import com.spentra.backend.model.dto.category.CategoryResponse;
import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.model.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Transfer Object (DTO) representing a transaction response.
 */
@Getter
@AllArgsConstructor
public class ExpenseResponse {
    private UUID id;
    private String title;
    private Double amount;
    private CategoryResponse category;
    private TransactionType type;
    private LocalDate transactionDate;
    private Boolean isRecurring;
    private RecurrencePeriod recurrence;
    private LocalDate nextExecutionDate;
}

