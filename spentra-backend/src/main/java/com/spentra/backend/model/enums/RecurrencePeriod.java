package com.spentra.backend.model.enums;

/**
 * Represents the recurrence frequency of a recurring transaction.
 */
public enum RecurrencePeriod {
    /**
     * Non-recurring, one-off transaction.
     */
    NONE,

    /**
     * Occurs every day.
     */
    DAILY,

    /**
     * Occurs every week.
     */
    WEEKLY,

    /**
     * Occurs every month.
     */
    MONTHLY,

    /**
     * Occurs every year.
     */
    YEARLY
}
