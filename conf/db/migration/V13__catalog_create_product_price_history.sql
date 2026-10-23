CREATE TABLE catalog_product_price_history
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id           BIGINT UNSIGNED NOT NULL,
    old_price            DECIMAL(12, 2)  NOT NULL,
    new_price            DECIMAL(12, 2)  NOT NULL,
    old_discount_percent DECIMAL(5, 2)   NOT NULL,
    new_discount_percent DECIMAL(5, 2)   NOT NULL,
    changed_by_public_id BINARY(16)      NOT NULL,
    created_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_catalog_product_price_history_changed
        CHECK (old_price <> new_price OR old_discount_percent <> new_discount_percent),
    INDEX ix_catalog_product_price_history_product_id_created_at (product_id, created_at)
);
