# Ghi chú chuẩn bị hạ tầng agent

Đã đọc README và quy trình trong bộ hướng dẫn tại `D:/AppData/Code/product/mobile-hrm/afolder/agent-bootstrap/.agent-bootstrap`. Đây là bộ tài liệu bootstrap cho AI agent, không phải bộ thiết kế UI.

## Hiện trạng có bằng chứng

- Workspace hiện có Git và bộ tài liệu mockup mới tạo, chưa có manifest app hoặc source RN Flutter.
- Chủ sản phẩm chưa quyết định RN hay Flutter. Không ghi lệnh build test giả định thành lệnh chạy được.
- Nhiệm vụ đang được giao là tài liệu BA và mockup phone web, có bổ sung chia tiền nhóm.
- Không thay đổi thư mục nguồn bootstrap ở ổ D.

## Cách vận dụng trong bàn giao này

Ghi rõ yêu cầu đã xác nhận và quy tắc đề xuất, lưu phạm vi và điểm chưa xác minh, kiểm tra mockup bằng trình duyệt thật, ghi lại kết quả thật trong QA.md. Không tự nhận mockup có backend, phân quyền server hoặc đồng bộ nhiều người.

## Blueprint cho bước triển khai tiếp theo

Đề xuất mức minimal sau khi chủ sản phẩm chọn stack. Một ứng dụng mobile và một trải nghiệm web có thể dùng chung hợp đồng nghiệp vụ; chưa giả định hai client cùng một codebase. Giữ BA làm nguồn yêu cầu, tách các quy tắc tính phiếu và chia tiền khỏi UI.

Các tài liệu có ích khi bắt đầu production: hồ sơ dự án với stack và lệnh thực tế; hướng dẫn chạy local; quyết định xác thực phiên khách; hợp đồng API và phân quyền; trạng thái công việc; checklist release Android web và iOS nếu có. Xác minh lại manifest trước khi viết lệnh. Những thay đổi vào hướng dẫn gốc, hook, CI hoặc cấu hình đang hoạt động cần blueprint cụ thể theo quy trình bootstrap; chưa thực hiện việc thiết lập đó trong bộ BA UI này.

## Câu hỏi còn mở

RN hay Flutter; web có dùng chung framework không; backend và cơ chế xác thực; yêu cầu phê duyệt khoản ghi hộ; tài khoản phát hành; chính sách lưu dữ liệu và quy trình xuất xóa sổ tiền. Không suy ra các câu trả lời từ repo mobile HRM chứa bộ bootstrap.
