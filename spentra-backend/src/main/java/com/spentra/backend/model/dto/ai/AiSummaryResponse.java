package com.spentra.backend.model.dto.ai;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiSummaryResponse {
    private UUID id;
    private String yearMonth;
    private String summaryText;
    private Double totalSpent;
    private String topCategory;
    private LocalDateTime generatedAt;
}
