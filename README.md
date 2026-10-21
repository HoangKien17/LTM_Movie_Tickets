# Galaxy Cinema

**Mỗi suất chiếu, một trải nghiệm mới.**

Galaxy Cinema là ứng dụng **đặt vé xem phim trên máy tính** cho môn Lập trình mạng. Giao diện được viết bằng Java Swing; Client trao đổi với Server qua TCP Socket, còn Server xử lý nghiệp vụ và truy cập MySQL bằng JDBC. Client không kết nối trực tiếp đến database.

Tên repository `LTM_Movie_Tickets` và database `movie_tickets_ltm` được giữ để tương thích với dự án hiện tại; tên hiển thị trong ứng dụng là **Galaxy Cinema**.

## Chức năng hiện có

**Người dùng**

- Đăng ký, đăng nhập bằng email hoặc số điện thoại và đăng xuất.
- Xem danh sách phim, mô tả, poster và các suất chiếu.
- Xem sơ đồ ghế còn trống hoặc đã được đặt; chọn tối đa 10 ghế trong một lần đặt.
- Chọn ghế, mua thêm đồ ăn/nước uống và thanh toán bằng QR đúng tổng tiền vé + sản phẩm.
- Ghế được giữ tối đa 10 phút trong lúc thanh toán; chỉ phát hành vé sau khi giao dịch thành công.
- Đặt vé, xem danh sách/chi tiết đơn của mình và hủy đơn demo. Đơn đã thanh toán thật cần hoàn tiền trước khi hủy.
- Xem vé QR ngay sau khi đặt hoặc từ **Vé của tôi → Vé QR**; lưu QR thành ảnh PNG.
- Sơ đồ ghế hiển thị đúng hàng/số ghế, có màn chiếu và khoảng lối đi giữa.
- Ghế tự cập nhật khi client khác đặt/hủy, giữ các lựa chọn vẫn còn trống.
- Mở menu tài khoản ở biểu tượng cạnh lời chào để xem/cập nhật họ tên, email, số điện thoại và mật khẩu.

**Nhân viên soát vé (staff) và quản trị viên**

- Nhập mã vé hoặc đọc ảnh QR PNG/JPG, xem thông tin và xác nhận khách vào rạp.
- Một QR áp dụng cho toàn bộ ghế trong một đơn. Vé hủy/đã dùng bị từ chối.
- Hai máy soát cùng mã chỉ có một máy thành công. Đơn đã soát không được hủy.

**Quản trị viên**

- Thêm, xem, sửa và xóa phim; chọn poster JPG/PNG từ máy khi thêm hoặc sửa phim. Khi sửa phim, trường để trống được giữ nguyên.
- Thêm, xem, sửa và xóa phòng chiếu; cấu hình ghế bằng số hàng × số ghế mỗi hàng.
- Thêm, xem, sửa và xóa suất chiếu.
- Xem người dùng, tất cả đơn vé, hủy đơn và xem thống kê.
- Mở **Giám sát server** để xem kết nối, thời điểm kết nối, số yêu cầu, thời gian xử lý và nhật ký.
  Màn hình tự làm mới mỗi 2 giây khi đang mở; chỉ admin có quyền truy cập.
- Xem, thêm, sửa, bật lại hoặc ngừng bán đồ ăn và nước uống. Ngừng bán không xóa lịch sử của đơn cũ.
- Xem báo cáo theo phim gồm số vé còn hiệu lực và doanh thu; các đơn/vé đã hủy không được tính vào báo cáo.

Thanh toán có hai chế độ: `demo` để phát triển không chuyển tiền và `payos` để nhận thanh toán thật vào tài khoản ngân hàng đã liên kết với kênh payOS. Server giữ khóa bí mật và tự kiểm tra giao dịch; Client không được tự xác nhận đã trả tiền.

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
- QR dùng ZXing Core 3.5.4; JSON payOS dùng Gson 2.13.2. Các JAR và giấy phép đã có trong `lib/`.
  Khi biên dịch cần thêm -cp 'lib/*' như lệnh bên dưới.

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

### Nâng cấp database đang dùng

Dừng Server, mở database/migrate_v2.sql trong MySQL Workbench và chạy toàn bộ.
Script bổ sung token QR, thời điểm/người soát vé và quyền staff, giữ nguyên dữ liệu.
Có thể chạy lại migration. Database mới tạo bằng schema.sql đã có cấu trúc này.

Sau đó chạy `database/migrate_v3_payment_food.sql`. Migration V3 thêm sản phẩm, chi tiết đồ ăn,
giao dịch thanh toán và trạng thái giữ ghế. Có thể chạy lại script. Người cài mới chỉ cần chạy
`database/schema.sql` vì schema mới đã chứa cả V2 và V3.

Hướng dẫn đầy đủ, danh sách file và kiểm thử: [HUONG_DAN_V2.md](HUONG_DAN_V2.md).

### 3. Cấu hình kết nối MySQL cho Server

Tạo file `.env` tại thư mục gốc dự án:

```dotenv
MOVIE_DB_URL=jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm
MOVIE_DB_USER=root
MOVIE_DB_PASSWORD=mat_khau_mysql_cua_ban
PAYMENT_MODE=demo
```

Thay tên đăng nhập và mật khẩu bằng thông tin MySQL trên máy chạy Server. `.env` đã được đưa vào `.gitignore`, vì vậy không commit file chứa mật khẩu. Các biến môi trường `MOVIE_DB_URL`, `MOVIE_DB_USER`, `MOVIE_DB_PASSWORD` trong phiên chạy Server được ưu tiên hơn giá trị trong `.env`. Nếu sửa `.env`, hãy khởi động lại Server.

Để nhận tiền thật, tạo và liên kết kênh thanh toán tại payOS rồi đổi phần thanh toán trong `.env`:

```dotenv
PAYMENT_MODE=payos
PAYOS_CLIENT_ID=client_id_cua_kenh_thanh_toan
PAYOS_API_KEY=api_key_cua_kenh_thanh_toan
PAYOS_CHECKSUM_KEY=checksum_key_cua_kenh_thanh_toan
PAYOS_RETURN_URL=https://payos.vn
PAYOS_CANCEL_URL=https://payos.vn
```

Tiền được chuyển vào tài khoản ngân hàng liên kết với kênh payOS; không ghi số tài khoản vào mã nguồn.
Ba khóa trên chỉ đặt ở máy Server và không commit `.env`. Xem [HUONG_DAN_V3.md](HUONG_DAN_V3.md).

### 4. Biên dịch

```powershell
$javaFiles = (Get-ChildItem -Recurse -File -Filter '*.java' | Where-Object { $_.FullName -notmatch '\\(out|bin|target)\\' }).FullName
New-Item -ItemType Directory -Path out -Force | Out-Null
javac -encoding UTF-8 --release 11 -cp 'lib/*' -d out $javaFiles
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
2. Chọn ghế, thêm bắp/nước và tạo thanh toán. Client khác thấy ghế chuyển sang đang giữ ngay lập tức.
3. Ở chế độ demo bấm **Giả lập đã thanh toán**; ở chế độ payOS quét QR bằng ứng dụng ngân hàng. Khi Server xác nhận giao dịch, vé QR xuất hiện và mọi Client được cập nhật.
4. Đăng nhập tài khoản admin để xem danh sách đơn, thống kê và báo cáo doanh thu theo phim.

## Ảnh giao diện và poster phim

- `assets/galaxy-cyber-hero-v1.png` là cảnh nền đăng nhập và ảnh dự phòng khi phim chưa có poster. `AuthBackdrop` dùng thêm các ảnh mẫu `img/conan.jpeg`, `img/doraemon.jpeg`, `img/dragonball.jpeg`; thiếu ảnh vẫn có nền dự phòng. Chạy Client từ thư mục gốc dự án. Banner và thẻ phim dùng poster thật do Server gửi qua TCP.
- `img/` là nơi Server lưu poster do admin tải lên. Admin chọn ảnh JPG/PNG tối đa **2 MB**; Server tạo ảnh JPEG kích thước **244 × 340**, lưu tên file trong MySQL rồi gửi nội dung ảnh cho Client qua TCP.
- Nếu đặt biến môi trường `MOVIE_POSTER_DIR` cho Server, poster mới sẽ được lưu ở thư mục đó thay cho `img/`. Ảnh từ đường dẫn cũ trong thư mục người dùng vẫn được hỗ trợ đọc.
- Ảnh vừa tải lên `img/` nằm trên **máy chạy Server**. Muốn đưa ảnh đó lên GitHub để chia sẻ cho nhóm, cần `git add`, commit và push; lưu vào thư mục không tự đồng bộ lên GitHub.

## Kiểm thử luồng đặt vé

`tests.Step7SmokeTest` chạy qua **TCP Server thật** và kiểm tra đăng nhập, phân quyền, đọc phim/suất/ghế, hai Client cùng giữ một ghế, thanh toán demo, xem/hủy đơn và đăng xuất. Trước khi chạy, cần:

- Server đang chạy và kết nối được MySQL.
- Hai tài khoản **khác nhau**, cùng có vai trò `user`.
- Ít nhất một suất chiếu trong tương lai còn ghế trống.
- Server chạy với `PAYMENT_MODE=demo`.

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
- **Không thấy ảnh nền khi đăng nhập**: kiểm tra `assets/galaxy-cyber-hero-v1.png`, chạy Client từ thư mục gốc và mở lại Client. Ảnh cuộn phim vàng cũ không còn được giao diện mới sử dụng.
- **`ClassNotFoundException` hoặc vẫn thấy giao diện cũ**: biên dịch lại toàn bộ mã vào `out/`, đóng Client cũ rồi chạy lại bằng lệnh ở trên.



## Tài khoản nhân viên và soát vé

Đăng ký một tài khoản qua giao diện, sau đó admin database cấp vai trò:

~~~sql
USE movie_tickets_ltm;
UPDATE nguoi_dung SET vai_tro='staff' WHERE gmail='email-nhan-vien@example.com';
~~~

Đăng xuất và đăng nhập lại. Staff có trang Soát vé; admin có Soát vé và Giám sát server.
Nhân viên không có quyền đọc danh sách người dùng, quản trị phim/phòng hoặc xem giám sát.

## Kiểm thử tính năng mới

~~~powershell
java -cp 'out;lib/*' tests.FeatureUnitTest
java -cp 'out;lib/*' tests.RealtimeTransportTest
java -cp 'out;lib/*' tests.RealtimeUiTest
~~~

tests.FeatureSmokeTest chạy qua server thật, tự tạo tài khoản/phim/phòng/đơn để kiểm tra
QR và hai máy soát đồng thời. Chỉ chạy trên MySQL thử nghiệm riêng và đặt
MOVIE_TEST_ISOLATED=1, MOVIE_DB_URL, MOVIE_DB_USER, MOVIE_DB_PASSWORD,
MOVIE_SERVER_HOST, MOVIE_SERVER_PORT đúng môi trường đó. Xem HUONG_DAN_V2.md.

Server đọc MOVIE_SERVER_PORT, mặc định 2040. Cổng của client phải khớp.
Nhật ký giám sát giữ tối đa 500 dòng trong RAM, xóa khi khởi động lại server;
trạng thái soát vé lưu bền vững trong MySQL.
## Giao diện neon cập nhật

- Đăng nhập/đăng ký có tab chuyển màn hình, nền điện ảnh và panel kính tối. Ghi nhớ chỉ lưu email/số điện thoại, không lưu mật khẩu hay tự đăng nhập.
- Trang chủ có bảng chọn ngày, phòng và giờ chiếu bên phải. Lựa chọn đi theo mã suất chiếu thật từ Server.
- Ghế có viền cyan, ghế đang chọn màu xanh có dấu kiểm; bảng đơn, bắp nước và tổng tiền đặt bên phải. Tối đa 10 ghế mỗi đơn.
- Thanh toán QR có thời gian giữ ghế; vé điện tử có đường xé, mã QR đọc được, tải toàn bộ vé PNG và in vé. QR vé chỉ chứa token xác thực.
- Nút Soát vé, Quản trị và Giám sát server xuất hiện theo quyền của tài khoản.
- Chưa có dữ liệu ghế VIP/điểm đánh giá. Thanh toán thẻ và ví trực tiếp chưa hỗ trợ; giao diện giữ các tùy chọn này ở trạng thái vô hiệu hóa.

Kiểm thử giao diện không cần MySQL (biên dịch trước):

```powershell
java -cp 'out;lib/*' tests.FeatureUnitTest
java -cp 'out;lib/*' web.UiRedesignTest
java -cp 'out;lib/*' tests.RealtimeUiTest
java -cp 'out;lib/*' web.NeonUiTest
```

Hai kiểm thử cuối mở TCP giả lập trên localhost. `web.NeonUiTest` xuất ảnh trong `../neon-ui-qa/` bằng dữ liệu kiểm thử, không đặt vé hay thanh toán thật.