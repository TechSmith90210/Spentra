package com.spentra.backend.model.dto.budget;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Transfer Object (DTO) representing a monthly budget summary,
 * comparing the limit against actual spending.
 */
@Getter
@AllArgsConstructor
public class BudgetSummaryResponse {
    private UUID budgetId;
    private UUID categoryId;
    private String categoryName;
    private Double amountLimit;
    private Double actualSpent;
    private Double remaining;
    private Boolean isExceeded;
    private String budgetMonth;
}
