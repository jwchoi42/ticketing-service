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

Task Continuity Rule
===
- 작업을 **연속성**을 보장하기 위해, 작업 진행 상황을 `.agent/rules/project/active_task.md`에 기록하고 관리합니다.
- 복잡한 작업이나 다단계 작업(Refactoring, Feature Implementation) 수행 시 반드시 이 파일을 최신 상태로 유지합니다.
- 새로운 세션을 시작하면, 가장 먼저 `.agent/rules/project/active_task.md`를 확인하여 이전 작업의 중단 지점을 파악합니다.

`active_task.md` Structure
---
- **Current Objective**: 현재 진행 중인 상위 작업의 목표
- **Progress Checklist**: 하위 작업 항목과 완료 여부 (`[x]`, `[ ]`)
- **Context & Notes**: 다음 작업자에게 전달할 특이사항, 수정 중인 파일, 남아있는 에러 등

File Encoding Protection Rule
===
- **All files MUST be saved in UTF-8 encoding.**
- Broken encoding (Mojibake) is unacceptable.
- When using shell commands to write files on Windows, always specify encoding (e.g., `Out-File -Encoding UTF8` or `[System.IO.File]::WriteAllText`).
