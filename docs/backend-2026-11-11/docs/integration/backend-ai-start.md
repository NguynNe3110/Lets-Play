# Hướng dẫn bàn giao cho backend và AI

Gửi **toàn bộ gói `handoff/backend-2026-11-11` hoặc `handoff/keo-backend-handoff-2026-11-11.zip`**. Bản 10/11 thiếu các xác nhận mới. Gói được tạo từ nguồn chuẩn bằng `scripts/build-backend-handoff.cjs`; không gửi riêng SKILL.md hoặc riêng bản Swagger. Prototype chưa cập nhật toàn bộ xác nhận mới, chỉ tham khảo giao diện, không phải chuẩn nghiệp vụ mới nhất.

Đọc trước [delta xác nhận 11/11](../ba/product-rules-2026-11-11.md) và [biên bản](../ba/product-confirmation-2026-11-11.md). PC-01–14 thay baseline cũ mâu thuẫn; model/API/event là đề xuất tích hợp, 19 AC chưa chạy. Phần còn mở phải báo DECISION_REQUIRED trước ticket phụ thuộc.

## 1 Thành phần gói

| Thành phần | Dùng để làm gì |
| --- | --- |
| README.md và AGENTS.md của gói | Điểm vào cho người và AI ở repo backend; không áp hướng dẫn chạy React Native vào backend |
| docs/integration/ | Báo cáo GAP, bằng chứng schema rút gọn, mapping API đang có → yêu cầu mới, cách đọc và review |
| docs/ba/photo-memories/ | Toàn bộ nghiệp vụ ảnh/album/DM: quyền, dữ liệu, API, trạng thái, sự kiện, AC và quyết định |
| context/BA.md | Bản gốc nghiệp vụ phòng, ẩn tên, ba mức phiếu, Borda và chia tiền; có ghi chú nguồn lịch sử |
| agent-skills/ | Skill BA, tích hợp và backend contract với liên kết tương đối dùng được trong gói |
| prototype/ | HTML/CSS/JS tĩnh, dữ liệu giả, không gọi backend thật; mở index.html để thử hoặc phục vụ bằng HTTP local |
| evidence/mobile-source/ | Những file mobile nhỏ được BA trỏ tới, chỉ là bằng chứng hiện trạng repo, không là cấu trúc backend phải sao chép |
| manifest.json | Inventory/SHA-256 các file được đóng gói để biết đúng bản bàn giao |

Không cần giao `.git`, `node_modules`, Android/iOS build, `.env`, token, Firebase config, keystore, ảnh/chats cá nhân trong `locket-images`, hoặc toàn bộ `.agent-bootstrap`. Source prototype không chứa ảnh người dùng thật; ảnh minh họa SVG được tạo riêng. Bảng nghiên cứu trong BA đủ giải thích tham chiếu Locket.

## 2 Cách đặt vào repo backend

Đặt gói trong thư mục riêng như `docs/keo-handoff/`, giữ nguyên cấu trúc bên trong. Nếu backend đã có AGENTS.md, chỉ thêm đường dẫn đến AGENTS.md của gói khi làm nghiệp vụ Kèo; **không ghi đè** hướng dẫn stack/build/migration đang đúng của backend. Skill trong gói được AGENTS định tuyến thủ công, không giả định mọi IDE tự nạp skill từ thư mục bất kỳ.

Backend cần gửi lại full OpenAPI JSON từ source/service hiện tại, DTO response có kiểu cho sổ tiền, danh sách enum/transition, test chứng minh quyền/ẩn tên và chiến lược version. File `swagger-evidence` trong gói là bản trích để đối chiếu, không dùng làm nguồn phát sinh client hoặc thay Swagger gốc.

## 3 Thứ tự đọc

1. README gói và `docs/integration/backend-gap-review.md` để biết phần đã có, lệch và chưa được kiểm chứng.
2. `api-compatibility-map.md` để tránh xây lại auth hoặc phá API/room cũ.
3. `context/BA.md` cho room/phiếu/tiền; `docs/ba/photo-memories/08-decisions.md` cho các xác nhận và đề xuất.
4. BA 03–06 khi thiết kế backend; BA 02 cho luồng và BA 07 cho acceptance cases.
5. `prototype/dist/index.html` để hiểu giao diện điện thoại và thử quyền bằng dữ liệu giả.

Prototype theo **nghiệp vụ mục tiêu**, không bắt chước các lệch hiện tại: phiếu ba mức, nhiều chặng, mở lại có lịch sử, receiver confirm, tách nơi đăng ảnh và ba chế độ album. Multi-payer, lịch rảnh và đi lại thể hiện khả năng thêm từ Swagger để review; không tự sửa yêu cầu đã chốt. Không dùng một nút đã bấm được làm bằng chứng server đã enforce.

## 4 Prompt có thể giao AI backend

```text
Đọc README.md và AGENTS.md trong gói bàn giao Kèo, rồi dùng
agent-skills/keo-backend-contract/SKILL.md.

Đọc docs/ba/product-rules-2026-11-11.md và biên bản cùng ngày trước.
PC-01–14 ưu tiên hơn baseline cũ mâu thuẫn. Prototype chỉ tham khảo UI.
Trước triển khai, trả bảng yêu cầu đã có/thiếu/lệch theo source backend,
đề xuất migration và API, chia ticket liên kết AC; liệt kê DECISION_REQUIRED.

Đối chiếu docs/integration/backend-gap-review.md với code, schema, migration,
test và OpenAPI hiện tại của repo backend này. Ghi bằng chứng handler/service
và trạng thái cho từng GAP; tách contract mismatch với runtime chưa kiểm.

Giữ API đang dùng và dữ liệu room/phiếu/tiền cũ. Tái sử dụng account/guest-claim,
SSE và multi-payer hiện có khi phù hợp. Đề xuất adapter/version cho thay đổi
ba mức phiếu và itinerary nhiều chặng; không map boolean thành ba mức.

Cho tính năng ảnh, đọc BA 02–08. Giữ C-06–08 đã xác nhận và phân biệt các D
còn đề xuất. Ticket phải có actor/state, request/response/error, transaction,
idempotency, event, migration và AC. Prototype chỉ là mô phỏng trải nghiệm.

Thực hiện phạm vi được giao trong ticket, chạy test đúng stack của repo,
báo lệnh/kết quả thực và phần chưa kiểm. Không thử trên phòng production
hoặc phát thông báo thật nếu chưa được yêu cầu trong một môi trường test cụ thể.
```

## 5 Những điều cần backend phản hồi trước tích hợp

- Các GAP là chủ ý sản phẩm nào, lỗi contract nào, hoặc đã xử lý trong code nhưng chưa cập nhật Swagger?
- Actor nào được xác nhận settlement; multi-payer/ghi hộ/adjustment/reversal vận hành thế nào? Cung cấp response thật và test.
- Voters/count được trả ở phase nào? Có author/token trong SSE hoặc DTO được broadcast chung không?
- Membership guest gắn Profile.id sau claim thế nào; khi mất thiết bị, rời phòng hoặc xóa account thì owner ảnh/DM xử lý ra sao?
- Prefix/version cho API mới, lifecycle room riêng khỏi phase và ba mode album triển khai tại đâu?

Những câu này để xác định hợp đồng thực tế, không là yêu cầu hỏi lại người dùng ba lựa chọn C-06–08 đã xác nhận. Quyết định phát sinh phải ghi vào sổ quyết định và cập nhật API/AC cùng lúc.
