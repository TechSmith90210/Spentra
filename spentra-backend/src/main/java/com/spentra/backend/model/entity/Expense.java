package com.spentra.backend.model.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.model.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a transaction in the system.
 * It can represent either an expense (outgoing debit) or a credit (incoming income).
 * Supports optional category mapping, user ownership, and recurring scheduling configurations.
 */
@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue
    private UUID id;

    private String title;

    private Double amount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Type of the transaction: EXPENSE (outgoing payment) or CREDIT (incoming money).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TransactionType type = TransactionType.EXPENSE;

    /**
     * The date when the transaction took place.
     */
    @Column(name = "transaction_date")
    private LocalDate transactionDate = LocalDate.now();

    /**
     * Boolean flag indicating whether this transaction is configured to recur regularly.
     */
    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    /**
     * The recurrence period frequency (DAILY, WEEKLY, MONTHLY, YEARLY, or NONE) for the transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence")
    private RecurrencePeriod recurrence = RecurrencePeriod.NONE;

    /**
     * The next scheduled execution date when this recurring transaction should spawn a new instance.
     */
    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;
}
