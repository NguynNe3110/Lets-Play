# Kế hoạch hoàn thiện Kotlin MVVM Base

Ngày lập: 15/11/2026. Cập nhật: 16/11/2026. Trạng thái: **Đang triển khai** (Đã hoàn thành và kiểm chứng toàn diện 140 tests: P0, P1, P2, P3, P5; P4, P6, P7, P8 đã hoàn thành hợp đồng kiến trúc cốt lõi, fake adapters, DataStore session restoration, i18n mapper và CI automation; các hạng mục phụ thuộc SDK cloud thật, Room cache, release signing automation và runtime profiling nằm trong backlog theo lộ trình; P9 hoàn thiện quy hoạch kiến trúc collection & pagination UI sẵn sàng triển khai).

Tài liệu gộp yêu cầu về Input/Output, BaseViewModel linh hoạt, i18n, xử lý luồng tương đương RxDart, các backend ngoài REST và môi trường dev/production. Mục tiêu là một bộ nền tảng Android có ví dụ và kiểm chứng rõ ràng, mỗi feature lựa chọn cơ chế phù hợp.

## 1. Mục tiêu và phạm vi

- Giữ MVVM, Compose, coroutines, StateFlow, Hilt và cấu trúc theo layer hiện tại.
- Chuẩn hóa use case bằng `BaseInput`/`BaseOutput` theo quy ước người dùng mong muốn.
- Có sẵn loading, lỗi, retry và concurrency; sử dụng được với cả `BaseViewModel` lẫn `ViewModel` Android thông thường qua composition.
- Hỗ trợ lỗi linh hoạt: lỗi tại trường nhập (inline), dialog, snackbar, screen error state và xử lý tùy chỉnh (`ErrorPresentationMode`).
- Có đường đi i18n xuyên suốt từ lỗi nghiệp vụ tới thông báo UI với deferred resolution (`UiText`).
- Có ví dụ debounce, throttle, latest, bỏ thao tác khi đang bận (`droppable`) và xử lý tuần tự an toàn (`sequential` với suspending `enqueue` và non-blocking `tryEnqueue`).
- Có dev/prod thật ở cấp build variant và hợp đồng cấu hình để kết nối backend riêng (`AppConfig`), kèm task kiểm tra phát hành `checkProdReleaseConfig`.
- Có lộ trình tích hợp GraphQL, Supabase và Firebase, kèm mức độ hoàn thiện được công bố rõ ràng: đã kiểm chứng 100% qua repository contracts và fake adapters (29 unit tests); live cloud SDKs nằm trong roadmap backlog.
- Cung cấp bộ Collection Components dùng chung cho Jetpack Compose (`AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid`) với đầy đủ các slot trạng thái: initial loading, initial error/retry, empty, refresh, append loading, append error/footer retry, end-of-list, đồng bộ với `ErrorPresentationMode` và i18n `UiText` (P9).
- Bảo toàn các hành vi đã có: cancellation, session recovery, bản nháp qua `SavedStateHandle`, mutation/observation độc lập và navigation arguments.

**Phạm vi của tài liệu hiện tại:** Đóng vai trò kim chỉ nam kiến trúc (Living Architecture Document), phản ánh chính xác hiện trạng triển khai của codebase (140 unit tests pass, boundaries pass, 4 variants compile và CI hoạt động), đồng thời vạch rõ ranh giới giữa các thành phần đã kiểm chứng và các hạng mục roadmap trong tương lai.

## 2. Căn cứ từ code hiện tại

### 2.1. Ma trận năng lực kiến trúc (Capability Matrix)

| Năng lực / Thành phần | Trạng thái | Minh chứng / Bằng chứng kiểm thử | Vị trí sở hữu |
| --- | --- | --- | --- |
| **Module Boundaries & 5 Layer Core** | Đã kiểm chứng (Verified) | `verifyModuleBoundaries` kiểm tra dependency allowlist chặt chẽ cho cả 5 modules (`app`, `domain`, `data`, `shared`, `resources`) | Root `build.gradle.kts` |
| **Build Variants (dev/prod x debug/release)** | Đã kiểm chứng (Verified) | Suffix `.dev` và `.debug`, 4 variants: `devDebug`, `devRelease`, `prodDebug`, `prodRelease`; task `checkProdReleaseConfig` chặn demo fallback | `app/build.gradle.kts` (P1) |
| **Cấu hình môi trường & URL Injection** | Đã kiểm chứng (Verified) | `AppConfig`, `AppEnvironment`, URL validation, Retrofit injection qua `DataModule`, 6 tests trong `AppConfigTest` | `app/src/main/java/com/example/base/config/AppConfig.kt` (P1) |
| **Hợp đồng Input/Output Use Case** | Đã kiểm chứng (Verified) | `BaseInput`, `BaseOutput`, `NoInput`, `NoOutput`, `AddTodoInput/Output`, `GetTodosOutput`, `GetUsersInput/Output`, 17 tests trong `UseCaseContractsTest` | `domain/src/main/java/com/example/base/domain/usecase/base/` (P2) |
| **Lỗi nghiệp vụ có cấu trúc** | Đã kiểm chứng (Verified) | `ValidationError(field, code, params)`, xóa hardcoded tiếng Việt trong `AppException`, 6 tests trong `AppExceptionTest` | `shared/src/main/java/com/example/base/shared/error/` (P2) |
| **Flow & Coroutine Concurrency Policies** | Đã kiểm chứng (Verified) | `throttleFirst`, `droppable`, `sequential` (suspending `enqueue` & `tryEnqueue`), `debounceWithLatest`, 15 virtual time tests trong `FlowPoliciesTest` | `shared/src/main/java/com/example/base/shared/flow/` (P5) |
| **ActionRunner & Loading Tracking** | Đã kiểm chứng (Verified) | `ActionRunner` và `LoadingManager` độc lập ViewModel, reference counting loading theo `LoadingKey`, 12 tests trong `ActionRunnerTest` | `shared/src/main/java/com/example/base/shared/action/` (P5) |
| **Toolkit ViewModel & UI Presentation** | Đã kiểm chứng (Verified) | `UiErrorMessageMapper`, `ScreenUiController`, `ErrorPresentationMode`, `UserViewModel` không kế thừa BaseViewModel, điều phối search latestRequestId, 10 tests trong `BaseViewModelTest`, 10 tests trong `UserViewModelTest` | `app/src/main/java/com/example/base/ui/base/`, `app/src/main/java/com/example/base/ui/user/` (P3) |
| **Đa ngôn ngữ i18n (en / vi)** | Đã kiểm chứng (Verified) | Resource strings đối xứng 38 keys (trong file 53 dòng) tại `resources/src/main/res/values/strings.xml` và `values-vi/strings.xml`, `UiErrorMessageMapper` (8 tests trong `UiErrorMessageMapperTest` hỗ trợ en/vi), `UiText` deferred resolution với context tại UI | `resources/`, `app/src/main/java/com/example/base/ui/base/` (P4) |
| **Tích hợp Backend ngoài REST (GraphQL, Supabase, Firebase)** | Đã kiểm chứng Contract & Fake Adapters (Verified on Fakes) | MockWebServer REST switching (6 tests), GraphQL adapter với transport & partial errors (8 tests), Supabase CRUD/Auth/RLS (8 tests), Firebase CRUD/realtime cleanup (7 tests). Tổng cộng 29 unit tests. Live cloud SDKs với backend thật giữ trong backlog | `data/src/main/java/com/example/base/data/integration/` (P6) |
| **Session Restoration & Expiration** | Đã kiểm chứng (Verified) | DataStore session snapshot, retryable restore, expiration persistence, 6 tests trong `DataStoreSessionManagerTest` & `DataStoreSessionFailureTest`, draft persistence qua `SavedStateHandle` (7 tests trong `TodoViewModelTest`) | `app/src/main/java/com/example/base/session/` (P7) |
| **CI Automation & Variant Testing** | Đã kiểm chứng (Verified) | `.github/workflows/ci.yml` định nghĩa hoàn chỉnh và sẵn sàng cho GitHub Actions remote execution; xác thực cục bộ chạy các lệnh Gradle tương ứng | `.github/workflows/ci.yml` (P8) |
| **List/Grid và paged collection UI** | Quy hoạch chi tiết (Architecture Detailed; sẵn sàng triển khai) | Đặc tả hợp đồng `CollectionLoadState`, `PagedListUiState<T>`, API signature `AppList`/`AppPagedList`, scroll prefetch & generation cancellation | `app/src/main/java/com/example/base/ui/components/collection/` (P9) |

### 2.2. Hiện trạng các hạng mục

| Hạng mục | Hiện trạng quan sát được | Hướng xử lý |
| --- | --- | --- |
| Module | 5 modules: `app`, `domain`, `data`, `shared`, `resources` | Duy trì 5 modules làm core; `data` giữ là pure Kotlin/JVM |
| Startup | `SessionState` (`Loading`, `Ready`, `Error`), `DataStoreSessionManager` tự phục hồi session | Duy trì cơ chế hiện có, không tái tạo framework phức tạp |
| Base use case | `BaseUseCase`, `BaseFlowUseCase`, `BaseSyncUseCase`, `BasePagingUseCase` | Chuẩn hóa toàn bộ tham số vào/ra qua `BaseInput`/`BaseOutput` |
| Input/Output | `AddTodoInput/Output`, `GetTodosOutput`, `GetUsersInput/Output`, `NoInput/NoOutput` | Duy trì hợp đồng tường minh, kiểm thử chặt chẽ tại `UseCaseContractsTest` |
| ViewModel | Hỗ trợ 2 mô hình: kế thừa `BaseViewModel` (`TodoViewModel`, `UsersViewModel`) hoặc composition thuần với Android `ViewModel` (`UserViewModel`) | Khuyến khích composition; toolkit `ActionRunner`/`ScreenUiController` dùng độc lập |
| Lỗi & UI Presentation | `AppException`, `ValidationError`, `ErrorPresentationMode` (Auto, Dialog, Snackbar, Inline, ScreenError, Custom, None), mapping qua `DefaultUiErrorMessageMapper(context)` | Lỗi nghiệp vụ có cấu trúc; presentation mapping giải quyết `UiText` tại composable scope |
| Todo & Users | Flow là nguồn dữ liệu; bản nháp dùng `SavedStateHandle`; Users phân trang và search điều phối `latestRequestId` | Duy trì làm regression test suite mẫu |
| Collection & Paging UI | Todo và Users dùng LazyColumn trực tiếp; Users có nút Load more thủ công, lỗi append thiếu footer retry tại chỗ | Chuẩn hóa `AppList`/`AppGrid` và `AppPagedList`/`AppPagedGrid` tái sử dụng; giữ nguyên tùy chọn dùng LazyColumn nguyên bản cho màn hình đơn giản |
| BaseScreen | `BaseScreenScaffold` mở, nhận `ScreenUiController` hoặc state/callbacks trực tiếp; LaunchedEffect key theo ID effect đầu tiên | Không bắt buộc bọc `BaseViewModel`; không cancel/restart effect khi có effect mới |
| Môi trường | 4 variants (`devDebug`, `devRelease`, `prodDebug`, `prodRelease`), `AppConfig` injection, task `checkProdReleaseConfig` chặn fallback demo | Inject cấu hình qua Dagger Hilt `DataModule`, URL validation nghiêm ngặt |
| i18n | `strings.xml` mặc định (en) và `values-vi/strings.xml` (38 keys đối xứng trong file 53 dòng), `UiText` deferred resolution với context UI | Đã hoàn thành core i18n; Per-app language API và date/number formatters trong backlog |
| Backend ngoài REST | Hợp đồng repository trả domain model; 3 adapters (GraphQL, Supabase, Firebase) kèm fake clients (29 unit tests) | Duy trì 100% test pass trên fake adapters; live cloud SDK smoke test trong backlog |
| Chất lượng kiểm thử | **140 tests PASS (0 failures, 0 errors)** trên cả 5 modules: `:app` (50), `:data` (36), `:domain` (17), `:shared` (37) | Duy trì regression test suite và chạy tự động trên CI |
| Lint & CI | CI workflow `.github/workflows/ci.yml` sẵn sàng cho GitHub Actions remote execution; xác thực local qua các Gradle tasks | Bổ sung task lint theo variant và dọn dẹp Compose lint warnings trong backlog |

Bản Flutter tham khảo tại `D:/AppData/Code/base/flutter_bloc_clean`:
- `domain/lib/src/usecase/base/io` và các use case có Input/Output riêng.
- `app/lib/base/bloc/base_bloc.dart` có tùy chọn loading/error/retry.
- `app/lib/base/bloc/mixin/event_transformer_mixin.dart` có các chiến lược xử lý sự kiện.
- `app/lib/exception_handler/exception_message_mapper.dart` tách lỗi khỏi bản dịch.

Không coi danh sách tính năng trong README của một base là bằng chứng mọi tích hợp đã hoạt động. Ví dụ, bản Flutter cục bộ có tài liệu/schema GraphQL nhưng một số dependency GraphQL đang được comment. Mỗi capability của bản Kotlin cần có trạng thái và bằng chứng kiểm thử riêng biệt (được xác thực qua 140 tests).

## 3. Nguyên tắc thiết kế đã chọn

1. **Composition over Inheritance:** Cơ chế dùng chung (`ActionRunner`, `LoadingManager`, `ScreenUiController`) có thể gọi độc lập; `BaseViewModel` chỉ là lớp tiện ích tùy chọn.
2. **Domain Isolation:** Domain không biết `Context`, `Resources`, `NavController`, `BuildConfig`, Retrofit hay SDK backend ngoài.
3. **Hợp đồng bất biến:** `BaseInput`/`BaseOutput` là interface thông thường; data class tạo các hợp đồng bất biến. Không dùng sealed interface cho marker cần được triển khai ở nhiều package/module.
4. **Tách biệt DTO/Entity:** Entity và DTO không bắt buộc triển khai `BaseOutput`. Output của nghiệp vụ bọc entity khi cần.
5. **Đơn nhất Result type:** Public use case tiếp tục trả `Result<Output>` hoặc `Flow<Result<Output>>`; không đưa một kiểu Result thứ hai vào dự án.
6. **Bảo toàn Cancellation:** `CancellationException` luôn được ném lại; việc hủy coroutine không được diễn giải thành hoàn tác mutation ở server.
7. **Bảo vệ Mutation khi Retry:** Retry mặc định không tự động replay mutation (`allowRetry = false`); loading và cách trình bày lỗi là những quyết định độc lập.
8. **Main-safety tại nguồn:** Nơi trực tiếp thực hiện blocking I/O hoặc tính toán nặng chịu trách nhiệm main-safety và chọn dispatcher qua `DispatcherProvider`. Không dựa hoàn toàn vào một mặc định IO ở lớp base.
9. **Single Source of Truth:** Giữ source of truth trong repository; ViewModel không ghép một lần ghi và một lần đọc mới thành một kết quả mutation.
10. **Linh hoạt Presentation:** Không bắt mọi màn hình có chung một UiState khổng lồ hoặc bọc `BaseScreen`. Màn hình tự do chọn `ErrorPresentationMode` (Inline, Dialog, Snackbar, ScreenError).
11. **Deferred Resolution cho i18n:** Lỗi và thông báo trì hoãn việc lấy chuỗi hiển thị qua `UiText` (`UiText.StringResource` / `UiText.DynamicString`), resolve đúng locale tại composable scope thông qua `LocalContext.current`.
12. **Bảo vệ ranh giới Adapter ngoài REST:** SDK/generated models của backend ngoài (GraphQL, Supabase, Firebase) không rò rỉ qua repository contract tới domain/UI. Module `data` giữ là pure Kotlin/JVM library; các adapter được kiểm chứng bằng fake clients độc lập với cloud SDKs.

## 4. Dev/prod trong Android: thiết kế mục tiêu

### 4.1. Hai khái niệm độc lập

- **Product flavor** chọn môi trường: dev và prod; prod là viết tắt của production.
- **Build type** chọn cách đóng gói: debug và release.
- **Build variant** là tổ hợp, ví dụ `devDebug` hoặc `prodRelease`.
- Đây không phải hai nhánh Git, cũng không phải hai bản sao codebase.
- Chọn prod không tự biến app thành bản đã ký/phát hành; release cũng không đồng nghĩa dùng backend production.

Một flavor dimension tên `environment` được tạo riêng tại module `app`. Các module JVM (`domain`, `data`, `shared`, `resources`) không cần flavor dev/prod.

### 4.2. Ma trận build variants

Để tránh bản debug ghi đè app đang dùng, áp dụng suffix `.dev` cho flavor dev và `.debug` cho build type debug. Namespace/package Kotlin giữ nguyên `com.example.base`.

| Variant | Backend | Application ID mẫu | Mục đích |
| --- | --- | --- | --- |
| `devDebug` | dev | `com.example.base.dev.debug` | Lập trình và kiểm thử hằng ngày |
| `devRelease` | dev | `com.example.base.dev` | Kiểm tra hành vi bản release với dữ liệu dev |
| `prodDebug` | prod | `com.example.base.debug` | Chẩn đoán có chủ đích với backend prod |
| `prodRelease` | prod | `com.example.base` | Artifact phát hành chính thức |

- Tên app thể hiện môi trường và build type để phân biệt khi cài song song trên thiết bị.
- Không đưa `prodDebug` vào luồng phát hành; không mặc định mở app với prod.
- Mỗi application ID có sandbox dữ liệu riêng. Debug không đọc được session của `prodRelease`.
- Khi thêm nhà cung cấp xác thực, deep link hoặc Firebase, đăng ký đúng app ID và chứng thư của từng variant cần dùng.
- Việc ký là độc lập với suffix: không dùng debug key cho bản phát hành thật.

### 4.3. Cấu hình và dependency injection

- `AppEnvironment` và `AppConfig` bất biến tại application composition root (`app/src/main/java/com/example/base/config/AppConfig.kt`).
- Flavor cung cấp môi trường, REST base URL và các endpoint/public client config cần thiết.
- Module `app` đọc `BuildConfig` rồi inject cấu hình vào client; `data`/`domain` không đọc `BuildConfig` của app.
- Endpoint được inject động qua `DataModule`, loại bỏ hoàn toàn hardcoded URL.
- Nguồn cấu hình local dùng file bị ignore; CI dùng biến môi trường hoặc Gradle properties.
- Thứ tự ưu tiên: tham số Gradle rõ ràng, biến môi trường CI, file local, cấu hình demo công khai đã khai báo.
- Validate URL, scheme, trailing slash khi Retrofit yêu cầu và cấu hình bắt buộc của variant được build.
- Không âm thầm fallback prod sang endpoint dev.
- Task `checkProdReleaseConfig` được tích hợp vào `assembleProdRelease` và `bundleProdRelease`, tự động chặn và ném `GradleException` nếu bản phát hành sử dụng JSONPlaceholder demo URL mà không có cờ cho phép rõ ràng (`-PallowDemoProd=true` hoặc `-PDEMO_MODE=true`).

### 4.4. Signing, logging và thông tin nhạy cảm

- Debug dùng debug signing tự động của Android SDK. Release không có key thì báo rõ artifact unsigned; unsigned không được xem là sẵn sàng phát hành.
- Signing release đọc từ cấu hình CI/local an toàn; không commit keystore, mật khẩu, token người dùng hay private/admin key vào Git.
- `BuildConfig` và file đóng gói trong APK có thể bị trích xuất, chỉ dùng cho thông tin client được phép công khai. Trường `BuildConfig.DEMO_MODE` phân biệt rõ ràng giữa cấu hình demo và production thực tế.
- Supabase publishable/anon key phải theo mô hình phân quyền backend (Row Level Security); service-role/admin key không được đưa vào ứng dụng Android.
- Logging không in password, token, header Authorization hoặc toàn bộ Input/Output nhạy cảm.
- Bật/tắt logging theo build type và cấu hình rõ ràng qua `AppLogger`.
- Kiểm tra release và cấu hình ProGuard/R8 trước khi thay đổi minification.

### 4.5. Hướng dẫn lệnh Gradle cho build variants

Trong Android Studio: mở cửa sổ **Build Variants**, chọn `devDebug` để làm việc thường nhật; đổi sang variant cần kiểm tra rồi chạy/build.

Các lệnh Gradle chuẩn cho các variants:

~~~powershell
.\gradlew.bat :app:assembleDevDebug
.\gradlew.bat :app:assembleDevRelease
.\gradlew.bat :app:assembleProdDebug
.\gradlew.bat :app:assembleProdRelease
.\gradlew.bat :app:bundleProdRelease
~~~

Các lệnh assemble trên đã hoạt động đầy đủ trong Gradle và CI. Đối với `assembleProdRelease`, nếu chưa cấu hình URL production thật, phải truyền tham số demo `-PallowDemoProd=true` để vượt qua task kiểm tra `checkProdReleaseConfig`. Phân biệt assemble APK, bundle AAB và tạo artifact có signing hợp lệ (release signing automation nằm trong backlog P8).

## 5. Các giai đoạn triển khai

Mỗi giai đoạn là một thay đổi độc lập, có thể review và chạy kiểm tra trước khi chuyển sang bước phụ thuộc.

### P0 — Chốt baseline và danh mục năng lực

- [x] Ghi snapshot build/test/lint hiện tại, cả test count và giới hạn kiểm chứng.
- [x] Lập capability matrix với trạng thái: đã kiểm chứng, có ví dụ, đang thiết kế, chờ tích hợp.
- [x] Ghi các contract phải giữ: session recovery, effect acknowledgment, draft, cancellation, mutation/observation.
- [x] Audit tương thích Kotlin/AGP/Gradle/JDK/Compose lint; nếu cần nâng phiên bản, thực hiện trong thay đổi toolchain riêng.
- [x] Không tự gộp migration AGP lớn với refactor kiến trúc.

**Đầu ra:** Baseline có thể tái lập; tài liệu cập nhật số lượng test thực tế (140 tests pass) thay thế số liệu cũ (34/35/103 tests).

### P1 — Môi trường dev/prod và build variants

Phụ thuộc P0.

- [x] Thêm environment dimension và ma trận bốn variant ở mục 4 (`devDebug`, `devRelease`, `prodDebug`, `prodRelease`).
- [x] Tạo config local mẫu không chứa bí mật; cập nhật `.gitignore` và hướng dẫn CI input.
- [x] Inject `AppConfig` vào các client qua Hilt; dùng cấu hình cho endpoint trong `DataModule`.
- [x] Thêm validation cấu hình của variant được yêu cầu (`AppConfigTest`: 6 tests).
- [x] Thêm task kiểm tra phát hành `checkProdReleaseConfig` chặn việc build `prodRelease` với endpoint demo mặc định nếu không có cờ cho phép tường minh (`-PallowDemoProd=true` hoặc `-PDEMO_MODE=true`).
- [x] Cập nhật app label, artifact naming, `DEMO_MODE` buildConfigField và metadata môi trường.
- [x] Chuyển tài liệu/test tasks từ Debug chung sang tên variant chính xác (`testDevDebugUnitTest`, `testProdDebugUnitTest`).
- [x] Mô tả signing release và phân biệt demo/prod thật.

**Nghiệm thu:** Bốn variant compile/package được; MockWebServer test chứng minh URL switching dev/prod độc lập (`RestEnvironmentMockWebServerTest`: 6 tests); task `checkProdReleaseConfig` ném `GradleException` khi build prodRelease thiếu cấu hình production thực.

### P2 — Hợp đồng Input/Output và lỗi nghiệp vụ

Phụ thuộc P0.

- [x] Thêm `BaseInput`, `BaseOutput` và `NoInput`/`NoOutput` cho thao tác không có dữ liệu (`domain/src/main/java/com/example/base/domain/usecase/base/`).
- [x] Chuyển `AddTodo` sang `AddTodoInput(title)` và `AddTodoOutput(todo)`.
- [x] Chuyển toggle, observation và Users sang hợp đồng có tên tương ứng (`GetTodosOutput`, `GetUsersInput/Output`).
- [x] Với paging, tách `PageResult`/`PagingInput` vào model/contract dùng bởi repository.
- [x] Đầu ra paging chuẩn hóa: `GetUsersOutput(users, page, totalPages)`.
- [x] Giữ public `Result<Output>` / `Flow<Result<Output>>`; không tạo hệ thống Result song song.
- [x] Không bọc `Result` lồng nhau.
- [x] Chuẩn hóa lỗi validation có `field`/`code` và tham số (`ValidationError` trong `shared/src/main/java/com/example/base/shared/error/AppException.kt`); chuyển chuỗi lỗi tìm kiếm sang resource `UiText.StringResource(R.string.error_search_query_empty)`.
- [x] Rà mapper lỗi: lỗi tham số nghiệp vụ không bị quy thành lỗi parse dữ liệu server (`ErrorMapper.kt`).
- [x] Chuẩn hóa logger/dispatcher injection giữa các use case (`AppLogger`, `DispatcherProvider`).

**Nghiệm thu:** Toàn bộ 17 tests trong `UseCaseContractsTest` PASS; domain không chứa Android `Context`/`Resources`; validation, error identity và cancellation được bảo toàn.

### P3 — Toolkit thao tác độc lập với kế thừa ViewModel

Phụ thuộc P2.

- [x] Tách runner xử lý action/retry/cancellation, trạng thái loading theo key (`LoadingKey`, `LoadingManager`, `ActionRunner` trong `shared/src/main/java/com/example/base/shared/action/`).
- [x] `BaseViewModel` sử dụng các helper trên qua composition (`app/src/main/java/com/example/base/ui/base/BaseViewModel.kt`).
- [x] ViewModel kế thừa trực tiếp `androidx.lifecycle.ViewModel` vẫn dùng được toolkit qua composition (`app/src/main/java/com/example/base/ui/user/UserViewModel.kt`).
- [x] Điều phối search submit, keystroke debounce và refresh qua `latestRequestId` (AtomicLong), hủy coroutine search đang chạy khi có query mới và loại bỏ kết quả cũ hoàn thành muộn (`UserViewModel.kt`).
- [x] Scope do caller cung cấp (`ActionRunner.launch(scope)`, `droppable(scope)`, `sequential(scope)`); không tạo scope sống vô hạn trong helper.
- [x] Hỗ trợ loading: overlay chung (`BaseScreen`), indicator cục bộ (local loading key / pagination loading), caller tự xử lý hoặc không hiển thị.
- [x] Hỗ trợ error presentation: dialog, snackbar, inline/form, screen error, custom hoặc caller tự xử lý (`ErrorPresentationMode` trong `app/src/main/java/com/example/base/ui/base/ErrorPresentationMode.kt`).
- [x] Inline/form và screen error cập nhật state của feature qua callback (`UserViewModel.searchFieldError`); runner không tự sở hữu cấu trúc form.
- [x] Tách retry policy khỏi nơi hiển thị lỗi; giữ mặc định không replay mutation (`allowRetry = false`).
- [x] Loại bỏ quy tắc mọi `ValidationError` đều tự động thành dialog; screen chọn policy qua `ErrorPresentationMode`.
- [x] `BaseScreen` nhận state/callback hoặc controller UI (`ScreenUiController`) thay vì bắt buộc nhận `BaseViewModel` (`app/src/main/java/com/example/base/ui/base/BaseScreen.kt`).
- [x] Cố định `LaunchedEffect` key trong `BaseScreen.kt` theo `pendingEffects.firstOrNull()?.id`, ngăn ngừa việc effect đang hiển thị bị cancel và restart khi có effect mới đến hàng đợi.
- [x] Giữ một nơi sở hữu scaffold/insets (`BaseScreenScaffold`); screen có thể dùng UI riêng và không bọc `BaseScreen`.
- [x] Critical session state tiếp tục độc lập với snackbar/dialog; không đưa auth trở lại base chung (`AppNavHost.kt`).

**Nghiệm thu:**
- Có ví dụ kế thừa `BaseViewModel` (`TodoViewModel`, `UsersViewModel`) và ví dụ kế thừa trực tiếp `androidx.lifecycle.ViewModel` (`UserViewModel`).
- 10 tests trong `BaseViewModelTest`, 10 tests trong `UserViewModelTest` (kiểm tra triệt để tính năng hủy in-flight search và tránh ghi đè kết quả cũ), và 12 tests trong `ActionRunnerTest` xác nhận reference counting loading và retry policy hoạt động chính xác.

### P4 — i18n hoàn chỉnh cho UI và lỗi

Phụ thuộc P2/P3.

- [x] Chọn ngôn ngữ mặc định (tiếng Anh) và cung cấp bộ tiếng Anh/tiếng Việt đối xứng 38 keys trong file 53 dòng (`resources/src/main/res/values/strings.xml` và `resources/src/main/res/values-vi/strings.xml`).
- [x] Dùng `strings.xml`, resource theo locale, placeholders (`%1$d`, `%1$s`).
- [x] Tạo mapper presentation từ loại/mã lỗi sang resource; domain không giữ Context hoặc resource ID (`UiErrorMessageMapper`, `DefaultUiErrorMessageMapper` trong `app/src/main/java/com/example/base/ui/base/UiErrorMessageMapper.kt`).
- [x] Cung cấp context qua `LocalContext.current` trong `BaseScreen.kt`, `DefaultAppEffectHandler` và `UsersScreen.kt` để snackbar, dialog và màn hình giải quyết chuỗi dịch i18n chính xác thay vì fallback sang raw exception message.
- [x] Áp dụng cùng mapping cho snackbar, dialog, inline field error và screen error.
- [x] Đưa title, Retry, OK, loading/empty/session messages và validation errors (`error_search_query_empty`) vào resources.
- [x] Đảm bảo thông báo đang chờ được resolve theo locale hiện tại qua deferred resolution (`UiText.StringResource` / `UiText.DynamicString` trong `app/src/main/java/com/example/base/ui/base/UiText.kt`).
- [ ] *Genuinely Pending (Roadmap):* Tích hợp Per-app language preferences API (Android 13+ `LocaleManager` và `AppCompatDelegate.setApplicationLocales()` kèm fallback Android cũ).
- [ ] *Genuinely Pending (Roadmap):* Bổ sung ví dụ định dạng số/tiền tệ/ngày tháng theo locale (`NumberFormat`, `DateTimeFormatter`).

**Nghiệm thu:**
- 8 tests trong `UiErrorMessageMapperTest` chứng minh mapping đầy đủ các mã lỗi sang string resource ID tương ứng trên cả 2 locale tiếng Anh và tiếng Việt.
- Đổi configuration không làm mất pending effect; deferred resolution qua `UiText` bảo đảm chuỗi hiển thị đúng ngôn ngữ active.

### P5 — Flow/coroutine policies và ví dụ tương đương RxDart

Phụ thuộc P2/P3.

- [x] Thêm ví dụ search: debounce, distinctUntilChanged và latest-result policy (`debounceWithLatest`).
- [x] Dùng `mapLatest`/`flatMapLatest` đúng với kết quả suspend/Flow; test response cũ không ghi đè response mới.
- [x] Định nghĩa rõ việc hủy trước hay sau cửa sổ debounce trong `debounceWithLatest`.
- [x] Thêm `throttleFirst` với cửa sổ thời gian và hành vi nhận phần tử đầu được định nghĩa rõ ràng.
- [x] Ví dụ `droppable`: bỏ lần nhấn mới khi tác vụ đang chạy (`Droppable.kt`).
- [x] Nâng cấp ví dụ `sequential`: cung cấp cả suspending `enqueue(...)`/`send(...)` và non-blocking `tryEnqueue(...)`: Boolean. Khi buffer có giới hạn và dùng `BufferOverflow.SUSPEND`, caller sẽ tạm dừng chờ dung lượng thay vì âm thầm bỏ rơi thao tác (`Sequential.kt`).
- [x] Bổ sung `SequentialQueue` / `sequentialQueue(...)` cho phép xếp hàng các suspending lambdas tuần tự an toàn.
- [x] Giới hạn concurrency thay vì khởi chạy coroutine không kiểm soát.
- [x] Không tạo alias chỉ đổi tên operator chuẩn; helper mới bổ sung chính sách xử lý cụ thể.
- [x] Làm rõ lifecycle collection theo nguồn: repository Todo observation phát Flow hỗ trợ `WhileSubscribed`; `TodoViewModel` hiện collect trong `viewModelScope` để duy trì state trong suốt vòng đời ViewModel, trong khi observation chỉ phục vụ tầng UI có thể thu nhận qua `WhileSubscribed`.

**Nghiệm thu:** Toàn bộ 15 virtual time tests trong `FlowPoliciesTest` PASS (kiểm tra triệt để debounce, throttleFirst, droppable, sequential suspend enqueue / tryEnqueue và cancellation semantics).

### P6 — Hợp đồng data và bộ tích hợp ngoài REST

Phụ thuộc P1/P2; UI sample phụ thuộc P3/P4.

Deliverable bao gồm hợp đồng repository trừu tượng và ba adapter độc lập. Không xây dựng "universal backend client" làm mờ nhạt semantics khác biệt của từng nền tảng.

| Gói | Công việc | Bằng chứng hoàn thành | Vị trí source |
| --- | --- | --- | --- |
| **REST hiện có và bước tích hợp tiếp** | JSONPlaceholder: config/DTO/error mapping/timeout/retry hiện có; Practice API của người dùng mới đọc contract, chưa tích hợp | `RestEnvironmentMockWebServerTest` (6 tests hiện có); auth rotation/Products cần test riêng khi triển khai | `data/.../integration/rest/`; [kế hoạch Practice API](rest-api-integration-plan.md) |
| **GraphQL** | Schema/query hợp đồng, phân biệt transport error, GraphQL errors và partial data policy | `GraphQLAdapterTest` (8 tests) | `data/.../integration/graphql/` |
| **Supabase** | Adapter CRUD, Auth error mapping, RLS (Row Level Security) mapping | `SupabaseAdapterTest` (8 tests) | `data/.../integration/supabase/` |
| **Firebase** | Firestore CRUD contract, realtime snapshot listener cleanup khi hủy coroutine scope | `FirebaseAdapterTest` (7 tests) | `data/.../integration/firebase/` |

- [x] SDK/generated models không đi qua repository contract tới ViewModel (`GraphQLAdapter`, `SupabaseAdapter`, `FirebaseAdapter` map về domain `Todo`, `User`, `BaseOutput`).
- [x] Repository trả domain model, `Result` và `Flow` theo hợp đồng; không bắt mọi nguồn có semantics HTTP.
- [x] Module `data` giữ là pure Kotlin/JVM library, không ép chuyển thành Android library.
- [x] Provider không dùng không yêu cầu key/config khi build core và không khởi tạo lúc startup.
- [x] Cấu hình cloud dev/prod độc lập (`FirebaseConfig`, `SupabaseConfig`, `RestEnvironmentConfig`), validate invariants; không dùng chung dữ liệu thật cho test.
- [x] Không tự tạo tài khoản cloud, deploy schema/rules hoặc gọi mutation production trong quá trình kiểm thử (100% qua MockWebServer và fake client adapters).
- [ ] *Genuinely Pending (Roadmap):* Tích hợp dependency SDK cloud thật (Apollo Kotlin runtime, Supabase Kotlin SDK, Firebase Android BOM) và smoke test trên project dev thật khi có cấu hình/credentials.
- [ ] *Genuinely Pending (Roadmap):* Bổ sung tài liệu setup chi tiết từng backend cloud và cơ chế feature-flag toggle adapter khi bật live SDK.

**Nghiệm thu:** 29 tests tích hợp PASS trong `:data` (`RestEnvironmentMockWebServerTest`: 6, `GraphQLAdapterTest`: 8, `SupabaseAdapterTest`: 8, `FirebaseAdapterTest`: 7); thay thế adapter không làm thay đổi domain contracts hay UI layer; listener realtime được giải phóng hoàn toàn khi scope hủy. Live cloud SDKs được xác định là backlog roadmap tiếp theo.

### P7 — Persistence, session/auth và khả năng dùng cho sản phẩm

Phụ thuộc P1/P2.

- [x] Giữ session snapshot/retry restore hiện có và kiểm tra cô lập giữa app ID (`DataStoreSessionManager.kt`, `DataStoreSessionManagerTest`: 2 tests, `DataStoreSessionFailureTest`: 4 tests).
- [x] Lưu bền bản nháp (draft) qua process recreation bằng `SavedStateHandle` (`TodoViewModel.kt`, `TodoViewModelTest`: 7 tests).
- [ ] *Genuinely Pending (Roadmap):* Thêm Room/SQLite local database và cache invalidation policy cho domain models.
- [ ] *Genuinely Pending (Roadmap):* Quy định offline-first, stale-while-revalidate data và sync queue mutation có kiểm soát xung đột.
- [ ] *Genuinely Pending (Roadmap):* Thiết kế single-flight token refresh coordinator cho REST client (tránh concurrent refresh, replay giới hạn).
- [ ] *Genuinely Pending (Roadmap):* Phân quyền auth SDK so với REST refresh interceptor.
- [ ] *Genuinely Pending (Roadmap):* Mã hóa storage cho credentials nhạy cảm trên thiết bị (Android KeyStore / EncryptedSharedPreferences thay vì plain DataStore).
- [ ] *Genuinely Pending (Roadmap):* Kiểm thử route deep link và navigation saved state qua process recreation.
- [ ] *Genuinely Pending (Roadmap):* Ghi rõ các extension ngoài phạm vi đầu: background sync, upload/download, push notification, analytics/crash reporting, remote config.

**Nghiệm thu:** Session khôi phục an toàn qua DataStore snapshot; coroutine retry khôi phục phiên khi gặp lỗi tạm thời; draft text trong form được giữ nguyên qua process death bằng `SavedStateHandle`.

### P8 — CI, release và kiểm chứng hiệu quả

Bắt đầu CI tối thiểu ngay sau P1; mở rộng theo các giai đoạn sau.

- [x] CI chạy boundaries, JVM tests và app tests theo variant phù hợp (`.github/workflows/ci.yml`).
- [x] Ma trận tối thiểu: devDebug và prodDebug cho test & assemble (`.github/workflows/ci.yml`).
- [x] Dùng mock/fake cho PR/CI không đòi hỏi tài khoản cloud (xác thực 100% offline).
- [x] Cập nhật tài liệu kỹ thuật architecture, testing và workflow; cập nhật baseline chính xác lên 140 tests.
- [x] Định nghĩa CI rõ ràng: workflow đã được cấu hình trong `.github/workflows/ci.yml` sẵn sàng chạy từ xa trên GitHub Actions; môi trường cục bộ sử dụng các lệnh Gradle chuẩn tương ứng.
- [ ] *Genuinely Pending (Roadmap):* Chạy lint theo variant trong CI (`lintDevDebug`, `lintProdRelease`) và dọn dẹp Compose lint warnings.
- [ ] *Genuinely Pending (Roadmap):* Cloud smoke tests thành workflow riêng với cấu hình dev secrets.
- [ ] *Genuinely Pending (Roadmap):* Signing release automation và xuất bản AAB/APK với signing hợp lệ.
- [ ] *Genuinely Pending (Roadmap):* Bật R8 và cấu hình quy tắc ProGuard/consumer-rules cho release.
- [ ] *Genuinely Pending (Roadmap):* Đo baseline runtime profiling (startup time, frame drops/scrolling, memory footprint).

**Nghiệm thu:** CI workflow `.github/workflows/ci.yml` đã được định nghĩa đầy đủ, kiểm tra boundary, chạy 140 unit tests và biên dịch artifact mà không phụ thuộc bất kỳ bí mật bên ngoài nào đối với core tests.

### P9 — List/Grid dùng chung và giao diện phân trang (Collection & Pagination UI)

**Trạng thái: Quy hoạch kiến trúc chi tiết (Architecture Detailed; sẵn sàng triển khai)**. Bổ sung và hoàn thiện toàn diện từ review ngày 15/11/2026 theo yêu cầu của người dùng. P9 kế thừa và đồng bộ 100% với các hợp đồng đã hoàn thành tại P2 (Input/Output), P3 (Toolkit không ép kế thừa), P4 (i18n `UiText`) và P5 (Concurrency policies).

#### 1. Căn cứ và Đối chiếu Kiến trúc

- **Bản Flutter tham chiếu (`flutter_bloc_clean`):** Cung cấp `CommonPagedListView<T>`, `CommonPagedGridView<T>`, `CommonSliverPagedListView<T>` và `CommonPagingController<T>` tại `app/lib/common_view/paged_view/` với các slot trạng thái hoàn chỉnh (first load error, append error, empty, loading, end-of-list).
- **Hiện trạng Kotlin MVVM Base:**
  - `TodoScreen.kt` và `UsersScreen.kt` đang gọi `LazyColumn` trực tiếp trong từng màn hình.
  - `UsersViewModel` quản lý phân trang thủ công qua `loadNextPage()` và `UsersUiState.Success(items, page, totalPages, hasNext, isLoadingMore)`.
  - Khi append trang tiếp theo bị lỗi, lỗi chỉ được bắn ra dưới dạng `UiEffect.ShowSnackbar`, thiếu hẳn footer retry ngay tại cuối danh sách để người dùng bấm tải lại mà không làm gián đoạn danh sách đã tải.
  - Khi đổi search query, chưa có cơ chế generation ID đồng bộ với collection để hủy bỏ kết quả append của query cũ.
- **Mục tiêu P9:** Xây dựng bộ Composable collections dùng chung (`AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid`) độc lập, linh hoạt, tuân thủ Clean Architecture và Compose best practices:
  - Không ép buộc ViewModel phải kế thừa bất kỳ Base class nào (hoạt động tốt với cả `BaseViewModel` lẫn `androidx.lifecycle.ViewModel` thông thường).
  - Không đưa Android UI types hay Compose dependencies vào `:domain` hoặc `:data`.
  - Màn hình đơn giản vẫn tự do dùng `LazyColumn`/`LazyRow` nguyên bản của Compose mà không bị ép buộc bọc `AppList`.

#### 2. Mô hình trạng thái (State Modeling)

Toàn bộ trạng thái tải của collection được đóng gói vào các mô hình dữ liệu bất biến (immutable data classes/interfaces) đặt tại `app/src/main/java/com/example/base/ui/components/collection/model/`:

```kotlin
// Trạng thái tải của từng phân đoạn (first load, refresh, append)
sealed interface CollectionLoadState {
    object Idle : CollectionLoadState
    object Loading : CollectionLoadState
    data class Error(
        val error: AppException,
        val retry: () -> Unit
    ) : CollectionLoadState
}

// Trạng thái hoàn chỉnh cho danh sách có phân trang
data class PagedListUiState<T>(
    val items: List<T> = emptyList(),
    val firstLoadState: CollectionLoadState = CollectionLoadState.Idle,
    val refreshState: CollectionLoadState = CollectionLoadState.Idle,
    val appendLoadState: CollectionLoadState = CollectionLoadState.Idle,
    val isEndOfPagination: Boolean = false,
    val page: Int = 1,
    val generationId: Long = 0L
) {
    val isInitialLoading: Boolean get() = firstLoadState is CollectionLoadState.Loading
    val isRefreshing: Boolean get() = refreshState is CollectionLoadState.Loading
    val isAppending: Boolean get() = appendLoadState is CollectionLoadState.Loading
    val isEmpty: Boolean get() = items.isEmpty() && firstLoadState is CollectionLoadState.Idle
}
```

- **Tích hợp i18n:** Khi `CollectionLoadState.Error` xảy ra, đối tượng `AppException` được truyền kèm. Composable sẽ dùng `DefaultUiErrorMessageMapper(LocalContext.current)` để tự động chuyển đổi mã lỗi sang `UiText` được bản địa hóa (`en` hoặc `vi`).

#### 3. Đặc tả Hợp đồng Composable API (API Specifications)

Bộ collection composables được đặt tại `app/src/main/java/com/example/base/ui/components/collection/`:

##### 3.1. Danh sách và Lưới tiêu chuẩn (Non-paged `AppList` & `AppGrid`)
Dùng cho danh sách vừa và nhỏ không cần phân trang (ví dụ danh sách Todo, giỏ hàng, menu cài đặt):

```kotlin
@Composable
fun <T> AppList(
    items: List<T>,
    loadState: CollectionLoadState,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    itemKey: ((T) -> Any)? = null,
    contentType: ((T) -> Any)? = null,
    onRefresh: (() -> Unit)? = null,
    emptySlot: @Composable (() -> Unit)? = null,
    loadingSlot: @Composable (() -> Unit)? = null,
    errorSlot: @Composable ((AppException, () -> Unit) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
)

@Composable
fun <T> AppGrid(
    items: List<T>,
    columns: GridCells,
    loadState: CollectionLoadState,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    itemKey: ((T) -> Any)? = null,
    contentType: ((T) -> Any)? = null,
    onRefresh: (() -> Unit)? = null,
    emptySlot: @Composable (() -> Unit)? = null,
    loadingSlot: @Composable (() -> Unit)? = null,
    errorSlot: @Composable ((AppException, () -> Unit) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
)
```

##### 3.2. Danh sách và Lưới phân trang (`AppPagedList` & `AppPagedGrid`)
Dùng cho danh sách lớn cần chia trang từ backend (ví dụ Users, News feed, Product catalog):

```kotlin
@Composable
fun <T> AppPagedList(
    pagedState: PagedListUiState<T>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    prefetchDistance: Int = 3,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    itemKey: ((T) -> Any)? = null,
    contentType: ((T) -> Any)? = null,
    emptySlot: @Composable (() -> Unit)? = null,
    firstPageLoadingSlot: @Composable (() -> Unit)? = null,
    firstPageErrorSlot: @Composable ((AppException, () -> Unit) -> Unit)? = null,
    appendLoadingSlot: @Composable (() -> Unit)? = null,
    appendErrorSlot: @Composable ((AppException, () -> Unit) -> Unit)? = null,
    endOfPaginationSlot: @Composable (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
)

@Composable
fun <T> AppPagedGrid(
    pagedState: PagedListUiState<T>,
    columns: GridCells,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    prefetchDistance: Int = 4,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    itemKey: ((T) -> Any)? = null,
    contentType: ((T) -> Any)? = null,
    emptySlot: @Composable (() -> Unit)? = null,
    firstPageLoadingSlot: @Composable (() -> Unit)? = null,
    firstPageErrorSlot: @Composable ((AppException, () -> Unit) -> Unit)? = null,
    appendLoadingSlot: @Composable (() -> Unit)? = null,
    appendErrorSlot: @Composable ((AppException, () -> Unit) -> Unit)? = null,
    endOfPaginationSlot: @Composable (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
)
```

#### 4. Cơ chế Infinite Scroll & Xử lý Tranh chấp Concurrency

1. **Phát hiện cuộn thông minh (Scroll Prefetching):**
   - Không gọi `onLoadMore` trực tiếp trong recomposition.
   - Sử dụng `derivedStateOf` để quan sát vị trí item hiển thị cuối cùng:
     ```kotlin
     val shouldLoadMore = remember(state, prefetchDistance, pagedState.isAppending, pagedState.isEndOfPagination) {
         derivedStateOf {
             val lastVisibleItemIndex = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
             val totalItemsCount = state.layoutInfo.totalItemsCount
             totalItemsCount > 0 &&
                 lastVisibleItemIndex >= (totalItemsCount - 1 - prefetchDistance) &&
                 !pagedState.isAppending &&
                 !pagedState.isEndOfPagination &&
                 pagedState.appendLoadState !is CollectionLoadState.Error
         }
     }
     LaunchedEffect(shouldLoadMore) {
         snapshotFlow { shouldLoadMore.value }
             .filter { it }
             .collect { onLoadMore() }
     }
     ```
2. **Quản lý Thế hệ Request (Generation ID & Race Condition Guard):**
   - Khi người dùng đổi từ khóa tìm kiếm (query) hoặc làm mới danh sách (refresh), `generationId` tự động tăng lên 1 đơn vị.
   - Bất kỳ response append nào thuộc `generationId` cũ về muộn sẽ bị loại bỏ hoàn toàn, không bao giờ append nhầm dữ liệu vào danh sách mới.
3. **Bảo toàn vị trí cuộn & Chống tràn bộ nhớ:**
   - Hoist `LazyListState` / `LazyGridState` qua `rememberLazyListState()`.
   - **Tuyệt đối không lưu toàn bộ danh sách `List<T>` vào `SavedStateHandle`** để tránh văng app do lỗi `TransactionTooLargeException` của hệ điều hành Android khi danh sách có hàng trăm phần tử.
   - Chỉ lưu trữ các metadata tối thiểu trong `SavedStateHandle`: `page`, `searchQuery`, `firstVisibleItemIndex`, `firstVisibleItemScrollOffset`.

#### 5. Chiến lược Dual-Track Paging Engine

Để cân bằng giữa sự gọn nhẹ độc lập và khả năng mở rộng quy mô lớn, P9 định hình kiến trúc động cơ 2 làn (Dual-Track):

```
                     ┌────────────────────────────────────────┐
                     │          UI Presentation Layer         │
                     │  AppPagedList<T>  /  AppPagedGrid<T>   │
                     └───────────────────▲────────────────────┘
                                         │
                        Phụ thuộc PagedListUiState<T>
                                         │
        ┌────────────────────────────────┴────────────────────────────────┐
        │                                                                 │
┌───────┴────────────────────────┐              ┌─────────────────────────┴───────────────┐
│ Track 1: Pure State Machine    │              │ Track 2: Jetpack Paging 3 Adapter       │
│ (Mặc định cho Core Base)       │              │ (Mở rộng cho Offline-First lớn)         │
│ - 100% Kotlin Coroutines       │              │ - androidx.paging:paging-compose        │
│ - Tích hợp ActionRunner & Flow │              │ - Hỗ trợ RemoteMediator & Room Paging   │
│ - Dễ Unit Test (Virtual Time)  │              │ - Chuyển đổi LazyPagingItems -> State   │
└────────────────────────────────┘              └─────────────────────────────────────────┘
```

- **Lựa chọn triển khai cho Core:** Chọn **Track 1 (Pure State Machine)** làm mặc định:
  - Không thêm bất kỳ third-party library nào vào core.
  - Phù hợp 100% với hợp đồng `BaseInput`/`BaseOutput`, `PageResult<T>`, `PagingInput` và `ActionRunner` hiện có.
  - Viết unit test nhanh chóng với virtual time và không cần mock Android lifecycle phức tạp.
- **Mở rộng Track 2:** Khi dự án bước sang giai đoạn cần Room Database offline caching lớn (như mô tả trong `docs/product-driven-roadmap.md`), chỉ cần viết thêm một extension mapper biến đổi `LazyPagingItems<T>` thành `PagedListUiState<T>` mà không làm thay đổi UI layer.

#### 6. Kế hoạch Di chuyển Mẫu (Migration Plan)

1. **Tạo module components:** Tạo thư mục `app/src/main/java/com/example/base/ui/components/collection/` gồm `AppList.kt`, `AppGrid.kt`, `AppPagedList.kt`, `AppPagedGrid.kt` và `model/PagedListUiState.kt`.
2. **Di chuyển `UsersScreen` và `UsersViewModel`:**
   - Cập nhật `UsersUiState` sử dụng `PagedListUiState<User>`.
   - Bỏ nút "Load More" thủ công, thay bằng cơ chế tự động prefetch của `AppPagedList`.
   - Khi load trang tiếp bị lỗi, hiển thị `appendErrorSlot` dạng inline card với nút "Thử lại trang X" ngay cuối danh sách.
3. **Cập nhật `TodoScreen`:** Chuyển `LazyColumn` trực tiếp trong `TodoScreen` sang dùng `AppList` với `emptySlot` và `loadingSlot` chuẩn.

#### 7. Tiêu chí Nghiệm thu P9

- [ ] Bộ composable `AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid` biên dịch thành công trong `:app`.
- [ ] Màn hình `UsersScreen` phân trang mượt mà: cuộn gần cuối tự động tải trang kế tiếp; hết trang hiển thị slot "Đã tải hết dữ liệu".
- [ ] Lỗi tải lần đầu hiển thị màn hình lỗi toàn phần kèm nút Retry; lỗi tải trang tiếp (append error) hiển thị footer error kèm nút Retry cục bộ, giữ nguyên danh sách item cũ.
- [ ] Thay đổi ô tìm kiếm hoặc pull-to-refresh tự động hủy request cũ và tăng `generationId`, không xuất hiện lỗi append chéo dữ liệu.
- [ ] Viết unit tests kiểm chứng State Machine với virtual time: first-load, refresh, append success, append failure & footer retry, end-of-list.
- [ ] Task `verifyModuleBoundaries` PASS và không đưa Android types vào `:domain` hay `:data`.

#### P9.1 — Skeleton và shimmer loading

**Trạng thái: Đã triển khai ngày 16/11/2026.** Thành phần `ShimmerLoading.kt` đã được xây dựng trong `app/src/main/java/com/example/base/ui/components/loading/ShimmerLoading.kt` bằng native Jetpack Compose Brush linear gradient shimmer và `rememberInfiniteTransition`, có fallback skeleton tĩnh khi bật giảm chuyển động (motion reduction / animation scale = 0).

**Phạm vi thiết kế:**
- [x] Thêm primitive placeholder hình chữ nhật/bo góc, hình tròn và dòng chữ ở UI layer trong `app/src/main/java/com/example/base/ui/components/loading/ShimmerLoading.kt` (`SkeletonBox`, `SkeletonTextLine`, `SkeletonThumbnail`, `SkeletonCard`, `ProductCardSkeleton`, `ProductGridSkeleton`).
- [x] Dùng Compose shapes/Box và Brush gradient, animation API native; không thêm thư viện shimmer bên ngoài.
- [x] Cung cấp container/modifier dùng chung tiến trình shimmer (`Modifier.shimmerPlaceholder(...)` và `ShimmerScope`).
- [x] Tận dụng loading slots; màn hình tự quyết định hình dạng/kích thước placeholder, áp dụng trực tiếp trong `ProductListScreen.kt`.
- [x] Dùng màu theo theme Material, kiểm tra sáng/tối. Số placeholder hữu hạn theo viewport/layout.

**Quy tắc hiển thị:**
- [x] Initial load khi chưa có dữ liệu: skeleton grid tương ứng với bố cục Product card.
- [x] Refresh khi đã có items: giữ items và trạng thái cuộn, dùng refresh indicator; không thay toàn bộ danh sách bằng skeleton.
- [x] Append: giữ items, chỉ dùng loading footer/slot; lỗi append đổi sang footer retry.
- [x] Empty/error: tắt loading; không để placeholder che lỗi hoặc chạy vô hạn.
- [x] Hỗ trợ giảm chuyển động (motion reduction) qua `LocalReducedMotion` và kiểm tra animation scale hệ thống.

**Kết nối REST:** dùng Public Products làm ví dụ list/grid theo [kế hoạch REST](rest-api-integration-plan.md). Kiểm tra trạng thái tải chậm bằng MockWebServer/fake delay; không giả định API live có query parameter delay/fault injection.

**Nghiệm thu:**
- [ ] Có sample skeleton list và grid tùy chỉnh qua slot, không coupling sang domain/data.
- [ ] Kiểm tra initial → success/empty/error, refresh giữ items, append giữ items và retry footer.
- [ ] Kiểm tra light/dark, font scale, TalkBack và tắt/giảm animation; animation dừng khi hết loading hoặc UI không còn hiển thị.
- [ ] Có kiểm tra UI phù hợp và đo runtime nếu công bố cải thiện FPS/CPU; không coi ảnh preview đẹp là bằng chứng tối ưu.
- [ ] Cập nhật hướng dẫn chỉ sau khi component thực sự tồn tại. Lượt này chỉ cập nhật plan, chưa thêm dependency, code hoặc test.

Tham chiếu: [Compose Brush](https://developer.android.com/develop/ui/compose/graphics/draw/brush), [InfiniteTransition](https://developer.android.com/develop/ui/compose/animation/value-based).

## 6. Thứ tự và điểm dừng có thể review

| Mốc | Nội dung | Kết quả sử dụng được | Trạng thái hiện tại |
| --- | --- | --- | --- |
| **A** | P0 + P1 | Chọn môi trường, build/cài song song và cấu hình endpoint rõ ràng; task `checkProdReleaseConfig` chặn demo endpoint | **Đã hoàn thành & Kiểm chứng (Verified)** |
| **B** | P2 + P3 | Hợp đồng Input/Output và toolkit không bắt buộc kế thừa BaseViewModel; search latestRequestId coordination | **Đã hoàn thành & Kiểm chứng (Verified)** |
| **C** | P4 + P5 | i18n UI context mapping + ví dụ form/search/concurrency hoàn chỉnh (sequential suspend enqueue) | **Hoàn thành Core (Verified P5 & Core P4; Per-app language API và formatters trong backlog)** |
| **D** | P6, từng backend | Các tích hợp ngoài REST có setup và bằng chứng riêng | **Hoàn thành Contract & Fake Adapters (Verified on Fakes: 29 tests; live cloud SDKs trong backlog)** |
| **E** | P7 + P8 | DataStore session, CI automation, release và sample tiệm cận sản phẩm | **Hoàn thành DataStore Session & CI Core (Room cache, refresh coordinator, release signing và profiling trong backlog)** |
| **F** | P9 | List/Grid dùng chung và UI phân trang (`AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid`, `PagedListUiState`, auto-prefetch, footer retry inline) | **Quy hoạch chi tiết; sẵn sàng triển khai** |

Mỗi mốc cập nhật capability matrix; giữ code build được và 140 tests pass sau từng bước. Không refactor toàn bộ use case và thay mọi SDK trong cùng một thay đổi.

## 7. Chiến lược kiểm tra

Các lệnh kiểm tra cốt lõi đang hoạt động trong repository:

~~~powershell
.\gradlew.bat verifyModuleBoundaries
.\gradlew.bat :domain:test :data:test :shared:test
.\gradlew.bat :app:testDevDebugUnitTest :app:testProdDebugUnitTest
.\gradlew.bat :app:assembleDevDebug :app:assembleProdDebug
.\gradlew.bat :app:assembleDevRelease
.\gradlew.bat :app:assembleProdRelease -PallowDemoProd=true
.\gradlew.bat :app:lintDevDebug :app:lintProdRelease
~~~

CI được tự động hóa qua `.github/workflows/ci.yml` và sẵn sàng thực thi từ xa trên GitHub Actions runner; xác thực cục bộ thực thi trực tiếp các lệnh Gradle trên.

### Phân bổ 140 Unit Tests hiện tại:
- **`:app` (50 tests):**
  - `AppConfigTest`: 6 tests (variant configuration validation, URL invariants)
  - `AppNavigatorTest`: 1 test (navigation contracts)
  - `DataStoreSessionManagerTest`: 2 tests (snapshot persistence & restoration)
  - `DataStoreSessionFailureTest`: 4 tests (retryable restore, error recovery)
  - `InMemorySessionManagerTest`: 2 tests (memory state fallback)
  - `BaseViewModelTest`: 10 tests (loading tracking, retry execution, effect queues)
  - `UiErrorMessageMapperTest`: 8 tests (mapping domain errors sang UI strings en/vi, locale context resolution)
  - `TodoViewModelTest`: 7 tests (draft state qua SavedStateHandle, todo mutations)
  - `UserViewModelTest`: 10 tests (direct ViewModel inheritance, inline search validation, in-flight cancellation, latestRequestId out-of-order rejection)
- **`:data` (36 tests):**
  - `DataErrorMapperTest`: 1 test (HTTP & network error classification)
  - `InMemoryTodoDataSourceTest`: 1 test (local data source operations)
  - `JsonPlaceholderApiTest`: 4 tests (Retrofit DTO parsing)
  - `UserRepositoryRetrofitTest`: 1 test (repository implementation)
  - `RestEnvironmentMockWebServerTest`: 6 tests (dev/prod endpoint dynamic switching)
  - `GraphQLAdapterTest`: 8 tests (GraphQL queries/mutations, transport vs GraphQL errors, partial data)
  - `SupabaseAdapterTest`: 8 tests (Supabase client CRUD, Auth mapping, RLS policy errors)
  - `FirebaseAdapterTest`: 7 tests (Firestore CRUD contracts, realtime snapshot listener cleanup)
- **`:domain` (17 tests):**
  - `UseCaseContractsTest`: 17 tests (`BaseInput`, `BaseOutput`, suspend, Flow, paging contracts)
- **`:shared` (37 tests):**
  - `ActionRunnerTest`: 12 tests (concurrency, loading ref-counting, cancellation, retry policy)
  - `AppExceptionTest`: 6 tests (structured errors, validation parameters, code identity)
  - `FlowPoliciesTest`: 15 tests (debounceWithLatest, throttleFirst, droppable, sequential suspend enqueue & non-blocking tryEnqueue)
  - `RetryExecutorTest`: 4 tests (exponential backoff, retry limits, non-retryable conditions)

## 8. Đầu vào cần có ở giai đoạn phụ thuộc

Các thông tin này không chặn việc hoàn thiện core demo và contract tests:

- Application ID/tên sản phẩm thật nếu đổi khỏi `com.example.base`.
- Đã có [Practice API và hợp đồng auth/refresh](rest-api-integration-plan.md) do người dùng cung cấp ngày 16/11/2026. Cần chốt deployment dev/prod thực tế và kiểm chứng runtime khi tích hợp; không còn coi contract REST là thông tin chưa được cung cấp.
- Ngôn ngữ mặc định bổ sung và bộ locale mở rộng ngoài en/vi.
- Backend cloud đầu tiên cần live SDK sample (chọn giữa Apollo Kotlin, Supabase Kotlin hoặc Firebase Android BOM).
- Cấu hình Firebase (`google-services.json`), Supabase project URL & Anon Key dev/prod thật.
- Keystore và thông tin release signing do chủ dự án hoặc CI quản lý bảo mật.
- Mô hình Room database, schema versioning và migration strategy.

Khi đến bước cần dữ liệu chưa có, hoàn thành phần độc lập và ghi rõ phần còn thiếu; không tự tạo credential hoặc tuyên bố đã kiểm chứng live cloud mà không có kiểm thử thực tế.

## 9. Điều kiện hoàn thành kế hoạch tổng thể

- [x] Có bảng capability và hướng dẫn lựa chọn, không bắt học mọi SDK để tạo màn hình (Section 2.1).
- [x] Use case có Input/Output rõ (`BaseInput`, `BaseOutput`); các layer không rò SDK/UI types (`UseCaseContractsTest`: 17 tests).
- [x] Có hai ví dụ minh họa dùng (`TodoViewModel`, `UsersViewModel`) và không kế thừa BaseViewModel (`UserViewModel`, `UserViewModelTest`).
- [x] Một form có validation inline (`UserViewModel`), một luồng có dialog/snackbar và loading cục bộ (`TodoViewModel`, `UsersViewModel`).
- [x] i18n và các concurrency policies có regression tests (`UiErrorMessageMapperTest`: 8 tests, `FlowPoliciesTest`: 15 tests).
- [x] Dev/prod và debug/release độc lập, chọn được trong IDE/CLI (4 variants, `AppConfig`, `checkProdReleaseConfig`, `RestEnvironmentMockWebServerTest`: 6 tests).
- [x] Backend adapters được đánh dấu đúng mức độ kiểm chứng: 29 unit tests trên repository contracts & fake adapters; live cloud SDKs trong backlog.
- [x] Có CI core (`.github/workflows/ci.yml`), hướng dẫn release/signing và báo cáo giới hạn.
- [ ] Bộ Collection UI dùng chung (`AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid`) và phân trang có footer retry inline, auto-prefetch, generation cancellation (P9).
- [ ] Đo đạc số liệu runtime profiling trên thiết bị thật (baseline startup time, scrolling frame metrics, memory footprint).

## 10. Tài liệu tham chiếu

Các đường dẫn dưới đây là nguồn chính thức cho khái niệm; phiên bản SDK/plugin sẽ được kiểm tra lại ở giai đoạn triển khai:

- [Android: build variants, product flavors và build types](https://developer.android.com/build/build-variants)
- [Android: Gradle tips, BuildConfig và cấu hình](https://developer.android.com/build/gradle-tips)
- [Android: app signing](https://developer.android.com/studio/publish/app-signing)
- [Android: architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Android: coroutines best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Android: localization](https://developer.android.com/guide/topics/resources/localization)
- [Android: per-app languages](https://developer.android.com/guide/topics/resources/app-languages)
- [Kotlin Flow: debounce](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/debounce.html)
- [Kotlin Flow: sample](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/sample.html)
- [Kotlin Flow: flatMapLatest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html)
- [Apollo Kotlin](https://www.apollographql.com/docs/kotlin)
- [Supabase Kotlin](https://supabase.com/docs/reference/kotlin/introduction)
- [Firebase Android setup](https://firebase.google.com/docs/android/setup)

## 11. Quy ước lưu kết quả review

Theo yêu cầu người dùng ngày 15/11/2026, **mỗi lượt review phải tạo hoặc cập nhật một file plan trong docs**.

- Với phần việc thuộc kế hoạch này, cập nhật mục tương ứng và ghi ngày, phạm vi, source evidence, việc cần làm, tiêu chí nghiệm thu và giới hạn kiểm chứng.
- Với chủ đề độc lập, tạo file plan riêng và liên kết từ kế hoạch tổng thể; không ghi vào thư mục đang bị gitignore nếu tài liệu cần theo dõi trong Git.
- Phân biệt: phát hiện lỗi, capability còn thiếu, đề xuất thiết kế, đã sửa qua đọc source và đã kiểm chứng bằng test/run.
- Không đổi checkbox sang hoàn thành chỉ vì đã có skeleton/fake hoặc mô tả trong commit.
- Giữ các quyết định của người dùng: Kotlin/Android idiomatic, Input/Output rõ ràng, composition tùy chọn và năm-module core mặc định.

| Ngày | Phạm vi review | Kết quả và hành động | Bằng chứng kiểm chứng |
| --- | --- | --- | --- |
| 15/11/2026 | List/Grid và UI phân trang, đối chiếu base Flutter | Bổ sung P9; xác nhận Flutter có common paged list/grid, Kotlin mới có LazyColumn trong các sample | Source review + tài liệu Compose/Paging; chưa sửa implementation hoặc chạy kiểm tra mới |
| 15/11/2026 | Hoàn thiện quy hoạch chi tiết P9 (Collection & Pagination UI) | Đặc tả mô hình trạng thái PagedListUiState, chữ ký API composable, logic prefetch cuộn, cơ chế generation cancellation và kế hoạch di chuyển UsersScreen | Tài liệu kế hoạch chi tiết trong docs/base-evolution-plan.md; sẵn sàng bước vào triển khai code |
| 15/11/2026 | Review tổng thể core v1 và đối chiếu Flutter tại revision 53edac7 | [Kế hoạch ổn định v1](base-v1-review-plan.md): ưu tiên lỗi search giữ nhầm collector job, URL validation, lifecycle effects, i18n còn sót và snippet API chưa tồn tại; giữ P9 là kế hoạch | 140 test khác nhau qua ở core/app variants; 4 variants assemble với demo authorization; lint devDebug 0 lỗi/72 cảnh báo, prodRelease 0 lỗi/49 cảnh báo; source + bytecode search; malformed URL còn qua guard |
| 16/11/2026 | Skeleton/shimmer Compose và REST API do người dùng cung cấp | Bổ sung P9.1 và [kế hoạch REST](rest-api-integration-plan.md): public Products trước; nested envelopes/errors, paging metadata, 204, PATCH và refresh-token rotation | Đọc Swagger/OpenAPI qua HTTP GET và tìm component trong source; chưa gọi business API, tích hợp code hoặc chạy test mới |

Tham chiếu bổ sung cho P9:
- [Compose: lazy lists và lazy grids](https://developer.android.com/develop/ui/compose/lists)
- [Jetpack Paging: overview và load/refresh/retry](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
