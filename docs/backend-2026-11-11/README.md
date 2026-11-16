# Gói bàn giao Kèo cho backend và AI

Ngày 11/11/2026. Đây là gói tài liệu, bằng chứng hợp đồng và prototype **giao diện điện thoại**. Không có secret, token runtime, source backend hoặc dữ liệu người dùng thật. Backend đang có contract riêng; không bắt đầu bằng giả định toàn hệ thống chưa có backend.

## Bắt đầu từ đây

**Đọc trước:** [delta đã xác nhận 11/11, model/API/event và 19 AC](docs/ba/product-rules-2026-11-11.md), đối chiếu [biên bản lựa chọn](docs/ba/product-confirmation-2026-11-11.md). PC-01–14 thay các baseline cũ mâu thuẫn được liệt kê. Prototype chưa cập nhật toàn bộ các xác nhận mới, chỉ dùng tham khảo giao diện. Phần còn mở phải báo DECISION_REQUIRED, không tự chọn hoặc coi mọi đề xuất là đã duyệt.

1. Đọc [báo cáo đối chiếu Swagger](docs/integration/backend-gap-review.md): phần phù hợp, 18 GAP và giới hạn chưa test runtime.
2. Đọc [hướng dẫn bàn giao và prompt AI](docs/integration/backend-ai-start.md) cùng [mapping tương thích](docs/integration/api-compatibility-map.md).
3. Đọc [BA ảnh](docs/ba/photo-memories/README.md), đặc biệt [quyết định đã xác nhận và còn đề xuất](docs/ba/photo-memories/08-decisions.md).
4. Dùng [BA phòng và chia tiền gốc](context/BA.md) để đối chiếu ba mức phiếu, Borda và tiền; bỏ qua giả định công nghệ đã cũ.
5. Mở [prototype điện thoại](prototype/dist/index.html) và [hướng dẫn thử](prototype/README.md). Chỉ dữ liệu giả local, không gọi backend thật.

Nếu repo backend đã có AGENTS.md, dẫn đến [AGENTS của gói](AGENTS.md) khi làm nghiệp vụ Kèo; không ghi đè cấu hình/hướng dẫn stack đang có. Các file trong evidence/mobile-source chỉ cung cấp ngữ cảnh source mobile, không là skeleton backend phải sao chép.

Skill dùng cho review/triển khai là [keo-backend-contract](agent-skills/keo-backend-contract/SKILL.md). Dữ liệu kiểm kê và SHA-256 ở manifest.json. File swagger-evidence là **trích chứng cứ**, không là full OpenAPI để generate client. Nguồn JSON đầy đủ phải lấy từ backend/source hiện tại.

Các ảnh tham chiếu Locket riêng tư không kèm gói. Bảng quan sát trong BA/01 vẫn đủ để hiểu yêu cầu. Prototype có ảnh SVG minh họa riêng.
