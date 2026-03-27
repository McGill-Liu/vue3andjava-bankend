USE points_mall;

ALTER TABLE admin_user
  ADD COLUMN permissions_json LONGTEXT NULL;

UPDATE admin_user
SET permissions_json = JSON_OBJECT(
  'NOTIFICATIONS', 'EDIT',
  'APPROVALS', 'EDIT',
  'USERS', 'EDIT',
  'POINTS', 'EDIT',
  'PRODUCTS', 'EDIT',
  'ORDERS', 'EDIT',
  'ADMINS', 'EDIT'
)
WHERE role = 'SUPER_ADMIN'
  AND (permissions_json IS NULL OR permissions_json = '');

UPDATE admin_user
SET permissions_json = JSON_OBJECT(
  'NOTIFICATIONS', 'EDIT',
  'USERS', 'VIEW',
  'POINTS', 'VIEW',
  'PRODUCTS', 'EDIT',
  'ORDERS', 'EDIT'
)
WHERE role = 'OPERATOR'
  AND (permissions_json IS NULL OR permissions_json = '');
