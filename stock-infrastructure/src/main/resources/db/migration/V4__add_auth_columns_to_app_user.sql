-- Add authentication columns to the existing app_user table (US-1.1).
-- Expand phase: nullable for now (one legacy row exists); NOT NULL will come
-- in a later migration once existing users are backfilled.
ALTER TABLE app_user
    ADD COLUMN email         VARCHAR(255),
    ADD COLUMN password_hash VARCHAR(255);

ALTER TABLE app_user
    ADD CONSTRAINT uq_app_user_email UNIQUE (email);

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_email_not_blank CHECK (length(trim(email)) > 0);