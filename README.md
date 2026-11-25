# TaskFlow

TaskFlow is a private-project task tracker with a Kotlin/Spring Boot API and a React/TypeScript board. See [LEARNING.md](LEARNING.md) for a guided walkthrough and interview practice.

For Docker, copy `.env.example` to `.env`, replace `JWT_SECRET` with a random secret of at least 32 bytes, and run `docker compose up --build`. Visit http://localhost:3000 and Swagger at http://localhost:8080/swagger-ui.html. A generated API snapshot is in [backend/openapi.yaml](backend/openapi.yaml). PostgreSQL data persists in the `taskflow_data` volume.

For local development, use JDK 21, Gradle 8.14, Node 22 and PostgreSQL. Create a PostgreSQL database and user named `taskflow`, set `JWT_SECRET`, and run `gradle bootRun` in `backend/`. Override `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `JWT_EXPIRY_SECONDS` as needed. Run `npm install` and `npm run dev` in `frontend/`; Vite proxies the API to port 8080. Set `VITE_API_URL` to override the API base URL. Run tests with `gradle test` and `npm test` in their respective directories.

Flyway seeds a demo account: `demo@taskflow.local` / `demo12345`. These credentials are for local demonstrations only.

The controller validates request DTOs and delegates to transactional services; services enforce project ownership and call repository interfaces. The mapper converts entities to response DTOs. Spring Security checks signed JWTs before protected controllers run. TanStack Query manages server state and invalidates cached board data after mutations; Axios centralizes requests and expired-session handling. The task form uses React Hook Form and Zod for client-side validation, with the API enforcing the same rules independently.

Spring Data repositories use the Repository pattern, `DtoMapper` acts as a DTO factory, and `TaskSortStrategy` selects the due-date or priority ordering strategy. JPA Specifications compose task-filter predicates. The project owner is the only eligible assignee. Overdue means a non-Done task with a due date before the server's current date. Pagination covers the filtered list across columns, while the summary covers the whole project. Priority sorting puts High first, then Medium, then Low.

The JWT is kept in local storage for this demo, which exposes it to injected JavaScript. For production, use a stronger session design, hardened CSP, token revocation and refresh, rate limiting, stricter CORS, and a full PostgreSQL integration suite. Never deploy the demo credentials.
