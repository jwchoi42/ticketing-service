# Test
- Use JUnit 5.
- Prefer running single tests, and not the whole test suite, for performance.

# Integration Test
- Use Testcontainers to provide isolated environments for MySQL, Redis, and Kafka.

# Acceptance Test (Behavior-Driven Development)
- Use Cucumber with the JUnit Platform Engine.
- Store `.feature` files.
- Explicitly use `Given-When-Then` Comment to divide the test logic.