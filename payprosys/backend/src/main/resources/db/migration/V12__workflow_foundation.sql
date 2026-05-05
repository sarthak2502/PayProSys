-- Phase 1: workflow step definitions, user review levels, batch flow columns, review event log scaffold.

CREATE TABLE corporate_workflow_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corporate_id UUID NOT NULL REFERENCES corporates(id) ON DELETE CASCADE,
    step_level INT NOT NULL,
    label VARCHAR(255) NOT NULL,
    CONSTRAINT uq_corporate_workflow_step UNIQUE (corporate_id, step_level),
    CONSTRAINT chk_corporate_step_level CHECK (step_level >= 1)
);

CREATE INDEX idx_corporate_workflow_corporate ON corporate_workflow_steps(corporate_id);

CREATE TABLE bank_workflow_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_id UUID NOT NULL REFERENCES banks(id) ON DELETE CASCADE,
    step_level INT NOT NULL,
    label VARCHAR(255) NOT NULL,
    CONSTRAINT uq_bank_workflow_step UNIQUE (bank_id, step_level),
    CONSTRAINT chk_bank_step_level CHECK (step_level >= 1)
);

CREATE INDEX idx_bank_workflow_bank ON bank_workflow_steps(bank_id);

CREATE TABLE corporate_user_review_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corporate_id UUID NOT NULL REFERENCES corporates(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_level INT NOT NULL,
    CONSTRAINT uq_corporate_user_review UNIQUE (corporate_id, user_id, review_level),
    CONSTRAINT chk_corporate_review_level CHECK (review_level >= 1)
);

CREATE INDEX idx_corp_user_review_corporate ON corporate_user_review_levels(corporate_id);
CREATE INDEX idx_corp_user_review_user ON corporate_user_review_levels(user_id);

CREATE TABLE bank_user_review_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_id UUID NOT NULL REFERENCES banks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_level INT NOT NULL,
    CONSTRAINT uq_bank_user_review UNIQUE (bank_id, user_id, review_level),
    CONSTRAINT chk_bank_review_level CHECK (review_level >= 1)
);

CREATE INDEX idx_bank_user_review_bank ON bank_user_review_levels(bank_id);

CREATE TABLE payroll_batch_review_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_batch_id UUID NOT NULL REFERENCES payroll_batches(id) ON DELETE CASCADE,
    actor_user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(64) NOT NULL,
    body TEXT,
    bank_visible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payroll_batch_review_batch ON payroll_batch_review_events(payroll_batch_id, created_at DESC);

ALTER TABLE payroll_batches ADD COLUMN corporate_flow_state VARCHAR(40);
ALTER TABLE payroll_batches ADD COLUMN bank_flow_state VARCHAR(40);
ALTER TABLE payroll_batches ADD COLUMN current_corporate_review_level SMALLINT;
ALTER TABLE payroll_batches ADD COLUMN current_bank_review_level SMALLINT;
ALTER TABLE payroll_batches ADD COLUMN remarks_for_bank TEXT;

UPDATE payroll_batches SET
    corporate_flow_state = CASE WHEN batch_status = 'PENDING' THEN 'CORP_NEW' ELSE 'CORP_SENT_TO_BANK' END,
    current_corporate_review_level = CASE WHEN batch_status = 'PENDING' THEN 1 ELSE NULL END,
    bank_flow_state = CASE WHEN batch_status = 'SUBMITTED' THEN 'BANK_NEW' ELSE NULL END,
    current_bank_review_level = CASE WHEN batch_status = 'SUBMITTED' THEN 1 ELSE NULL END
WHERE corporate_flow_state IS NULL;

ALTER TABLE payroll_batches ALTER COLUMN corporate_flow_state SET NOT NULL;
ALTER TABLE payroll_batches ALTER COLUMN corporate_flow_state SET DEFAULT 'CORP_NEW';

-- Default 3 levels per corporate / bank (POC); Corporate Admin will configure in a later phase.
INSERT INTO corporate_workflow_steps (id, corporate_id, step_level, label)
SELECT gen_random_uuid(), c.id, gs.n,
       CASE gs.n WHEN 1 THEN 'Level 1' WHEN 2 THEN 'Level 2' ELSE 'Level 3' END
FROM corporates c
CROSS JOIN generate_series(1, 3) AS gs(n)
WHERE NOT EXISTS (SELECT 1 FROM corporate_workflow_steps s WHERE s.corporate_id = c.id);

INSERT INTO bank_workflow_steps (id, bank_id, step_level, label)
SELECT gen_random_uuid(), b.id, gs.n,
       CASE gs.n WHEN 1 THEN 'Level 1' WHEN 2 THEN 'Level 2' ELSE 'Level 3' END
FROM banks b
CROSS JOIN generate_series(1, 3) AS gs(n)
WHERE NOT EXISTS (SELECT 1 FROM bank_workflow_steps s WHERE s.bank_id = b.id);
