# Kiểm tra bộ tài liệu Khoảnh khắc

Ngày 10/11/2026. Phạm vi kiểm tra là tài liệu và skill vừa tạo; chưa triển khai camera/backend nên không có kết quả nghiệm thu tính năng.

## 1 Kiểm tra đã thực hiện

- Đọc README, manifest, navigation, types, store và storage của module Kèo để xác minh hiện trạng local và các phase/role đang có. Không đọc hoặc ghi secret.
- Xem trực tiếp 7 PNG trong `context/locket-images`; đối chiếu nguồn Locket chính thức, ghi URL và ngày tại 01. Không suy ra ACL/backend Locket từ screenshot.
- Kiểm cấu trúc bộ BA ban đầu: 24 FR, 38 BR, 10 UC, 14 màn/trạng thái, 75 AC, 12 NFR, 6 phụ thuộc, 8 xác nhận và 19 dòng quyết định/chi tiết đề xuất. Mỗi FR được đưa vào bảng truy vết AC. Sau khi nhận backend/prototype và yêu cầu giao diện điện thoại, đã thêm C-09–11 và tài liệu tích hợp riêng.
- Kiểm đường dẫn Markdown tương đối trong bộ BA, AGENTS, skill và README, JSON fenced examples, số cột bảng, fence đóng/mở, mã tham chiếu không tồn tại, Unicode lỗi và placeholder chưa điền.
- Parse frontmatter hai skill bằng package `yaml` hiện có trong repo; kiểm tên, độ dài, field cho phép, description và scaffold theo tiêu chí tương ứng của skill-creator. Không thêm dependency.
- Đối chiếu thủ công các bất biến: tách placement/ngữ cảnh; quyền owner sau rời; thu hồi grant khi hủy/chặn; deadline từ endedAt; ba chế độ host; archived khác hiddenForMe; xóa đúng phạm vi; DM reference không cấp quyền ảnh.

## 2 Kết quả và giới hạn công cụ

Script kiểm tra tài liệu task-local chạy bằng `node tmp/verify-photo-ba.cjs`. File này ở thư mục tmp bị ignore, không là dependency hoặc thành phần vận hành của app. Kết quả cuối: không có broken local link, ID không tồn tại, JSON lỗi, bảng lệch cột, fence chưa đóng hoặc frontmatter skill sai. Việc này kiểm cấu trúc, không chứng minh toàn bộ nghiệp vụ đã được người dùng duyệt.

Đã thử validator Python đi kèm skill-creator cho cả hai skill; cả hai bị dừng vì môi trường Python thiếu module `yaml` (`ModuleNotFoundError`). Không báo validator gốc pass. Phần kiểm thay thế dùng YAML parser của repo và các kiểm tra cấu trúc tương ứng, không cài package mới hoặc sửa môi trường toàn cục.

Các thay đổi này chỉ là Markdown và hướng dẫn agent, nên không chạy lại Jest, TypeScript, lint app hoặc build native. Các lệnh và kết quả cũ trong `docs/verification.md` không được coi là test cho tính năng ảnh mới. 75 AC hiện ở trạng thái **chưa chạy**.

Cập nhật lần làm prototype: kiểm thử model local và render functions được ghi riêng tại [prototype-verification](../../integration/prototype-verification.md). Không dùng 34 test domain / 100 render path đó để đánh dấu toàn bộ 75 AC production đã pass. Hiện có ba skill, thêm `keo-backend-contract` cho Swagger và repo backend.

## 3 Những điểm phải giữ khi bàn giao

Đã cập nhật ba trả lời của người dùng thành C-06–08: đủ cảm xúc/comment/DM; host thiết lập ba chế độ sau kết thúc; album phòng chọn sẵn, cá nhân tùy chọn. Mặc định OPEN, 72 giờ gợi ý, giới hạn 1–720 giờ, chính sách người mới/rời nhóm, block, audience, quota và retention vẫn có nhãn đề xuất trong 08.

`context/` bị ignore bởi `.git/info/exclude` local. Link ảnh/BA cũ được kiểm tồn tại trong workspace này, không có nghĩa nguồn đó sẽ có trên bản clone. Bộ mới đã tóm tắt quan sát và giới hạn tại 01. Các file `docs/ba/`, `agent-skills/` và `AGENTS.md` mới không bị ignore. Hai skill được AGENTS định tuyến, chưa cài toàn cục và chưa thử hành vi trong một phiên agent độc lập.
