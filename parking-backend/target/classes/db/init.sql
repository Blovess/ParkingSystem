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
    type VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL普通车位 CHARGING充电桩车位',
    zone VARCHAR(20) COMMENT '所属区块：A区/B区/C区/D区',
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

-- ==================== Graph Model ====================
CREATE TABLE IF NOT EXISTS graph_vertex (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '顶点名称',
    type VARCHAR(20) NOT NULL COMMENT 'ENTRY/EXIT/ELEVATOR1/ELEVATOR2/ROAD',
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS graph_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_vertex_id BIGINT NOT NULL,
    to_vertex_id BIGINT NOT NULL,
    weight DOUBLE NOT NULL COMMENT '边的权重（欧氏距离）',
    FOREIGN KEY (from_vertex_id) REFERENCES graph_vertex(id),
    FOREIGN KEY (to_vertex_id) REFERENCES graph_vertex(id)
);

-- Seed data
-- admin / Admin@2026
INSERT IGNORE INTO sys_user (id, username, password, role) VALUES
(1, 'admin', '$2b$10$FZ3Ai170DK0JvDMWDc.8X.OV82dMOHAkjg1N4P7.LmOIXJtfrRdl.', 'admin');

-- Clear old parking_space data then insert all 136 spots matching the canvas layout
TRUNCATE TABLE parking_space;

-- A区: 20 spots (IDs 1-20), zone='A区'
-- Charging: A001-A005 (IDs 1-5), type='CHARGING'
INSERT INTO parking_space (id, space_code, area, status, type, zone, x_coordinate, y_coordinate) VALUES
(1, 'A001', 'A', 0, 'CHARGING', 'A区', 52, 65),
(2, 'A002', 'A', 0, 'CHARGING', 'A区', 86, 65),
(3, 'A003', 'A', 0, 'CHARGING', 'A区', 120, 65),
(4, 'A004', 'A', 0, 'CHARGING', 'A区', 154, 65),
(5, 'A005', 'A', 0, 'CHARGING', 'A区', 188, 65),
(6, 'A006', 'A', 0, 'NORMAL', 'A区', 256, 65),
(7, 'A007', 'A', 0, 'NORMAL', 'A区', 290, 65),
(8, 'A008', 'A', 0, 'NORMAL', 'A区', 324, 65),
(9, 'A009', 'A', 0, 'NORMAL', 'A区', 358, 65),
(10, 'A010', 'A', 0, 'NORMAL', 'A区', 392, 65),
(11, 'A011', 'A', 0, 'NORMAL', 'A区', 562, 65),
(12, 'A012', 'A', 0, 'NORMAL', 'A区', 596, 65),
(13, 'A013', 'A', 0, 'NORMAL', 'A区', 630, 65),
(14, 'A014', 'A', 0, 'NORMAL', 'A区', 664, 65),
(15, 'A015', 'A', 0, 'NORMAL', 'A区', 698, 65),
(16, 'A016', 'A', 0, 'NORMAL', 'A区', 766, 65),
(17, 'A017', 'A', 0, 'NORMAL', 'A区', 800, 65),
(18, 'A018', 'A', 0, 'NORMAL', 'A区', 834, 65),
(19, 'A019', 'A', 0, 'NORMAL', 'A区', 868, 65),
(20, 'A020', 'A', 0, 'NORMAL', 'A区', 902, 65),

-- B区: 48 spots (IDs 21-68), zone='B区', all NORMAL
(21, 'B001', 'B', 0, 'NORMAL', 'B区', 52, 185),
(22, 'B002', 'B', 0, 'NORMAL', 'B区', 86, 185),
(23, 'B003', 'B', 0, 'NORMAL', 'B区', 120, 185),
(24, 'B004', 'B', 0, 'NORMAL', 'B区', 154, 185),
(25, 'B005', 'B', 0, 'NORMAL', 'B区', 188, 185),
(26, 'B006', 'B', 0, 'NORMAL', 'B区', 52, 245),
(27, 'B007', 'B', 0, 'NORMAL', 'B区', 86, 245),
(28, 'B008', 'B', 0, 'NORMAL', 'B区', 120, 245),
(29, 'B009', 'B', 0, 'NORMAL', 'B区', 154, 245),
(30, 'B010', 'B', 0, 'NORMAL', 'B区', 188, 245),
(31, 'B011', 'B', 0, 'NORMAL', 'B区', 222, 185),
(32, 'B012', 'B', 0, 'NORMAL', 'B区', 256, 185),
(33, 'B013', 'B', 0, 'NORMAL', 'B区', 290, 185),
(34, 'B014', 'B', 0, 'NORMAL', 'B区', 324, 185),
(35, 'B015', 'B', 0, 'NORMAL', 'B区', 358, 185),
(36, 'B016', 'B', 0, 'NORMAL', 'B区', 426, 185),
(37, 'B017', 'B', 0, 'NORMAL', 'B区', 460, 185),
(38, 'B018', 'B', 0, 'NORMAL', 'B区', 494, 185),
(39, 'B019', 'B', 0, 'NORMAL', 'B区', 528, 185),
(40, 'B020', 'B', 0, 'NORMAL', 'B区', 596, 185),
(41, 'B021', 'B', 0, 'NORMAL', 'B区', 630, 185),
(42, 'B022', 'B', 0, 'NORMAL', 'B区', 664, 185),
(43, 'B023', 'B', 0, 'NORMAL', 'B区', 698, 185),
(44, 'B024', 'B', 0, 'NORMAL', 'B区', 732, 185),
(45, 'B025', 'B', 0, 'NORMAL', 'B区', 222, 245),
(46, 'B026', 'B', 0, 'NORMAL', 'B区', 256, 245),
(47, 'B027', 'B', 0, 'NORMAL', 'B区', 290, 245),
(48, 'B028', 'B', 0, 'NORMAL', 'B区', 324, 245),
(49, 'B029', 'B', 0, 'NORMAL', 'B区', 358, 245),
(50, 'B030', 'B', 0, 'NORMAL', 'B区', 426, 245),
(51, 'B031', 'B', 0, 'NORMAL', 'B区', 460, 245),
(52, 'B032', 'B', 0, 'NORMAL', 'B区', 494, 245),
(53, 'B033', 'B', 0, 'NORMAL', 'B区', 528, 245),
(54, 'B034', 'B', 0, 'NORMAL', 'B区', 596, 245),
(55, 'B035', 'B', 0, 'NORMAL', 'B区', 630, 245),
(56, 'B036', 'B', 0, 'NORMAL', 'B区', 664, 245),
(57, 'B037', 'B', 0, 'NORMAL', 'B区', 698, 245),
(58, 'B038', 'B', 0, 'NORMAL', 'B区', 732, 245),
(59, 'B039', 'B', 0, 'NORMAL', 'B区', 766, 185),
(60, 'B040', 'B', 0, 'NORMAL', 'B区', 800, 185),
(61, 'B041', 'B', 0, 'NORMAL', 'B区', 834, 185),
(62, 'B042', 'B', 0, 'NORMAL', 'B区', 868, 185),
(63, 'B043', 'B', 0, 'NORMAL', 'B区', 902, 185),
(64, 'B044', 'B', 0, 'NORMAL', 'B区', 766, 245),
(65, 'B045', 'B', 0, 'NORMAL', 'B区', 800, 245),
(66, 'B046', 'B', 0, 'NORMAL', 'B区', 834, 245),
(67, 'B047', 'B', 0, 'NORMAL', 'B区', 868, 245),
(68, 'B048', 'B', 0, 'NORMAL', 'B区', 902, 245),

-- C区: 48 spots (IDs 69-116), zone='C区'
-- Charging: C039-C048 (IDs 107-116)
(69, 'C001', 'C', 0, 'NORMAL', 'C区', 52, 365),
(70, 'C002', 'C', 0, 'NORMAL', 'C区', 86, 365),
(71, 'C003', 'C', 0, 'NORMAL', 'C区', 120, 365),
(72, 'C004', 'C', 0, 'NORMAL', 'C区', 154, 365),
(73, 'C005', 'C', 0, 'NORMAL', 'C区', 188, 365),
(74, 'C006', 'C', 0, 'NORMAL', 'C区', 52, 425),
(75, 'C007', 'C', 0, 'NORMAL', 'C区', 86, 425),
(76, 'C008', 'C', 0, 'NORMAL', 'C区', 120, 425),
(77, 'C009', 'C', 0, 'NORMAL', 'C区', 154, 425),
(78, 'C010', 'C', 0, 'NORMAL', 'C区', 188, 425),
(79, 'C011', 'C', 0, 'NORMAL', 'C区', 222, 365),
(80, 'C012', 'C', 0, 'NORMAL', 'C区', 256, 365),
(81, 'C013', 'C', 0, 'NORMAL', 'C区', 290, 365),
(82, 'C014', 'C', 0, 'NORMAL', 'C区', 324, 365),
(83, 'C015', 'C', 0, 'NORMAL', 'C区', 358, 365),
(84, 'C016', 'C', 0, 'NORMAL', 'C区', 426, 365),
(85, 'C017', 'C', 0, 'NORMAL', 'C区', 460, 365),
(86, 'C018', 'C', 0, 'NORMAL', 'C区', 494, 365),
(87, 'C019', 'C', 0, 'NORMAL', 'C区', 528, 365),
(88, 'C020', 'C', 0, 'NORMAL', 'C区', 596, 365),
(89, 'C021', 'C', 0, 'NORMAL', 'C区', 630, 365),
(90, 'C022', 'C', 0, 'NORMAL', 'C区', 664, 365),
(91, 'C023', 'C', 0, 'NORMAL', 'C区', 698, 365),
(92, 'C024', 'C', 0, 'NORMAL', 'C区', 732, 365),
(93, 'C025', 'C', 0, 'NORMAL', 'C区', 222, 425),
(94, 'C026', 'C', 0, 'NORMAL', 'C区', 256, 425),
(95, 'C027', 'C', 0, 'NORMAL', 'C区', 290, 425),
(96, 'C028', 'C', 0, 'NORMAL', 'C区', 324, 425),
(97, 'C029', 'C', 0, 'NORMAL', 'C区', 358, 425),
(98, 'C030', 'C', 0, 'NORMAL', 'C区', 426, 425),
(99, 'C031', 'C', 0, 'NORMAL', 'C区', 460, 425),
(100, 'C032', 'C', 0, 'NORMAL', 'C区', 494, 425),
(101, 'C033', 'C', 0, 'NORMAL', 'C区', 528, 425),
(102, 'C034', 'C', 0, 'NORMAL', 'C区', 596, 425),
(103, 'C035', 'C', 0, 'NORMAL', 'C区', 630, 425),
(104, 'C036', 'C', 0, 'NORMAL', 'C区', 664, 425),
(105, 'C037', 'C', 0, 'NORMAL', 'C区', 698, 425),
(106, 'C038', 'C', 0, 'NORMAL', 'C区', 732, 425),
(107, 'C039', 'C', 0, 'CHARGING', 'C区', 766, 365),
(108, 'C040', 'C', 0, 'CHARGING', 'C区', 800, 365),
(109, 'C041', 'C', 0, 'CHARGING', 'C区', 834, 365),
(110, 'C042', 'C', 0, 'CHARGING', 'C区', 868, 365),
(111, 'C043', 'C', 0, 'CHARGING', 'C区', 902, 365),
(112, 'C044', 'C', 0, 'CHARGING', 'C区', 766, 425),
(113, 'C045', 'C', 0, 'CHARGING', 'C区', 800, 425),
(114, 'C046', 'C', 0, 'CHARGING', 'C区', 834, 425),
(115, 'C047', 'C', 0, 'CHARGING', 'C区', 868, 425),
(116, 'C048', 'C', 0, 'CHARGING', 'C区', 902, 425),

-- D区: 20 spots (IDs 117-136), zone='D区', all NORMAL
(117, 'D001', 'D', 0, 'NORMAL', 'D区', 52, 545),
(118, 'D002', 'D', 0, 'NORMAL', 'D区', 86, 545),
(119, 'D003', 'D', 0, 'NORMAL', 'D区', 120, 545),
(120, 'D004', 'D', 0, 'NORMAL', 'D区', 154, 545),
(121, 'D005', 'D', 0, 'NORMAL', 'D区', 188, 545),
(122, 'D006', 'D', 0, 'NORMAL', 'D区', 256, 545),
(123, 'D007', 'D', 0, 'NORMAL', 'D区', 290, 545),
(124, 'D008', 'D', 0, 'NORMAL', 'D区', 324, 545),
(125, 'D009', 'D', 0, 'NORMAL', 'D区', 358, 545),
(126, 'D010', 'D', 0, 'NORMAL', 'D区', 392, 545),
(127, 'D011', 'D', 0, 'NORMAL', 'D区', 562, 545),
(128, 'D012', 'D', 0, 'NORMAL', 'D区', 596, 545),
(129, 'D013', 'D', 0, 'NORMAL', 'D区', 630, 545),
(130, 'D014', 'D', 0, 'NORMAL', 'D区', 664, 545),
(131, 'D015', 'D', 0, 'NORMAL', 'D区', 698, 545),
(132, 'D016', 'D', 0, 'NORMAL', 'D区', 766, 545),
(133, 'D017', 'D', 0, 'NORMAL', 'D区', 800, 545),
(134, 'D018', 'D', 0, 'NORMAL', 'D区', 834, 545),
(135, 'D019', 'D', 0, 'NORMAL', 'D区', 868, 545),
(136, 'D020', 'D', 0, 'NORMAL', 'D区', 902, 545);

-- ==================== Graph Vertex Seed Data ====================
-- Road layout matches the canvas in ParkingMonitor.vue:
--   3 horizontal roads: y=125 (H1, A-B), y=305 (H2, B-C), y=485 (H3, C-D)
--   5 vertical lanes: x=35 (left), x=188 (gx=4), x=477 (center/elevator), x=766 (gx=21), x=919 (right)
--   Entry: left side of H2 (near "← 入口")
--   Exit: right side of H1 (near "出口 →")

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE graph_edge;
TRUNCATE TABLE graph_vertex;
SET FOREIGN_KEY_CHECKS = 1;

-- Vertices (19 total)
INSERT INTO graph_vertex (id, name, type, x, y) VALUES
-- Special vertices
(1, '入口', 'ENTRY', 0, 305),
(2, '出口', 'EXIT', 954, 125),
(3, '1号电梯', 'ELEVATOR1', 477, 65),
(4, '2号电梯', 'ELEVATOR2', 477, 545),

-- H1 road (y=125): left → gx=4 → center/elevator → gx=21 → right
(5, 'H1_L', 'ROAD', 35, 125),
(6, 'H1_C', 'ROAD', 477, 125),
(7, 'H1_R', 'ROAD', 919, 125),

-- H2 road (y=305): left → gx=4 → center → gx=21 → right
(8, 'H2_L', 'ROAD', 35, 305),
(9, 'H2_C', 'ROAD', 477, 305),
(10, 'H2_R', 'ROAD', 919, 305),

-- H3 road (y=485): left → gx=4 → center → gx=21 → right
(11, 'H3_L', 'ROAD', 35, 485),
(12, 'H3_C', 'ROAD', 477, 485),
(13, 'H3_R', 'ROAD', 919, 485),

-- Vertical road at gx=4 (x=188): connects H1-H2-H3
(14, 'V4_H1', 'ROAD', 188, 125),
(15, 'V4_H2', 'ROAD', 188, 305),
(16, 'V4_H3', 'ROAD', 188, 485),

-- Vertical road at gx=21 (x=766): connects H1-H2-H3
(17, 'V21_H1', 'ROAD', 766, 125),
(18, 'V21_H2', 'ROAD', 766, 305),
(19, 'V21_H3', 'ROAD', 766, 485);

-- ==================== Graph Edge Seed Data ====================
-- All edges are bidirectional (each pair stored once, graph is undirected)

-- === H1 horizontal (y=125) ===
-- H1_L(5) ↔ V4_H1(14): |188-35| = 153
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (5, 14, 153);
-- V4_H1(14) ↔ H1_C(6): |477-188| = 289
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (14, 6, 289);
-- H1_C(6) ↔ V21_H1(17): |766-477| = 289
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (6, 17, 289);
-- V21_H1(17) ↔ H1_R(7): |919-766| = 153
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (17, 7, 153);

-- === H2 horizontal (y=305) ===
-- H2_L(8) ↔ V4_H2(15): |188-35| = 153
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (8, 15, 153);
-- V4_H2(15) ↔ H2_C(9): |477-188| = 289
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (15, 9, 289);
-- H2_C(9) ↔ V21_H2(18): |766-477| = 289
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (9, 18, 289);
-- V21_H2(18) ↔ H2_R(10): |919-766| = 153
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (18, 10, 153);

-- === H3 horizontal (y=485) ===
-- H3_L(11) ↔ V4_H3(16): |188-35| = 153
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (11, 16, 153);
-- V4_H3(16) ↔ H3_C(12): |477-188| = 289
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (16, 12, 289);
-- H3_C(12) ↔ V21_H3(19): |766-477| = 289
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (12, 19, 289);
-- V21_H3(19) ↔ H3_R(13): |919-766| = 153
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (19, 13, 153);

-- === Vertical x=35 (left edge) ===
-- H1_L(5) ↔ H2_L(8): |305-125| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (5, 8, 180);
-- H2_L(8) ↔ H3_L(11): |485-305| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (8, 11, 180);

-- === Vertical x=188 (gx=4 road) ===
-- V4_H1(14) ↔ V4_H2(15): |305-125| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (14, 15, 180);
-- V4_H2(15) ↔ V4_H3(16): |485-305| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (15, 16, 180);

-- === Vertical x=477 (center / elevator line) ===
-- H1_C(6) ↔ H2_C(9): |305-125| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (6, 9, 180);
-- H2_C(9) ↔ H3_C(12): |485-305| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (9, 12, 180);

-- === Vertical x=766 (gx=21 road) ===
-- V21_H1(17) ↔ V21_H2(18): |305-125| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (17, 18, 180);
-- V21_H2(18) ↔ V21_H3(19): |485-305| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (18, 19, 180);

-- === Vertical x=919 (right edge) ===
-- H1_R(7) ↔ H2_R(10): |305-125| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (7, 10, 180);
-- H2_R(10) ↔ H3_R(13): |485-305| = 180
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (10, 13, 180);

-- === Entry/Exit connections ===
-- ENTRY(1) ↔ H2_L(8): |35-0| = 35
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (1, 8, 35);
-- EXIT(2) ↔ H1_R(7): |954-919| = 35
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (2, 7, 35);

-- === Elevator connections ===
-- ELEVATOR1(3) ↔ H1_C(6): |125-65| = 60
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (3, 6, 60);
-- ELEVATOR2(4) ↔ H3_C(12): |545-485| = 60
INSERT INTO graph_edge (from_vertex_id, to_vertex_id, weight) VALUES (4, 12, 60);
