CREATE TABLE catalog_product_status_history
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id           BIGINT UNSIGNED NOT NULL,
    action               VARCHAR(32) NOT NULL,
    old_status           VARCHAR(32) NULL,
    new_status           VARCHAR(32) NOT NULL,
    changed_by_public_id BINARY(16)      NOT NULL,
    created_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_catalog_product_status_history_action
        CHECK (action IN ('CREATED', 'STATUS_CHANGED', 'DELETED')
) ,
    CONSTRAINT ck_catalog_product_status_history_old_status
        CHECK ((action = 'CREATED' AND old_status IS NULL) OR (action <> 'CREATED' AND old_status IS NOT NULL)),
    CONSTRAINT ck_catalog_product_status_history_old_status_value
        CHECK (old_status IS NULL OR old_status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_catalog_product_status_history_new_status
        CHECK (new_status IN ('ACTIVE', 'DISABLED')),
    INDEX ix_catalog_product_status_history_product_id_created_at (product_id, created_at)
);