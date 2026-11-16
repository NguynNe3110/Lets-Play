# Hướng dẫn AI backend Kèo

Áp dụng khi làm nghiệp vụ Kèo từ gói này. Giữ hướng dẫn stack, build, migration và quyền hạn hiện có của repo backend; gói không cấp quyền tác động production.

Ưu tiên [delta xác nhận 11/11](docs/ba/product-rules-2026-11-11.md) và [biên bản](docs/ba/product-confirmation-2026-11-11.md) trước mọi baseline/skill/prototype cũ mâu thuẫn: account bắt buộc cho luồng mới; host sửa ý chưa chốt; lookback 30 ngày tại lúc kết bạn/join, không TTL; block không ẩn ROOM; quyền tiền former-member; đóng lưu lịch sử; host đảo settlement có lý do. Model/API là hợp đồng đề xuất, AC chưa chạy. Prototype không phải chuẩn nghiệp vụ mới nhất.

Đọc [README](README.md), [skill backend contract](agent-skills/keo-backend-contract/SKILL.md), [GAP review](docs/integration/backend-gap-review.md) và [mapping](docs/integration/api-compatibility-map.md) trước khi đổi contract. Dùng source/test/OpenAPI của backend làm bằng chứng implementation hiện tại. Report tĩnh không có nghĩa bug runtime đã xác minh.

Yêu cầu mục tiêu ở [BA phòng/tiền](context/BA.md) và [BA ảnh](docs/ba/photo-memories/README.md). [Quyết định](docs/ba/photo-memories/08-decisions.md) phân biệt xác nhận và đề xuất. C-06–08 đã xác nhận: đủ reaction/comment chung/DM riêng; từ phòng album được chọn sẵn, cá nhân tùy chọn; host chọn ba chế độ sau kết thúc. Không hỏi lại các xác nhận này vô cớ; không coi mọi mặc định là đã duyệt.

Không xây lại auth, phá API /api đang dùng, làm mất room/phiếu/công nợ hoặc ép boolean thành phiếu ba mức. Tách account khỏi membership. Comment ẩn tên của đề xuất khác comment ảnh/album có tên và khác DM. Quyền phải kiểm server tại commit và đọc media; prototype không là bảo mật.

Khi nhiệm vụ là BA, dùng [skill BA](agent-skills/keo-photo-ba/SKILL.md). Khi tích hợp tính năng ảnh, dùng [skill tích hợp](agent-skills/keo-photo-integration/SKILL.md) theo phần liên quan; các lệnh React Native chỉ dành source mobile tham chiếu, không áp cho repo backend.

Mỗi ticket liên kết GAP/FR/BR/AC; mô tả actor/state, request/response/error, transaction/retry/concurrency, compatibility/migration và event. Kiểm thử theo lệnh thật của repo. Báo bằng chứng thực, không đánh pass từ prototype hoặc tên route. Không log token/ảnh/DM và không thử trên phòng production khi chưa có yêu cầu test cụ thể.
