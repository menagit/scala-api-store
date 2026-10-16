CREATE TABLE shopping_cart (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_public_id BINARY(16)      NOT NULL,
    created_at     DATETIME(6)     NOT NULL,
    updated_at     DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shopping_cart_user_public_id UNIQUE (user_public_id)
);

CREATE TABLE shopping_cart_item (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cart_id    BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity   INT             NOT NULL,
    created_at DATETIME(6)     NOT NULL,
    updated_at DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shopping_cart_item_cart_id_product_id UNIQUE (cart_id, product_id),
    CONSTRAINT ck_shopping_cart_item_quantity CHECK (quantity > 0)
);
