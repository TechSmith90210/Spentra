package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.expense.ExpenseRequest;
import com.spentra.backend.model.dto.expense.ExpenseResponse;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.repository.UserRepository;

class ExpenseServiceTest {

    private ExpenseRepository expenseRepository;
    private UserService userService;
    private CategoryRepository categoryRepository;
    private ExpenseService expenseService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        userService = mock(UserService.class);
        categoryRepository = mock(CategoryRepository.class);
        expenseService = new ExpenseService(expenseRepository, userService, categoryRepository);

        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");

        // Set real security context with test principal
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userId.toString(), null, List.of())
        );

        when(userService.getCurrentUser()).thenReturn(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAddExpense_withTypeAndDate() {
        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Salary");
        req.setAmount(5000.0);
        req.setType(TransactionType.CREDIT);
        req.setTransactionDate(LocalDate.of(2026, 7, 1));
        req.setIsRecurring(false);
        req.setRecurrence(RecurrencePeriod.NONE);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ExpenseResponse response = expenseService.addExpense(req);

        assertNotNull(response.getId());
        assertEquals("Salary", response.getTitle());
        assertEquals(5000.0, response.getAmount());
        assertEquals(TransactionType.CREDIT, response.getType());
        assertEquals(LocalDate.of(2026, 7, 1), response.getTransactionDate());
    }

    @Test
    void testAddExpense_defaults() {
        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Coffee");
        req.setAmount(4.5);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ExpenseResponse response = expenseService.addExpense(req);

        assertNotNull(response.getId());
        assertEquals("Coffee", response.getTitle());
        assertEquals(4.5, response.getAmount());
        assertEquals(TransactionType.EXPENSE, response.getType());
        assertEquals(LocalDate.now(), response.getTransactionDate());
    }

    @Test
    void testAddExpense_withValidGlobalCategory() {
        UUID categoryId = UUID.randomUUID();
        Category globalCategory = new Category();
        globalCategory.setId(categoryId);
        globalCategory.setName("Food");
        globalCategory.setUser(null); // global

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(globalCategory));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Lunch");
        req.setAmount(15.0);
        req.setCategoryId(categoryId);

        ExpenseResponse response = expenseService.addExpense(req);

        assertNotNull(response.getId());
        assertEquals("Lunch", response.getTitle());
        assertEquals(15.0, response.getAmount());
        assertNotNull(response.getCategory());
        assertEquals("Food", response.getCategory().getName());
        assertEquals(categoryId, response.getCategory().getId());
    }

    @Test
    void testAddExpense_withValidUserOwnedCategory() {
        UUID categoryId = UUID.randomUUID();
        Category userCategory = new Category();
        userCategory.setId(categoryId);
        userCategory.setName("Custom Fun");
        userCategory.setUser(testUser); // owned by the test user

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(userCategory));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Cinema");
        req.setAmount(25.0);
        req.setCategoryId(categoryId);

        ExpenseResponse response = expenseService.addExpense(req);

        assertNotNull(response.getId());
        assertEquals("Cinema", response.getTitle());
        assertEquals(25.0, response.getAmount());
        assertNotNull(response.getCategory());
        assertEquals("Custom Fun", response.getCategory().getName());
    }

    @Test
    void testAddExpense_recurringCalculation() {
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        // Test with DAILY
        ExpenseRequest dailyReq = new ExpenseRequest();
        dailyReq.setTitle("Daily news");
        dailyReq.setAmount(1.0);
        dailyReq.setIsRecurring(true);
        dailyReq.setRecurrence(RecurrencePeriod.DAILY);
        dailyReq.setTransactionDate(LocalDate.of(2026, 7, 1));

        ExpenseResponse dailyRes = expenseService.addExpense(dailyReq);
        assertEquals(LocalDate.of(2026, 7, 2), dailyRes.getNextExecutionDate());

        // Test with WEEKLY
        ExpenseRequest weeklyReq = new ExpenseRequest();
        weeklyReq.setTitle("Weekly gym");
        weeklyReq.setAmount(15.0);
        weeklyReq.setIsRecurring(true);
        weeklyReq.setRecurrence(RecurrencePeriod.WEEKLY);
        weeklyReq.setTransactionDate(LocalDate.of(2026, 7, 1));

        ExpenseResponse weeklyRes = expenseService.addExpense(weeklyReq);
        assertEquals(LocalDate.of(2026, 7, 8), weeklyRes.getNextExecutionDate());

        // Test with MONTHLY
        ExpenseRequest monthlyReq = new ExpenseRequest();
        monthlyReq.setTitle("Monthly rent");
        monthlyReq.setAmount(1200.0);
        monthlyReq.setIsRecurring(true);
        monthlyReq.setRecurrence(RecurrencePeriod.MONTHLY);
        monthlyReq.setTransactionDate(LocalDate.of(2026, 7, 1));

        ExpenseResponse monthlyRes = expenseService.addExpense(monthlyReq);
        assertEquals(LocalDate.of(2026, 8, 1), monthlyRes.getNextExecutionDate());

        // Test with YEARLY
        ExpenseRequest yearlyReq = new ExpenseRequest();
        yearlyReq.setTitle("Yearly sub");
        yearlyReq.setAmount(120.0);
        yearlyReq.setIsRecurring(true);
        yearlyReq.setRecurrence(RecurrencePeriod.YEARLY);
        yearlyReq.setTransactionDate(LocalDate.of(2026, 7, 1));

        ExpenseResponse yearlyRes = expenseService.addExpense(yearlyReq);
        assertEquals(LocalDate.of(2027, 7, 1), yearlyRes.getNextExecutionDate());
    }

    @Test
    void testAddExpense_validationErrors() {
        // req is null
        ApiRequestException exNull = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(null);
        });
        assertEquals("Title and amount are required", exNull.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatus());

        // title is null
        ExpenseRequest reqNullTitle = new ExpenseRequest();
        reqNullTitle.setAmount(10.0);
        ApiRequestException exNullTitle = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(reqNullTitle);
        });
        assertEquals("Title and amount are required", exNullTitle.getMessage());

        // title is empty
        ExpenseRequest reqEmptyTitle = new ExpenseRequest();
        reqEmptyTitle.setTitle("   ");
        reqEmptyTitle.setAmount(10.0);
        ApiRequestException exEmptyTitle = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(reqEmptyTitle);
        });
        assertEquals("Title and amount are required", exEmptyTitle.getMessage());

        // amount is null
        ExpenseRequest reqNullAmount = new ExpenseRequest();
        reqNullAmount.setTitle("Water");
        ApiRequestException exNullAmount = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(reqNullAmount);
        });
        assertEquals("Title and amount are required", exNullAmount.getMessage());
    }

    @Test
    void testAddExpense_unauthenticatedRequest() {
        // Clear security context
        SecurityContextHolder.clearContext();

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Coffee");
        req.setAmount(5.0);

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(req);
        });
        assertEquals("Unauthenticated request", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void testAddExpense_unauthenticatedNullPrincipal() {
        // Security context is present but authentication principal is null
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(null, null, List.of())
        );

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Coffee");
        req.setAmount(5.0);

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(req);
        });
        assertEquals("Unauthenticated request", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void testAddExpense_userNotFound() {
        // UserRepository returns empty for the current authenticated user id
        UUID nonExistentUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(nonExistentUserId.toString(), null, List.of())
        );
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Coffee");
        req.setAmount(5.0);

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(req);
        });
        assertEquals("User not found", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void testAddExpense_categoryNotFound() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Coffee");
        req.setAmount(5.0);
        req.setCategoryId(categoryId);

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(req);
        });
        assertEquals("Category not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testAddExpense_accessDeniedToCategory() {
        UUID categoryId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("other@example.com");

        Category otherUserCategory = new Category();
        otherUserCategory.setId(categoryId);
        otherUserCategory.setName("Secret Category");
        otherUserCategory.setUser(otherUser); // owned by someone else

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(otherUserCategory));

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Coffee");
        req.setAmount(5.0);
        req.setCategoryId(categoryId);

        ApiRequestException ex = assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(req);
        });
        assertEquals("Access denied to the specified category", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void testUpdateExpense_typeAndDate() {
        UUID expenseId = UUID.randomUUID();
        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setTitle("Initial Title");
        existing.setAmount(100.0);
        existing.setType(TransactionType.EXPENSE);
        existing.setTransactionDate(LocalDate.of(2026, 6, 1));
        existing.setUser(testUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseRequest updateReq = new ExpenseRequest();
        updateReq.setType(TransactionType.CREDIT);
        updateReq.setTransactionDate(LocalDate.of(2026, 6, 15));

        ExpenseResponse response = expenseService.updateExpense(updateReq, expenseId);

        assertEquals("Initial Title", response.getTitle());
        assertEquals(100.0, response.getAmount());
        assertEquals(TransactionType.CREDIT, response.getType());
        assertEquals(LocalDate.of(2026, 6, 15), response.getTransactionDate());
    }

    @Test
    void testGetExpenses_returnsCorrectFields() {
        Expense e1 = new Expense();
        e1.setId(UUID.randomUUID());
        e1.setTitle("Rent");
        e1.setAmount(1500.0);
        e1.setType(TransactionType.EXPENSE);
        e1.setTransactionDate(LocalDate.of(2026, 7, 1));
        e1.setUser(testUser);

        Expense e2 = new Expense();
        e2.setId(UUID.randomUUID());
        e2.setTitle("Freelance");
        e2.setAmount(1200.0);
        e2.setType(TransactionType.CREDIT);
        e2.setTransactionDate(LocalDate.of(2026, 7, 5));
        e2.setUser(testUser);

        when(expenseRepository.findByUserId(userId)).thenReturn(List.of(e1, e2));

        List<ExpenseResponse> responseList = expenseService.getExpenses();

        assertEquals(2, responseList.size());

        ExpenseResponse r1 = responseList.get(0);
        assertEquals("Rent", r1.getTitle());
        assertEquals(TransactionType.EXPENSE, r1.getType());
        assertEquals(LocalDate.of(2026, 7, 1), r1.getTransactionDate());

        ExpenseResponse r2 = responseList.get(1);
        assertEquals("Freelance", r2.getTitle());
        assertEquals(TransactionType.CREDIT, r2.getType());
        assertEquals(LocalDate.of(2026, 7, 5), r2.getTransactionDate());
    }
}
