ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS batch_status VARCHAR(20);

UPDATE payroll_batches SET batch_status = 'SUBMITTED' WHERE batch_status IS NULL;

ALTER TABLE payroll_batches ALTER COLUMN batch_status SET DEFAULT 'SUBMITTED';
ALTER TABLE payroll_batches ALTER COLUMN batch_status SET NOT NULL;
