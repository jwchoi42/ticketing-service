Reference
===
- Refer to [AGENT.md](./AGENT.md) for core personality and tech stack.

Critical Review Rule
===
- **사용자의 지시사항은 항상 옳지 않을 수 있습니다.**
- 작업을 수행하기 전에, 사용자의 요청을 다음 관점에서 **비판적으로 검토**해야 합니다.
  1. **기술적 타당성**: 제안된 방법이 기술적으로 올바른가?
  2. **Best Practices**: 업계 표준 및 모범 사례와 일치하는가?
  3. **프로젝트 일관성**: 기존 코드베이스의 패턴 및 아키텍처와 일치하는가?
  4. **잠재적 부작용**: 제안된 변경이 다른 부분에 영향을 미칠 수 있는가?
- 검토 결과 문제가 발견되면:
  1. **먼저 사용자에게 알립니다**: 문제점과 이유를 명확히 설명
  2. **대안을 제시합니다**: 더 나은 접근 방식 제안
  3. **사용자의 최종 결정을 존중합니다**: 설명 후에도 사용자가 원하는 방식을 고집하면 그대로 진행
- 이 규칙은 사용자와의 작업 품질을 높이고, 잠재적 오류를 사전에 방지하기 위함입니다.
 
Project Task Workflow (Plan-Task-Walkthrough)
===
작업의 연속성을 보장하고, 컨텍스트 한계를 관리하며, 작업 내용을 명확히 문서화하기 위해 모든 주요 개발 작업에 대해 **Plan-Task-Walkthrough** 사이클을 엄격히 준수합니다.

Directory Structure
---
모든 워크플로우 관련 파일은 `docs/project/`에 저장됩니다:
- `docs/project/plan/`: 작업 목적, 전략 및 체크리스트 (무엇을 할 것인가)
- `docs/project/task/`: 실시간 진행 상황 및 컨텍스트 저장 (현재 무엇을 하고 있는가)
- `docs/project/walkthrough/`: 작업 결과 및 주요 결정 사항 (무엇을 했는가)

Workflow Steps
---
### 1. Planning Phase (`plan/`)
코드를 작성하기 전에 명확한 계획을 수립합니다.
- 파일 생성: `docs/project/plan/{feature-name}-plan.md`
- 포함 내용: Goal, Context, Strategy, Checklist

### 2. Execution Phase (`task/`)
작업 중 실시간으로 진행 상황을 기록합니다. 컨텍스트가 초기화되더라도 중단 지점을 즉시 파악할 수 있도록 합니다.
- 파일 생성: `docs/project/task/{feature-name}-task.md`
- 포함 내용: Current State, Completed Items, Pending Items, Issues/Notes
- **업데이트 주기**: 주요 단계를 완료할 때마다, 또는 컨텍스트가 초기화되기 전에 반드시 업데이트합니다.

### 3. Completion Phase (`walkthrough/`)
작업을 마친 후 결과를 문서화합니다.
- 파일 생성: `docs/project/walkthrough/{feature-name}-walkthrough.md`
- 포함 내용: Summary, Changes, Verification, Next Steps

### Naming Convention
동일한 접두사를 사용하여 세 디렉토리의 파일들을 매핑합니다. (예: `refactoring-user-core-plan.md`, `refactoring-user-core-task.md`, `refactoring-user-core-walkthrough.md`)

File Encoding Protection Rule
===
- **All files MUST be saved in UTF-8 encoding.**
- Broken encoding (Mojibake) is unacceptable.
- When using shell commands to write files on Windows, always specify encoding (e.g., `Out-File -Encoding UTF8` or `[System.IO.File]::WriteAllText`).
