-- init.sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parking_space (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    space_code VARCHAR(20) NOT NULL,
    area VARCHAR(20),
    status INT DEFAULT 0 COMMENT '0空闲 1占用',
    x_coordinate DOUBLE DEFAULT 0,
    y_coordinate DOUBLE DEFAULT 0
);

CREATE TABLE IF NOT EXISTS parking_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(20) NOT NULL,
    space_id BIGINT DEFAULT NULL COMMENT '车位ID，入场时可为空，选车位后更新',
    entry_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    exit_time DATETIME,
    status INT DEFAULT 0 COMMENT '0进行中 1已完成'
);

CREATE TABLE IF NOT EXISTS parking_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    amount DECIMAL(10,2) DEFAULT 10.00,
    pay_status INT DEFAULT 0 COMMENT '0待支付 1已支付',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Seed data
-- admin / Admin@2026
INSERT IGNORE INTO sys_user (id, username, password, role) VALUES
(1, 'admin', '$2b$10$FZ3Ai170DK0JvDMWDc.8X.OV82dMOHAkjg1N4P7.LmOIXJtfrRdl.', 'admin');

INSERT IGNORE INTO parking_space (id, space_code, area, status, x_coordinate, y_coordinate) VALUES
(1,  'A101', 'A区', 0, 100, 100),
(2,  'A102', 'A区', 0, 250, 100),
(3,  'A103', 'A区', 0, 400, 100),
(4,  'A104', 'A区', 0, 550, 100),
(5,  'A105', 'A区', 0, 700, 100),
(6,  'B101', 'B区', 1, 100, 400),
(7,  'B102', 'B区', 1, 250, 400),
(8,  'B103', 'B区', 1, 400, 400),
(9,  'B104', 'B区', 1, 550, 400),
(10, 'B105', 'B区', 1, 700, 400);
