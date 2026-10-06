CREATE TABLE identity_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id     BINARY(16)      NOT NULL,
    email         VARCHAR(254) COLLATE utf8mb4_bin NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    first_name    VARCHAR(100)    NOT NULL,
    last_name     VARCHAR(100)    NOT NULL,
    role          VARCHAR(32)     NOT NULL,
    token_version INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at    DATETIME(6)     NOT NULL,
    updated_at    DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_identity_user_email UNIQUE (email),
    CONSTRAINT uk_identity_user_public_id UNIQUE (public_id),
    CONSTRAINT ck_identity_user_role CHECK (role IN ('MANAGER', 'CLIENT')),
    CONSTRAINT ck_identity_user_email_lowercase CHECK (email = LOWER(email))
);