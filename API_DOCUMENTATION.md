# Spentra API Documentation (Frontend Integration Guide)

This document contains descriptions, routes, payloads, responses, and error definitions for the Spentra Expense Tracker API to facilitate frontend development.

---

## Global Constraints & Configurations

### 1. Base URL
- Local Development: `http://localhost:8080` (or as configured on your dev server).

### 2. Authorization Header
All endpoints, except for the Authentication endpoints (`/api/auth/**`), are secured and require a JSON Web Token (JWT).
- **Header Key**: `Authorization`
- **Header Value**: `Bearer <your-jwt-token>`
- **Example**:
  ```http
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkMm...
  ```

### 3. Standard Error Format
When a request fails, the API returns a standard exception object with the following fields:
```json
{
  "message": "Specific error description",
  "statusCode": 400,
  "timestamp": "2026-06-27T02:15:00+05:30"
}
```

---

## 1. Authentication Endpoints

### SignUp (User Registration)
- **Method**: `POST`
- **Route**: `/api/auth/signUp`
- **Auth Required**: No

#### Request Body
- `email` (String, Required, Must be valid format)
- `password` (String, Required, Min 8 chars, must contain a letter and a number)
- `confirmPassword` (String, Required, Must match password)
- `name` (String, Optional)
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "confirmPassword": "Password123",
  "name": "Jane Doe"
}
```

#### Successful Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...",
  "name": "Jane Doe",
  "email": "user@example.com"
}
```

#### Error Responses
- **400 Bad Request**: Validation failed (e.g., passwords don't match, invalid email format).
- **409 Conflict**: User already exists with the specified email.

---

### Login
- **Method**: `POST`
- **Route**: `/api/auth/login`
- **Auth Required**: No

#### Request Body
- `email` (String, Required)
- `password` (String, Required)
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```

#### Successful Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...",
  "email": "user@example.com",
  "name": "Jane Doe"
}
```

#### Error Responses
- **404 Not Found**: User not registered with the specified email.
- **401 Unauthorized**: Invalid credentials.

---

## 2. Category Endpoints

### Get Categories
Retrieves all global/system categories plus any custom categories created by the authenticated user.
- **Method**: `GET`
- **Route**: `/api/categories`
- **Auth Required**: Yes

#### Successful Response (200 OK)
```json
[
  {
    "id": "e5c26b52-4467-4228-87e0-63c67586520f",
    "name": "Food & Dining"
  },
  {
    "id": "89b5c2a1-fa44-48f8-b633-5c8e0ad7911b",
    "name": "Entertainment (Custom)"
  }
]
```

---

### Add Category
Creates a custom category for the authenticated user.
- **Method**: `POST`
- **Route**: `/api/categories`
- **Auth Required**: Yes

#### Request Body
- `name` (String, Required, Not Empty)
```json
{
  "name": "Subscriptions"
}
```

#### Successful Response (200 OK)
```json
{
  "id": "c71a3962-d922-482a-a92c-0e7040cc24b9",
  "name": "Subscriptions"
}
```

---

### Update Category
Updates the display name of a custom category.
- **Method**: `PATCH`
- **Route**: `/api/categories/{id}`
- **Auth Required**: Yes

#### Request Body
- `name` (String, Required)
```json
{
  "name": "Streaming Services"
}
```

#### Successful Response (200 OK)
```json
{
  "id": "c71a3962-d922-482a-a92c-0e7040cc24b9",
  "name": "Streaming Services"
}
```

#### Error Responses
- **403 Forbidden**: Access Denied (if attempting to modify a global/system category or a category owned by another user).
- **404 Not Found**: Category ID does not exist.

---

### Delete Category
Deletes a user-specific custom category.
- **Method**: `DELETE`
- **Route**: `/api/categories/{id}`
- **Auth Required**: Yes

#### Successful Response (204 No Content)
- *Empty Response Body*

#### Error Responses
- **403 Forbidden**: Access Denied (if attempting to delete a system category or a category owned by another user).
- **404 Not Found**: Category ID does not exist.

---

## 3. Transaction (Expense/Credit) Endpoints

### Get Transactions
Retrieves all transactions (expenses and credits) belonging to the authenticated user.
- **Method**: `GET`
- **Route**: `/api/expenses`
- **Auth Required**: Yes

#### Successful Response (200 OK)
```json
[
  {
    "id": "a98c7651-4011-477f-a6bd-27b9213123ab",
    "title": "Netflix Subscription",
    "amount": 15.49,
    "category": {
      "id": "c71a3962-d922-482a-a92c-0e7040cc24b9",
      "name": "Streaming Services"
    },
    "type": "EXPENSE",
    "transactionDate": "2026-06-25",
    "isRecurring": true,
    "recurrence": "MONTHLY",
    "nextExecutionDate": "2026-07-25"
  },
  {
    "id": "50c82f9d-1144-4861-a083-ef76a08901cb",
    "title": "Monthly Salary",
    "amount": 3500.00,
    "category": null,
    "type": "CREDIT",
    "transactionDate": "2026-06-01",
    "isRecurring": false,
    "recurrence": "NONE",
    "nextExecutionDate": null
  }
]
```

---

### Add Transaction
Creates a new transaction for the authenticated user. Can be configured as a recurring transaction.
- **Method**: `POST`
- **Route**: `/api/expenses`
- **Auth Required**: Yes

#### Request Body
- `title` (String, Required)
- `amount` (Double, Required)
- `categoryId` (UUID, Optional)
- `type` (String enum `EXPENSE` | `CREDIT`, Optional, defaults to `EXPENSE`)
- `transactionDate` (String date `YYYY-MM-DD`, Optional, defaults to current date)
- `isRecurring` (Boolean, Optional, defaults to `false`)
- `recurrence` (String enum `NONE` | `DAILY` | `WEEKLY` | `MONTHLY` | `YEARLY`, Optional, defaults to `NONE`)
```json
{
  "title": "Grocery Shopping",
  "amount": 75.50,
  "categoryId": "e5c26b52-4467-4228-87e0-63c67586520f",
  "type": "EXPENSE",
  "transactionDate": "2026-06-27",
  "isRecurring": false,
  "recurrence": "NONE"
}
```

#### Successful Response (200 OK)
```json
{
  "id": "8d7f5263-2281-420a-8bf3-3e11b2cb0ac1",
  "title": "Grocery Shopping",
  "amount": 75.50,
  "category": {
    "id": "e5c26b52-4467-4228-87e0-63c67586520f",
    "name": "Food & Dining"
  },
  "type": "EXPENSE",
  "transactionDate": "2026-06-27",
  "isRecurring": false,
  "recurrence": "NONE",
  "nextExecutionDate": null
}
```

---

### Update Transaction
Updates transaction details. Enforces ownership check.
- **Method**: `PATCH`
- **Route**: `/api/expenses/{id}`
- **Auth Required**: Yes

#### Request Body
- All fields in the creation request are optional here.
```json
{
  "amount": 80.00
}
```

#### Successful Response (200 OK)
```json
{
  "id": "8d7f5263-2281-420a-8bf3-3e11b2cb0ac1",
  "title": "Grocery Shopping",
  "amount": 80.00,
  "category": {
    "id": "e5c26b52-4467-4228-87e0-63c67586520f",
    "name": "Food & Dining"
  },
  "type": "EXPENSE",
  "transactionDate": "2026-06-27",
  "isRecurring": false,
  "recurrence": "NONE",
  "nextExecutionDate": null
}
```

#### Error Responses
- **403 Forbidden**: Access Denied (if attempting to modify a transaction belonging to another user).
- **404 Not Found**: Transaction ID does not exist.

---

### Delete Transaction
Deletes a transaction. Enforces ownership check.
- **Method**: `DELETE`
- **Route**: `/api/expenses/{id}`
- **Auth Required**: Yes

#### Successful Response (204 No Content)
- *Empty Response Body*

#### Error Responses
- **403 Forbidden**: Access Denied.
- **404 Not Found**: Transaction ID does not exist.

---

## 4. Budget Endpoints

### Set / Update Budget
Sets or updates a monthly limit threshold for a specific category or globally.
- **Method**: `POST`
- **Route**: `/api/budgets`
- **Auth Required**: Yes

#### Request Body
- `amountLimit` (Double, Required, Must be positive)
- `categoryId` (UUID, Optional - if omitted or null, this sets a **Global** budget limit)
- `budgetMonth` (String `"YYYY-MM"`, Optional - defaults to the current month)
```json
{
  "amountLimit": 500.00,
  "categoryId": "e5c26b52-4467-4228-87e0-63c67586520f",
  "budgetMonth": "2026-06"
}
```

#### Successful Response (200 OK)
```json
{
  "id": "f58bc12a-3377-4933-bf4b-e85d82efba02",
  "categoryId": "e5c26b52-4467-4228-87e0-63c67586520f",
  "categoryName": "Food & Dining",
  "amountLimit": 500.00,
  "budgetMonth": "2026-06"
}
```

---

### Get Budget Summary
Retrieves user budget limits compared against actual spent amounts.
- **Method**: `GET`
- **Route**: `/api/budgets/summary`
- **Auth Required**: Yes

#### Query Parameters
- `month` (String `"YYYY-MM"`, Optional - defaults to current calendar month)
- **Example**: `/api/budgets/summary?month=2026-06`

#### Successful Response (200 OK)
```json
[
  {
    "budgetId": "f58bc12a-3377-4933-bf4b-e85d82efba02",
    "categoryId": "e5c26b52-4467-4228-87e0-63c67586520f",
    "categoryName": "Food & Dining",
    "amountLimit": 500.00,
    "actualSpent": 75.50,
    "remaining": 424.50,
    "isExceeded": false,
    "budgetMonth": "2026-06"
  },
  {
    "budgetId": "c01828cb-14d2-45e5-aa08-d7fa1bb90a88",
    "categoryId": null,
    "categoryName": "Global",
    "amountLimit": 1000.00,
    "actualSpent": 1250.00,
    "remaining": -250.00,
    "isExceeded": true,
    "budgetMonth": "2026-06"
  }
]
```
