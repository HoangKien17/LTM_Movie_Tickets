# LTM Movie Tickets

Ứng dụng đặt vé xem phim cho môn Lập trình mạng, dùng Java Swing, TCP Socket
và MySQL. Client chỉ kết nối với Server; Server xử lý nghiệp vụ và truy cập MySQL.

## Chuẩn bị

- Cài JDK 11 trở lên và khởi động MySQL.
- Lần đầu dùng dự án, import `database/schema.sql` vào MySQL.
- Tạo file `.env` tại thư mục gốc dự án và điền thông tin MySQL của máy bạn:

```text
MOVIE_DB_URL=jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm
MOVIE_DB_USER=root
MOVIE_DB_PASSWORD=mat_khau_mysql_cua_ban
```

`.env` đã được Git bỏ qua. Nếu terminal có biến môi trường `MOVIE_DB_*`, các
biến đó được ưu tiên hơn giá trị trong `.env`.

## Biên dịch và chạy trên Windows PowerShell

Tại thư mục gốc dự án, dừng Server cũ bằng Ctrl+C, đóng Client cũ rồi biên dịch:

```powershell
$javaFiles = (Get-ChildItem -Recurse -File -Filter '*.java' | Where-Object { $_.FullName -notmatch '\\(out|bin|target)\\' }).FullName
New-Item -ItemType Directory -Path out -Force | Out-Null
javac -encoding UTF-8 --release 11 -d out $javaFiles
```

Nếu biên dịch không báo lỗi, chạy Server trong terminal thứ nhất:

```powershell
java -cp 'out;lib/*' server.BasicServer
```

Khi Server báo sẵn sàng ở cổng 2040, mở terminal thứ hai tại cùng thư mục để
chạy Client:

```powershell
java -cp 'out;lib/*' web.BasicClientUI
```

Sau mỗi lần sửa file `.java`, biên dịch lại và khởi động lại chương trình.
Nếu chỉ sửa `.env`, khởi động lại Server là đủ.

## Ảnh phim

Admin chọn ảnh JPG hoặc PNG tối đa 2 MB khi thêm hoặc sửa phim. Client gửi ảnh
qua TCP; Server tạo poster JPEG và lưu vào thư mục `img/` của dự án. MySQL lưu
tên file poster; các Client nhận ảnh từ Server khi tải danh sách phim. Các ảnh
đã lưu bằng bản cũ trong thư mục người dùng vẫn được Server đọc để không làm
mất poster hiện có.

Mở Server từ thư mục gốc dự án để đường dẫn `img/` trỏ đúng chỗ. Nếu đặt biến
môi trường `MOVIE_POSTER_DIR`, Server sẽ lưu ảnh ở thư mục được cấu hình thay vì
`img/`. Các ảnh mẫu có sẵn trong `img/` chỉ hiển thị cho phim sau khi admin
chọn chúng trong giao diện thêm hoặc sửa phim.

## Cấu trúc chính

```text
web.BasicClientUI → web.MovieClient → server.BasicServer
                                   → service.* → repository.* → mysql.CSDL → MySQL
```

`database/schema.sql` chứa cấu trúc và dữ liệu mẫu. `lib/` chứa MySQL
Connector/J cần khi chạy Server. `tests/Step7SmokeTest.java` kiểm tra các luồng
đặt vé và phân quyền với database thử nghiệm.
