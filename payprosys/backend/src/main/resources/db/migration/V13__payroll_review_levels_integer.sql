-- Hibernate maps Integer to INTEGER; V12 used SMALLINT. Align types for ddl-auto=validate (Docker/stage).
ALTER TABLE payroll_batches
    ALTER COLUMN current_corporate_review_level TYPE INTEGER USING current_corporate_review_level::integer;

ALTER TABLE payroll_batches
    ALTER COLUMN current_bank_review_level TYPE INTEGER USING current_bank_review_level::integer;
