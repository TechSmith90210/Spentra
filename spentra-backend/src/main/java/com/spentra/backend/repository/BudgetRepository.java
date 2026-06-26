package com.spentra.backend.repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Budget;

/**
 * Repository interface for Budget database operations.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /**
     * Retrieves all budgets configured for a user in a specific month.
     *
     * @param userId user ID
     * @param budgetMonth YearMonth representing the billing month
     * @return List of Budgets
     */
    List<Budget> findByUserIdAndBudgetMonth(UUID userId, YearMonth budgetMonth);

    /**
     * Resolves a specific budget by user, category (can be null for global), and month.
     *
     * @param userId user ID
     * @param category category entity, or null for global budget
     * @param budgetMonth YearMonth representing the billing month
     * @return Optional Budget
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND (:category IS NULL AND b.category IS NULL OR b.category = :category) " +
           "AND b.budgetMonth = :budgetMonth")
    Optional<Budget> findByUserIdAndCategoryAndBudgetMonth(
        @Param("userId") UUID userId,
        @Param("category") Category category,
        @Param("budgetMonth") YearMonth budgetMonth
    );
}
