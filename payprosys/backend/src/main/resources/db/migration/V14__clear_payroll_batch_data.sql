-- Remove all uploaded payroll data and workflow events (POC reset). Keeps banks, corporates, users, and workflow step/assignment config.

DELETE FROM payroll_batch_review_events;
DELETE FROM payroll_records;
DELETE FROM payroll_batches;
