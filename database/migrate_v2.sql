-- Chạy một lần trên database hiện có; có thể chạy lại an toàn.
-- Dừng Server trước khi nâng cấp. Không xóa đơn/vé hoặc thay mật khẩu.
USE movie_tickets_ltm;
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_movie_tickets_v2$$
CREATE PROCEDURE upgrade_movie_tickets_v2()
BEGIN
    ALTER TABLE nguoi_dung MODIFY vai_tro ENUM('user','admin','staff') NOT NULL DEFAULT 'user';
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='don_dat_ve' AND column_name='ticket_token') THEN
        ALTER TABLE don_dat_ve ADD ticket_token VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='don_dat_ve' AND column_name='checked_in_at') THEN
        ALTER TABLE don_dat_ve ADD checked_in_at DATETIME NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='don_dat_ve' AND column_name='checked_in_by') THEN
        ALTER TABLE don_dat_ve ADD checked_in_by INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name='don_dat_ve' AND index_name='uq_don_ticket_token') THEN
        ALTER TABLE don_dat_ve ADD UNIQUE KEY uq_don_ticket_token(ticket_token);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema=DATABASE() AND table_name='don_dat_ve' AND constraint_name='fk_don_checker') THEN
        ALTER TABLE don_dat_ve ADD CONSTRAINT fk_don_checker
            FOREIGN KEY (checked_in_by) REFERENCES nguoi_dung(id);
    END IF;
END$$
CALL upgrade_movie_tickets_v2()$$
DROP PROCEDURE upgrade_movie_tickets_v2$$
DELIMITER ;
-- Token của đơn cũ được tạo an toàn khi chủ đơn mở Vé QR lần đầu.
-- Cấp quyền nhân viên cho tài khoản đã đăng ký (thay email thật):
-- UPDATE nguoi_dung SET vai_tro='staff' WHERE gmail='nhanvien@example.com';
