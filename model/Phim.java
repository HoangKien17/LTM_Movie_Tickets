package model;

import java.time.LocalDate;

/** Thông tin phim độc lập với giao diện, Socket và JDBC. */
public final class Phim {
    private final int id;
    private final String tenPhim;
    private final String theLoai;
    private final int thoiLuong;
    private final LocalDate ngayKhoiChieu;
    private final String moTa;
    private final String anh;

    public Phim(int id, String tenPhim, String theLoai, int thoiLuong,
                LocalDate ngayKhoiChieu, String moTa, String anh) {
        this.id = id;
        this.tenPhim = tenPhim;
        this.theLoai = theLoai;
        this.thoiLuong = thoiLuong;
        this.ngayKhoiChieu = ngayKhoiChieu;
        this.moTa = moTa;
        this.anh = anh;
    }

    public int getId() { return id; }
    public String getTenPhim() { return tenPhim; }
    public String getTheLoai() { return theLoai; }
    public int getThoiLuong() { return thoiLuong; }
    public LocalDate getNgayKhoiChieu() { return ngayKhoiChieu; }
    public String getMoTa() { return moTa; }
    public String getAnh() { return anh; }
}
