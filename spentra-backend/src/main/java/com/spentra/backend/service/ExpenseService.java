package com.spentra.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.spentra.backend.model.dto.ExpenseRequest;
import com.spentra.backend.model.dto.ExpenseResponse;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository repo;

    public ExpenseResponse addExpense(ExpenseRequest req) {
        Expense expense = new Expense();

        expense.setTitle(req.getTitle());
        expense.setAmount(req.getAmount());

        Expense saved = repo.save(expense);

        return new ExpenseResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getAmount());
    }

    public List<ExpenseResponse> getExpenses() {
        List<Expense> expenses = repo.findAll();

        List<ExpenseResponse> responses = expenses.stream().map(
                expense -> new ExpenseResponse(expense.getId(), expense.getTitle(), expense.getAmount()))
                .collect(Collectors.toList());

        return responses;
    }

    public ExpenseResponse updateExpense(ExpenseRequest expenseRequest, UUID id) {
        Expense existingExpense = repo.findById(id).orElseThrow(
                () -> new RuntimeException(
                        "cannot find an expense with this id, check details and try again"));

        if (expenseRequest.getTitle() != null) {
            existingExpense.setTitle(expenseRequest.getTitle());
        }

        if (expenseRequest.getAmount() != null) {
            existingExpense.setAmount(expenseRequest.getAmount());
        }

        Expense updatedExpense = repo.save(existingExpense);

        return new ExpenseResponse(updatedExpense.getId(), updatedExpense.getTitle(), updatedExpense.getAmount());

    }

    public void deleteExpense(UUID id) {
        Expense expenseToDelete = repo.findById(id).orElseThrow(
                () -> new RuntimeException(
                        "cannot find an expense with this id, check details and try again"));

        repo.delete(expenseToDelete);
    }

}
