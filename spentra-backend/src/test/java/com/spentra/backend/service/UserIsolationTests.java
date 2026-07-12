package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.spentra.backend.exception.ApiRequestException;
import com.spentra.backend.model.dto.category.CategoryRequest;
import com.spentra.backend.model.dto.category.CategoryResponse;
import com.spentra.backend.model.dto.expense.ExpenseRequest;
import com.spentra.backend.model.dto.expense.ExpenseResponse;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.AuthProvider;
import com.spentra.backend.model.enums.RecurrencePeriod;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;
import com.spentra.backend.repository.UserRepository;

@SpringBootTest
public class UserIsolationTests {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private User user1;
    private User user2;

    @BeforeEach
    public void setup() {
        // Clear repositories to ensure clean state
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create and save test users
        user1 = new User();
        user1.setName("Alice");
        user1.setEmail("alice@test.com");
        user1.setProvider(AuthProvider.LOCAL);
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("Bob");
        user2.setEmail("bob@test.com");
        user2.setProvider(AuthProvider.LOCAL);
        user2 = userRepository.save(user2);
    }

    @AfterEach
    public void cleanup() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testCategoryUserIsolation() {
        // 1. Authenticate as Alice (User 1)
        authenticateAs(user1);

        CategoryRequest req = new CategoryRequest();
        req.setName("Alice's Food");
        CategoryResponse aliceCategory = categoryService.addCategory(req);

        // Verify category is bound to Alice
        assertNotNull(aliceCategory.getId());
        Category savedCat = categoryRepository.findById(aliceCategory.getId()).orElse(null);
        assertNotNull(savedCat);
        assertEquals(user1.getId(), savedCat.getUser().getId());

        // 2. Fetch categories as Alice - should see Alice's category
        List<CategoryResponse> aliceCategories = categoryService.getCategories();
        assertTrue(aliceCategories.stream().anyMatch(c -> c.getName().equals("Alice's Food")));

        // 3. Create a global category directly in repo
        Category globalCat = new Category();
        globalCat.setName("Global Utilities");
        globalCat.setUser(null);
        globalCat = categoryRepository.save(globalCat);

        // Alice should see both her category and the global category
        aliceCategories = categoryService.getCategories();
        assertEquals(2, aliceCategories.size());
        assertTrue(aliceCategories.stream().anyMatch(c -> c.getName().equals("Global Utilities")));

        // 4. Authenticate as Bob (User 2)
        authenticateAs(user2);

        // Bob should only see the global category, not Alice's category
        List<CategoryResponse> bobCategories = categoryService.getCategories();
        assertEquals(1, bobCategories.size());
        assertEquals("Global Utilities", bobCategories.get(0).getName());

        // Bob tries to update Alice's category - should fail
        CategoryRequest updateReq = new CategoryRequest();
        updateReq.setName("Bob's Food");
        UUID aliceCatId = aliceCategory.getId();
        assertThrows(ApiRequestException.class, () -> {
            categoryService.updateCategory(updateReq, aliceCatId);
        });

        // Bob tries to delete Alice's category - should fail
        assertThrows(ApiRequestException.class, () -> {
            categoryService.deleteCategory(aliceCatId);
        });

        // Bob tries to update global category - should fail
        CategoryRequest updateGlobalReq = new CategoryRequest();
        updateGlobalReq.setName("Modified Global");
        UUID globalCatId = globalCat.getId();
        assertThrows(ApiRequestException.class, () -> {
            categoryService.updateCategory(updateGlobalReq, globalCatId);
        });

        // Bob tries to delete global category - should fail
        assertThrows(ApiRequestException.class, () -> {
            categoryService.deleteCategory(globalCatId);
        });
    }

    @Test
    public void testExpenseUserIsolation() {
        // 1. Create a global category
        Category globalCat = new Category();
        globalCat.setName("Global Utilities");
        globalCat.setUser(null);
        globalCat = categoryRepository.save(globalCat);

        // Create Alice's private category
        authenticateAs(user1);
        CategoryRequest catReq = new CategoryRequest();
        catReq.setName("Alice's Travel");
        CategoryResponse aliceCat = categoryService.addCategory(catReq);

        // 2. Add Expense as Alice
        ExpenseRequest expenseReq = new ExpenseRequest();
        expenseReq.setTitle("Flight to Paris");
        expenseReq.setAmount(500.0);
        expenseReq.setCategoryId(aliceCat.getId());
        expenseReq.setType(TransactionType.EXPENSE);
        expenseReq.setTransactionDate(LocalDate.now());
        expenseReq.setIsRecurring(false);
        expenseReq.setRecurrence(RecurrencePeriod.NONE);

        ExpenseResponse aliceExpense = expenseService.addExpense(expenseReq);
        assertNotNull(aliceExpense.getId());

        // Verify stored expense is bound to Alice
        Expense savedExpense = expenseRepository.findById(aliceExpense.getId()).orElse(null);
        assertNotNull(savedExpense);
        assertEquals(user1.getId(), savedExpense.getUser().getId());

        // 3. Fetch expenses as Alice
        List<ExpenseResponse> aliceExpenses = expenseService.getExpenses();
        assertEquals(1, aliceExpenses.size());
        assertEquals("Flight to Paris", aliceExpenses.get(0).getTitle());

        // 4. Authenticate as Bob
        authenticateAs(user2);

        // Bob fetches expenses - should be empty
        List<ExpenseResponse> bobExpenses = expenseService.getExpenses();
        assertTrue(bobExpenses.isEmpty());

        // Bob tries to add expense under Alice's private category - should fail with Forbidden
        ExpenseRequest bobExpenseReq = new ExpenseRequest();
        bobExpenseReq.setTitle("Bob's Flight");
        bobExpenseReq.setAmount(100.0);
        bobExpenseReq.setCategoryId(aliceCat.getId());
        assertThrows(ApiRequestException.class, () -> {
            expenseService.addExpense(bobExpenseReq);
        });

        // Bob tries to update Alice's expense - should fail
        ExpenseRequest updateReq = new ExpenseRequest();
        updateReq.setTitle("Bob's Hijacked Flight");
        UUID aliceExpenseId = aliceExpense.getId();
        assertThrows(ApiRequestException.class, () -> {
            expenseService.updateExpense(updateReq, aliceExpenseId);
        });

        // Bob tries to delete Alice's expense - should fail
        assertThrows(ApiRequestException.class, () -> {
            expenseService.deleteExpense(aliceExpenseId);
        });
    }
}
