# Báo cáo tổng hợp toàn bộ các cải tiến kiến trúc Kotlin MVVM Base

> **Nguồn hiện hành 16/11/2026:** [Đặc tả và hiện trạng thống nhất](LetsPlay-Spec/UNIFIED-SPEC.md). Trang này giữ lịch sử/baseline; các trạng thái, yêu cầu hoặc số test cũ mâu thuẫn dùng bản thống nhất. Báo cáo backend mới được phân biệt với source Android và mô phỏng prototype.


> **Dành cho App Developer / Product Engineer:** Tài liệu này tóm tắt toàn bộ những gì AI và các phiên làm việc trong quá khứ đã xây dựng và cải tiến trong dự án. Bản tóm tắt được viết dưới góc nhìn luồng ứng dụng (**App Flow**), giải thích: *Vấn đề trước đây là gì? AI đã giải quyết thế nào? Trong code đang có những class nào? Và khi bạn làm tính năng mới thì sử dụng ra sao?*

---

## 1. Bản đồ tổng quan dự án hiện tại

Hiện tại, codebase được tổ chức thành **5 module độc lập** với ranh giới nghiêm ngặt được kiểm tra tự động qua task `verifyModuleBoundaries`:

```
                 ┌────────────────────────────────┐
                 │          :app (Android)        │
                 │ UI, Compose, ViewModels, DI    │
                 └──────┬───────────────┬─────────┘
                        │               │
       ┌────────────────▼──────┐ ┌──────▼───────────────┐
       │   :domain (Pure JVM)  │ │  :resources (Android)│
       │ Models, UseCase IO,   │ │ Themes, Colors,      │
       │ Repository Interfaces │ │ Strings (en / vi)    │
       └──────────────▲────────┘ └──────────────────────┘
                      │
       ┌──────────────┴────────┐
       │    :data (Pure JVM)   │
       │ REST, GraphQL, Firebase│
       │ Supabase Fake Adapters │
       └──────────────┬────────┘
                      │
       ┌──────────────▼────────┐
       │   :shared (Pure JVM)  │
       │ Concurrency, Action,  │
       │ Dispatchers, Errors   │
       └───────────────────────┘
```

- **Tổng số Unit Test hiện có:** **140 unique tests PASS (0 failures, 0 errors)** khi chạy kiểm tra cục bộ qua lệnh Gradle (`verifyModuleBoundaries`, unit test cả 5 module, build 4 variants).
  > [!NOTE]
  > Kết quả 140 tests dựa trên test suite unit test cục bộ đã được kiểm chứng thực tế bằng lệnh Gradle; không bao gồm đo đạc hiệu năng runtime trên thiết bị thật (FPS, startup time, bộ nhớ) hay kết quả kiểm chứng từ CI server từ xa.

---

## 2. Toàn bộ lịch sử cải tiến qua các phiên (Past to Present)

### Giai đoạn 1: Thiết lập nền móng Clean Architecture (Commit `e61e593`)
- **Trước đó:** Chưa có cấu trúc dự án chuẩn, các thành phần bị phụ thuộc lộn xộn.
- **AI đã làm:**
  1. Tách dự án thành 5 module: `:app`, `:domain`, `:data`, `:shared`, `:resources`.
  2. Tạo task `verifyModuleBoundaries` trong root `build.gradle.kts` để chặn việc import chéo trái phép giữa các tầng.
  3. Xây dựng use case mẫu (`AddTodoUseCase`, `GetTodosUseCase`, `GetUsersUseCase`, v.v.).
  4. Triển khai tầng Data với Retrofit (`JsonPlaceholderApi`), InMemory Cache, và DataStore Session Manager để tự động lưu/khôi phục phiên đăng nhập.
  5. Setup Jetpack Compose với giao diện mẫu Todo và Users.

---

### Giai đoạn 2: Chuẩn hóa hợp đồng Input/Output (Commit `85b6743`)
- **Vấn đề trước đây:**
  - Use case nhận tham số tự do (ví dụ `AddTodoUseCase.invoke(title: String)`) hoặc trả về entity thô.
  - Khi nghiệp vụ mở rộng thêm tham số (ví dụ: title, description, priority), chữ ký hàm bị vỡ, phải sửa nhiều nơi.
- **AI đã làm:**
  1. Đưa vào hợp đồng `BaseInput`, `BaseOutput`, `NoInput`, `NoOutput` trong `domain/src/main/java/com/example/base/domain/usecase/base/`.
  2. Mọi use case đều có Input/Output rõ ràng: `AddTodoInput(title)` ➔ `AddTodoOutput(todo)`.
  3. Tách biệt `ValidationError` khỏi ngôn ngữ hiển thị: lỗi nghiệp vụ chỉ mang mã code (`EMPTY_TITLE`, `FIELD_REQUIRED`) và metadata, không chứa câu chữ cứng.

---

### Giai đoạn 3: Mở rộng và chuẩn hóa kiến trúc cốt lõi (Commit `e031db8`)
Giai đoạn này tập trung giải quyết các bài toán kiến trúc cốt lõi cho Android MVVM starter:

#### 3.1. Build Variants & Cấu hình môi trường (P1)
- **Vấn đề trước đây:** App chỉ có `debug` và `release`. URL server bị hardcode trong `DataModule`. Bản test cài đè lên bản dùng thật.
- **AI đã làm:**
  - Tạo 4 Build Variants với `Application ID` riêng biệt:
    - `devDebug` (`com.example.base.dev.debug`): Lập trình hàng ngày với backend Dev.
    - `devRelease` (`com.example.base.dev`): Bản build release với backend Dev (hỗ trợ kiểm tra cấu hình tối ưu trước khi ra production).
    - `prodDebug` (`com.example.base.debug`): Bật log chẩn đoán lỗi trên backend Production.
    - `prodRelease` (`com.example.base`): Bản phát hành chính thức cho người dùng.
  - Có thể cài song song cả 2 bản Dev và Prod trên cùng 1 điện thoại mà không bị ghi đè.
  - Tạo `AppConfig` ([AppConfig.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/config/AppConfig.kt)) tự động validate URL (yêu cầu http/https, có dấu `/` cuối) và inject động qua Hilt, xóa bỏ 100% hardcode URL.

#### 3.2. Toolkit ViewModel độc lập - Không ép kế thừa (P3)
- **Vấn đề trước đây:** Mọi ViewModel muốn có loading, hiển thị dialog lỗi hay retry đều buộc phải kế thừa `BaseViewModel`. Nếu một ViewModel cần kế thừa class khác hoặc muốn viết đơn giản thì bị bế tắc.
- **AI đã làm:**
  - Tách lõi xử lý thành các helper dùng độc lập:
    - `ActionRunner` ([ActionRunner.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/shared/src/main/java/com/example/base/shared/action/ActionRunner.kt)): Chuyên chạy tác vụ bất đồng bộ, quản lý loading, bắt lỗi và cơ chế retry.
    - `LoadingManager` ([LoadingManager.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/shared/src/main/java/com/example/base/shared/action/LoadingManager.kt)): Đếm số lượng tác vụ đang chạy theo `LoadingKey`. Khi có 3 tác vụ chạy cùng lúc, chỉ khi cả 3 xong thì loading overlay mới tắt.
    - `ScreenUiController` & `ErrorPresentationMode` ([app/ui/base](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/base)): Màn hình có thể chọn cách hiển thị lỗi linh hoạt: Hiện Dialog, Snackbar, báo đỏ ngay dưới ô nhập (Inline), hoặc hiện toàn màn hình (ScreenError).
  - Tạo `UserViewModel` ([UserViewModel.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/user/UserViewModel.kt)): Đây là **ví dụ minh họa kiến trúc composition** (architectural composition example). ViewModel này kế thừa trực tiếp `androidx.lifecycle.ViewModel` chuẩn của Android (không bắt buộc kế thừa `BaseViewModel`), đồng thời phối hợp `LoadingManager` và `ActionRunner` qua composition, có bộ unit test đi kèm. *Lưu ý: `UserViewModel` hiện đóng vai trò mẫu code kiến trúc đã được kiểm chứng bằng unit test; chưa được nối vào màn hình/route chạy thật trong `AppNavHost` và chưa kiểm chứng thao tác người dùng trực tiếp trên thiết bị.*

#### 3.3. Xử lý luồng Concurrency tương đương RxDart (P5)
- **Vấn đề trước đây:** Kotlin coroutines thiếu các chính sách luồng tiện dụng như trong RxDart / Flutter BLoC (như click tặc bị gọi 2 lần, gõ ô tìm kiếm bị race condition response cũ ghi đè response mới).
- **AI đã làm:** Bổ sung bộ công cụ trong `shared/src/main/java/com/example/base/shared/flow/`:
  - `throttleFirst`: Nhận sự kiện đầu tiên, bỏ qua các lần bấm tiếp theo trong khoảng thời gian định trước (chống click đúp mở 2 màn hình).
  - `droppable`: Khi tác vụ đang chạy thì bỏ qua toàn bộ lần gọi mới cho đến khi xong hẳn.
  - `debounceWithLatest`: Gõ phím tìm kiếm chờ ngừng gõ mới gọi API, và tự hủy request trước nếu có ký tự mới.
  - `sequential`: Hàng đợi xử lý tuần tự từng tác vụ một.

#### 3.4. Đa ngôn ngữ i18n xuyên suốt (P4)
- **Vấn đề trước đây:** Lỗi từ server hoặc domain hay bị hardcode tiếng Anh hoặc tiếng Việt lẫn lộn.
- **AI đã làm:**
  - Tạo `UiText` ([UiText.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/base/UiText.kt)): Trì hoãn việc lấy chuỗi cho đến khi ra tới màn hình Compose. Khi người dùng đổi ngôn ngữ máy, chuỗi tự động đổi theo mà không cần khởi động lại logic.
  - Tạo `UiErrorMessageMapper` ([UiErrorMessageMapper.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/base/UiErrorMessageMapper.kt)): Tự động dịch các mã lỗi (`network_error`, `session_expired`, `validation_error`) sang string tương ứng trong `resources/values/strings.xml` và `resources/values-vi/strings.xml`.

#### 3.5. Hợp đồng backend ngoài REST (P6)
- **Vấn đề trước đây:** Tầng data bị dính chặt với Retrofit HTTP REST API.
- **AI đã làm:**
  - Tạo fake clients và adapters trong `data/src/main/java/com/example/base/data/integration/`:
    - `GraphQLAdapter`: Xử lý query/mutation, phân biệt lỗi transport vs lỗi partial data.
    - `SupabaseAdapter`: Xử lý CRUD, Auth và lỗi Row Level Security (RLS).
    - `FirebaseAdapter`: Xử lý Firestore CRUD và tự dọn dẹp realtime listener khi ViewModel bị hủy.
  - Viết 29 unit tests chứng minh tầng Domain có thể đổi backend sang GraphQL/Firebase/Supabase mà không cần sửa 1 dòng code nghiệp vụ nào.

#### 3.6. Cấu hình CI Workflow (P8)
- **AI đã làm:** Khởi tạo file cấu hình workflow [.github/workflows/ci.yml](file:///D:/AppData/Code/base/kotlin_mvvm_base/.github/workflows/ci.yml) định nghĩa các bước kiểm tra tự động: ranh giới module, unit test cả 5 module, và build các variant.
- *Lưu ý về phạm vi kiểm chứng:* Đây là file cấu hình workflow được lưu trong repository; chưa có chứng nhận chạy pass trên remote GitHub Actions runner thực tế. Mọi kết quả pass hiện tại đều dựa trên việc chạy lệnh Gradle cục bộ trên máy phát triển.

---

### Giai đoạn 4: Hoàn thiện 5 điểm thực chiến chuyên sâu (Commit `7a5cce0` & hiện tại)
Dựa trên phản hồi thực tế từ bạn, AI đã sửa dứt điểm 5 vấn đề cốt lõi:

1. **Sửa hàng đợi `Sequential.kt`:** Bổ sung suspend function `enqueue(...)`/`send(...)` để khi buffer đầy thì coroutine tự động tạm dừng (suspend) chờ chỗ trống, không còn tình trạng bị rơi mất tác vụ âm thầm như khi dùng `trySend()`.
2. **Chặn release nhầm backend demo (`build.gradle.kts`):** Tạo task `checkProdReleaseConfig`. Nếu bạn build `prodRelease` mà quên cấu hình URL thật (vẫn để fallback về JSONPlaceholder), Gradle sẽ lập tức ném lỗi chặn lại ngay, trừ khi bạn cố ý bật cờ `-PallowDemoProd=true`.
3. **i18n đi tới tận giao diện:** Đảm bảo `AppEffectHandler`, `BaseScreen`, `UsersScreen` đều nhận `Context` để dịch thông báo snackbar/dialog theo đúng ngôn ngữ đang kích hoạt. Đưa thông báo lỗi rỗng ô tìm kiếm vào file `strings.xml`.
4. **Sửa lỗi nhấp nháy/restart snackbar (`BaseScreen.kt`):** Chỉnh `LaunchedEffect` chỉ lắng nghe ID của effect đầu tiên (`pendingEffects.firstOrNull()?.id`). Khi có snackbar thứ 2 tới, snackbar thứ 1 đang hiện không bị hủy rồi nhảy lại từ đầu.
5. **Chống race-condition tìm kiếm trong ví dụ kiến trúc ([UserViewModel.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/user/UserViewModel.kt)):** Thêm biến `latestRequestId` (`AtomicLong`). Khi người dùng gõ chữ nhanh hoặc vừa gõ vừa bấm refresh: request cũ đang chạy sẽ bị hủy ngay, và nếu kết quả cũ có lỡ trả về muộn hơn thì cũng bị loại bỏ, bảo đảm kết quả trả về đúng lần tìm kiếm mới nhất (được kiểm chứng qua unit test của ViewModel; đây là composition example, chưa có UI screen trong `AppNavHost`).

---

### Giai đoạn 5: Quy hoạch Bộ Collection UI & Phân trang dùng chung (P9 - Thiết kế dự kiến, chưa triển khai)

> [!IMPORTANT]
> **Trạng thái P9:** Toàn bộ nội dung P9 dưới đây là **đặc tả thiết kế đang lập kế hoạch (Design Specification / Backlog)**, **CHƯA ĐƯỢC TRIỂN KHAI (Not Implemented)** trong codebase hiện tại. Thư mục `app/src/main/java/com/example/base/ui/components/collection/` và các Composable `AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid` chưa tồn tại trong mã nguồn. Hiện tại các màn hình mẫu ([TodoScreen.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/todo/TodoScreen.kt), [UsersScreen.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/users/UsersScreen.kt)) vẫn sử dụng trực tiếp `LazyColumn` của Jetpack Compose.

- **Vấn đề đặt ra:**
  - Các màn hình `TodoScreen` và `UsersScreen` đang gọi `LazyColumn` trực tiếp trong từng màn hình, chưa có bộ UI collection dùng chung có thể tái sử dụng.
  - Khi phân trang (`Users`), app dùng nút "Load More" thủ công; nếu việc tải trang tiếp theo bị lỗi thì chỉ bắn snackbar rời rạc mà không có nút "Thử lại" ngay tại cuối danh sách (footer retry).
  - Khi người dùng đổi từ khóa tìm kiếm nhanh, nếu không có cơ chế `generationId` đồng bộ, kết quả trang cũ có thể bị nối nhầm vào danh sách mới.
- **Đặc tả thiết kế đề xuất cho P9:**
  1. **Bộ 4 Composable Collection dự kiến (`app/src/main/java/com/example/base/ui/components/collection/` - Chưa triển khai):**
     - `AppList<T>` & `AppGrid<T>`: Dành cho danh sách/lưới thường, tích hợp sẵn pull-to-refresh, empty slot, loading slot và error retry slot.
     - `AppPagedList<T>` & `AppPagedGrid<T>`: Dành cho danh sách/lưới phân trang lớn, tự động phát hiện cuộn gần cuối để tải thêm (scroll prefetch), có footer loading indicator, inline footer error retry và slot báo hết trang.
  2. **Mô hình trạng thái `PagedListUiState<T>`:** Quản lý độc lập 3 pha tải (`firstLoadState`, `refreshState`, `appendLoadState`) cùng `generationId` để triệt tiêu race condition.
  3. **Kiến trúc Dual-Track Paging Engine:** Chọn Track 1 (Pure State Machine trên Coroutines) làm mặc định cho core để giữ base nhẹ, không phụ thuộc thư viện ngoài; đồng thời mở cổng Track 2 (Adapter cho Jetpack Paging 3) khi cần mở rộng offline cache lớn.
  4. **Chống tràn bộ nhớ Android:** Không lưu `List<T>` lớn vào `SavedStateHandle` (tránh `TransactionTooLargeException`), chỉ lưu `page`, `query`, và `scrollOffset`.

---

## 3. "Cheat Sheet" cho App Developer: Khi làm app flow thì xài thế nào?

Bạn không cần nhớ hết 140 bài test hay chi tiết bên dưới. Khi cần viết tính năng mới, hãy dùng theo các mẫu sau:

### Cách 1: Tạo UseCase mới
Tạo trong `domain/src/main/java/com/example/base/domain/usecase/`:
```kotlin
// 1. Định nghĩa Input và Output
data class LoginInput(val email: String, val pass: String) : BaseInput
data class LoginOutput(val token: String, val user: User) : BaseOutput

// 2. Kế thừa BaseUseCase
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    dispatcherProvider: DispatcherProvider,
    logger: AppLogger
) : BaseUseCase<LoginInput, LoginOutput>(dispatcherProvider, logger) {
    override suspend fun execute(input: LoginInput): Result<LoginOutput> {
        if (input.email.isBlank()) {
            return Result.failure(ValidationError(field = "email", code = "REQUIRED"))
        }
        return authRepository.login(input.email, input.pass)
    }
}
```

---

### Cách 2: Viết ViewModel và gọi UseCase có Loading & Error
Bạn có thể chọn 1 trong 2 phong cách:

#### Phong cách A: Dùng `BaseViewModel` (Nhanh, tiện lợi)
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : BaseViewModel() {

    fun onLoginClick(email: String, pass: String) {
        // Tự động bật loading, tự động bắt lỗi hiện Dialog/Snackbar nếu thất bại
        launchAction(
            loadingKey = LoadingKey.Global, // Hoặc LoadingKey.Local("login")
            errorMode = ErrorPresentationMode.Snackbar
        ) {
            val result = loginUseCase(LoginInput(email, pass))
            result.onSuccess { output ->
                sendEffect(UiEffect.Navigate("home"))
            }
        }
    }
}
```

#### Phong cách B: Kế thừa `ViewModel()` thông thường (Composition)
Xem code mẫu kiến trúc tại: [`UserViewModel.kt`](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/user/UserViewModel.kt).

> [!NOTE]
> `UserViewModel` là **ví dụ minh họa kiến trúc composition** (architectural composition example) có unit test bao phủ, chứng minh ViewModel chuẩn không cần kế thừa `BaseViewModel` mà vẫn có thể compose `ActionRunner`, `LoadingManager`, `debounceWithLatest`, và `droppable`. `UserViewModel` hiện chưa được gắn vào màn hình/route chạy thật trong `AppNavHost`.

Bạn chỉ cần inject hoặc khởi tạo `ActionRunner`, gọi `actionRunner.execute(...)` (hoặc launch coroutine qua `actionRunner.launch(...)`) là có đầy đủ loading, retry và error handling mà không cần kế thừa `BaseViewModel`.

---

### Cách 3: Chống bấm đúp (Click tặc) trong UI
Trong Compose screen, dùng `throttleFirst` hoặc `droppable`:
```kotlin
// Trong ViewModel:
val onSubmitClick = droppable(viewModelScope) {
    // Nếu user bấm 5 lần liên tục khi hàm này đang chạy, 4 lần sau sẽ bị bỏ qua
    saveDataUseCase(NoInput)
}
```

---

### Cách 4: Hiển thị giao diện màn hình với `BaseScreen`
Trong file UI Compose (`app/src/main/java/com/example/base/ui/...`):
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    BaseScreen(
        viewModel = viewModel, // Hoặc truyền controller: ScreenUiController riêng
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) {
        // Nội dung màn hình của bạn
        // Loading overlay và dialog lỗi sẽ tự động hiển thị đè lên khi có sự kiện!
    }
}
```

> [!IMPORTANT]
> **Lưu ý về signature của `BaseScreen`:**
> Tham số `content` của `BaseScreen` có chữ ký là `content: @Composable () -> Unit` (lambda **KHÔNG** nhận tham số `PaddingValues`).
>
> Lý do: `BaseScreenScaffold` đã tự động xử lý Scaffold padding bên trong container `Box` với modifier:
> ```kotlin
> Modifier
>     .fillMaxSize()
>     .padding(padding)
>     .consumeWindowInsets(padding)
> ```
> Vì vậy, khi viết nội dung bên trong lambda `BaseScreen { ... }`, bạn không khai báo `{ paddingValues -> ... }` mà viết trực tiếp các Composable nội dung màn hình. Xem cách sử dụng thực tế trong [AppNavHost.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/navigation/AppNavHost.kt#L38-L50).

---

### Cách 5: Sử dụng Collection UI & Danh sách Phân trang (P9 - Thiết kế dự kiến, chưa triển khai)

> [!WARNING]
> **APIs Đang Quy Hoạch - Chưa Triển Khai (P9 Design Spec Under Planning):**
> Các Composable `AppList`, `AppGrid`, `AppPagedList`, `AppPagedGrid` dưới đây là **đặc tả thiết kế đề xuất (planned / proposed APIs)** thuộc kế hoạch P9, **HIỆN CHƯA CÓ TRONG SOURCE CODE**.
> - Không gọi các hàm này trong code tính năng hiện tại vì sẽ gặp lỗi Unresolved reference (`AppList`, `AppPagedList` chưa tồn tại).
> - Với codebase hiện tại, hãy sử dụng `LazyColumn` hoặc `LazyVerticalGrid` chuẩn của Jetpack Compose (xem ví dụ thực tế đang chạy tại [TodoScreen.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/todo/TodoScreen.kt) và [UsersScreen.kt](file:///D:/AppData/Code/base/kotlin_mvvm_base/app/src/main/java/com/example/base/ui/users/UsersScreen.kt)).
> - Các đoạn mã dưới đây là pseudocode minh họa cho API dự kiến khi P9 được triển khai chính thức.

#### A. Danh sách thông thường với `AppList` (Đặc tả thiết kế dự kiến - Planned API):
```kotlin
// PSEUDOCODE - API DỰ KIẾN (Chưa có trong mã nguồn hiện tại):
AppList(
    items = uiState.todos,
    loadState = uiState.loadState,
    onRefresh = { viewModel.refresh() },
    emptySlot = { Text("Chưa có công việc nào!") },
    errorSlot = { error, retry ->
        Button(onClick = retry) { Text("Thử lại") }
    }
) { todo ->
    TodoItemRow(todo = todo)
}
```

#### B. Danh sách phân trang với `AppPagedList` (Đặc tả thiết kế dự kiến - Planned API):
```kotlin
// PSEUDOCODE - API DỰ KIẾN (Chưa có trong mã nguồn hiện tại):
AppPagedList(
    pagedState = uiState.pagedUsers, // PagedListUiState<User>
    onLoadMore = { viewModel.loadNextPage() },
    onRefresh = { viewModel.refresh() },
    prefetchDistance = 3, // Tự động tải tiếp khi còn cách cuối danh sách 3 items
    emptySlot = { Text("Không tìm thấy người dùng") },
    appendErrorSlot = { error, retry ->
        // Nút thử lại hiển thị ngay ở cuối danh sách mà không mất các trang trước
        TextButton(onClick = retry) { Text("Tải trang tiếp thất bại. Bấm để thử lại!") }
    },
    endOfPaginationSlot = { Text("Đã hiển thị toàn bộ danh sách.") }
) { user ->
    UserItemRow(user = user)
}
```

---

## 4. Tóm tắt các tài liệu kỹ thuật liên quan trong thư mục `docs/`

- **[base-evolution-plan.md](file:///D:/AppData/Code/base/kotlin_mvvm_base/docs/base-evolution-plan.md):** Kế hoạch kiến trúc chi tiết từ P0 đến P9, phân định rõ ràng các tính năng đã kiểm chứng (140 tests) vs quy hoạch bộ Collection & Pagination UI (P9) và lộ trình tương lai.
- **[base-v1-review-plan.md](file:///D:/AppData/Code/base/kotlin_mvvm_base/docs/base-v1-review-plan.md):** Báo cáo review và kế hoạch ổn định v1 (R1–R5), xác định giới hạn kiểm chứng thực tế và danh sách các điểm cần khắc phục.
- **[architecture.md](file:///D:/AppData/Code/base/kotlin_mvvm_base/docs/architecture.md):** Bản mô tả kiến trúc 5 module, ranh giới dependency và composition root Hilt.
- **[testing.md](file:///D:/AppData/Code/base/kotlin_mvvm_base/docs/testing.md):** Hướng dẫn chạy 140 bài test tự động qua dòng lệnh Gradle trên Windows PowerShell.
- **[product-driven-roadmap.md](file:///D:/AppData/Code/base/kotlin_mvvm_base/docs/product-driven-roadmap.md):** Lộ trình phát triển sản phẩm thực chiến (Room database, single-flight refresh token, KeyStore encryption).
- **[rest-api-integration-plan.md](file:///D:/AppData/Code/base/kotlin_mvvm_base/docs/rest-api-integration-plan.md):** Kế hoạch tích hợp REST API thực chiến và refresh token flow.
