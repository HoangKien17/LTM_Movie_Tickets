# Chạy và kiểm thử Bước 7

Bản Bước 7 tiếp tục từ ZIP Bước 6. Database/schema.sql không đổi, nên nếu Bước 6 đã chạy như ảnh kiểm thử hai Client, không cần import lại SQL.

## 1. Chuẩn bị

1. Giữ bản Bước 6 để dự phòng. Giải nén ZIP Bước 7 vào thư mục mới rồi mở thư mục đó trong VS Code.
2. Dừng Server Bước 6 bằng Ctrl+C. Server Bước 7 dùng cùng cổng 2040.
3. Chuẩn bị hai tài khoản **thường** khác nhau (ví dụ client1 và client2) và một suất chiếu ở tương lai còn ít nhất một ghế trống.

## 2. Biên dịch

Chạy từng dòng trong Terminal PowerShell, tại thư mục gốc đã giải nén:

```powershell
$javaFiles = (Get-ChildItem -Recurse -File -Filter '*.java').FullName
New-Item -ItemType Directory -Path out -Force | Out-Null
javac -encoding UTF-8 --release 11 -d out $javaFiles
Test-Path .\out\tests\Step7SmokeTest.class
```

Dòng cuối phải hiện `True`.

## 3. Chạy Server Bước 7

Trong Terminal thứ nhất:

```powershell
$env:MOVIE_DB_URL = 'jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm'
$env:MOVIE_DB_USER = 'root'
$env:MOVIE_DB_PASSWORD = Read-Host 'Mật khẩu MySQL'
java -cp 'out;lib/*' server.BasicServer
```

Giữ Terminal này mở. Nếu MySQL không có mật khẩu, đặt `$env:MOVIE_DB_PASSWORD = ''`.

## 4. Chạy bài kiểm thử tự động

Mở Terminal thứ hai trong cùng thư mục. Nhập thông tin của hai tài khoản thường đã đăng ký qua Client:

```powershell
$env:MOVIE_TEST_USER_1 = Read-Host 'Email hoặc SĐT tài khoản 1'
$env:MOVIE_TEST_PASSWORD_1 = Read-Host 'Mật khẩu tài khoản 1'
$env:MOVIE_TEST_USER_2 = Read-Host 'Email hoặc SĐT tài khoản 2'
$env:MOVIE_TEST_PASSWORD_2 = Read-Host 'Mật khẩu tài khoản 2'
java -cp 'out;lib/*' tests.Step7SmokeTest
```

Kết quả mong đợi: bảy dòng `[1/7]` đến `[7/7]` đều `OK`, cuối cùng có `BƯỚC 7: TẤT CẢ KIỂM THỬ TỰ ĐỘNG ĐÃ ĐẠT`.

Bài kiểm thử **tạo một đơn vé rồi hủy đơn đó**. Nếu bị ngắt sau khi đặt thành công, chương trình sẽ cố hủy tự động; nếu không hủy được, Terminal in mã đơn để bạn hủy thủ công. Nó không tạo tài khoản mới và không cần quyền admin.

## 5. Kiểm thử giao diện và quản trị

Mở Terminal thứ ba:

```powershell
java -cp 'out;lib/*' web.BasicClientUI
```

Làm theo [TEST_STEP7.md](TEST_STEP7.md) để kiểm tra phần giao diện, đăng ký và quản trị mà bài tự động không thay thế.

Nếu bài kiểm thử lỗi, gửi ảnh **toàn bộ Terminal kiểm thử và Terminal Server**. Đừng gửi mật khẩu.
