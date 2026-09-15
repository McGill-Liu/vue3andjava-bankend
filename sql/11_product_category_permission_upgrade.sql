-- Upgrade an existing database before deploying the product-category management page.
-- Run this once. Resolve duplicate category names first if this statement reports a duplicate-key error.

USE points_mall;

ALTER TABLE product_category
    ADD CONSTRAINT uk_product_category_name UNIQUE (name);

-- PRODUCT_CATEGORIES and OPERATION_RECORDS are intentionally not added to existing
-- operator permission JSON. They default to no permission and can be granted in 员工管理.
