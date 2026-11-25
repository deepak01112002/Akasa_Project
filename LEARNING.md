# Learning TaskFlow

Follow a request from `frontend/src/App.tsx`: a user submits a form or moves a task. `frontend/src/api.ts` adds the stored JWT. The filter in `Security.kt` verifies it and supplies a user ID. A controller validates the request and calls a service. The service checks ownership, applies rules, and uses the repositories in `Repositories.kt`. JPA maps `Models.kt` to tables created by Flyway. The service maps the result to a DTO; React Query refreshes the board.

Read `Models.kt` alongside `V1__schema.sql` and trace the foreign keys. Then trace registration and login through `Api.kt`, `Services.kt`, and `Security.kt`. In `TaskApi.kt`, follow the ownership checks and the status-change transaction that records an activity. In `App.tsx`, compare dragging a card with choosing a status: both call the same API. Read and run the tests, then change a validation rule and see which tests fail.

For interview practice, explain the create-task flow from click to database and back; why passwords are hashed and entities are not returned; how another user's request is rejected; how token expiry works; and the trade-offs of local storage. Explain how pagination affects the columns and why the summary can differ from filtered results.

Practice extensions: add project members, task comments, recurring tasks, viewer/editor roles, notifications, and live board updates. For each, describe the migration, API contract, authorization, UI states, tests, and rollout before coding.

## Trace a task creation

1. In `App.tsx`, `TaskForm` registers fields with React Hook Form and validates them using the Zod schema. A valid submission invokes the `save` mutation. For a new task, the mutation sends a POST to `/projects/{id}/tasks` through `api`.
2. In `api.ts`, Axios adds `/api` to the path and attaches the JWT as a Bearer token. The server's `JwtFilter` verifies the signature and expiration and stores the user ID in Spring Security's context.
3. `TaskController.create` receives that ID and the validated `TaskRequest`. Bean Validation rejects a blank or overlong title before the service runs.
4. `TaskService.create` loads the project and compares its owner ID with the token's user ID. It checks the due date and assignee, saves the task, and writes a creation activity in the same transaction. If a database operation fails, the transaction rolls both changes back.
5. The service returns a `TaskDto`, not a JPA entity. The browser receives the response and React Query invalidates the board query, which reloads the task list and summary.

## Read the code with a debugger

Set breakpoints in `AuthController.login`, `JwtFilter.doFilterInternal`, `TaskController.create`, and `TaskService.create`. Sign in, create a task, and inspect the request body, authenticated ID, project owner, saved task, and returned DTO at each breakpoint. Compare a request with an expired token and one for another owner's project. Then step through a status change and find the corresponding row in `task_activities`.

## Interview rehearsal

- **Why JWT?** It lets the server verify a signed, time-limited identity on each request without a server-side session. It does not replace authorization: every project and task request still checks ownership.
- **Why DTOs?** They define the public contract independently of persistence, avoid leaking password hashes and lazy relationships, and give validation a clear boundary.
- **Why a transaction for status changes?** Updating the task and writing its activity should succeed or fail together. Discuss concurrent edits and optimistic locking as a possible extension.
- **Why React Query?** It tracks loading and error states and refreshes cached server data after mutations. Filters and form inputs remain local component state.
- **Live extension: project members.** Add a membership table and role, migrate the schema, define invitation and membership endpoints, change all project/task authorization checks, restrict assignees to members, add UI and tests for owner/member/nonmember access. Consider invitation expiry, removal, and ownership transfer before coding.
- **Live extension: comments.** Add a comment entity with task, author, text and timestamp; validate input; authorize via the task's project; expose list/create endpoints; render loading and errors; test cross-project access.
- **Live extension: real-time updates.** Start with polling and React Query invalidation; if latency or traffic requires it, evaluate server-sent events or WebSockets. Authenticate subscriptions and ensure users receive only their own projects' events.
