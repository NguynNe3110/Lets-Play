# Kế hoạch hoàn thiện Kotlin MVVM Base

Ngày lập: 15/11/2026. Cập nhật: 15/11/2026. Trạng thái: **Đang triển khai** (Đã hoàn thành và kiểm chứng toàn diện 131 tests: P0, P1, P2, P3, P5; P4, P6, P7, P8 đã hoàn thành hợp đồng kiến trúc cốt lõi, fake adapters, DataStore session restoration, i18n mapper và CI automation; các hạng mục phụ thuộc SDK cloud thật, Room cache, release signing automation và runtime profiling nằm trong backlog theo lộ trình).

Tài liệu gộp yêu cầu về Input/Output, BaseViewModel linh hoạt, i18n, xử lý luồng tương đương RxDart, các backend ngoài REST và môi trường dev/production. Mục tiêu là một bộ nền tảng Android có ví dụ và kiểm chứng rõ ràng, mỗi feature lựa chọn cơ chế phù hợp.

## 1. Mục tiêu và phạm vi

- Giữ MVVM, Compose, coroutines, StateFlow, Hilt và cấu trúc theo layer hiện tại.
- Chuẩn hóa use case bằng `BaseInput`/`BaseOutput` theo quy ước người dùng mong muốn.
- Có sẵn loading, lỗi, retry và concurrency; sử dụng được với cả `BaseViewModel` lẫn `ViewModel` Android thông thường qua composition.
- Hỗ trợ lỗi linh hoạt: lỗi tại trường nhập (inline), dialog, snackbar, screen error state và xử lý tùy chỉnh (`ErrorPresentationMode`).
- Có đường đi i18n xuyên suốt từ lỗi nghiệp vụ tới thông báo UI với deferred resolution (`UiText`).
- Có ví dụ debounce, throttle, latest, bỏ thao tác khi đang bận (`droppable`) và xử lý tuần tự (`sequential`).
- Có dev/prod thật ở cấp build variant và hợp đồng cấu hình để kết nối backend riêng (`AppConfig`).
- Có lộ trình tích hợp GraphQL, Supabase và Firebase, kèm mức độ hoàn thiện được công bố rõ ràng (đã kiểm chứng qua fake adapters và contract tests).
- Bảo toàn các hành vi đã có: cancellation, session recovery, bản nháp qua `SavedStateHandle`, mutation/observation độc lập và navigation arguments.

**Phạm vi của tài liệu hiện tại:** Đóng vai trò kim chỉ nam kiến trúc (Living Architecture Document), phản ánh chính xác hiện trạng triển khai của codebase (131 unit tests pass, boundaries pass, 4 variants compile và CI hoạt động), đồng thời vạch rõ ranh giới giữa các thành phần đã kiểm chứng và các hạng mục roadmap trong tương lai.

## 2. Căn cứ từ code hiện tại

### 2.1. Ma trận năng lực kiến trúc (Capability Matrix)

| Năng lực / Thành phần | Trạng thái | Minh chứng / Bằng chứng kiểm thử | Vị trí sở hữu |
| --- | --- | --- | --- |
| **Module Boundaries & 5 Layer Core** | Đã kiểm chứng (Verified) | `verifyModuleBoundaries` kiểm tra dependency allowlist chặt chẽ cho cả 5 modules (`app`, `domain`, `data`, `shared`, `resources`) | Root `build.gradle.kts` |
| **Build Variants (dev/prod x debug/release)** | Đã kiểm chứng (Verified) | Suffix `.dev` và `.debug`, 4 variants: `devDebug`, `devRelease`, `prodDebug`, `prodRelease` | `app/build.gradle.kts` (P1) |
| **Cấu hình môi trường & URL Injection** | Đã kiểm chứng (Verified) | `AppConfig`, `AppEnvironment`, URL validation, Retrofit injection qua `DataModule`, 6 tests trong `AppConfigTest` | `app/src/main/java/com/example/base/config/AppConfig.kt` (P1) |
| **Hợp đồng Input/Output Use Case** | Đã kiểm chứng (Verified) | `BaseInput`, `BaseOutput`, `NoInput`, `NoOutput`, `AddTodoInput/Output`, `GetTodosOutput`, `GetUsersInput/Output`, 17 tests trong `UseCaseContractsTest` | `domain/src/main/java/com/example/base/domain/usecase/base/` (P2) |
| **Lỗi nghiệp vụ có cấu trúc** | Đã kiểm chứng (Verified) | `ValidationError(field, code, params)`, xóa hardcoded tiếng Việt trong `AppException`, 6 tests trong `AppExceptionTest` | `shared/src/main/java/com/example/base/shared/error/` (P2) |
| **Flow & Coroutine Concurrency Policies** | Đã kiểm chứng (Verified) | `throttleFirst`, `droppable`, `sequential`, `debounceWithLatest` với 11 virtual time tests trong `FlowPoliciesTest` | `shared/src/main/java/com/example/base/shared/flow/` (P5) |
| **ActionRunner & Loading Tracking** | Đã kiểm chứng (Verified) | `ActionRunner` và `LoadingManager` độc lập ViewModel, reference counting loading theo `LoadingKey`, 12 tests trong `ActionRunnerTest` | `shared/src/main/java/com/example/base/shared/action/` (P5) |
| **Toolkit ViewModel & UI Presentation** | Đã kiểm chứng (Verified) | `UiErrorMessageMapper`, `ScreenUiController`, `ErrorPresentationMode`, `UserViewModel` không kế thừa BaseViewModel, 10 tests trong `BaseViewModelTest`, 7 tests trong `UserViewModelTest` | `app/src/main/java/com/example/base/ui/base/`, `app/src/main/java/com/example/base/ui/user/` (P3) |
| **Đa ngôn ngữ i18n (en / vi)** | Đã kiểm chứng (Verified) | Resource strings đối xứng 37 keys (trong file 52 dòng) tại `resources/src/main/res/values/strings.xml` và `values-vi/strings.xml`, `UiErrorMessageMapper` (6 tests trong `UiErrorMessageMapperTest`), `UiText` deferred resolution | `resources/`, `app/src/main/java/com/example/base/ui/base/` (P4) |
| **Tích hợp Backend ngoài REST (GraphQL, Supabase, Firebase)** | Đã kiểm chứng Contract & Fake Adapters (Verified on Fakes) | MockWebServer REST switching (6 tests), GraphQL adapter với transport & partial errors (8 tests), Supabase CRUD/Auth/RLS (8 tests), Firebase CRUD/realtime cleanup (7 tests). Live cloud smoke tests với backend thật giữ trong backlog | `data/src/main/java/com/example/base/data/integration/` (P6) |
| **Session Restoration & Expiration** | Đã kiểm chứng (Verified) | DataStore session snapshot, retryable restore, expiration persistence, 6 tests trong `DataStoreSessionManagerTest` & `DataStoreSessionFailureTest`, draft persistence qua `SavedStateHandle` (7 tests trong `TodoViewModelTest`) | `app/src/main/java/com/example/base/session/` (P7) |
| **CI Automation & Variant Testing** | Đã kiểm chứng (Verified) | `.github/workflows/ci.yml` chạy boundary check, JVM tests trên cả 5 module, app test `devDebug` & `prodDebug`, compile/assemble `devDebug` & `prodDebug` | `.github/workflows/ci.yml` (P8) |

### 2.2. Hiện trạng các hạng mục

| Hạng mục | Hiện trạng quan sát được | Hướng xử lý |
| --- | --- | --- |
| Module | 5 modules: `app`, `domain`, `data`, `shared`, `resources` | Duy trì 5 modules làm core; `data` giữ là pure Kotlin/JVM |
| Startup | `SessionState` (`Loading`, `Ready`, `Error`), `DataStoreSessionManager` tự phục hồi session | Duy trì cơ chế hiện có, không tái tạo framework phức tạp |
| Base use case | `BaseUseCase`, `BaseFlowUseCase`, `BaseSyncUseCase`, `BasePagingUseCase` | Chuẩn hóa toàn bộ tham số vào/ra qua `BaseInput`/`BaseOutput` |
| Input/Output | `AddTodoInput/Output`, `GetTodosOutput`, `GetUsersInput/Output`, `NoInput/NoOutput` | Duy trì hợp đồng tường minh, kiểm thử chặt chẽ tại `UseCaseContractsTest` |
| ViewModel | Hỗ trợ 2 mô hình: kế thừa `BaseViewModel` (`TodoViewModel`, `UsersViewModel`) hoặc composition thuần với Android `ViewModel` (`UserViewModel`) | Khuyến khích composition; toolkit `ActionRunner`/`ScreenUiController` dùng độc lập |
| Lỗi | `AppException`, `ValidationError(field, code, params)`, `ErrorPresentationMode` (Auto, Dialog, Snackbar, Inline, ScreenError, Custom, None) | Lỗi nghiệp vụ có cấu trúc; presentation mapping quyết định cách hiển thị qua `UiText` |
| Todo & Users | Flow là nguồn dữ liệu; bản nháp dùng `SavedStateHandle`; Users phân trang và search debounce | Duy trì làm regression test suite mẫu |
| BaseScreen | `BaseScreenScaffold` mở, nhận `ScreenUiController` hoặc state/callbacks trực tiếp | Không bắt buộc bọc `BaseViewModel`; hỗ trợ loading overlay và local indicators |
| Môi trường | 4 variants (`devDebug`, `devRelease`, `prodDebug`, `prodRelease`), `AppConfig` injection | Inject cấu hình qua Dagger Hilt `DataModule`, URL validation nghiêm ngặt |
| i18n | `strings.xml` mặc định (en) và `values-vi/strings.xml` (37 keys đối xứng trong file 52 dòng), `UiText` deferred resolution | Đã hoàn thành core i18n; Per-app language API và date/number formatters trong backlog |
| Backend ngoài REST | Hợp đồng repository trả domain model; 3 adapters (GraphQL, Supabase, Firebase) kèm fake clients và tests | Duy trì 100% test pass trên fake adapters; live cloud SDK smoke test trong backlog |
| Chất lượng kiểm thử | **131 tests PASS (0 failures, 0 errors)** trên cả 5 modules: `:app` (45), `:data` (36), `:domain` (17), `:shared` (33) | Duy trì regression test suite và chạy tự động trên CI |
| Lint & CI | CI workflow `.github/workflows/ci.yml` kiểm tra boundaries, test đa variant và assemble | Bổ sung task lint theo variant và dọn dẹp Compose lint warnings trong backlog |

Bản Flutter tham khảo tại `D:/AppData/Code/base/flutter_bloc_clean`:
- `domain/lib/src/usecase/base/io` và các use case có Input/Output riêng.
- `app/lib/base/bloc/base_bloc.dart` có tùy chọn loading/error/retry.
- `app/lib/base/bloc/mixin/event_transformer_mixin.dart` có các chiến lược xử lý sự kiện.
- `app/lib/exception_handler/exception_message_mapper.dart` tách lỗi khỏi bản dịch.

Không coi danh sách tính năng trong README của một base là bằng chứng mọi tích hợp đã hoạt động. Ví dụ, bản Flutter cục bộ có tài liệu/schema GraphQL nhưng một số dependency GraphQL đang được comment. Mỗi capability của bản Kotlin cần có trạng thái và bằng chứng kiểm thử riêng biệt (được xác thực qua 131 tests).

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
11. **Deferred Resolution cho i18n:** Lỗi và thông báo trì hoãn việc lấy chuỗi hiển thị qua `UiText` (`UiText.StringResource` / `UiText.DynamicString`), cho phép resolve đúng locale tại composable scope.
12. **Bảo vệ ranh giới Adapter ngoài REST:** SDK/generated models của backend ngoài (GraphQL, Supabase, Firebase) không rò rỉ qua repository contract tới domain/UI. Module `data` giữ là pure Kotlin/JVM library.

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
- Bản cấu hình cho sản phẩm phải yêu cầu endpoint thực; task kiểm tra phát hành không chấp nhận chế độ demo.

### 4.4. Signing, logging và thông tin nhạy cảm

- Debug dùng debug signing tự động của Android SDK. Release không có key thì báo rõ artifact unsigned; unsigned không được xem là sẵn sàng phát hành.
- Signing release đọc từ cấu hình CI/local an toàn; không commit keystore, mật khẩu, token người dùng hay private/admin key vào Git.
- `BuildConfig` và file đóng gói trong APK có thể bị trích xuất, chỉ dùng cho thông tin client được phép công khai.
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

Các lệnh assemble trên đã hoạt động đầy đủ trong Gradle và CI. Cần phân biệt assemble APK, bundle AAB và tạo artifact có signing hợp lệ (release signing automation nằm trong backlog P8).

## 5. Các giai đoạn triển khai

Mỗi giai đoạn là một thay đổi độc lập, có thể review và chạy kiểm tra trước khi chuyển sang bước phụ thuộc.

### P0 — Chốt baseline và danh mục năng lực

- [x] Ghi snapshot build/test/lint hiện tại, cả test count và giới hạn kiểm chứng.
- [x] Lập capability matrix với trạng thái: đã kiểm chứng, có ví dụ, đang thiết kế, chờ tích hợp.
- [x] Ghi các contract phải giữ: session recovery, effect acknowledgment, draft, cancellation, mutation/observation.
- [x] Audit tương thích Kotlin/AGP/Gradle/JDK/Compose lint; nếu cần nâng phiên bản, thực hiện trong thay đổi toolchain riêng.
- [x] Không tự gộp migration AGP lớn với refactor kiến trúc.

**Đầu ra:** Baseline có thể tái lập; tài liệu cập nhật số lượng test thực tế (131 tests pass) thay thế số liệu cũ (34/35 tests).

### P1 — Môi trường dev/prod và build variants

Phụ thuộc P0.

- [x] Thêm environment dimension và ma trận bốn variant ở mục 4 (`devDebug`, `devRelease`, `prodDebug`, `prodRelease`).
- [x] Tạo config local mẫu không chứa bí mật; cập nhật `.gitignore` và hướng dẫn CI input.
- [x] Inject `AppConfig` vào các client qua Hilt; dùng cấu hình cho endpoint trong `DataModule`.
- [x] Thêm validation cấu hình của variant được yêu cầu (`AppConfigTest`: 6 tests).
- [x] Cập nhật app label, artifact naming và metadata môi trường.
- [x] Chuyển tài liệu/test tasks từ Debug chung sang tên variant chính xác (`testDevDebugUnitTest`, `testProdDebugUnitTest`).
- [x] Mô tả signing release và phân biệt demo/prod thật.

**Nghiệm thu:** Bốn variant compile/package được; MockWebServer test chứng minh URL switching dev/prod độc lập (`RestEnvironmentMockWebServerTest`: 6 tests); validation báo lỗi rõ ràng nếu URL sai định dạng.

### P2 — Hợp đồng Input/Output và lỗi nghiệp vụ

Phụ thuộc P0.

- [x] Thêm `BaseInput`, `BaseOutput` và `NoInput`/`NoOutput` cho thao tác không có dữ liệu (`domain/src/main/java/com/example/base/domain/usecase/base/`).
- [x] Chuyển `AddTodo` sang `AddTodoInput(title)` và `AddTodoOutput(todo)`.
- [x] Chuyển toggle, observation và Users sang hợp đồng có tên tương ứng (`GetTodosOutput`, `GetUsersInput/Output`).
- [x] Với paging, tách `PageResult`/`PagingInput` vào model/contract dùng bởi repository.
- [x] Đầu ra paging chuẩn hóa: `GetUsersOutput(users, page, totalPages)`.
- [x] Giữ public `Result<Output>` / `Flow<Result<Output>>`; không tạo hệ thống Result song song.
- [x] Không bọc `Result` lồng nhau.
- [x] Chuẩn hóa lỗi validation có `field`/`code` và tham số (`ValidationError` trong `shared/src/main/java/com/example/base/shared/error/AppException.kt`); tách lỗi nghiệp vụ khỏi câu chữ UI.
- [x] Rà mapper lỗi: lỗi tham số nghiệp vụ không bị quy thành lỗi parse dữ liệu server (`ErrorMapper.kt`).
- [x] Chuẩn hóa logger/dispatcher injection giữa các use case (`AppLogger`, `DispatcherProvider`).

**Nghiệm thu:** Toàn bộ 17 tests trong `UseCaseContractsTest` PASS; domain không chứa Android `Context`/`Resources`; validation, error identity và cancellation được bảo toàn.

### P3 — Toolkit thao tác độc lập với kế thừa ViewModel

Phụ thuộc P2.

- [x] Tách runner xử lý action/retry/cancellation, trạng thái loading theo key (`LoadingKey`, `LoadingManager`, `ActionRunner` trong `shared/src/main/java/com/example/base/shared/action/`).
- [x] `BaseViewModel` sử dụng các helper trên qua composition (`app/src/main/java/com/example/base/ui/base/BaseViewModel.kt`).
- [x] ViewModel kế thừa trực tiếp `androidx.lifecycle.ViewModel` vẫn dùng được toolkit qua composition (`app/src/main/java/com/example/base/ui/user/UserViewModel.kt`).
- [x] Scope do caller cung cấp (`ActionRunner.launch(scope)`, `droppable(scope)`, `sequential(scope)`); không tạo scope sống vô hạn trong helper.
- [x] Hỗ trợ loading: overlay chung (`BaseScreen`), indicator cục bộ (local loading key / pagination loading), caller tự xử lý hoặc không hiển thị.
- [x] Hỗ trợ error presentation: dialog, snackbar, inline/form, screen error, custom hoặc caller tự xử lý (`ErrorPresentationMode` trong `app/src/main/java/com/example/base/ui/base/ErrorPresentationMode.kt`).
- [x] Inline/form và screen error cập nhật state của feature qua callback (`UserViewModel.searchFieldError`); runner không tự sở hữu cấu trúc form.
- [x] Tách retry policy khỏi nơi hiển thị lỗi; giữ mặc định không replay mutation (`allowRetry = false`).
- [x] Loại bỏ quy tắc mọi `ValidationError` đều tự động thành dialog; screen chọn policy qua `ErrorPresentationMode`.
- [x] `BaseScreen` nhận state/callback hoặc controller UI (`ScreenUiController`) thay vì bắt buộc nhận `BaseViewModel` (`app/src/main/java/com/example/base/ui/base/BaseScreen.kt`).
- [x] Giữ một nơi sở hữu scaffold/insets (`BaseScreenScaffold`); screen có thể dùng UI riêng và không bọc `BaseScreen`.
- [x] Critical session state tiếp tục độc lập với snackbar/dialog; không đưa auth trở lại base chung (`AppNavHost.kt`).

**Nghiệm thu:**
- Có ví dụ kế thừa `BaseViewModel` (`TodoViewModel`, `UsersViewModel`) và ví dụ kế thừa trực tiếp `androidx.lifecycle.ViewModel` (`UserViewModel`).
- Kiểm thử chứng minh `UserViewModel` không kế thừa `BaseViewModel` vẫn dùng được runner, loading manager và error mapper (`UserViewModelTest.kt#extendsViewModelDirectlyAndNotBaseViewModel`).
- 10 tests trong `BaseViewModelTest` và 12 tests trong `ActionRunnerTest` xác nhận reference counting loading và retry policy hoạt động chính xác.

### P4 — i18n hoàn chỉnh cho UI và lỗi

Phụ thuộc P2/P3.

- [x] Chọn ngôn ngữ mặc định (tiếng Anh) và cung cấp bộ tiếng Anh/tiếng Việt đối xứng 37 keys trong file 52 dòng (`resources/src/main/res/values/strings.xml` và `resources/src/main/res/values-vi/strings.xml`).
- [x] Dùng `strings.xml`, resource theo locale, placeholders (`%1$d`, `%1$s`).
- [x] Tạo mapper presentation từ loại/mã lỗi sang resource; domain không giữ Context hoặc resource ID (`UiErrorMessageMapper`, `DefaultUiErrorMessageMapper` trong `app/src/main/java/com/example/base/ui/base/UiErrorMessageMapper.kt`).
- [x] Áp dụng cùng mapping cho snackbar, dialog, inline field error và screen error.
- [x] Đưa title, Retry, OK, loading/empty/session messages còn hard-code vào resources.
- [x] Đảm bảo thông báo đang chờ được resolve theo locale hiện tại qua deferred resolution (`UiText.StringResource` / `UiText.DynamicString` trong `app/src/main/java/com/example/base/ui/base/UiText.kt`).
- [ ] *Genuinely Pending (Roadmap):* Tích hợp Per-app language preferences API (Android 13+ `LocaleManager` và `AppCompatDelegate.setApplicationLocales()` kèm fallback Android cũ).
- [ ] *Genuinely Pending (Roadmap):* Bổ sung ví dụ định dạng số/tiền tệ/ngày tháng theo locale (`NumberFormat`, `DateTimeFormatter`).

**Nghiệm thu:**
- 6 tests trong `UiErrorMessageMapperTest` chứng minh mapping đầy đủ các mã lỗi (`network_error`, `session_expired`, `validation_error`, v.v.) sang string resource ID tương ứng.
- Đổi configuration không làm mất pending effect; deferred resolution qua `UiText` bảo đảm chuỗi hiển thị đúng ngôn ngữ active.

### P5 — Flow/coroutine policies và ví dụ tương đương RxDart

Phụ thuộc P2/P3.

- [x] Thêm ví dụ search: debounce, distinctUntilChanged và latest-result policy (`debounceWithLatest`).
- [x] Dùng `mapLatest`/`flatMapLatest` đúng với kết quả suspend/Flow; test response cũ không ghi đè response mới.
- [x] Định nghĩa rõ việc hủy trước hay sau cửa sổ debounce trong `debounceWithLatest`.
- [x] Thêm `throttleFirst` với cửa sổ thời gian và hành vi nhận phần tử đầu được định nghĩa rõ ràng.
- [x] Ví dụ `droppable`: bỏ lần nhấn mới khi tác vụ đang chạy (`Droppable.kt`).
- [x] Ví dụ `sequential`: hàng đợi được xử lý theo thứ tự; cấu hình buffer qua `Sequential.kt`.
- [x] Giới hạn concurrency thay vì khởi chạy coroutine không kiểm soát.
- [x] Không tạo alias chỉ đổi tên operator chuẩn; helper mới bổ sung chính sách xử lý cụ thể.
- [x] Chọn lifecycle collection theo nguồn: observation chỉ phục vụ UI dùng `WhileSubscribed`.

**Nghiệm thu:** Toàn bộ 11 virtual time tests trong `FlowPoliciesTest` PASS (kiểm tra triệt để debounce, throttleFirst, droppable, sequential và cancellation semantics).

### P6 — Hợp đồng data và bộ tích hợp ngoài REST

Phụ thuộc P1/P2; UI sample phụ thuộc P3/P4.

Deliverable bao gồm hợp đồng repository trừu tượng và ba adapter độc lập. Không xây dựng "universal backend client" làm mờ nhạt semantics khác biệt của từng nền tảng.

| Gói | Công việc | Bằng chứng hoàn thành | Vị trí source |
| --- | --- | --- | --- |
| **REST hiện có** | Config theo môi trường, DTO/error mapping và timeout/retry | `RestEnvironmentMockWebServerTest` (6 tests) | `data/.../integration/rest/` |
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

**Nghiệm thu:** 29 tests tích hợp PASS trong `:data` (`RestEnvironmentMockWebServerTest`: 6, `GraphQLAdapterTest`: 8, `SupabaseAdapterTest`: 8, `FirebaseAdapterTest`: 7); thay thế adapter không làm thay đổi domain contracts hay UI layer; listener realtime được giải phóng hoàn toàn khi scope hủy.

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
- [x] Dùng mock/fake cho PR/CI không đòi hỏi tài khoản cloud.
- [x] Cập nhật tài liệu kỹ thuật architecture, testing và workflow; cập nhật baseline chính xác lên 131 tests.
- [ ] *Genuinely Pending (Roadmap):* Chạy lint theo variant trong CI (`lintDevDebug`, `lintProdRelease`) và dọn dẹp Compose lint warnings.
- [ ] *Genuinely Pending (Roadmap):* Cloud smoke tests thành workflow riêng với cấu hình dev secrets.
- [ ] *Genuinely Pending (Roadmap):* Signing release automation và xuất bản AAB/APK với signing hợp lệ.
- [ ] *Genuinely Pending (Roadmap):* Bật R8 và cấu hình quy tắc ProGuard/consumer-rules cho release.
- [ ] *Genuinely Pending (Roadmap):* Đo baseline runtime profiling (startup time, frame drops/scrolling, memory footprint).

**Nghiệm thu:** CI workflow `.github/workflows/ci.yml` chạy tự động và thành công trên GitHub Actions cho boundary check, test toàn bộ 5 module và assemble các variant; không phụ thuộc bí mật bên ngoài đối với core tests.

## 6. Thứ tự và điểm dừng có thể review

| Mốc | Nội dung | Kết quả sử dụng được | Trạng thái hiện tại |
| --- | --- | --- | --- |
| **A** | P0 + P1 | Chọn môi trường, build/cài song song và cấu hình endpoint rõ ràng | **Đã hoàn thành & Kiểm chứng (Verified)** |
| **B** | P2 + P3 | Hợp đồng Input/Output và toolkit không bắt buộc kế thừa BaseViewModel | **Đã hoàn thành & Kiểm chứng (Verified)** |
| **C** | P4 + P5 | i18n + ví dụ form/search/concurrency hoàn chỉnh | **Hoàn thành Core (Verified P5 & Core P4; Per-app language API và formatters trong backlog)** |
| **D** | P6, từng backend | Các tích hợp ngoài REST có setup và bằng chứng riêng | **Hoàn thành Contract & Fake Adapters (Verified on Fakes; live cloud SDKs trong backlog)** |
| **E** | P7 + P8 | DataStore session, CI automation, release và sample tiệm cận sản phẩm | **Hoàn thành DataStore Session & CI Core (Room cache, refresh coordinator, release signing và profiling trong backlog)** |

Mỗi mốc cập nhật capability matrix; giữ code build được và 131 tests pass sau từng bước. Không refactor toàn bộ use case và thay mọi SDK trong cùng một thay đổi.

## 7. Chiến lược kiểm tra

Các lệnh kiểm tra cốt lõi đang hoạt động trong repository:

~~~powershell
.\gradlew.bat verifyModuleBoundaries
.\gradlew.bat :domain:test :data:test :shared:test
.\gradlew.bat :app:testDevDebugUnitTest :app:testProdDebugUnitTest
.\gradlew.bat :app:assembleDevDebug :app:assembleProdDebug
.\gradlew.bat :app:assembleDevRelease :app:assembleProdRelease
.\gradlew.bat :app:lintDevDebug :app:lintProdRelease
~~~

### Phân bổ 131 Unit Tests hiện tại:
- **`:app` (45 tests):**
  - `AppConfigTest`: 6 tests (variant configuration validation, URL invariants)
  - `AppNavigatorTest`: 1 test (navigation contracts)
  - `DataStoreSessionManagerTest`: 2 tests (snapshot persistence & restoration)
  - `DataStoreSessionFailureTest`: 4 tests (retryable restore, error recovery)
  - `InMemorySessionManagerTest`: 2 tests (memory state fallback)
  - `BaseViewModelTest`: 10 tests (loading tracking, retry execution, effect queues)
  - `UiErrorMessageMapperTest`: 6 tests (mapping domain errors sang UI strings en/vi)
  - `TodoViewModelTest`: 7 tests (draft state qua SavedStateHandle, todo mutations)
  - `UserViewModelTest`: 7 tests (direct ViewModel inheritance, inline search validation)
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
- **`:shared` (33 tests):**
  - `ActionRunnerTest`: 12 tests (concurrency, loading ref-counting, cancellation, retry policy)
  - `AppExceptionTest`: 6 tests (structured errors, validation parameters, code identity)
  - `FlowPoliciesTest`: 11 tests (debounceWithLatest, throttleFirst, droppable, sequential)
  - `RetryExecutorTest`: 4 tests (exponential backoff, retry limits, non-retryable conditions)

## 8. Đầu vào cần có ở giai đoạn phụ thuộc

Các thông tin này không chặn việc hoàn thiện core demo và contract tests:

- Application ID/tên sản phẩm thật nếu đổi khỏi `com.example.base`.
- Endpoint dev/prod thật và hợp đồng auth/refresh của backend sản phẩm.
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
- [x] i18n và các concurrency policies có regression tests (`UiErrorMessageMapperTest`: 6 tests, `FlowPoliciesTest`: 11 tests).
- [x] Dev/prod và debug/release độc lập, chọn được trong IDE/CLI (4 variants, `AppConfig`, `RestEnvironmentMockWebServerTest`: 6 tests).
- [x] Backend adapters được đánh dấu đúng mức độ kiểm chứng; mọi integration chưa xong vẫn hiện trong backlog (P6 fake adapters verified; live cloud SDK in backlog).
- [x] Có CI core (`.github/workflows/ci.yml`), hướng dẫn release/signing và báo cáo giới hạn.
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
