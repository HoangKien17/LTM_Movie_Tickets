# Galaxy Cinema

**Mỗi suất chiếu, một trải nghiệm mới.**

Galaxy Cinema là ứng dụng **đặt vé xem phim trên máy tính** cho môn Lập trình mạng. Giao diện được viết bằng Java Swing; Client trao đổi với Server qua TCP Socket, còn Server xử lý nghiệp vụ và truy cập MySQL bằng JDBC. Client không kết nối trực tiếp đến database.

Tên repository `LTM_Movie_Tickets` và database `movie_tickets_ltm` được giữ để tương thích với dự án hiện tại; tên hiển thị trong ứng dụng là **Galaxy Cinema**.

## Chức năng hiện có

**Người dùng**

- Đăng ký, đăng nhập bằng email hoặc số điện thoại và đăng xuất.
- Xem danh sách phim, mô tả, poster và các suất chiếu.
- Xem sơ đồ ghế còn trống hoặc đã được đặt; chọn tối đa 10 ghế trong một lần đặt.
- Đặt vé, xem danh sách/chi tiết đơn của mình và hủy đơn.

**Quản trị viên**

- Thêm, xem, sửa và xóa phim; chọn poster JPG/PNG từ máy khi thêm hoặc sửa phim. Khi sửa phim, trường để trống được giữ nguyên.
- Thêm, xem, sửa và xóa phòng chiếu; cấu hình ghế bằng số hàng × số ghế mỗi hàng.
- Thêm, xem, sửa và xóa suất chiếu.
- Xem người dùng, tất cả đơn vé, hủy đơn và xem thống kê.
- Xem báo cáo theo phim gồm số vé còn hiệu lực và doanh thu; các đơn/vé đã hủy không được tính vào báo cáo.

Ứng dụng hiện ghi nhận đơn đặt vé trong database; **chưa tích hợp thanh toán trực tuyến**.

## Kiến trúc và luồng xử lý

```text
Java Swing Client                     Java TCP Server                       MySQL
web.BasicClientUI ──> web.MovieClient ──TCP :2040──> server.BasicServer
                                                    └──> service.* ──> repository.*
                                                                      └──> mysql.CSDL ──JDBC──> database
```

1. Người dùng thao tác trên giao diện Swing. `MovieClient` gửi một yêu cầu UTF-8 qua TCP.
2. `BasicServer` giữ phiên đăng nhập riêng cho từng kết nối, kiểm tra quyền và chuyển yêu cầu đến lớp `service`.
3. Lớp `service` kiểm tra quy tắc nghiệp vụ; lớp `repository` thực hiện truy vấn MySQL.
4. Server trả kết quả về Client; giao diện cập nhật phim, suất chiếu, ghế hoặc vé.

Giao thức dùng một dòng cho mỗi yêu cầu, các trường được ngăn bằng `;`; dữ liệu văn bản được mã hóa Base64 URL-safe trong `protocol.Protocol`. Phản hồi bắt đầu bằng `OK;...` hoặc `ERR;...` và kết thúc bằng dòng `END`. Server dùng một nhóm luồng để phục vụ nhiều Client. Khi hai người cùng đặt một ghế, giao dịch database và ràng buộc ghế còn hiệu lực chỉ cho phép một yêu cầu thành công.

## Yêu cầu trước khi chạy

- JDK **11 trở lên** (`java -version` và `javac -version`).
- MySQL Server đang chạy và một tài khoản MySQL có quyền tạo database/bảng khi khởi tạo.
- Windows PowerShell nếu dùng các lệnh bên dưới. Repository đã có `lib/mysql-connector-j-9.7.0.jar`, không cần tải thêm JDBC driver.

Các lệnh sau được chạy **tại thư mục gốc dự án** (nơi có `database/`, `web/`, `server/` và `README.md`).

## Cài đặt và khởi động

### 1. Lấy mã nguồn

Nếu chưa có dự án trên máy:

```powershell
git clone https://github.com/HoangKien17/LTM_Movie_Tickets.git
cd LTM_Movie_Tickets
```

Nếu đã clone trước đó, cập nhật mã bằng `git pull` trong thư mục dự án.

### 2. Tạo database

Mở MySQL Workbench, kết nối MySQL, mở file `database/schema.sql` và chạy toàn bộ script. Script tạo database `movie_tickets_ltm`, các bảng và dữ liệu mẫu gồm phim, phòng, ghế và một suất chiếu vào ngày hôm sau.

Hoặc, nếu lệnh `mysql` đã có trong `PATH`, chạy tại PowerShell ở thư mục gốc:

```powershell
mysql -u root -p -e "source database/schema.sql"
```

MySQL sẽ hỏi mật khẩu ở Terminal. Cú pháp `source` được [MySQL hỗ trợ khi chạy script ở chế độ batch](https://dev.mysql.com/doc/refman/9.1/en/batch-mode.html).

> `schema.sql` dùng `CREATE TABLE IF NOT EXISTS`: chạy lại script **không tự nâng cấp cấu trúc bảng cũ**. Nếu bạn đang dùng database của phiên bản dự án trước, hãy sao lưu dữ liệu và kiểm tra schema trước khi cập nhật.

### 3. Cấu hình kết nối MySQL cho Server

Tạo file `.env` tại thư mục gốc dự án:

```dotenv
MOVIE_DB_URL=jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm
MOVIE_DB_USER=root
MOVIE_DB_PASSWORD=mat_khau_mysql_cua_ban
```

Thay tên đăng nhập và mật khẩu bằng thông tin MySQL trên máy chạy Server. `.env` đã được đưa vào `.gitignore`, vì vậy không commit file chứa mật khẩu. Các biến môi trường `MOVIE_DB_URL`, `MOVIE_DB_USER`, `MOVIE_DB_PASSWORD` trong phiên chạy Server được ưu tiên hơn giá trị trong `.env`. Nếu sửa `.env`, hãy khởi động lại Server.

### 4. Biên dịch

```powershell
$javaFiles = (Get-ChildItem -Recurse -File -Filter '*.java' | Where-Object { $_.FullName -notmatch '\\(out|bin|target)\\' }).FullName
New-Item -ItemType Directory -Path out -Force | Out-Null
javac -encoding UTF-8 --release 11 -d out $javaFiles
```

Chỉ tiếp tục khi lệnh `javac` không báo lỗi. Sau mỗi lần sửa file `.java`, hãy biên dịch lại và khởi động lại chương trình.

### 5. Chạy Server và Client

Trong **Terminal thứ nhất**, tại thư mục gốc dự án:

```powershell
java -cp 'out;lib/*' server.BasicServer
```

Khi thấy `Galaxy Cinema Server đã sẵn sàng tại cổng 2040`, mở **Terminal thứ hai** cũng tại thư mục gốc và chạy:

```powershell
java -cp 'out;lib/*' web.BasicClientUI
```

Giữ Terminal Server mở trong lúc dùng ứng dụng. Dừng Server bằng `Ctrl+C`. Nếu bạn chỉ thay ảnh tĩnh trong `assets/`, hãy đóng rồi mở lại Client; không cần biên dịch Java.

### 6. Tạo tài khoản quản trị

Người đăng ký qua giao diện mặc định có quyền `user`. Để cấp quyền quản trị cho một tài khoản của nhóm, đăng ký tài khoản đó trước, rồi chạy SQL sau trong MySQL Workbench (thay email thật của tài khoản):

```sql
USE movie_tickets_ltm;
UPDATE nguoi_dung
SET vai_tro = 'admin'
WHERE gmail = 'email-cua-ban@example.com';
```

Đăng xuất rồi đăng nhập lại để Server nhận vai trò `admin` trong phiên mới. Các thao tác quản trị đều được Server kiểm tra quyền.

## Chạy Client trên máy khác trong mạng LAN

Chạy MySQL và Server trên máy chủ. Trên máy Client, mở PowerShell tại thư mục dự án và đặt địa chỉ IP của máy chạy Server trước khi khởi động giao diện:

```powershell
$env:MOVIE_SERVER_HOST = '192.168.1.10'
$env:MOVIE_SERVER_PORT = '2040'
java -cp 'out;lib/*' web.BasicClientUI
```

Đổi `192.168.1.10` thành IP thật của máy Server và bảo đảm cổng TCP `2040` cho phép kết nối trong mạng. Mặc định Client kết nối `127.0.0.1:2040`. Chỉ Server cần kết nối MySQL; Client vẫn truy cập dữ liệu thông qua Server.

## Kịch bản trình diễn nhanh

1. Khởi động Server và mở hai Client; đăng nhập bằng hai tài khoản khác nhau.
2. Chọn cùng một suất chiếu và cùng một ghế ở hai Client, sau đó gửi yêu cầu đặt gần như đồng thời. Chỉ một Client đặt thành công.
3. Tài khoản đặt thành công mở **Vé của tôi**, xem chi tiết rồi hủy đơn. Tải lại sơ đồ ghế để thấy ghế trống trở lại.
4. Đăng nhập tài khoản admin để xem danh sách đơn, thống kê và báo cáo doanh thu theo phim.

## Ảnh giao diện và poster phim

- `assets/galaxy-cinema-auth.png` là hình cuộn phim tĩnh trên màn hình đăng nhập/đăng ký. Tên file phải khớp chính xác; Client đọc ảnh theo đường dẫn tương đối từ thư mục chạy chương trình.
- `img/` là nơi Server lưu poster do admin tải lên. Admin chọn ảnh JPG/PNG tối đa **2 MB**; Server tạo ảnh JPEG kích thước **244 × 340**, lưu tên file trong MySQL rồi gửi nội dung ảnh cho Client qua TCP.
- Nếu đặt biến môi trường `MOVIE_POSTER_DIR` cho Server, poster mới sẽ được lưu ở thư mục đó thay cho `img/`. Ảnh từ đường dẫn cũ trong thư mục người dùng vẫn được hỗ trợ đọc.
- Ảnh vừa tải lên `img/` nằm trên **máy chạy Server**. Muốn đưa ảnh đó lên GitHub để chia sẻ cho nhóm, cần `git add`, commit và push; lưu vào thư mục không tự đồng bộ lên GitHub.

## Kiểm thử luồng đặt vé

`tests.Step7SmokeTest` chạy qua **TCP Server thật** và kiểm tra đăng nhập, phân quyền, đọc phim/suất/ghế, hai Client cùng đặt một ghế, xem/hủy đơn và đăng xuất. Trước khi chạy, cần:

- Server đang chạy và kết nối được MySQL.
- Hai tài khoản **khác nhau**, cùng có vai trò `user`.
- Ít nhất một suất chiếu trong tương lai còn ghế trống.

Trong Terminal PowerShell mới, đặt bốn biến môi trường cho hai tài khoản rồi chạy:

```powershell
$env:MOVIE_TEST_USER_1 = 'email-nguoi-dung-1@example.com'
$env:MOVIE_TEST_PASSWORD_1 = Read-Host 'Mật khẩu người dùng 1'
$env:MOVIE_TEST_USER_2 = 'email-nguoi-dung-2@example.com'
$env:MOVIE_TEST_PASSWORD_2 = Read-Host 'Mật khẩu người dùng 2'
java -cp 'out;lib/*' tests.Step7SmokeTest
```

Test **tạo một đơn thật rồi hủy đơn đó**, nên chạy với database thử nghiệm. Bản ghi đơn đã hủy vẫn là lịch sử trong database.

## Cấu trúc thư mục

```text
assets/       Ảnh tĩnh dùng cho giao diện Galaxy Cinema
database/     Script tạo database và dữ liệu mẫu
img/          Poster phim trên máy Server và các ảnh mẫu
lib/          MySQL Connector/J
model/        Các đối tượng dữ liệu
mysql/        Cấu hình kết nối JDBC của Server
protocol/     Mã hóa tham số của giao thức TCP
repository/   Truy vấn và giao dịch MySQL
server/       TCP Server và xử lý lệnh
service/      Quy tắc nghiệp vụ, xử lý poster
tests/        Kiểm thử qua TCP Server
utils/        Hàm hỗ trợ, gồm xử lý mật khẩu
web/          Giao diện Java Swing và TCP Client (không phải website)
.env          Cấu hình MySQL riêng trên máy, không đưa lên Git
out/          File .class sinh ra khi biên dịch, không đưa lên Git
```

## Khi gặp lỗi

- **`Access denied for user ...`**: kiểm tra tài khoản/mật khẩu trong `.env`, biến môi trường `MOVIE_DB_*` và quyền của tài khoản MySQL. Chạy Server từ thư mục gốc rồi khởi động lại.
- **Server báo MySQL chưa sẵn sàng**: kiểm tra dịch vụ MySQL, URL/cổng database và việc đã chạy `database/schema.sql`.
- **`Address already in use` hoặc không mở được cổng 2040**: dừng phiên Server cũ bằng `Ctrl+C` trước khi chạy lại.
- **Client không kết nối được Server**: chạy Server trước; kiểm tra `MOVIE_SERVER_HOST`, `MOVIE_SERVER_PORT` và tường lửa nếu kết nối qua LAN.
- **Không thấy hình cuộn phim khi đăng nhập**: kiểm tra đúng file `assets/galaxy-cinema-auth.png`, chạy Client từ thư mục gốc và mở lại Client.
- **`ClassNotFoundException` hoặc vẫn thấy giao diện cũ**: biên dịch lại toàn bộ mã vào `out/`, đóng Client cũ rồi chạy lại bằng lệnh ở trên.

