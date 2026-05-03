-- Seed one bank and one BANK_ADMIN user for initial login.
-- Default password: password (BCrypt hash for "password", 10 rounds)
INSERT INTO banks (id, name, created_at) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Default Bank', NOW())
ON CONFLICT DO NOTHING;

INSERT INTO users (id, first_name, last_name, email, password, status, created_at, bank_id) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Admin', 'User', 'admin@payprosys.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11')
ON CONFLICT DO NOTHING;

-- Attach BANK_ADMIN role to seed user (role id 1 = BANK_ADMIN from V2)
INSERT INTO user_roles (user_id, role_id)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', id FROM roles WHERE name = 'BANK_ADMIN' LIMIT 1
ON CONFLICT DO NOTHING;
