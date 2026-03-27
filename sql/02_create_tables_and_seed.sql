CREATE TABLE IF NOT EXISTS points_mall.admin_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  phone VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL,
  enabled BIT(1) NOT NULL DEFAULT b'1'
);

CREATE TABLE IF NOT EXISTS points_mall.customer_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  name VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL UNIQUE,
  id_card_no VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL,
  approved_at DATETIME NULL,
  approved_by BIGINT NULL
);

CREATE TABLE IF NOT EXISTS points_mall.customer_address (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  customer_id BIGINT NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  recipient_phone VARCHAR(20) NOT NULL,
  detail_address VARCHAR(255) NOT NULL,
  default_address BIT(1) NOT NULL DEFAULT b'0',
  KEY idx_customer_address_customer_id (customer_id)
);

CREATE TABLE IF NOT EXISTS points_mall.product_category (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BIT(1) NOT NULL DEFAULT b'1'
);

CREATE TABLE IF NOT EXISTS points_mall.product (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  category_id BIGINT NOT NULL,
  name VARCHAR(150) NOT NULL,
  cover_image_url VARCHAR(500) NOT NULL,
  gallery_json LONGTEXT NULL,
  points_cost INT NOT NULL,
  stock INT NOT NULL,
  enabled BIT(1) NOT NULL DEFAULT b'1',
  sort_order INT NOT NULL DEFAULT 0,
  description LONGTEXT NULL,
  KEY idx_product_category_id (category_id),
  KEY idx_product_enabled_created_at (enabled, created_at)
);

CREATE TABLE IF NOT EXISTS points_mall.points_account (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  customer_id BIGINT NOT NULL UNIQUE,
  balance INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS points_mall.points_transaction (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  customer_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  amount INT NOT NULL,
  balance_after INT NOT NULL,
  order_id BIGINT NULL,
  remark VARCHAR(255) NULL,
  KEY idx_points_transaction_customer_id (customer_id),
  KEY idx_points_transaction_order_id (order_id)
);

CREATE TABLE IF NOT EXISTS points_mall.order_main (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  customer_phone VARCHAR(20) NOT NULL,
  total_points INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  recipient_phone VARCHAR(20) NOT NULL,
  recipient_address VARCHAR(255) NOT NULL,
  shipping_company VARCHAR(100) NULL,
  shipping_no VARCHAR(100) NULL,
  shipped_at DATETIME NULL,
  completed_at DATETIME NULL,
  cancelled_at DATETIME NULL,
  KEY idx_order_main_customer_id (customer_id),
  KEY idx_order_main_status_created_at (status, created_at),
  KEY idx_order_main_status_shipped_at (status, shipped_at)
);

CREATE TABLE IF NOT EXISTS points_mall.order_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(150) NOT NULL,
  product_cover_image VARCHAR(500) NOT NULL,
  points_cost INT NOT NULL,
  quantity INT NOT NULL,
  KEY idx_order_item_order_id (order_id),
  KEY idx_order_item_product_id (product_id)
);

CREATE TABLE IF NOT EXISTS points_mall.notification_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  KEY idx_notification_status (status),
  KEY idx_notification_order_id (order_id),
  KEY idx_notification_customer_id (customer_id)
);

INSERT INTO points_mall.product_category (name, sort_order, enabled)
SELECT '精选好物', 1, b'1'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM points_mall.product_category WHERE name = '精选好物'
);

INSERT INTO points_mall.product_category (name, sort_order, enabled)
SELECT '生活用品', 2, b'1'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM points_mall.product_category WHERE name = '生活用品'
);

INSERT INTO points_mall.product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id,
       '积分礼盒',
       'https://dummyimage.com/320x320/f2f4f8/334155&text=Gift',
       '[]',
       200,
       50,
       b'1',
       1,
       '内部客户兑换使用的示例商品。'
FROM points_mall.product_category c
WHERE c.name = '精选好物'
  AND NOT EXISTS (
    SELECT 1 FROM points_mall.product WHERE name = '积分礼盒'
  );

INSERT INTO points_mall.product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id,
       '品牌保温杯',
       'https://dummyimage.com/320x320/e2e8f0/0f172a&text=Cup',
       '[]',
       120,
       100,
       b'1',
       2,
       '适合作为积分兑换的日常实用商品。'
FROM points_mall.product_category c
WHERE c.name = '生活用品'
  AND NOT EXISTS (
    SELECT 1 FROM points_mall.product WHERE name = '品牌保温杯'
  );
