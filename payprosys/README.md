# PayProSys — Payroll Processing System

Production-structured starter for a full-stack **Payroll Processing System** with multi-tenant hierarchy: **Bank → Corporate → Users → Payroll**.

**Convention:** Keep **`main`** as the source of truth. Whenever you **merge into `main`**, refresh the **Current state** section (and add rows to the doc table if new docs appear) so you can return weeks later and rely on this README alone.

---

## Current state

**Working in the app today**

- **Login** — Email + password; bearer token in the browser (demo-oriented; not production security). Login and `whoami` return optional **bank/corporate logo URLs** when set.
- **Super Admin** — Create banks (and bundled bank admin user); manage banks and users as designed.
- **Bank** — List/create **corporates** for their bank; create **corporate admin**; manage bank-side users. Payroll: **Inbox**, **History**, **Payments** (completed batches), **Workflow settings** (review levels and assignments). **Profile** — change password; set **bank logo URL** (https or small `data:image/…`) for header branding.
- **Corporate** — Manage **users**; **Upload** (Excel `.xlsx`): choose **year**, **month**, **batch type** (payroll vs vendor), file, then **Upload**; **draft** batches listed on Upload with **Submit** / **Delete**. After submit, work moves to **Inbox** (L1…Ln corporate review, send to bank, etc.). **History** — closed batches (rejected or bank process payment) with **month** filter; bank users also filter by **corporate**. **Payments** — completed batches only; filters (batch type, months multi-select, corporate for bank); **View** opens a **dialog** with totals and payment lines (no navigation to full batch detail from this screen). **Workflow settings** for corporate admins. **Profile** — password + **corporate logo URL**.
- **Payroll workflow (POC)** — Multi-step **corporate** and **bank** review, **inbox** vs **read-only view**, **remark timeline**, **send to bank** with bank-visible remarks, bank approve / send back / **process payment** (terminal). **Batch detail** page drives actions when the batch is in flight; **terminal** batches (completed / rejected / process payment) hide workflow actions but still load **payroll lines** for audit (including from History).
- **Data model** — Flyway migrations through **V19** (workflow tables, batch kind `PAYROLL`/`VENDOR`, `COMPLETED` status, bank/corporate `logo_url`, etc.). New banks/corporates get default workflow steps.
- **Run locally** — `payprosys/devops/docker-compose.yml` (Postgres + API + static UI on ports **3000** / **8080** / **5432**).

**Docs / backlog**

- **`docs/WORKFLOW_REQUIREMENTS.md`** — Workflow rules and scope (keep in sync as behavior evolves).
- **`FUTURE_FEATURES.md`** — Backlog and “not in this POC” items. Other docs under **`docs/`** (business, demo guide, overview pack) will be refreshed in a future pass.

---

## Documentation (what each file is for)

| Location | What it is |
|----------|------------|
| **docs/BUSINESS_REQUIREMENTS.md** (+ `.txt`) | Short, non-technical description of the product for stakeholders. |
| **docs/DEMO_AND_USER_GUIDE.md** | Step-by-step order to run through a demo (who logs in, what to click). |
| **docs/WORKFLOW_REQUIREMENTS.md** | Corporate/bank workflow: approvers, inbox, states, visibility (implementation tracks this doc). |
| **docs/PayProSys_overview_pack.*** (`html` / `pdf` / `docx`) | One pack: wireframes + story. Regenerate PDF from the `.html` with Chrome `--print-to-pdf` if you edit it. |
| **docs/IMPLEMENTATION_PLAN_PAYROLL_AND_CORP_ROLES.md** | Older / alternate implementation notes; kept for reference. |
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

**Monorepo:** One repository for backend, frontend, and devops. Suitable for shared CI and local/Docker runs; split later if needed.

## Tech stack

| Layer    | Stack |
|----------|--------|
| Backend  | Java 21, Spring Boot 3.x, Spring Web, Data JPA, AOP, Lombok, Validation, PostgreSQL, Maven, Flyway, Apache POI; demo **Bearer** session filter (not full Spring Security) |
| Frontend | React, Vite, Axios, React Router, CSS |
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

Uses `application-dev.yml` (DB: `localhost:5432/payprosys`, Flyway).

### 3. Run frontend locally

```bash
cd payprosys/frontend
npm install
npm run dev
```

Open http://localhost:3000. Vite proxies `/api` to `http://localhost:8080`.

### 4. First login (demo users)

Plain-text passwords for demo (see Flyway seeds):

| Role         | Email                     | Password  | Use |
|--------------|----------------------------|-----------|-----|
| **SUPER_ADMIN** | `superadmin@payprosys.com` | `password` | Banks and super-admin flows. |
| **BANK_ADMIN**  | `admin@payprosys.com`      | `password` | Default bank; corporates and bank payroll settings. |

Corporate users are created when a bank adds a corporate and admin. Corporate **payroll users** (`CORP_USER`) can upload; **corporate admins** manage users and workflow settings.

---

## Backend layout

- **controller** — REST; base path `/api`; standard wrapper `{ success, message, data }`. Includes **Auth**, **Payroll** (upload, batches, records), **PayrollWorkflow** (inbox, history, transitions, review events), **WorkflowConfig**, **Profile** (organization branding, change password).
- **service** — Transactional business logic.
- **repository** — Spring Data JPA.
- **entity** — JPA entities (Bank, Corporate, User, PayrollBatch, PayrollRecord, workflow step/review/assignment entities, etc.).
- **dto** — Request/response DTOs.
- **mapper** — Entity ↔ DTO.
- **config** — CORS, filter registration.
- **auth** — Simple session store, auth filter, role checks.
- **aop** — Service method logging.
- **exception** — Global handler and `ApiResponse` error format.

**Auth (demo):**  
- `POST /api/auth/login` — returns token; passwords plain in DB for demo.  
- `GET /api/auth/whoami` — principal, roles, bank/corporate ids and names, logo URLs.  
- `POST /api/auth/activate` — optional activation path.  
- **`/api/profile/organization`**, **`PATCH .../branding`**, **`POST .../change-password`** — profile and branding (see `ProfileController`).  
- Other `/api/**` calls expect `Authorization: Bearer <token>`.

**Profiles:** `dev` (local DB, verbose logging), `stage` (Docker/env vars), `prod` (no DDL, env-only).

---

## Frontend layout

- **Layout** — Sidebar brand (`public/logo.svg`), role-based nav: Dashboard, Banks, Corporates, Users; for payroll roles: **Inbox**, **History**, **Upload**, **Payments**; **Workflow settings** (corp/bank admins); **Profile**. Header shows org name and optional org logo.
- **Pages** — Login; Banks; Corporates; Users; payroll flow pages above; **Profile** (password + logo URL for bank/corporate).
- **API** — Axios with `Authorization: Bearer <token>`, 401 → redirect to login.

---

## Excel upload

- **Format:** `.xlsx` only.
- **Columns:** Employee Name, Account Number, Joining Date, CPR ID, Amount; optional **Payment for** (case-insensitive headers).
- **Batch type** — `PAYROLL` or `VENDOR` (stored on the batch; shown on detail and used in Payments filters).
- Backend uses **Apache POI**; persists batch + rows; new uploads are **drafts** until **Submit** on the Upload page.

---

## CI/CD (GitHub Actions)

- **Backend:** On push/PR to `main`/`develop` under `payprosys/backend/**`: Maven build, tests, Docker image build (no push).
- **Frontend:** On push/PR under `payprosys/frontend/**`: `npm ci`/`npm install`, build, Docker image build (no push).

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

- **Backend running locally** (`mvn spring-boot:run`): logs go to the **same terminal** where you started the backend.
- **Backend in Docker**: `cd payprosys/devops && docker compose logs -f backend`

With `dev` profile, verbose logging and SQL are enabled in dev config.

### 2. Browser (frontend / network)

- DevTools → **Network** — confirm `POST /api/auth/login` returns 200 and response includes `token`.

### 3. Seed user and backend reachability

- Flyway must have applied migrations (start backend once against an empty DB).
- Frontend must proxy `/api` to the backend (Vite dev server or Docker nginx).

### 4. Quick checks

| Check | Command / action |
|------|-------------------|
| Backend up? | `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login` (405 or 401 is OK; connection refused = backend down). |
| DB + migrations | Backend startup logs should show Flyway success. |

### 5. API returns 403 after login

Role mismatch for the route (e.g. super-admin-only). Check `whoami` / login response **roles** and the Network tab **Authorization** header.

---

## Notes

- **Layered backend**, DTOs, no entities in controllers.
- **Constructor injection** in backend services where used; Lombok `@RequiredArgsConstructor` on components.
