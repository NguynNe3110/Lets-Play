# Tích hợp yêu cầu mới vào backend hiện có

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../ba/product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Đọc [báo cáo GAP](backend-gap-review.md) trước. Swagger hiện có prefix `/api`; tên logical API trong BA ảnh là hợp đồng mục tiêu, không là lệnh đổi toàn bộ route/service hiện tại. Không tạo một auth service mới chỉ vì tài liệu cũ từng nói repo mobile chưa có backend.

## 1 Ranh giới danh tính

`Profile.id` là ứng viên stable account ID cho owner Moment và participant Conversation; backend phải xác minh độ bền/uniqueness thực tế. `Member.id` dùng nhận dạng membership trong phòng, không dùng làm userId xuyên phòng. Guest access token, account bearer token và guestId do client gửi là ba khái niệm riêng; không lấy guestId tự khai làm bằng chứng sở hữu.

Đề xuất: người dùng vẫn vào phòng bằng guest theo luồng đang có, nhưng đăng/chia sẻ ảnh cá nhân và DM cần account đã xác thực. Claim membership vào account phải giữ quyền ảnh đúng owner, tránh nhận ảnh hoặc vai trò của người khác. Phương án cho guest sở hữu ảnh cần contract khôi phục/chuyển quyền riêng; chưa coi đó là yêu cầu đã duyệt. Không đổi client đang dùng guest trước khi có plan migration.

## 2 Bản đồ chức năng và thay đổi

| Khả năng mục tiêu | API hiện có liên quan | Hướng tích hợp đề xuất |
| --- | --- | --- |
| Account / login | /api/auth/* và Profile | Dùng lại, xác minh expire/revoke/Google token; không copy password sang module ảnh |
| Room membership | create/join/get/claim/leave/kick | Adapter cho `canReadAlbum`; membership revoke có hiệu lực tại commit, không chờ SSE |
| Planning phase | POST phase với COLLECTING/VOTING/FINALIZED | Giữ cho LEGACY_SINGLE; thêm planningMode GROUP_ITINERARY cho room mới sau khi chốt quyết định |
| Ba mức phiếu | POST toggleVote cũ | Bổ sung PUT /api/rooms/{roomId}/proposals/{id}/my-vote body `{value:0|1|2,roundId}`; response thống kê theo quyền, không suy giá trị từ boolean |
| Shortlist / order / plan version | selectedProposalId, plan cũ | Command shortlist, order-ballot và plan-version mới; không nhét array vào field string cũ |
| Lịch rảnh | availability routes | Giữ như poll nhiều giờ; không dùng availableCount làm điểm Borda hoạt động |
| Chi tiền nhiều payer | expenses với payers/shares | Có thể giữ; thêm DTO, sum invariant, approval và adjustment; tên fields existing map tại adapter |
| Settlement | status PUT hiện tại | Thu hẹp actor/transition; enum và idempotency; có thể thêm command confirm/reject/cancel/reverse để rõ ý định |
| Upload và Moment | Chưa thấy | Logical /media/*, /moments dưới /api, áp BA 04–06; tách Media READY và publish |
| Chia sẻ cá nhân / nhóm | Chưa thấy | Placement và PersonalGrant; không thêm một `isPublic` vào ảnh để thay hai tập quyền |
| Album / ba chế độ | Room.status string, phase FINALIZED | Tách room lifecycle khỏi planning phase; thêm album policy/deadline từ endedAt; không dùng FINALIZED đồng nghĩa album chỉ đọc |
| Friend / block / DM | Chưa thấy | Dùng account ID ổn định, pair uniqueness, epoch revoke; source placement reference kiểm quyền mỗi lần đọc |
| Event ảnh | SSE room hiện có | Tái dùng transport nếu phù hợp; personal/DM dùng stream user có audience riêng, không phát toàn bộ lên room SSE |
| Notification ảnh | Push proposal/notifications room | Thêm type/audience/dedup theo user + command; DM không xuất hiện trong room notification |

Tên route **mới** ở bảng là đề xuất để review; chưa phải endpoint có thật. FE không được tự gọi trước khi BE công bố version/schema tương ứng. Chọn naming khác được nếu vẫn ánh xạ rõ FR/BR/AC và giữ semantics.

## 3 Tương thích và migration

Không âm thầm dịch COLLECTING/VOTING/FINALIZED thành đủ bốn phase và vòng đời mới, vì FINALIZED hiện chọn một proposal còn BA mới là lịch nhiều chặng. Giữ room cũ ở `LEGACY_SINGLE`, snapshot lựa chọn/phiếu cũ; room mới dùng `GROUP_ITINERARY` khi sản phẩm chọn. Client nhận capabilities/planningMode để render, không đoán từ tên enum.

Không biến phiếu true thành “Rất muốn” hay false thành “Để dịp khác” vì false còn có thể là chưa bầu. Nếu room đổi mode, giữ phiếu cũ như lịch sử và yêu cầu bình chọn mới. Mở lại kế hoạch có phiên bản; không chạy migration xóa lịch sử hoặc thanh toán đã xác nhận.

Backend có thể dùng /api/v2 cho các thay đổi không tương thích hoặc command song song trong /api. Chọn một chiến lược và công bố trong ADR trước deploy; không bắt buộc đổi prefix toàn hệ thống sang /v1 vì BA ảnh từng dùng nó làm ví dụ.

## 4 Điều kiện xác nhận một GAP đã xử lý

Ticket ghi GAP ID, yêu cầu gốc, route/DTO trước và sau, quyền actor/state, dữ liệu cũ xử lý ra sao, test âm tính, retry/race và bằng chứng response. Review source và chạy test trong repo backend thật. Prototype chứng minh trải nghiệm mô phỏng, không chứng minh server/db đã enforce cùng quy tắc.
