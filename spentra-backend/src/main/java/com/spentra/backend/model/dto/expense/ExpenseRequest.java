package com.spentra.backend.model.dto.expense;

import java.time.LocalDate;
import java.util.UUID;

import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.model.enums.TransactionType;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for creating or updating a transaction (expense/credit).
 */
@Getter
@Setter
public class ExpenseRequest {
    /**
     * Brief title or description of the transaction.
     */
    private String title;

    /**
     * Monetary amount of the transaction.
     */
    private Double amount;

    /**
     * Optional ID of the category this transaction belongs to.
     */
    private UUID categoryId;

    /**
     * Type of the transaction: EXPENSE or CREDIT. Defaults to EXPENSE if omitted.
     */
    private TransactionType type;

    /**
     * Date when the transaction occurred. Defaults to current date if omitted.
     */
    private LocalDate transactionDate;

    /**
     * Flag indicating whether the transaction is recurring.
     */
    private Boolean isRecurring;

    /**
     * Recurrence frequency: NONE, DAILY, WEEKLY, MONTHLY, or YEARLY.
     */
    private RecurrencePeriod recurrence;
}

