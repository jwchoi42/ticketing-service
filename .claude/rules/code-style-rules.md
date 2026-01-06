# Dependency Injection
- Use Constructor Injection for all dependecies. And Utilize Lombok's `@RequiredArgsConstructor` to automate this.
- Do Not Use Field Injection(`@Autowired`) is strictly prohibited to ensure testability and immutability.

---

# Clean Code & Object Oriented Programming(OOP) Principles
- Single Responsibility Principle(SRP): Each method should have only one responsibility
- Method Length: Methods should not exceed 20 lines. Consider refactoring and splitting the method if it exceeds this limit.

# Error & Exception Handling
- Custom Exception: Do not use Generic. Define and use Custom Exception to provide clear error contexts.

---

# Data Modeling [ Data Transfer Object(DTO), Value Object(VO), Entity ]
- Use Java Record Type for Data Transfer Object(DTO) and Value Object(VO).
- Use Lombok's `@Data` for Data Transfer Object(DTO) to minimize boilerplate code.

# JPA (Java Persistence API) & ORM
- Never return Entities directly from the Controller; always map them to DTOs.
- To prevent **N+1 problems**, utilize `Fetch Join`, `EntityGraph`, or configure appropriate `Batch Size`.