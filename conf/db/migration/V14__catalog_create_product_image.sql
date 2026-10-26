CREATE TABLE catalog_product_image
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id BIGINT UNSIGNED NOT NULL,
    position   INT             NOT NULL,
    large_key  VARCHAR(255)    NOT NULL,
    thumb_key  VARCHAR(255)    NOT NULL,
    created_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_catalog_product_image_product_id_position UNIQUE (product_id, position)
);
