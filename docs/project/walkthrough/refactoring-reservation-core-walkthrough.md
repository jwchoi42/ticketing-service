# Walkthrough - Reservation Domain Refactoring

## Description
This document summarizes the refactoring process for the `reservation` domain.

## Changes Made

### 1. Immutability & Safety
- Applied the `final` keyword to all method parameters across:
    - **Domain Entities**: `Reservation`
    - **Application Services**: `ReservationService`
    - **Persistence Adapters**: `ReservationPersistenceAdapter`, `ReservationEntity`
    - **Web Controllers**: `ReservationController`

### 2. Domain Model Enrichment
- Added validation logic to the `Reservation` factory methods (`create`, `withId`, `withSeatIds`).
- Enforces that `userId`, `matchId`, and `status` are not null, and `seatIds` is not empty when provided.

### 3. Exception Handling
- Introduced `ReservationException` and specific subclasses:
    - `ReservationNotFoundException`
    - `ReservationHoldExpiredException`
    - `ReservationSeatNotHeldException`
- Replaced generic `IllegalStateException` and `IllegalArgumentException` in `ReservationService` with these domain exceptions.

### 4. DTO Separation
- Created `ReservationResponse` DTO to separate terminal web response from the domain model.
- Updated `ReservationController` to use `ReservationResponse`.

## Verification Results

### 1. Compilation Check
- Run: `./gradlew classes`
- Result: **BUILD SUCCESSFUL**
