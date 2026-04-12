USE points_mall;

SET @has_phone := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_user'
    AND COLUMN_NAME = 'phone'
);
SET @drop_phone_sql := IF(@has_phone > 0, 'ALTER TABLE admin_user DROP COLUMN phone', 'SELECT 1');
PREPARE stmt FROM @drop_phone_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_name_unique := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_user'
    AND INDEX_NAME = 'uk_admin_user_name'
);
SET @add_name_unique_sql := IF(@has_name_unique = 0, 'ALTER TABLE admin_user ADD UNIQUE KEY uk_admin_user_name (name)', 'SELECT 1');
PREPARE stmt FROM @add_name_unique_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE admin_user
SET name = CONVERT(0xE8B685E7BAA7E7AEA1E79086E59198 USING utf8mb4),
    email = 'textas7lee@foxmail.com'
WHERE role = 'SUPER_ADMIN';

UPDATE admin_user
SET name = CONVERT(0xE4B89AE58AA1E59198 USING utf8mb4)
WHERE role = 'OPERATOR';
