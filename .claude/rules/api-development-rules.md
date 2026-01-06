# Controller
- Use the `@RestController` annotation for all API entry points.
- Strictly follow REST(Representational State Transfer) principles for URI design and HTTP methods.

# Request & Response
- Never expose Java Persistence API(JPA) Entities directly. Always use Data Transfer Object(DTO) for both requests and responses.
- Naming Convention: [Action][Resource]Request / [Action][Resource]Response
- All Requests and Responses must follow a standardized format and consistent structure.
- Every Response must be wrapped in `ResponseEntity` to return the appopriate HTTP status cod along with the data.

# Validation
- Use `@Valid` or `@Validated` annotation in the Controller method parameters.
- Ensure all mandatory fields are validated using constrains such as `@NotNull`, `@NotBlank`, or `@NotEmpty`

# Exception Handling
- Use `@RestControllerAdvice` to handle exceptions globally and maintain consistent error response.

---

# Documentation
- Use Swagger for API documentation.
- Ensure all endpoints are testable via Swagger UI to verify development progress and functionality