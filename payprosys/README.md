# PayProSys — Payroll Processing System

Production-structured starter for a full-stack **Payroll Processing System** with multi-tenant hierarchy: **Bank → Corporate → Users → Payroll**.

**Convention:** Keep **`main`** as the source of truth. Whenever you **merge into `main`**, refresh the **Current state** section (and add rows to the doc table if new docs appear) so you can return weeks later and rely on this README alone.

---

## Current state

**Working in the app today**

- **Login** — Email + password; bearer token in the browser (demo-oriented; not production security).
- **Super Admin** — Create banks (and bundled bank admin user).
- **Bank** — List/create **corporates** for their bank; create **corporate admin**; manage bank-side users; optional corporate–bank user links.
- **Corporate** — Manage **users**; **Payroll upload** (`.xlsx`, pick month); each upload is a **batch** (starts **pending**). **Submit** or **delete** while pending; after submit, delete is blocked. **Employee Payments** menu: **Pending** vs **Submitted** batch lists; open payment lines; **CSV** export (batch list + lines in the modal). **Banks** only see **submitted** batches and those lines.
- **Run locally** — `payprosys/devops/docker-compose.yml` (Postgres + API + static UI on ports **3000** / **8080** / **5432**).

**Not implemented yet** (see **`docs/WORKFLOW_REQUIREMENTS.md`**; backlog **`FUTURE_FEATURES.md`**)

- Multi-level **L1…Ln** review, **Inbox/History**, remark threads, corporate **approve vs send to bank** / send-to-bank flow, full bank approval chain.

---

## Documentation (what each file is for)

| Location | What it is |
|----------|------------|
| **docs/BUSINESS_REQUIREMENTS.md** (+ `.txt`) | Short, non-technical description of the product for stakeholders. |
| **docs/DEMO_AND_USER_GUIDE.md** | Step-by-step order to run through a demo (who logs in, what to click). |
| **docs/WORKFLOW_REQUIREMENTS.md** | Agreed **next** POC scope: corporate/bank linear approvers, inbox, states, visibility rules (implementation not started on `main` at time of writing). |
| **docs/PayProSys_overview_pack.*** (`html` / `pdf` / `docx`) | One pack: simple wireframes + same story as the business doc (good to attach to an intro email). Regenerate PDF from the `.html` with Chrome `--print-to-pdf` if you edit it. |
| **docs/IMPLEMENTATION_PLAN_PAYROLL_AND_CORP_ROLES.md** | Older / alternate implementation notes (operator vs reviewer); kept for reference. |
| **FUTURE_FEATURES.md** | Backlog and “not in this POC” items. |

---

## Repository structure

```
payprosys/
  backend/          # Spring Boot 3.x (Java 21)
  frontend/         # React + Vite
  devops/           # Docker Compose
  .github/          # GitHub Actions CI
```

**Monorepo:** This layout is a **monorepo**: one repository containing backend, frontend, and devops. It is suitable at this stage because:

- Single place for code, issues, and PRs
- Shared CI workflows and versioning
- Easy local and Docker-based runs
- When you add more services or move to AWS, you can keep the monorepo or split later

## Tech stack

| Layer    | Stack |
|----------|--------|
| Backend  | Java 21, Spring Boot 3.x, Spring Web, Data JPA, AOP, Lombok, Validation, PostgreSQL, Maven, Flyway, Apache POI; demo **Bearer** session filter (not full Spring Security) |
| Frontend | React (latest), Vite, Axios, React Router, CSS |
| DevOps   | Docker, Docker Compose, GitHub Actions (build + test + Docker image) |

---

## Quick start

### Prerequisites

- **Java 21**, **Maven 3.9+**, **Node.js 20+**, **Docker & Docker Compose**, **PostgreSQL 16** (or use Docker only).

### 1. Run with Docker Compose (recommended)

From the **repository root** (parent of `payprosys`):

```bash
cd payprosys/devops
docker compose up -d
```

- **Backend:** http://localhost:8080  
- **Frontend:** http://localhost:3000  
- **Postgres:** localhost:5432 (user/pass: payprosys/payprosys)

Frontend talks to the backend via the proxy in `docker-compose` (frontend nginx → backend service).

### 2. Run backend locally

```bash
cd payprosys/backend
# Ensure Postgres is running (e.g. docker compose up -d postgres from devops)
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

Uses `application-dev.yml` (DB: `localhost:5432/payprosys`, Flyway, JWT dev secret).

### 3. Run frontend locally

```bash
cd payprosys/frontend
npm install
npm run dev
```

Open http://localhost:3000. Vite proxies `/api` to `http://localhost:8080`.

### 4. First login (demo users)

Two seed users (plain-text password for demo):

| Role         | Email                     | Password  | Use |
|--------------|----------------------------|-----------|-----|
| **SUPER_ADMIN** | `superadmin@payprosys.com` | `password` | Create banks and assign bank admin credentials. Sees list of banks with each bank admin's login. |
| **BANK_ADMIN**  | `admin@payprosys.com`      | `password` | Belongs to Default Bank. Sees only **Corporates** (no Banks). Can create corporates and assign corporate admin credentials. |

- **Super admin** logs in → sees **Banks** only: add bank (with bank admin email/password), view all banks and their admin login details.
- **Bank admin** logs in → sees **Corporates** only: add corporate (with corporate admin email/password), view corporates for their bank and admin credentials.
- **Corporate admin** (created by bank) → sees Users, Payroll for their corporate.

---

## Backend layout

- **controller** — REST; base path `/api`; standard wrapper `{ success, message, data }`.
- **service** — Transactional business logic.
- **repository** — Spring Data JPA.
- **entity** — JPA entities (Bank, Corporate, Role, User, PayrollBatch, PayrollRecord).
- **dto** — Request/response DTOs; entities are not exposed.
- **mapper** — Entity ↔ DTO.
- **config** — CORS, filter registration.
- **auth** — Simple session store, auth filter, role checks (no Spring Security).
- **aop** — Service method logging (START/END).
- **exception** — Global handler and `ApiResponse` error format.
- **filter** — Request logging (method, URI, status, time).

**Auth (demo):**  
- `POST /api/auth/login` — returns token; passwords plain in DB for demo.  
- `POST /api/auth/activate` — optional activation path.  
- Other `/api/**` calls expect `Authorization: Bearer <token>`. Controllers enforce roles (Super Admin → banks; Bank → corporates; Corporate → users & payroll).

**Profiles:** `dev` (local DB, verbose logging), `stage` (Docker/env vars), `prod` (no DDL, env-only).

---

## Frontend layout

- **Layout** — Header (PayProSys, notification placeholder, user dropdown), sidebar (role-based: Dashboard, Banks, Corporates, Users, **Payroll upload**, **Employee payments**).
- **Pages** — Login (email/password, token in `localStorage`), Banks, Corporates, Users, **Payroll upload** (month + file, batches with submit/delete), **Employee payments** (tabs, view lines, CSV).
- **API** — Axios instance with `Authorization: Bearer <token>`, 401 → redirect to login.

---

## Excel upload

- **Format:** `.xlsx` only.
- **Columns:** Employee Name, Account Number, Joining Date, CPR ID, Amount; optional **Payment for** (case-insensitive headers).
- Backend uses **Apache POI**; persists batch + rows; batch starts **pending** until corporate **submit**s.

---

## CI/CD (GitHub Actions)

- **Backend:** On push/PR to `main`/`develop` under `payprosys/backend/**`: Maven build, tests, Docker image build (no push).
- **Frontend:** On push/PR under `payprosys/frontend/**`: `npm ci`/`npm install`, build, Docker image build (no push).

Workflows are prepared for future AWS deployment (e.g. add deploy step when ready).

---

## Run steps summary

| Goal              | Command / step |
|-------------------|-----------------|
| Full stack        | `cd payprosys/devops && docker compose up -d` |
| Backend only      | Postgres up → `cd payprosys/backend && mvn spring-boot:run` (profile `dev`) |
| Frontend only     | `cd payprosys/frontend && npm install && npm run dev` |
| Run tests         | `cd payprosys/backend && mvn test` |

---

## Troubleshooting — login failing / how to check logs

### 1. Backend logs

- **Backend running locally** (`mvn spring-boot:run`): logs go to the **same terminal** where you started the backend. Look for lines around the time you click “Sign in” (e.g. `Authentication failed`, `Invalid email or password`, SQL or stack traces).
- **Backend in Docker**: view backend container logs:
  ```bash
  cd payprosys/devops
  docker compose logs -f backend
  ```
  Use `Ctrl+C` to stop following.

With `dev` profile, `application-dev.yml` already sets `logging.level.com.payprosys: DEBUG` and `show-sql: true`, so auth and SQL are logged.

### 2. Browser (frontend / network)

- Open **DevTools** (F12 or right‑click → Inspect) → **Network** tab.
- Try logging in with **admin@payprosys.com** / **password**.
- Find the **login** request (e.g. `login` or `auth/login`). Check:
  - **Status**: 401 = wrong credentials or user not found; 404/Connection refused = backend not reachable (e.g. not on port 8080).
  - **Response** body: often `{ "success": false, "message": "Invalid email or password" }` for auth failures.

### 3. Seed user and backend reachability

- **Seed user:** email `admin@payprosys.com`, password `password` (see `V3__seed_admin_user.sql`). Must be ACTIVE; Flyway must have run (DB exists and migrations applied).
- **Frontend → backend:** frontend calls `/api`; Vite proxies that to `http://localhost:8080`. If the backend is not running on **port 8080**, login will fail (e.g. connection refused in Network tab). Start backend with `SPRING_PROFILES_ACTIVE=dev` and ensure nothing else uses 8080.

### 4. Quick checks

| Check | Command / action |
|------|-------------------|
| Backend up? | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login` (405 or 401 is OK; connection refused = backend down). |
| DB + migrations | Backend startup logs should show Flyway migrations; if the DB is empty or new, run backend once so Flyway applies `V1`, `V2`, `V3`. |
| CORS / proxy | If you use Vite dev server, use the app at `http://localhost:3000` so `/api` is proxied to 8080; avoid calling `http://localhost:8080` from the browser if the frontend is on 3000. |

### 5. API returns 403 after login (Banks, Corporates, etc.)

If APIs return **403** while login succeeds, the token may be missing on the request or the logged-in user’s **role** does not match that screen (e.g. bank user calling super-admin-only routes). Check the Network tab request headers and `whoami` / login response roles.

---

## Notes

- **Clean, extensible structure** — layered backend, DTOs, no entities in controllers.
- **Constructor injection only** in backend; no field injection.
