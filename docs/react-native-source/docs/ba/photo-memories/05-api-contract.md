# Hợp đồng API cho backend và mobile

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Base path đề xuất `/v1`. Chưa có các endpoint này trong repo. Model/enum/DTO tại [04](04-domain-data.md), ACL và state machine tại [03](03-permissions-and-lifecycle.md) là một phần của hợp đồng. Endpoint phụ thuộc tại mục 12 cần được team sở hữu chốt, không được ghi nhận đã có backend.

Cập nhật từ Swagger người dùng cung cấp: backend ngoài repo dùng `/api` và đã có account/room/tiền/SSE. `/v1` trong bộ này là prefix minh họa của hợp đồng ảnh mục tiêu, **không yêu cầu đổi route hiện có hoặc xây lại auth**. Dùng [mapping tương thích](../../integration/api-compatibility-map.md) để chốt prefix/version/adapter, giữ những API đang được client sử dụng.

## 1 Quy ước toàn bộ API

HTTPS và `Authorization: Bearer <access-token>` cho mọi endpoint trừ PUT storage có quyền tạm riêng. Server suy ra actor từ token. JSON UTF-8; `Content-Type: application/json`. PATCH policy và POST thêm placements yêu cầu `If-Match: "<revision>"`; revision được GET trước đó từ đúng entity. Mutation API yêu cầu `Idempotency-Key` là UUID, trừ PUT upload storage dùng session riêng. Khi có `clientMutationId` trong body thì phải bằng header này.

JSON cá nhân hóa dùng `Cache-Control: private, no-store`. ETag trong hợp đồng là token để If-Match khi ghi, không phải quyền trả 304 bỏ qua ACL hoặc trạng thái tính động. Các GET chứa capabilities/deadline/count luôn trả kết quả authorize và tính mới; không giữ OPEN bằng album revision cũ sau deadline. Media cache dùng cơ chế riêng tại 06.

```json
{
  "data": {"id": "example_id"},
  "meta": {"requestId": "req_example", "serverTime": "2026-11-10T13:00:00.000Z"}
}
```

List trả `data: {items: [], nextCursor: null}`; metadata như trên. `204` không có body. Lỗi không dùng HTTP 200:

```json
{
  "error": {
    "code": "ALBUM_WINDOW_EXPIRED",
    "message": "Album đã hết thời gian đóng góp",
    "details": {
      "albumId": "alb_01",
      "contributionDeadlineAt": "2026-11-13T13:00:00.000Z",
      "retryable": false
    }
  },
  "meta": {"requestId": "req_example", "serverTime": "2026-11-13T13:00:00.001Z"}
}
```

Chỉ trả details về album khi actor còn quyền đọc. Trường hợp không có quyền trả 404 chung, không đưa membership/recipient/roomName vào lỗi. Validation `details.fields` là array `{path,code,message}`, dùng JSON path như `destinations.personal.audienceSnapshotId`; không echo token hay text nhạy cảm. Mobile map code thành thông điệp, không parse message.

## 2 Idempotency và concurrency

Scope key là `(actorId, operationName, Idempotency-Key)`, có fingerprint của path parameter và JSON canonical. Một ý định và payload giữ một key; timeout retry cùng key, user đổi đích/file/text tạo key mới. Chênh payload với key cũ trả 409 `IDEMPOTENCY_KEY_REUSED`. Key đang xử lý trả 409 `REQUEST_IN_PROGRESS`, `Retry-After: 2`. Nếu đã commit, replay trả receipt cùng resource ID, không chạy effect/fanout lần nữa.

Receipt/idempotency cache tối thiểu 24 giờ; Moment/comment/message có unique actor/clientMutationId bền theo vòng đời nội dung và lưu fingerprint để retry muộn không nhân bản. Các create khác sau TTL phải refetch trước tạo command mới; không tự coi hết cache là thất bại. Replay vẫn cần phiên hợp lệ và authorize truy cập receipt; receipt không chứa ảnh/text/người nhận của ngữ cảnh đã mất quyền.

Write trả thành công khi transaction commit. Timeout 5xx không có nghĩa chưa commit. Có endpoint tra mutation mục 6. Đổi policy dùng album revision; add placements dùng Moment revision. Thiếu If-Match trả 428 `PRECONDITION_REQUIRED`; sai trả 412 `VERSION_CONFLICT` kèm currentRevision khi còn quyền. Quyền và deadline luôn được kiểm lại tại commit, dù revision client đúng.

Đặt/đổi/bỏ reaction áp “lần commit mới nhất thắng” giữa các ý định khác key; retry cùng key không chạy lại để ghi đè ý định mới. Mobile serialize thao tác trên cùng target hoặc refetch state cuối; không dùng timestamp điện thoại quyết định thắng. Read watermark dùng max nên request cũ không làm unread tăng trở lại.

## 3 Phân trang và bộ lọc

| Loại list | Default / max limit | Thứ tự |
| --- | --- | --- |
| Ảnh, album ảnh, thư viện, inbox | 20 / 100 | Mới trước cũ sau theo 04; inbox updatedAt/id DESC |
| Comment, reaction actor | 20 / 100 | Comment createdAt/id ASC; reaction userId ASC |
| Message history | 50 / 100 | sequence DESC; UI render ngược lại |

`cursor` opaque do server ký, gắn actor, endpoint, filter, limit và mốc đọc. Query page sau phải giữ filter và limit, cursor không hợp lệ/khác filter/hết hạn 24 giờ trả 400 `INVALID_CURSOR`. Pagination là keyset, không offset. List ảnh/comment dùng creation watermark nội bộ của trang đầu để nội dung tạo sau đó không chen vào page sau, kể cả cùng millisecond; message dùng lastSequence tại trang đầu. Refresh mới lấy phần mới. ACL luôn xét hiện tại, không đóng băng quyền bằng cursor. Khi có xóa/block, page có thể ngắn, `nextCursor` dựa vị trí quét hợp lệ tiếp theo; không lặp ID hoặc bỏ qua phần còn thấy được chỉ vì lọc quyền sau LIMIT.

Inbox là list có updatedAt thay đổi: khi nhận message/recall/read event làm thứ tự đổi, mobile reset cursor và refetch từ đầu, dedup theo conversationId. Không hứa snapshot vị trí inbox ổn định trong lúc hội thoại được cập nhật. Read state/actor count vẫn phản ánh hiện tại trên mọi list.

Query tháng: `month=2026-11`; ảnh album dùng room timezone; thư viện có `timezone=Asia%2FHo_Chi_Minh` (default timezone profile, fallback Asia/Ho_Chi_Minh). Không nhận ngày/tháng sai. `authorId` chỉ áp album/feed và vẫn chịu ACL. Không có API xem contributor/album count từ link mời chưa join.

## 4 Upload ảnh

### POST /media/upload-sessions

Request:

```json
{
  "source": "CAMERA",
  "contentType": "image/jpeg",
  "byteSize": 842193,
  "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
  "width": 1600,
  "height": 1600,
  "capturedAt": "2026-11-10T12:59:00.000Z",
  "clientTimezone": "Asia/Ho_Chi_Minh"
}
```

Các field ngoài capturedAt/clientTimezone bắt buộc. JPEG vuông, 320–2.048 px, ≤10 MiB; server vẫn decode kiểm tra. `201 data`: `{mediaAssetId,uploadUrl,method:"PUT",requiredHeaders,uploadExpiresAt}`. URL chỉ để upload đúng asset/user và không phải public read URL. FE dùng **chính xác** requiredHeaders, không tự mang bearer token app sang storage. Lỗi 413 file lớn, 415 format, 422 dimension/metadata, 429 quota.

### PUT uploadUrl

Body binary, không phải base64 JSON; requiredHeaders từ session. Storage trả 2xx nghĩa bytes đã nhận, chưa publish. Lỗi hết hạn phải xin session mới; không tiếp tục PUT vào URL đã complete. Session có hạn 15 phút, complete cũng phải bắt đầu trước uploadExpiresAt. Checksum verify sau nhận đủ bytes. URL không được ghi vào log/analytics hoặc share ra ngoài.

### POST /media-assets/{mediaAssetId}/complete

Owner, body `{}`. Server xác nhận đối tượng upload đã hiện hữu đầy đủ rồi chuyển PROCESSING; worker kiểm size/checksum/decode trước READY. Thiếu upload trả 409 `UPLOAD_INCOMPLETE`. `202 data: {mediaAssetId,status:"PROCESSING"}`; nếu đã READY, `200` cùng asset/status; không chạy job lần nữa. FAILED trả 409 `MEDIA_PROCESSING_FAILED`, expired trả 410 `MEDIA_EXPIRED`, không hồi sinh. Không gọi complete thay cho PUT chưa xong.

### GET /media-assets/{mediaAssetId}

Chỉ owner. `200 data`: `{id,status,readyAt,expiresAt,failureCode,width,height}`; các field chưa có null. FE poll 1, 2, 3 rồi 5 giây/lần, dừng khi READY/FAILED/EXPIRED hoặc sau 120 giây processing; khi hết thời gian client refetch một lần và báo xử lý chậm, không tự publish file chưa READY. Server timeout chuyển FAILED qua job, có status để lần mở sau đọc được.

### DELETE /media-assets/{mediaAssetId}

Owner hủy asset chưa gắn Moment; `204`, trạng thái EXPIRED/PURGE_PENDING tùy storage. Không hủy được asset đã publish: 409 `MEDIA_IN_USE`, phải dùng xóa Moment. Hủy xử lý và publish cạnh tranh phải chỉ một bên commit. Upload session/key mới cần tạo sau asset đã FAILED/EXPIRED.

## 5 Chọn người nhận cá nhân

### POST /photo-audience-snapshots

Body `{mode:"ALL_FRIENDS_SNAPSHOT"}` hoặc `{mode:"SELECTED_FRIENDS",recipientUserIds:["usr_binh"]}`. Đây là preview riêng của owner, không làm người nhận thấy ảnh. Không tạo snapshot cho OWNER. Validate 1–200 bạn được phép, không trùng/không self; ALL không âm thầm giới hạn 200.

`201 data`: `{id,mode,recipientCount,recipients:UserRef[],createdAt,expiresAt}`. Tập recipients bất biến; tồn tại 10 phút. Nếu quan hệ đổi, snapshot không tự thêm người mới; commit dùng snapshot kiểm lại mọi recipient. Đã tiêu thụ chỉ retry đúng command cũ, không dùng lại cho ảnh khác.

Lỗi: `AUDIENCE_EMPTY`, `AUDIENCE_LIMIT_EXCEEDED`, `INVALID_RECIPIENT` (422), snapshot mất hiệu lực tại publish là `AUDIENCE_SNAPSHOT_EXPIRED`/`AUDIENCE_CHANGED` (409). Error không tiết lộ user không được tra cứu.

## 6 Đăng và quản lý khoảnh khắc

### POST /moments

```json
{
  "clientMutationId": "11111111-1111-4111-8111-111111111111",
  "mediaAssetId": "med_01",
  "caption": "Cả nhóm tới nơi rồi",
  "destinations": {
    "personal": {"audienceMode": "SELECTED_FRIENDS", "audienceSnapshotId": "aud_01"},
    "room": {"roomId": "room_dalat"}
  }
}
```

`caption` optional default `""`. Trong destinations có `personal`, `room` hoặc cả hai; không chấp nhận object rỗng, array phòng, duplicate hoặc null thay object. Personal OWNER phải **không gửi** audienceSnapshotId; hai mode còn lại bắt buộc snapshot cùng mode/owner. ROOM chỉ nhận roomId, server tìm album đúng phòng. Không gửi tự chọn albumId khác roomId.

`201 data` receipt:

```json
{
  "momentId": "mom_01",
  "revision": 1,
  "publishedAt": "2026-11-10T13:00:00.000Z",
  "placements": [
    {"id": "pl_personal_01", "kind": "PERSONAL"},
    {"id": "pl_room_01", "kind": "ROOM"}
  ]
}
```

Không có trạng thái “personal success, room failed”. Bất kỳ đích không hợp lệ thì không tạo Moment/placement/grant, snapshot chưa bị tiêu thụ và asset READY giữ đến expiresAt. Command thất bại vẫn không cho đổi payload bằng key cũ; tạo ý định/key mới sau người dùng xác nhận lại.

Lỗi thêm: `MEDIA_NOT_READY`, `MEDIA_ALREADY_PUBLISHED`, `ALBUM_READ_ONLY`, `ALBUM_WINDOW_EXPIRED`, `AUDIENCE_CHANGED` (409), `RESOURCE_NOT_AVAILABLE` (404), `MEDIA_EXPIRED` (410). Sender không có membership không được nhận tên phòng trong details.

### GET /me/photo-mutations/{clientMutationId}

Tra các create nội dung: Moment, add placements, photo/album comment, message. `200 data`: `{operation,status:"IN_PROGRESS|COMMITTED|REJECTED",resourceIds:[],errorCode:null|code}`. Chỉ actor sở hữu operation; không trả text/media. Operation chưa được server nhận trả 404 `MUTATION_NOT_FOUND`, chỉ có nghĩa chưa tìm thấy; client vẫn retry **cùng key**, không tạo ID mới. Các create đã commit còn unique content thì phải tra được trong vòng đời dữ liệu kể cả idempotency cache hết 24 giờ. REJECTED không giữ chỗ cho post; thay payload vẫn cần key mới.

### POST /moments/{momentId}/placements

Owner, `If-Match` Moment revision. Body `{clientMutationId,destinations}` theo schema POST /moments, không có media/caption. Thêm scope chưa có ACTIVE; không sửa place cũ. `201 data`: `{momentId,revision,placements:[{id,kind}]}`. Có scope đang ACTIVE trả 409 `PLACEMENT_ALREADY_EXISTS`; không tạo nửa còn lại. ROOM phải OPEN theo policy, owner có membership; PERSONAL cần audience mới. Gỡ rồi thêm lại tạo place mới với tương tác rỗng.

### DELETE /placements/{placementId}

Body không bắt buộc với owner; host cần `{reason:"Nội dung không phù hợp với album"}` 1–500 code points. Owner hoặc HOST đang có quyền đọc nhóm; chỉ remove place, không xóa media. `204`; retry owner trên place đã remove vẫn `204`. Không phải owner/host trả 404. Nếu client/HTTP stack không hỗ trợ DELETE body cho host, backend phải thống nhất route command tương đương trước triển khai; không bỏ trường reason.

### DELETE /moments/{momentId}

Chỉ owner; `204` sau xóa logic, thu hồi mọi nơi và ghi outbox; purge media async. Owner không cần membership. Retry owner trên tombstone trả `204`. Moment của người khác trả 404, HOST không có ngoại lệ. Đây là xóa mọi nơi, không được map vào CTA Gỡ khỏi album.

## 7 API đọc ảnh và album

| Endpoint | Params | Kết quả data | Quyền và lưu ý |
| --- | --- | --- | --- |
| GET /me/moments | cursor, limit, month, timezone | List MomentSummary | Chỉ ảnh owner; group-only cũng có; không trùng khi hai nơi đăng |
| GET /moments/{id} | Không | OwnedMomentDetail | Chỉ owner; sau rời vẫn thấy đích tối thiểu của chính ảnh mình |
| GET /photo-feed | cursor, limit, authorId optional | List PlacementView PERSONAL | Grant + friendship hiện tại + không block; có thể gồm PERSONAL của mình |
| GET /users/{userId}/photo-timeline | cursor, limit | List PlacementView PERSONAL | Lọc cùng ACL; biết userId không đủ để xem |
| GET /placements/{id} | Không | PlacementView | Quyền đúng placement, không chuyển scope vì cùng ảnh |
| GET /rooms/{roomId}/album | Không | AlbumView, `ETag` album revision | ACTIVE membership; kể cả chỉ đọc/rỗng |
| GET /albums/{albumId}/photos | cursor, limit, authorId, month | List PlacementView ROOM | ACL + lọc block; tháng theo placement.createdAt và room.timezone |
| GET /albums/{albumId}/contributors | cursor, limit | List UserRef | Tác giả có ít nhất một ảnh còn nhìn thấy; gồm người đã rời nhưng còn ảnh, loại blocked/deleted; sort userId ASC, default 20/max 100 |

Feed có PERSONAL của mình theo audience OWNER cũng chỉ mình thấy, không đẩy cho bạn bè. Owner muốn thư viện đầy đủ dùng /me/moments. Nếu không có quyền đọc một dòng cá nhân của user đang biết qua directory, API trả list rỗng cho ảnh chưa được cấp; nếu user ID không tồn tại/không tra cứu được trả 404. Không trả tổng số ảnh cá nhân thật.

### GET /placements/{id}/media?variant=thumbnail|display

Kiểm quyền placement tại request; `200` binary hoặc chuyển tiếp tới đường media bảo vệ có giới hạn tại 06. Không cấp public storage URL. `thumbnail` cạnh 320 px, `display` tối đa 2.048 px; không cung cấp original có EXIF. Người mất quyền trả 404, kể cả khi biết đường dẫn từ DM.

### GET /moments/{id}/media?variant=thumbnail|display

Chỉ owner của Moment PUBLISHED; dùng thư viện khi đã rời nhóm. Không dùng endpoint này cho viewer/DM tham chiếu. `media.thumbnailPath/displayPath` trong DTO owner trỏ route này; các path trong PlacementView trỏ route placement. Mọi path đều cần auth, không phải quyền xem vĩnh viễn.

## 8 Thiết lập album và kết thúc kèo

### PATCH /albums/{albumId}/post-end-policy

HOST ACTIVE, phòng ACTIVE/ENDED, `If-Match` album revision.

```json
{"mode": "WINDOWED", "windowHours": 72}
```

VIEW_ONLY/OPEN phải gửi `windowHours:null`; WINDOWED nguyên 1–720. Request không được gửi deadline/endedAt. `200 data`: AlbumView và ETag mới, bao gồm serverTime và deadline tính lại. Nếu host chuyển sang cùng policy không đổi thì trả `200` cùng revision, không phát event thay đổi giả. Lỗi 403 `HOST_REQUIRED` chỉ khi actor là thành viên có quyền biết album; 409 `ROOM_ARCHIVED`; 412 version.

Các command kết thúc/mở lại/lưu trữ/khôi phục/xóa phòng thuộc DEP-02. Contract tối thiểu ở mục 12, phải serialize với đóng góp và phát event tương ứng. Khóa album không được thực hiện bằng cách gán `phase=plan` hoặc cờ local archived.

## 9 Reaction và bình luận

| Endpoint | Body / query | Kết quả | Quy tắc bổ sung |
| --- | --- | --- | --- |
| PUT /placements/{id}/my-reaction | `{type:"HEART"}` | 200 `{type,revision,updatedAt}` | PLACE còn đọc được; ROOM phải OPEN; upsert một loại |
| DELETE /placements/{id}/my-reaction | Không | 204 | Tự bỏ, cho cleanup sau mất parent access bằng ownership của reaction |
| GET /placements/{id}/reactions | cursor, limit, type optional | List `{user:UserRef,type}` | Actor/count lọc block; không trả row không thấy |
| POST /placements/{id}/comments | `{clientMutationId,text}` | 201 `{id,createdAt,state}` | Chỉ ROOM, OPEN; PERSONAL trả 422 `COMMENTS_NOT_SUPPORTED` nếu còn đọc được place |
| GET /placements/{id}/comments | cursor, limit | List CommentView | Chỉ ROOM; READ_ONLY vẫn đọc; không người ngoài |
| DELETE /photo-comments/{id} | Owner không body; host `{reason}` | 204 | Author cleanup không cần parent đọc; host cần quyền và audit |
| POST /albums/{id}/comments | `{clientMutationId,text}` | 201 `{id,createdAt,state}` | Album OPEN, membership ACTIVE |
| GET /albums/{id}/comments | cursor, limit | List CommentView | Thread chung, không kéo comment ảnh |
| DELETE /album-comments/{id} | Owner không body; host `{reason}` | 204 | Cùng phân quyền xóa photo comment |

Không có PATCH comment/text V1. GET một comment tombstone nếu cần deep link: `/photo-comments/{id}` hoặc `/album-comments/{id}` trả CommentView/tombstone khi parent vẫn đọc được, ngược lại 404. Parent REMOVED không được dùng tombstone để đoán nội dung.

## 10 Hội thoại riêng

### POST /conversations/direct

Body `{peerUserId}`. Kiểm điều kiện liên lạc tại 03, không có text nên không tạo message/unread. `201 data:{id,peer:UserRef}` nếu tạo mới, `200` cùng ID nếu đã có. Unique pair ngăn hai thiết bị tạo hai conversation. Self trả 422 `SELF_CONVERSATION_NOT_ALLOWED`; quan hệ không đủ trả 403 `MESSAGING_NOT_ALLOWED` khi peer được phép tra cứu, khác thì 404.

### POST /conversations/{id}/messages

```json
{
  "clientMutationId": "22222222-2222-4222-8222-222222222222",
  "text": "Góc chụp đẹp quá!",
  "referencePlacementId": "pl_room_01"
}
```

Reference optional, không gửi null nếu không có. `201 data:{id,conversationId,sequence,createdAt,state:"SENT"}`. Transaction kiểm participant, quan hệ hiện tại, block và quyền **cả hai** với source nếu có. 403 `MESSAGING_NOT_ALLOWED`, 409 `REFERENCE_NOT_AVAILABLE`; không tự bỏ reference và gửi text. Khi user chọn gửi lại không reference phải có ý định/key mới.

| Endpoint | Params/body | Kết quả và nghĩa |
| --- | --- | --- |
| GET /conversations | cursor, limit | List ConversationView của chính user, newest updatedAt trước |
| GET /conversations/{id}/messages | cursor, limit | List MessageView; participant được đọc text cũ dù quan hệ đã mất; reference tính quyền theo viewer hiện tại |
| PUT /conversations/{id}/read-watermark | `{lastReadSequence:42}` | 200 `{lastReadSequence,unreadCount}`; max cũ/mới; vượt lastSequence trả 422 |
| PUT /conversations/{id}/delivered-watermark | `{lastDeliveredSequence:42}` | 200 watermark thực tế; chỉ khi app đã nhận message, không phải push delivery |
| POST /messages/{id}/recall | `{}` | 200 `{id,state:"RECALLED"}`; chỉ sender, không giới hạn thời gian baseline; vẫn cho recall khi block |
| GET /me/unread-summary | Không | 200 `{conversationUnreadTotal,notificationUnreadTotal}`; tổng số message chưa đọc, không phải số thread |

Watermark không tiết lộ conversation không thuộc user. Tin sender tự gửi không tăng unread của chính sender. Recall message trước watermark chưa đọc làm giảm unread bên kia; read event sync mọi thiết bị. Tin reference bị mất quyền không được xóa text tự động hoặc lộ thumbnail cache. Không có endpoint HOST xem inbox nhóm.

## 11 Báo cáo và lỗi chuẩn

### POST /content-reports

Body `{targetType,targetId,reason,description?}` theo 04. `201 data:{id,status:"OPEN",createdAt}`. Một reporter/target/reason đang OPEN hoặc IN_REVIEW thì trả `200` report cũ thay tạo spam; reason khác tạo record khác theo hạn mức. Chỉ đọc target hiện tại mới report; nếu nội dung đã biến mất thì 404. Việc chặn user là command DEP-03 riêng, không tự xảy ra vì report.

| HTTP | Code | FE xử lý |
| --- | --- | --- |
| 400 | INVALID_CURSOR, INVALID_JSON | Sửa request/reset pagination, không retry vô hạn |
| 401 | AUTHENTICATION_REQUIRED, SESSION_EXPIRED | Refresh phiên tối đa một lần theo auth; giữ nháp, không đăng dưới tài khoản khác |
| 403 | HOST_REQUIRED, MESSAGING_NOT_ALLOWED, ACCOUNT_RESTRICTED | Khóa hành động, refetch quyền; không xin mở rộng quyền bằng client |
| 404 | RESOURCE_NOT_AVAILABLE, MUTATION_NOT_FOUND | Target không khả dụng; mutation-not-found xử lý như mục 6 |
| 409 | ALBUM_READ_ONLY, ALBUM_WINDOW_EXPIRED, ROOM_ARCHIVED, ALBUM_CAPACITY_REACHED | Giữ nháp, cập nhật trạng thái; chỉ retry khi user đổi đích/policy hoặc capacity thật sự hợp lệ |
| 409 | AUDIENCE_CHANGED, AUDIENCE_SNAPSHOT_EXPIRED, AUDIENCE_SNAPSHOT_CONSUMED | Xin snapshot mới, yêu cầu xác nhận lại trước publish |
| 409 | MEDIA_NOT_READY, MEDIA_ALREADY_PUBLISHED, MEDIA_IN_USE, MEDIA_PROCESSING_FAILED, UPLOAD_INCOMPLETE, PLACEMENT_ALREADY_EXISTS, REFERENCE_NOT_AVAILABLE | Refetch target; không tạo bản trùng hoặc tự bỏ đích/reference |
| 409 | IDEMPOTENCY_KEY_REUSED, REQUEST_IN_PROGRESS | Không đổi payload bằng key cũ; request đang chạy retry cùng key theo Retry-After |
| 410 | MEDIA_EXPIRED, UPLOAD_EXPIRED | Xin upload session mới nếu người dùng vẫn muốn đăng |
| 412 / 428 | VERSION_CONFLICT / PRECONDITION_REQUIRED | Refetch và cho người dùng review lại thay đổi; không overwrite policy âm thầm |
| 413 / 415 | MEDIA_TOO_LARGE / UNSUPPORTED_MEDIA_TYPE | Encode lại/chụp lại, không retry bytes cũ |
| 422 | VALIDATION_ERROR, INVALID_RECIPIENT, AUDIENCE_EMPTY, AUDIENCE_LIMIT_EXCEEDED, INVALID_IMAGE, COMMENTS_NOT_SUPPORTED, SELF_CONVERSATION_NOT_ALLOWED | Hiển thị lỗi đúng field; không retry khi chưa đổi input |
| 429 | RATE_LIMITED, STORAGE_QUOTA_EXCEEDED | Tôn trọng Retry-After; quota là thông báo riêng có cách giải quyết |
| 500 / 503 | INTERNAL_ERROR / SERVICE_UNAVAILABLE | Backoff có giới hạn, tra mutation trước coi chưa đăng |

Process thất bại trả failureCode trong GET asset; không dùng code HTTP upload làm bằng chứng bytes đã publish. Reason string của host 1–500 code points; report description 0–2.000. Mọi endpoint không nhận field ngoài schema.

API event catch-up, notifications và preferences được định nghĩa tại [06 mục 2 và 3](06-events-notifications-nfr.md), áp cùng auth/envelope/idempotency, có ngoại lệ limit/cursor được ghi rõ ở đó.

## 12 Giao kèo với các service phụ thuộc

Delta mới về proposal/vote/shortlist/plan version, expected revision, event và AC nằm tại [kế hoạch liên tục](../continuous-planning.md). Không khóa góp ý hoặc vote theo phase cũ; kết thúc phòng yêu cầu có lịch đã công bố. Đây là hợp đồng đích cần backend xác nhận, chưa đổi endpoint hiện có.

Không đặc tả trọn login, mời nhóm hay danh bạ ở đây. Các route sau là shape để FE/BE xác nhận hoặc map vào API hiện có của hệ thống tương lai.

| Command/query đề xuất | Input tối thiểu | Output/effect bắt buộc cho module ảnh |
| --- | --- | --- |
| GET /me | Token | id, status, displayName, timezone; danh tính không đổi theo thiết bị |
| GET /me/friends | cursor,limit | Friends ACCEPTED và UserRef; không lấy danh sách tên local |
| POST /rooms/{id}/join | invitation token hợp lệ | Membership ACTIVE duy nhất; không cần tình bạn với host |
| POST /rooms/{id}/end | If-Match room revision, `{}` | ENDED, endedAt server; policy hiệu lực và room/album event |
| POST /rooms/{id}/reopen | If-Match room revision, `{}` | ACTIVE; archive history endedAt; xác nhận hạn kết thúc lần sau tính lại |
| POST /rooms/{id}/archive | If-Match room revision, `{}` | ARCHIVED chung, giữ previousLifecycle/endedAt |
| POST /rooms/{id}/restore | If-Match room revision, `{}` | Khôi phục lifecycle và endedAt cũ, không reset deadline |
| DELETE /rooms/{id} | HOST, xác nhận cấp UI, Idempotency-Key | DELETED, thu hồi album; không xóa Moment owner khác |
| POST /rooms/{id}/leave; remove-member command | User/host theo room rules | Thu hồi membership và quyền media; host leave yêu cầu transfer |
| DELETE friendship; POST/DELETE block | ID cặp, auth | Current state + event có revision; revoke personal grants, invalidate cache |
| Device register/preferences | Device token và user session | Push không gửi nhầm sau logout/chuyển tài khoản |

Mỗi command thực thi auth và idempotency theo service sở hữu, không chỉ tin UI. Album creation cùng lúc room creation cần transaction hoặc event consumer idempotent với unique roomId; trước khi album sẵn sàng không trả album không phân quyền hoặc tạo trùng khi join.
