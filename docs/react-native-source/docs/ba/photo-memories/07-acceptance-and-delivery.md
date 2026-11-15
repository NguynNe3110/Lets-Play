# Nghiệm thu truy vết và bàn giao

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Delta UI feed/composer và các case riêng được ghi tại [09-ui-feed-and-capture-composer.md](09-ui-feed-and-capture-composer.md). Các case dưới đây vẫn là đặc tả chưa thực thi.

Đây là **đặc tả kiểm thử chưa thực thi**, không phải báo cáo test của tính năng đã viết. Các case dùng baseline và các quyết định đã xác nhận tại [08](08-decisions.md). Product, backend, mobile và QA review cùng một bộ để phát hiện khác cách hiểu trước implement.

## 1 Fixture nghiệp vụ chuẩn

- An `usr_an` là HOST phòng Đà Lạt `room_dalat`, timezone Asia/Ho_Chi_Minh; Bình và Chi là MEMBER ACTIVE; Dũng chưa tham gia. Không có block ban đầu.
- An và Bình là bạn ACCEPTED; An và Dũng là bạn ACCEPTED; Chi chưa là bạn An. Tất cả user ACTIVE, ID là ví dụ.
- M1 là Moment của An, PERSONAL P1 SELECTED_FRIENDS cho Bình và ROOM P2 trong album Đà Lạt. Không gửi cho Dũng.
- Phòng ACTIVE, planningPhase plan; album OPEN, revision 1. Khi kết thúc trong case thời hạn: endedAt `2026-11-10T13:00:00.000Z` tương ứng 20:00 GMT+7; WINDOWED 72 giờ cho deadline `2026-11-13T13:00:00.000Z`.
- Mỗi test độc lập reset fixture, trừ khi ghi rõ là bước tiếp theo. Dùng ít nhất hai phiên/tài khoản hoặc hai thiết bị cho đồng bộ, không mô phỏng tất cả bằng đổi local role.

## 2 Chụp và đăng

| Mã | Given | When | Then |
| --- | --- | --- | --- |
| AC-01 | App chưa được cấp quyền camera | An mở Chụp rồi từ chối | Không crash; có hướng dẫn; không xin microphone; không có asset/post |
| AC-02 | Từ phòng ACTIVE | An chụp và mở preview | Album Đà Lạt chọn sẵn, cá nhân tắt; thấy rõ tên nơi đăng, bỏ chọn được |
| AC-03 | Từ camera cá nhân, đang ở nhiều phòng | An chụp | Không tự chọn phòng nào; không tự gửi cho mọi bạn; PERSONAL bật thì default OWNER |
| AC-04 | Preview có crop/rotation | An chụp lại rồi đăng | Chỉ ảnh sau cùng đúng chiều/crop vào asset; không có post từ ảnh bị thay |
| AC-05 | Không có đích nào | Bấm Đăng hoặc gọi API destinations rỗng | FE chặn; BE trả 422; không tạo Moment |
| AC-06 | Snapshot gồm Bình, Dũng; cả hai vẫn là bạn | An đăng cả PERSONAL và ROOM | Một Moment, hai placement, asset không nhân đôi, đúng hai grant, owner library một ô |
| AC-07 | ROOM đã khóa, PERSONAL hợp lệ | Publish cả hai | Toàn bộ không commit, không grant/notification PERSONAL; nháp/media READY giữ; không tự chọn lại đích |
| AC-08 | POST publish commit nhưng response bị mất | Retry cùng key/payload, rồi tra mutation | Cùng momentId và placement IDs, một outbox effect mỗi đích, không thêm quota publish lần retry |
| AC-09 | Key đã dùng với caption A | Gửi lại key đó với caption B/room khác | 409 IDEMPOTENCY_KEY_REUSED, không đổi post cũ |
| AC-10 | Asset READY đã gắn M1 | Publish media đó với mutation mới | 409 MEDIA_ALREADY_PUBLISHED, không tạo bản trùng |
| AC-11 | ALL snapshot gồm Bình | Dũng được kết bạn sau snapshot rồi An publish | Chỉ Bình nhận, Dũng không tự nhận; nếu user muốn Dũng phải xác nhận snapshot mới |
| AC-12 | Snapshot gồm Bình | Bình hủy kết bạn trước publish commit | 409 AUDIENCE_CHANGED, không publish bất kỳ đích nào |
| AC-13 | Không có bạn ACCEPTED | Chọn ALL; sau đó chọn OWNER hoặc ROOM | ALL báo AUDIENCE_EMPTY; OWNER/ROOM hợp lệ không bị ép kết bạn |
| AC-14 | File sai MIME/checksum, >10 MiB hoặc dimension không hợp lệ | Upload/complete | Bị từ chối đúng code hoặc asset FAILED; không READY/publish; không phát thumbnail riêng tư công khai |
| AC-15 | Upload complete bị gọi lặp | Hai request complete cùng asset | Một job xử lý có hiệu lực; kết quả cùng asset; không nhận file ghi đè sau complete |
| AC-16 | Offline sau chụp | Đóng/mở app rồi có mạng | Nháp gắn user cũ còn trong 24 giờ; chưa có post; tái xác nhận đích/quyền trước publish |
| AC-17 | Có nháp chưa đăng | Logout rồi login user khác | Nháp và cache user trước bị xóa, không đăng ảnh dưới owner mới |

### 2.1 Delta UI feed và composer

| Mã | Given | When | Then |
| --- | --- | --- | --- |
| UI-AC-01 | Người dùng có quyền mở album có ảnh của nhiều thành viên | Mở album và cuộn gần cuối trang | Ảnh của mình và người khác được hiển thị theo đúng ACL; trang tiếp theo được tải bằng cursor, không lặp `placementId` |
| UI-AC-02 | Feed trả `nextCursor=null` | Cuộn tiếp nhiều lần | Không gọi thêm request; không hiện spinner vô hạn hoặc item trùng |
| UI-AC-03 | Request trang tiếp theo lỗi mạng | Cuộn đến cuối | Item đã xem vẫn giữ; có nút thử lại đúng cursor; không mất vị trí hoặc reset sai về trang đầu |
| UI-AC-04 | Người dùng mất quyền một ảnh sau khi trang đầu đã tải | Refetch/nhận event rồi cuộn hoặc mở ảnh | Ảnh bị bỏ/đánh dấu không khả dụng và media không còn render từ cache; các ảnh còn quyền vẫn xem được |
| UI-AC-05 | Người dùng mở camera từ phòng ACTIVE | Chụp xong | Composer mở ngay, hiển thị ảnh preview, album phòng đã chọn sẵn, title/caption và thao tác hủy/chụp lại/đăng |
| UI-AC-06 | Người dùng đang ở composer | Bấm Hủy hoặc Chụp lại | Không có upload/ publish; ảnh cũ không tạo Moment; chụp lại thay đúng nháp hiện tại |
| UI-AC-07 | Composer có title/caption và chọn PERSONAL/ROOM | Bấm Đăng | Upload/processing/publish hiển thị theo trạng thái; chỉ báo thành công sau khi tất cả đích commit nguyên tử |
| UI-AC-08 | Publish timeout sau khi server có thể đã commit | Retry hoặc mở lại app | Tra/retry cùng mutation key trả cùng receipt, không tạo Moment/placement/notification trùng |
| UI-AC-09 | Album bị khóa hoặc audience đổi trước commit | Bấm Đăng | Không đăng một phần; giữ nháp/media READY, nêu đích lỗi và yêu cầu chọn lại khi cần |
| UI-AC-10 | Có nhiều ảnh được phép xem | Vuốt chậm/nhanh một lần trong feed | Một viewport có một ảnh; chuyển tối đa một ảnh và snap đúng trang; trang cuối không tạo trang trắng hay lặp ảnh |
| UI-AC-11 | Đang xem ảnh thứ hai hoặc sau đó | Đổi kích thước/orientation | Giữ ID ảnh đang xem, tính lại kích thước trang; nội dung/tương tác không bị thanh điều hướng che |
| UI-AC-12 | Vừa chụp hoặc chọn ảnh | Mở composer theo ảnh tham chiếu mới | Có “Gửi đến...”, “Thêm một tin nhắn” trên ảnh, X / máy bay gửi / Aa và hàng avatar; không có form chú thích/nơi lưu dưới ảnh; X không lưu/upload |
| UI-AC-13 | Có danh sách bạn bè hợp lệ từ adapter | Chọn nhiều avatar, đổi sang Tất cả và ngược lại | Dùng stable recipient IDs; hai kiểu chọn loại trừ; kèo độc lập; server tiếp tục authorize tại commit |
| UI-AC-14 | Chưa có danh sách bạn bè/publish adapter hoặc gửi lỗi | Mở composer, thử gửi/lưu riêng | Không thêm bạn giả hay dùng members[]; gửi bị khóa khi thiếu adapter, lỗi giữ nháp; nút tải xuống chỉ lưu riêng và không báo đã gửi |

## 3 Phân quyền và tách ngữ cảnh

| Mã | Given | When | Then |
| --- | --- | --- | --- |
| AC-18 | M1/P1/P2 fixture | Bình xem feed cá nhân rồi album nhóm | P1 không có roomId/tên nhóm/comment nhóm; P2 không có người nhận hoặc reaction P1 |
| AC-19 | Chi không được share PERSONAL nhưng là MEMBER | Chi gọi P1 rồi P2 | P1 404; P2 đọc được; không tự cấp PERSONAL vì chung nhóm |
| AC-20 | Dũng là bạn An nhưng không grant P1 và chưa join | Gọi detail/list/media/cover/count của album bằng ID biết trước | 404 chung, không thumbnail/count/tên nhóm hoặc event nhóm |
| AC-21 | Dũng có link mời nhưng chưa join | Mở deep link ảnh nhóm | Không xem ảnh cho đến membership ACTIVE được server chấp nhận |
| AC-22 | Dũng join thành công sau khi M1 đã đăng | Mở album và feed cá nhân | Xem P2 lịch sử còn hợp lệ; vẫn không có P1; không tự kết bạn Chi hoặc nhận notification cũ |
| AC-23 | An rời phòng sau transfer host | An mở thư viện và gọi P2/comments | Xem/xóa được M1 qua owner path; không đọc P2/comment/reaction; nhóm vẫn có ảnh nếu An chưa xóa |
| AC-24 | Host biết ID Moment của member | Host gọi DELETE /moments/{id} | 404; host chỉ được gỡ ROOM qua place và reason; PERSONAL/thư viện tác giả giữ |
| AC-25 | Bình hủy kết bạn An nhưng vẫn ACTIVE Đà Lạt | Bình mở P1/P2 rồi kết bạn lại ngay trước khi worker revoke chạy | P1/media cá nhân bị thu hồi bằng sharingEpoch; P2 còn theo nhóm; không có cửa sổ phục hồi grant cũ |
| AC-26 | An chặn Bình; cả hai chung phòng với Chi | Ba người tải album, cover, comments/count | An/Bình không thấy nội dung của nhau, counts/cover khớp; Chi vẫn thấy nội dung hợp lệ của cả hai |
| AC-27 | Client đã cache media URL | User bị remove, sau đó gọi URL cũ/request mới và chuyển offline | Request mới bị chặn sau commit; view/cache cũ hết quyền trong 60 giây; không sử dụng cache làm quyền vĩnh viễn |
| AC-28 | Client sửa local role/currentUserIndex hoặc đoán room code | Gọi policy API/góp ảnh nhóm | Server chỉ tin token/membership; không tăng quyền |
| AC-29 | Một ảnh có cả PERSONAL và ROOM, user xem được cả hai | Chọn reaction ở P1 và comment P2 | Reaction P1 không sang P2; comment P2 không sang P1; library vẫn một ảnh |
| AC-30 | Phòng có đề xuất ẩn tên và sổ tiền | Đăng ảnh/comment, kết thúc hoặc xóa ảnh | Không trả mapping tác giả ý tưởng; không sửa amount, shares, transfer hoặc số dư |

## 4 Ba chế độ album và thời hạn

| Mã | Given | When | Then |
| --- | --- | --- | --- |
| AC-31 | Host đặt VIEW_ONLY trước kết thúc; phòng ACTIVE | Thành viên góp ảnh rồi host kết thúc | Trước end được góp; sau commit end chỉ đọc; ảnh/comment cũ vẫn xem |
| AC-32 | Host đặt OPEN, room ENDED | Thành viên thêm ảnh/comment/reaction sau nhiều ngày | Vẫn được khi membership/quota hợp lệ; không tự đóng theo mốc 90 ngày cũ |
| AC-33 | WINDOWED 72h và endedAt như fixture | Publish lúc `2026-11-13T12:59:59.999Z` | Được nếu transaction commit trước deadline; deadline trả đúng `13:00:00.000Z` |
| AC-34 | Cùng fixture | Publish đúng `2026-11-13T13:00:00.000Z` hoặc sau | 409 ALBUM_WINDOW_EXPIRED, không partial commit |
| AC-35 | Upload bắt đầu trước hạn | READY và publish sau hạn | Không giữ chỗ do upload; nháp còn để chọn đích khác qua ý định mới |
| AC-36 | Member Chi có ID album/revision | PATCH policy | 403 HOST_REQUIRED, policy/revision không đổi |
| AC-37 | Hai host session cùng đọc revision 1, policy hiện tại OPEN | Session A lưu WINDOWED 72h, B lưu VIEW_ONLY với revision cũ | A đổi policy và tăng revision; B 412, phải refetch/review, không overwrite |
| AC-38 | ENDED + OPEN đã 5 ngày | Host đổi WINDOWED 72h | Deadline tính từ endedAt cũ và đã qua; khóa ngay, không mở 72h mới từ lúc sửa |
| AC-39 | ENDED đã hết cửa sổ 72h | Host tăng WINDOWED lên 168h, now còn trong hạn mới | Album mở lại, event/capabilities cập nhật; không sinh lại ảnh/comment cũ đã xóa |
| AC-40 | ENDED WINDOWED 72h đã qua hạn, sau đó ARCHIVED | Host restore | Khôi phục ENDED với endedAt cũ; vẫn hết hạn, không reset đồng hồ |
| AC-41 | Phòng ARCHIVED dù mode OPEN | Góp ảnh/comment, đổi reaction; rồi tự xóa comment/bỏ reaction/xóa ảnh | Đóng góp mới bị chặn; thao tác tự dọn/xóa hợp lệ vẫn được; policy edit chặn |
| AC-42 | Job hết hạn và realtime tạm dừng | Server đến deadline, client timer còn lệch | Mutation vẫn từ chối chính xác; GET effectiveWriteState đúng; không cần job đóng cờ |
| AC-43 | Album policy/view đang cache, revision không đổi khi thời gian qua | GET tại sau deadline với ETag cũ | Trả trạng thái động READ_ONLY; không dùng 304/revision tĩnh để giữ OPEN |
| AC-44 | WINDOWED input 0, 721, 1.5 hoặc OPEN có windowHours=72 | Save | 422 đúng field; không âm thầm clamp/đổi unit |
| AC-45 | Host mở lại ENDED rồi kết thúc lần nữa | Xác nhận luồng đầy đủ | endedAt mới do server, hạn tính lại, có audit và cảnh báo trước thao tác |
| AC-46 | Change policy/remove membership chạy đồng thời publish | Lặp race có kiểm soát commit order | Publish chỉ thắng khi authorize đúng tại điểm commit; không có write sau transaction khóa/thu hồi đã thắng |

## 5 Tương tác và tin nhắn

| Mã | Given | When | Then |
| --- | --- | --- | --- |
| AC-47 | Bình đặt HEART P2 | PUT FIRE rồi DELETE, có retry cùng key | Tối đa một reaction hiện hành; counts cuối đúng, không đếm các lần retry |
| AC-48 | Có comment text hợp lệ | Retry create cùng clientMutationId sau response mất | Một comment ID/text, không hai notification |
| AC-49 | Caption 280/281, comment 1.000/1.001 và DM 2.000/2.001 code points sau NFC | Gửi input tiếng Việt/emoji; thêm HTML script literal | Biên hợp lệ nhận, vượt trả 422; render plain text không chạy HTML; whitespace-only bị chặn |
| AC-50 | P1 PERSONAL hợp lệ | Gọi comments P1 | 422 COMMENTS_NOT_SUPPORTED; trả lời riêng không bị gán thành comment |
| AC-51 | Chi bình luận album, Bình bình luận P2 | Đọc hai thread | Comment chung chỉ ở album; comment ảnh chỉ ở P2; delete ảnh không xóa comment album |
| AC-52 | Bình có quyền P2 và được nhắn An | Chọn Trả lời riêng trên P2, gửi text | Một conversation cặp, message có reference P2; chỉ hai bên thấy; không event/notification nhóm |
| AC-53 | Chi chưa là bạn An nhưng chung phòng và xem P2 | Gửi DM reference P2 | Được theo D-13; nếu bỏ chung phòng và không friendship thì gửi mới bị chặn |
| AC-54 | Hai thiết bị đồng thời mở hội thoại An/Bình | Ensure conversation rồi gửi cùng mutation | Một conversation, một message, một sequence; unread peer tăng một |
| AC-55 | An rời nhóm hoặc P2 bị gỡ sau khi DM đã gửi | Bình/An mở lại thread | Text còn; reference ROOM unavailable với viewer mất quyền, không chuyển sang owner media path |
| AC-56 | P2 source còn cho Bình nhưng tác giả An đã rời | Bình cố gửi DM mới tham chiếu P2 cho An | 409 REFERENCE_NOT_AVAILABLE; không tự gửi text không reference |
| AC-57 | An block Bình | Bình gửi, host truy cập thread, rồi hai bên xem text cũ | Gửi bị chặn; host 404; hai participant giữ lịch sử text, reference bị ẩn theo ACL |
| AC-58 | lastReadSequence=40, có message tới 42 | PUT read 42 rồi request cũ read 41 từ thiết bị khác | Watermark vẫn 42, unread không tăng lại; sender tự gửi không tạo unread cho mình |
| AC-59 | Tin chưa đọc do peer gửi | Peer recall, có retry recall | Tombstone không text/reference, unread giảm đúng một; sequence không bị đánh lại |
| AC-60 | Message tham chiếu album đã chỉ đọc | User đủ điều kiện DM gửi theo ảnh còn đọc được | DM vẫn được; policy chỉ khóa đóng góp album, không khóa liên lạc riêng |

## 6 Xóa đồng bộ và vận hành

| Mã | Given | When | Then |
| --- | --- | --- | --- |
| AC-61 | M1 có P1 và P2 | Owner gỡ P2, sau đó thêm vào album lại | P1 và owner media còn; P2 cũ/reference/comment mất; place mới ID mới, không phục hồi tương tác |
| AC-62 | M1 có hai nơi đăng và DM reference | Owner xóa M1 rồi retry | Hai nơi không đọc được, DM image unavailable, text giữ, một purge job hiệu lực; retry owner 204 |
| AC-63 | Room có ảnh nhiều owner và PERSONAL khác | Host xóa phòng online | Album/ROOM không đọc; Moment/PERSONAL giữ trong thư viện owner; không xóa sổ tiền ngoài chính sách module |
| AC-64 | User yêu cầu xóa account | Hoàn tất account deletion | Ảnh user mất mọi quyền, comment/reaction xử lý như 03; không hồi sinh từ backup; không xóa số dư người khác |
| AC-65 | Album có >100 ảnh, có thêm/xóa/block giữa các page | Phân trang, giữ filter/cursor | Trang ≤limit, không lộ item/count bị block, không offset skip; ảnh mới lấy qua refresh; cursor sai bị reject |
| AC-66 | Cùng command publish hai nơi, Bình ở cả hai | Fanout/retry worker | Bình nhận một thông báo ảnh mới, ưu tiên PERSONAL link; không tên phòng trong push cá nhân |
| AC-67 | Rời nhóm hoặc block sau commit trước push | Worker chuẩn bị gửi | Reauthorize, suppress notification không còn quyền; push không có ảnh/text/URL để vượt quyền |
| AC-68 | Event duplicate/out of order hoặc cursor quá hạn | Reconnect | Dedup eventId, refetch state mới; expired cursor full sync; không bật OPEN từ event cũ |
| AC-69 | Người dùng tắt quyền push của OS | Đăng ảnh, gửi/đọc DM | Tính năng vẫn hoạt động, unread in-app đúng; không ép bật push |
| AC-70 | User report comment/DM | Gửi report lặp và host hỏi danh tính người report | Dedup report mở; queue nội bộ có record; host không đọc DM/reporter |
| AC-71 | DB commit nhưng outbox worker crash | Restart worker và retry | Post vẫn có một bản; event gửi bù, không duplicate notification hoặc số liệu publish |
| AC-72 | Purge storage lỗi và backup được restore | Retry cleanup/recovery | Không phục vụ ảnh đã xóa trong lúc chờ purge; deletion ledger chặn hồi sinh; có cảnh báo purge lag |
| AC-73 | Chạm nhanh nhiều lần hoặc user vượt quota | Gửi write/delete | Double tap cùng key không thêm nội dung/quota; rate limit có Retry-After; xóa/recall không bị quota tạo nội dung chặn |
| AC-74 | Nhiều phòng, một ảnh của người khác đang xem được | Thử thêm ảnh đó bằng momentId vào phòng mình | Không được vì không owner; không dùng quyền đọc thay quyền đăng lại |
| AC-75 | Chủ sở hữu còn media trong thư viện nhưng không còn place ACTIVE | Mở thư viện, thêm nơi đăng hợp lệ hoặc xóa | Moment vẫn quản lý được; place mới tương tác rỗng; không auto-purge vì chỉ gỡ nơi cuối |

## 7 Truy vết yêu cầu và kiểm thử

| FR | UC / quy tắc trọng tâm | AC bắt buộc |
| --- | --- | --- |
| FR-01, FR-02 | UC-01; BR-17 | AC-01–04 |
| FR-03, FR-04 | UC-01; BR-07–09,17 | AC-02–06,11–13 |
| FR-05, FR-06 | UC-02; BR-05,18,31–32 | AC-06–10,14–15,35,46,71 |
| FR-07, FR-08 | UC-03; BR-06–10,26–27 | AC-18–29,65,75 |
| FR-09, FR-10 | UC-04; BR-02–04,16 | AC-21–23,61,74–75 |
| FR-11 | UC-05; BR-12,14,27 | AC-29,41,47 |
| FR-12, FR-13 | UC-05; BR-12,15 | AC-48–51 |
| FR-14, FR-15 | UC-06; BR-28–30 | AC-52–60 |
| FR-16 | UC-08; BR-11,13,19,25 | AC-23–24,41,61–64,75 |
| FR-17 | UC-07; BR-21–24,32 | AC-31–46 |
| FR-18 | UC-09; BR-08–10,34–35,37 | AC-20–28,53,55–57,63–64 |
| FR-19 | 06 thông báo | AC-54,58–59,66–69,71 |
| FR-20 | UC-10; BR-11,35 | AC-26–27,57,70 |
| FR-21, FR-22 | UC-02,03; BR-26,31,38 | AC-08,16–17,27,43,65,68,71 |
| FR-23 | UC-07; BR-24–25 | AC-31–46,60 |
| FR-24 | BR-20,37 | AC-18,24,28–30,63–64 |

Các dải AC trong bảng là tham chiếu case, không phải kết quả pass. NFR-01 đến NFR-12 cần kế hoạch kỹ thuật tại 06, nhất là concurrency, tải và media gateway; unit test pure function không đủ chứng minh quyền thật hoặc media revoke.

## 8 Thứ tự triển khai có thể giao đội backend

| Gói | Đầu ra review được | Phụ thuộc và tiêu chí hoàn thành |
| --- | --- | --- |
| W-01 | Quyết định và contract review | Chốt D-04–19 còn mở; giữ C-06–08; thống nhất auth/room/friend/block và retention |
| W-02 | Identity/membership adapters và room lifecycle | DEP-01–03; stable IDs, leave/remove/end/archive có event/revision thật; không fake local role |
| W-03 | Media private pipeline | Upload/complete/READY, variants, EXIF removal, orphan cleanup, quyền media gateway, quota |
| W-04 | Moment, placements và audience | Publish atomic/idempotent, grants, library/feed, add/remove/delete, outbox |
| W-05 | Album và policy | Album một/phòng, ba chế độ, deadline, list/filter/cover/count có ACL, host settings |
| W-06 | Tương tác | Reaction, photo comment, album comment, moderation và self-cleanup |
| W-07 | DM giới hạn | Unique pair, message sequence/reference ACL, read/delivered/recall, unread |
| W-08 | Đồng bộ và notification | Event catch-up, foreground realtime/polling, fanout dedup, settings và device logout |
| W-09 | Mobile flows và QA tích hợp | Hai tài khoản/thiết bị, fault/race, image lease, regression tiền/phiếu; metric và runbook |

W là chia gói theo đầu ra, không phải ước lượng sprint hoặc cam kết có thể bỏ W-07 khỏi bản đầu. Không implement online bằng cách đẩy nguyên JSON rooms.v1 của thiết bị lên server và tin các field role/owner.

## 9 Điều kiện một ticket sẵn sàng và hoàn tất

Ticket sẵn sàng khi có FR/BR/AC liên quan, quyết định áp dụng, request/response và error cụ thể, actor/ACL, state trước/sau và side effect. Nếu thay đặc tả, cập nhật 08 và mọi nơi chịu ảnh hưởng trước khi báo contract đã ổn định.

Ticket hoàn tất khi code giữ contract đã thống nhất, migration/schema phù hợp, negative permission được kiểm, retry/race liên quan được kiểm, không leak payload/caches, event có dedup và có bằng chứng test thực tế. Màn hiển thị được bằng mock data không chứng minh backend đã xử lý quyền hay upload. Giữ dữ liệu local hiện có đọc được; thay đổi schema cũ phải có chiến lược migration riêng.

## 10 Kiểm tra bộ tài liệu trong lượt bàn giao

Đầu ra là các file Markdown và skill trong repo, không có code tính năng. Trước bàn giao kiểm đường dẫn nội bộ, mã FR/BR/UC/AC, trạng thái xác nhận D-01–03, tính nhất quán ba mode, các endpoint dùng trong DTO/event và frontmatter skill. Kết quả thực tế của việc kiểm tra tài liệu được ghi ở [verification](verification.md), tách với các AC tính năng chưa chạy.
