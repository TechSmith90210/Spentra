package com.spentra.backend.model.entity;

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

/**
 * Entity representing a monthly spending limit (budget) set by a user.
 * A budget can be category-specific or global (overall monthly limit, where category is null).
 */
@Getter
@Setter
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    @Column(name = "amount_limit", nullable = false)
    private Double amountLimit;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "budget_month", nullable = false)
    private YearMonth budgetMonth;
}
