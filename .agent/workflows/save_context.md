---
description: Save current session artifacts (task, plan, walkthrough) to docs/agent/antigravity/artifact for persistence and handover.
---

1.  **Check/Create Directory**: Ensure that the directory `docs/agent/antigravity/artifact` exists. Create it if it doesn't.
    *   Path: `docs/agent/antigravity/artifact` (relative to project root)

2.  **Identify Artifacts**: Locate the current session's artifact files. These are typically:
    *   `task.md`
    *   `implementation_plan.md`
    *   `walkthrough.md`
    *   Any other relevant `.md` files in the session's brain/artifact directory.

3.  **Copy Files**: Copy these files to `docs/agent/antigravity/artifact`.
    *   Use `copy` command or `write_to_file` to save the content.
    *   Overwrite existing files in the target directory to update the "latest" state.

4.  **Notify**: Confirm to the user that the artifacts have been successfully saved to `docs/agent/antigravity/artifact`.
