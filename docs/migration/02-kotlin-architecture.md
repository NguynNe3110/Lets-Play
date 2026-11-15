# Kiến trúc Kotlin đề xuất

[Mục lục](README.md) · [Mapping](01-source-mapping.md) · [Kế hoạch](03-implementation-plan.md)

Đây là thiết kế để triển khai sau, chưa phải cấu trúc đã có trong LetsPlay.

## 1. Hướng công nghệ

| Phần | Lựa chọn đề xuất | Căn cứ và giới hạn |
| --- | --- | --- |
| UI | Jetpack Compose + Material 3 | Base đã có; giữ diện mạo Let’s Play theo source |
| Presentation | MVVM + luồng dữ liệu một chiều | Mỗi màn có UiState bất biến, action qua hàm ViewModel |
| DI | Hilt trong Android composition root | User yêu cầu Hilt; JVM module dùng constructor/provider |
| Async | Coroutines, Flow/StateFlow | Dùng dispatcher inject; không nuốt CancellationException |
| Navigation | Navigation Compose của base ở mốc đầu | Không ghép việc chuyển Navigation 3 và nâng toolchain vào mọi feature |
| REST | Retrofit + OkHttp + Gson sẵn trong base | DTO có validation tường minh, không chỉ dựa vào parse JSON thành công |
| SSE | OkHttp event stream + Flow adapter | Hủy theo session/màn; invalidate rồi refetch theo viewer |
| Relational storage | Room trong module Android | Local rooms, ledger, photo metadata và dữ liệu cache có scope |
| Preferences | DataStore | Filter/theme/onboarding; không dùng để chứa toàn bộ ledger/ảnh |
| Secrets | CredentialStore được bảo vệ bằng Keystore | Không lưu accessToken dạng rõ trong preference/cache/log |
| Camera/import | CameraX + Android Photo Picker | Đọc skill và xác minh API/version khi đến ticket thực hiện |
| Ảnh | Image loader có giới hạn kích thước/cache | Chốt dependency/version ở P1/P5 theo toolchain, chưa thêm thư viện ở lượt này |
| Background | WorkManager khi có upload bền hoặc sync cần chạy lại | Không dùng để duy trì SSE liên tục; không tự retry mutation chưa có idempotency |
| Tests | Kotlin tests, coroutines-test, MockWebServer, Robolectric và Compose instrumentation | Chọn loại test theo hành vi; không coi test UI mock là test quyền server |

Các lựa chọn layer, repository, UI state và collection theo lifecycle phù hợp [hướng dẫn kiến trúc Android](https://developer.android.com/topic/architecture/recommendations). Quyết định giữ navigation hiện có là cách giảm phạm vi migration của dự án này; không phải tuyên bố đây là lựa chọn mới nhất.

## 2. Module và dependency

Base có năm module: app, resources, data, domain, shared. Trong đó data/domain/shared là JVM; không đưa Room/Context/Keystore trực tiếp vào data JVM.

Đề xuất thêm **data-android** để triển khai adapter phụ thuộc Android. Chưa cần chia mỗi màn thành một Gradle feature module.

~~~mermaid
flowchart TD
    app[":app · Compose / ViewModel / navigation / Hilt"] --> domain[":domain · models / repository contracts / use cases"]
    app --> data[":data · HTTP / DTO / repositories / ports"]
    app --> android[":data-android · Room / credential / files"]
    app --> resources[":resources · theme / icons / strings"]
    app --> shared[":shared · Result / error / dispatcher / action"]
    android --> data
    android --> domain
    android --> shared
    data --> domain
    data --> shared
    domain --> shared
~~~

| Module | Được chứa | Không đặt tại đây |
| --- | --- | --- |
| app | Route, screen, ViewModel, Hilt providers, Activity Result/Camera preview integration | SQL, bearer storage, tính tiền trong composable |
| resources | Token xanh–kem/theme ảnh tối, typography, strings, drawable | Repository/state phiên |
| domain | AccountId/MembershipId, room/planning/money/photo model, repository interface, policy/use case thuần | Android Context, Retrofit DTO, Room annotation, Hilt annotation |
| data | REST/SSE transport, DTO mapper, repository implementation, interface cho local/credential/file adapter | Android SDK hoặc secret trong cached Room DTO |
| data-android | Room entities/DAO/database, adapter các port, Keystore credential, file store | Điều hướng hoặc kiểm quyền online thay server |
| shared | Result/error/dispatcher/loading/retry primitive | Phụ thuộc feature và mutable cache account toàn cục |

Hilt providers ở app bind các adapter data-android vào port của data/domain. Không để data phụ thuộc ngược data-android. Nếu local repository cần transaction phức tạp, port cung cấp một thao tác atomic theo use case thay vì để repository gọi nhiều DAO write rời.

Khi thêm module phải cập nhật settings.gradle.kts, allowlist verifyModuleBoundaries ở root, CI và sơ đồ kiến trúc. Quy tắc mới: app được phụ thuộc data-android; data-android được phụ thuộc data/domain/shared; domain và shared vẫn thuần JVM.

## 3. Package dự kiến

~~~text
app/src/main/java/com/uzuu/letplay/
  navigation/
  di/
  ui/auth/
  ui/rooms/
  ui/room/{plan,proposals,availability,money,members}/
  ui/localrooms/
  ui/camera/
  ui/photos/
  ui/messages/
  ui/notifications/

domain/src/main/java/com/uzuu/letplay/domain/
  model/{identity,room,planning,money,photo,social}/
  repository/
  usecase/{auth,room,planning,money,photo,social}/

data/src/main/java/com/uzuu/letplay/data/
  api/  dto/  mapper/
  repository/
  local/          # JVM ports, không DAO Android
  credential/     # credential port
  events/

data-android/src/main/java/com/uzuu/letplay/data/android/
  database/{entity,dao,migration}/
  credential/
  files/
  preferences/
~~~

Đổi com.example.base và applicationId là ticket riêng. Package source và applicationId không cần trùng, nhưng kế hoạch hướng đến namespace thống nhất. Giữ quyền tác giả/license của base khi thay tên.

## 4. Luồng dữ liệu

UI action → ViewModel → use case/repository domain → implementation data → HTTP hoặc local adapter → committed state → Flow → UiState → Compose.

- RoomViewModel nhận roomId từ SavedStateHandle, không giữ Activity/Camera instance.
- UiState chứa loading/error/empty/content và draft; dùng collectAsStateWithLifecycle ở route.
- Dùng BaseViewModel/ActionRunner hiện có khi phù hợp. Giữ pending UI effect của base cho snackbar/navigation và acknowledge sau xử lý; dữ liệu nghiệp vụ bền nằm trong repository.
- Tách loading initial, refresh, submit theo key; không khóa toàn màn chỉ vì một tab tải thêm.
- Drawer/dialog/sheet draft giữ qua rotation bằng SavedStateHandle khi nhỏ; ảnh draft giữ file/metadata bền, không nhét Bitmap/Base64 vào Bundle.
- Giao dịch thành công nhưng refresh thất bại: báo đã nhận thao tác, cho tải lại; không đưa thành “thao tác thất bại” để người dùng tạo thêm bản ghi.

## 5. Session và cache

SessionState phân biệt initializing, signed-out, authenticated, expired; legacy guest token là credential theo room, không thay AccountId.

Mỗi request và cache entry gắn ViewerScope/session generation. Khi login/logout/đổi tài khoản:

1. Đổi generation và hủy request/SSE đang chạy.
2. Xóa state online của viewer cũ khỏi UI; tách cache theo account/guest scope.
3. Response cũ bị bỏ qua dù server đã xử lý.
4. Secret write được serialize; nếu không ghi bền được thì báo rõ, không tuyên bố đã lưu phiên.
5. Ảnh local owner cũ giữ riêng, không tự chuyển chủ sang account vừa đăng nhập.

Token ưu tiên theo contract của operation: room token nếu legacy route yêu cầu; account bearer cho account route; claim gửi account bearer cùng guest token riêng. Không dùng một interceptor luôn gắn mọi token cho mọi host. Upload signed URL tương lai dùng client riêng theo contract để tránh gửi Authorization sang storage host.

Không tự bổ sung refresh-token flow vì client hiện chỉ mô tả accessToken/expiresAt. Khi 401: dừng vòng reconnect, chuyển trạng thái hết phiên và yêu cầu đăng nhập lại theo contract đã xác minh.

## 6. Mutation, lỗi và đồng bộ

| Trường hợp | Hành vi bắt buộc |
| --- | --- |
| Command đang chạy cùng phòng | Serialize hoặc khóa theo command/room; UI chống double tap |
| Mạng timeout/5xx sau gửi tiền/vote | Kết quả có thể đã commit; refetch/reconcile trước khi gửi lại |
| Backend có idempotency | Giữ cùng mutation key cho retry cùng ý định |
| Toggle vote legacy | Không auto retry; không đổi thành set vote ba mức |
| Revision cũ trả sau revision mới | Không ghi đè cache mới |
| Partial room/ledger/collaboration load | Giữ phần thành công; lỗi riêng từng phần |
| SSE event | Chỉ invalidate; refetch theo quyền viewer; không tin payload broadcast làm domain state |
| SSE reconnect | Backoff có giới hạn, Last-Event-ID theo contract; coalesce refresh |
| 401/403/404 khi đọc phòng | Không tiếp tục render cache như vẫn có quyền |
| Background hoặc rời màn | Hủy subscription/poll; quay lại refetch trước khi nối stream |
| Unknown enum/schema sai | Trạng thái không hỗ trợ/lỗi dữ liệu; không mặc định thành host/confirmed/published |

Base retry cho mutation phải để allowRetry=false trừ khi operation được chứng minh idempotent. SSE/poll không được biến thành hai bộ ghi state cạnh tranh. P3 kiểm event ID có thực sự tương ứng revision; không mặc định khởi tạo Last-Event-ID bằng room revision chỉ vì cùng là số.

## 7. Các miền nghiệp vụ riêng

**Planning:** Proposal/Vote/Shortlist/OrderBallot/PlanVersion/Completion riêng. UI section không quyết định quyền. Published plan và draft độc lập. Mốc A giữ legacy; mốc B theo PC-01–14 và CP, capability rõ từ server.

**Money:** MoneyVnd dùng Long và kiểm tràn. Payers/shares khác nhau. Tính local bằng hàm thuần; online đọc balance authoritative từ server. Pending/approval chưa đủ không thay số dư. Sửa sau settlement phải amendment/reversal; không sửa list expense rồi tự tính lại để giả backend đã đồng ý.

**Photos:** LocalPhoto là thư viện trên máy. Mốc B tách MediaAsset → Moment → Placement; reaction/comment thuộc nơi đăng. AlbumComment và Message là hai thread riêng. Owner library không cấp lại album khi người đó rời phòng.

**Room lifecycle:** ACTIVE/ENDED/closed-history không là planning phase. Kết thúc chuyến đi không tự khóa sổ tiền. Hidden/archive cá nhân không khóa đóng góp của cả nhóm. Chỉ đóng lịch sử khi server kiểm hết nợ và không còn pending tiền.

## 8. UI cần bảo toàn

Giữ theme phòng từ theme.ts: nền #F7F6F2, chữ #24372F, xanh #245C49, xanh nhạt #EAF1E9, đào #FAEDE3 và tím #EEEAF6. Theme ảnh tối dùng nền #080A09 và accent #B8DBAA. Không áp một theme tối toàn app chỉ vì camera dùng nền tối.

- Composer ở cùng capture intent; caption nằm trên ảnh, X/gửi/Aa và hàng người nhận; lưu riêng khác gửi.
- Feed dùng VerticalPager một ảnh mỗi vùng xem, snap một trang, stable ID khi xoay máy; không thay bằng danh sách nhiều thẻ cùng lộ ra.
- Inbox chưa có backend thì thể hiện chưa khả dụng, không seed hội thoại giả trong bản sản phẩm.
- Bảo toàn tiếng Việt, font scale, TalkBack, touch target 48 dp; insets status/navigation/IME. Kiểm small phone và landscape.
- Quyết định tương tác dựa trên capability/policy, không đưa thuật ngữ DTO/MMKV/SSE vào flow người dùng.

Khi thực sự sửa UI, nạp craft-floor và hướng dẫn native của skill impeccable, đọc visual truth từ source/ảnh tham chiếu và kiểm rendered UI. Lượt này chỉ mapping, không tạo visual world mới.

## 9. Toolchain

Phiên bản hiện khai báo: AGP 8.3.1, Kotlin 1.9.23, Compose BOM 2024.03.00, Hilt 2.51.1, Navigation Compose 2.8.9, compile/targetSdk 34; bytecode target Java 17. CI của base dùng JDK 21. Đây là **inventory**, chưa xác nhận tổ hợp build thành công trên máy.

P1 chạy baseline trước, sau đó đối chiếu compatibility matrix và yêu cầu SDK/thư viện/phát hành tại thời điểm thực hiện. Nâng phiên bản theo nhóm tương thích trong ticket riêng, ghi lỗi trước/sau và giữ khả năng rollback. Không tự gọi các version đang có là “latest” hoặc đưa AGP 9 vào phạm vi chỉ vì có skill nâng cấp.

Nguồn hướng dẫn: [Android architecture](https://developer.android.com/topic/architecture/recommendations), [base architecture](../architecture.md), [version catalog](../../gradle/libs.versions.toml), [app Gradle](../../app/build.gradle.kts).
