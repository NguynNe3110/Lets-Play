# Kế hoạch triển khai

[Mục lục](README.md) · [Mapping](01-source-mapping.md) · [Kiến trúc](02-kotlin-architecture.md) · [Nghiệm thu và quyết định](04-validation-and-decisions.md)

**Trạng thái:** kế hoạch, chưa thực thi P1 trở đi. Tên file/package ở đây là mục tiêu đề xuất. Mỗi ticket làm thành thay đổi có thể review và kiểm riêng; không cần dồn cả migration vào một PR.

## 1. Thứ tự và phụ thuộc

~~~mermaid
flowchart LR
    P0["P0 · tài liệu / mapping"] --> P1["P1 · baseline / nền tảng"]
    P1 --> P2["P2 · domain / storage"]
    P2 --> P3["P3 · online hiện có"]
    P2 --> P4["P4 · local tương đương"]
    P3 --> P5["P5 · camera / ảnh local"]
    P4 --> P5
    P3 --> P6["P6 · contract nghiệp vụ mới"]
    P5 --> P7["P7 · ảnh online / social / DM"]
    P6 --> P7
    P5 --> P8["P8 · migration / phát hành"]
    P6 --> P8
    P7 --> P8
~~~

P3/P4 có thể triển khai độc lập sau khi các hợp đồng P2 ổn định. Backend review có thể bắt đầu từ P0 bằng bộ GAP và PC; không cần đợi UI camera xong. Nếu backend chưa đáp ứng P6/P7, vẫn hoàn thành các mốc kỹ thuật không phụ thuộc, nhưng không ghi đạt sản phẩm B.

## 2. P0 — Bàn giao tài liệu và hiện trạng

| Ticket | Việc làm | Đầu ra / điều kiện xong |
| --- | --- | --- |
| P0-01 | Kiểm kê source RN và base Kotlin | Bảng mapping có nguồn, hiện trạng local/online và phần placeholder |
| P0-02 | Chuyển docs và ngữ cảnh nghiệp vụ | docs/react-native-source, nguồn gốc và liên kết được ghi trong TRANSFER |
| P0-03 | Ghi kiến trúc, backlog và validation | Bộ docs/migration; không đánh dấu AC runtime pass |
| P0-04 | Ghi các quyết định ảnh hưởng triển khai | Decision log ở file nghiệm thu; giả định không thành xác nhận |

P0 là phạm vi lượt hiện tại. Tài liệu base và RN được giữ riêng để người triển khai không làm theo script RN khi đang làm Kotlin.

## 3. P1 — Baseline và khung sản phẩm

**Phụ thuộc:** P0. **Đích:** project Kotlin có entry Let’s Play và build kiểm soát được; chưa cần dữ liệu server.

| Ticket | Thay đổi dự kiến | Kiểm chứng |
| --- | --- | --- |
| P1-01 | Chạy build/test base, ghi JDK/SDK/Gradle và lỗi có sẵn | Lưu log baseline; không quy lỗi cũ cho migration |
| P1-02 | Chốt tổ hợp toolchain/dependency tương thích, bổ sung Room/image/camera khi cần | Gradle sync + compile; version catalog duy nhất; không nâng rải rác |
| P1-03 | Thêm data-android, cập nhật settings, dependency allowlist và CI | verifyModuleBoundaries; JVM modules không import Android |
| P1-04 | Chuyển rootProject.name sang LetsPlay; cấu hình namespace/app label/applicationId dev | APK dev cài song song RN được; chưa đụng signing release |
| P1-05 | Tạo route contract/main tabs và session entry state | Tab/back stack đúng; UI loading/empty/error có thật |
| P1-06 | Chuyển token theme/icon/string đầu tiên | Compose render + font scale/TalkBack/insets trên màn thật |
| P1-07 | Tách JSONPlaceholder/Todo/Users khỏi entry và DI sản phẩm | App Let’s Play không gọi API mẫu; chỉ xóa sample khi test thay thế sẵn |

File chính: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, app/build.gradle.kts, manifest, app/navigation, app/di, resources/theme, module data-android mới.

**Gate G1:** baseline đã ghi rõ; app dev mở được, navigation không còn trỏ mẫu; module boundaries và các test base liên quan pass. Nếu baseline lỗi dependency/SDK thì sửa có log, không coi đã pass vì mới tạo graph.

## 4. P2 — Model, nghiệp vụ thuần và persistence

**Phụ thuộc:** P1. **Đích:** nền dữ liệu cho cả local và online, chưa gắn toàn bộ màn.

| Ticket | Thay đổi dự kiến | Điều kiện xong |
| --- | --- | --- |
| P2-01 | ID type riêng, RoomLifecycle, legacy modes, MoneyVnd và repository contracts | Không dùng tên/index làm account/membership online |
| P2-02 | Port equalShares/balances/debt suggestions và validator | 100đ/3 người = 34/33/33; tổng zero; pending không tác động; kiểm overflow |
| P2-03 | Port planning reducer/scoring/Borda/timeline thành rules/use case | Thêm/vote giữ plan; sửa đúng ý reset đúng vote; draft không sửa published plan |
| P2-04 | Thiết kế Room schema/DAO và local port; transaction cho room/ledger | Hai phòng cô lập; restart giữ dữ liệu; foreign key/indices/migration được kiểm |
| P2-05 | LegacyRoomDecoder và legacy photo decoder v1 | Dữ liệu thiếu field mới vẫn đọc theo default cũ; hỏng/unknown version không ghi đè |
| P2-06 | CredentialStore + ViewerScope/session coordinator | Secret bảo vệ; write serialize; account switch không nhận response cũ |
| P2-07 | File store và photo draft metadata | Copy thành công rồi mới commit metadata; lỗi I/O không tạo record “đã lưu” giả |

File chính: domain/model, domain/repository, domain/usecase/planning/money, data/local ports, data-android/database/credential/files, app/di.

**Gate G2:** unit test nghiệp vụ và transaction/restart test pass. Mapping index legacy được giữ trong namespace local. Không gộp local room vào online vì trùng tên hoặc mã.

## 5. P3 — Phòng online theo API hiện có

**Phụ thuộc:** P2. **Đích:** một vertical slice auth → room → mutation → refetch theo source client RN.

| Ticket | Thay đổi dự kiến | Điều kiện xong |
| --- | --- | --- |
| P3-01 | Lấy full OpenAPI/response mẫu môi trường test, đối chiếu schema.ts/client.ts | DTO không generate từ swagger-evidence trích; route khác biệt được ghi |
| P3-02 | Auth login/register/logout, hết phiên, lưu account và legacy room credentials | Cold start/401/logout/account switch đúng scope; không tự dựng auth service thứ hai |
| P3-03 | Room list/create/preview/join/get/claim/members/leave/kick | Guest claim giữ account bearer và guest token riêng; create requestId không đổi khi retry cùng ý định |
| P3-04 | Proposal/comment/vote legacy/phase/selected plan/completion | UI giữ phiếu boolean và plan đơn theo backend; không giả ba mức/multi-stop |
| P3-05 | Availability/attendance/preferences/ride và room metadata | Poll khung giờ khác Borda; partial load lỗi không xóa dữ liệu đã thành công |
| P3-06 | Ledger/multi-payer/settlement và audit UI | Dùng response thật; unknown status fail rõ, không tự suy confirmed |
| P3-07 | SSE + polling fallback + revision cache/session cancellation | Refetch theo viewer, dừng khi background/blocked/logout; không có stream trùng |
| P3-08 | API error/uncertain-result UX | Timeout sau mutation không auto resend; refresh lỗi không phủ nhận command đã nhận |

File chính: data/api/dto/mapper/events/repository, app/ui/auth/rooms/room, app/navigation.

**Gate G3:** MockWebServer tests cho request/auth/schema/error/cancellation; tích hợp bằng tài khoản/phòng test được chỉ định. Actor/cross-room/receiver cần response thật; fixture pass chỉ chứng minh adapter. Mốc này có thể dùng compatibility guest trong thử nghiệm; đích phát hành bắt buộc đăng nhập theo PC.

## 6. P4 — Phòng local tương đương RN

**Phụ thuộc:** P2 và route shell P1; không buộc P3 xong. **Đích:** giữ công cụ local và đường đọc dữ liệu cũ, phân biệt với phòng online.

| Ticket | Thay đổi dự kiến | Điều kiện xong |
| --- | --- | --- |
| P4-01 | List/create/search/open-code/demo/archive/restore/delete local | Phòng mới không seed mẫu; mã chỉ mở cùng thiết bị; xác nhận trước xóa |
| P4-02 | Đề xuất/vote 2/1/0/shortlist/order/Borda/publish/history/end | Giữ plan và completion khi thêm ý/vote; host/member local chỉ là mô phỏng |
| P4-03 | Form chi đều/custom, payer, balance, debt và transfer pending | Port kịch bản roomSession/money; sổ mỗi phòng độc lập |
| P4-04 | Share plan, notes và persistence khi process restart | Nội dung chia sẻ đúng; không mất draft khi lỗi write |
| P4-05 | Màn xem legacy data và thông báo phạm vi local | Không tạo lời hứa đồng bộ hoặc quyền online từ local role |

Không tự biến local ledger một payer thành bằng chứng đã hỗ trợ approval multi-payer. Những nâng cấp sản phẩm mới thuộc P6, không âm thầm rewrite lịch sử legacy.

**Gate G4:** luồng tạo phòng rỗng → góp ý/vote → xếp lịch/chốt → chi tiền → khởi động lại; số liệu và lịch sử khớp fixture RN. Archive local không khóa phòng server.

## 7. P5 — Camera, composer và thư viện local

**Phụ thuộc:** P1/P2, ViewerScope P3 và liên kết local room P4. **Đích:** tương đương luồng ảnh RN hiện tại trên Android.

| Ticket | Thay đổi dự kiến | Điều kiện xong |
| --- | --- | --- |
| P5-01 | CameraX preview/capture/front-back/flash và permission denial | Camera theo lifecycle; thiết bị không có camera vẫn import được |
| P5-02 | Photo Picker, copy content URI, xử lý EXIF/rotation và ảnh lớn | Không cần quét toàn thư viện; tránh full-resolution bitmap trong UI |
| P5-03 | CaptureComposer theo source: caption trên ảnh, X/gửi/Aa, retake/reselect | Giữ caption/room/recipient khi thay ảnh; cancel không tạo post |
| P5-04 | Local save riêng với publish; trạng thái draft và lỗi write | Nhãn/receipt chỉ nói lưu trên máy; thiếu adapter không gửi giả |
| P5-05 | Memories lịch/lưới, photo detail và VerticalPager | Một ảnh/viewport, giữ item khi rotate, lỗi decode có bounded retry |
| P5-06 | Reaction/comment local và xóa ảnh/metadata | Cô lập owner scope; comment local không bị gọi là chia sẻ liên thiết bị |
| P5-07 | Kiểm UI/insets/IME/large font và camera thật | Ghi thiết bị/API + ảnh rendered; unit test không thay native validation |

File chính: app/ui/camera/photos, data-android/files/database, resources/theme/icon, manifest và Activity Result integration.

**Gate G5:** capture/import → composer → retake/cancel/save → gallery/restart chạy trên thiết bị; ảnh lớn không OOM trong kịch bản đã kiểm. Mốc A đạt khi G3/G4/G5 và checklist parity pass. Mốc A chưa đạt toàn bộ BA sản phẩm.

## 8. P6 — Khớp backend với nghiệp vụ đã xác nhận

**Phụ thuộc:** P3; review hợp đồng bắt đầu từ P0. **Đích:** các tính năng phòng/plan/money mục tiêu, có server enforce.

Mỗi ticket yêu cầu phía backend cung cấp schema/version, actor/state, errors, idempotency/concurrency, event và dữ liệu cũ. Workspace hiện không có source backend; phần server ghi là phụ thuộc ngoài, không tự đánh dấu hoàn tất.

| Ticket | Nghiệp vụ đích | Phụ thuộc / nguồn |
| --- | --- | --- |
| P6-01 | Account bắt buộc, join policy/approval, rotate/lock invite, chuyển host có accept | PC và stable account/membership mapping; giữ đường claim dữ liệu guest cũ |
| P6-02 | Đặt phiếu 2/1/0 xuyên suốt ACTIVE, unknown khác zero, host sửa ý chưa chốt đúng màn/quyền | CP và PC; replace toggle bằng command xác định, reset đúng content revision |
| P6-03 | Shortlist/order ballot/Borda/plan version, complete/skip/undo có audit | Publish atomic có expected revisions; giữ snapshot/completion |
| P6-04 | Multi-payer approval, sửa tăng share, amendment sau settlement | Approval set rõ payer/share và revision; pending chưa tính balance |
| P6-05 | Sender report → receiver confirm/reject, cancel pending, partial pay, reversal | Actor kiểm ở server; retry không tính hai lần; audit giữ bản gốc |
| P6-06 | Former-member self-finance và đóng/lưu lịch sử | Không cấp lại album/toàn ledger; close atomic khi zero debt và không pending |
| P6-07 | Capability/version compatibility và chuyển legacy room | LEGACY_SINGLE khác GROUP_ITINERARY; không ép boolean vote thành ba mức |

**Gate G6:** test thật ít nhất hai account + người ngoài phòng; conflict/retry/race và invariants tiền. Bật tính năng khi contract được xác minh, không từ feature flag client đơn thuần. Những AC liên quan còn thiếu server giữ NOT_VERIFIED.

## 9. P7 — Ảnh online, bạn bè, DM và thông báo

**Phụ thuộc:** P5, danh tính/lifecycle P6 và API backend phù hợp.

| Ticket | Thay đổi dự kiến | Điều kiện xong |
| --- | --- | --- |
| P7-01 | Friendship/block, chính sách lookback và history grant theo lần quan hệ/membership | Cutoff cố định; selected audience không thành all friends; block không ẩn album chung |
| P7-02 | Media upload/process/READY, hủy, retry và durable draft | Pending upload không thành post; không replay dưới account khác |
| P7-03 | Publish Moment + PERSONAL/ROOM Placement atomic | Tối đa đích theo contract; lỗi một đích không đăng phần còn lại; retry cùng mutation ID |
| P7-04 | Cursor feed/album, filter, refresh/dedup, media ACL/cache lease | Dừng nextCursor=null; lỗi trang sau giữ item/cursor; revoke không hiển thị cache như còn quyền |
| P7-05 | Reaction/photo comment/album comment đúng placement/thread | Không leak comment/count nhóm sang PERSONAL |
| P7-06 | Album VIEW_ONLY/OPEN/WINDOWED và owner self-delete | Deadline theo endedAt server; quyền xem lịch sử độc lập quyền đóng góp |
| P7-07 | Conversation/inbox/DM tham chiếu ảnh | Hai người đủ quyền, host không đọc DM; reference mất quyền không giữ thumbnail vĩnh viễn |
| P7-08 | In-app notifications, push preference, dedup và deep link | Tắt push vẫn có in-app; không lộ tác giả đề xuất/token/DM trong room event |
| P7-09 | Tích hợp CameraComposer recipient adapter và receipt online | Bạn từ server bằng stable ID; chỉ báo đã đăng khi commit đủ đích |

**Gate G7:** kiểm hai user, hai room, friend/unfriend/block, join/leave, lịch sử trước/sau cutoff, deadline/race, reconnect/replay và quyền media thật. Không dùng ảnh chỉ nằm trên máy khác để giả feed online.

## 10. P8 — Di trú và phát hành

**Phụ thuộc:** các gate tương ứng phạm vi phát hành; sản phẩm B cần G6/G7, không lấy G5 thay thế.

| Ticket | Thay đổi dự kiến | Điều kiện xong |
| --- | --- | --- |
| P8-01 | Chọn cài song song hay update đè, ký và applicationId | Quyết định có bằng chứng package/signing; test dev không thay RN release |
| P8-02 | Export/import legacy room/photo và media manifest | Dry-run, validation, transaction, idempotent import, giữ dữ liệu lỗi để khôi phục |
| P8-03 | Nếu update đè: reader/bridge MMKV và credential tương thích | Kiểm file format, encryption/key alias/path và upgrade artifact thực tế; không đoán từ JSON schema |
| P8-04 | Regression/performance/lifecycle/a11y/device matrix | Kết quả ghi theo thiết bị/API, phần chưa kiểm rõ |
| P8-05 | CI theo module/variant, lint, release config, secret/log review | Không JSONPlaceholder/OTA HRM; prod endpoint và release manifest đúng |
| P8-06 | Cập nhật README/architecture/testing, rollout và rollback | Tài liệu mô tả runtime cuối; backup/forward-compatible migration được diễn tập |

**Gate G8:** không mất dữ liệu trên đường migration đã chọn; release build và checklist phạm vi pass. Rollback code không được ngầm hứa rollback database schema hoặc lấy lại dữ liệu đã xóa.

## 11. Gợi ý các PR đầu tiên khi bắt đầu code

1. PR nền tảng: baseline, toolchain cần thiết, data-android, boundary/CI và cấu hình dev.
2. PR domain: ID/MoneyVnd, hàm chia tiền/planning và test port.
3. PR storage: Room/local ports, credentials, decoder v1 và test transaction.
4. PR vertical slice online: đăng nhập → danh sách → chi tiết một phòng → mutation/refetch.
5. PR local room/planning/money; sau đó PR camera/composer/gallery.
6. Các PR nghiệp vụ mới tách theo ticket contract P6/P7.

Ước lượng và chia người thực hiện sau khi P1 baseline và P3-01 response thực tế hoàn tất. Không đặt deadline chi tiết dựa trên số màn hình vì rủi ro lớn nhất là contract, quyền và di trú dữ liệu.
