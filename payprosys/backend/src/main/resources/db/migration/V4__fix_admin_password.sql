-- Fix seed admin password: V3 hash did not match "password". Update to a valid BCrypt hash.
UPDATE users
SET password = '$2a$10$moxHaI3B.CPlzGJZ2eoymekOMTJulLrOhnfgKTSWxWVrqTzm3d2Em'
WHERE email = 'admin@payprosys.com';
