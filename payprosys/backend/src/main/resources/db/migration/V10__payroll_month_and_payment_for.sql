-- Batch: month for which this payroll is (corporate admin selects month when uploading)
ALTER TABLE payroll_batches ADD COLUMN year_month INT;
CREATE INDEX idx_payroll_batch_year_month ON payroll_batches(year_month);

-- Record: payment details (e.g. salary, reimbursement)
ALTER TABLE payroll_records ADD COLUMN payment_for VARCHAR(500);
