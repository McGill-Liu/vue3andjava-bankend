USE points_mall;

CREATE TABLE IF NOT EXISTS product_transaction (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  KEY idx_product_transaction_product_id (product_id),
  KEY idx_product_transaction_order_id (order_id)
);

INSERT INTO product_transaction (
  product_id, product_name, type, quantity_change, stock_before, stock_after, actor_type, actor_name, remark
)
SELECT p.id, p.name, 'INITIAL_STOCK', p.stock, 0, p.stock, 'SYSTEM', '系统初始化', '升级初始化现有库存'
FROM product p
WHERE NOT EXISTS (
  SELECT 1 FROM product_transaction t WHERE t.product_id = p.id
);

UPDATE admin_user
SET permissions_json = JSON_SET(COALESCE(permissions_json, JSON_OBJECT()), '$.PRODUCT_TRANSACTIONS', 'VIEW')
WHERE role IN ('SUPER_ADMIN', 'OPERATOR');
