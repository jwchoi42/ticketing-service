
---
description: Code Refactoring Plan
---

Code Refactoring Plan
===

This document outlines the refactoring tasks identified based on the `code-style` and `refactoring-guide` documents. The tasks are categorized by their nature and impact.

## 1. Code Style Refactoring

### 1.1. Naming Conventions
- [ ] **Implementation Classes**: Rename `*Impl` classes to specific technical names (e.g., `UserRepositoryImpl` -> `JpaUserRepository`).
  - *Candidates*: Run `find src/main/java -name "*Impl.java"` to identify candidates.
- [ ] **Test Methods**: Rename Korean test methods in BDD acceptance tests to English.

### 1.2. Immutability & Safety
- [ ] **Return Collections**: Ensure `List` or `Map` returned from Domain/Service layers are wrapped in `Collections.unmodifiableList` or copied (Defensive Copy).
- [ ] **Final Parameters**: Add `final` keyword to method parameters in Domain and Service layers.
- [ ] **Optional Usage**: Replace `Optional.get()` with `.orElseThrow(...)` across the codebase.
  - *Candidates*: Run `grep -r "Optional.get()" src/main/java` to identify candidates.

### 1.3. Method & Class Structure
- [ ] **Method Ordering**: Reorder methods in classes: `public` -> `private`. Move `private` methods closer to their usage if only used once (Locality Rule).
- [ ] **Stream Complexity**: Identify Java Stream pipelines with >3 operations and consider refactoring to `for-loop` for debuggability.

## 2. Structural & Architectural Refactoring

### 2.1. Domain Model Enrichment
- [ ] **Anemic Model Check**: Review Entities for lack of business logic (only getters/setters). Move logic from Service to Entity/Value Object.
  - *Focus*: `Allocation`, `Seat`, `Match` entities.

### 2.2. Service Layer Responsibility
- [ ] **Single Responsibility**: Split Services that handle multiple unrelated concerns (e.g., separating `Snapshot` reading from `Change` processing).

### 2.3. Layer Boundaries
- [ ] **DTO Separation**: Verify no DTOs (`*Request`, `*Response`) are used inside Domain Entities or core Business Logic methods.
- [ ] **Exception Handling**: Replace generic `IllegalStateException`/`IllegalArgumentException` with specific Domain Exceptions (e.g., `SeatAlreadyOccupiedException`).

## 3. Legacy & Performance

### 3.1. Performance Optimization
- [ ] **N+1 Logic**: Review loops invoking DB calls (repositories) and refactor to batch fetches.

### 3.2. Legacy Code
- [ ] **Deprecation**: Mark any superseded classes/methods with `@Deprecated` during incremental refactoring.

## Execution Strategy

- **Priority**:
    1. `Optional.get()` removal (Safety).
    2. Naming convention updates (Clarity).
    3. Structural Refactorings (Maintenance).
- **Process**: Follow the [Refactoring Guide](../convention/refactoring-guide.md) workflow (Branch -> Refactor -> Test -> Commit).
