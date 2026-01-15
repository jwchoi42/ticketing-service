# Frontend Development Rule

## Backend Isolation Rule
- **STRICT RESTRICTION**: When assigned a task related to the frontend (in the `frontend/` folder), the AI agent **MUST NOT** modify any files in the backend source code directory (`src/`).
- **Reason**: To maintain a clear separation between frontend and backend work and prevent accidental side effects in the core business logic while working on the UI.

## Handling Backend Constraints
If a frontend task cannot be completed or is blocked due to backend limitations (e.g., missing API, incomplete data fields, incorrect response schema, or bugs in the API), the agent must follow these steps:

1. **Identify and Explain**: Clearly identify the specific backend constraint and explain why it blocks the current frontend task.
2. **Stop Modification**: Do not attempt to fix or modify the backend code yourself.
3. **Request User Input**: Inform the User of the situation and explicitly ask for a prompt input or guidance on how to proceed.
   - Possible options to suggest to the User:
     - Providing mock data for the missing/incomplete part.
     - Skipping the specific feature until the backend is updated.
     - Asking the User to implement the necessary backend changes.
