---
name: keo-backend-contract
description: Review hoặc triển khai backend Kèo dựa trên Swagger hiện có và BA mục tiêu, giữ tương thích tài khoản, phòng, phiếu và sổ tiền; làm rõ các GAP và tích hợp ảnh, album, DM. Dùng khi giao việc cho backend hoặc AI trong repo backend, không dùng để suy ra runtime đã đúng chỉ từ Swagger hoặc prototype.
---

# Backend contract Kèo

Đọc [đối chiếu](../../docs/integration/backend-gap-review.md), [mapping tương thích](../../docs/integration/api-compatibility-map.md), [BA ảnh](../../docs/ba/photo-memories/README.md) và [quyết định](../../docs/ba/photo-memories/08-decisions.md). Đọc code/schema/test thật trong repo backend trước khi thay đổi; snapshot Swagger là bằng chứng contract tại một thời điểm, không thay source hiện tại. File evidence được ghi rõ là bản trích, không đem generate client.

## Những điều phải giữ đúng

Backend đã có contract account login/register/Google, room guest/claim, ledger nhiều payer và room SSE. Tái sử dụng và bổ sung theo nhu cầu; không xây hệ auth thứ hai, đổi hết prefix /api, xóa API cũ hoặc làm mất room/phiếu/công nợ đã tồn tại chỉ để khớp ví dụ trong BA.

Tách user/account khỏi membership; không nhận tên, guestId tự khai hoặc role client gửi làm quyền. Không trả author/voters cá nhân của ý tưởng ẩn tên. Phiếu mục tiêu có giá trị 2/1/0 và chưa chọn riêng, upsert deterministic, không toggle khi retry. Lịch rảnh nhiều giờ khác shortlist/Borda nhiều chặng.

Tiền là số nguyên VND, tổng payer bằng tổng share bằng total. Ghi hộ đợi người ứng duyệt, nhận tiền chỉ receiver xác nhận, pending không đổi balances. Sau thanh toán, sửa sai qua điều chỉnh/đảo có lịch sử và chấp thuận cần thiết. Multi-payer là khả năng đang có trong schema, không tự loại bỏ; chốt semantics tại ticket.

Ảnh dùng Moment và Placement riêng, quyền PERSONAL/ROOM riêng. Comment ảnh nhóm, comment album và DM là ba thread khác nhau. Host không đọc DM hoặc xóa media owner khác. Ba chế độ album sau kết thúc do host chọn là yêu cầu đã xác nhận; hạn/default/quota/retention còn chi tiết đề xuất, đọc 08.

## Cách xử lý GAP và triển khai

Đối chiếu GAP với handler/service/entity/migration/test hiện tại và response thật của môi trường test được chỉ định. Ghi CONTRACT_MISMATCH, VERIFIED_PASS, VERIFIED_FAIL, DECISION_REQUIRED hoặc NOT_VERIFIED cùng bằng chứng. Schema có voters không tự chứng minh runtime rò dữ liệu; security operation thiếu không tự chứng minh API public. Không báo pass từ tên route, screenshot, UI mock hoặc bản prototype.

Khi cần đổi contract, giữ khách hàng cũ qua adapter/version/capabilities. Không ép vote boolean thành ba mức; room chốt đơn cũ khác itinerary nhiều chặng mới. Ticket nêu dữ liệu cũ và rollback tương thích, auth/state, request/response/error, idempotency, concurrency, event và AC liên quan.

Chọn test theo stack và lệnh thật của repo. Kiểm ít nhất actor đúng/sai, cross-room, retry sau commit, deadline/race hoặc invariant tiền khi ticket chạm các điểm đó. Không chạy thử tạo/xóa nội dung hoặc gửi thông báo trên phòng thật chỉ vì có URL production. Không log access token, guest token, signed URL, ảnh hoặc DM text.

Tài liệu quyết định, BA, DTO/API, event và AC phải được cập nhật cùng thay đổi. Giữ các xác nhận C-06–08; không biến một đề xuất hoặc hành vi prototype thành phê duyệt của chủ sản phẩm. Tập trung thực hiện nhiệm vụ đã được giao, không tự thêm cổng xin duyệt cho thao tác đã được người dùng cho phép.
