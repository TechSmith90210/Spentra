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
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private ExpenseService expenseService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        userRepository = mock(UserRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        expenseService = new ExpenseService(expenseRepository, userRepository, categoryRepository);

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
