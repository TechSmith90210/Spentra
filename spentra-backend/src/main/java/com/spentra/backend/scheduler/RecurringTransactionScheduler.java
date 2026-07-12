package com.spentra.backend.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.service.ExpenseService;

import lombok.RequiredArgsConstructor;

/**
 * Scheduler component responsible for automating recurring transactions.
 * Periodically searches for active recurring templates that are due and spawns their transactional instances.
 */
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;

    /**
     * Scheduled task to process recurring transactions.
     * Runs daily at midnight to find and generate instances for due recurring transactions.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processRecurringTransactions() {
        log.info("Starting processing of recurring transactions...");
        LocalDate today = LocalDate.now();

        // 1. Fetch recurring transaction templates whose execution date has arrived or passed
        List<Expense> dueTemplates = expenseRepository.findByIsRecurringTrueAndNextExecutionDateLessThanEqual(today);

        log.info("Found {} recurring transaction templates due for execution.", dueTemplates.size());

        // 2. Loop and generate instances, advancing the next execution date
        for (Expense template : dueTemplates) {
            try {
                // Catch-up logic in case the server was down or start date was set in the past
                while (template.getNextExecutionDate() != null && !template.getNextExecutionDate().isAfter(today)) {
                    LocalDate executionDate = template.getNextExecutionDate();

                    // Generate a new one-off transaction instance representing the payment occurrence
                    Expense occurrence = new Expense();
                    occurrence.setTitle(template.getTitle());
                    occurrence.setAmount(template.getAmount());
                    occurrence.setUser(template.getUser());
                    occurrence.setCategory(template.getCategory());
                    occurrence.setType(template.getType());
                    occurrence.setTransactionDate(executionDate);
                    occurrence.setIsRecurring(false);
                    occurrence.setRecurrence(RecurrencePeriod.NONE);
                    occurrence.setNextExecutionDate(null);

                    expenseRepository.save(occurrence);
                    log.info("Generated transaction instance '{}' for date {}.", template.getTitle(), executionDate);

                    // Advance the template's next execution date based on recurrence period
                    LocalDate nextDate = expenseService.calculateNextExecutionDate(executionDate, template.getRecurrence());
                    template.setNextExecutionDate(nextDate);

                    // If recurrence is invalid or NONE, disable future executions
                    if (nextDate == null) {
                        log.warn("Next execution date resolved to null for template ID: {}. Disabling recurrence.", template.getId());
                        template.setIsRecurring(false);
                    }
                }

                expenseRepository.save(template);
            } catch (Exception e) {
                log.error("Failed to process recurring transaction template with ID: {}", template.getId(), e);
            }
        }
        log.info("Finished processing of recurring transactions.");
    }
}
