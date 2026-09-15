-- Existing installations only: make checkout retries idempotent without changing historical orders.
-- Do not run this after sql/00_fresh_production_schema.sql because the fresh schema already includes it.
USE points_mall;

ALTER TABLE order_main
    ADD COLUMN checkout_token VARCHAR(64) NULL AFTER customer_id,
    ADD UNIQUE KEY uk_order_customer_checkout_token (customer_id, checkout_token);
