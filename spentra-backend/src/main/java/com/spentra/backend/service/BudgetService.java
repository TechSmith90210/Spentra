package com.spentra.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.budget.BudgetRequest;
import com.spentra.backend.model.dto.budget.BudgetResponse;
import com.spentra.backend.model.dto.budget.BudgetSummaryResponse;
import com.spentra.backend.model.entity.Budget;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.BudgetRepository;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class handling business logic for monthly budget planning and spending evaluations.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository repo;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Helper method to retrieve the currently authenticated user from SecurityContext.
     */
    private User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userIdStr == null) {
            throw new ApiRequestException("Unauthenticated request", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.UNAUTHORIZED));
    }

    /**
     * Resolves the Category entity and validates that it is accessible to the user
     * (either global category or owned by the user).
     */
    private Category resolveCategory(UUID categoryId, UUID userId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiRequestException("Category not found", HttpStatus.NOT_FOUND));

        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new ApiRequestException("Access denied to the specified category", HttpStatus.FORBIDDEN);
        }
        return category;
    }

    /**
     * Parses YearMonth from a string, defaulting to YearMonth.now() if null or empty.
     */
    private YearMonth parseYearMonth(String monthStr) {
        if (monthStr == null || monthStr.trim().isEmpty()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(monthStr);
        } catch (DateTimeParseException e) {
            throw new ApiRequestException("Invalid month format. Please use YYYY-MM", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Creates a new budget or updates an existing one for the user, category, and month.
     *
     * @param req budget request details
     * @return BudgetResponse details of the created/updated budget
     */
    public BudgetResponse createOrUpdateBudget(BudgetRequest req) {
        if (req == null || req.getAmountLimit() == null || req.getAmountLimit() < 0) {
            throw new ApiRequestException("A valid positive limit amount is required", HttpStatus.BAD_REQUEST);
        }

        User currentUser = getCurrentUser();
        YearMonth budgetMonth = parseYearMonth(req.getBudgetMonth());
        Category category = resolveCategory(req.getCategoryId(), currentUser.getId());

        // Check if a budget already exists for this user, category, and month
        Budget budget = repo.findByUserIdAndCategoryAndBudgetMonth(currentUser.getId(), category, budgetMonth)
                .orElseGet(() -> {
                    Budget newBudget = new Budget();
                    newBudget.setUser(currentUser);
                    newBudget.setCategory(category);
                    newBudget.setBudgetMonth(budgetMonth);
                    return newBudget;
                });

        budget.setAmountLimit(req.getAmountLimit());
        Budget saved = repo.save(budget);

        UUID categoryId = saved.getCategory() != null ? saved.getCategory().getId() : null;
        String categoryName = saved.getCategory() != null ? saved.getCategory().getName() : "Global";

        return new BudgetResponse(
                saved.getId(),
                categoryId,
                categoryName,
                saved.getAmountLimit(),
                saved.getBudgetMonth().toString()
        );
    }

    /**
     * Aggregates budgets configured for a given month and computes the actual spending
     * to check if thresholds are exceeded.
     *
     * @param monthStr YearMonth filter in "YYYY-MM" format (defaults to current month if null)
     * @return List of BudgetSummaryResponse
     */
    public List<BudgetSummaryResponse> getBudgetSummary(String monthStr) {
        User currentUser = getCurrentUser();
        YearMonth budgetMonth = parseYearMonth(monthStr);

        List<Budget> budgets = repo.findByUserIdAndBudgetMonth(currentUser.getId(), budgetMonth);

        LocalDate startDate = budgetMonth.atDay(1);
        LocalDate endDate = budgetMonth.atEndOfMonth();

        return budgets.stream().map(budget -> {
            Category category = budget.getCategory();
            Double limit = budget.getAmountLimit();

            // Query actual spent
            Double actualSpent = expenseRepository.calculateTotalSpent(
                    currentUser.getId(),
                    TransactionType.EXPENSE,
                    category,
                    startDate,
                    endDate
            );

            Double remaining = limit - actualSpent;
            Boolean isExceeded = actualSpent > limit;

            UUID categoryId = category != null ? category.getId() : null;
            String categoryName = category != null ? category.getName() : "Global";

            return new BudgetSummaryResponse(
                    budget.getId(),
                    categoryId,
                    categoryName,
                    limit,
                    actualSpent,
                    remaining,
                    isExceeded,
                    budgetMonth.toString()
            );
        }).collect(Collectors.toList());
    }
}
