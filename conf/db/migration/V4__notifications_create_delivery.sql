CREATE TABLE notifications_delivery (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id          BIGINT UNSIGNED NOT NULL,
    recipient_user_id BIGINT UNSIGNED NOT NULL,
    recipient_email   VARCHAR(254)    NOT NULL,
    template          VARCHAR(64)     NOT NULL,
    data              JSON            NOT NULL,
    status            VARCHAR(32)     NOT NULL,
    attempts          INT UNSIGNED    NOT NULL DEFAULT 0,
    next_attempt_at   DATETIME(6)     NOT NULL,
    created_at        DATETIME(6)     NOT NULL,
    updated_at        DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_notifications_delivery_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    UNIQUE KEY uk_notifications_delivery_event_id_recipient_user_id_template (event_id, recipient_user_id, template),
    INDEX ix_notifications_delivery_status_next_attempt_at (status, next_attempt_at)
);