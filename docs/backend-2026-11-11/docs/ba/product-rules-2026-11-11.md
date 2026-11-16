# Delta BA đã xác nhận ngày 11/11/2026

Nguồn xác nhận: [biên bản phỏng vấn](product-confirmation-2026-11-11.md). File này là phần bổ sung có ưu tiên cao hơn các đoạn baseline cũ mâu thuẫn được liệt kê bên dưới. Không xác nhận implementation, endpoint đang tồn tại hoặc AC đã pass. Quy tắc không bị thay thế tiếp tục theo bộ BA ảnh và continuous-planning. Không tự đổi prefix API hoặc migration MMKV/native.

## 1 Quy tắc nghiệp vụ thay thế

| ID | Quy tắc đã xác nhận | Phần cũ được thay thế |
| --- | --- | --- |
| PC-01 | Một phòng một chuyến; bắt buộc account. Host chọn join ngay hoặc duyệt; chuyển host cần người nhận đồng ý | Guest là luồng sản phẩm mặc định trong BA cũ; backend guest legacy phải giữ tương thích |
| PC-02 | Góp ý/vote suốt ACTIVE, ba mức 2/1/0, tổng từng mức hiện ngay, giấu author/voter với cả host. Host chọn nhiều hoạt động; Borda n-1..0 gợi ý thứ tự; hòa giữ shortlist ban đầu | Khóa theo phase, giấu aggregate tới cuối vòng, toggle boolean/chốt một ý không đáp ứng mục tiêu |
| PC-03 | Host sửa nội dung ý bất kỳ chưa thuộc lịch chốt ở màn Vote; tác giả sửa ý mình ở Đề xuất. Không sửa nội dung tại Sắp lịch. Sửa vô hiệu phiếu riêng ý, báo nhóm phần nội dung sửa mà không lộ tác giả | Quyền host chỉ sửa ý mình; giữ phiếu sau sửa |
| PC-04 | Lịch chốt khóa nội dung ở mọi màn/API. Muốn thay nội dung tạo đề xuất mới. Host đổi giờ/thứ tự qua version mới, giữ snapshot/completion; được chốt khi thiếu phản hồi với cảnh báo. Thành viên hoàn thành; chỉ host bỏ qua; có hoàn tác/audit | Sửa trực tiếp nội dung chặng đã chốt hoặc reset toàn lịch |
| PC-05 | Camera và nhập ảnh máy; pixel/caption bất biến sau publish. Mỗi PERSONAL chọn tất cả bạn bè hoặc một số bạn; album không tự tạo chia sẻ cá nhân | Camera-only và mặc định không cho camera roll ở D-09 |
| PC-06 | Bạn mới: không mở lịch sử hoặc mở 30 ngày trước mốc kết bạn. Không có full-history cá nhân. Không phải TTL; ảnh sau kết bạn và ảnh đã được mở không tự hết hạn vì thời gian. Hủy bạn thu hồi; kết bạn lại xét như lần mới | Snapshot không bao giờ mở ảnh cũ ở D-07, hoặc cửa sổ trượt 30 ngày |
| PC-07 | Album: FROM_JOIN / LOOKBACK_30_DAYS / FULL, mặc định FULL. Chốt policy tại lần join; đổi policy chỉ áp dụng join sau đó. Mất membership mất album; owner vẫn quản lý ảnh mình | Thành viên mới luôn xem toàn bộ bất kể policy ở D-05 |
| PC-08 | DM giữa bạn bè hoặc cùng phòng với membership hiện hành. Block khóa DM và PERSONAL; không ẩn ảnh/bình luận ROOM theo block, vẫn kiểm membership/lịch sử. Không ảnh hưởng tiền | D-14 và công thức ROOM có block-filter; counts/cover ROOM không lọc block đơn thuần |
| PC-09 | Nhiều payer; chia đều/custom; người không dùng không chịu. Ghi hộ chờ payer, nhiều payer chờ tất cả trước khi tính khoản | Model một payer là giới hạn bản đầu |
| PC-10 | Trước settlement liên quan: host sửa ngay, audit/thông báo. Thành viên đổi tiền payer khác cần host/payer duyệt; tăng share người khác cần người đó hoặc host duyệt. Bản hiệu lực không bị thay bởi đề nghị đang chờ | Sửa creator-only không xét người chịu hoặc cho creator thay tiền người khác ngay |
| PC-11 | Sau settlement liên quan, kể cả host cần điều chỉnh với đồng ý của các bên bị thay đổi; giữ bản gốc. Sender báo chuyển pending; receiver xác nhận mới đổi balance. Đảo settlement do hai bên đồng ý hoặc host ngay với lý do/thông báo, không xóa giao dịch gốc | Cấm host đảo trong mọi trường hợp; host sửa expense sau paid không cần duyệt |
| PC-12 | ENDED vẫn thêm expense/settlement. Người rời chỉ truy cập tài chính liên quan mình để đối soát, không toàn sổ, album hay kế hoạch | Mất membership đồng nghĩa mất mọi đường đối soát |
| PC-13 | Chỉ đóng/lưu lịch sử khi không còn balance/pending tiền; giữ lịch/đề xuất/album/sổ theo quyền cũ, chỉ đọc. Owner được xóa ảnh mình. Bản đầu không tự hết hạn | Xóa phòng nghiệp vụ = DELETED thu hồi toàn album; retention 90 ngày |
| PC-14 | Thông báo in-app bền vững và push theo permission: sửa ý, chốt/cập nhật lịch, duyệt tiền, host sửa/đảo; không leak author | Push-only, hoặc sửa không báo |

Các quyền host không cấp quyền đọc tác giả ẩn danh hoặc DM. Giữ Moment / Placement / AlbumComment / Message riêng. Policy xem lịch sử độc lập ba chế độ đóng góp sau kết thúc; các chi tiết D-19 chưa hỏi vẫn là đề xuất. Mở lại phòng và xử lý dữ liệu local không tự tạo lại grant đã thu hồi.

## 2 Ranh giới quyền lịch sử

Mốc quan hệ phải do server ghi tại accept friendship hoặc commit membership, không lấy ngày client. Lưu policy snapshot riêng từng viewer/owner và từng lần quan hệ; đổi setting không retroactive. Hủy rồi kết bạn lại không phục hồi grant cũ, nhưng có thể cấp quyền mới từ lookback của lần mới. SELECTED_FRIENDS không được mở cho người ngoài danh sách chỉ vì họ nằm trong lookback.

Đề xuất chi tiết tích hợp cần backend review: dùng timestamp tạo nơi đăng làm thời điểm phân phối; LOOKBACK cho phép từ `relationshipStartedAt - 30*24h` trở đi (biên dưới inclusive), FROM_JOIN từ relationshipStartedAt. Phải kiểm lại relationship/membership hiện hành, target và quyền từng đích ở mỗi list/detail/count/cover/media/reference. Không dùng `now - 30d` để cắt ảnh đã cấp. Cách chia sẻ lại một Moment cũ vào nơi đăng mới cần được thể hiện rõ trên UI và contract, không tự tin EXIF hoặc raw media URL.

Owner library không chịu giới hạn lịch sử của người xem; quyền ảnh nhóm sau leave không được hồi sinh qua owner library hoặc DM. Block ngăn gửi DM mới nhưng không tự xóa text cũ; ROOM reference vẫn kiểm source permission. PERSONAL grant/reaction vẫn theo quyền riêng, không union tương tác vào ROOM.

## 3 Delta model/DTO/API đề xuất

Đây là logical contract cần adapter/version, không phải khẳng định server đã có route/field. Backend phải trả OpenAPI và error schema thật trước implement client.

| Nhóm | Dữ liệu/command cần bổ sung | Kiểm tại commit |
| --- | --- | --- |
| Identity/join | Account ID, join policy/request, membership ID/status/joinedAt, host-transfer acceptance | Không nhận guestId/tên/role client làm quyền; giữ API guest cũ cho tương thích |
| Photo history | Owner setting NO_HISTORY/LOOKBACK_30_DAYS; album setting FROM_JOIN/LOOKBACK_30_DAYS/FULL; relationship instance ID, policy snapshot, fixed historyFrom, server relationshipStartedAt, placement time | Viewer được chọn và quan hệ hiện hành; setting version áp lúc join/accept; revoke/re-friend; history predicate ở mọi projection |
| Planning | contentRevision, my vote 0/1/2 hoặc null, aggregate; shortlist revision, order ballot, plan version snapshot, completion/skip audit | Host/owner thật; lock content theo lịch chốt kể cả gọi API trực tiếp; edit chỉ reset phiếu ý đó; stale version conflict |
| Ledger | Payers/shares, effective revision và pending amendment, affected actors/approvals, original entry, immutable adjustment/reversal/audit, idempotency key | Sum payer = sum share = total VND integer; phân biệt host trước/sau paid; pending không đổi số dư; không tính hai lần khi retry |
| Former-member finance | Principal stable account và liên kết khoản/giao dịch của chính user, DTO giới hạn | Không dùng ACTIVE membership làm điều kiện duy nhất cho self finance; không trả khoản không liên quan, aggregate toàn phòng hoặc album |
| Close/history | Command đóng lưu lịch sử, retained reader entitlement, read-only reason; khác hiddenForMe và technical DELETED | Atomic kiểm balance zero, không pending tiền; chặn mutation mới sau close; giữ self-delete ảnh |
| Notifications | Type, resource ID, revision, recipient, read watermark, dedup key; push preference | Audience hiện hành, không leak author/DM/token, retry không nhân bản |

Approval cho sửa share và payer cần response nêu rõ ai còn phải duyệt; client không tự suy từ một boolean acknowledged. Concurrency giữa settlement và expense amendment phải kiểm lại trạng thái tại commit; không cho stale approval ghi đè revision mới. Các chi tiết còn mở ở biên bản là DECISION_REQUIRED cho ticket liên quan, không tự gắn VERIFIED_PASS.

## 4 Event và vận hành

Đề xuất event bổ sung: friendship.accepted/revoked, room.membership-approved/left/removed, photo.history-policy-updated, proposal.content-updated (content revision + vote invalidation), plan.version-published, plan.stage-skipped/restored, expense.amendment-requested/approved/rejected/applied, settlement.reversal-requested/applied, room.history-closed. Tên phải map với backend, không bắt buộc tự đổi stream hiện có.

Publish dữ liệu và outbox trong cùng commit hoặc cơ chế tương đương. Event quyền không thay authorization; worker/push chậm không được kéo dài quyền cũ. Chính sách snapshot giữ cho viewer hiện tại sau setting change; cập nhật setting phải nói rõ chỉ áp người mới. ROOM block không làm biến mất nội dung/count; PERSONAL và DM vẫn revoke theo block. Reconnect phải refetch ACL/version và dedup; không tin cache hoặc local clock.

Không tự dọn lịch sử theo lịch 90 ngày. Cần capacity/quota/backup/purge cho vận hành; quyết định không tự hết hạn bản đầu không hứa storage vô hạn, không cản owner xóa ảnh hoặc luồng xóa account hợp lệ. Không xóa ledger do ảnh/account/room UI delete.

## 5 AC bổ sung — chưa chạy

| AC | Tình huống | Kết quả bắt buộc |
| --- | --- | --- |
| PC-AC01 | Thêm/vote sau chốt khi ACTIVE | Thành công, giữ lịch/completion; không lộ actor phiếu |
| PC-AC02 | Host sửa ý người khác chưa chốt | Thành công, reset đúng phiếu ý đó, thông báo nội dung sửa không lộ author |
| PC-AC03 | Owner/host sửa nội dung ý đã chốt qua màn cũ hoặc API trực tiếp | Từ chối; tạo ý thay thế; đổi giờ/thứ tự phải qua version |
| PC-AC04 | Member bỏ qua chặng / host bỏ qua rồi hoàn tác | Member bị từ chối, host thành công, audit giữ |
| PC-AC05 | Kết bạn, mở lịch sử 30 ngày, sau 60 ngày đọc ảnh đã mở | Còn đọc được nếu quyền khác còn; ảnh trước lookback không được mở |
| PC-AC06 | Đổi setting rồi kết bạn mới, hủy/kết bạn lại | Bạn hiện tại giữ snapshot; lần quan hệ mới nhận policy mới; grant cũ không hồi sinh |
| PC-AC07 | Album FULL/FROM_JOIN/LOOKBACK rồi thời gian trôi | Đúng cutoff cố định của từng membership; setting mới không retroactive; defaults FULL |
| PC-AC08 | PERSONAL chọn riêng Bình; Chi kết bạn trong lookback | Chi không đọc bài chọn riêng Bình; album không leak ra PERSONAL |
| PC-AC09 | Hai người block nhưng cùng phòng | DM/PERSONAL bị chặn; ROOM ảnh/comment vẫn theo membership/history; tiền không thay |
| PC-AC10 | Camera roll + camera, sửa caption sau publish | Cả nguồn ảnh hợp lệ được dùng; PATCH pixel/caption bị từ chối |
| PC-AC11 | Multi-payer ghi hộ, một payer chưa duyệt | Chưa tính khoản vào số dư; retry approval không tính trùng |
| PC-AC12 | Creator/payer tăng share người khác; host sửa trước paid | Member phải được affected person hoặc host duyệt; host được sửa, audit/notice |
| PC-AC13 | Host sửa expense có settlement liên quan | Phải amendment và affected approvals; không ghi đè khoản gốc |
| PC-AC14 | Sender báo chuyển; sai người confirm; retry đúng receiver | Pending không đổi balance; sai actor từ chối; đúng actor tính một lần |
| PC-AC15 | Đảo settlement qua hai bên hoặc host | Hai đường đúng actor; host phải có reason/notice; giữ giao dịch gốc, balance đúng |
| PC-AC16 | Người rời đọc self finance, toàn ledger, album | Self finance liên quan được; toàn ledger/album bị từ chối; own media self-management còn |
| PC-AC17 | Close chạy đồng thời với expense/settlement pending | Serialize hoặc conflict; không close với công nợ/pending tiền |
| PC-AC18 | Đã close, đọc lịch sử, tạo nội dung, owner xóa ảnh | Đọc theo quyền giữ lại; không tạo mới; self-delete ảnh hợp lệ, ledger nguyên |
| PC-AC19 | Tắt push, mutation thành công, replay event | In-app vẫn có; không push; không nhân thông báo, không leak author |

Kiểm cross-user/cross-room, race/retry và payload thật phía server mới đủ chứng minh quyền. Tài liệu và prototype không phải runtime pass. Các AC cũ mâu thuẫn trong phạm vi trên phải dùng kết quả mới, không chạy đồng thời hai kỳ vọng trái nhau.
