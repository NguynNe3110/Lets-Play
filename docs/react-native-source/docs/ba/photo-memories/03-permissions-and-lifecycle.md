# Quyền truy cập và vòng đời

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Tài liệu này là nguồn chuẩn về quyền và trạng thái. Các mặc định chưa được xác nhận thuộc [08](08-decisions.md); đặc biệt D-05, D-06, D-14 và D-19 cần được review trước production. Không tái sử dụng cơ chế ẩn tên của đề xuất cho ảnh hoặc comment.

## 1 Bất biến nghiệp vụ

| Mã | Quy tắc |
| --- | --- |
| BR-01 | `userId` ổn định xác định tác giả; tên, room code và membership index không cấp quyền |
| BR-02 | Một phòng online có đúng một album logic; tạo phòng và album phải nhất quán, join không tạo album mới |
| BR-03 | Một Moment sở hữu đúng một MediaAsset và một owner; không chuyển owner trong V1 |
| BR-04 | Mỗi Moment tối đa một PERSONAL placement ACTIVE và một ROOM placement ACTIVE; có thể không còn placement ACTIVE sau gỡ |
| BR-05 | Mỗi lần publish/add placements là nguyên tử cho toàn bộ đích đã chọn; không có thành công một phần |
| BR-06 | Đọc Moment trực tiếp chỉ dành owner; người được chia sẻ đọc qua placement và luôn được kiểm quyền ngữ cảnh đó |
| BR-07 | PERSONAL audience là OWNER, ALL_FRIENDS_SNAPSHOT hoặc SELECTED_FRIENDS; không có PUBLIC |
| BR-08 | Tập người nhận PERSONAL được đóng băng lúc xác nhận snapshot/đăng; bạn mới không tự nhận ảnh cũ |
| BR-09 | Grant cá nhân cần vẫn là bạn hiện tại và không block; hủy kết bạn đánh dấu REVOKED vĩnh viễn cho grant cũ, kết bạn lại không phục hồi |
| BR-10 | ROOM audience là membership ACTIVE hiện tại, kể cả người join sau ảnh; room phải chưa DELETED |
| BR-11 | Chủ phòng có quyền điều hành album nhưng không sở hữu ảnh của người khác hoặc DM |
| BR-12 | Tương tác gắn placement, không gắn Moment dùng chung; album comment gắn album; DM gắn conversation |
| BR-13 | Owner được đọc/xóa media của mình từ thư viện dù đã rời nhóm; điều này không cấp quyền đọc comment/reaction nhóm |
| BR-14 | Một reaction hiện hành mỗi user mỗi placement; đổi thay loại cũ; DELETE của chính mình không cần album mở đóng góp |
| BR-15 | Comment có danh tính, text bất biến, chỉ xóa/gỡ; không tự chuyển comment thành DM hoặc ngược lại |
| BR-16 | Không cho lấy ảnh chỉ được chia sẻ để thêm vào album bằng API; chỉ owner tạo nơi đăng mới |
| BR-17 | Chụp trong phòng chọn sẵn album phòng, cá nhân tùy chọn; không tự chọn mọi phòng hoặc đăng ngầm |
| BR-18 | Media READY chỉ là file sẵn sàng; chỉ transaction publish thành công mới có Moment và fanout |
| BR-19 | Caption/pixel không sửa sau publish trong V1; đích mới cần thao tác xác nhận mới; không hồi sinh placement đã REMOVED |
| BR-20 | Lịch sử chi tiền và bình chọn không thay đổi khi chụp, comment, kết thúc hoặc xóa ảnh |
| BR-21 | Deadline và thứ tự commit lấy server UTC; capturedAt từ thiết bị chỉ là metadata tham khảo |
| BR-22 | Deadline đóng góp áp dụng theo `now < deadline`; bằng deadline là hết hạn; thời điểm bắt đầu upload không giữ chỗ |
| BR-23 | Chỉ HOST hiện tại được thay thiết lập sau kết thúc; cập nhật dùng revision và audit |
| BR-24 | VIEW_ONLY chặn đóng góp mới; OPEN cho tiếp tục; WINDOWED theo endedAt và số giờ host chọn; ARCHIVED luôn chỉ đọc |
| BR-25 | Khóa đóng góp chặn thêm ảnh, thêm comment, đặt/đổi reaction; vẫn cho tự xóa/bỏ reaction, gỡ ảnh, báo cáo và xóa toàn bộ ảnh của mình |
| BR-26 | Khi mất quyền, list/count/cover/thumbnail/detail/deep link/media đều phải áp ACL; không chỉ ẩn CTA trên mobile |
| BR-27 | Danh sách/số đếm tương tác chỉ gồm đối tượng người xem được phép thấy; không trả field ngữ cảnh khác rồi ẩn UI |
| BR-28 | DM chỉ có hai user khác nhau; một conversation mỗi cặp, điều kiện gửi ở mục 4; host không có đặc quyền |
| BR-29 | Tham chiếu ảnh trong DM không cấp quyền mới; mỗi lần đọc ảnh tham chiếu phải kiểm lại source placement |
| BR-30 | Xóa/gỡ source làm ảnh tham chiếu không khả dụng; không tự xóa đoạn text DM đã gửi |
| BR-31 | Idempotency ngăn double tap/retry tạo nội dung trùng; optimistic UI không được coi là server commit |
| BR-32 | Khi quyền đổi giữa validate và commit, mutation phải kiểm tra lại bằng transaction/khóa/version tương đương |
| BR-33 | User SUSPENDED không tạo nội dung/liên lạc mới; chính sách đọc và xóa dữ liệu qua recovery do identity/moderation cung cấp |
| BR-34 | User DELETED mất truy cập; Moment của họ bị xóa logic, nội dung của họ xử lý theo mục 7; sổ tiền không tự cascade |
| BR-35 | Block là quan hệ hai chiều trong authorize nếu một bên chặn; hiệu lực cụ thể tại mục 5, không xóa dữ liệu tự động |
| BR-36 | Ảnh đã có trên thiết bị của người khác ngoài app không được hứa thu hồi; quyền server và cache app vẫn bị thu hồi |
| BR-37 | Album tự có không có nghĩa mọi thành viên đã đồng ý chia sẻ cá nhân; group membership không tự tạo friendship |
| BR-38 | Nếu không xác minh được quyền, server từ chối cấp nội dung hoặc media; không mặc định cho xem vì đã cache membership |

## 2 Công thức đọc và ghi

Ký hiệu `u` là người yêu cầu, `m` là Moment, `p` là placement, `a` là album, `t` là server time tại điểm kiểm tra/commit. Tất cả request cần phiên hợp lệ, user hợp lệ và target còn tồn tại. Các quyền tự xóa tài khoản bị khóa do DEP-01/06 phải đi qua cơ chế recovery, không tự cấp bằng client.

```text
isBlocked(u, v) = tồn tại block u→v HOẶC v→u

canReadOwnedMoment(u, m) = m.PUBLISHED AND u = m.ownerId

canReadPersonal(u, p) =
  p.ACTIVE AND p.kind = PERSONAL AND m.PUBLISHED
  AND (
    u = m.ownerId
    OR (grant(p,u).ACTIVE AND grant.sharingEpoch = currentPair.sharingEpoch
        AND friendship(u,m.ownerId).ACCEPTED
        AND NOT isBlocked(u,m.ownerId))
  )

canReadRoomPlacement(u, p) =
  p.ACTIVE AND p.kind = ROOM AND m.PUBLISHED
  AND room(p).lifecycle != DELETED
  AND membership(u,room(p)).ACTIVE
  AND (u = m.ownerId OR NOT isBlocked(u,m.ownerId))

canReadAlbum(u, a) = room(a).lifecycle != DELETED
                     AND membership(u,room(a)).ACTIVE

canContributeAlbum(u, a, t) =
  canReadAlbum(u,a) AND effectiveWriteState(a,t) = OPEN

canAddRoomPlacement(u,m,a,t) =
  canReadOwnedMoment(u,m) AND canContributeAlbum(u,a,t)
  AND m không có ROOM placement ACTIVE

canWritePhotoInteraction(u,p,t) =
  canReadPlacement(u,p)
  AND (p.kind = PERSONAL OR canContributeAlbum(u,album(p),t))
```

PERSONAL không có public comment ở V1. `canWritePhotoInteraction` của PERSONAL chỉ áp reaction; DM dùng quy tắc riêng. Tác giả có thể reaction ảnh của mình; tự DM bị chặn. Với album comment cần `canContributeAlbum`; với comment/reaction người khác đã bị block, lọc nội dung và số đếm theo mục 5.

Quyền gỡ place của owner đi qua `ownerId`, không cần quyền đọc nhóm. Quyền host gỡ của người khác cần HOST ACTIVE và có quyền đọc target; moderator dùng quyền vận hành riêng có audit. `GET /moments/{id}` không cho người ngoài owner đọc chỉ vì họ biết ID hoặc có quyền một placement.

## 3 Ma trận quyền

“Thành viên” trong bảng là ACTIVE và không bị chặn với tác giả tương ứng. Mọi dấu Có vẫn cần trạng thái target tồn tại.

| Hành động | Owner còn trong nhóm | Owner đã rời nhóm | Thành viên khác | HOST | Bạn nhận PERSONAL ngoài nhóm |
| --- | --- | --- | --- | --- | --- |
| Xem media của owner qua thư viện | Có | Có | Không | Không | Không |
| Xem ảnh/comment của ROOM | Có theo membership | Không | Có | Có | Không |
| Xem PERSONAL được cấp | Có với ảnh mình | Có với ảnh mình | Chỉ nếu có grant | Chỉ nếu có grant | Có khi grant/quan hệ còn hiệu lực |
| Góp ảnh của mình vào album | Khi OPEN | Không | Khi OPEN | Khi OPEN | Không |
| Reaction ROOM, thêm comment | Khi OPEN | Không | Khi OPEN | Khi OPEN | Không |
| Xóa comment/bỏ reaction của chính mình | Có | Qua endpoint self-cleanup giới hạn | Có | Có như thành viên | Chỉ reaction PERSONAL của mình |
| Gỡ ROOM placement của ảnh mình | Có | Có qua thư viện | Không | Chỉ ảnh của chính host hoặc điều hành | Không |
| Gỡ ROOM placement của ảnh người khác | Không | Không | Không | Có, lý do và audit | Không |
| Xóa Moment toàn bộ | Có | Có | Không | Chỉ khi là owner | Không |
| Đổi policy sau kết thúc | Nếu là HOST | Không | Không | Có khi ACTIVE/ENDED | Không |
| Xem DM giữa hai người khác | Không | Không | Không | Không | Không |

Endpoint tự dọn sau mất membership không được trả lại text comment, ảnh, danh sách người khác hoặc metadata album. Nó chỉ nhận ID của nội dung do chính user tạo và thực hiện DELETE idempotent. Không bổ sung list lịch sử album để hỗ trợ thao tác này.

## 4 Điều kiện gửi tin nhắn riêng

Đủ cả: user gửi/nhận ACTIVE, khác nhau, không bị block; cặp đang là bạn bè ACCEPTED **hoặc** có ít nhất một phòng chưa DELETED mà cả hai có membership ACTIVE. Phòng có thể ACTIVE, ENDED hoặc ARCHIVED: khóa album không tự khóa liên lạc riêng. Điều này là lựa chọn D-13 cần review.

Nếu có `referencePlacementId`, cả hai phải đọc được đúng placement tại commit. Ảnh thuộc ROOM và tác giả đã rời phòng không đủ điều kiện tham chiếu ROOM, dù họ còn sở hữu Moment. Người xem có thể gửi text thường nếu còn quan hệ liên lạc, không được chuyển tham chiếu sang Moment hoặc PERSONAL khác để qua quyền.

Mỗi bên đọc lịch sử text của chính conversation khi còn phiên hợp lệ; block/hủy kết bạn/mất phòng chung khóa gửi mới nhưng không tự xóa text cũ. Read watermark chỉ thay đổi khi user đọc thread, không suy ra đã đọc từ push đã nhận. Thu hồi tin của mình bất kỳ lúc nào trong V1, tạo tombstone; không sửa text.

## 5 Quy tắc chặn và quyền theo người xem

Baseline D-14: block ngăn gửi DM hai chiều, thu hồi grant PERSONAL cũ hai chiều và ẩn nội dung đối phương trong album chung. Không tự xóa nội dung khỏi album với người thứ ba. Bỏ chặn không phục hồi grant cá nhân; ảnh nhóm/comment hiện lại nếu membership và target vẫn hợp lệ. Không tự phục hồi friendship đã bị hủy; DEP-03 phải xác định hai trạng thái độc lập.

Hủy kết bạn hoặc tạo block phải tăng `sharingEpoch` của cặp ngay trong ranh giới commit quyền. Grant và audience snapshot giữ epoch đã cấp; epoch không reset khi kết bạn lại/bỏ chặn. Như vậy hủy rồi kết bạn lại rất nhanh cũng không phục hồi grant cũ trong lúc worker chưa đánh dấu REVOKED. Worker chỉ dọn/đánh dấu vật lý và gửi event, không là cơ chế duy nhất chặn đọc. Snapshot có epoch cũ bị AUDIENCE_CHANGED ngay cả khi hai người lại đang là bạn ở thời điểm publish.

Trong album, ảnh do đối phương tạo không được trả về; cover phải chọn ảnh khác có quyền. Comment và reaction do đối phương tạo được ẩn, kể cả trên ảnh của người thứ ba hoặc comment album. Count và danh sách actor phải lọc giống nhau. Vì vậy `visiblePhotoCount`, `visibleCommentCount` và `visibleReactionCounts` có thể khác giữa hai thành viên. Không trả total gốc, tên actor ẩn hoặc vị trí trống gợi ra nội dung bị ẩn. Không áp block vào lịch sử công nợ hoặc bỏ tên khỏi danh sách thành viên; UI giải thích người dùng vẫn có thể cùng một phòng.

Baseline tương tác: người có quyền đọc placement được xem reaction hiện có tại placement đó, kể cả PERSONAL; chỉ loại actor bị xóa hoặc bị block với viewer. Reaction/comment đã tạo vẫn tồn tại khi actor rời nhóm/hủy kết bạn, trừ khi tự xóa hoặc account deletion. Vì danh sách reaction PERSONAL có thể cho biết một số người cũng được chia sẻ ảnh, đây là chi tiết D-08 cần review; không đồng nghĩa API được trả toàn bộ audience của tác giả.

Ảnh của chính user vẫn đọc từ thư viện. Chủ phòng bị một người chặn không được lấy cớ quản trị để đọc nội dung đang ẩn; xử lý report qua moderator nếu cần. Block không hứa giấu việc hai người đang cùng nhóm, cũng không hứa xóa bản chụp/tải ngoài app.

## 6 Vòng đời phòng và thiết lập sau kết thúc

### 6.1 Hai trường khác nhau

`planningPhase`: `ideas / vote / order / plan` là giá trị kế thừa để đọc dữ liệu/client cũ; theo [yêu cầu mới](../continuous-planning.md), không khóa góp ý hoặc vote khi phòng ACTIVE. Lịch đã công bố và shortlist dự kiến có version riêng. `room.lifecycle`: `ACTIVE / ENDED / ARCHIVED / DELETED`, không ghi đè enum Phase hiện có âm thầm. `ARCHIVED` chung khác `hiddenForMe` và cờ local `SavedRoom.archived`.

| Từ | Đến | Ai và điều kiện baseline | Tác động album |
| --- | --- | --- | --- |
| ACTIVE | ENDED | HOST, có lịch đã công bố, xác nhận kèm policy đang lưu | Ghi endedAt; áp postEndPolicy |
| ENDED | ACTIVE | HOST xác nhận mở lại | Xóa endedAt hiện hành, lưu history; album OPEN |
| ACTIVE/ENDED | ARCHIVED | HOST xác nhận lưu trữ chung | Ghi previousLifecycle và previousEndedAt; luôn READ_ONLY |
| ARCHIVED | previousLifecycle | HOST khôi phục | Khôi phục endedAt cũ; **không tính lại hạn** |
| ACTIVE/ENDED/ARCHIVED | DELETED | HOST qua luồng xóa phòng chung | Album/ROOM placement không còn truy cập; ảnh owner giữ theo mục 7 |

Không tự chuyển ENDED vì đánh dấu hết chặng, qua giờ đi chơi hoặc đã trả hết tiền. Không cho PATCH arbitrary lifecycle từ client; DEP-02 cần command có kiểm quyền, revision và audit. Đặt lại endedAt trong lần kết thúc mới chỉ qua mở lại rồi kết thúc, có ghi nhận hậu quả đối với thời hạn.

### 6.2 Policy và trạng thái hiệu lực

```text
postEndPolicy.mode = VIEW_ONLY | OPEN | WINDOWED
postEndPolicy.windowHours = null nếu VIEW_ONLY hoặc OPEN
postEndPolicy.windowHours = integer [1,720] nếu WINDOWED

contributionDeadlineAt = endedAt + windowHours * 3600 giây
                         chỉ khi lifecycle ENDED và mode WINDOWED

ACTIVE                              => OPEN
ENDED + OPEN                        => OPEN
ENDED + VIEW_ONLY                   => READ_ONLY / POST_END_VIEW_ONLY
ENDED + WINDOWED + t < deadline     => OPEN
ENDED + WINDOWED + t >= deadline    => READ_ONLY / POST_END_WINDOW_EXPIRED
ARCHIVED                            => READ_ONLY / ROOM_ARCHIVED
DELETED                             => UNAVAILABLE / ROOM_DELETED
```

Mặc định đề xuất OPEN; host có thể thiết lập từ trước kết thúc. `effectiveWriteState` là kết quả tính động, không phải cờ do app tự lưu. Cấu hình WINDOWED tồn tại khi ACTIVE nhưng chưa có deadline. Khi ARCHIVED có thể trả policy và endedAt lịch sử cho người có quyền đọc, nhưng `contributionDeadlineAt` hiện hành null; không cho đổi policy trong trạng thái này. Sau restore tính lại từ endedAt cũ.

Host được tăng/giảm thời hạn hoặc chuyển mode khi ENDED. Rút hạn đã qua khóa ngay; tăng hạn đủ để mở lại thì cho đóng góp lại. Transaction policy và publish phải serialize theo album/room revision; không dùng kiểm tra cache vài phút trước. Job phát event hết hạn có thể chậm nhưng việc từ chối ghi tại deadline không được chậm theo job.

### 6.3 Các trạng thái còn lại

| Đối tượng | Trạng thái và chuyển tiếp | Hiển thị |
| --- | --- | --- |
| Nháp local | DRAFT → UPLOADING → PROCESSING → PUBLISHING → PUBLISHED; có FAILED/CANCELLED để retry/bỏ | Chỉ người tạo; không tính số ảnh server |
| MediaAsset | UPLOADING → PROCESSING → READY; UPLOADING/PROCESSING → FAILED; READY chưa gắn → EXPIRED; media gắn ảnh bị xóa → PURGE_PENDING → PURGED | Media không READY không được publish |
| Moment | PUBLISHED → DELETED hoặc HIDDEN_BY_MODERATION; hidden có thể moderator khôi phục nếu chưa xóa | DELETED không restore V1 |
| Placement | ACTIVE → REMOVED; không restore | Tạo lại là ID mới, không kéo theo tương tác cũ |
| Comment | ACTIVE → DELETED_BY_AUTHOR hoặc REMOVED_BY_HOST/MODERATOR | List bỏ text, có tombstone tại ID đã biết |
| Message | SENT → RECALLED; delivered/read là watermark, không đổi text | Tombstone giữ sequence để watermark không lệch |

Moderation khôi phục Moment chỉ khôi phục các placement vẫn ACTIVE, không phục hồi thứ owner/host đã gỡ hoặc owner đã xóa. Không để thao tác moderator thắng yêu cầu xóa hợp lệ của owner.

## 7 Ma trận tác động khi xóa và mất quyền

| Sự kiện | Moment/media | PERSONAL | ROOM và album comment | DM tham chiếu / text |
| --- | --- | --- | --- | --- |
| Owner gỡ PERSONAL | Giữ thư viện | Placement REMOVED, grant và tương tác không đọc được | Giữ nguyên | Reference PERSONAL không khả dụng; text giữ |
| Owner hoặc host gỡ ROOM | Giữ thư viện và PERSONAL | Giữ nguyên | Placement REMOVED, comment/reaction ảnh mất truy cập; comment album độc lập giữ | Reference ROOM không khả dụng; text giữ |
| Owner xóa Moment | DELETED, thu hồi mọi đường media, chờ purge | Mọi placement mất truy cập | Mọi placement của ảnh mất truy cập; comment album không tự xóa | Ảnh không khả dụng ở mọi reference; text giữ |
| Thành viên LEFT/REMOVED | Ảnh đã góp giữ, owner vẫn quản lý | Quyền cá nhân xét độc lập | Người đó mất album; nhóm giữ ảnh/comment còn hợp lệ | Reference ROOM của người đó mất quyền; text giữ |
| Thành viên mới ACTIVE | Không đổi | Không có grant tự động | Được lịch sử còn hợp lệ theo D-05 | Không được đọc conversation người khác |
| Hủy kết bạn | Không đổi | Thu hồi grant cũ hai chiều; không phục hồi khi kết bạn lại | Nếu còn membership, quyền nhóm không đổi | Gửi mới chỉ khi còn điều kiện khác; reference PERSONAL mất quyền |
| Block | Không xóa media | Thu hồi grant cũ hai chiều | Nội dung hai người ẩn đối với nhau, người thứ ba vẫn xem theo ACL | Không gửi mới, reference đối phương bị ẩn; text cũ giữ |
| Xóa phòng online | Giữ Moment/media trong thư viện owner | Giữ nguyên | Album và ROOM placement mất truy cập; album/ảnh comment mất truy cập | Reference ROOM không khả dụng; text giữ |
| Xóa tài khoản | Moment của user xóa logic, chờ purge | Thu hồi toàn bộ grant liên quan | Comment do user viết xóa nội dung, reaction xóa; không xóa ảnh owner khác | Tin do user gửi chuyển RECALLED, text/reference null ngay khi xóa logic, reason nội bộ ACCOUNT_DELETED; tin bên kia giữ; peer hiển thị “Tài khoản đã xóa”, avatar null |

Xóa tài khoản không cascade xóa khoản chi, thanh toán hoặc bằng chứng tài chính của nhóm; đó là chính sách của module tiền, cần xử lý danh tính hiển thị riêng. Bản sao tối thiểu phục vụ report/audit phải có quyền nội bộ và TTL riêng, không dùng làm bản công khai sau khi owner xóa.

## 8 Tránh rò dữ liệu qua các API phụ

Người ngoài không được nhận cover, số ảnh, contributor list hoặc thống kê của album chỉ vì có mã/link. PERSONAL DTO không chứa `roomId`, `albumId`, tên phòng, membership, comment nhóm, tất cả người nhận hoặc số placement toàn cục. ROOM DTO không chứa danh sách người nhận PERSONAL hoặc reaction của PERSONAL. Chỉ owner management DTO được liệt kê đích do mình tạo.

Một user được cả PERSONAL và ROOM của cùng ảnh vẫn phải truy cập hai ngữ cảnh riêng nếu muốn tương tác; không union comment/count. Feed PERSONAL không gộp ảnh ROOM để tránh biến quyền nhóm thành quyền cá nhân. Owner library deduplicate theo Moment nhưng không tự cho owner đã rời phòng xem group comment.

Read object không có quyền trả 404 `RESOURCE_NOT_AVAILABLE`, không phân biệt “không tồn tại” với “có nhưng của nhóm khác”. Mutation trên album còn quyền đọc nhưng bị đóng trả 409 `ALBUM_READ_ONLY` hoặc `ALBUM_WINDOW_EXPIRED`. Thao tác self-cleanup vẫn DELETE được tài nguyên do mình tạo, không trả payload cũ. Event/push cũng phải kiểm audience hiện tại và không đính kèm ảnh hoặc text nhạy cảm để vượt ACL.
