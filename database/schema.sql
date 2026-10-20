CREATE DATABASE IF NOT EXISTS movie_tickets_ltm
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE movie_tickets_ltm;

CREATE TABLE IF NOT EXISTS nguoi_dung (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    gmail VARCHAR(100) NOT NULL UNIQUE,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    mat_khau VARCHAR(255) NOT NULL,
    vai_tro ENUM('user', 'admin', 'staff') NOT NULL DEFAULT 'user',
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
    trang_thai ENUM('pending_payment', 'active', 'cancelled', 'expired') NOT NULL DEFAULT 'pending_payment',
    ngay_dat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    paid_at DATETIME NULL,
    ticket_token VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    checked_in_at DATETIME NULL,
    checked_in_by INT NULL,
    UNIQUE KEY uq_don_ticket_token (ticket_token),
    CONSTRAINT fk_don_checker FOREIGN KEY (checked_in_by) REFERENCES nguoi_dung(id),
    CONSTRAINT fk_don_nguoi FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id),
    CONSTRAINT fk_don_xuat FOREIGN KEY (xuat_chieu_id) REFERENCES xuat_chieu(id),
    INDEX ix_don_nguoi (nguoi_dung_id, ngay_dat)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ve (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    don_dat_ve_id BIGINT NOT NULL,
    xuat_chieu_id INT NOT NULL,
    ghe_id INT NOT NULL,
    trang_thai ENUM('held', 'active', 'cancelled', 'expired') NOT NULL DEFAULT 'held',
    active_slot TINYINT GENERATED ALWAYS AS
        (CASE WHEN trang_thai IN ('held','active') THEN 1 ELSE NULL END) STORED,
    CONSTRAINT fk_ve_don FOREIGN KEY (don_dat_ve_id) REFERENCES don_dat_ve(id),
    CONSTRAINT fk_ve_xuat FOREIGN KEY (xuat_chieu_id) REFERENCES xuat_chieu(id),
    CONSTRAINT fk_ve_ghe FOREIGN KEY (ghe_id) REFERENCES ghe(id),
    INDEX ix_ve_xuat (xuat_chieu_id),
    UNIQUE KEY uq_ghe_con_hieu_luc (xuat_chieu_id, ghe_id, active_slot)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS san_pham (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ten_san_pham VARCHAR(120) NOT NULL,
    loai ENUM('food','drink') NOT NULL,
    gia DECIMAL(12,2) NOT NULL,
    dang_ban TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT chk_san_pham_gia CHECK (gia >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chi_tiet_do_an (
    don_dat_ve_id BIGINT NOT NULL,
    san_pham_id INT NOT NULL,
    so_luong INT NOT NULL,
    don_gia DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (don_dat_ve_id, san_pham_id),
    CONSTRAINT fk_ctda_don FOREIGN KEY (don_dat_ve_id) REFERENCES don_dat_ve(id),
    CONSTRAINT fk_ctda_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham(id),
    CONSTRAINT chk_ctda_so_luong CHECK (so_luong BETWEEN 1 AND 20)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS thanh_toan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    don_dat_ve_id BIGINT NOT NULL UNIQUE,
    nha_cung_cap VARCHAR(20) NOT NULL,
    order_code BIGINT NOT NULL UNIQUE,
    so_tien DECIMAL(14,2) NOT NULL,
    trang_thai ENUM('pending','paid','cancelled','expired','failed') NOT NULL DEFAULT 'pending',
    provider_payment_id VARCHAR(100),
    qr_payload TEXT,
    checkout_url VARCHAR(500),
    transaction_ref VARCHAR(150),
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    CONSTRAINT fk_thanh_toan_don FOREIGN KEY (don_dat_ve_id) REFERENCES don_dat_ve(id),
    INDEX ix_thanh_toan_pending (trang_thai, ngay_tao)
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

INSERT INTO san_pham (id,ten_san_pham,loai,gia,dang_ban) VALUES
    (1,'Bắp rang bơ nhỏ','food',35000,1),
    (2,'Bắp rang bơ lớn','food',55000,1),
    (3,'Pepsi','drink',30000,1),
    (4,'Nước suối','drink',20000,1)
ON DUPLICATE KEY UPDATE ten_san_pham=VALUES(ten_san_pham),loai=VALUES(loai),gia=VALUES(gia);

-- Sau khi đăng ký tài khoản qua Client, cấp quyền quản trị có chủ đích:
-- UPDATE nguoi_dung SET vai_tro = 'admin' WHERE gmail = 'email-cua-ban@example.com';
