-- Apply after 13_product_purchase_limit.sql to an existing MySQL 8.0 database.
-- Existing products remain unlimited because both limit columns default to NULL.
USE points_mall;

ALTER TABLE product
    DROP CHECK chk_product_purchase_limit,
    RENAME COLUMN purchase_limit TO per_order_limit,
    ADD COLUMN customer_total_limit INT NULL AFTER per_order_limit,
    ADD CONSTRAINT chk_product_per_order_limit
        CHECK (per_order_limit IS NULL OR per_order_limit BETWEEN 1 AND 999),
    ADD CONSTRAINT chk_product_customer_total_limit
        CHECK (customer_total_limit IS NULL OR customer_total_limit BETWEEN 1 AND 999),
    ADD CONSTRAINT chk_product_limit_relation
        CHECK (per_order_limit IS NULL OR customer_total_limit IS NULL OR per_order_limit <= customer_total_limit);

ALTER TABLE order_main
    ADD INDEX idx_order_main_customer_status (customer_id, status);

ALTER TABLE order_item
    ADD INDEX idx_order_item_order_product (order_id, product_id);
