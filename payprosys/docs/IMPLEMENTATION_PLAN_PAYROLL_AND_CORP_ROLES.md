# Implementation Plan: Payroll Sub-pages & Corporate Roles (CA / CO / CR)

## Clarifications (please confirm)

1. **Batch status after CR reject**  
   When CR rejects a **submitted** batch, should that batch’s records **disappear from Employee payments** (so only SUBMITTED or APPROVED batches count), or only the status label change (data still visible)?

2. **Definition of “submitted” for Employee payments**  
   Should Employee payments show records from:
   - **Option A:** Batches with status **SUBMITTED** (CO submitted, pending CR), **or**
   - **Option B:** Batches with status **APPROVED** only (after CR approves)?  
   *(Assumption in plan: Option A — records appear as soon as CO submits; CR approve/reject updates status and, if you confirm (1), CR reject could hide the batch from Employee payments.)*

3. **CORP_USER**  
   Replace **CORP_USER** with **CORP_OPERATOR** and **CORP_REVIEWER** (and keep **CORP_ADMIN**)? Any existing CORP_USER accounts would need migration to CO or CR.

---

## Scope Summary

| Area | What |
|------|------|
| **Nav** | Rename “Payroll Upload” → “Payroll”; two sub-routes: Upload, Employee payments. |
| **Upload page** | Upload section + Recent batches (View / Submit / Reject; status after action; only submitted batches feed Employee payments). |
| **Employee payments page** | Filters: month, batch; only submitted (or approved) batches; DataTable with pagination + sorting. |
| **Corporate roles** | CA, CO, CR. All can view corporate users. CA adds CO/CR. CO: upload, create batch, submit/reject, re-upload on rejected, view Employee payments. CR: view Employee payments, approve/reject whole batch. |

---

## Step-by-Step Implementation Plan

### Phase 1: Backend — Batch status & roles

1. **Migration**  
   - Add `status` to `payroll_batches`: e.g. `DRAFT | SUBMITTED | APPROVED | REJECTED`.  
   - Seed new roles: `CORP_OPERATOR`, `CORP_REVIEWER`. Decide whether to keep or drop `CORP_USER` (and migrate existing users).

2. **Entity & DTO**  
   - Add `PayrollBatchStatus` enum and `status` on `PayrollBatch`.  
   - Include `status` in `PayrollBatchDto`.  
   - Optionally: `reviewed_by_id`, `reviewed_at` for CR actions.

3. **Service & API**  
   - Upload: create batch in **DRAFT**.  
   - Endpoints: **Submit batch** (CO, DRAFT → SUBMITTED), **Reject batch** (CO: DRAFT → REJECTED; CR: SUBMITTED → REJECTED), **Approve batch** (CR, SUBMITTED → APPROVED).  
   - **Re-upload for rejected batch** (CO): replace records for batch in REJECTED, set back to DRAFT (no history).  
   - **List batches**: by corporate, with status; filter by status for “recent batches” and for Employee payments.  
   - **List records for Employee payments**: only from batches with status SUBMITTED (and optionally APPROVED); support filter by month and by batch id.  
   - **List records in a batch** (for “View”): by batch id, any status.

4. **Authorization**  
   - Payroll upload / create batch / submit / reject (own batch): **CORP_OPERATOR**.  
   - Approve / reject submitted batch: **CORP_REVIEWER**.  
   - Get batches / records: CO and CR (and CA if desired).  
   - User create (CO/CR): **CORP_ADMIN** only.  
   - Replace existing CORP_USER usage with CO/CR where appropriate.

### Phase 2: Frontend — Payroll layout & Upload page

5. **Routing & nav**  
   - Rename “Payroll Upload” → “Payroll”.  
   - Routes: `/payroll` (redirect to `/payroll/upload` or default tab), `/payroll/upload`, `/payroll/employee-payments`.  
   - Payroll nav item with sub-links or tabs: Upload | Employee payments.

6. **Upload page**  
   - **Upload section**: unchanged in essence (month + file); creates batch in DRAFT.  
   - **Recent batches**: table with columns e.g. Month, File, Records, Amount, Status, Actions.  
   - **Actions**:  
     - **View**: open modal or drawer with list of employees (records) in that batch.  
     - **Submit** (CO only, batch in DRAFT): call submit API; then show status, hide Submit/Reject.  
     - **Reject** (CO only, batch in DRAFT): call reject API; then show status, hide buttons.  
     - **Re-upload** (CO only, batch in REJECTED): same as new upload but target batch id; replace records, set DRAFT.  
   - After Submit/Reject, replace action buttons with status badge (SUBMITTED / REJECTED / APPROVED).

7. **Employee payments page**  
   - Filters: **Month** (dropdown), **Batch** (dropdown; only submitted/approved batches).  
   - DataTable: columns (e.g. Employee name, Account number, Joining date, CPR ID, Amount, Payment for, Batch, Month).  
   - Server-side or client-side **pagination** and **sorting** (implement per stack; e.g. React Table or similar).  
   - Data source: existing “records by corporate” API extended with status filter and optional batch filter; only batches in SUBMITTED (and optionally APPROVED) included.

### Phase 3: Corporate roles & users

8. **Backend**  
   - Create user: allow **CORP_OPERATOR** and **CORP_REVIEWER** as role options when caller is CORP_ADMIN.  
   - By-corporate user list: allow CO and CR (all corporate users can view list).  
   - Adjust PayrollController and any filters so CO can upload/submit/reject; CR can approve/reject.

9. **Frontend**  
   - **Users page** (corporate): all corporate users (CA/CO/CR) see the same list.  
   - **Create user** (CA only): role dropdown includes Operator and Reviewer (no longer “Corporate user” if CORP_USER removed).  
   - **Payroll**: show Upload and Employee payments to CO and CR; hide or disable Submit/Reject/Approve by role.

### Phase 4: Polish & README

10. **README**  
    - Update main README: Payroll has Upload and Employee payments; corporate roles CA / CO / CR and their permissions.  
11. **FUTURE_FEATURES.md**  
    - Already added; update when batch history, per-record review, or workflows are scoped.

---

## Order of work (brief)

1. DB migration (status, roles).  
2. Batch entity + status enum + DTOs.  
3. Payroll service: submit / reject / approve / re-upload; list batches/records with status filters.  
4. Payroll controller: new endpoints; auth by CO/CR.  
5. Frontend: Payroll routes + nav; Upload page with recent batches and View/Submit/Reject/Re-upload.  
6. Frontend: Employee payments page with filters and DataTable (pagination + sort).  
7. Corporate roles: backend create-user and list; frontend Users page and Payroll visibility by role.

Once clarifications (1)–(3) are confirmed, implementation can follow this plan as-is.
