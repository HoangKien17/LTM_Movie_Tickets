# Danh sách kiểm thử Bước 7

## Kết quả đã xác nhận

- Mã nguồn Bước 7 biên dịch với Java 11: **đạt** trên bản ZIP tạo ở đây.
- Client không import JDBC, không gọi MySQL trực tiếp: **đạt** qua kiểm tra mã nguồn.
- TCP Server đang chạy ở cổng 2040 từ chối `GET_BOOKINGS`, `ADMIN_GET_USERS` và `GET_MOVIES` khi chưa đăng nhập: **đạt**.
- Hai Client cùng chọn một ghế: ảnh người dùng gửi cho thấy một Client nhận lỗi ghế vừa được đặt. Đây là **bằng chứng thủ công** cho tình huống tranh chấp ở Bước 6.
- Bài kiểm thử tự động trên MySQL máy người dùng: **chờ chạy** bằng `tests.Step7SmokeTest`.

## Bài kiểm thử tự động

Bài `tests.Step7SmokeTest` cần hai tài khoản thường và một suất chiếu tương lai còn ghế. Nó kiểm tra:

1. Chưa đăng nhập không xem được vé hay dữ liệu quản trị; đăng ký sai bị từ chối.
2. Sai mật khẩu bị từ chối; hai tài khoản thường đăng nhập; không được xem danh sách người dùng.
3. Danh sách phim, suất chiếu và sơ đồ ghế trả dữ liệu hợp lệ.
4. Một ghế bị lặp trong cùng yêu cầu đặt vé bị từ chối.
5. Hai Client cùng đặt một ghế: đúng một thành công.
6. Chỉ chủ đơn xem/hủy được vé; ghế chuyển sang đã đặt.
7. Hủy đơn mở lại ghế; không thể hủy hai lần; đăng xuất chặn xem vé.

## Kiểm thử thủ công còn cần làm

- Đăng ký tài khoản mới bằng giao diện. Thử email/SĐT đã tồn tại và mật khẩu quá ngắn; phải báo lỗi rõ ràng.
- Đăng nhập bằng email rồi bằng SĐT; thử sai mật khẩu.
- Chọn phim, xem suất chiếu, chọn hai ghế trống rồi đặt. Kiểm tra tổng tiền bằng số ghế nhân giá vé; xem chi tiết trong “Vé của tôi”.
- Hủy đơn vừa đặt, tải lại sơ đồ ghế; hai ghế trống lại. Thử hủy lần hai; phải báo lỗi.
- Đăng nhập tài khoản thường, thử tab quản trị; không thể sử dụng.
- Dùng tài khoản admin: tạo **phim thử mới**, **phòng thử mới**, **suất chiếu thử mới**. Xem danh sách và sửa từng mục khi chưa có vé.
- Xem người dùng, mọi đơn vé và thống kê. Doanh thu chỉ tính đơn còn hiệu lực; hủy đơn làm giảm doanh thu.
- Xóa suất chiếu thử, phòng thử, phim thử theo thứ tự đó. Thử xóa suất/phòng đang có vé; phải bị chặn.
- Đóng Server khi Client đang mở rồi thao tác; Client phải báo không liên hệ được, không đứng giao diện.

## Tiêu chí kết thúc Bước 7

Bảy mục tự động đạt, các mục thủ công ở trên đạt và không còn lỗi trong Terminal Server. Nếu một mục thất bại, ghi lại thao tác, thông báo lỗi và ảnh hai Terminal để sửa đúng nguyên nhân.
