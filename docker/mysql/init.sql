-- Create databases for each microservice
CREATE DATABASE IF NOT EXISTS finpay_users;
CREATE DATABASE IF NOT EXISTS finpay_payments;
CREATE DATABASE IF NOT EXISTS finpay_wallets;
CREATE DATABASE IF NOT EXISTS finpay_notifications;

-- Grant privileges to finpay user
GRANT ALL PRIVILEGES ON finpay_users.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_payments.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_wallets.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_notifications.* TO 'finpay'@'%';

FLUSH PRIVILEGES;
