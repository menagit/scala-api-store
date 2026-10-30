ALTER TABLE identity_token
    ADD COLUMN client_id VARCHAR(64) NULL AFTER user_id;