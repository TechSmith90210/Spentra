package com.spentra.backend.model.dto.budget;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Transfer Object (DTO) representing a budget response.
 */
@Getter
@AllArgsConstructor
public class BudgetResponse {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private Double amountLimit;
    private String budgetMonth;
}
