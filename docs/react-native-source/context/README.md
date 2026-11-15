# Bộ BA và mockup Kèo

Đặc tả bổ sung ngày 10/11/2026 cho chụp ảnh, album nhóm, cảm xúc, bình luận và tin nhắn riêng nằm tại [bộ BA Khoảnh khắc](../docs/ba/photo-memories/README.md). Đọc bộ mới và sổ quyết định trước khi triển khai ảnh; `BA.md`/`BA.html` ở đây là tài liệu cũ của kế hoạch và chia tiền, chưa chứa yêu cầu ảnh mới.

Mở `mockup.html` trực tiếp bằng Chrome hoặc Edge. Không cần cài package, chạy server hay kết nối Internet. `BA.html` là bản tài liệu dễ đọc và có nút in; `BA.md` là nguồn có thể chỉnh sửa.

## Tệp bàn giao

- BA.md và BA.html: đặc tả nghiệp vụ tiếng Việt, quyền, luồng phòng, dữ liệu, ngoại lệ và tiêu chí chấp nhận.
- mockup.html: prototype responsive, có chế độ Phone và Web, chạy độc lập bằng HTML CSS JavaScript.
- mockup-web.png và mockup-phone.png: ảnh xem nhanh màn bình chọn.
- phone-gop-y.png, phone-sap-lich.png và phone-da-chot.png: các màn tiếp theo trên điện thoại.
- web-sap-lich.png: màn sắp lịch trên desktop.
- phone-chia-tien.png và web-chia-tien.png: màn chia tiền nhóm.
- BOOTSTRAP-NOTES.md: ghi chú vận dụng bộ agent-bootstrap đã được chỉ đường dẫn.
- QA.md: kết quả kiểm tra thực tế và các phần chưa kiểm chứng.

## Bổ sung chia tiền nhóm

Tab Chia tiền luôn ở trong phòng, dùng dữ liệu chi tiêu có tên. Thử thêm khoản, chọn người đã trả, chọn người tham gia, chia đều hoặc nhập phần riêng. Có tổng chi, số dư An, gợi ý ai cần trả ai và lịch sử thanh toán. Nút mô phỏng chuyển tạo pending; chỉ xác nhận khoản có người nhận An thì số dư mới đổi. Không chuyển tiền thật.

money.js là nguồn tác giả của chức năng chia tiền; bản bàn giao đã nhúng nội dung vào mockup.html để HTML vẫn mở độc lập. Nếu chỉnh money.js cần nhúng lại vào HTML. Chi tiết production về ghi hộ, điều chỉnh, chia phần lẻ và xác nhận các bên nằm trong mục 13 của BA.

## Những gì thử được

Thanh đầu là công cụ trình diễn. Chọn giai đoạn để xem các màn, đổi vai chủ phòng hoặc thành viên. Trong phòng, thử chọn đủ bốn hoạt động rồi bấm Xong lượt chọn. Chủ phòng có thể đóng vòng sau xác nhận, chọn shortlist, dùng nút lên xuống hoặc kéo bằng chuột để đổi thứ tự, gửi thứ tự, chốt sau khi xác nhận lưu ý, rồi đánh dấu từng chặng hoàn thành.

Ở giai đoạn Góp ý, thêm nhiều đề xuất, sửa xóa ý vừa tạo và hưởng ứng ý mẫu. Nút Tạo buổi mới và biểu tượng trang chủ mở luồng tạo hoặc tham gia; mã tham gia mẫu là KEO726. Thử mã sai để xem lỗi. Có thể thêm ghi chú lưu ý và đặt lại dữ liệu mẫu.

## Giới hạn được chủ động giữ trong prototype

- Chỉ là mockup đánh giá UI và flow, chưa phải app production.
- Không có server, danh tính thật, lời mời thật, đăng nhập, phân quyền server hoặc đồng bộ nhiều người. Dữ liệu cục bộ không có tính bảo mật ẩn danh.
- Bốn phiếu của bạn bè là dữ liệu fixture. Không giả lập một server realtime.
- Thứ tự cá nhân được chỉnh và lưu; tổng hợp Borda nhiều thành viên chưa được cài. Lịch mẫu dùng thứ tự hiện tại và giờ bắt đầu 18 giờ, thêm 15 phút di chuyển mỗi chặng. Chưa có biểu mẫu chỉnh giờ từng chặng.
- Chưa có quản lý thành viên, deadline, khôi phục phiên, lịch sử vòng, thông báo, trường địa điểm, cơ chế chống spam hay kiểm tra ngân sách tự động. BA mô tả yêu cầu để xây tiếp, không khẳng định mockup đã thực hiện hết.
- Kéo chuột hoạt động trên desktop. Điện thoại và bàn phím dùng nút Lên Xuống, không cần kéo chính xác.
- Tên hiển thị trong prototype cố định là An; trường nhập tên trong biểu mẫu chỉ minh họa luồng vào phòng.
- Phòng mới vẫn dùng nhóm bạn mẫu sáu người để so sánh bố cục; không tạo nhóm thật.
- Có localStorage giữ thao tác trên trình duyệt. Nếu không được cấp lưu trữ, bản mẫu hiện thông báo và vẫn có thể thao tác trong phiên.

## Hướng thiết kế

Màu xanh trầm, nền kem và điểm nhấn đất nung; thẻ nội dung rõ; font hệ thống hỗ trợ tiếng Việt, không tải font bên ngoài. Desktop chia điều hướng, nội dung và lưu ý; phone một cột với lưu ý mở theo yêu cầu. SVG nội tuyến, không emoji cấu trúc. Phản hồi thao tác nhẹ và tôn trọng prefers-reduced-motion.

Đã tham khảo skill ui-ux-pro-max. Hai truy vấn design-system trả về hướng landing mang chủ đề cưới không phù hợp nên không dùng kết quả đó làm design system. Hướng màu và bố cục trong mockup là lựa chọn thiết kế riêng cho sản phẩm. Truy vấn UX về dragging movements khớp yêu cầu; áp dụng nút thay thế kéo, tên truy cập và focus hiển thị.

## Các điểm cần chủ sản phẩm duyệt

Thang điểm 2 1 0; Borda; quyền chủ phòng chốt khi thiếu phiếu; phiên khách; khôi phục quyền; lưu xóa dữ liệu 90 ngày. Xem BR01 đến BR09 và mục 11 trong BA.

Đây là bộ thiết kế sản phẩm chưa chốt RN hoặc Flutter. Không có kết nối dịch vụ bên ngoài hoặc triển khai công khai.
