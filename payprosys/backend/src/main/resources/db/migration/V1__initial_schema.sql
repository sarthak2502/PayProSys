CREATE TABLE banks (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE corporates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    bank_id UUID NOT NULL REFERENCES banks(id)
);
CREATE INDEX idx_corporate_bank_id ON corporates(bank_id);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    activation_token VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    bank_id UUID REFERENCES banks(id),
    corporate_id UUID REFERENCES corporates(id)
);
CREATE INDEX idx_user_bank_id ON users(bank_id);
CREATE INDEX idx_user_corporate_id ON users(corporate_id);
CREATE UNIQUE INDEX idx_user_email ON users(email);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE payroll_batches (
    id UUID PRIMARY KEY,
    corporate_id UUID NOT NULL REFERENCES corporates(id),
    uploaded_by_id UUID NOT NULL REFERENCES users(id),
    total_records INT NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    file_name VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_payroll_batch_corporate_id ON payroll_batches(corporate_id);

CREATE TABLE payroll_records (
    id UUID PRIMARY KEY,
    payroll_batch_id UUID NOT NULL REFERENCES payroll_batches(id),
    employee_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    joining_date DATE NOT NULL,
    cpr_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL
);
CREATE INDEX idx_payroll_record_batch_id ON payroll_records(payroll_batch_id);
