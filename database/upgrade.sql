-- 已经执行过旧版 schema.sql 的数据库，请执行本升级脚本。
USE acgn_screenroom;

SET @has_role = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role');
SET @sql = IF(@has_role = 0, 'ALTER TABLE users ADD COLUMN role ENUM(''USER'',''ADMIN'') NOT NULL DEFAULT ''USER'' AFTER password_hash', 'SELECT 1');
PREPARE add_role FROM @sql;
EXECUTE add_role;
DEALLOCATE PREPARE add_role;
UPDATE users SET role = 'USER' WHERE role IS NULL OR role = '';
INSERT IGNORE INTO users(username, email, password_hash, role) VALUES ('admin', 'admin@example.com', 'admin123', 'ADMIN');

-- 如果旧表缺少电影分类字段，请一并执行：
SET @has_media_type = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'anime' AND column_name = 'media_type');
SET @sql = IF(@has_media_type = 0, 'ALTER TABLE anime ADD COLUMN media_type ENUM(''ANIME'',''MOVIE'') NOT NULL DEFAULT ''ANIME'' AFTER season', 'SELECT 1');
PREPARE add_media_type FROM @sql;
EXECUTE add_media_type;
DEALLOCATE PREPARE add_media_type;