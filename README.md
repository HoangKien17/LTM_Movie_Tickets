# LTM Movie Tickets — giao diện mới sau Bước 7

Ứng dụng đặt vé xem phim cho môn Lập trình mạng, dùng Java Swing + TCP Socket + MySQL.

## Chạy dự án

Xem [RUN_MODERN_UI.md](RUN_MODERN_UI.md) để chạy giao diện mới. Database và TCP Server tiếp tục từ Bước 7, không cần import lại SQL.

## Luồng xử lý

```text
web.BasicClientUI (Swing)
        ↓
web.MovieClient (TCP, UTF-8, kết thúc phản hồi bằng END)
        ↓
server.BasicServer (phiên đăng nhập, phân quyền)
        ↓
service.BasicService / CinemaService (quy tắc nghiệp vụ)
        ↓
repository.* (JDBC)
        ↓
mysql.CSDL → MySQL
```

Client không chứa SQL và không có mật khẩu MySQL. Đặt nhiều ghế được xử lý trong một giao dịch MySQL. Ràng buộc duy nhất trên ghế còn hiệu lực ngăn hai Client đặt trùng ghế.

## Chức năng

- Người dùng: đăng ký, đăng nhập, xem phim/suất chiếu/ghế, đặt nhiều ghế, xem vé, hủy đơn và đăng xuất.
- Quản trị: thêm/sửa/xóa phim, phòng, suất chiếu; xem người dùng, đơn vé, doanh thu; hủy đơn vé.
- Dữ liệu mẫu: một phim, một phòng, 12 ghế và một suất chiếu.

Bản này tiếp tục từ ZIP Bước 6. Giao diện cũ và Server cũ vẫn nằm trong ZIP Bước 5 để tham khảo; gói hiện tại chỉ chứa luồng đang chạy.

Bước 7 thêm `tests.Step7SmokeTest` để kiểm tra giao thức, phân quyền, đặt trùng ghế và hủy vé bằng hai Client. Danh sách kiểm thử thủ công nằm ở [TEST_STEP7.md](TEST_STEP7.md).

Giao diện mới tách màu sắc và thành phần hiển thị vào `web.UiTheme`. Các màn hình đăng nhập, phim, suất chiếu, ghế, vé và quản trị được thiết kế lại trong `web.BasicClientUI`; dữ liệu vẫn đi qua `web.MovieClient`.
