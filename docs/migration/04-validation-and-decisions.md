# Nghiệm thu, dữ liệu và quyết định còn mở

[Mục lục](README.md) · [Backlog](03-implementation-plan.md) · [Tài liệu chuyển sang](../react-native-source/TRANSFER.md)

## 1. Trạng thái bằng chứng

- SOURCE_PRESENT: tìm thấy code/lời gọi trong RN; chưa có nghĩa đã chạy được server/native.
- PROPOSED: thiết kế hoặc phần BA chưa chốt; không tự nâng thành yêu cầu xác nhận.
- CONTRACT_REQUIRED: cần backend công bố schema/semantics trước khi tích hợp.
- NOT_VERIFIED: chưa chạy kiểm chứng tương ứng.
- VERIFIED_PASS / VERIFIED_FAIL: chỉ dùng kèm lệnh, commit, thiết bị/môi trường, input và kết quả thật.

Trong lượt lập kế hoạch này: **Kotlin runtime, build, API runtime, camera và AC sản phẩm đều NOT_VERIFIED**. Có đọc source/schema/test và chuyển tài liệu. Không chạy Gradle/Jest vì không thay code; validation của lượt này là nội dung, đường dẫn, inventory và phạm vi Git.

## 2. Kịch bản nghiệm thu migration

Các mã MIG-AC là checklist mới cho việc chuyển nền tảng. Không thay mã AC/CP/PC của BA gốc. Trạng thái toàn bộ dòng dưới đây hiện là **CHƯA CHẠY**.

| ID | Given / When | Then cần chứng minh | Mốc / nguồn |
| --- | --- | --- | --- |
| MIG-AC01 | Base được cấu hình; build dev và kiểm module | APK build, boundary pass, không gọi JSONPlaceholder | P1; base CI |
| MIG-AC02 | Tạo hai phòng local rỗng; thêm ý/chi ở một phòng | Phòng mới không seed mẫu; phòng kia không đổi | P2/P4; roomStorage/roomSession |
| MIG-AC03 | Chia 100đ cho ba người theo thứ tự legacy | 34/33/33, tổng đúng 100; participant trùng/ngoài range bị từ chối | P2; money.test.ts |
| MIG-AC04 | Có expense và transfer pending; receiver confirm hợp lệ | Pending không đổi balance; confirmed tác động đúng một lần; tổng balance = 0 | P2/P6; money + PC |
| MIG-AC05 | Đã có lịch/completion; thêm ý/vote/đổi draft | Giữ published plan, version và completion đến khi publish thành công | P2/P4/P6; planning + CP-01–05 |
| MIG-AC06 | Sửa ý chưa chốt hoặc cố sửa ý đã chốt | Reset đúng phiếu content revision; chặng đang đi không bị sửa âm thầm | P2/P6; planning + PC |
| MIG-AC07 | Phiếu là 0, chưa có phiếu và boolean legacy | Ba trạng thái không bị gộp; không ép vote cũ thành mức 2/1/0 | P2/P3/P6; compatibility map |
| MIG-AC08 | Đang tải/SSE hoặc mở composer; đổi account/logout | Dữ liệu viewer cũ không vào state mới; không lưu/gửi nháp dưới owner mới | P3/P5; OnlineProvider/photo capture |
| MIG-AC09 | Mutation commit nhưng response timeout hoặc refresh lỗi | UI nêu kết quả chưa rõ/đã nhận; không auto tạo command mới | P3/P6; client.ts |
| MIG-AC10 | Account bearer, room guest và claim tồn tại | Claim dùng đúng hai credential; secret không có trong display DTO/log | P3; credentials/OnlineProvider |
| MIG-AC11 | Capture/import thành công; retake/reselect/cancel hoặc lỗi save | Composer giữ ngữ cảnh; cancel không post; lỗi giữ nháp; đổi ảnh không nhân bản | P5; photoCaptureFlow |
| MIG-AC12 | Save local khi publish adapter chưa có | Chỉ báo lưu trên máy; không gắn PUBLISHED hoặc gửi đến bạn giả | P5; PhotoProvider/PhotoComposer |
| MIG-AC13 | Feed nhiều ảnh; vuốt và resize/rotate | Một ảnh/viewport, một trang mỗi lần snap; giữ ID đang xem; decode có giới hạn | P5; PhotoFeed và UI delta |
| MIG-AC14 | V1 thiếu source/field mới hoặc file JSON hỏng | Default hợp lệ; unknown version/hỏng không ghi đè dữ liệu gốc | P2/P8; roomStorage/photoSource |
| MIG-AC15 | Server mới có nhiều payer/share/approval; sửa sau settlement | Đủ duyệt mới tính; amendment/reversal giữ lịch sử và kiểm revision | P6; PC-AC11–15 |
| MIG-AC16 | Người rời phòng còn nợ; đọc/confirm self-finance | Chỉ được dữ liệu liên quan; không được album/toàn sổ/khoản mới ngoài quyền | P6; PC-AC16 |
| MIG-AC17 | Close chạy đồng thời expense/settlement pending | Server serialize/conflict; không close khi có nợ/pending; giữ read history hợp lệ | P6; PC-AC17–18 |
| MIG-AC18 | Một Moment chia PERSONAL và ROOM | ACL/reaction/comment riêng; lỗi một đích không publish một phần | P7; BA ảnh |
| MIG-AC19 | Cursor load lỗi/duplicate/revoke giữa hai trang | Giữ cursor/items hợp lệ, dedup placementId, dừng hết trang, loại media mất quyền | P7; UI feed delta |
| MIG-AC20 | Kết bạn/join với lookback; thời gian trôi/đổi setting/rejoin | Cutoff cố định; không TTL; grant theo lần quan hệ, không hồi sinh grant cũ | P7; PC-AC05–08 |
| MIG-AC21 | Block nhưng cùng phòng; gửi DM hoặc đọc ảnh nhóm | PERSONAL/DM bị chặn; album chung vẫn theo membership; tiền không thay | P7; PC-AC09 |
| MIG-AC22 | VIEW_ONLY/OPEN/WINDOWED, deadline và owner delete | Server time kiểm ở commit; xem lịch sử riêng quyền góp; owner có self-delete | P7; BA ảnh/PC |
| MIG-AC23 | Push bị tắt; cùng event replay nhiều lần | In-app còn, không push, không nhân thông báo hoặc leak author | P7; PC-AC19 |
| MIG-AC24 | Import cùng bundle hai lần hoặc mất điện giữa import | Không nhân room/expense/photo; atomic hoặc resumable có checkpoint; nguồn giữ nguyên | P8; migration design |
| MIG-AC25 | Update đè bản RN trên thiết bị có MMKV/credential/ảnh thật | Dữ liệu đọc được, ownership không đổi, rollback theo schema đã kiểm | P8; upgrade artifact thực tế |

MIG-AC04/15–23 cần test actor/cross-user/cross-room phía server khi chạm online. Pass hàm thuần hoặc UI fixture không thay thế bằng chứng authorization.

## 3. Cách kiểm khi triển khai

Dùng terminal từ root LetsPlay. Đây là **lệnh dự kiến**, không phải log đã chạy trong lượt này.

~~~powershell
.\gradlew.bat verifyModuleBoundaries
.\gradlew.bat :shared:test :domain:test :data:test
.\gradlew.bat :app:testDevDebugUnitTest :app:testProdDebugUnitTest
.\gradlew.bat :app:assembleDevDebug :app:assembleProdDebug
~~~

Khi data-android được thêm, bổ sung task unit/instrumentation/lint tương ứng module và CI. Khi có test thiết bị, xác định device/test runner/variant trước khi chạy connected tests; chỉ ghi lệnh task thực sự tồn tại sau sync.

Port trước những kịch bản đang có giá trị: money, scoring, planning, roomStorage, roomSession, photoSource, photoCaptureFlow và photoInteractions. Kiểm API bằng MockWebServer cho header/serialization/cancellation/5xx/schema mismatch; kiểm Room bằng transaction/migration tests; kiểm UI bằng thao tác thật và semantics, tránh test chỉ mirror implementation.

Device matrix tối thiểu đề xuất: máy API tối thiểu được hỗ trợ, một máy API mới với edge-to-edge, camera thật, thiết bị bộ nhớ thấp/ảnh lớn; portrait/landscape, bàn phím mở, chữ lớn và TalkBack. Chốt máy/API cụ thể ở P1/P5. Chưa cam kết foldable/tablet hoàn chỉnh hoặc iOS.

## 4. Chiến lược giữ dữ liệu RN

### Cài Kotlin song song

ApplicationId khác thì sandbox khác. App Kotlin không tự đọc MMKV hoặc Keychain của app RN. Không yêu cầu người dùng xóa app cũ để “làm sạch”.

Đường đề xuất: bổ sung export ở RN, bundle versioned có room JSON + photo metadata + media files + manifest kiểm toàn vẹn; Kotlin import bằng document picker. Không mang password/token vào file export.

Importer validate version, ID, participant mapping, tổng tiền và media path trước; cho xem số lượng/lỗi; commit dữ liệu hợp lệ theo chính sách atomic được nêu rõ. Import key/source provenance giúp chạy lại không nhân bản. File lỗi được giữ và báo, không âm thầm bỏ ledger. Dữ liệu online phục hồi qua login/API/claim hợp lệ, không tự đăng room local lên server.

### Update đè cùng applicationId

Cần cùng signing identity phù hợp và versionCode cao hơn. Kế hoạch hiện chưa xác minh signing/versionCode phát hành. Không lấy applicationId RN rồi cài đè để thử.

Schema JSON v1 không đủ để đọc file MMKV nhị phân. Cần khảo sát storage instance ID/key/path, encryption và native reader/bridge; credential Keychain cần đối chiếu alias/key format/Keystore, không coi DataStoreSessionManager của base đọc được. Ảnh kiểm đường dẫn app-private thực tế.

Trước nâng cấp: backup có thể phục hồi, fixture thiết bị chứa room/ledger/ảnh/credential thật của môi trường test, reader tương thích và rollback đã diễn tập. Việc đọc credential cũ không tự cấp quyền account hoặc nhận ảnh thuộc scope khác.

### Khi chuyển model

- Giữ bản raw nguồn và import provenance, không sửa tại chỗ file cũ ngay lần đầu.
- Mapping LocalMemberId dựa room + index gốc; giữ mảng legacy để đối chiếu khoản cũ. Không merge thành viên chỉ vì cùng tên.
- Local room/role không thành membership server. Claim guest membership chỉ qua API đúng contract.
- Ảnh LOCAL_ONLY giữ owner scope/source/room intent; không tự publish hoặc chuyển chủ khi login.
- Plan history, completed, pending transfer và khoản đã xác nhận giữ nguyên ý nghĩa. Không normalize bằng cách xóa history.
- Việc chuyển file và DB cần staging/cleanup: lỗi giữa copy và transaction không để metadata “đã lưu” trỏ file chưa có.
- Rollback phần mềm phải tính forward compatibility của database. Không dùng destructive migration để vượt lỗi schema.

## 5. Decision log

| ID | Quyết định/điểm mở | Trạng thái | Chặn phần nào |
| --- | --- | --- | --- |
| MIG-D01–03 | Đích LetsPlay, Jetpack/Hilt, kế hoạch trước | XÁC NHẬN | Không chặn |
| MIG-D04–07 | Compose, data-android, Android trước, giữ visual identity | ĐỀ XUẤT/GIẢ ĐỊNH đã ghi ở mục lục | Chốt khi bắt đầu P1; chưa thay code |
| MIG-Q01 | Phát hành cài song song hay thay RN cùng package/signing | CẦN QUYẾT ĐỊNH | P8 production migration; P1 dev vẫn làm được |
| MIG-Q02 | Full OpenAPI và môi trường/tài khoản test backend hiện tại | CẦN THÔNG TIN | P3 integration/G3, P6/P7; adapter fixture vẫn làm được |
| MIG-Q03 | Cách rollout account bắt buộc cho guest hiện hữu | CẦN CONTRACT/MIGRATION | Public release; không đổi yêu cầu PC về bắt buộc đăng nhập |
| MIG-Q04 | Capability/version route cho multi-stop vote/plan | CẦN BACKEND | P6 planning; không suy từ legacy phase |
| MIG-Q05 | Approval set payer/share, người rời và điều chỉnh đang pending | CHI TIẾT BA CÒN MỞ | Ticket tiền liên quan; giữ xác nhận tăng share và sau settlement |
| MIG-Q06 | Provider media, signed URL/revoke, upload/retry/quota | CẦN BACKEND/VẬN HÀNH | P7 media production |
| MIG-Q07 | Timestamp publish theo placement khi chia lại, history grant, default WINDOWED còn đề xuất | CẦN CONTRACT/QUYẾT ĐỊNH CHI TIẾT | Các ticket phụ thuộc; không hỏi lại ba mode đã xác nhận |
| MIG-Q08 | Push provider, device registration và notification contract | CẦN BACKEND | P7-08; không tự đưa Firebase Auth/Firestore vào stack |
| MIG-Q09 | Android min/target SDK và thiết bị phát hành | CẦN AUDIT THỜI ĐIỂM TRIỂN KHAI | P1 toolchain/P8 release |

Không cần mở một buổi phỏng vấn sản phẩm mới cho toàn bộ các điểm trên. Đọc nguồn trước, giải quyết từng điểm đúng ticket; phần độc lập tiếp tục được. Thay đổi có ảnh hưởng nghiệp vụ cần cập nhật quyết định → BA → model/API/event → AC cùng nhau.

## 6. Điều kiện bàn giao cuối

Bản chuyển nền tảng chỉ được ghi hoàn tất khi có traceability tính năng → source → Kotlin → test/result, migration dữ liệu đã kiểm và các giới hạn phát hành được chấp nhận rõ. Không xóa công việc chờ backend khỏi checklist để làm cho trạng thái trông hoàn thành.

Trong lượt hiện tại, đầu ra hoàn tất là **bộ mapping, kiến trúc, backlog, checklist và tài liệu được chuyển**. Việc triển khai và nghiệm thu Kotlin là công việc tiếp theo đã được chia ticket, chưa được báo pass.
