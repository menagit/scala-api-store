ALTER TABLE notifications_delivery
    ADD COLUMN secret VARCHAR(255) NULL AFTER data;