package com.spentra.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.spentra.backend.model.dto.ai.AiSummaryResponse;
import com.spentra.backend.model.entity.AiSummary;
import com.spentra.backend.model.entity.Category;
import com.spentra.backend.model.entity.Expense;
import com.spentra.backend.model.entity.User;
import com.spentra.backend.model.enums.TransactionType;
import com.spentra.backend.repository.AiSummaryRepository;
import com.spentra.backend.repository.CategoryRepository;
import com.spentra.backend.repository.ExpenseRepository;

@ExtendWith(MockitoExtension.class)
class AiInsightsServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private AiSummaryRepository aiSummaryRepository;
    @Mock private UserService userService;
    @Mock private RestTemplate restTemplate;

    private GeminiService service;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new GeminiService(categoryRepository, expenseRepository, aiSummaryRepository, userService, restTemplate);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("insights@example.com");
        when(userService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getOrGenerateInsights_returnsCachedSummary() {
        YearMonth month = YearMonth.of(2026, 8);
        AiSummary cached = new AiSummary();
        cached.setId(UUID.randomUUID());
        cached.setUser(user);
        cached.setYearMonth(month);
        cached.setSummaryText("Cached summary");
        cached.setTotalSpent(123.0);
        cached.setTopCategory("Food");
        cached.setGeneratedAt(LocalDateTime.of(2026, 8, 15, 12, 0));

        when(aiSummaryRepository.findByUserIdAndYearMonth(userId, month)).thenReturn(Optional.of(cached));

        AiSummaryResponse response = service.getOrGenerateInsights("2026-08", "INR");

        assertEquals("Cached summary", response.getSummaryText());
        assertEquals(123.0, response.getTotalSpent());
        verify(expenseRepository, org.mockito.Mockito.never()).findByUserId(any());
    }

    @Test
    void getOrGenerateInsights_noTransactions_returnsEmptyState() {
        YearMonth month = YearMonth.of(2026, 8);
        when(aiSummaryRepository.findByUserIdAndYearMonth(userId, month)).thenReturn(Optional.empty());
        when(expenseRepository.findByUserId(userId)).thenReturn(List.of());

        AiSummaryResponse response = service.getOrGenerateInsights("2026-08", "INR");

        assertNotNull(response);
        assertEquals("2026-08", response.getYearMonth());
        assertEquals("Not enough data yet. Add some transactions and check back!", response.getSummaryText());
        assertEquals(0.0, response.getTotalSpent());
    }

    @Test
    void refreshInsights_deletesAndRegenerates() {
        YearMonth month = YearMonth.of(2026, 8);

        Category food = new Category();
        food.setId(UUID.randomUUID());
        food.setName("Food");

        Expense e1 = new Expense();
        e1.setAmount(40.0);
        e1.setType(TransactionType.EXPENSE);
        e1.setTransactionDate(LocalDate.of(2026, 8, 10));
        e1.setCategory(food);

        Expense e2 = new Expense();
        e2.setAmount(60.0);
        e2.setType(TransactionType.EXPENSE);
        e2.setTransactionDate(LocalDate.of(2026, 8, 11));
        e2.setCategory(food);

        when(expenseRepository.findByUserId(userId)).thenReturn(List.of(e1, e2));

        AiSummaryResponse response = service.refreshInsights("2026-08", "USD");

        assertEquals(100.0, response.getTotalSpent());
        assertEquals("Food", response.getTopCategory());
        verify(aiSummaryRepository).deleteByUserIdAndYearMonth(userId, month);
        verify(aiSummaryRepository).save(any(AiSummary.class));
    }
}
