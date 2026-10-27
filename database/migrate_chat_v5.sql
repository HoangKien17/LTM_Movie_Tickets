-- Chạy trên database đang dùng; không xóa hoặc sửa dữ liệu vé hiện có.
USE movie_tickets_ltm;

CREATE TABLE IF NOT EXISTS tin_nhan_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    xuat_chieu_id INT NOT NULL,
    nguoi_dung_id INT NOT NULL,
    noi_dung VARCHAR(2000) NOT NULL,
    ngay_gui DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY ix_chat_show_id (xuat_chieu_id, id),
    CONSTRAINT fk_chat_show FOREIGN KEY (xuat_chieu_id) REFERENCES xuat_chieu(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_user FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
