package com.spentra.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spentra.backend.model.dto.budget.BudgetRequest;
import com.spentra.backend.model.dto.budget.BudgetResponse;
import com.spentra.backend.model.dto.budget.BudgetSummaryResponse;
import com.spentra.backend.service.BudgetService;

/**
 * Controller class handling HTTP requests for monthly budgets.
 * Exposes endpoints to set monthly limits and fetch spending comparisons.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    /**
     * Constructor for dependency injection.
     *
     * @param service the budget service
     */
    public BudgetController(BudgetService service) {
        this.service = service;
    }

    /**
     * Sets or updates a monthly budget limit for a specific category or globally.
     *
     * @param req budget request payload containing limit, category, and month
     * @return BudgetResponse details of the set budget
     */
    @PostMapping
    public BudgetResponse createOrUpdateBudget(@RequestBody BudgetRequest req) {
        return service.createOrUpdateBudget(req);
    }

    /**
     * Retrieves the budget tracking summaries comparing limit against actual spending.
     *
     * @param month optional month parameter in "YYYY-MM" format. Defaults to current month if omitted.
     * @return List of BudgetSummaryResponse representing user budgets
     */
    @GetMapping("/summary")
    public List<BudgetSummaryResponse> getBudgetSummary(@RequestParam(required = false) String month) {
        return service.getBudgetSummary(month);
    }
}
