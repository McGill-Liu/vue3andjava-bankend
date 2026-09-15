ALTER TABLE customer_user
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN temp_password_expires_at DATETIME NULL,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN login_locked_until DATETIME NULL,
    ADD COLUMN wechat_open_id VARCHAR(128) NULL,
    ADD COLUMN wechat_bound_at DATETIME NULL,
    ADD CONSTRAINT uk_customer_user_wechat_open_id UNIQUE (wechat_open_id);

CREATE TABLE operation_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
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
    INDEX idx_operation_record_created_at (created_at),
    INDEX idx_operation_record_operator_id (operator_id),
    INDEX idx_operation_record_target_id (target_id)
);
