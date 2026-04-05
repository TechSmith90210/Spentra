package com.spentra.backend.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseRequest {
    private String title;
    private Double amount;
}
