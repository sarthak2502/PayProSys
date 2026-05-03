# PayProSys — Future / Potential Features

This document lists planned or potential features that are **not** in the current scope. Use it for prioritisation and when picking up new work.

---

## Corporate

- **Configurable roles & workflows (POC — specified)** — Corporate Admin / Bank Admin each configure **linear L1…Ln** (display names) and assign users to levels; Super Admin does not configure workflows. **Inbox** / **History**, remarks (no file re-upload in POC), **approve vs send to bank**, bank visibility of **only** send-to-bank remarks. See **`docs/WORKFLOW_REQUIREMENTS.md`** for locked decisions, state sketches, and rollout sections.
- **Per-record review** — Reviewer approves/rejects individual employee lines in a batch, not only the whole batch. (Future; after batch-level workflow is stable.)

## Payroll / Batches

- **Batch history and comparison** — Versions after re-upload on rejected batches; diff between versions. (Future.)
- **Audit trail** — Immutable log of who acted when (submit, approve, send back, send to bank). Can complement remarks history.
- **Corporate / bank state machine** — Replace or extend today’s simple `PENDING` / `SUBMITTED` with a fuller lifecycle, e.g. corporate: *New → In review → Clarification → Approved / Rejected → Sent to bank*; bank: *New → In review → Sent to corporate / Clarification → Approved → Process payment*. Exact transitions TBD with product.

## Bank

- **Bank-side approval chain** — Mirror of corporate inbox/review after batch is sent to bank; send back to corporate at L1 with return to **last corporate level** (TBD). (Planned with corporate workflow.)

## General

- **Activation flow** — New users currently start as ACTIVE; optional email-based activation could be reintroduced.
- **Password policy** — Enforce strength, expiry, or reset flow.
- **Reporting / exports** — Export payroll or employee payments to Excel/PDF; **partial:** CSV download from Employee Payments (batch list + line items in browser).
- **Inbox UX** — Dedicated menu; filters by “action required for my role”; optional **History** tab for batches the role has touched (including view-only after send-back).

---

## Earlier “quick win” backlog (from engineering triage)

- **Server-side or paged exports** — For very large batches, generate CSV/Excel on the server.
- **Pagination / sorting** — Employee payments and batch tables at scale.
- **Wireframe alignment** — Ongoing UI polish vs stakeholder pack.

---

*Last updated: multi-level role/workflow + state transitions (planning phase; no dev committed until clarifications are agreed).*
