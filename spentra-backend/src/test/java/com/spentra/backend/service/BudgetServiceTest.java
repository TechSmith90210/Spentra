package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.spentra.backend.model.dto.budget.BudgetSummaryResponse;
import com.spentra.backend.model.entity.Budget;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.BudgetRepository;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.repository.UserRepository;

class BudgetServiceTest {

    private BudgetRepository budgetRepository;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private ExpenseRepository expenseRepository;
    private BudgetService budgetService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        userRepository = mock(UserRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        budgetService = new BudgetService(budgetRepository, userRepository, categoryRepository, expenseRepository);

        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");

        // Mock security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @Test
    void testGetBudgetSummary_Optimized() {
        YearMonth currentMonth = YearMonth.of(2026, 7);
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        Category category1 = new Category();
        UUID catId1 = UUID.randomUUID();
        category1.setId(catId1);
        category1.setName("Gaming");

        Category category2 = new Category();
        UUID catId2 = UUID.randomUUID();
        category2.setId(catId2);
        category2.setName("Food");

        Budget budget1 = new Budget();
        budget1.setId(UUID.randomUUID());
        budget1.setUser(testUser);
        budget1.setCategory(category1);
        budget1.setAmountLimit(100.0);
        budget1.setBudgetMonth(currentMonth);

        Budget budget2 = new Budget();
        budget2.setId(UUID.randomUUID());
        budget2.setUser(testUser);
        budget2.setCategory(category2);
        budget2.setAmountLimit(200.0);
        budget2.setBudgetMonth(currentMonth);

        List<Budget> budgets = List.of(budget1, budget2);
        when(budgetRepository.findByUserIdAndBudgetMonth(userId, currentMonth)).thenReturn(budgets);

        // Mock result of the single optimized query
        List<Object[]> queryResults = new ArrayList<>();
        queryResults.add(new Object[] { catId1, 120.0 });
        queryResults.add(new Object[] { catId2, 150.0 });
        when(expenseRepository.calculateTotalSpentByCategory(eq(userId), eq(TransactionType.EXPENSE), eq(start), eq(end)))
                .thenReturn(queryResults);

        List<BudgetSummaryResponse> summary = budgetService.getBudgetSummary("2026-07");

        assertEquals(2, summary.size());

        BudgetSummaryResponse s1 = summary.stream().filter(s -> s.getCategoryId().equals(catId1)).findFirst().get();
        assertEquals(120.0, s1.getActualSpent());
        assertEquals(-20.0, s1.getRemaining());
        assertTrue(s1.getIsExceeded());

        BudgetSummaryResponse s2 = summary.stream().filter(s -> s.getCategoryId().equals(catId2)).findFirst().get();
        assertEquals(150.0, s2.getActualSpent());
        assertEquals(50.0, s2.getRemaining());
        assertFalse(s2.getIsExceeded());
    }
}
