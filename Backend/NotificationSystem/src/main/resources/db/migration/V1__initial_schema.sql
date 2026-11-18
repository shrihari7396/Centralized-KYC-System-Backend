-- V1__initial_schema.sql for NotificationSystem (kyc_notification_db)

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_notif_target_status (target_id, status),
    INDEX idx_notif_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
