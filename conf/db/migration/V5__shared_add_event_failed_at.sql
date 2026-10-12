ALTER TABLE shared_event
    ADD COLUMN failed_at DATETIME(6) NULL AFTER processed_at;