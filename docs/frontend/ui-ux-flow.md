# Frontend UI/UX & User Flow

## 1. User Interface Structure

### Seat Selection Screen (`/matches/[matchId]`)
The seat selection interface is designed with a Mobile-First approach, composed of the following vertical sections:

1.  **Match Info**: Displays Home vs Away teams, Stadium, and Date/Time at the top.
2.  **Zone Selection (Segment Control)**:
    *   **Level 1**: Area (e.g., Infield, Outfield) selection using a horizontal scrollable segment/tab bar.
    *   **Level 2**: Section (e.g., Red 101, Blue 202) selection using a secondary horizontal scrollable segment/tab bar (appears after Area selection).
3.  **Block Navigation & Status**:
    *   Displays current Block Name.
    *   **Connection Status**: The SSE (Real-time) connection status is displayed explicitly **under the Block Name** (e.g., "Live", "Reconnecting").
4.  **Seat Grid**: Scrollable area displaying the seats for the selected block.

### Mobile Navigation
- **Bottom Tab Bar**: Fixed at the bottom of the screen.
- **Items**: Home (Matches), Tickets, Profile.

## 2. Authentication Flow

### Protected Routes
The following pages require the user to be logged in. If an unauthenticated user attempts to access these pages, they must be redirected to the **Login Page** (`/log-in`).

1.  **Tickets** (`/tickets`): View my reservation history.
2.  **Profile** (`/profile`): View user profile and settings.
3.  **Seat Selection** (`/matches/[matchId]`): Accessing the seat map for booking.

### User Journey
1.  **Booking Flow**:
    *   User views **Match List** (Public).
    *   User clicks "Book Tickets".
    *   **Check**: Is user logged in?
        *   **No**: Redirect to `/log-in`. After login, redirect back to the intended match page.
        *   **Yes**: Proceed to `Seat Selection`.

2.  **Menu Access**:
    *   User clicks "Tickets" or "Profile" tab.
    *   **Check**: Is user logged in?
        *   **No**: Redirect to `/log-in`.
        *   **Yes**: Show respective page.

## 3. Interaction Details
- **Feedback**: When redirection occurs due to auth requirements, a Toast message (e.g., "Login required") may be shown.


## 4. Admin Features (Integrated)

### Match List Extension
For users with `ADMIN` role, the Match List page (`/matches` or Home) includes additional controls:

1.  **Create Match**: A "Create Match" button (e.g., FAB or Header Action) to open a creation dialog/modal.
2.  **Match Item Actions**: Each match card displays admin-only actions:
    - **Open**: Changes status from `DRAFT` to `OPEN`.
    - **Edit**: Opens the edit dialog.
    - **Delete**: Removes the match (confirmation required).

