INSERT INTO points_mall.notification_message (created_at,updated_at,`type`,status,title,content,order_id,customer_id,processed_at,processed_by) VALUES
	 ('2026-03-27 15:42:45','2026-03-27 16:29:01','ORDER_CREATED','PROCESSED','新订单待发货','用户张三已提交订单 PM202603270001，请同步处理发货和外部积分。',1,2,'2026-03-27 16:29:01',1),
	 ('2026-03-27 15:42:45','2026-03-27 15:42:45','ORDER_CREATED','UNPROCESSED','新订单待发货','用户李四已提交订单 PM202603270002，请同步处理发货和外部积分。',2,3,NULL,NULL);
