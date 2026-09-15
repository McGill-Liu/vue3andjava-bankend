-- Points Mall complete rebuild script for MySQL 8.0+.
-- WARNING: this script permanently deletes the entire points_mall database first.
-- Back up and verify the backup before execution. It creates structure only: no
-- demo data or administrator account is inserted. Afterward, create the first
-- administrator with the one-time BOOTSTRAP_SUPER_ADMIN_* environment variables.

DROP DATABASE IF EXISTS points_mall;

CREATE DATABASE points_mall
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE points_mall;

CREATE TABLE admin_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    super_admin_marker TINYINT GENERATED ALWAYS AS
        (CASE WHEN role = 'SUPER_ADMIN' THEN 1 ELSE NULL END) STORED,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    permissions_json LONGTEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_name (name),
    UNIQUE KEY uk_admin_user_email (email),
    UNIQUE KEY uk_admin_user_single_super_admin (super_admin_marker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    id_card_no VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    temp_password_expires_at DATETIME NULL,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    login_locked_until DATETIME NULL,
    wechat_open_id VARCHAR(128) NULL,
    wechat_bound_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_user_phone (phone),
    UNIQUE KEY uk_customer_user_id_card_no (id_card_no),
    UNIQUE KEY uk_customer_user_wechat_open_id (wechat_open_id),
    KEY idx_customer_user_status (status),
    CONSTRAINT chk_customer_user_failed_login_attempts CHECK (failed_login_attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    default_address TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_customer_address_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_category_name (name),
    KEY idx_product_category_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    cover_image_url VARCHAR(500) NOT NULL,
    gallery_json LONGTEXT NULL,
    points_cost INT NOT NULL,
    stock INT NOT NULL,
    per_order_limit INT NULL,
    customer_total_limit INT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    description LONGTEXT NULL,
    PRIMARY KEY (id),
    KEY idx_product_category_id (category_id),
    KEY idx_product_enabled_created_at (enabled, created_at),
    CONSTRAINT chk_product_points_cost CHECK (points_cost >= 0),
    CONSTRAINT chk_product_stock CHECK (stock >= 0),
    CONSTRAINT chk_product_per_order_limit CHECK (per_order_limit IS NULL OR per_order_limit BETWEEN 1 AND 999),
    CONSTRAINT chk_product_customer_total_limit CHECK (customer_total_limit IS NULL OR customer_total_limit BETWEEN 1 AND 999),
    CONSTRAINT chk_product_limit_relation CHECK (
        per_order_limit IS NULL OR customer_total_limit IS NULL OR per_order_limit <= customer_total_limit
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE points_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_account_customer_id (customer_id),
    CONSTRAINT chk_points_account_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    type VARCHAR(24) NOT NULL,
    quantity_change INT NOT NULL,
    stock_before INT NOT NULL,
    stock_after INT NOT NULL,
    order_id BIGINT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id BIGINT NULL,
    actor_name VARCHAR(100) NOT NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_product_transaction_product_id (product_id),
    KEY idx_product_transaction_order_id (order_id),
    CONSTRAINT chk_product_transaction_stock_before CHECK (stock_before >= 0),
    CONSTRAINT chk_product_transaction_stock_after CHECK (stock_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE points_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    customer_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    balance_before INT NOT NULL,
    balance_after INT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_id_card_no VARCHAR(64) NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id BIGINT NULL,
    actor_name VARCHAR(100) NOT NULL,
    order_id BIGINT NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_points_transaction_customer_id (customer_id),
    KEY idx_points_transaction_order_id (order_id),
    CONSTRAINT chk_points_transaction_balance_before CHECK (balance_before >= 0),
    CONSTRAINT chk_points_transaction_balance_after CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_main (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    order_no VARCHAR(64) NOT NULL,
    customer_id BIGINT NOT NULL,
    checkout_token VARCHAR(64) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_id_card_no VARCHAR(64) NOT NULL,
    total_points INT NOT NULL,
    balance_before INT NOT NULL,
    balance_after INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    recipient_address VARCHAR(255) NOT NULL,
    shipping_company VARCHAR(100) NULL,
    shipping_no VARCHAR(100) NULL,
    shipped_at DATETIME NULL,
    completed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_main_order_no (order_no),
    UNIQUE KEY uk_order_customer_checkout_token (customer_id, checkout_token),
    KEY idx_order_main_customer_id (customer_id),
    KEY idx_order_main_customer_status (customer_id, status),
    KEY idx_order_main_status_created_at (status, created_at),
    KEY idx_order_main_status_shipped_at (status, shipped_at),
    CONSTRAINT chk_order_main_total_points CHECK (total_points >= 0),
    CONSTRAINT chk_order_main_balance_before CHECK (balance_before >= 0),
    CONSTRAINT chk_order_main_balance_after CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    product_cover_image VARCHAR(500) NOT NULL,
    points_cost INT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_item_order_id (order_id),
    KEY idx_order_item_order_product (order_id, product_id),
    KEY idx_order_item_product_id (product_id),
    CONSTRAINT chk_order_item_points_cost CHECK (points_cost >= 0),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content LONGTEXT NOT NULL,
    order_id BIGINT NULL,
    customer_id BIGINT NULL,
    processed_at DATETIME NULL,
    processed_by BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_status (status),
    KEY idx_notification_order_id (order_id),
    KEY idx_notification_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NULL,
    target_name VARCHAR(255) NULL,
    success TINYINT(1) NOT NULL,
    message VARCHAR(500) NULL,
    request_ip VARCHAR(64) NULL,
    PRIMARY KEY (id),
    KEY idx_operation_record_created_at (created_at),
    KEY idx_operation_record_operator_id (operator_id),
    KEY idx_operation_record_target_id (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Foreign keys are added after all tables exist because transaction tables can
-- refer to orders while an order operation writes transaction records as well.
ALTER TABLE customer_address
    ADD CONSTRAINT fk_customer_address_customer
        FOREIGN KEY (customer_id) REFERENCES customer_user (id);

ALTER TABLE product
    ADD CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES product_category (id);

ALTER TABLE points_account
    ADD CONSTRAINT fk_points_account_customer
        FOREIGN KEY (customer_id) REFERENCES customer_user (id);

ALTER TABLE order_main
    ADD CONSTRAINT fk_order_main_customer
        FOREIGN KEY (customer_id) REFERENCES customer_user (id);

ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES order_main (id),
    ADD CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id) REFERENCES product (id);

ALTER TABLE points_transaction
    ADD CONSTRAINT fk_points_transaction_customer
        FOREIGN KEY (customer_id) REFERENCES customer_user (id),
    ADD CONSTRAINT fk_points_transaction_order
        FOREIGN KEY (order_id) REFERENCES order_main (id);

ALTER TABLE product_transaction
    ADD CONSTRAINT fk_product_transaction_product
        FOREIGN KEY (product_id) REFERENCES product (id),
    ADD CONSTRAINT fk_product_transaction_order
        FOREIGN KEY (order_id) REFERENCES order_main (id);

ALTER TABLE notification_message
    ADD CONSTRAINT fk_notification_order
        FOREIGN KEY (order_id) REFERENCES order_main (id),
    ADD CONSTRAINT fk_notification_customer
        FOREIGN KEY (customer_id) REFERENCES customer_user (id);

-- Optional: create a least-privilege application account. Replace the password first.
-- CREATE USER 'pointsmall_app'@'%' IDENTIFIED BY 'replace-with-a-long-random-password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON points_mall.* TO 'pointsmall_app'@'%';
-- FLUSH PRIVILEGES;
