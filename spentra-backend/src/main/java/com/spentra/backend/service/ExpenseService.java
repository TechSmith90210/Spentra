package com.spentra.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.category.CategoryResponse;
import com.spentra.backend.model.dto.expense.ExpenseRequest;
import com.spentra.backend.model.dto.expense.ExpenseResponse;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class responsible for handling transaction (expense/credit) operations.
 * Enforces user isolation to ensure security across all CRUD endpoints.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository repo;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Helper method to retrieve the currently authenticated user from SecurityContext.
     *
     * @return User the authenticated user entity
     * @throws ApiRequestException if the user is unauthenticated or not found
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
     *
     * @param categoryId the category ID to resolve
     * @param userId the current user's ID
     * @return the verified Category entity
     */
    private Category resolveCategory(UUID categoryId, UUID userId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiRequestException("Category not found", HttpStatus.NOT_FOUND));
        
        // Category must be global (user is null) or owned by the active user
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new ApiRequestException("Access denied to the specified category", HttpStatus.FORBIDDEN);
        }
        return category;
    }

    /**
     * Map Category entity to CategoryResponse.
     */
    private CategoryResponse mapToCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(category.getId(), category.getName());
    }

    /**
     * Map Expense entity to ExpenseResponse.
     */
    private ExpenseResponse mapToExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                mapToCategoryResponse(expense.getCategory()),
                expense.getType(),
                expense.getTransactionDate(),
                expense.getIsRecurring(),
                expense.getRecurrence(),
                expense.getNextExecutionDate()
        );
    }

    /**
     * Helper to compute the next execution date for a recurring transaction.
     *
     * @param currentDate starting date
     * @param period frequency of recurrence
     * @return LocalDate the calculated next execution date, or null if period is NONE
     */
    public LocalDate calculateNextExecutionDate(LocalDate currentDate, RecurrencePeriod period) {
        if (period == null || period == RecurrencePeriod.NONE) {
            return null;
        }
        switch (period) {
            case DAILY:
                return currentDate.plusDays(1);
            case WEEKLY:
                return currentDate.plusWeeks(1);
            case MONTHLY:
                return currentDate.plusMonths(1);
            case YEARLY:
                return currentDate.plusYears(1);
            default:
                return null;
        }
    }

    /**
     * Adds a transaction (expense or credit) for the authenticated user.
     *
     * @param req request details
     * @return ExpenseResponse representing the created transaction
     */
    public ExpenseResponse addExpense(ExpenseRequest req) {
        if (req == null || req.getTitle() == null || req.getTitle().trim().isEmpty() || req.getAmount() == null) {
            throw new ApiRequestException("Title and amount are required", HttpStatus.BAD_REQUEST);
        }

        User currentUser = getCurrentUser();
        Expense expense = new Expense();

        expense.setTitle(req.getTitle());
        expense.setAmount(req.getAmount());
        expense.setUser(currentUser);
        expense.setCategory(resolveCategory(req.getCategoryId(), currentUser.getId()));
        
        TransactionType type = req.getType() != null ? req.getType() : TransactionType.EXPENSE;
        expense.setType(type);

        LocalDate date = req.getTransactionDate() != null ? req.getTransactionDate() : LocalDate.now();
        expense.setTransactionDate(date);

        Boolean isRec = req.getIsRecurring() != null ? req.getIsRecurring() : false;
        expense.setIsRecurring(isRec);

        RecurrencePeriod recPeriod = req.getRecurrence() != null ? req.getRecurrence() : RecurrencePeriod.NONE;
        expense.setRecurrence(recPeriod);

        if (isRec && recPeriod != RecurrencePeriod.NONE) {
            expense.setNextExecutionDate(calculateNextExecutionDate(date, recPeriod));
        } else {
            expense.setNextExecutionDate(null);
        }

        Expense saved = repo.save(expense);
        return mapToExpenseResponse(saved);
    }

    /**
     * Retrieves all transactions belonging to the authenticated user.
     *
     * @return List of ExpenseResponse
     */
    public List<ExpenseResponse> getExpenses() {
        User currentUser = getCurrentUser();
        List<Expense> expenses = repo.findByUserId(currentUser.getId());

        return expenses.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing transaction details with user isolation validation.
     *
     * @param req request containing details to update
     * @param id the ID of the transaction to update
     * @return ExpenseResponse representing the updated transaction
     */
    public ExpenseResponse updateExpense(ExpenseRequest req, UUID id) {
        Expense existingExpense = repo.findById(id)
                .orElseThrow(() -> new ApiRequestException("Transaction not found with the specified ID", HttpStatus.NOT_FOUND));

        User currentUser = getCurrentUser();

        // Enforce user isolation
        if (!existingExpense.getUser().getId().equals(currentUser.getId())) {
            throw new ApiRequestException("Access denied: You do not own this transaction", HttpStatus.FORBIDDEN);
        }

        if (req.getTitle() != null) {
            existingExpense.setTitle(req.getTitle());
        }

        if (req.getAmount() != null) {
            existingExpense.setAmount(req.getAmount());
        }

        if (req.getCategoryId() != null) {
            existingExpense.setCategory(resolveCategory(req.getCategoryId(), currentUser.getId()));
        }

        if (req.getType() != null) {
            existingExpense.setType(req.getType());
        }

        if (req.getTransactionDate() != null) {
            existingExpense.setTransactionDate(req.getTransactionDate());
        }

        if (req.getIsRecurring() != null) {
            existingExpense.setIsRecurring(req.getIsRecurring());
        }

        if (req.getRecurrence() != null) {
            existingExpense.setRecurrence(req.getRecurrence());
        }

        // Recalculate next execution date if recurrence properties changed
        if (existingExpense.getIsRecurring() && existingExpense.getRecurrence() != RecurrencePeriod.NONE) {
            existingExpense.setNextExecutionDate(calculateNextExecutionDate(existingExpense.getTransactionDate(), existingExpense.getRecurrence()));
        } else {
            existingExpense.setNextExecutionDate(null);
        }

        Expense updatedExpense = repo.save(existingExpense);
        return mapToExpenseResponse(updatedExpense);
    }

    /**
     * Deletes a transaction with user isolation check.
     *
     * @param id transaction ID
     */
    public void deleteExpense(UUID id) {
        Expense expenseToDelete = repo.findById(id)
                .orElseThrow(() -> new ApiRequestException("Transaction not found with the specified ID", HttpStatus.NOT_FOUND));

        User currentUser = getCurrentUser();

        // Enforce user isolation
        if (!expenseToDelete.getUser().getId().equals(currentUser.getId())) {
            throw new ApiRequestException("Access denied: You do not own this transaction", HttpStatus.FORBIDDEN);
        }

        repo.delete(expenseToDelete);
    }
}

