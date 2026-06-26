package com.spentra.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.enums.TransactionType;

/**
 * Repository interface for Expense database operations.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Finds all transactions (expenses/credits) belonging to a specific user.
     *
     * @param userId the UUID of the owner user
     * @return list of matching transactions
     */
    List<Expense> findByUserId(UUID userId);

    /**
     * Finds all recurring transaction templates that are due for execution.
     *
     * @param date the date threshold (typically today)
     * @return list of recurring transactions that should run
     */
    List<Expense> findByIsRecurringTrueAndNextExecutionDateLessThanEqual(LocalDate date);

    /**
     * Calculates the sum of transaction amounts of a specific type (EXPENSE/CREDIT)
     * for a user, within a date range, and filtering by category (or null for overall spending).
     *
     * @param userId the user whose transactions are being summed
     * @param type the transaction type (EXPENSE or CREDIT)
     * @param category the category entity, or null to calculate global spending
     * @param startDate the start date of the period (inclusive)
     * @param endDate the end date of the period (inclusive)
     * @return the total amount spent or received
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.user.id = :userId " +
           "AND e.type = :type " +
           "AND (:category IS NULL AND e.category IS NULL OR e.category = :category) " +
           "AND e.transactionDate BETWEEN :startDate AND :endDate")
    Double calculateTotalSpent(
        @Param("userId") UUID userId,
        @Param("type") TransactionType type,
        @Param("category") Category category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}