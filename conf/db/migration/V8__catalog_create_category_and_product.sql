CREATE TABLE catalog_category (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100)    NOT NULL,
    created_at DATETIME(6)     NOT NULL,
    updated_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_catalog_category_name UNIQUE (name)
);

CREATE TABLE catalog_product (
     id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
     name             VARCHAR(200)    NOT NULL,
     description      VARCHAR(2000)   NOT NULL,
     category_id      BIGINT UNSIGNED NOT NULL,
     price            DECIMAL(12, 2)  NOT NULL,
     discount_percent DECIMAL(5, 2)   NOT NULL DEFAULT 0,
     stock            INT             NOT NULL,
     status           VARCHAR(32)     NOT NULL,
     deleted_at       DATETIME(6)     NULL,
     created_at       DATETIME(6)     NOT NULL,
     updated_at       DATETIME(6)     NOT NULL,
     PRIMARY KEY (id),
     CONSTRAINT ck_catalog_product_price CHECK (price > 0),
     CONSTRAINT ck_catalog_product_discount_percent CHECK (discount_percent >= 0 AND discount_percent <= 99.99),
     CONSTRAINT ck_catalog_product_stock CHECK (stock >= 0),
     CONSTRAINT ck_catalog_product_status CHECK (status IN ('ACTIVE', 'DISABLED')),
     INDEX ix_catalog_product_category_id_status_deleted_at (category_id, status, deleted_at)
);