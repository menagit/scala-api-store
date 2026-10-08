CREATE TABLE identity_token (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    type        VARCHAR(32)     NOT NULL,
    token_hash  BINARY(32)      NOT NULL,
    expires_at  DATETIME(6)     NOT NULL,
    revoked_at  DATETIME(6)     NULL,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_identity_token_type CHECK (type IN ('REFRESH', 'PASSWORD_RESET')),
    UNIQUE KEY uk_identity_token_token_hash (token_hash),
    INDEX ix_identity_token_user_id_type (user_id, type),
    INDEX ix_identity_token_expires_at (expires_at)
);