package com.spentra.backend.model.dto.expense;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseRequest {
    private String title;
    private Double amount;
}
