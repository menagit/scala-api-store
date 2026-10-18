CREATE TABLE ordering_order (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id          BINARY(16)      NOT NULL,
    user_public_id     BINARY(16)      NOT NULL,
    status             VARCHAR(32)     NOT NULL,
    total              DECIMAL(12,2)   NOT NULL,
    tracking_number    VARCHAR(64)     NULL,
    estimated_delivery DATE            NULL,
    created_at         DATETIME(6)     NOT NULL,
    updated_at         DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ordering_order_public_id UNIQUE (public_id),
    CONSTRAINT ck_ordering_order_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    INDEX ix_ordering_order_user_public_id_created_at (user_public_id, created_at)
);

CREATE TABLE ordering_order_item (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id              BIGINT UNSIGNED NOT NULL,
    product_id            BIGINT UNSIGNED NOT NULL,
    product_name          VARCHAR(200)    NOT NULL,
    unit_price            DECIMAL(12,2)   NOT NULL,
    discount_percent      DECIMAL(5,2)    NOT NULL,
    discounted_unit_price DECIMAL(12,2)   NOT NULL,
    quantity              INT             NOT NULL,
    line_total            DECIMAL(12,2)   NOT NULL,
    created_at            DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ordering_order_item_order_id_product_id UNIQUE (order_id, product_id),
    CONSTRAINT ck_ordering_order_item_quantity CHECK (quantity > 0)
);

CREATE TABLE ordering_order_status_history (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id             BIGINT UNSIGNED NOT NULL,
    old_status           VARCHAR(32)     NULL,
    new_status           VARCHAR(32)     NOT NULL,
    changed_by_type      VARCHAR(32)     NOT NULL,
    changed_by_public_id BINARY(16)      NULL,
    created_at           DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_ordering_order_status_history_old_status
        CHECK (old_status IS NULL OR old_status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_ordering_order_status_history_new_status
        CHECK (new_status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_ordering_order_status_history_old_new
        CHECK (old_status IS NULL OR old_status <> new_status),
    CONSTRAINT ck_ordering_order_status_history_changed_by_type
        CHECK (changed_by_type IN ('CLIENT', 'MANAGER', 'SYSTEM')),
    CONSTRAINT ck_ordering_order_status_history_changed_by_public_id
        CHECK ((changed_by_type = 'SYSTEM') = (changed_by_public_id IS NULL)),
    INDEX ix_ordering_order_status_history_order_id_created_at (order_id, created_at)
);
