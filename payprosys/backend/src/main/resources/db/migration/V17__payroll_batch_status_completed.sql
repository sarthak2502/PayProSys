-- Terminal batches after bank marks process payment use COMPLETED instead of SUBMITTED.
UPDATE payroll_batches
SET batch_status = 'COMPLETED'
WHERE bank_flow_state = 'BANK_PROCESS_PAYMENT'
  AND batch_status = 'SUBMITTED';
