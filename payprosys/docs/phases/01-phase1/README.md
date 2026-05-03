# Phase 1 — Workflow foundation (DB + model)

**Branch:** `feature/01-phase1-workflow-foundation`  
**Order:** `01` — first implementation slice after agreed requirements.

## Scope (from `docs/WORKFLOW_REQUIREMENTS.md`)

- Corporate / bank **workflow step** definitions (L1…Ln + display labels), **user ↔ level** assignment.
- Batch fields: **full lifecycle status**, **current step**, **uploader**, remarks model (internal vs **bank-facing** text on send-to-bank), minimal **actor/step** history for “past reviewers can view.”
- Flyway migrations; align or supersede current `PENDING` / `SUBMITTED` where needed.

## Out of Phase 1 (later phases)

- Inbox/History UI, transition APIs, bank chain UI — Phases 2–4.
