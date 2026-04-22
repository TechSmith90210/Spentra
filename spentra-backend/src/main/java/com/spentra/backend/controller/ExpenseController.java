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

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    // di added
    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExpenseResponse> getExpenses() {
        return service.getExpenses();
    }

    @PostMapping
    public ExpenseResponse addExpense(@RequestBody ExpenseRequest exp) {
        // pass the Request Body object to service layer
        return service.addExpense(exp);
    }

    // this is called when user wants to update an expense
    @PatchMapping("/{id}")
    public ExpenseResponse updateExpense(@RequestBody ExpenseRequest req, @PathVariable UUID id) {
        // pass the request to service layer
        return service.updateExpense(req, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        service.deleteExpense(id);
        // pass the request to service layer
        return ResponseEntity.noContent().build();
    }

}
