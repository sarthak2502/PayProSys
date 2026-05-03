-- Demo only: store admin password in plain text for simple login (no Spring Security / no crypto).
UPDATE users SET password = 'password' WHERE email = 'admin@payprosys.com';
