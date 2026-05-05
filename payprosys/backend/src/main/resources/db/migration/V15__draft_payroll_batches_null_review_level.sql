-- Batches not yet submitted for corporate review should have no review level (L1 inbox excludes them).
UPDATE payroll_batches b
SET current_corporate_review_level = NULL
WHERE batch_status = 'PENDING'
  AND corporate_flow_state = 'CORP_NEW'
  AND current_corporate_review_level IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM payroll_batch_review_events e WHERE e.payroll_batch_id = b.id
  );
