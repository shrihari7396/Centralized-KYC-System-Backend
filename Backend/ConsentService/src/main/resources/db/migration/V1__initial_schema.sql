-- Flyway migration V1: Initial Consent Service Schema
CREATE TABLE IF NOT EXISTS `consents` (
    `id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(100) NOT NULL,
    `bank_id` VARCHAR(255) NOT NULL,
    `consent_status` VARCHAR(50) NOT NULL,
    `requested_at` DATETIME NULL,
    `approved_at` DATETIME NULL,
    `expiry_at` DATETIME NULL,
    `active` BIT(1) NOT NULL DEFAULT b'0',
    `consent_documents` TEXT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_user_bank` (`user_id`, `bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
