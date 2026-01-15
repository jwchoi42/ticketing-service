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

Class Structure & Formatting
---

- **Method Ordering**:
  - Place `public` methods first, followed by `private` methods.
  - **Locality Rule**: If a `private` method is used by only one `public` method, place it immediately below that `public` method.
  - Group related methods together to maximize cohesion.

Immutability Strategy
---

- **Collection Returns**:
  - **Defensive Copy**: Always perform a defensive copy when returning collections from domain objects to ensure encapsulation (e.g., `return new ArrayList<>(list)` or `List.copyOf(list)`).
- **`final` Usage**:
  - **Parameters**: **Mandatory**. Use `final` for method parameters to prevent accidental reassignment.
  - **Local Variables**: **Optional**. Omit `final` for readability, unless necessary for closure capturing.

Functional vs Imperative Style
---

- **Java Streams**:
  - Use for simple filtering and mapping operations.
  - **Rule of Thumb**: If the pipeline logic exceeds **3 steps** or requires modifying external variables, use a **for-loop** instead for better debuggability.
- **Optional**:
  - Use **only for return types**. Do not use for fields or parameters.
  - **Strict Ban on `get()`**: Never use `Optional.get()`. Force explicit exception handling using `.orElseThrow(() -> new ...Exception())`.

Naming Conventions
---

- **Implementations**:
  - Include specific technical stack or feature characteristics in the name.
  - **Bad**: `UserRepositoryImpl`
  - **Good**: `JpaUserRepository`, `InMemoryUserRepository`
- **Test Methods**:
  - **BDD Acceptance Tests**: Use **English** names for test methods, as the `@DisplayName` or Cucumber annotations already handle the Korean context. AVOID Korean method names in BDD to maintain technical consistency.

Service Layer Implementation Style
---

- **Narrative Flow**: Service methods should act as high-level orchestrators that read like English sentences.
- **Composed Method Pattern**: Break down complex logic into small, descriptive private methods or domain method calls.
- **Level of Abstraction**: Keep all steps in a method at the same conceptual level.

**Example:**
```java
// Good: Reads like a story
@Transactional
public Ticket reserveTicket(ReserveCommand command) {
    User user = loadUser(command.userId());
    Match match = loadMatch(command.matchId());
    
    validateReservationEligibility(user, match);
    
    Ticket ticket = match.issueTicket(user, command.seatId());
    
    return recordTicket(ticket);
}
```

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

API Response Convention
===

- **성공 응답**: `SuccessResponse<T>` 사용
- **실패 응답**: `ErrorResponse` 사용
- **HTTP 상태 코드**: 응답 본문에 포함하지 **않습니다**. HTTP 상태 코드로만 표현합니다.
- **timestamp**: 항상 포함됩니다.
- **message, data**: 둘 다 생략 가능합니다 (`@JsonInclude(NON_NULL)`).

**응답 구조:**
```java
// 성공 응답
public record SuccessResponse<T>(
    @JsonInclude(JsonInclude.Include.NON_NULL) String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) T data,
    LocalDateTime timestamp
) { }

// 실패 응답
public record ErrorResponse(
    @JsonInclude(JsonInclude.Include.NON_NULL) String message,
    LocalDateTime timestamp
) { }
```

**사용 예시:**
```java
// Controller - 성공
@PostMapping("/sign-up")
@ResponseStatus(HttpStatus.CREATED)
public SuccessResponse<UserResponse> signUp(@RequestBody SignUpRequest request) {
    UserResponse response = signUpUseCase.signUp(command);
    return SuccessResponse.of(response);
}

// ControllerAdvice - 실패
@ExceptionHandler(DomainException.class)
public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
    return ResponseEntity
        .status(e.getStatus())
        .body(ErrorResponse.of(e.getMessage()));
}
```

UseCase Return Type Convention
===

- **UseCase는 Domain 엔티티를 직접 반환하지 않습니다.**
- **DTO(Response 객체)를 반환**하여 Controller가 Domain에 직접 의존하지 않도록 합니다.
- 이는 도메인 변경 시 Controller 수정을 방지하고, 계층 간 결합도를 낮춥니다.

**잘못된 예시:**
```java
// UseCase
User signUp(SignUpCommand command);

// Controller
User user = signUpUseCase.signUp(command);
return ApiResponse.success(UserResponse.from(user));  // Controller가 Domain 참조
```

**올바른 예시:**
```java
// UseCase
UserResponse signUp(SignUpCommand command);

// Controller
UserResponse response = signUpUseCase.signUp(command);
return ApiResponse.success(response);  // Controller는 DTO만 참조
```

**Service 구현:**
```java
@Override
public UserResponse signUp(SignUpCommand command) {
    User user = User.create(command.email(), command.password());
    User saved = recordUserPort.record(user);
    return UserResponse.from(saved);  // Service에서 DTO 변환
}
```

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
