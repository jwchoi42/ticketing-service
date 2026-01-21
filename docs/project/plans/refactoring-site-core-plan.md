---
description: Refactoring Plan for Site Domain
---

# Site Core Refactoring Plan

## Goal
Apply `code-style` and `refactoring-guide` standards to the `site` domain.

## Context
Refactoring based on [Code Style](../../convention/code-style.md) and [Refactoring Guide](../../convention/refactoring-guide.md).

## Strategy
1. **Code Style Review**: Method arguments, Record formatting, Immutability, Naming.
2. **Refactoring**: Domain model richness, DTO separation, specific Domain Exceptions.

## Checklist

### 1. Code Style
- [x] **Naming**: Ensure `*Impl` classes are renamed (e.g., `JpaSiteRepository`). (Verified: None found)
- [x] **Immutability**: Add `final` to all parameters in Domain/Service/Adapter/Controller.
- [x] **Optional**: Remove `Optional.get()`, use `.orElseThrow()`. (Verified: None found)
- [x] **Ordering**: Check method ordering and Field Ordering Convention.

### 2. Architecture & Design
- [x] **Domain Model**: Move logic from Service to `Site`/`Stadium` Entity. (Added validation to domain constructors)
- [x] **DTO Separation**: Ensure no Web DTOs used in Domain layer. (Verified)
- [x] **Exceptions**: Replace generic exceptions with `SiteException` subclasses. (Introduced `SiteException`)

### 3. Test
- [ ] **Terminology**: Rename Korean test methods to English in BDD steps.
- [ ] **Verification**: Run site-related tests after refactoring.
