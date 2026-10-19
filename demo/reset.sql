-- Demo reset: wipes shop data. Local database only.
TRUNCATE TABLE shopping_cart_item;
TRUNCATE TABLE shopping_cart;
TRUNCATE TABLE ordering_order_item;
TRUNCATE TABLE ordering_order_status_history;
TRUNCATE TABLE ordering_order;
TRUNCATE TABLE catalog_product_status_history;
TRUNCATE TABLE catalog_product;
TRUNCATE TABLE shared_event;
TRUNCATE TABLE notifications_delivery;
TRUNCATE TABLE identity_token;

INSERT INTO catalog_product
(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
VALUES
(1, 'Wireless Mouse', 'A quiet wireless mouse for everyday work.', 1, 19.99, 0, 25, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(2, 'Mechanical Keyboard', 'A compact keyboard with tactile switches.', 1, 89.90, 10, 12, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(3, 'Running Shoes', 'Light shoes for daily runs.', 2, 64.50, 0, 30, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(4, 'Ceramic Mug Set', 'Four ceramic mugs for coffee or tea.', 3, 24.00, 15, 40, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(5, 'Yoga Mat', 'A non-slip mat for yoga and stretching.', 5, 29.99, 0, 3, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

UPDATE identity_user SET role = 'MANAGER' WHERE email = 'demo-manager@apistore.com';
