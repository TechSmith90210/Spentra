package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;

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

        // Set real security context with test principal
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userId.toString(), null, List.of())
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateOrUpdateBudget_NullRequest() {
        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(null);
        });
        assertEquals("A valid positive limit amount is required", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_NullAmountLimit() {
        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(null);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("A valid positive limit amount is required", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_NegativeAmountLimit() {
        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(-50.0);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("A valid positive limit amount is required", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_NullAuthentication() {
        // Clear security context
        SecurityContextHolder.clearContext();

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(100.0);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("Unauthenticated request", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_NullPrincipal() {
        // Set security context with null principal
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(null, null, List.of())
        );

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(100.0);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("Unauthenticated request", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_UserNotFound() {
        UUID nonExistentUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(nonExistentUserId.toString(), null, List.of())
        );

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(100.0);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("User not found", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_CategoryNotFound() {
        UUID nonExistentCategoryId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentCategoryId)).thenReturn(Optional.empty());

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(100.0);
        req.setCategoryId(nonExistentCategoryId);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("Category not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_CategoryAccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);

        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setUser(otherUser); // Owned by someone else

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(100.0);
        req.setCategoryId(categoryId);
        req.setBudgetMonth("2026-07");

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });
        assertEquals("Access denied to the specified category", exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void testCreateOrUpdateBudget_CreateGlobalBudget() {
        YearMonth targetMonth = YearMonth.of(2026, 7);

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(350.0);
        req.setBudgetMonth("2026-07");
        req.setCategoryId(null);

        // Mock repo lookup to return empty
        when(budgetRepository.findByUserIdAndCategoryAndBudgetMonth(userId, null, targetMonth))
                .thenReturn(Optional.empty());

        // Mock repo save
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        BudgetResponse response = budgetService.createOrUpdateBudget(req);

        assertEquals(350.0, response.getAmountLimit());
        assertEquals("Global", response.getCategoryName());
        assertEquals(null, response.getCategoryId());
        assertEquals("2026-07", response.getBudgetMonth());
    }

    @Test
    void testCreateOrUpdateBudget_UpdateGlobalBudget() {
        YearMonth targetMonth = YearMonth.of(2026, 7);

        Budget existingBudget = new Budget();
        existingBudget.setId(UUID.randomUUID());
        existingBudget.setUser(testUser);
        existingBudget.setCategory(null);
        existingBudget.setAmountLimit(300.0);
        existingBudget.setBudgetMonth(targetMonth);

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(450.0);
        req.setBudgetMonth("2026-07");
        req.setCategoryId(null);

        // Mock repo lookup to return the existing budget
        when(budgetRepository.findByUserIdAndCategoryAndBudgetMonth(userId, null, targetMonth))
                .thenReturn(Optional.of(existingBudget));

        // Mock repo save
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = budgetService.createOrUpdateBudget(req);

        assertEquals(existingBudget.getId(), response.getId());
        assertEquals(450.0, response.getAmountLimit());
        assertEquals("Global", response.getCategoryName());
        assertEquals(null, response.getCategoryId());
        assertEquals("2026-07", response.getBudgetMonth());
    }

    @Test
    void testCreateOrUpdateBudget_CreateCategoryBudget() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Shopping");
        category.setUser(testUser);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        YearMonth targetMonth = YearMonth.of(2026, 7);

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(150.0);
        req.setBudgetMonth("2026-07");
        req.setCategoryId(categoryId);

        // Mock repo lookup
        when(budgetRepository.findByUserIdAndCategoryAndBudgetMonth(userId, category, targetMonth))
                .thenReturn(Optional.empty());

        // Mock repo save
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        BudgetResponse response = budgetService.createOrUpdateBudget(req);

        assertEquals(150.0, response.getAmountLimit());
        assertEquals("Shopping", response.getCategoryName());
        assertEquals(categoryId, response.getCategoryId());
        assertEquals("2026-07", response.getBudgetMonth());
    }

    @Test
    void testCreateOrUpdateBudget_UpdateCategoryBudget() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Shopping");
        category.setUser(testUser);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        YearMonth targetMonth = YearMonth.of(2026, 7);

        Budget existingBudget = new Budget();
        existingBudget.setId(UUID.randomUUID());
        existingBudget.setUser(testUser);
        existingBudget.setCategory(category);
        existingBudget.setAmountLimit(120.0);
        existingBudget.setBudgetMonth(targetMonth);

        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(200.0);
        req.setBudgetMonth("2026-07");
        req.setCategoryId(categoryId);

        // Mock repo lookup
        when(budgetRepository.findByUserIdAndCategoryAndBudgetMonth(userId, category, targetMonth))
                .thenReturn(Optional.of(existingBudget));

        // Mock repo save
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = budgetService.createOrUpdateBudget(req);

        assertEquals(existingBudget.getId(), response.getId());
        assertEquals(200.0, response.getAmountLimit());
        assertEquals("Shopping", response.getCategoryName());
        assertEquals(categoryId, response.getCategoryId());
        assertEquals("2026-07", response.getBudgetMonth());
    }

    @Test
    void testCreateOrUpdateBudget_DefaultToCurrentMonth() {
        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(250.0);
        req.setBudgetMonth(null); // Should default to YearMonth.now()
        req.setCategoryId(null);

        YearMonth currentMonth = YearMonth.now();

        when(budgetRepository.findByUserIdAndCategoryAndBudgetMonth(userId, null, currentMonth))
                .thenReturn(Optional.empty());

        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        BudgetResponse response = budgetService.createOrUpdateBudget(req);

        assertEquals(250.0, response.getAmountLimit());
        assertEquals("Global", response.getCategoryName());
        assertEquals(currentMonth.toString(), response.getBudgetMonth());
    }

    @Test
    void testCreateOrUpdateBudget_InvalidMonthFormat() {
        BudgetRequest req = new BudgetRequest();
        req.setAmountLimit(100.0);
        req.setBudgetMonth("invalid-month");
        req.setCategoryId(null);

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> {
            budgetService.createOrUpdateBudget(req);
        });

        assertEquals("Invalid month format. Please use YYYY-MM", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
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

    @Test
    void testGetBudgetSummary_Global() {
        YearMonth currentMonth = YearMonth.of(2026, 7);
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        Budget globalBudget = new Budget();
        globalBudget.setId(UUID.randomUUID());
        globalBudget.setUser(testUser);
        globalBudget.setCategory(null); // Global budget
        globalBudget.setAmountLimit(500.0);
        globalBudget.setBudgetMonth(currentMonth);

        List<Budget> budgets = List.of(globalBudget);
        when(budgetRepository.findByUserIdAndBudgetMonth(userId, currentMonth)).thenReturn(budgets);

        // Expenses across multiple categories
        List<Object[]> queryResults = new ArrayList<>();
        queryResults.add(new Object[] { UUID.randomUUID(), 120.0 });
        queryResults.add(new Object[] { UUID.randomUUID(), 150.0 });
        queryResults.add(new Object[] { null, 30.0 }); // Uncategorized
        when(expenseRepository.calculateTotalSpentByCategory(eq(userId), eq(TransactionType.EXPENSE), eq(start), eq(end)))
                .thenReturn(queryResults);

        List<BudgetSummaryResponse> summary = budgetService.getBudgetSummary("2026-07");

        assertEquals(1, summary.size());

        BudgetSummaryResponse s = summary.get(0);
        assertEquals("Global", s.getCategoryName());
        assertEquals(null, s.getCategoryId());
        // For global budget, actualSpent should be the sum of ALL monthly expenses: 120 + 150 + 30 = 300.0
        assertEquals(300.0, s.getActualSpent());
        assertEquals(200.0, s.getRemaining());
        assertFalse(s.getIsExceeded());
    }

    @Test
    void testGetBudgetSummary_Mixed() {
        YearMonth currentMonth = YearMonth.of(2026, 7);
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        Category category1 = new Category();
        UUID catId1 = UUID.randomUUID();
        category1.setId(catId1);
        category1.setName("Gaming");

        Budget globalBudget = new Budget();
        globalBudget.setId(UUID.randomUUID());
        globalBudget.setUser(testUser);
        globalBudget.setCategory(null); // Global
        globalBudget.setAmountLimit(300.0);
        globalBudget.setBudgetMonth(currentMonth);

        Budget budget1 = new Budget();
        budget1.setId(UUID.randomUUID());
        budget1.setUser(testUser);
        budget1.setCategory(category1);
        budget1.setAmountLimit(100.0);
        budget1.setBudgetMonth(currentMonth);

        List<Budget> budgets = List.of(globalBudget, budget1);
        when(budgetRepository.findByUserIdAndBudgetMonth(userId, currentMonth)).thenReturn(budgets);

        // Expenses
        List<Object[]> queryResults = new ArrayList<>();
        queryResults.add(new Object[] { catId1, 120.0 });
        queryResults.add(new Object[] { UUID.randomUUID(), 150.0 });
        queryResults.add(new Object[] { null, 50.0 });
        when(expenseRepository.calculateTotalSpentByCategory(eq(userId), eq(TransactionType.EXPENSE), eq(start), eq(end)))
                .thenReturn(queryResults);

        List<BudgetSummaryResponse> summary = budgetService.getBudgetSummary("2026-07");

        assertEquals(2, summary.size());

        // Verify global budget summary
        BudgetSummaryResponse sGlobal = summary.stream().filter(s -> s.getCategoryId() == null).findFirst().get();
        // Total expenses: 120 + 150 + 50 = 320.0
        assertEquals(320.0, sGlobal.getActualSpent());
        assertEquals(-20.0, sGlobal.getRemaining());
        assertTrue(sGlobal.getIsExceeded());

        // Verify category specific budget summary
        BudgetSummaryResponse sCat = summary.stream().filter(s -> catId1.equals(s.getCategoryId())).findFirst().get();
        assertEquals(120.0, sCat.getActualSpent());
        assertEquals(-20.0, sCat.getRemaining());
        assertTrue(sCat.getIsExceeded());
    }
}