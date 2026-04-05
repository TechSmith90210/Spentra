package com.spentra.backend.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExpenseResponse {
    private UUID id;
    private String title;
    private Double amount;
}
