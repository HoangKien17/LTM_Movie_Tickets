-- Nâng cấp database V2 lên V3: đồ ăn, giữ ghế và thanh toán.
-- Dừng server trước khi chạy. Script không xóa đơn/vé hiện có.
USE movie_tickets_ltm;
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_movie_tickets_v3$$
CREATE PROCEDURE upgrade_movie_tickets_v3()
BEGIN
    ALTER TABLE don_dat_ve MODIFY trang_thai
        ENUM('pending_payment','active','cancelled','expired') NOT NULL DEFAULT 'pending_payment';
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='don_dat_ve' AND column_name='expires_at') THEN
        ALTER TABLE don_dat_ve ADD expires_at DATETIME NULL AFTER ngay_dat;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='don_dat_ve' AND column_name='paid_at') THEN
        ALTER TABLE don_dat_ve ADD paid_at DATETIME NULL AFTER expires_at;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='ve' AND index_name='ix_ve_xuat') THEN
        ALTER TABLE ve ADD INDEX ix_ve_xuat(xuat_chieu_id);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
        AND table_name='ve' AND index_name='uq_ghe_con_hieu_luc') THEN
        ALTER TABLE ve DROP INDEX uq_ghe_con_hieu_luc;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='ve' AND column_name='active_slot') THEN
        ALTER TABLE ve DROP COLUMN active_slot;
    END IF;
    ALTER TABLE ve MODIFY trang_thai ENUM('held','active','cancelled','expired') NOT NULL DEFAULT 'held';
    ALTER TABLE ve ADD active_slot TINYINT GENERATED ALWAYS AS
        (CASE WHEN trang_thai IN ('held','active') THEN 1 ELSE NULL END) STORED;
    ALTER TABLE ve ADD UNIQUE KEY uq_ghe_con_hieu_luc(xuat_chieu_id,ghe_id,active_slot);
END$$
CALL upgrade_movie_tickets_v3()$$
DROP PROCEDURE upgrade_movie_tickets_v3$$
DELIMITER ;

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
    PRIMARY KEY (don_dat_ve_id,san_pham_id),
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
    provider_payment_id VARCHAR(100), qr_payload TEXT, checkout_url VARCHAR(500),
    transaction_ref VARCHAR(150), ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    CONSTRAINT fk_thanh_toan_don FOREIGN KEY (don_dat_ve_id) REFERENCES don_dat_ve(id),
    INDEX ix_thanh_toan_pending(trang_thai,ngay_tao)
) ENGINE=InnoDB;

INSERT INTO san_pham (id,ten_san_pham,loai,gia,dang_ban) VALUES
    (1,'Bắp rang bơ nhỏ','food',35000,1),(2,'Bắp rang bơ lớn','food',55000,1),
    (3,'Pepsi','drink',30000,1),(4,'Nước suối','drink',20000,1)
ON DUPLICATE KEY UPDATE ten_san_pham=VALUES(ten_san_pham),loai=VALUES(loai),gia=VALUES(gia);
