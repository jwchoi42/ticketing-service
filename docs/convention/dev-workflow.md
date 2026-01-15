---
trigger: model_decision
---

Development Workflow
===

This workflow defines the standard process for developing features in the Ticketing Service.

Core Principles
---

- **Test-Driven Development (BDD)**: Define features in English (Gherkin) before implementation.
- **Shift Left**: Catch bugs early via Cucumber tests.
- **Statelessness**: Ensure tests are independent and repeatable.

Hexagonal Architecture + BDD (Cucumber) Development Process
===

1단계: Feature 정의 (구현 전)
---
코드를 한 줄도 짜기 전에 .feature 파일을 먼저 작성합니다.

- **할 일**: 기획자/UI 디자이너와 합의된 비즈니스 요구사항을 Gherkin 문법(Given-When-Then)으로 기술합니다.
- **목표**: 우리가 무엇을 만들지 '명세서'를 확정하는 단계입니다.

2단계: 실패하는 인수 테스트 작성 (Outside-In 시작)
---
작성한 Feature를 실행할 Step Definitions와 인수 테스트 코드를 만듭니다.

- 이때 테스트는 **실패(Red)**해야 합니다. (아직 기능 구현 전이기 때문)
- 헥사고날 관점에서는 **입력 어댑터(Controller)**를 호출하는 테스트를 먼저 짜게 됩니다.

3단계: 내부 구성 요소 구현 (Inside-Out)
---
인수 테스트를 통과시키기 위해 내부 로직을 구현합니다.
의존성 방향은 외부에서 내부로 흐르지만(Dependencies Flow In), **구현 순서는 도메인 독립성을 위해 내부에서 외부로(Inside-Out) 진행합니다.**

### 상세 구현 순서 (Detailed Flow)
1. **Domain Model & Entity**: 핵심 비즈니스 객체와 규칙을 가장 먼저 정의합니다.
2. **Inbound Port**: 외부에서 도메인을 사용하기 위한 인터페이스(UseCase)를 정의합니다. (What)
3. **UseCase -> Service Implementation**: 비즈니스 로직을 구체적으로 구현합니다. (How)
   - *단위 테스트 병행*: 유스케이스와 도메인은 별도의 단위 테스트로 검증합니다.
4. **Outbound Port**: 서비스가 필요로 하는 외부 시스템(DB 등)의 인터페이스를 정의합니다.
5. **Outbound Adapter**: 실제 외부 시스템 연결(Persistence 등)을 구현합니다.
6. **Inbound Adapter**: Web/API 레이어를 구현하여 외부 요청을 UseCase로 연결합니다.

4단계: 어댑터 연결 및 테스트 통과 (Inside-Out 완성)
---
DB나 외부 API 어댑터를 완성하여 전체 흐름을 연결합니다.

- **할 일**: 2단계에서 실패했던 Cucumber 테스트를 다시 실행합니다.
- **목표**: 모든 레이어가 연결되어 **테스트가 성공(Green)**하는 것을 확인합니다.

Verification Rule
---
- **CRITICAL**: The AI agent **MUST NOT** run `./gradlew test` or any test execution commands automatically.
- Test execution will be performed by the User, or specifically requested by the User in a separate turn.
- The agent should focus on code correctness, compilation (using `./gradlew classes` or similar), and static analysis.

Test Execution Rule
---

- **Do NOT run tests automatically**: Running full test suites or even specific tests with `./gradlew test` can be time-consuming and resource-intensive. 
- **User Control**: The User maintains control over when tests are executed.
- **Exception**: Only run tests if the User explicitly says "Run the tests now" or provides a specific command to execute.
