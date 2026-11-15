# Đối chiếu backend hiện tại với nghiệp vụ Kèo

Ngày 10/11/2026. Nguồn là OpenAPI 3.1.0 do chủ sản phẩm dán trực tiếp trong hội thoại từ [Swagger backend](https://let-splay-be.onrender.com/swagger-ui/index.html#/). Bản trích bằng chứng tại [swagger-evidence-2026-11-10.json](swagger-evidence-2026-11-10.json) **không phải bản OpenAPI đầy đủ dùng generate client**. Nội dung dán lặp hai lần; báo cáo phân tích một bản.

**Kết luận:** backend đã mô tả được nền tạo/tham gia phòng, phiên khách, tài khoản, đề xuất, lịch rảnh, sổ chi, thanh toán và SSE. Tuy nhiên hợp đồng hiện tại chưa khớp toàn bộ BA đã bàn: phiếu đang là bật/tắt; chốt một đề xuất; chưa có bước thứ tự nhiều chặng; quyền host sửa/xóa ý của người khác rộng hơn BA; FINALIZED không mở lại; rời phòng xóa phiếu. Phần tiền có endpoint nhưng schema chưa đủ rõ để xác nhận các bất biến tài chính. Chưa thấy endpoint ảnh/album/cảm xúc/DM trong bản được cung cấp.

Đây là review hợp đồng, **chưa phải test runtime hoặc kết luận có lỗ hổng đang khai thác được**. Không có source backend, token test hoặc response thật. Công cụ web không truy cập được URL; curl trong workspace lỗi proxy 127.0.0.1; cả browser host và native computer-use đều không khả dụng. [Website Vercel](https://letsplay-eta.vercel.app/) vì vậy chưa được kiểm tra trực quan hay thao tác. Không suy ra website dùng đúng API hoặc render đúng chỉ từ Swagger.

## 1 Cách phân loại

`PHÙ HỢP MỘT PHẦN` là đã có route/schema cho nhu cầu nhưng chưa chứng minh behavior. `LỆCH HỢP ĐỒNG` là mô tả/schema nói hành vi khác BA. `CHƯA ĐẶC TẢ` là thiếu thông tin để kết luận, không đồng nghĩa code chưa làm. `CHƯA THẤY ROUTE` là không có trong JSON được gửi, không khẳng định toàn bộ codebase không có.

## 2 Những phần có thể tái sử dụng

| Nhóm | Bằng chứng trong Swagger | Đánh giá |
| --- | --- | --- |
| Tạo / xem trước / join | POST /api/rooms, /api/rooms/preview, /api/rooms/join | Đúng hướng; CreateRoom có requestId và guestId; join mô tả tiếp tục membership. Cần test dedup, tên rỗng và code hết hạn |
| Danh tính online | /api/auth/register, login, google, me, rooms; /api/rooms/{id}/claim | Có contract tài khoản và gắn membership khách; không cần mặc định xây một hệ login thứ hai cho ảnh |
| Quyền phòng | GET phòng và kick/leave; nhiều operation có GuestSession | Có ý định kiểm membership; chưa test 401/403/404, host/member và token hết hạn |
| Đề xuất ẩn tác giả | Proposal có createdByMe, không thấy authorId | Có thể hỗ trợ UI quyền của mình mà không lộ tác giả; cần kiểm mọi response/comment/event |
| Lịch rảnh | Options, thêm slots, PUT phiếu nhiều giờ, chọn giờ, comment ẩn tên | Là tính năng thêm có ích, không thay thế phiếu ba mức hoặc Borda hoạt động |
| Kế hoạch / tham gia / xe | plan, attendance, preferences, ride, meetup-point | Có contract bổ sung; enum trạng thái và quyền chi tiết còn thiếu |
| Chi tiêu | GET/POST expenses, PUT/DELETE expense, POST settlements, PUT settlement status | Có sổ và điều chỉnh theo endpoint; response object chung chưa cho FE biết model thật |
| SSE và push | GET events với Last-Event-ID, push-subscriptions | Tái sử dụng transport; cần event payload, replay/retention và quyền theo recipient |
| Calendar | GET/PUT calendar-link lưu eventId | Chứng minh có API lưu liên kết, chưa chứng minh đã gọi Google tạo/cập nhật sự kiện |

## 3 Các lệch và thiếu cần xử lý

| ID | Ưu tiên | Bằng chứng cụ thể | So với BA và tác động | Việc cần làm |
| --- | --- | --- | --- | --- |
| GAP-01 | P0 | POST proposals/{id}/vote không body, mô tả bật/tắt; Proposal có votedByMe boolean/voteCount | BA UC04 cần 2/1/0, chưa chọn khác 0, upsert một phiếu; retry toggle có thể đảo ý ngoài ý muốn | Thêm command đặt giá trị rõ ràng, roundId và idempotency; không map boolean thành ba mức |
| GAP-02 | P0 | ChangePhase.selectedProposalId là một string; RoomResponse.selectedProposalId đơn | BA UC05–06 có shortlist nhiều hoạt động và nhiều chặng | Thêm selectedProposalIds/itinerary có version; giữ room legacy theo chế độ cũ nếu còn client |
| GAP-03 | P0 | Mô tả phase chỉ COLLECTING ↔ VOTING → FINALIZED | Thiếu Sắp lịch/Borda, phiếu thứ tự đủ, khóa shortlist | Thêm module order ballot, shortlist revision và plan version; poll lịch rảnh là khác nghiệp vụ |
| GAP-04 | P0 | update/deleteProposal mô tả người tạo **hoặc chủ phòng**, trước chốt | BA chỉ tác giả sửa/xóa ý mình trong Góp ý; không sửa khi đang vote/order | Thu hẹp quyền/state; host điều hành qua chuyển giai đoạn, không sửa hộ nội dung ẩn tên |
| GAP-05 | P1 | FINALIZED được mô tả trạng thái cuối; quay COLLECTING giữ phiếu | BA cho mở lại; đổi nội dung cần vòng mới, giữ lịch cũ và bỏ hiệu lực phiếu cũ | Thêm reopen có phân biệt đổi giờ/thứ tự và đổi nội dung; không tái dùng phiếu cho ý đã đổi |
| GAP-06 | P0 cần test | Proposal.voters[] chứa Voter.displayName, có voteCount | Khi đang vote, BA không trả danh tính phiếu/aggregate sớm. Schema có field chưa chứng minh runtime đã trả | Test payload theo phase/user; nên bỏ voters khỏi public DTO và chỉ trả thống kê đúng giai đoạn |
| GAP-07 | P1 | leave mô tả xóa phiếu theo membership | BA giữ đóng góp của lần chốt hiện tại để rời nhóm không đổi lịch đã chốt | Snapshot electorate/plan/votes theo vòng; leave chỉ thu hồi truy cập, không sửa lịch sử đã chốt |
| GAP-08 | P0 chưa rõ | SettlementStatus.status là string không enum; endpoint cho “đã thanh toán hoặc đang chờ” | Chưa biết ai được xác nhận, có reject/cancel/reverse không, trạng thái nào đổi số dư | Đặc tả enum + transition + actor; chỉ người nhận confirm, retry không cộng hai lần, đảo hai bên |
| GAP-09 | P0 chưa rõ | Expense response/settlement/ledger là object additionalProperties, không DTO cụ thể | FE/QA chưa biết balances, revisions, approval, history hay số nguyên | Công bố DTO Expense/Share/Settlement/Balance/Error và ví dụ số dư; test tổng bằng 0 |
| GAP-10 | P0 chưa rõ | Schema Expense có payers[] và shares[], không approvalStatus/approval command rõ | Ghi hộ theo BA phải chờ người trả; multi-payer là mở rộng của backend, không tự là lỗi | Giữ multi-payer nếu chọn; định nghĩa tất cả payer nào phải duyệt, chỉ tính khi đủ duyệt; không âm thầm cắt còn một người |
| GAP-11 | P0 chưa rõ | Có PUT/DELETE expense và “lịch sử xóa”, không hợp đồng điều chỉnh sau confirmed settlement | Lưu lịch sử xóa không đủ chứng minh không làm đổi công nợ đã đối soát | Khóa sửa/xóa trực tiếp khi có thanh toán liên quan; thêm adjustment approval/reversal và audit |
| GAP-12 | P0 hợp đồng | totalAmount không nằm required Expense; amount không required Settlement/Allocation; title có minLength=0 | OpenAPI chưa nói đủ validation và default; có thể backend custom validate nhưng FE không biết | Chốt field bắt buộc/default; sum(payers)=sum(shares)=total; không trùng membership, không decimal/âm/self-transfer |
| GAP-13 | P0 hợp đồng | Không thấy If-Match/idempotency headers; có Room.revision và CreateRoom.requestId | Revision có mặt không đồng nghĩa concurrency/idempotency đã thực thi, toggle đặc biệt rủi ro | Định nghĩa request key cho tiền/phiếu/ảnh, conflict error, commit và replay; ưu tiên tương thích hiện tại |
| GAP-14 | P0 hợp đồng | Nhiều operation tiền/plan có Authorization optional và không security operation/global; một số route có GuestSession | Đây là thiếu mô tả auth, **chưa là bằng chứng route public** | Khai báo security đúng cho từng route; phân biệt account token/room guest token; test chéo phòng/user |
| GAP-15 | P1 hợp đồng | Chỉ mô tả success; thiếu Error schema/status; nhiều enum là string tự do | FE không xử lý được trạng thái khóa, conflict, invalid input một cách ổn định | Enum hóa phase/status/attendance/settlement, công bố lỗi và response JSON đúng Content-Type |
| GAP-16 | P0 trước ảnh | Profile.id có account, Member.id có membership; chưa mô tả mapping/cross-room identity, friendship/block | Ảnh cá nhân và DM cần owner ổn định qua phòng/thiết bị; không dùng membership hoặc tên làm account | Tái dùng account hiện có; thiết kế claim/merge không nhân owner; thêm friendship/block/sharingEpoch |
| GAP-17 | P0 cho scope mới | Không thấy media/moment/placement/album/reaction/photo comment/DM route trong bản JSON | Chưa đáp ứng tính năng lõi ảnh mới và ba chế độ sau kết thúc host cấu hình | Triển khai theo BA ảnh, qua lớp tương thích tại tài liệu mapping, không coi comment đề xuất là DM/album |
| GAP-18 | P1 trước tích hợp | SSE schema là SseEmitter.timeout; RoomResponse có accessToken | Chưa có event envelope thực tế; nếu serialize chung token/user-specific field có thể gửi sai người | Công bố event data/revision/replay; dựng payload theo recipient, không phát token trong broadcast/log/push |

P0 là thứ tự xử lý/review đề xuất, không kết luận mọi dòng là bug runtime. “Không thấy route” cần backend xác nhận code nội bộ và các API bị ẩn khỏi Swagger trước khi mở ticket xây lại.

## 4 Đối chiếu riêng tính năng ảnh

Trong input được cung cấp không có route/schema MediaAsset, Moment, Placement, Album, PhotoReaction, PhotoComment, AlbumComment, Conversation hoặc Message. `RoomCommentRequest` và Comment trong proposal/availability là bình luận ẩn danh của kế hoạch, không thỏa yêu cầu comment ảnh có tên hoặc DM hai người. `PushSubscriptionRequest` hiện nói thông báo đề xuất mới; chưa có audience/event cho ảnh.

Ba lựa chọn người dùng đã xác nhận phải giữ: từ phòng chọn sẵn album và cá nhân tùy chọn; đủ reaction/comment chung/DM; host chọn VIEW_ONLY/OPEN/WINDOWED sau kết thúc. Chi tiết chưa chốt vẫn là đề xuất tại BA/08, không lấy prototype làm biên bản duyệt mọi mặc định.

### 4.1 Delta UI ngày 11/11/2026

UI mong muốn không chỉ là thay layout. Feed xem ảnh của mình và người khác cần API list theo `cursor`, `nextCursor`/`hasMore`, item theo `placementId` và ACL tại từng request. Composer sau chụp cần upload lifecycle, audience preview, publish atomic và idempotency; nếu chỉ đổi màn hình mà giữ local save thì không đáp ứng được “tải ảnh lên/chọn người chia sẻ”.

Phần có thể làm độc lập bằng mock là bố cục feed, skeleton, prefetch, composer, progress và copy. Phần cần backend/adapter là:

- quyền xem từng placement/media, filter tác giả không mở rộng ACL;
- cursor pagination và xử lý refresh/retry/mất quyền giữa các trang;
- upload/processing/publish, chọn PERSONAL/ROOM, audience snapshot;
- receipt sau commit, retry timeout không tạo bài trùng và event invalidate feed/album.

Do đó không nên kết luận các UI này “không liên quan backend”. Chỉ visual shell không liên quan; dữ liệu, quyền, phân trang và trạng thái đăng liên quan trực tiếp. Contract mục tiêu chi tiết tại [09-ui-feed-and-capture-composer.md](../ba/photo-memories/09-ui-feed-and-capture-composer.md) và các mục 3–7 của [05-api-contract.md](../ba/photo-memories/05-api-contract.md). Đây vẫn là gap/đề xuất cho backend ngoài repo, chưa phải route đã có.

## 5 Runtime cần kiểm khi truy cập được

Tạo hai tài khoản test và một guest trong môi trường test được backend chỉ định; không dùng nhóm thật. Kiểm tạo/join lặp và claim không nhân membership; quyền host/member/cross-room; payload ẩn tên/phiếu trong từng phase; retry vote/settlement sau commit mất response; chia 100 đồng ba người; người ứng không dùng khoản chi; ghi hộ cần payer duyệt; confirm người nhận; sửa/xóa sau thanh toán; leave không đổi plan snapshot; SSE reconnect/revoke và token không broadcast.

Lấy response thật và source handler để chuyển từng GAP sang VERIFIED_PASS/VERIFIED_FAIL hoặc DECISION_REQUIRED. Hiện không ghi PASS cho website, API runtime, database constraints, cloud security hoặc đồng bộ đa thiết bị.

## 6 Kết quả website hiện tại

**UI_NOT_VERIFIED**. URL đã nhận là https://letsplay-eta.vercel.app/. Không có screenshot/DOM/session truy cập thành công trong lượt này. Chưa xác định màn hình đang hiển thị, frontend có gọi backend trên Render không, tài khoản nào cần để xem, hay các thao tác trên web có đúng BA. Prototype mới là bản riêng theo nghiệp vụ mục tiêu, không phải bản sửa hoặc bản xác nhận website đang deploy.
