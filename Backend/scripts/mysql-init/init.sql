CREATE DATABASE IF NOT EXISTS `users` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `kyc` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `kyc_consent_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `kyc_notification_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON `users`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `kyc`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `kyc_consent_db`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `kyc_notification_db`.* TO 'root'@'%';
FLUSH PRIVILEGES;
