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
        // Mock getCurrentUser() throwing Exception for user not found
        when(userService.getCurrentUser()).thenThrow(new ApiRequestException("User not found", HttpStatus.UNAUTHORIZED));

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
        existing.setIsRecurring(false);
        existing.setRecurrence(RecurrencePeriod.NONE);
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
    void testUpdateExpense_notFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(expenseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Updated Title");

        ApiRequestException ex = assertThrows(
                ApiRequestException.class,
                () -> expenseService.updateExpense(req, nonExistentId)
        );

        assertEquals("Transaction not found with the specified ID", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testUpdateExpense_accessDenied() {
        UUID expenseId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setUser(otherUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("Unauthorized Edit");

        ApiRequestException ex = assertThrows(
                ApiRequestException.class,
                () -> expenseService.updateExpense(req, expenseId)
        );

        assertEquals("Access denied: You do not own this transaction", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void testUpdateExpense_partialFieldsUpdate() {
        UUID expenseId = UUID.randomUUID();
        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setTitle("Old Title");
        existing.setAmount(50.0);
        existing.setType(TransactionType.EXPENSE);
        existing.setTransactionDate(LocalDate.of(2026, 1, 1));
        existing.setIsRecurring(false);
        existing.setRecurrence(RecurrencePeriod.NONE);
        existing.setUser(testUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseRequest req = new ExpenseRequest();
        req.setTitle("New Title");
        req.setAmount(75.0);

        ExpenseResponse response = expenseService.updateExpense(req, expenseId);

        assertEquals("New Title", response.getTitle());
        assertEquals(75.0, response.getAmount());
        assertEquals(TransactionType.EXPENSE, response.getType());
        assertEquals(LocalDate.of(2026, 1, 1), response.getTransactionDate());
        assertFalse(response.getIsRecurring());
        assertEquals(RecurrencePeriod.NONE, response.getRecurrence());
        assertNull(response.getNextExecutionDate());
    }

    @Test
    void testUpdateExpense_withCategory_globalAndUserOwned() {
        UUID expenseId = UUID.randomUUID();
        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setTitle("Groceries");
        existing.setAmount(150.0);
        existing.setType(TransactionType.EXPENSE);
        existing.setTransactionDate(LocalDate.of(2026, 3, 1));
        existing.setIsRecurring(false);
        existing.setRecurrence(RecurrencePeriod.NONE);
        existing.setUser(testUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Global category
        UUID globalCatId = UUID.randomUUID();
        Category globalCategory = new Category();
        globalCategory.setId(globalCatId);
        globalCategory.setName("Food");
        globalCategory.setUser(null);

        when(categoryRepository.findById(globalCatId)).thenReturn(Optional.of(globalCategory));

        ExpenseRequest req1 = new ExpenseRequest();
        req1.setCategoryId(globalCatId);

        ExpenseResponse res1 = expenseService.updateExpense(req1, expenseId);
        assertNotNull(res1.getCategory());
        assertEquals(globalCatId, res1.getCategory().getId());
        assertEquals("Food", res1.getCategory().getName());

        // User-owned category
        UUID userCatId = UUID.randomUUID();
        Category userCategory = new Category();
        userCategory.setId(userCatId);
        userCategory.setName("Personal Food");
        userCategory.setUser(testUser);

        when(categoryRepository.findById(userCatId)).thenReturn(Optional.of(userCategory));

        ExpenseRequest req2 = new ExpenseRequest();
        req2.setCategoryId(userCatId);

        ExpenseResponse res2 = expenseService.updateExpense(req2, expenseId);
        assertNotNull(res2.getCategory());
        assertEquals(userCatId, res2.getCategory().getId());
        assertEquals("Personal Food", res2.getCategory().getName());
    }

    @Test
    void testUpdateExpense_categoryAccessDenied() {
        UUID expenseId = UUID.randomUUID();
        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setUser(testUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));

        UUID otherCatId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        Category otherUserCat = new Category();
        otherUserCat.setId(otherCatId);
        otherUserCat.setName("Private Category");
        otherUserCat.setUser(otherUser);

        when(categoryRepository.findById(otherCatId)).thenReturn(Optional.of(otherUserCat));

        ExpenseRequest req = new ExpenseRequest();
        req.setCategoryId(otherCatId);

        ApiRequestException ex = assertThrows(
                ApiRequestException.class,
                () -> expenseService.updateExpense(req, expenseId)
        );

        assertEquals("Access denied to the specified category", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void testUpdateExpense_categoryNotFound() {
        UUID expenseId = UUID.randomUUID();
        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setUser(testUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));

        UUID missingCatId = UUID.randomUUID();
        when(categoryRepository.findById(missingCatId)).thenReturn(Optional.empty());

        ExpenseRequest req = new ExpenseRequest();
        req.setCategoryId(missingCatId);

        ApiRequestException ex = assertThrows(
                ApiRequestException.class,
                () -> expenseService.updateExpense(req, expenseId)
        );

        assertEquals("Category not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testUpdateExpense_recurrenceRecalculations() {
        UUID expenseId = UUID.randomUUID();
        Expense existing = new Expense();
        existing.setId(expenseId);
        existing.setTitle("Subscription");
        existing.setAmount(10.0);
        existing.setTransactionDate(LocalDate.of(2026, 5, 10));
        existing.setIsRecurring(false);
        existing.setRecurrence(RecurrencePeriod.NONE);
        existing.setNextExecutionDate(null);
        existing.setUser(testUser);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Enable recurrence MONTHLY
        ExpenseRequest reqMonthly = new ExpenseRequest();
        reqMonthly.setIsRecurring(true);
        reqMonthly.setRecurrence(RecurrencePeriod.MONTHLY);

        ExpenseResponse resMonthly = expenseService.updateExpense(reqMonthly, expenseId);
        assertTrue(resMonthly.getIsRecurring());
        assertEquals(RecurrencePeriod.MONTHLY, resMonthly.getRecurrence());
        assertEquals(LocalDate.of(2026, 6, 10), resMonthly.getNextExecutionDate());

        // Change recurrence to DAILY
        ExpenseRequest reqDaily = new ExpenseRequest();
        reqDaily.setRecurrence(RecurrencePeriod.DAILY);

        ExpenseResponse resDaily = expenseService.updateExpense(reqDaily, expenseId);
        assertEquals(LocalDate.of(2026, 5, 11), resDaily.getNextExecutionDate());

        // Change recurrence to WEEKLY
        ExpenseRequest reqWeekly = new ExpenseRequest();
        reqWeekly.setRecurrence(RecurrencePeriod.WEEKLY);

        ExpenseResponse resWeekly = expenseService.updateExpense(reqWeekly, expenseId);
        assertEquals(LocalDate.of(2026, 5, 17), resWeekly.getNextExecutionDate());

        // Change recurrence to YEARLY
        ExpenseRequest reqYearly = new ExpenseRequest();
        reqYearly.setRecurrence(RecurrencePeriod.YEARLY);

        ExpenseResponse resYearly = expenseService.updateExpense(reqYearly, expenseId);
        assertEquals(LocalDate.of(2027, 5, 10), resYearly.getNextExecutionDate());

        // Disable recurrence by setting isRecurring = false
        ExpenseRequest reqDisable = new ExpenseRequest();
        reqDisable.setIsRecurring(false);

        ExpenseResponse resDisabled = expenseService.updateExpense(reqDisable, expenseId);
        assertFalse(resDisabled.getIsRecurring());
        assertNull(resDisabled.getNextExecutionDate());

        // Set recurrence period to NONE while isRecurring is true
        ExpenseRequest reqNone = new ExpenseRequest();
        reqNone.setIsRecurring(true);
        reqNone.setRecurrence(RecurrencePeriod.NONE);

        ExpenseResponse resNone = expenseService.updateExpense(reqNone, expenseId);
        assertTrue(resNone.getIsRecurring());
        assertEquals(RecurrencePeriod.NONE, resNone.getRecurrence());
        assertNull(resNone.getNextExecutionDate());
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
