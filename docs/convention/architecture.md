---
trigger: model_decision
description: rule-architecture
---

Package Structure
===

dev.{project}.{domain}/
├── adapter/
│   ├── in/
│   │   └── web/              
│   └── out/
│       └── persistence/      
├── application/
│   ├── port/
│   │   ├── in/               
│   │   └── out/
│   │       └── persistence/  
│   └── service/              
└── domain/                   

Dependency Rules
===
- `domain`: No External dependencies (Plain Old Java Object, POJO)
- `application`: Depends only on the `domain`
- `adapter`: May depend on both `domain` and `application`
- **Direction**: Dependencies must flow from the outside to the inside. (Inbound → Application → Domain ← Outbound)

Naming & Type Conventions
---

### Inbound (Web)
- Use `*Request` / `*Response` for Web DTOs.
- All requests must be converted to `Command` or `Query` before entering the Application layer.

### Application Ports
- **Command**: Use Java `record` for immutable state-changing data (e.g., `CreateOrderCommand`).
- **Query**: Use for read-only data requests (e.g., `GetOrderDetailQuery`).
- **UseCase**: Interface name for inbound ports (e.g., `PlaceOrderUseCase`).
- **One Interface per UseCase**: Each UseCase interface must have exactly one method. This encourages granular interfaces and minimizes dependencies for callers.

### Outbound (Persistence)
- Port naming must be technology-neutral:
  - `load...()`: To fetch data.
  - `record...()`: To save or update data.
  - `remove...()`: To delete data.
- Port Location: `application.port.out.persistence`

Exception Handling
---

- **Global Exceptions**: Generic exceptions (e.g., Exception, IllegalArgumentException, MethodArgumentNotValidException) must be handled in dev.ticketing.common.web.GlobalExceptionHandler.
- **Domain Exceptions**: Domain-specific exceptions (e.g., LoginFailureException, NotEnoughSeatsException) must be handled in *ExceptionHandler within the domain's web adapter package (dev.ticketing.core.{domain}.adapter.in.web).
  - Use @RestControllerAdvice(basePackageClasses = {Domain}Controller.class) to scope the handler to the specific domain.
