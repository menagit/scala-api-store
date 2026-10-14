ALTER TABLE notifications_delivery
DROP INDEX uk_notifications_delivery_event_id_recipient_user_id_template,
DROP COLUMN recipient_user_id,
    ADD COLUMN recipient_public_id VARCHAR(36) NOT NULL AFTER event_id,
    ADD UNIQUE KEY uk_notifications_delivery_event_id_recipient_public_id_template
        (event_id, recipient_public_id, template);