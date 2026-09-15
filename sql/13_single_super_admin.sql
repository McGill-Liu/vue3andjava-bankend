-- MySQL 8.0+: enforce that at most one SUPER_ADMIN can exist.
-- Existing installations must first verify that only one SUPER_ADMIN exists.
USE points_mall;

ALTER TABLE admin_user
    ADD COLUMN super_admin_marker TINYINT GENERATED ALWAYS AS
        (CASE WHEN role = 'SUPER_ADMIN' THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_admin_user_single_super_admin (super_admin_marker);
