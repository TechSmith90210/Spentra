# Spentra MVP Checklist & Learning Guide

This document lists the remaining tasks to complete your Expense Tracker API MVP, along with learning-oriented Spring Boot code snippets.

---

## 1. User Isolation & Authentication Binding

Ensure users can only view and modify their own expenses and categories.

### 📝 Tasks
- [ ] Retrieve the authenticated user's ID from Spring Security's context.
- [ ] Bind categories and expenses to the authenticated user on creation.
- [ ] Update repository queries to filter by `userId` (or allow global categories where `user_id` is null).
- [ ] Block updates and deletions of records that do not belong to the active user.

### 💡 Spring Boot Code Snippets

#### Retrieving User ID from Security Context
When a request passes through `JwtSecurityFilter`, it saves the user ID (UUID string) as the security principal. Retrieve it in your service classes:
```java
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

// 1. Get the principal set by JwtSecurityFilter
String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
UUID currentUserId = UUID.fromString(userIdStr);

// 2. Fetch the User entity
User currentUser = userRepository.findById(currentUserId)
    .orElseThrow(() -> new RuntimeException("User not found"));
```

#### Querying User-Specific Data in Repositories
Update your repository interfaces to include queries that filter by user ID:
```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    // Spring Boot automatically implements this query from the method name!
    List<Expense> findByUserId(UUID userId);
}
```
For categories (user-specific + global categories):
```java
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    // Fetch categories created by the user OR global ones (where user_id is null)
    List<Category> findByUserIdOrUserIsNull(UUID userId);
}
```

---

## 2. Credits vs. Expenses Differentiation

Add support for incoming credits (salary, transfers) and outgoing expenses, along with dates.

### 📝 Tasks
- [x] Create a `TransactionType` enum (`EXPENSE`, `CREDIT`).
- [x] Add the `type` and `transactionDate` fields to [Expense](file:///Users/salman/development/Spentra/spentra-backend/src/main/java/com/spentra/backend/model/entity/Expense.java) (or refactor entity name to `Transaction`).
- [x] Expose fields in request/response DTOs.

### 💡 Spring Boot Code Snippets

#### Creating the Enum
```java
package com.spentra.backend.model.enums;

public enum TransactionType {
    EXPENSE,
    CREDIT
}
```

#### Mapping the Enum and Date in the Entity
```java
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

@Enumerated(EnumType.STRING)
private TransactionType type; // Saves as 'EXPENSE' or 'CREDIT' in the database

private LocalDate transactionDate; // Use java.time.LocalDate for database dates
```

---

## 3. Monthly Budgets

Allow users to set limit thresholds for categories or global spending on a monthly basis.

### 📝 Tasks
- [ ] Create the `Budget` database entity.
- [ ] Add `POST /api/budgets` to set or update a monthly budget.
- [ ] Add `GET /api/budgets/summary` to fetch actual spending vs. budgeted limits.

### 💡 Spring Boot Code Snippets

#### Budget Entity Structure
```java
import jakarta.persistence.*;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id") // Nullable for global/overall budget
    private Category category;

    private Double amountLimit;

    // Convert YearMonth to a column (e.g. integer or string date format)
    private YearMonth budgetMonth; 
}
```

#### Calculating Monthly Spending (Repository Query)
To compare actual expenses against the budget, compute total expenses for a specific month:
```java
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
           "AND e.type = 'EXPENSE' " +
           "AND e.transactionDate BETWEEN :startDate AND :endDate")
    Double calculateTotalExpenses(UUID userId, LocalDate startDate, LocalDate endDate);
}
```

---

## 4. Recurring Transactions (Subscriptions & Recurring Credits)

Automate transactions that occur regularly (e.g. Netflix subscription, monthly salary).

### 📝 Tasks
- [ ] Create a `RecurrencePeriod` enum (`NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`).
- [ ] Add `recurrence` and `isRecurring` flags to your transactions.
- [ ] Write a scheduled background runner in Spring Boot.

### 💡 Spring Boot Code Snippets

#### Enabling Scheduled Tasks
Add `@EnableScheduling` to your main application or a configuration class:
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Enables Spring Boot's internal scheduler engine
}
```

#### Scheduled Background Service
Use `@Scheduled` to periodically check and create recurring transactions:
```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    @Scheduled(cron = "0 0 0 * * *") // Runs everyday at midnight
    public void generateRecurringTransactions() {
        System.out.println("Checking for due recurring transactions...");
        // 1. Query all transactions where isRecurring = true
        // 2. If the next execution date has arrived/passed:
        //    a. Generate a new transaction record
        //    b. Update next payment date on the recurring template
    }
}
```
*Tip: Standard cron syntax `"0 0 0 * * *"` represents `Second Minute Hour Day Month Day-of-week`.*
