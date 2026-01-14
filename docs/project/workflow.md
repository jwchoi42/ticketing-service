
---
description: Project Task Management Workflow (Plan-Task-Walkthrough)
---

Project Task Workflow
===

To manage context limits, potential interruptions, and ensure clear documentation of work, we follow a strict **Plan-Task-Walkthrough** cycle for all significant development tasks.

Directory Structure
---

All files related to this workflow are stored in `docs/project/`:

```
docs/project/
├── plan/          # What we intend to do (Requirements, Strategy, Checklist)
├── task/          # Real-time progress tracking (Context Saver, Current Status)
└── walkthrough/   # What was done (Results, Key Decisions, Verification)
```

Workflow Steps
---

### 1. Planning Phase (`docs/project/plan`)

Before writing code, establish a clear plan.

1.  **Create File**: `docs/project/plan/{feature-name}-plan.md`
2.  **Content**:
    *   **Goal**: High-level objective.
    *   **Context**: Relevant background information.
    *   **Strategy**: Technical approach (Architecture, Refactoring steps).
    *   **Checklist**: Detailed breakdown of tasks (Todo list).

### 2. Execution Phase (`docs/project/task`)

While working, maintain a live record of progress. This is crucial for recovering context if the session ends abruptly or the model context is reset.

1.  **Create File**: `docs/project/task/{feature-name}-task.md`
2.  **Content**:
    *   **Current State**: What is currently being worked on?
    *   **Completed Items**: Checked off items from the Plan.
    *   **Pending Items**: Remaining work.
    *   **Issues/Notes**: Temporary blockers, ideas, or things to remember.
3.  **Update Frequency**: Update this file after every major step or before any context reset.

### 3. Completion Phase (`docs/project/walkthrough`)

After finishing the task, document the outcome.

1.  **Create File**: `docs/project/walkthrough/{feature-name}-walkthrough.md`
2.  **Content**:
    *   **Summary**: Brief overview of the implementation.
    *   **Changes**: Key files modified and architectural decisions.
    *   **Verification**: How to test/verify the changes (Screenshots, Logs, Commands).
    *   **Next Steps**: Follow-up tasks or technical debt created.

Naming Convention
---
Ensure files in all three directories share the same prefix for easy mapping.

*   `plan/refactoring-user-core-plan.md`
*   `task/refactoring-user-core-task.md`
*   `walkthrough/refactoring-user-core-walkthrough.md`
