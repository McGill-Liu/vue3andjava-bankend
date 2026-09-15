-- Apply once to an existing MySQL 8.0 database that must keep its current data.
-- Existing products remain unlimited because purchase_limit defaults to NULL.
USE points_mall;

ALTER TABLE product
    ADD COLUMN purchase_limit INT NULL AFTER stock,
    ADD CONSTRAINT chk_product_purchase_limit
        CHECK (purchase_limit IS NULL OR purchase_limit BETWEEN 1 AND 999);
