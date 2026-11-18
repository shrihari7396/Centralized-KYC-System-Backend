-- V1__initial_schema.sql for LogMonitoringSystem

CREATE TABLE IF NOT EXISTS logs (
    id BINARY(16) NOT NULL,
    service_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    level INT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    payload TEXT,
    ingested_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_logs_service_event (service_id, event_type),
    INDEX idx_logs_occurred_at (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS traffic_profiles (
    `key` VARCHAR(255) NOT NULL,
    ip VARCHAR(50),
    endpoint VARCHAR(255),
    request_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_payload_size DOUBLE DEFAULT 0.0,
    payload_entropy DOUBLE DEFAULT 0.0,
    avg_inter_request_gap DOUBLE DEFAULT 0.0,
    window_start DATETIME(6),
    window_end DATETIME(6),
    PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS threat_assessments (
    id BINARY(16) NOT NULL,
    source_key VARCHAR(255),
    threat_score DOUBLE DEFAULT 0.0,
    classification VARCHAR(50),
    decision VARCHAR(50),
    evaluated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
