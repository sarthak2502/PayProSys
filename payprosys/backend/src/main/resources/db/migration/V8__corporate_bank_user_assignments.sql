CREATE TABLE corporate_bank_user_assignments (
    corporate_id UUID NOT NULL REFERENCES corporates(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (corporate_id, user_id)
);
CREATE INDEX idx_corporate_bank_user_corporate ON corporate_bank_user_assignments(corporate_id);
CREATE INDEX idx_corporate_bank_user_user ON corporate_bank_user_assignments(user_id);
