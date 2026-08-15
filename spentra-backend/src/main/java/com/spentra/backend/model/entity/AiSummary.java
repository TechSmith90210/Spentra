package com.spentra.backend.model.entity;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import com.spentra.backend.model.converter.YearMonthConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_summaries")
public class AiSummary {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "year_month", nullable = false)
    private YearMonth yearMonth;

    @Column(name = "summary_text", nullable = false, length = 1000)
    private String summaryText;

    @Column(name = "total_spent", nullable = false)
    private Double totalSpent;

    @Column(name = "top_category")
    private String topCategory;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
