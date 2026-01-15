
---
trigger: model_decision
description: refactoring-guide
---

Refactoring Guide
===

This document provides guidelines and patterns for refactoring code in the Ticketing Service project. It captures best practices, common refactoring scenarios, and step-by-step approaches to maintain code quality while evolving the system.

Core Refactoring Principles & Strategies
===

1. **Test-First Refactoring**
   - Always ensure tests exist before refactoring.
   - Run tests before and after refactoring to verify behavior preservation.
   - Update tests if the refactoring changes the public API (but behavior should remain the same).

2. **Incremental & Atomic Changes**
   - **Small Steps**: Make small, focused changes rather than "Big Bang" refactorings.
   - **Frequent Commits**: Commit each logical step that keeps the system in a working (compilable) state.
   - **Separation**: Do not mix refactoring changes with new feature development.

3. **Behavior Preservation**
   - Refactoring improves internal structure without altering external behavior.
   - If functional changes are needed, treat them as separate tasks.

4. **Architecture Compliance**
   - Maintain dependency direction (Outside → Application → Domain).
   - Keep domain logic pure and independent.
   - Respect hexagonal architecture boundaries.

5. **Legacy & Performance Management**
   - **Gradual Replacement**: Refactor legacy code incrementally. Allow old and new implementations to coexist if necessary (Strangler Fig Pattern), deprecating the old one.
   - **Performance vs Readability**: Prioritize readability by default. However, **critical performance defects** (e.g., N+1 queries) must be resolved immediately.

Project-Specific Anti-Patterns
===

Anemic Domain Model
---
- **Symptom**: Business logic (validations, state calculations) resides in `Service` classes while Entities are just data holders (Getters/Setters).
- **Refactoring Goal**: **Rich Domain Model**. Move business logic into Entities/Value Objects. Services should strictly orchestrate flow, delegating logic to the domain.

DTO Invasion
---
- **Symptom**: DTOs (Request/Response objects) are used inside the Domain layer or Entity methods.
- **Refactoring Goal**: **Strict Separation**. DTOs must be converted to Domain Models/Commands at the boundary (Controller or Service entry). The Domain layer must completely ignore the existence of DTOs.

Common Refactoring Scenarios
===

1. Renaming for Clarity
---
**When to Apply**: Names don't reflect current purpose, business terminology changed, or code evolved.

**Steps**:
1.  **Identify scope**: Affected files (DTOs, UseCases, Services, Tests).
2.  **Update in order**: Domain models → Application (Ports/Services) → Adapters → Tests.
3.  **Verify**: `./gradlew classes`.
4.  **Sync Documentation**: Update API specs/feature files.

2. Extracting Services
---
**When to Apply**: Service has multiple responsibilities (SRP Violation) or mixing concerns (Snapshot vs Changes).

**Steps**:
1.  **Identify boundaries**: Logical separation points.
2.  **Create Interfaces**: New output ports.
3.  **Extract Implementation**: Move methods to new service/adapter.
4.  **Update DI**: Inject new service into callers.
5.  **Refactor Tests**: Split test classes.

3. Refactoring DTOs and Records
---
**When to Apply**: Ambiguous names, disorganized structure, or unclear API contracts.

**Steps**:
1.  **Rename**: Reflect actual purpose (`GetStatusResponse` -> `AllocationStatusSnapshotResponse`).
2.  **Order Fields**: Follow Field Ordering Convention (Hierarchy: User > Match > Seat).
3.  **Update References**: Controllers, Mappers, Tests.

4. Introducing Domain Exceptions
---
**When to Apply**: Using generic exceptions (`IllegalStateException`) or poor error context.

**Steps**:
1.  **Create Exception**: `{Situation}Exception` in `{domain}/application/service/exception/`.
2.  **Constructors**: Support message and cause.
3.  **Replace**: Swap generic exceptions in Service layer.
4.  **Handle**: Add to `ControllerAdvice`.

5. Migrating Storage Backends
---
**When to Apply**: RDB → Redis migration or introducing caching.

**Steps**:
1.  **Abstract**: Ensure Port interface exists.
2.  **Implement Adapter**: Create new adapter (e.g., `RedisAllocationAdapter`).
3.  **Toggle**: Feature flag (`use-redis-allocation`).
4.  **Parallel Run/Test**: Verify parity.
5.  **Switch & Cleanup**: Redirect traffic and remove old adapter.

6. Refactoring Test Code
---
**When to Apply**: Duplicated setup, unclear steps, or bloated `TestContext`.

**Steps**:
1.  **Extract Utilities**: Helper methods and `@Before` hooks.
2.  **Refactor Context**: Group state, list support (`heldSeatId` -> `heldSeatIds`).
3.  **Update Gherkin**: Standardize terminology.

Refactoring Workflow
===

1. **Preparation**: Review code, ensure tests pass, create `refactor/{topic}` branch.
2. **Implementation**: Inside-Out approach (Domain → Ports → Services → Adapters).
3. **Verification**: 
   - **Continuous**: Check compilation (`./gradlew classes`) frequently to ensure code consistency.
   - **Batch**: Run tests (`./gradlew test`) after completing a logical unit of refactoring (e.g., entire domain update) to verify behavior preservation.
4. **Finalization**: Commit (`refactor: ...`), create PR.

Common Pitfalls to Avoid
===

1.  **Big Bang Refactoring**: Break into smaller chunks.
2.  **Mixing Features**: Don't add features during refactoring.
3.  **Breaking Tests**: Update tests, don't delete them.
4.  **Incomplete Refactoring**: Don't leave mixed patterns.

Tools and Verification
===

**Commands**:
```bash
./gradlew classes                       # Check compilation
./gradlew test --tests "*AllocationTest" # Run specific tests
./gradlew checkstyleMain                # Check style
```

References
===
- [Architecture Guide](./architecture.md)
- [Code Style](./code-style.md)
- [Dev Workflow](./dev-workflow.md)
