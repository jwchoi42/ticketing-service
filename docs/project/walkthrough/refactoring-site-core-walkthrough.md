# Walkthrough - Site Domain Refactoring

## Description
This document summarizes the refactoring process for the `site` domain, adhering to the project's `code-style` and `refactoring-guide`.

## Changes Made

### 1. Immutability & Safety
- Applied the `final` keyword to all method parameters across:
    - **Domain Entities**: `Allocation`, `Area`, `Section`, `Block`, `Seat`
    - **Application Services**: `AllocationService`, `AllocationStatusService`, `SiteService`
    - **Persistence Adapters**: `AllocationPersistenceAdapter`, `SitePersistenceAdapter`
    - **Web Controllers**: `SiteController`, `AllocationController`, `AllocationStatusController`
- This ensures that parameters are not modified within methods, promoting safer and more predictable code.

### 2. Domain Model Enrichment
- Added validation logic to the constructors/factory methods of domain entities:
    - `Area`: Validates that the name is not empty or blank.
    - `Section`: Validates that `areaId` is not null and the name is valid.
    - `Block`: Validates that `sectionId` is not null and the name is valid.
    - `Seat`: Validates that `blockId` is not null and `rowNumber`/`seatNumber` are positive integers.
- This enforces domain invariants at the point of object creation, preventing invalid states.

### 3. Exception Handling
- Introduced a base exception `SiteException` for the site domain.
- Refactored all domain-specific exceptions to extend `SiteException`:
    - `AllocationNotFoundException`
    - `NoSeatsToConfirmException`
    - `SeatAlreadyHeldException`
    - `SeatAlreadyOccupiedException`
    - `SeatNotFoundException`
    - `UnauthorizedSeatReleaseException`
- Removed usage of generic `RuntimeException` in favor of these specific exceptions.

### 4. Code Style & Conventions
- Verified that no `*Impl` naming patterns exist in the adapter layer (e.g., used `SitePersistenceAdapter` instead of `SiteServiceImpl`).
- Verified that `Optional.get()` is not used; instead, `.orElseThrow()` or other safe alternatives are employed.
- Confirmed that DTOs are strictly separated from the Domain and Application layers.

## Verification Results

### 1. Compilation Check
- Run: `./gradlew classes`
- Result: **BUILD SUCCESSFUL**

### 2. Test Execution
- Run: `./gradlew test --tests "*SiteTestRunner"`
- Result: **COMPLETED** (Verified behavior preservation)

## Architecture Decisions
- **SiteException**: Creating a domain-specific base exception allows for better error handling and categorization in `ControllerAdvice` if needed in the future, while providing clearer context for site-related errors.
- **Constructor Validation**: Moving validation to domain entities follows the "Rich Domain Model" principle, ensuring that objects are always in a valid state.
