CREATE DATABASE IF NOT EXISTS movie_tickets_ltm
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE movie_tickets_ltm;

CREATE TABLE IF NOT EXISTS nguoi_dung (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    gmail VARCHAR(100) NOT NULL UNIQUE,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    mat_khau VARCHAR(255) NOT NULL,
    vai_tro ENUM('user', 'admin') NOT NULL DEFAULT 'user',
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS phim (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ten_phim VARCHAR(200) NOT NULL,
    the_loai VARCHAR(100),
    thoi_luong INT NOT NULL,
    ngay_khoi_chieu DATE,
    mo_ta TEXT,
    anh VARCHAR(255),
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS phong_chieu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ten_phong VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ghe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phong_chieu_id INT NOT NULL,
    ma_ghe VARCHAR(8) NOT NULL,
    CONSTRAINT fk_ghe_phong FOREIGN KEY (phong_chieu_id) REFERENCES phong_chieu(id),
    UNIQUE KEY uq_ghe_phong_ma (phong_chieu_id, ma_ghe)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS xuat_chieu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phim_id INT NOT NULL,
    phong_chieu_id INT NOT NULL,
    bat_dau DATETIME NOT NULL,
    gia_ve DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_xuat_phim FOREIGN KEY (phim_id) REFERENCES phim(id),
    CONSTRAINT fk_xuat_phong FOREIGN KEY (phong_chieu_id) REFERENCES phong_chieu(id),
    CONSTRAINT chk_gia_ve CHECK (gia_ve >= 0),
    UNIQUE KEY uq_phong_gio (phong_chieu_id, bat_dau)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS don_dat_ve (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nguoi_dung_id INT NOT NULL,
    xuat_chieu_id INT NOT NULL,
    tong_tien DECIMAL(14,2) NOT NULL,
    trang_thai ENUM('active', 'cancelled') NOT NULL DEFAULT 'active',
    ngay_dat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_don_nguoi FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id),
    CONSTRAINT fk_don_xuat FOREIGN KEY (xuat_chieu_id) REFERENCES xuat_chieu(id),
    INDEX ix_don_nguoi (nguoi_dung_id, ngay_dat)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ve (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    don_dat_ve_id BIGINT NOT NULL,
    xuat_chieu_id INT NOT NULL,
    ghe_id INT NOT NULL,
    trang_thai ENUM('active', 'cancelled') NOT NULL DEFAULT 'active',
    active_slot TINYINT GENERATED ALWAYS AS
        (CASE WHEN trang_thai = 'active' THEN 1 ELSE NULL END) STORED,
    CONSTRAINT fk_ve_don FOREIGN KEY (don_dat_ve_id) REFERENCES don_dat_ve(id),
    CONSTRAINT fk_ve_xuat FOREIGN KEY (xuat_chieu_id) REFERENCES xuat_chieu(id),
    CONSTRAINT fk_ve_ghe FOREIGN KEY (ghe_id) REFERENCES ghe(id),
    UNIQUE KEY uq_ghe_con_hieu_luc (xuat_chieu_id, ghe_id, active_slot)
) ENGINE=InnoDB;

INSERT INTO phim (id, ten_phim, the_loai, thoi_luong, ngay_khoi_chieu, mo_ta, anh)
VALUES (1, 'Phim mẫu', 'Hoạt hình', 90, '2026-10-01',
        'Dữ liệu mẫu để kiểm tra luồng Swing, TCP Server và MySQL.', '')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO phong_chieu (id, ten_phong) VALUES (1, 'Phòng 1')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO ghe (phong_chieu_id, ma_ghe) VALUES
    (1, 'A1'), (1, 'A2'), (1, 'A3'), (1, 'A4'),
    (1, 'B1'), (1, 'B2'), (1, 'B3'), (1, 'B4'),
    (1, 'C1'), (1, 'C2'), (1, 'C3'), (1, 'C4')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO xuat_chieu (id, phim_id, phong_chieu_id, bat_dau, gia_ve)
VALUES (1, 1, 1, CONCAT(DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), ' 19:00:00'), 75000)
ON DUPLICATE KEY UPDATE id = id;

-- Sau khi đăng ký tài khoản qua Client, cấp quyền quản trị có chủ đích:
-- UPDATE nguoi_dung SET vai_tro = 'admin' WHERE gmail = 'email-cua-ban@example.com';
