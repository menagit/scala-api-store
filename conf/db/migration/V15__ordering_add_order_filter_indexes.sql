ALTER TABLE ordering_order
    ADD INDEX ix_ordering_order_status_created_at (status, created_at),
    ADD INDEX ix_ordering_order_created_at (created_at);
