package com.spentra.backend.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.service.ExpenseService;

class RecurringTransactionSchedulerTest {

    private ExpenseRepository expenseRepository;
    private ExpenseService expenseService;
    private RecurringTransactionScheduler scheduler;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        expenseService = mock(ExpenseService.class);
        scheduler = new RecurringTransactionScheduler(expenseRepository, expenseService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessRecurringTransactions_NoTemplates() {
        when(expenseRepository.findByIsRecurringTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        scheduler.processRecurringTransactions();

        verify(expenseRepository, never()).saveAll(any(List.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessRecurringTransactions_SingleOccurrence() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        Category category = new Category();
        category.setId(UUID.randomUUID());

        Expense template = new Expense();
        template.setId(UUID.randomUUID());
        template.setTitle("Netflix");
        template.setAmount(15.99);
        template.setUser(user);
        template.setCategory(category);
        template.setType(TransactionType.EXPENSE);
        template.setIsRecurring(true);
        template.setRecurrence(RecurrencePeriod.MONTHLY);
        template.setNextExecutionDate(today);

        when(expenseRepository.findByIsRecurringTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(template));

        LocalDate nextDate = today.plusMonths(1);
        when(expenseService.calculateNextExecutionDate(today, RecurrencePeriod.MONTHLY)).thenReturn(nextDate);

        scheduler.processRecurringTransactions();

        ArgumentCaptor<List> expenseCaptor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository, times(2)).saveAll(expenseCaptor.capture());

        List<List> savedBatches = expenseCaptor.getAllValues();
        assertEquals(2, savedBatches.size());

        List<Expense> occurrences = savedBatches.get(0);
        assertEquals(1, occurrences.size());
        Expense occurrence = occurrences.get(0);
        assertEquals("Netflix", occurrence.getTitle());
        assertEquals(15.99, occurrence.getAmount());
        assertEquals(user, occurrence.getUser());
        assertEquals(category, occurrence.getCategory());
        assertEquals(TransactionType.EXPENSE, occurrence.getType());
        assertEquals(today, occurrence.getTransactionDate());
        assertFalse(occurrence.getIsRecurring());
        assertEquals(RecurrencePeriod.NONE, occurrence.getRecurrence());
        assertNull(occurrence.getNextExecutionDate());

        List<Expense> templates = savedBatches.get(1);
        assertEquals(1, templates.size());
        Expense updatedTemplate = templates.get(0);
        assertEquals(template.getId(), updatedTemplate.getId());
        assertEquals(nextDate, updatedTemplate.getNextExecutionDate());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessRecurringTransactions_CatchUpMultipleOccurrences() {
        LocalDate today = LocalDate.now();
        LocalDate twoWeeksAgo = today.minusWeeks(2);
        LocalDate oneWeekAgo = today.minusWeeks(1);

        User user = new User();
        user.setId(UUID.randomUUID());

        Expense template = new Expense();
        template.setId(UUID.randomUUID());
        template.setTitle("Weekly Gym");
        template.setAmount(10.00);
        template.setUser(user);
        template.setType(TransactionType.EXPENSE);
        template.setIsRecurring(true);
        template.setRecurrence(RecurrencePeriod.WEEKLY);
        template.setNextExecutionDate(twoWeeksAgo);

        when(expenseRepository.findByIsRecurringTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(template));

        when(expenseService.calculateNextExecutionDate(twoWeeksAgo, RecurrencePeriod.WEEKLY)).thenReturn(oneWeekAgo);
        when(expenseService.calculateNextExecutionDate(oneWeekAgo, RecurrencePeriod.WEEKLY)).thenReturn(today);
        when(expenseService.calculateNextExecutionDate(today, RecurrencePeriod.WEEKLY)).thenReturn(today.plusWeeks(1));

        scheduler.processRecurringTransactions();

        ArgumentCaptor<List> expenseCaptor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository, times(2)).saveAll(expenseCaptor.capture());

        List<List> savedBatches = expenseCaptor.getAllValues();
        assertEquals(2, savedBatches.size());

        List<Expense> occurrences = savedBatches.get(0);
        assertEquals(3, occurrences.size());
        assertEquals(twoWeeksAgo, occurrences.get(0).getTransactionDate());
        assertEquals(oneWeekAgo, occurrences.get(1).getTransactionDate());
        assertEquals(today, occurrences.get(2).getTransactionDate());

        for (int i = 0; i < 3; i++) {
            assertFalse(occurrences.get(i).getIsRecurring());
            assertEquals(RecurrencePeriod.NONE, occurrences.get(i).getRecurrence());
            assertNull(occurrences.get(i).getNextExecutionDate());
        }

        List<Expense> templates = savedBatches.get(1);
        assertEquals(1, templates.size());
        Expense updatedTemplate = templates.get(0);
        assertEquals(template.getId(), updatedTemplate.getId());
        assertEquals(today.plusWeeks(1), updatedTemplate.getNextExecutionDate());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessRecurringTransactions_ServiceExceptionHandling() {
        LocalDate today = LocalDate.now();
        Expense template = new Expense();
        template.setId(UUID.randomUUID());
        template.setTitle("Error Template");
        template.setIsRecurring(true);
        template.setRecurrence(RecurrencePeriod.DAILY);
        template.setNextExecutionDate(today);

        when(expenseRepository.findByIsRecurringTrueAndNextExecutionDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(template));

        when(expenseService.calculateNextExecutionDate(any(LocalDate.class), any(RecurrencePeriod.class)))
                .thenThrow(new RuntimeException("Service Error"));

        scheduler.processRecurringTransactions();

        verify(expenseRepository, never()).saveAll(any(List.class));
    }
}
