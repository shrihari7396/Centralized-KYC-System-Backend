-- V1__initial_schema.sql for KycManagementService (kyc database)

CREATE TABLE IF NOT EXISTS kyc_entities (
    user_id VARCHAR(255) NOT NULL,
    aadhaar_no TEXT,
    pan TEXT,
    driving_license TEXT,
    street VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kyc_records (
    kyc_id BINARY(16) NOT NULL,
    user_id VARCHAR(255),
    mobile_number VARCHAR(50),
    aadhar_number VARCHAR(255),
    pan_number VARCHAR(50),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    email VARCHAR(255),
    dob DATE,
    address TEXT,
    verification_status VARCHAR(50),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (kyc_id),
    INDEX idx_kycrecords_user_id (user_id),
    INDEX idx_kycrecords_mobile (mobile_number),
    INDEX idx_kycrecords_email (email),
    INDEX idx_kycrecords_aadhar (aadhar_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
