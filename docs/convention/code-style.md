---
trigger: model_decision
description: rule-code-style
---

Java
===

Method
---

- Keep up to 3 arguments on a single line.
- If there are more than 3 arguments, or if they are too long/complex, place each argument on a new line (Chop down if long). 

Record
---

- Records with up to three fields should be written in a single line.

Imports
---

- Group imports into distinct blocks separated by a single blank line in the following order:
  1. **Java Standard/Jakarta Libraries** (java.*, jakarta.*)
  2. **Spring Framework** (org.springframework.*)
  3. **Lombok** (lombok.*)
  4. **Swagger/OpenAPI** (io.swagger.*, org.springdoc.*)
  5. **Other External Libraries** (com.fasterxml.*, etc.)
  6. **Project Internal Classes** (dev.ticketing.*)

Jakarta Persistence
===

* To maintain consistency, apply class annotations in the following specific order.
  - @Table
  - @Entity

Dependency Injection
===

- Use Lombok's @RequiredArgsConstructor for constructor-based dependency injection in Spring components and Cucumber step classes.
- Avoid using field-based @Autowired injection.
- Dependencies should be declared as private final.

Domain Model Construction
===

To ensure domain model integrity and manage ID generation/reconstruction, follow these rules:

1.  **Lombok Usage**:
    *   Use `@Getter` (No setters for immutability).
    *   Use `@Builder` (Public).
    *   Use `@AllArgsConstructor(access = AccessLevel.PRIVATE)` to force usage of factory methods or builder.
    *   No public constructors.

2.  **Factory Methods**:
    *   **New Creation (`create`)**: Use a static `create(...)` method for creating new domain objects (where ID is typically `null`).
    *   **Reconstruction (`withId` or Builder)**: Use `User.withId(...)` or `User.builder()` when reconstructing objects from persistence (where ID exists).

**Example:**
```java
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    private Long id;
    private String email;

    // For new creation (ID is null)
    public static User create(String email) {
        return User.builder()
            .email(email)
            .build();
    }
    
    // For reconstruction (Optional convenience method, Builder is also fine)
    public static User withId(Long id, String email) {
        return User.builder()
             .id(id)
             .email(email)
             .build();
    }
}
```

Field Ordering Convention
===

- To maintain consistency across parameters, records, and entities, follow the hierarchical order below:
  1. **User** (userId, userEmail, etc.)
  2. **Match** (matchId, etc.)
  3. **Area** (areaId, areaType, etc.)
  4. **Section** (sectionId, sectionType, etc.)
  5. **Block** (blockId, etc.)
  6. **Seat** (seatId, etc.)

**Example:**
```java
public record AllocateSeatCommand(
    Long userId,
    Long matchId,
    Long seatId
) {}
```

Global Exception Handling Naming
===

- Classes annotated with @RestControllerAdvice must use the suffix **ControllerAdvice** instead of ExceptionHandler.
- This emphasizes their role as part of the controller layer's cross-cutting concerns.
- 예: AllocationControllerAdvice, UserControllerAdvice

Web Model Package Structure
===

- 웹 어댑터의 model 패키지는 반드시 request와 response 서브 패키지로 구분하여 관리합니다.
- 예: ...adapter.in.web.model.request, ...adapter.in.web.model.response
- 기능별 하위 패키지가 존재하는 경우, 해당 패키지 내부에 request와 response를 둡니다.
- 예: ...adapter.in.web.model.allocation.request, ...adapter.in.web.model.allocation.response

Enum Handling in JPA Entities
===

- **Always use @Enumerated(EnumType.STRING)** for enum fields in JPA entities.
- **Never use EnumType.ORDINAL** as it is fragile to enum reordering.
- Enum values are stored as VARCHAR in the database for readability and maintainability.

**Example:**
```java
@Entity
public class SomeEntity {
    @Enumerated(EnumType.STRING)
    private SomeStatus status;  // Stored as "PENDING", "ACTIVE", etc.
}
```

**Rationale:**
- STRING mode ensures database values remain human-readable.
- Prevents data corruption when enum order changes.
- Facilitates debugging and direct database queries.

Logging Convention
===

- Use Lombok's `@Slf4j` for logging.
- **Never use `System.out.println()`** for debugging or production logs.
- Use appropriate log levels (`trace`, `debug`, `info`, `warn`, `error`).
- For debugging during development, prefer `log.info` or `log.debug` over `System.out.println`.

Acceptance Test (BDD) Database Cleanup
===

- **Automated Table Discovery**: Instead of manually listing repositories or tables, use JPA's `EntityManager` to discover all entity tables and truncate them.
- This ensures that adding new domains/entities automatically includes them in the cleanup process without manual updates to `DatabaseCleanupHook`.
- **Referential Integrity**: Disable foreign key checks during truncation if necessary (e.g., `SET REFERENTIAL_INTEGRITY FALSE` for H2).

File Encoding
===

- **Always use UTF-8 encoding** when creating or modifying files, especially those containing non-ASCII characters (e.g., Korean).
- When using PowerShell (Set-Content, Out-File, etc.), explicitly specify `-Encoding UTF8` or use `[System.IO.File]::WriteAllText()` to ensure correct encoding.
- This prevents "Mojibake" (broken characters) on different OS environments (Windows, Linux, macOS).
