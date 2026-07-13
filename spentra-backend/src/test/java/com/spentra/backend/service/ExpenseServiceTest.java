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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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

        // Mock security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);

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
