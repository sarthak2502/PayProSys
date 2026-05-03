-- Delete all data except super admin so the full flow can be tested again.
-- Order: child tables first, then users (except super admin), then corporates, banks.

DELETE FROM payroll_records;
DELETE FROM payroll_batches;
DELETE FROM corporate_bank_user_assignments;
DELETE FROM user_roles WHERE user_id != 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a99';
DELETE FROM users WHERE id != 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a99';
DELETE FROM corporates;
DELETE FROM banks;
