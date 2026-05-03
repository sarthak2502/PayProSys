-- Super admin: no bank_id, no corporate_id. Demo password: password
INSERT INTO users (id, first_name, last_name, email, password, status, created_at, bank_id, corporate_id) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a99', 'Super', 'Admin', 'superadmin@payprosys.com', 'password', 'ACTIVE', NOW(), NULL, NULL)
ON CONFLICT (email) DO NOTHING;

-- Attach SUPER_ADMIN role (use user id by email in case user already existed)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.email = 'superadmin@payprosys.com' AND r.name = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
