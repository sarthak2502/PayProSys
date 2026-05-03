-- Legacy rows could show PENDING while already handed to the bank.
UPDATE payroll_batches
SET batch_status = 'SUBMITTED'
WHERE corporate_flow_state = 'CORP_SENT_TO_BANK'
  AND batch_status <> 'SUBMITTED';
