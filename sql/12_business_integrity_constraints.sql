-- Apply once to an existing MySQL 8.0 production database.
-- Back up first. Every ALTER will fail instead of hiding existing invalid data.
USE points_mall;

ALTER TABLE product
    ADD CONSTRAINT chk_product_points_cost CHECK (points_cost >= 0),
    ADD CONSTRAINT chk_product_stock CHECK (stock >= 0);

ALTER TABLE points_account
    ADD CONSTRAINT chk_points_account_balance CHECK (balance >= 0);

ALTER TABLE order_main
    ADD CONSTRAINT chk_order_main_total_points CHECK (total_points >= 0),
    ADD CONSTRAINT chk_order_main_balance_before CHECK (balance_before >= 0),
    ADD CONSTRAINT chk_order_main_balance_after CHECK (balance_after >= 0);

ALTER TABLE order_item
    ADD CONSTRAINT chk_order_item_points_cost CHECK (points_cost >= 0),
    ADD CONSTRAINT chk_order_item_quantity CHECK (quantity > 0);
