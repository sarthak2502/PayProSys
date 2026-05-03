# PayProSys — Demo script & user guide

Use this when **walking someone through** the app—especially **after** a fresh database or **`docker compose` first run**.

---

## Important: “Empty database” vs first boot

After you start the backend, **Flyway migrations run automatically**. They usually:

1. Create **tables** (`V1`).
2. Insert **roles** (`V2`).
3. Optionally seed **Default Bank** + **Bank Admin** (`V3`).
4. Normalise passwords to plain demo (`V5`) where applicable.
5. Seed **Super Admin** (`V6`, `V7`).

So you **do not** hand-insert Super Admin SQL unless you stripped migrations—you get **superadmin@payprosys.com** from seeds.

Some environments also run **`V9`** reset (all data cleared **except** super admin)—then you only keep Super Admin; you must recreate banks/corporates in the UI.

**Default demo logins after a normal seed** (unless your migrations differ):

| User | Password | Role |
|------|-----------|------|
| `superadmin@payprosys.com` | `password` | SUPER_ADMIN |
| `admin@payprosys.com` | `password` | BANK_ADMIN (@ Default Bank)—*if `V3` ran and was not wiped* |

Always confirm active users via Super Admin → **Users** or direct DB query if unsure.

---

## Recommended demo sequence (cold start)

Follow this **order**. Each step implies logging in **as that persona** unless noted.

### Step 1 — Super Admin (`superadmin@payprosys.com` / `password`)

1. Open the app (**e.g. http://localhost:3000**).
2. Log in as Super Admin.
3. Sidebar: **Dashboard** (optional), **Banks** (visible for Super Admin).

**Demonstrate:**

- **Add Bank** — Bank name + first **Bank Admin** (email, password, name).
  - Explain: Only Super Admin creates **banks**; each bank gets an admin login to hand to the bank.

4. Sidebar: **Users** — show **All users** (including demo passwords column on this screen *for testing*).

---

### Step 2 — Bank Admin (`<bank‑admin‑email>` / `<password you set>`)

1. Log out; log in as the Bank Admin created in Step 1.
2. Header should show something like **PayProSys (Bank: &lt;Bank Name&gt;)**.

**Demonstrate:**

- **Corporates** — List is **only for this bank**.
- **Add Corporate** — Corporate name + first **Corporate Admin** + optional **assigned bank users** (multi-select).
- **Users** — List shows **bank users only** for this bank; Bank Admin can **create user** (another bank admin or bank user).

3. Optionally log in as a **Corporate Admin** immediately after noting the credentials shown on-screen.

---

### Step 3 — Corporate Admin (`<corp‑admin email>` / `<password you set>`)

1. Log in as Corporate Admin.
2. Header: **Corporate: &lt;Corporate Name&gt;**.

**Demonstrate:**

- **Users** — List **corporate** users only; **Create user** (corporate user / admin roles as configured).
- **Payroll / Employee payments** — As implemented in your build:
  - Choose **month**.
  - Upload **.xlsx** (see README Excel format; sample **payprosys/sample-payroll-employees.xlsx** in repo).
  - View **recent batches** and **employee payment lines**.

---

### Step 4 — Bank User (optional)

### Step 5 — Corporate User (`CORP_USER`, optional)

Demonstrate narrower permissions: Bank User can usually see corporates under that bank without seeing all payroll across corporates depending on RBAC in your build; Corporate users scoped to payroll and users within their corporate.

---

## One-page cheat sheet for the presenter

| # | Actor | Goal |
|---|--------|------|
| 1 | Super Admin | Create **bank + bank admin** |
| 2 | Bank Admin | Create **corporate + corporate admin**; optional bank users |
| 3 | Corporate Admin / User | Manage **corporate users**; **payroll Excel** + **employee payments** |

---

## Troubleshooting tips

| Symptom | Check |
|---------|--------|
| Cannot log in after fresh DB | Migrations ran? Use `superadmin@payprosys.com` / `password`. |
| 403 on banks | You are not logged in as Super Admin. |
| 403 / empty corporates | Logged-in user has no bank (wrong persona). |
| Payroll upload fails on row | Column headers spelling; `.xlsx` only; Joining Date format; rebuild backend if date-parse fix applied. |

**Logs:**

```bash
cd payprosys/devops
docker compose logs -f backend
```

---

## After the demo

- Log **out**.
- Optionally reset demo data (**V9‑style**) or truncate via documented migration—only do this if your team agrees (it wipes non–super‑admin users and related rows).
