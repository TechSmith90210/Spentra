package com.spentra.backend.model.dto.budget;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for creating or updating a budget.
 */
@Getter
@Setter
public class BudgetRequest {

    /**
     * Optional ID of the category this budget applies to.
     * If null, this represents a global (overall) monthly budget.
     */
    private UUID categoryId;

    /**
     * The spending limit set for this budget.
     */
    private Double amountLimit;

    /**
     * The month for which the budget is defined (in "YYYY-MM" format, e.g., "2026-06").
     */
    private String budgetMonth;
}
