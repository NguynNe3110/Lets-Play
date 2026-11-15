# Kết quả kiểm tra bộ BA và mockup

Ngày 04 tháng 11 năm 2026. Kiểm tra bằng Chromium của Google Chrome chạy headless qua Playwright. Dùng thư viện runtime có sẵn, không cài dependency trong dự án.

## Luồng đi chơi

Đạt chín kiểm tra: thêm đề xuất; một phiếu mỗi ý; đổi phiếu không cộng thêm; chặn hoàn tất khi còn thiếu; nút đổi thứ tự; thành viên không thấy nút chốt; yêu cầu xác nhận xử lý lưu ý; chốt sang lịch trình; đánh dấu chặng hoàn thành.

## Chia tiền

Đạt bảy kiểm tra: tổng số dư 0; số dư ví dụ sáu người đúng; 100 đồng chia ba ra 34 33 33; chia riêng sai tổng bị chặn; lưu khoản chi giữ tổng phần chia bằng tổng tiền; giao dịch pending không đổi số dư; xác nhận nhận tiền cập nhật sổ và tổng số dư vẫn 0.

## Bố cục và hình ảnh

Kiểm tra 40 tổ hợp: bốn giai đoạn phòng, hai tab kế hoạch chia tiền, năm chiều rộng 375 390 768 1024 1440 CSS px. Sau khi sửa nút lưu ý ở phone nhỏ, không có tràn ngang document trong các tổ hợp này. Màn phone và desktop được render và xem hình trực tiếp. Tài liệu BA.html được mở kiểm tra tiêu đề, nội dung, 13 mục và 4 bảng.

Đã xuất ảnh các giai đoạn trên phone, bình chọn và sắp lịch trên desktop, chia tiền trên cả hai kích thước. Không ghi nhận lỗi JavaScript pageerror trong các lượt kiểm tra.

## Giới hạn xác minh

Không phải kiểm thử app native RN Flutter, không chạy trên iPhone Android thật. Chưa xác minh rung, hiệu năng máy yếu, cảm nhận animation trên máy thật, screen reader hay đầy đủ thao tác bàn phím. Không có backend để kiểm thử phân quyền, bảo mật ẩn danh, đồng bộ nhiều người, giao dịch đồng thời và lưu trữ production. Các yêu cầu đó nằm trong BA để triển khai sau. Không coi 27 tiêu chí chấp nhận trong BA đều đã được thực hiện bởi mockup.
