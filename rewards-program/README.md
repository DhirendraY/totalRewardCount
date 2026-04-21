# Retailer Rewards Program

A Spring Boot REST API that calculates reward points earned by customers based on their purchase history.

---

## Table of Contents

- [Overview](#overview)
- [Reward Rules](#reward-rules)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Sample Requests & Responses](#sample-requests--responses)
- [Running the Tests](#running-the-tests)
- [Design Decisions](#design-decisions)

---

## Overview

A retailer offers a rewards program that awards points based on each recorded purchase.  
This application exposes RESTful endpoints to compute points earned **per customer per month** and the **grand total** over any time window.

---

## Reward Rules

| Purchase amount (per transaction) | Points earned |
|-----------------------------------|---------------|
| ≤ $50                             | 0 points      |
| $50 < amount ≤ $100               | 1 point per dollar above $50 |
| > $100                            | 1 point per dollar between $50–$100 **plus** 2 points per dollar above $100 |

**Example:** a $120 purchase = 2 × $20 + 1 × $50 = **90 points**

> Integer (floor) arithmetic is applied to the dollar portion; cents are ignored for point calculations.

---

## Project Structure

```
rewards-program/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/retailer/rewards/
    │   │   ├── RewardsApplication.java          ← Spring Boot entry point
    │   │   ├── controller/
    │   │   │   └── RewardsController.java       ← REST endpoints
    │   │   ├── service/
    │   │   │   └── RewardsService.java          ← Business logic & point calculation
    │   │   ├── model/
    │   │   │   ├── Transaction.java             ← Transaction domain model
    │   │   │   ├── MonthlyRewards.java          ← Per-month reward summary
    │   │   │   └── CustomerRewards.java         ← Aggregated customer reward summary
    │   │   ├── data/
    │   │   │   └── TransactionDataStore.java    ← In-memory seeded data set
    │   │   └── exception/
    │   │       ├── CustomerNotFoundException.java
    │   │       ├── InvalidDateRangeException.java
    │   │       └── GlobalExceptionHandler.java  ← Centralised error handling
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/retailer/rewards/
            ├── service/
            │   └── RewardsServiceTest.java      ← Unit tests (JUnit 5 + AssertJ)
            └── controller/
                └── RewardsControllerIntegrationTest.java ← Integration tests (MockMvc)
```

---

## Prerequisites

| Tool    | Version    |
|---------|------------|
| Java    | 17 or later|
| Maven   | 3.8+       |

---

## Running the Application

```bash
# from the rewards-program directory
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

---

## API Endpoints

| Method | Path                                    | Description                                         |
|--------|-----------------------------------------|-----------------------------------------------------|
| GET    | `/api/v1/rewards`                       | Rewards for **all** customers (all transactions)    |
| GET    | `/api/v1/rewards/{customerId}`          | Rewards for **one** customer                        |
| GET    | `/api/v1/rewards/range?startDate=&endDate=` | Rewards for all customers within a date range   |
| GET    | `/api/v1/rewards/{customerId}/range?startDate=&endDate=` | Rewards for one customer within a date range |

> Dates must be provided in `yyyy-MM-dd` format.

---

## Sample Requests & Responses

### All customers

```
GET /api/v1/rewards
```

```json
[
  {
    "customerId": "C001",
    "customerName": "Alice Smith",
    "monthlyRewards": [
      { "year": 2024, "month": 1, "monthLabel": "JANUARY 2024", "points": 115 },
      { "year": 2024, "month": 2, "monthLabel": "FEBRUARY 2024", "points": 250 },
      { "year": 2024, "month": 3, "monthLabel": "MARCH 2024", "points": 70 }
    ],
    "totalPoints": 435
  }
]
```

### Single customer

```
GET /api/v1/rewards/C001
```

### Date range

```
GET /api/v1/rewards/range?startDate=2024-01-01&endDate=2024-03-31
```

### Error response (404)

```json
{
  "timestamp": "2024-03-15T10:00:00.000",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with ID: C999"
}
```

---

## Running the Tests

```bash
mvn test
```

Test coverage includes:

- **Unit tests** (`RewardsServiceTest`) – parameterised point-calculation boundary tests, per-customer aggregation, date-range validation, null/negative/unknown input exception scenarios.
- **Integration tests** (`RewardsControllerIntegrationTest`) – full Spring context with MockMvc covering happy paths, 400 and 404 error responses, missing parameters, inverted date ranges, and invalid date formats.

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| In-memory data store | No external database is required; transactions are seeded dynamically relative to today's date, so months are never hard-coded. |
| `BigDecimal` for amounts | Avoids floating-point rounding errors common with `double`. |
| `YearMonth` grouping | Groups transactions by calendar month without hard-coding month names or numbers. |
| `GlobalExceptionHandler` | Centralises error handling and returns a uniform JSON error body for all failure modes. |
| Lombok | Reduces boilerplate on model classes while keeping them readable. |
| Java 17 | Uses modern language features (`List.of`, records-compatible patterns, `toList()`). |
