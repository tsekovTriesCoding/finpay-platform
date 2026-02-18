-- Create databases for each microservice
CREATE DATABASE IF NOT EXISTS finpay_users;
CREATE DATABASE IF NOT EXISTS finpay_payments;
CREATE DATABASE IF NOT EXISTS finpay_wallets;
CREATE DATABASE IF NOT EXISTS finpay_notifications;
CREATE DATABASE IF NOT EXISTS finpay_auth;

-- Grant privileges to finpay user
GRANT ALL PRIVILEGES ON finpay_users.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_payments.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_wallets.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_notifications.* TO 'finpay'@'%';
GRANT ALL PRIVILEGES ON finpay_auth.* TO 'finpay'@'%';

FLUSH PRIVILEGES;

-- Transactional Outbox tables (one per database)
-- JPA ddl-auto:update will auto-create these, but we define
-- them here for docker-compose cold-start and documentation.

-- auth-service outbox
USE finpay_auth;
CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    error_message TEXT,
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user-service outbox
USE finpay_users;
CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    error_message TEXT,
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- payment-service outbox
USE finpay_payments;
CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    error_message TEXT,
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- wallet-service outbox
USE finpay_wallets;
CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    error_message TEXT,
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- notification-service outbox
USE finpay_notifications;
CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    error_message TEXT,
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Consumer-side idempotency tables (one per consuming database)
-- Prevents duplicate message processing when at-least-once delivery
-- causes redelivery (outbox crash-after-send, Kafka offset not committed).
-- JPA ddl-auto:update will also auto-create these.

-- user-service processed events (consumes: auth-events)
USE finpay_users;
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) NOT NULL PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_processed_consumer_group (consumer_group),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- wallet-service processed events (consumes: wallet-commands, user-events)
USE finpay_wallets;
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) NOT NULL PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_processed_consumer_group (consumer_group),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- payment-service processed events (consumes: wallet-events)
USE finpay_payments;
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) NOT NULL PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_processed_consumer_group (consumer_group),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- notification-service processed events (consumes: 6 topics)
USE finpay_notifications;
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(64) NOT NULL PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_processed_consumer_group (consumer_group),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
