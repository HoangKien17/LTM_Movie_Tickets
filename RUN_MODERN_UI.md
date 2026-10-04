# Chạy giao diện mới trước Bước 8

Bản này dùng toàn bộ logic đã kiểm thử ở Bước 7. Chỉ có `web/BasicClientUI.java` được thiết kế lại và `web/UiTheme.java` được thêm vào để quản lý màu sắc, nút, bảng và ghế. Không thay đổi database/schema.sql, TCP Server hay cách đặt vé.

## Cách dùng ZIP mới

1. Giữ thư mục Bước 7 làm bản dự phòng. Giải nén `LTM-Movie-Tickets-ModernUI.zip` vào thư mục mới, rồi mở thư mục đó trong VS Code.
2. **Không import lại SQL.** Tài khoản, phim và vé vẫn nằm trong MySQL của bạn.
3. Nếu Server Bước 7 đang chạy ở cổng 2040, giữ nó chạy. Không mở thêm một Server cùng cổng.

## Biên dịch trong Terminal PowerShell của VS Code

Tại thư mục vừa giải nén, chạy từng dòng:

```powershell
$javaFiles = (Get-ChildItem -Recurse -File -Filter '*.java').FullName
New-Item -ItemType Directory -Path out -Force | Out-Null
javac -encoding UTF-8 --release 11 -d out $javaFiles
Test-Path .\out\web\BasicClientUI.class
```

Dòng cuối cần hiện `True`.

## Chạy Client

Trong Terminal khác hoặc sau khi biên dịch xong:

```powershell
java -cp 'out;lib/*' web.BasicClientUI
```

Nếu Server chưa chạy, mở Terminal riêng và dùng lại lệnh Server trong `RUN_STEP7.md`.

## Kiểm tra nhanh

- Đăng nhập bằng tài khoản đã có, sau đó thử màn hình đăng ký và quay lại.
- Ở “Phim đang chiếu”, nhấn “Xem suất chiếu” trên một thẻ phim.
- Chọn suất chiếu, chọn ghế màu xanh, kiểm tra tổng tiền rồi đặt vé.
- Vào “Vé của tôi” để xem chi tiết và hủy thử một đơn tương lai.
- Tài khoản admin nhìn thấy mục “Quản trị”; tài khoản thường không thấy. Thử danh sách phim, phòng, suất chiếu và thống kê.

Giao diện dùng chỗ giữ ảnh phim dạng màu chuyển sắc vì phim mẫu chưa có ảnh. Bản này giữ Java Swing + TCP Socket + MySQL.
