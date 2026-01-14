---
description: Refactoring Plan for Reservation Domain
---

# Reservation Core Refactoring Plan

## Goal
Apply `code-style` and `refactoring-guide` standards to the `reservation` domain.

## Context
Refactoring based on [Code Style](../../convention/code-style.md) and [Refactoring Guide](../../convention/refactoring-guide.md).

## Strategy
1. **Code Style Review**: Method arguments, Record formatting, Immutability, Naming.
2. **Refactoring**: Domain model richness, DTO separation, specific Domain Exceptions.

## Checklist

### 1. Code Style
- [ ] **Naming**: Ensure `*Impl` classes are renamed or don't exist.
- [ ] **Immutability**: Add `final` to all parameters in Domain/Service/Adapter/Controller.
- [ ] **Optional**: Remove `Optional.get()`, use `.orElseThrow()`.
- [ ] **Ordering**: Check method ordering and Field Ordering Convention.

### 2. Architecture & Design
- [ ] **Domain Model**: Move logic from Service to `Reservation` Entity.
- [ ] **DTO Separation**: Ensure no Web DTOs used in Domain layer.
- [ ] **Exceptions**: Introduce `ReservationException` and use specific subclasses.

### 3. Test
- [ ] **Terminology**: Rename Korean test methods to English if applicable.
- [ ] **Verification**: Run `ReservationTestRunner` after refactoring.
