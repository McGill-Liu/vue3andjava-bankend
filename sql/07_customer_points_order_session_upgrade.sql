USE points_mall;

DELETE oi FROM order_item oi JOIN order_main o ON o.id = oi.order_id WHERE o.status = 'CANCELLED';
DELETE n FROM notification_message n JOIN order_main o ON o.id = n.order_id WHERE o.status = 'CANCELLED';
DELETE pt FROM points_transaction pt JOIN order_main o ON o.id = pt.order_id WHERE o.status = 'CANCELLED';
DELETE FROM order_main WHERE status = 'CANCELLED';

DELETE pa FROM points_account pa JOIN customer_user u ON u.id = pa.customer_id
WHERE u.status IN ('PENDING_APPROVAL', 'REJECTED');
DELETE pt FROM points_transaction pt JOIN customer_user u ON u.id = pt.customer_id
WHERE u.status IN ('PENDING_APPROVAL', 'REJECTED');
DELETE a FROM customer_address a JOIN customer_user u ON u.id = a.customer_id
WHERE u.status IN ('PENDING_APPROVAL', 'REJECTED');
DELETE FROM customer_user WHERE status IN ('PENDING_APPROVAL', 'REJECTED');

ALTER TABLE customer_user DROP COLUMN approved_at, DROP COLUMN approved_by;
ALTER TABLE points_transaction
  ADD COLUMN balance_before INT NULL AFTER amount,
  ADD COLUMN customer_name VARCHAR(100) NULL AFTER balance_after,
  ADD COLUMN customer_phone VARCHAR(20) NULL AFTER customer_name,
  ADD COLUMN customer_id_card_no VARCHAR(64) NULL AFTER customer_phone,
  ADD COLUMN actor_type VARCHAR(20) NULL AFTER customer_id_card_no,
  ADD COLUMN actor_id BIGINT NULL AFTER actor_type,
  ADD COLUMN actor_name VARCHAR(100) NULL AFTER actor_id;
ALTER TABLE order_main
  ADD COLUMN customer_id_card_no VARCHAR(64) NULL AFTER customer_phone,
  ADD COLUMN balance_before INT NULL AFTER total_points,
  ADD COLUMN balance_after INT NULL AFTER balance_before;

UPDATE points_transaction pt
JOIN customer_user u ON u.id = pt.customer_id
SET pt.balance_before = pt.balance_after - pt.amount,
    pt.customer_name = u.name,
    pt.customer_phone = u.phone,
    pt.customer_id_card_no = u.id_card_no,
    pt.actor_type = CASE WHEN pt.type IN ('ORDER_DEDUCT', 'ORDER_REFUND') THEN 'CUSTOMER' ELSE 'ADMIN' END,
    pt.actor_id = CASE WHEN pt.type IN ('ORDER_DEDUCT', 'ORDER_REFUND') THEN u.id ELSE 1 END,
    pt.actor_name = CASE WHEN pt.type IN ('ORDER_DEDUCT', 'ORDER_REFUND') THEN u.name ELSE '超级管理员' END;

UPDATE order_main o
JOIN customer_user u ON u.id = o.customer_id
LEFT JOIN points_account pa ON pa.customer_id = o.customer_id
SET o.customer_id_card_no = u.id_card_no,
    o.balance_after = COALESCE(pa.balance, 0),
    o.balance_before = COALESCE(pa.balance, 0) + o.total_points;

ALTER TABLE points_transaction
  MODIFY balance_before INT NOT NULL,
  MODIFY customer_name VARCHAR(100) NOT NULL,
  MODIFY customer_phone VARCHAR(20) NOT NULL,
  MODIFY customer_id_card_no VARCHAR(64) NOT NULL,
  MODIFY actor_type VARCHAR(20) NOT NULL,
  MODIFY actor_name VARCHAR(100) NOT NULL;
ALTER TABLE order_main
  MODIFY customer_id_card_no VARCHAR(64) NOT NULL,
  MODIFY balance_before INT NOT NULL,
  MODIFY balance_after INT NOT NULL;

UPDATE admin_user
SET permissions_json = JSON_REMOVE(permissions_json, '$.APPROVALS')
WHERE permissions_json IS NOT NULL;
