package model;

/** Dữ liệu tài khoản do Repository đọc từ MySQL. */
public final class NguoiDung {
    private final int id;
    private final String hoTen;
    private final String gmail;
    private final String soDienThoai;
    private final String matKhauHash;
    private final String vaiTro;

    public NguoiDung(int id, String hoTen, String gmail, String soDienThoai,
                     String matKhauHash, String vaiTro) {
        this.id = id;
        this.hoTen = hoTen;
        this.gmail = gmail;
        this.soDienThoai = soDienThoai;
        this.matKhauHash = matKhauHash;
        this.vaiTro = vaiTro;
    }

    public int getId() { return id; }
    public String getHoTen() { return hoTen; }
    public String getGmail() { return gmail; }
    public String getSoDienThoai() { return soDienThoai; }
    public String getMatKhauHash() { return matKhauHash; }
    public String getVaiTro() { return vaiTro; }
}
