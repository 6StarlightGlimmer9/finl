-- 次元放映室统一 MySQL 数据库脚本
-- 适用：全新数据库初始化，也适用于已经执行过旧版脚本的数据库升级。
-- 执行：mysql -u root -p < database/acgn_screenroom.sql

CREATE DATABASE IF NOT EXISTS acgn_screenroom DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE acgn_screenroom;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(40) NOT NULL UNIQUE,
  email VARCHAR(120) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  avatar_url VARCHAR(500), bio VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

SET @has_role = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role');
SET @sql = IF(@has_role = 0, 'ALTER TABLE users ADD COLUMN role ENUM(''USER'',''ADMIN'') NOT NULL DEFAULT ''USER'' AFTER password_hash', 'SELECT 1');
PREPARE add_role FROM @sql; EXECUTE add_role; DEALLOCATE PREPARE add_role;
UPDATE users SET role = 'USER' WHERE role IS NULL OR role = '';

CREATE TABLE IF NOT EXISTS anime (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(160) NOT NULL, original_title VARCHAR(160), summary TEXT,
  cover_url VARCHAR(500), pv_url VARCHAR(500), season VARCHAR(30),
  media_type ENUM('ANIME','MOVIE') NOT NULL DEFAULT 'ANIME',
  status ENUM('AIRING','FINISHED','UPCOMING') NOT NULL DEFAULT 'UPCOMING',
  score DECIMAL(3,1) NOT NULL DEFAULT 0, score_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_anime_season (season), INDEX idx_anime_score (score)
) ENGINE=InnoDB;

SET @has_media_type = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'anime' AND column_name = 'media_type');
SET @sql = IF(@has_media_type = 0, 'ALTER TABLE anime ADD COLUMN media_type ENUM(''ANIME'',''MOVIE'') NOT NULL DEFAULT ''ANIME'' AFTER season', 'SELECT 1');
PREPARE add_media_type FROM @sql; EXECUTE add_media_type; DEALLOCATE PREPARE add_media_type;

CREATE TABLE IF NOT EXISTS genres (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(40) NOT NULL UNIQUE) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS anime_genres (
  anime_id BIGINT NOT NULL, genre_id BIGINT NOT NULL,
  PRIMARY KEY (anime_id, genre_id),
  FOREIGN KEY (anime_id) REFERENCES anime(id) ON DELETE CASCADE,
  FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ratings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, anime_id BIGINT NOT NULL,
  score TINYINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_anime_rating (user_id, anime_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (anime_id) REFERENCES anime(id) ON DELETE CASCADE,
  CHECK (score BETWEEN 1 AND 10)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, anime_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL, likes INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (anime_id) REFERENCES anime(id) ON DELETE CASCADE,
  INDEX idx_comments_anime_time (anime_id, created_at)
) ENGINE=InnoDB;

INSERT IGNORE INTO users(username, email, password_hash, role) VALUES
('screenroom', 'screenroom@example.com', '123456', 'USER'),
('admin', 'admin@example.com', 'admin123', 'ADMIN');
INSERT IGNORE INTO genres(name) VALUES ('奇幻'),('战斗'),('校园'),('青春'),('恋爱'),('治愈'),('动作'),('热血'),('冒险'),('科幻'),('悬疑'),('喜剧'),('日常'),('音乐'),('运动');
INSERT IGNORE INTO anime(title, original_title, summary, cover_url, pv_url, season, media_type, status, score, score_count) VALUES
('胆大党 第二季','DAN DA DAN Season 2','相信不可思议，也相信自己。','https://images.unsplash.com/photo-1578632767115-351597cf2477',NULL,'2026 夏','ANIME','AIRING',9.4,2486),
('薰香花朵凛然绽放','薫る花は凛と咲く','青春与温柔交错的校园故事。','https://images.unsplash.com/photo-1518709268805-4e9042af9f23',NULL,'2026 夏','ANIME','AIRING',9.1,1802),
('链锯人：剧场版','Chainsaw Man Movie','少年与恶魔之间无法预料的战斗。','https://images.unsplash.com/photo-1541562232579-512a21360020',NULL,'2026 夏','MOVIE','UPCOMING',9.0,1430),
('Summer Pockets','Summer Pockets','在海风吹过的夏日岛屿，遇见新的朋友。','https://images.unsplash.com/photo-1500534623283-312aade485b7',NULL,'2026 夏','ANIME','AIRING',8.8,986),
('药屋少女的呢喃 第二季','The Apothecary Diaries Season 2','宫廷谜案与少女成长交织的故事。','https://images.unsplash.com/photo-1518005020951-eccb494ad742',NULL,'2025 冬','ANIME','FINISHED',9.8,5320),
('葬送的芙莉莲','Frieren: Beyond Journey’s End','漫长旅途之后，重新理解相遇的意义。','https://images.unsplash.com/photo-1534447677768-be436bb09401',NULL,'2024 秋','ANIME','FINISHED',9.6,4812),
('铃芽之旅','Suzume','穿越灾厄与时空的少女旅程。','https://images.unsplash.com/photo-1489599849927-2ee91cede3ba',NULL,'2023 春','MOVIE','FINISHED',9.3,3670),
('你的名字。','Your Name.','在交错的梦境里寻找彼此。','https://images.unsplash.com/photo-1485846234645-a62644f84728',NULL,'2016 夏','MOVIE','FINISHED',9.5,7250);

INSERT IGNORE INTO anime_genres(anime_id, genre_id)
SELECT a.id, g.id FROM anime a JOIN genres g ON
(a.title = '胆大党 第二季' AND g.name IN ('奇幻','战斗','校园','喜剧')) OR
(a.title = '薰香花朵凛然绽放' AND g.name IN ('青春','恋爱','治愈','日常')) OR
(a.title = '链锯人：剧场版' AND g.name IN ('动作','热血','冒险','奇幻')) OR
(a.title = 'Summer Pockets' AND g.name IN ('青春','治愈','日常')) OR
(a.title = '药屋少女的呢喃 第二季' AND g.name IN ('恋爱','悬疑')) OR
(a.title = '葬送的芙莉莲' AND g.name IN ('奇幻','冒险','治愈')) OR
(a.title = '铃芽之旅' AND g.name IN ('奇幻','冒险','恋爱')) OR
(a.title = '你的名字。' AND g.name IN ('奇幻','恋爱','校园'));
