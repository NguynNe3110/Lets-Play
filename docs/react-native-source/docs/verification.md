# Kiểm tra ngày 05/11/2026

- `yarn typecheck`: thành công.
- `yarn lint`: không có lỗi; còn cảnh báo inline style trong các component React Native.
- Jest: 5 suites, 17 tests thành công. Có luồng phòng mới → góp ý → bình chọn → chốt kế hoạch, lưu/khôi phục nhiều phòng, tách sổ chi, chia phần lẻ và đối chiếu khoản thanh toán.
- Android ARM64: `:app:assembleDebug --offline` thành công.
- Đã đọc package trong APK trước khi cài: `com.uzuu.letplay.debug`, version `1.0.0-debug`.
- Cài APK và cold start trên điện thoại Android thật thành công. JavaScript được Metro biên dịch với HTTP 200; kiểm tra log sau khởi động không thấy lỗi ReactNativeJS/AndroidRuntime.
- Đã xem giao diện thật màn chính/biểu mẫu và phòng mẫu đã chốt trên điện thoại 1080 × 2400. Bộ icon SVG hiển thị đúng. Đã điều chỉnh wrap thanh công cụ phòng mẫu để tránh tràn ngang.
- Đã xem PNG icon launcher ở kích thước 1024; có SVG nguồn và PNG theo các kích thước Android/iOS.

Chưa kiểm tra trên thiết bị iOS, tablet, màn hình xoay ngang hoặc cỡ chữ hệ thống lớn nhất. Chưa kiểm chứng đồng bộ nhiều người vì app hiện chỉ có lưu trữ cục bộ. Không khẳng định đã kiểm thử thủ công mọi biểu mẫu trên máy thật.

Emulator đã thử khởi động nhưng cấu hình AVD sẵn có không hợp lệ; kiểm tra thực tế được tiếp tục bằng điện thoại thật sau khi kết nối lại.
