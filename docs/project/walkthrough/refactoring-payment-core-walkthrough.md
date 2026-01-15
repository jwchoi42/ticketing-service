# Walkthrough - Payment Domain Refactoring

## Description
This document summarizes the refactoring process for the `payment` domain.

## Changes Made

### 1. Immutability & Safety
- Applied the `final` keyword to all method parameters across:
    - **Domain Entities**: `Payment`
    - **Application Services**: `PaymentService`
    - **Persistence Adapters**: `PaymentPersistenceAdapter`, `PaymentEntity`
    - **Web Controllers**: `PaymentController`

### 2. Domain Model Enrichment
- Added validation logic to the `Payment` factory methods (`create`, `withId`).
- Enforces that `reservationId` is not null, `amount` is non-negative, and `method` is not empty.

### 3. Exception Handling
- Introduced `PaymentException` and specific subclasses:
    - `PaymentNotFoundException`
    - `InvalidPaymentStateException`
- Replaced generic `IllegalArgumentException` and `IllegalStateException` in `PaymentService` with these domain exceptions.
- Also used `ReservationNotFoundException` for cross-domain validation.

### 4. DTO Separation
- Created `PaymentResponse` DTO to separate terminal web response from the domain model.
- Updated `PaymentController` to use `PaymentResponse`.

## Verification Results

### 1. Compilation Check
- Run: `./gradlew classes`
- Result: **BUILD SUCCESSFUL**
