# Galaxy Cinema

**Mỗi suất chiếu, một trải nghiệm mới.**

Galaxy Cinema là ứng dụng **đặt vé xem phim trên máy tính** phục vụ môn học Lập trình mạng (LTM). Ứng dụng được thiết kế theo kiến trúc Client - Server đa tầng với giao diện đồ họa **Java Swing Cyberpunk / Neon Dark Mode** hiện đại. 

Toàn bộ quá trình trao đổi dữ liệu giữa Client và Server diễn ra thông qua **TCP Socket tùy biến** (giao thức dòng lệnh mã hóa Base64 URL-safe). Server đóng vai trò trung tâm xử lý nghiệp vụ, giao dịch thanh toán, quản lý kết nối đồng thời và truy vấn cơ sở dữ liệu **MySQL bằng JDBC**. Client hoàn toàn không kết nối trực tiếp đến database để đảm bảo tính an toàn dữ liệu. Ngoài ra, Server còn tích hợp một **HTTP Web Server** phục vụ khách hàng quét mã QR xem vé điện tử trực tiếp trên trình duyệt điện thoại di động.

---

## Mục lục

- [Tính năng hệ thống](#tính-năng-hệ-thống)
- [Kiến trúc và Luồng xử lý](#kiến-trúc-và-luồng-xử-lý)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Hướng dẫn cài đặt và khởi chạy](#hướng-dẫn-cài-đặt-và-khởi-chạy)
  - [1. Khởi tạo Cơ sở dữ liệu](#1-khởi-tạo-cơ-sở-dữ-liệu)
  - [2. Cấu hình file `.env`](#2-cấu-hình-file-env)
  - [3. Biên dịch mã nguồn](#3-biên-dịch-mã-nguồn)
  - [4. Khởi động Server](#4-khởi-động-server)
  - [5. Khởi động Client](#5-khởi-động-client)
  - [6. Cấp quyền Quản trị viên (Admin) và Nhân viên (Staff)](#6-cấp-quyền-quản-trị-viên-admin-và-nhân-viên-staff)
- [Kết nối Client trong mạng LAN](#kết-nối-client-trong-mạng-lan)
- [Thanh toán trực tuyến (Demo & PayOS)](#thanh-toán-trực-tuyến-demo--payos)
- [Quét QR xem vé trên Điện thoại di động](#quét-qr-xem-vé-trên-điện-thoại-di-động)
- [Khôi phục mật khẩu qua Email (Gmail SMTP)](#khôi-phục-mật-khẩu-qua-email-gmail-smtp)
- [Cấu trúc thư mục dự án](#cấu-trúc-thư-mục-dự-án)
- [Kiểm thử tự động (Testing)](#kiểm-thử-tự-động-testing)
- [Xử lý sự cố thường gặp (Troubleshooting)](#xử-lý-sự-cố-thường-gặp-troubleshooting)

---

## Tính năng hệ thống

### 1. Dành cho Khách hàng (User)
- **Tài khoản & Hồ sơ**:
  - Đăng ký tài khoản, đăng nhập bằng Email hoặc Số điện thoại.
  - Quên mật khẩu: Nhận mã xác thực OTP 6 số gửi tự động về Gmail cá nhân qua giao thức SMTP an toàn.
  - Xem và cập nhật thông tin cá nhân (Họ tên, SĐT, Email), đổi mật khẩu.
- **Trải nghiệm phim & Đặt vé**:
  - Xem danh sách phim đang chiếu, poster, thể loại, thời lượng, mô tả và lịch chiếu theo ngày.
  - Sơ đồ ghế trực quan: Hiển thị đúng mã hàng (A, B, C...) và số cột, hiển thị lối đi giữa và màn chiếu cong.
  - Chọn tối đa 10 ghế trong một lượt đặt.
  - **Giữ ghế thời gian thực**: Ghế được giữ tạm thời (tối đa 10 phút) ngay khi bấm tạo đơn thanh toán. Đồng bộ tức thời tới các máy khác xem cùng suất chiếu để tránh đặt trùng ghế.
  - **Combo Bắp & Nước**: Chọn đồ ăn, nước uống với bảng tính tiền tự động ngay tại màn hình đặt vé.
- **Thanh toán & Vé điện tử**:
  - Hỗ trợ thanh toán Demo (giả lập) hoặc PayOS (quét mã VietQR chuyển khoản thật).
  - Xuất vé điện tử dạng cuộn thẻ perforated neon đẹp mắt ngay sau khi thanh toán thành công.
  - Lưu ảnh vé dưới dạng PNG hoặc in vé.
  - **Xem vé trên điện thoại**: Quét mã QR trên vé bằng Zalo/Camera điện thoại để mở giao diện web vé điện tử trực tiếp trên di động.
- **Tương tác & Tiện ích**:
  - **Phòng chat trực tiếp (Showtime Live Chat)**: Nhắn tin trao đổi trong thời gian thực giữa các khán giả xem cùng một suất chiếu.
  - **Trung tâm thông báo (Notification Center)**: Nhận thông báo hệ thống và cập nhật trạng thái đơn vé.

### 2. Dành cho Nhân viên soát vé (Staff)
- Giao diện **Soát vé** chuyên dụng:
  - Nhập mã đặt vé (#ID) hoặc tải ảnh mã QR vé (PNG, JPG) từ máy tính.
  - Kiểm tra trạng thái vé: Hợp lệ, đã dùng hay đã bị hủy.
  - Xác nhận check-in cho khách vào phòng chiếu. Mỗi mã vé chỉ được soát thành công đúng 1 lần.
  - Xử lý đồng thời: Ngăn chặn triệt để tình trạng hai nhân viên soát cùng một vé tại hai cửa rạp khác nhau.

### 3. Dành cho Quản trị viên (Admin)
- Đầy đủ quyền hạn của Nhân viên và Khách hàng.
- **Quản lý danh mục**:
  - Quản lý Phim: Thêm, sửa thông tin, xóa phim, tải ảnh poster JPG/PNG lên Server (tự động resize và lưu trữ).
  - Quản lý Phòng chiếu & Ghế: Tùy biến số hàng × số cột cho từng phòng chiếu.
  - Quản lý Suất chiếu: Thêm lịch chiếu mới, điều chỉnh giá vé, xóa suất chiếu.
  - Quản lý Bắp nước: Thêm sản phẩm, sửa giá, ẩn/hiện đồ ăn nước uống.
- **Quản lý & Thống kê**:
  - Xem danh sách người dùng, phân quyền tài khoản.
  - Xem toàn bộ danh sách đơn đặt vé, hủy đơn theo yêu cầu.
  - Báo cáo doanh thu và số lượng vé bán theo từng phim (loại trừ các vé hủy/quá hạn).
- **Giám sát Server thời gian thực (Server Monitor)**:
  - Xem số lượng Client đang kết nối trực tiếp qua TCP.
  - Giám sát thông lượng (Throughput), tổng số request, thời gian xử lý trung bình và tỷ lệ lỗi.
  - Bảng nhật ký (Logs) hoạt động trực tiếp của Server, tự động cập nhật mỗi 2 giây.

---

## Kiến trúc và Luồng xử lý

```text
┌─────────────────────────────────┐                    ┌─────────────────────────────────┐
│     Client (Java Swing UI)      │                    │     Thiết bị di động (Mobile)   │
│  - BasicClientUI / AuthScreen   │                    │  - Quét QR bằng Zalo / Camera   │
└───────────────┬─────────────────┘                    └────────────────┬────────────────┘
                │ TCP Socket (Port 2040)                                │ HTTP GET /ticket?token=...
                ▼                                                       ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               Server (server.BasicServer)                              │
│                                                                                        │
│  ┌─────────────────────────────┐                    ┌───────────────────────────────┐  │
│  │    TCP Request Dispatcher   │                    │     TicketHttpServer (8080)   │  │
│  │ (ThreadPool, Session, Auth) │                    │   (Phục vụ trang web xem vé)  │  │
│  └──────────────┬──────────────┘                    └───────────────┬───────────────┘  │
│                 │                                                   │                  │
│                 ▼                                                   ▼                  │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                 Service Layer                                    │  │
│  │  - BasicService      - CinemaService     - PaymentService     - TicketService    │  │
│  │  - ChatService       - NotificationService - SmtpResetMailer  - PosterStorage    │  │
│  └──────────────────────────────────────────┬───────────────────────────────────────┘  │
│                                             │                                          │
│                                             ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                         Repository Layer & JDBC (CSDL)                           │  │
│  │  - NguoiDungRepo     - PhimRepo          - CinemaRepo         - OrderRepo        │  │
│  │  - TicketRepo        - ChatRepo          - NotificationRepo                      │  │
│  └──────────────────────────────────────────┬───────────────────────────────────────┘  │
└─────────────────────────────────────────────┼──────────────────────────────────────────┘
                                              │ JDBC
                                              ▼
                               ┌─────────────────────────────┐
                               │     MySQL Server (3306)     │
                               │  Database: movie_tickets_ltm│
                               └─────────────────────────────┘
```

1. **Giao thức mạng TCP**:
   - Mỗi thông điệp gửi đi có định dạng: `COMMAND;PARAM_1;PARAM_2;...` kết thúc bằng ký tự xuống dòng `\n`.
   - Các tham số chuỗi đều được mã hóa bằng **Base64 URL-safe** (`protocol.Protocol`) để không bị lỗi ký tự tiếng Việt, dấu phân cách hay ký tự xuống dòng.
   - Phản hồi từ Server bắt đầu bằng dòng `OK;...` hoặc `ERR;...`, tiếp theo là các dòng dữ liệu và kết thúc bằng dòng `END`.
   - Khi có thay đổi trạng thái ghế (`SEAT_CHANGED;<showId>`) hoặc tin nhắn mới (`CHAT_MESSAGE;<showId>;...`), Server chủ động phát sự kiện bất đồng bộ (Push Notification) tới các socket Client đang theo dõi.
2. **Concurrency & Giữ ghế an toàn**:
   - Sử dụng Transaction với ràng buộc `UNIQUE KEY` trên database, kết hợp trạng thái `held` có gắn thời hạn `expires_at`. Khi 2 Client cùng chọn 1 ghế tại một thời điểm, chỉ đúng 1 giao dịch thành công. Luồng ngầm định kỳ quét và giải phóng các ghế quá hạn thanh toán.
3. **HTTP Web Server xem vé**:
   - Khởi tạo qua `com.sun.net.httpserver.HttpServer` chạy độc lập trên cổng HTTP (mặc định 8080).
   - Tự động sinh link vé có chứa token bảo mật 64 ký tự hex. Khi quét bằng điện thoại, máy chủ trả về trang HTML/CSS responsive hiển thị thông tin rạp, phim, suất chiếu, ghế và mã QR.

---

## Yêu cầu môi trường

- **Java Development Kit (JDK)**: Phiên bản **11** trở lên (Khuyến nghị JDK 17 hoặc JDK 21).
- **Cơ sở dữ liệu**: **MySQL Server 8.0** trở lên.
- **Hệ điều hành**: Windows 10/11 (khuyên dùng PowerShell) hoặc Linux / macOS.
- **Thư viện bên thứ ba** (đã tích hợp sẵn trong thư mục `lib/`):
  - `mysql-connector-j-9.7.0.jar`: Driver kết nối MySQL JDBC.
  - `zxing-core-3.5.4.jar`: Thư viện sinh và giải mã ảnh mã vạch QR Code.
  - `gson-2.13.2.jar`: Thư viện xử lý dữ liệu JSON (dùng cho tích hợp PayOS).

---

## Hướng dẫn cài đặt và khởi chạy

### 1. Khởi tạo Cơ sở dữ liệu

Mở MySQL Workbench (hoặc terminal MySQL) và thực thi file script tạo database:

```sql
-- Chạy toàn bộ file database/schema.sql
```

Hoặc chạy nhanh bằng lệnh qua PowerShell/Command Prompt:

```powershell
mysql -u root -p -e "source database/schema.sql"
```

---

### 2. Cấu hình file `.env`

Tạo một file `.env` tại **thư mục gốc** của dự án (bạn có thể sao chép từ file mẫu `env.example`):

```dotenv
# Cấu hình kết nối MySQL
MOVIE_DB_URL=jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm
MOVIE_DB_USER=root
MOVIE_DB_PASSWORD=mat_khau_mysql_cua_ban

# Cổng dịch vụ
MOVIE_SERVER_PORT=2040
MOVIE_HTTP_PORT=8080

# Chế độ thanh toán: "demo" (giả lập) hoặc "payos" (chuyển khoản thật)
PAYMENT_MODE=demo

# Cấu hình xem vé từ xa bằng 4G/5G qua Ngrok / Cloudflare Tunnel (tùy chọn)
# Nếu để trống, Server sẽ tự động lấy địa chỉ IP LAN Wi-Fi nội bộ
PUBLIC_TICKET_URL=

# Cấu hình gửi Email OTP khôi phục mật khẩu (Gmail SMTP) (tùy chọn)
MOVIE_SMTP_HOST=smtp.gmail.com
MOVIE_SMTP_USER=email-cua-ban@gmail.com
MOVIE_SMTP_APP_PASSWORD=mat_khau_ung_dung_16_ky_tu
```

---

### 3. Biên dịch mã nguồn

Mở PowerShell tại thư mục gốc của dự án và chạy các lệnh sau:

```powershell
# Tạo thư mục chứa file class biên dịch
New-Item -ItemType Directory -Path out -Force | Out-Null

# Quét tất cả file mã nguồn .java
$javaFiles = (Get-ChildItem -Recurse -File -Filter '*.java' | Where-Object { $_.FullName -notmatch '\\(out|bin|target)\\' }).FullName

# Biên dịch với thư viện trong lib/
javac -encoding UTF-8 --release 11 -cp "lib/*;." -d out $javaFiles
```

---

### 4. Khởi động Server

Chạy lệnh sau trên **Terminal thứ nhất**:

```powershell
java -cp "out;lib/*" server.BasicServer
```

Khi Server khởi động thành công, bạn sẽ thấy thông báo:
```text
[HTTP] Web xem vé đã khởi động tại cổng 8080
[TCP] Galaxy Cinema Server đã sẵn sàng tại cổng 2040
```

---

### 5. Khởi động Client

Mở **Terminal thứ hai** tại thư mục dự án và chạy:

```powershell
java -cp "out;lib/*" web.BasicClientUI
```

Giao diện đăng nhập sẽ xuất hiện. Bạn có thể mở đồng thời nhiều Client để trải nghiệm tính năng đồng bộ ghế và chat trực tiếp.

---

### 6. Cấp quyền Quản trị viên (Admin) và Nhân viên (Staff)

Mọi tài khoản mới đăng ký qua màn hình Client đều có quyền mặc định là `user`. Để cấp quyền quản trị viên hoặc nhân viên soát vé, chạy câu lệnh sau trong MySQL:

```sql
USE movie_tickets_ltm;

-- Cấp quyền Quản trị viên (Admin)
UPDATE nguoi_dung SET vai_tro = 'admin' WHERE gmail = 'email-admin@example.com';

-- Cấp quyền Nhân viên soát vé (Staff)
UPDATE nguoi_dung SET vai_tro = 'staff' WHERE gmail = 'email-staff@example.com';
```

Sau khi chạy lệnh, hãy **đăng xuất và đăng nhập lại** trên Client để nhận giao diện tương ứng:
- Tài khoản `staff`: Sẽ xuất hiện nút **Soát vé**.
- Tài khoản `admin`: Sẽ xuất hiện các nút **Soát vé**, **Quản trị**, **Giám sát server**.

---

## Kết nối Client trong mạng LAN

Để chạy Client trên máy tính khác trong cùng mạng Wi-Fi/LAN với máy chủ Server:

1. Trên máy chạy Server: Tra cứu địa chỉ IP mạng nội bộ (ví dụ: `192.168.1.15`). Đảm bảo tường lửa (Windows Firewall) đã cho phép cổng TCP `2040` và cổng HTTP `8080`.
2. Trên máy Client: Mở PowerShell tại thư mục dự án và cấu hình biến môi trường trước khi chạy:

```powershell
$env:MOVIE_SERVER_HOST = '192.168.1.15'
$env:MOVIE_SERVER_PORT = '2040'
java -cp "out;lib/*" web.BasicClientUI
```

---

## Thanh toán trực tuyến (Demo & PayOS)

### 1. Chế độ Demo (`PAYMENT_MODE=demo`)
- Dành cho môi trường phát triển/thực hành môn học mà không cần tài khoản ngân hàng.
- Màn hình thanh toán sẽ xuất hiện đồng hồ đếm ngược 10 phút, mã QR mô phỏng và nút **"Giả lập đã thanh toán"**.
- Khi bấm nút, Client gửi yêu cầu `DEMO_CONFIRM_PAYMENT` tới Server để xác nhận giao dịch thành công.

### 2. Chế độ PayOS (`PAYMENT_MODE=payos`)
- Nhận tiền thanh toán thật chuyển về tài khoản ngân hàng của bạn thông qua cổng thanh toán **payOS**.
- Đăng ký tài khoản tại [payOS](https://payos.vn), tạo kênh thanh toán và cập nhật vào file `.env`:

```dotenv
PAYMENT_MODE=payos
PAYOS_CLIENT_ID=client_id_cua_ban
PAYOS_API_KEY=api_key_cua_ban
PAYOS_CHECKSUM_KEY=checksum_key_cua_ban
PAYOS_RETURN_URL=https://payos.vn
PAYOS_CANCEL_URL=https://payos.vn
```

- Khách hàng quét mã VietQR tự động sinh bằng bất kỳ app Mobile Banking nào (MBBank, Vietcombank, Techcombank, MoMo...). Server sẽ tự động kiểm tra trạng thái thanh toán và phát hành vé ngay lập tức.

---

## Quét QR xem vé trên Điện thoại di động

Vé điện tử có chứa một mã QR Code thông minh. Khách hàng có thể dùng ứng dụng **Camera điện thoại** hoặc **Zalo** để quét mã QR:

1. **Qua mạng Wi-Fi nội bộ**:
   - Nếu máy điện thoại kết nối cùng Wi-Fi với máy tính chạy Server, Server sẽ tự động sinh mã QR chứa link nội bộ dạng: `http://192.168.x.x:8080/ticket?token=...`
2. **Qua mạng 4G/5G từ xa (Sử dụng Ngrok)**:
   - Cài đặt [Ngrok](https://ngrok.com) và mở cổng 8080: `ngrok http 8080`
   - Sao chép đường dẫn công khai (ví dụ: `https://abc-xyz.ngrok-free.app`) và cấu hình vào `.env`:
     ```dotenv
     PUBLIC_TICKET_URL=https://abc-xyz.ngrok-free.app
     ```
   - Khởi động lại Server. Mã QR của mọi vé xuất ra sau đó sẽ tự động trỏ tới link công khai này, cho phép bất kỳ điện thoại nào bật 4G/5G đều xem được vé trực tuyến.

---

## Khôi phục mật khẩu qua Email (Gmail SMTP)

Ứng dụng hỗ trợ tính năng gửi mã xác thực OTP khôi phục mật khẩu qua giao thức Gmail SMTP:

1. Đăng nhập tài khoản Google của bạn, bật **Xác minh 2 bước** và tạo một **Mật khẩu ứng dụng (App Password)** gồm 16 ký tự.
2. Thêm vào file `.env` trên máy Server:
   ```dotenv
   MOVIE_SMTP_HOST=smtp.gmail.com
   MOVIE_SMTP_USER=email-cua-ban@gmail.com
   MOVIE_SMTP_APP_PASSWORD=xxxx xxxx xxxx xxxx
   ```
3. Tại màn hình Đăng nhập của Client, bấm **Quên mật khẩu?**, nhập email đã đăng ký và nhấn nhận mã OTP. Server sẽ gửi email chứa mã OTP gồm 6 chữ số có hiệu lực trong 10 phút.

---

## Cấu trúc thư mục dự án

```text
LTM-Movie-Tickets/
├── assets/                  # Ảnh nền giao diện và tài nguyên đồ họa tĩnh
├── database/                # Mã nguồn cơ sở dữ liệu (schema.sql)
├── img/                     # Thư mục lưu trữ poster phim (do Server quản lý)
├── lib/                     # Thư viện ngoài: MySQL Driver, ZXing QR, Gson
├── config/                  # Quản lý đọc cấu hình hệ thống từ .env và biến môi trường
│   └── AppConfig.java
├── model/                   # Lớp đối tượng thực thể (NguoiDung, Phim)
├── mysql/                   # Kết nối JDBC cơ sở dữ liệu
│   └── CSDL.java
├── payment/                 # Tích hợp cổng thanh toán (PaymentProvider, PayOS, Demo)
├── protocol/                # Giao thức TCP, mã hóa Base64 và định dạng mã vé QR
├── repository/              # Tầng DAO / SQL query trực tiếp MySQL
├── server/                  # Tầng máy chủ: TCP Socket Server, HTTP Ticket Server, Monitor
├── service/                 # Tầng nghiệp vụ: Đặt vé, Giữ ghế, Mailer, Chat, Quản trị
├── utils/                   # Tiện ích băm mật khẩu (BCrypt), xử lý sinh ảnh QR Code
├── web/                     # Giao diện người dùng Java Swing (Client)
├── tests/                   # Kịch bản kiểm thử tự động (Unit, UI, Realtime, Smoke tests)
├── .env                     # File cấu hình biến môi trường cục bộ (không commit)
├── env.example              # File mẫu cấu hình .env
├── README.md                # Tài liệu hướng dẫn sử dụng dự án
└── out/                     # Thư mục chứa bytecode Java sau khi biên dịch
```

---

## Kiểm thử tự động (Testing)

Dự án cung cấp bộ kịch bản kiểm thử đa dạng trong thư mục `tests/`:

### 1. Kiểm thử Logic & Tiện ích (Không cần kết nối MySQL)
```powershell
# Kiểm thử chữ ký số bảo mật của PayOS HMAC-SHA256
java -cp "out;lib/*" tests.PayOsSignatureTest

# Kiểm thử giao vận thời gian thực qua TCP Socket giả lập
java -cp "out;lib/*" tests.RealtimeTransportTest
```

### 2. Kiểm thử Giao diện & Hiển thị (Headless UI)
```powershell
# Kiểm thử giao diện đồng bộ ghế thời gian thực giữa các client
java -cp "out;lib/*" tests.RealtimeUiTest

# Kiểm thử luồng giao diện chat phòng chiếu
java -cp "out;lib/*" tests.ChatUiTest

# Kiểm thử chụp ảnh màn hình giao diện Neon (kết quả lưu vào ../neon-ui-qa/)
java -cp "out;lib/*" web.NeonUiTest
```

### 3. Kiểm thử Tích hợp toàn diện (Yêu cầu MySQL thử nghiệm riêng)
Các bài test tích hợp sẽ tự động tạo dữ liệu cô lập để kiểm tra luồng mua vé và thanh toán:
```powershell
$env:MOVIE_TEST_ISOLATED = "1"

# Kiểm thử luồng đặt bắp nước, giữ ghế, thanh toán demo và xuất vé
java -cp "out;lib/*" tests.PaymentFoodSmokeTest

# Kiểm thử luồng phân quyền tài khoản, đổi thông tin, soát vé và hủy đơn
java -cp "out;lib/*" tests.FeatureSmokeTest

# Kiểm thử tính năng gửi/nhận tin nhắn phòng chat qua TCP Server thật
java -cp "out;lib/*" tests.ChatFeatureTest
```

---

## Xử lý sự cố thường gặp (Troubleshooting)

1. **Lỗi `Access denied for user 'root'@'localhost'`**:
   - Kiểm tra lại mật khẩu MySQL trong file `.env` tại dòng `MOVIE_DB_PASSWORD`.
   - Đảm bảo MySQL Server đang chạy (`net start MySQL80` hoặc kiểm tra trên Services của Windows).
2. **Lỗi `Address already in use: bind` (Cổng 2040 hoặc 8080 bị chiếm dụng)**:
   - Phiên bản Server cũ vẫn đang chạy ngầm. Mở Task Manager tìm tắt tiến trình `java.exe`, hoặc đổi sang cổng khác trong `.env` (`MOVIE_SERVER_PORT=2041`).
3. **Client không thể kết nối tới Server (`Connection refused`)**:
   - Hãy chắc chắn rằng bạn đã khởi động Server trước khi mở Client.
   - Nếu kết nối qua LAN, kiểm tra tường lửa của máy chủ Server xem đã mở cổng 2040 chưa.
4. **Không thấy ảnh nền hiển thị khi mở ứng dụng**:
   - Hãy chắc chắn bạn chạy lệnh Java từ **thư mục gốc của dự án**, nơi có thư mục `assets/`.
5. **Gặp lỗi `ClassNotFoundException`**:
   - Hãy thực hiện biên dịch lại toàn bộ dự án vào thư mục `out/` theo hướng dẫn tại Mục 3 trước khi khởi chạy.
