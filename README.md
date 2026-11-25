# TaskFlow

TaskFlow is a private-project task tracker with a Kotlin/Spring Boot API and a React/TypeScript board. 

For Docker, copy `.env.example` to `.env`, replace `JWT_SECRET` with a random secret of at least 32 bytes, and run `docker compose up --build`. Visit http://localhost:3000 and Swagger at http://localhost:8080/swagger-ui.html. A generated API snapshot is in [backend/openapi.yaml](backend/openapi.yaml). PostgreSQL data persists in the `taskflow_data` volume.

For local development, use JDK 21, Gradle 8.14, Node 22 and PostgreSQL. Create a PostgreSQL database and user named `taskflow`, set `JWT_SECRET`, and run `gradle bootRun` in `backend/`. Override `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `JWT_EXPIRY_SECONDS` as needed. Run `npm install` and `npm run dev` in `frontend/`; Vite proxies the API to port 8080. Set `VITE_API_URL` to override the API base URL. Run tests with `gradle test` and `npm test` in their respective directories.

Flyway seeds a demo account: `demo@taskflow.local` / `demo12345`. These credentials are for local demonstrations only.


