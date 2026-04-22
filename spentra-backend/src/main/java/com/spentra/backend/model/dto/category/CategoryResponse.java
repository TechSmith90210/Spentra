package com.spentra.backend.model.dto.category;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
}
