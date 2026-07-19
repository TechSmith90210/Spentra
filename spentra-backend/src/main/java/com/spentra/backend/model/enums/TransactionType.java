package com.spentra.backend.model.enums;

/**
 * Represents the type of a transaction in the Spentra application.
 * Transactions can be classified as either EXPENSE (outgoing funds) or CREDIT (incoming funds).
 */
public enum TransactionType {
    /**
     * Outgoing spending/debit transaction.
     */
    EXPENSE,

    /**
     * Incoming income, salary, or transfer credit transaction.
     */
    CREDIT
}
