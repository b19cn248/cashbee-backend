-- ================================================================
-- CashBee Database Setup Script
-- ================================================================
-- Purpose: Create database and user for CashBee application
-- Usage: mysql -u root -p < scripts/setup-database.sql
-- ================================================================

-- Create database
CREATE DATABASE IF NOT EXISTS cashbee
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Create user (change password in production!)
CREATE USER IF NOT EXISTS 'cashbee_user'@'localhost'
IDENTIFIED BY 'cashbee_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON cashbee.* TO 'cashbee_user'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify
SELECT User, Host FROM mysql.user WHERE User = 'cashbee_user';
SHOW DATABASES LIKE 'cashbee';

-- Success message
SELECT 'Database setup completed successfully!' AS Status;
SELECT 'Database: cashbee' AS Info;
SELECT 'User: cashbee_user@localhost' AS Info;
SELECT 'Next: Update application.yml with database credentials' AS Info;
