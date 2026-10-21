USE movie_tickets_ltm;

CREATE TABLE IF NOT EXISTS thong_bao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nguoi_dung_id INT NOT NULL,
    don_dat_ve_id BIGINT NOT NULL,
    loai ENUM('booking_success', 'show_reminder') NOT NULL,
    ngay_tao DATETIME NOT NULL,
    da_doc BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uq_thong_bao_don_loai (nguoi_dung_id, don_dat_ve_id, loai),
    KEY ix_thong_bao_user_time (nguoi_dung_id, ngay_tao),
    CONSTRAINT fk_thong_bao_user FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id),
    CONSTRAINT fk_thong_bao_order FOREIGN KEY (don_dat_ve_id) REFERENCES don_dat_ve(id)
) ENGINE=InnoDB;
