CREATE TABLE shared_event (
                              id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                              event_type   VARCHAR(64)     NOT NULL,
                              payload      JSON            NOT NULL,
                              created_at   DATETIME(6)     NOT NULL,
                              processed_at DATETIME(6)     NULL,
                              PRIMARY KEY (id),
                              INDEX ix_shared_event_processed_at_id (processed_at, id)
);