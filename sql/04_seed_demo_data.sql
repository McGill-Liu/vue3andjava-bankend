USE points_mall;

INSERT INTO product_category (name, sort_order, enabled)
SELECT '数码周边', 3, b'1'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM product_category WHERE name = '数码周边'
);

INSERT INTO product_category (name, sort_order, enabled)
SELECT '办公文具', 4, b'1'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM product_category WHERE name = '办公文具'
);

INSERT INTO product_category (name, sort_order, enabled)
SELECT '家居日用', 5, b'1'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM product_category WHERE name = '家居日用'
);

INSERT INTO product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id, '蓝牙耳机', 'https://dummyimage.com/320x320/dbeafe/1e3a8a&text=Earbuds', '[]', 399, 25, b'1', 1, '轻量蓝牙耳机，适合日常通勤和运动使用。'
FROM product_category c
WHERE c.name = '数码周边'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = '蓝牙耳机');

INSERT INTO product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id, '便携充电宝', 'https://dummyimage.com/320x320/e0f2fe/0f172a&text=Power', '[]', 259, 40, b'1', 2, '10000mAh 便携充电宝，适合外出兑换。'
FROM product_category c
WHERE c.name = '数码周边'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = '便携充电宝');

INSERT INTO product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id, '商务笔记本', 'https://dummyimage.com/320x320/fef3c7/92400e&text=Notebook', '[]', 89, 80, b'1', 1, '简洁商务风笔记本，适合办公和会议记录。'
FROM product_category c
WHERE c.name = '办公文具'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = '商务笔记本');

INSERT INTO product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id, '金属签字笔', 'https://dummyimage.com/320x320/f1f5f9/334155&text=Pen', '[]', 49, 150, b'1', 2, '书写顺滑，适合作为低积分常规兑换品。'
FROM product_category c
WHERE c.name = '办公文具'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = '金属签字笔');

INSERT INTO product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id, '香薰加湿器', 'https://dummyimage.com/320x320/ede9fe/4338ca&text=Humidifier', '[]', 299, 18, b'1', 1, '桌面型香薰加湿器，适合家居和办公桌使用。'
FROM product_category c
WHERE c.name = '家居日用'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = '香薰加湿器');

INSERT INTO product (category_id, name, cover_image_url, gallery_json, points_cost, stock, enabled, sort_order, description)
SELECT c.id, '午休抱枕毯', 'https://dummyimage.com/320x320/fee2e2/991b1b&text=Blanket', '[]', 169, 35, b'1', 2, '抱枕和毛毯二合一，适合办公室午休。'
FROM product_category c
WHERE c.name = '家居日用'
  AND NOT EXISTS (SELECT 1 FROM product WHERE name = '午休抱枕毯');

INSERT INTO product_transaction (
  product_id, product_name, type, quantity_change, stock_before, stock_after, actor_type, actor_name, remark
)
SELECT p.id, p.name, 'INITIAL_STOCK', p.stock, 0, p.stock, 'SYSTEM', '系统初始化', '初始化商品库存'
FROM product p
WHERE NOT EXISTS (
  SELECT 1 FROM product_transaction t WHERE t.product_id = p.id
);

INSERT INTO customer_user (name, phone, id_card_no, password_hash, status)
SELECT '张三', '18800000001', '110101199001011234', '$2a$10$g7iHQSwR6sJyH0Ctzda6EO7XVv.QB94SSLSYvhgYD3JTDTuivhVQW', 'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM customer_user WHERE phone = '18800000001'
);

INSERT INTO customer_user (name, phone, id_card_no, password_hash, status)
SELECT '李四', '18800000002', '110101199202023456', '$2a$10$BnYA82ZIXIetBIM/kKIpuOMJhgbCNRLE2rkBczwScjnAEtTieUYzi', 'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM customer_user WHERE phone = '18800000002'
);

INSERT INTO points_account (customer_id, balance)
SELECT u.id, 1200
FROM customer_user u
WHERE u.phone = '18800000001'
  AND NOT EXISTS (SELECT 1 FROM points_account p WHERE p.customer_id = u.id);

INSERT INTO points_account (customer_id, balance)
SELECT u.id, 860
FROM customer_user u
WHERE u.phone = '18800000002'
  AND NOT EXISTS (SELECT 1 FROM points_account p WHERE p.customer_id = u.id);

INSERT INTO points_transaction (customer_id, type, amount, balance_before, balance_after, customer_name, customer_phone, customer_id_card_no, actor_type, actor_id, actor_name, remark)
SELECT u.id, 'ADMIN_INIT', 1200, 0, 1200, u.name, u.phone, u.id_card_no, 'ADMIN', 1, '超级管理员', '新增客户初始化积分'
FROM customer_user u
WHERE u.phone = '18800000001'
  AND NOT EXISTS (
    SELECT 1 FROM points_transaction t
    WHERE t.customer_id = u.id AND t.type = 'ADMIN_INIT'
  );

INSERT INTO points_transaction (customer_id, type, amount, balance_before, balance_after, customer_name, customer_phone, customer_id_card_no, actor_type, actor_id, actor_name, remark)
SELECT u.id, 'ADMIN_INIT', 860, 0, 860, u.name, u.phone, u.id_card_no, 'ADMIN', 1, '超级管理员', '新增客户初始化积分'
FROM customer_user u
WHERE u.phone = '18800000002'
  AND NOT EXISTS (
    SELECT 1 FROM points_transaction t
    WHERE t.customer_id = u.id AND t.type = 'ADMIN_INIT'
  );

INSERT INTO customer_address (customer_id, recipient_name, recipient_phone, detail_address, default_address)
SELECT u.id, '张三', '18800000001', '张江高科博云路 88 号', b'1'
FROM customer_user u
WHERE u.phone = '18800000001'
  AND NOT EXISTS (
    SELECT 1 FROM customer_address a
    WHERE a.customer_id = u.id AND a.detail_address = '张江高科博云路 88 号'
  );

INSERT INTO customer_address (customer_id, recipient_name, recipient_phone, detail_address, default_address)
SELECT u.id, '李四', '18800000002', '文三路 188 号 2 幢 1201', b'1'
FROM customer_user u
WHERE u.phone = '18800000002'
  AND NOT EXISTS (
    SELECT 1 FROM customer_address a
    WHERE a.customer_id = u.id AND a.detail_address = '文三路 188 号 2 幢 1201'
  );

INSERT INTO order_main (
  order_no, customer_id, customer_name, customer_phone, customer_id_card_no, total_points, balance_before, balance_after, status,
  recipient_name, recipient_phone, recipient_address, shipping_company, shipping_no, shipped_at
)
SELECT 'PM202603270001', u.id, '张三', '18800000001', u.id_card_no, 399, 1599, 1200, 'SHIPPED',
       '张三', '18800000001', '张江高科博云路 88 号',
       '顺丰', 'SF202603270001', NOW()
FROM customer_user u
WHERE u.phone = '18800000001'
  AND NOT EXISTS (SELECT 1 FROM order_main WHERE order_no = 'PM202603270001');

INSERT INTO order_main (
  order_no, customer_id, customer_name, customer_phone, customer_id_card_no, total_points, balance_before, balance_after, status,
  recipient_name, recipient_phone, recipient_address
)
SELECT 'PM202603270002', u.id, '李四', '18800000002', u.id_card_no, 169, 1029, 860, 'PENDING_SHIPMENT',
       '李四', '18800000002', '文三路 188 号 2 幢 1201'
FROM customer_user u
WHERE u.phone = '18800000002'
  AND NOT EXISTS (SELECT 1 FROM order_main WHERE order_no = 'PM202603270002');

INSERT INTO order_item (order_id, product_id, product_name, product_cover_image, points_cost, quantity)
SELECT o.id, p.id, p.name, p.cover_image_url, 399, 1
FROM order_main o
JOIN product p ON p.name = '蓝牙耳机'
WHERE o.order_no = 'PM202603270001'
  AND NOT EXISTS (
    SELECT 1 FROM order_item oi
    WHERE oi.order_id = o.id AND oi.product_id = p.id
  );

INSERT INTO order_item (order_id, product_id, product_name, product_cover_image, points_cost, quantity)
SELECT o.id, p.id, p.name, p.cover_image_url, 169, 1
FROM order_main o
JOIN product p ON p.name = '午休抱枕毯'
WHERE o.order_no = 'PM202603270002'
  AND NOT EXISTS (
    SELECT 1 FROM order_item oi
    WHERE oi.order_id = o.id AND oi.product_id = p.id
  );

INSERT INTO notification_message (type, status, title, content, order_id, customer_id)
SELECT 'ORDER_CREATED', 'UNPROCESSED', '新订单待发货', '用户张三已提交订单 PM202603270001，请同步处理发货和外部积分。', o.id, o.customer_id
FROM order_main o
WHERE o.order_no = 'PM202603270001'
  AND NOT EXISTS (
    SELECT 1 FROM notification_message n
    WHERE n.order_id = o.id AND n.type = 'ORDER_CREATED'
  );

INSERT INTO notification_message (type, status, title, content, order_id, customer_id)
SELECT 'ORDER_CREATED', 'UNPROCESSED', '新订单待发货', '用户李四已提交订单 PM202603270002，请同步处理发货和外部积分。', o.id, o.customer_id
FROM order_main o
WHERE o.order_no = 'PM202603270002'
  AND NOT EXISTS (
    SELECT 1 FROM notification_message n
    WHERE n.order_id = o.id AND n.type = 'ORDER_CREATED'
  );
