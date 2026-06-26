package com.spentra.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spentra.backend.model.dto.expense.ExpenseRequest;
import com.spentra.backend.model.dto.expense.ExpenseResponse;
import com.spentra.backend.service.ExpenseService;

/**
 * Controller class handling HTTP requests for transaction (expense/credit) management.
 * Offers endpoints for adding, fetching, updating, and deleting transactions.
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    /**
     * Constructor for dependency injection.
     *
      * @param service the transaction service
     */
    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    /**
     * Retrieves all transactions belonging to the authenticated user.
     *
     * @return List of ExpenseResponse containing all user transactions
     */
    @GetMapping
    public List<ExpenseResponse> getExpenses() {
        return service.getExpenses();
    }

    /**
     * Creates a new transaction (expense/credit) for the authenticated user.
     *
     * @param exp the transaction creation request payload
     * @return ExpenseResponse representing the created transaction details
     */
    @PostMapping
    public ExpenseResponse addExpense(@RequestBody ExpenseRequest exp) {
        return service.addExpense(exp);
    }

    /**
     * Updates an existing transaction for the authenticated user.
     * Enforces that the transaction belongs to the requesting user.
     *
     * @param req transaction update payload
     * @param id ID of the transaction to update
     * @return ExpenseResponse representing the updated transaction details
     */
    @PatchMapping("/{id}")
    public ExpenseResponse updateExpense(@RequestBody ExpenseRequest req, @PathVariable UUID id) {
        return service.updateExpense(req, id);
    }

    /**
     * Deletes a transaction belonging to the authenticated user.
     * Enforces ownership validation before deletion.
     *
     * @param id ID of the transaction to delete
     * @return ResponseEntity with No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        service.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}

