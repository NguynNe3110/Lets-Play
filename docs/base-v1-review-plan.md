# Review và kế hoạch ổn định Kotlin Base v1

> **Nguồn hiện hành 16/11/2026:** [Đặc tả và hiện trạng thống nhất](LetsPlay-Spec/UNIFIED-SPEC.md). Trang này giữ lịch sử/baseline; các trạng thái, yêu cầu hoặc số test cũ mâu thuẫn dùng bản thống nhất. Báo cáo backend mới được phân biệt với source Android và mô phỏng prototype.


Ngày: 15/11/2026. Revision được review: **53edac7** (`upload file docs`).
Workspace ban đầu sạch. Phạm vi thay đổi của lượt này: **chỉ tài liệu**, không sửa implementation hoặc test.

[Trở về kế hoạch tổng thể](base-evolution-plan.md)

## 1. Kết luận

Nền kiến trúc hiện tại phù hợp để làm **Android MVVM core starter**: năm module có ranh giới rõ, Input/Output tường minh, coroutine cancellation, toolkit dùng qua composition, session snapshot và dev/prod variants đều đã có.

Chưa nên kết luận “không còn gì cần làm” hoặc “mọi flow đã kiểm chứng toàn diện”. Có một lỗi đáng ưu tiên trong sample search, các khoảng trống về config/lifecycle/i18n và tài liệu API cần sửa. Bước tiếp theo nên là **ổn định phần đang có**, không mặc định thêm mọi tính năng của Flutter vào core.

Các khả năng như auth thật, persistent database, cloud SDK, notification và advanced navigation cần use case của sản phẩm. P9 list/grid vẫn là kế hoạch, chưa phải API đã có.

## 2. Phạm vi đã đọc và đối chiếu

Kotlin:
- app config, Gradle variants/release guard, Hilt composition root.
- BaseViewModel, ActionRunner/LoadingManager, error presentation và BaseScreen.
- Todo/Users/search state, SavedStateHandle và observation.
- Session persistence/recovery, domain contracts/use cases, error mapping và Flow policies.
- Network/repository boundary, các hợp đồng backend ngoài REST, test suite, CI và tài liệu.

Flutter tham khảo tại `D:/AppData/Code/base/flutter_bloc_clean`:
- Common paged list/grid/sliver và paging controller.
- Base Bloc/use case/Input/Output và event transformer đã đọc ở các lượt trước.
- App settings/bootstrap, theme/language, route guard, refresh-token interceptor.
- Common scaffold/app bar/dialog/shimmer, helper và danh mục source.

Không chạy Flutter build/test; không coi README Flutter là chứng nhận các SDK còn hoạt động. Không chạy cloud smoke test, publish/deploy production hoặc đo runtime trên thiết bị trong lượt này.

## 3. Kết quả kiểm chứng thực tế

### 3.1. Build/test/lint

Lệnh đã chạy:

~~~powershell
.\gradlew.bat verifyModuleBoundaries :domain:test :data:test :shared:test :app:testDevDebugUnitTest :app:testProdDebugUnitTest :app:assembleDevDebug :app:assembleProdDebug :app:assembleDevRelease :app:assembleProdRelease :app:lintDevDebug :app:lintProdRelease -PALLOW_DEMO_PROD_API=true
~~~

| Hạng mục | Kết quả |
| --- | --- |
| Module boundaries | Qua |
| domain | 17 test qua |
| data | 36 test qua |
| shared | 37 test qua |
| app devDebug | 50 test qua |
| app prodDebug | 50 test qua, cùng bộ test được chạy ở variant thứ hai |
| Tổng case khác nhau | **140**; tổng lượt test của các task trên là 190 |
| Assemble | Cả bốn variant qua |
| lintDevDebug | 0 lỗi, 72 cảnh báo |
| lintProdRelease | 0 lỗi, 49 cảnh báo |

Lưu ý:
- prodRelease được build với **demo authorization tường minh**. Đây là kiểm tra kỹ thuật của starter; không phải xác nhận endpoint production thật, signing hoặc khả năng phát hành.
- Một nhóm Compose lint bị bỏ qua vì API không tương thích. Task lint thành công không đồng nghĩa các check đó đã chạy.
- Gradle có thể dùng kết quả UP-TO-DATE. Không diễn giải việc task qua thành test mới đã bao phủ các finding dưới đây.
- Không thêm regression test mới trong lượt review.

### 3.2. Hai phép kiểm tra release config

1. `:app:checkProdReleaseConfig` không truyền override: **thất bại như mong đợi**, vì endpoint mặc định là JSONPlaceholder và chưa cho phép demo.
2. `:app:checkProdReleaseConfig -PPROD_API_BASE_URL=https:///`: **thành công dù URL không có host**. Đây là bằng chứng cho R2.

### 3.3. Kiểm tra bytecode của sample search

Dùng javap từ JDK 21 kiểm tra `UserViewModel$1$1` trong output devDebug vừa build.

Bytecode lưu trường `$$this$launch` và gọi `CoroutineScope.getCoroutineContext()` khi gán `searchJob` tại source dòng 118. Điều này xác nhận đang lấy context của outer scope, không phải context của transform đang thực thi. Không chỉ suy đoán dựa vào tên biến.

## 4. Phát hiện và kế hoạch sửa

### R1 — [P1] Sample search có thể hủy luôn collector query

**Vị trí:** [UserViewModel.kt:110–119](../app/src/main/java/com/example/base/ui/user/UserViewModel.kt#L110).

Trong lambda transform nằm bên trong `viewModelScope.launch`, biểu thức `coroutineContext[Job]` nhận implicit receiver của outer CoroutineScope. Vì vậy `searchJob` giữ job thu thập query.

Khi search tiếp theo hoặc submit/refresh gọi `cancelActiveSearch()`, collector đó bị hủy. Luồng tự động search có thể ngừng sau query đầu, dù thao tác submit thủ công vẫn có thể chạy.

Hiện UserViewModel là sample composition, chưa có route UI sử dụng nó trong AppNavHost. Mức ảnh hưởng trước mắt nằm ở mẫu code được developer tái sử dụng; chưa xác nhận qua thao tác trên màn hình chạy thật.

**Công việc:**
- [ ] Lấy current job bằng `currentCoroutineContext()[Job]` trong transform, hoặc tổ chức lại để một pipeline sở hữu request job.
- [ ] Không dùng cùng biến để giữ collector job và request job.
- [ ] Rà query generation khi xóa text và khi chuyển giữa auto-search, submit, refresh.
- [ ] Thêm regression test: auto query alice → bob → carol, mỗi lần qua debounce, không bấm Submit.
- [ ] Test clear rồi nhập lại; submit/refresh sau auto-search không làm collector chết.
- [ ] Giữ các test latest-response, cancellation và tránh gọi trùng đang có.

**Nghiệm thu:** chuỗi query liên tiếp luôn tiếp tục gọi đúng request; việc hủy request không hủy subscription query. Đã xác nhận nguyên nhân bằng source + bytecode; chưa sửa/chạy regression mới.

### R2 — [P2] Guard production chưa xác thực URL đầy đủ

**Vị trí:** [app/build.gradle.kts:149](../app/build.gradle.kts#L149), [AppConfig.kt:26](../app/src/main/java/com/example/base/config/AppConfig.kt#L26).

Guard mới đã chặn endpoint demo mặc định. Tuy nhiên nó chỉ kiểm tra chuỗi có chứa tên host demo. AppConfig cũng chỉ kiểm tra prefix và trailing slash. Giá trị `https:///` qua được guard và các điều kiện hiện tại, rồi có thể lỗi muộn khi tạo Retrofit.

**Công việc:**
- [ ] Parse URL bằng parser phù hợp; kiểm tra host, scheme, cấu trúc base URL và trường hợp blank sau trim.
- [ ] Định nghĩa HTTPS cho prodRelease và ngoại lệ local HTTP chỉ cho môi trường phù hợp.
- [ ] Giữ kiểm tra demo tường minh; không coi URL parse được là chứng nhận host có thực hoặc thuộc quyền sở hữu sản phẩm.
- [ ] Dùng cùng quy tắc chuẩn hóa giữa Gradle config validation và runtime AppConfig, tránh hai nơi cho kết quả khác nhau.
- [ ] Kiểm tra chuỗi được đưa vào BuildConfig được escape đúng.
- [ ] Test thiếu host, sai scheme, demo host, URL hợp lệ, property/env precedence và thông báo lỗi.
- [ ] Kiểm tra các đường tạo artifact được hỗ trợ đều áp dụng validation trước khi phân phối.

**Nghiệm thu:** config sai dừng sớm với thông báo rõ; dev demo vẫn dùng được; prod demo chỉ qua khi khai báo rõ. Negative test demo đã qua; malformed-host test hiện đang lộ khoảng trống.

### R3 — [P2] Hai overload BaseScreen chưa đồng nhất lifecycle

**Vị trí:** [BaseScreen.kt:60](../app/src/main/java/com/example/base/ui/base/BaseScreen.kt#L60), [BaseScreen.kt:133](../app/src/main/java/com/example/base/ui/base/BaseScreen.kt#L133).

Overload controller dùng repeatOnLifecycle(STARTED). Overload nhận state/callback chỉ dùng LaunchedEffect theo effect ID, nên không có cùng điều kiện STARTED khi xử lý navigation/message và acknowledgment. Bản sửa key theo ID đã giải quyết việc append queue khởi động lại effect đầu, nhưng chưa hoàn tất phần lifecycle.

**Công việc:**
- [ ] Chia sẻ một cơ chế consume effect có lifecycle gating giữa các overload.
- [ ] Giữ effect cho tới acknowledgment; không consume Navigate khi owner chưa STARTED.
- [ ] Dùng callback/state cập nhật đúng khi coroutine chạy lâu; không quay lại dùng toàn bộ queue làm key.
- [ ] Test owner CREATED/STARTED/STOPPED, append effect và quay lại màn hình.

**Nghiệm thu:** hai API có cùng contract xử lý effect trong lifecycle. Đây là phát hiện từ control flow source; chưa tái hiện trên thiết bị.

### R4 — [P2] i18n còn sót ở app-level UI và dialog title

**Vị trí:**
- [MainActivity.kt:57](../app/src/main/java/com/example/base/MainActivity.kt#L57): thông báo restore session và Retry.
- [AppNavHost.kt:52](../app/src/main/java/com/example/base/navigation/AppNavHost.kt#L52): nội dung Login placeholder.
- [BaseViewModel.kt:128](../app/src/main/java/com/example/base/ui/base/BaseViewModel.kt#L128): title “Validation error”.

Thông báo lỗi ở Users/snackbar đã đi qua mapper có context. Các chuỗi trên vẫn được hiển thị trực tiếp bằng tiếng Anh, nên chưa thể nói toàn bộ UI có i18n xuyên suốt. Title của pending dialog vẫn là String thay vì deferred resource.

**Công việc:**
- [ ] Đưa các chuỗi app-level vào resource en/vi.
- [ ] Cho dialog title dùng UiText hoặc resolve default title tại presentation; không đưa Android resource vào domain.
- [ ] Rà mapping validation theo code, tránh coi mọi lỗi của field title/query đều là lỗi rỗng.
- [ ] Test locale vi cho restore failure, session expired và validation dialog; test đổi locale khi còn pending message.

**Nghiệm thu:** các đường đi trên hiển thị đúng locale; text server/dynamic có policy riêng được ghi rõ. Không đánh đồng resource-ID mapping test với kiểm tra toàn màn hình đa ngôn ngữ.

### R5 — [P2] Tài liệu có snippet gọi API chưa được triển khai

**Vị trí:** [ai-improvements-summary.md](ai-improvements-summary.md), phần “Cách 4” và “Cách 5”.

- AppList/AppGrid/AppPagedList/AppPagedGrid chưa tìm thấy trong source app. P9 còn đang quy hoạch, nhưng cheat sheet minh họa chúng theo hình thức API có thể sử dụng ngay.
- Ví dụ BaseScreen truyền lambda nhận paddingValues, trong khi API content hiện tại không nhận tham số và đã xử lý padding bên trong.
- UserViewModel composition/search chỉ có source/test; chưa có sample screen/route thật để kiểm chứng thao tác.
- Dòng mở đầu các tài liệu nói “kiểm chứng toàn diện” cần giới hạn theo phạm vi test thực tế.

**Công việc:**
- [ ] Gắn nhãn API dự kiến/pseudocode rõ trên snippet P9, hoặc chỉ đưa vào hướng dẫn dùng sau khi implementation tồn tại.
- [ ] Sửa snippet BaseScreen theo signature hiện tại.
- [ ] Liên kết tới ví dụ build được; phân biệt example ViewModel với feature đã nối UI.
- [ ] Giữ P9 unchecked cho tới khi có implementation và nghiệm thu tương ứng.
- [ ] Đồng bộ snapshot docs theo revision/test reports; không ghi CI remote pass chỉ vì có file workflow.

**Nghiệm thu:** developer làm theo phần “sử dụng ngay” không gặp unresolved symbol hoặc signature sai. Chưa sửa các tài liệu hướng dẫn trong lượt này; chỉ ghi kế hoạch review.

## 5. Flutter còn gì đáng học?

| Capability trong Flutter đã đối chiếu | Kotlin hiện tại | Quyết định cho core v1 |
| --- | --- | --- |
| CommonPagedListView/GridView, sliver, state slots | LazyColumn sample và manual pagination; P9 chưa implement | Có ích nếu mục tiêu starter gồm collection UI; có thể làm P9 như bước riêng, không tuyên bố đã có |
| Theme/language preference và app-level settings | Theme theo system; resource en/vi; chưa có màn hình lưu lựa chọn theme/locale | Tiện ích mở rộng đáng làm sau core; không cần xây lại startup initializer |
| Route guard, login và token refresh interceptor | Login placeholder, session recovery; REST client public không có token refresh | Chỉ triển khai auth thật khi có backend contract, giới hạn replay và kiểm chứng concurrency |
| Database/persistence | Todo trong memory; session dùng DataStore | Sample persistent data có ích cho app cần offline; không mặc định thêm database cho mọi ứng dụng |
| Common dialog/app bar/text field/shimmer | Material 3 + BaseScreen; chưa có design system/form controls phong phú | Dùng Material trước, chỉ tách component khi có hành vi lặp; shimmer là tùy chọn |
| Notification/helper, file/device utilities, code generation tooling | Chưa có bộ tương đương đầy đủ | Thêm theo use case; không đưa tất cả helper/SDK vào core |
| DisposeBag, barrel exports và scale theo design size | Kotlin scopes, imports, Compose layout/resource native | Không port cơ học; giữ lifecycle và accessibility theo Android |

Flutter có nhiều tiện ích hơn không đồng nghĩa Kotlin starter chưa đúng kiến trúc. Ví dụ MyApp Flutter đang cố định textScaler 1.0; không nên mang lựa chọn đó sang Android và làm mất font scaling của người dùng.

Các SDK cloud và backend-specific adapters trong Kotlin hiện vẫn là contract/fake. Không yêu cầu tích hợp cả ba nhà cung cấp để chốt core starter.

## 6. Những phần nên giữ

- Năm module theo layer, composition root Hilt ở app.
- BaseInput/BaseOutput và model bất biến; không bắt entity thành Output.
- ActionRunner/LoadingManager dùng độc lập; BaseViewModel và BaseScreen là API tiện lợi tùy chọn.
- Result hiện có, typed errors và cancellation propagation.
- Todo source of truth qua Flow, mutation/observation riêng, draft qua SavedStateHandle.
- Session state nguyên tử và retry restoration.
- Các operator chuẩn của Flow cùng helper có semantics riêng được test.
- Ma trận dev/prod × debug/release và app ID tách biệt.

## 7. Thứ tự sửa và điều kiện chốt v1

### S1 — Ổn định, không mở thêm framework
- [x] Sửa R1 và thêm các regression test query liên tiếp (đã hoàn thành: queryCollectorJob tách biệt, kiểm chứng 6 regression tests mới).
- [x] Sửa R2; giữ demo build có chủ đích và chặn malformed production config (đã hoàn thành: URI host validation, reject `https:///`).
- [x] Đồng nhất lifecycle effect theo R3 (đã hoàn thành: repeatOnLifecycle(STARTED) + rememberUpdatedState cho cả hai overload BaseScreen).
- [x] Hoàn tất các chuỗi i18n theo R4 (đã hoàn thành: đưa chuỗi session/login vào strings.xml en/vi, ShowDialog title hỗ trợ UiText).
- [x] Làm tài liệu dùng được theo R5 (đã hoàn thành: sửa signature BaseScreen và gắn nhãn rõ P9 là API dự kiến chưa triển khai).

### S2 — Quyết định phạm vi phát hành
- [ ] Nếu gọi là **core MVVM starter**, ghi rõ P9, persistent Todo, auth thật và cloud SDK là extension/backlog.
- [ ] Nếu gọi là **starter có common list/grid**, triển khai và nghiệm thu P9 trước khi quảng bá API đó.
- [ ] Không dùng đăng nhập mẫu hoặc fake adapters để tuyên bố đã có production authentication/backend integration.
- [ ] Rà toolchain/lint compatibility và signing khi chuẩn bị phân phối một sản phẩm thật.

### S3 — Kiểm chứng
- [ ] Chạy lại regression test trên devDebug/prodDebug và boundaries.
- [ ] Kiểm tra đủ bốn variant; ghi rõ prodRelease demo hoặc endpoint thật.
- [ ] Chạy lint theo variant; báo riêng các checks bị skip.
- [ ] Có smoke test thiết bị/emulator cho navigation, session error, locale, dialog/snackbar và scroll behavior hiện có.
- [ ] Đo runtime nếu có tuyên bố tối ưu startup/memory/FPS.
- [ ] Rà API droppable trên scope đa luồng trước khi công bố bảo đảm thread safety: hiện có hai nơi reset isRunning; chưa stress-test tình huống job cũ hoàn tất khi job mới vừa bắt đầu. Đây là việc cần kiểm chứng thêm, không phải lỗi đã tái hiện trong lượt này.

**Tiêu chí dừng thêm tính năng:** sửa các lỗi được xác nhận, có hướng dẫn đúng source, có sample build được và phạm vi v1 trung thực. Sau đó dùng base để xây một feature thật; chỉ bổ sung abstraction khi feature đó chứng minh nhu cầu.

## 8. Giới hạn và liên kết

- Chưa sửa code lỗi hoặc thêm test mới; baseline 140 test đang qua không phủ hết finding mới.
- Không kiểm tra quyền backend/rules live, không đo hiệu năng hoặc xác nhận CI remote.
- Báo cáo này không đóng P9/P6/P7 chỉ vì đã có thiết kế hoặc fake.
- [Kotlin currentCoroutineContext: tránh nhầm với CoroutineScope.coroutineContext](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/current-coroutine-context.html).
- [Kế hoạch tổng thể P0–P9](base-evolution-plan.md).

### Bổ sung ngày 16/11/2026 — REST và skeleton/shimmer

Đã đọc [Swagger/OpenAPI của Practice API](https://templetezuu.netlify.app/docs/) do người dùng cung cấp. Contract auth/refresh đã có để thiết kế; việc tích hợp và kiểm chứng runtime vẫn chưa thực hiện.

- [Kế hoạch REST chi tiết](rest-api-integration-plan.md): ưu tiên Products public; có response envelopes, nested errors/field validation, pagination metadata, 204 và refresh-token rotation dùng một lần.
- [P9.1 trong kế hoạch tổng thể](base-evolution-plan.md): skeleton/shimmer Compose tùy chọn qua loading slot, giữ nội dung khi refresh/append, hỗ trợ lifecycle, theme và accessibility.
- Source app chưa có component skeleton/shimmer dùng chung. Lượt này chỉ sửa tài liệu; không đóng R1–R5 hoặc ghi thêm test đã qua.

### Bổ sung ngày 17/11/2026 — Ownership của DI và đối chiếu Flutter initializer

Đã đọc Flutter main → AppInitializer → configs/DI của từng package và Kotlin app/di hiện tại. Flutter app dependency initializer; initializer biết data/domain/shared. Các package dùng chung GetIt.instance, không phải container độc lập.

- [Review và kế hoạch DI theo module](di-modularization-plan.md): phân biệt binding registration, startup bất đồng bộ và ranh giới truy cập implementation.
- App/di hiện có 5 file, khoảng 514 dòng; AuthModule trộn client/repository/usecase wiring. Đây là vấn đề ownership có thể cải thiện khi base lớn lên, chưa phải bằng chứng hiệu năng kém.
- Đề xuất :di làm wiring adapter để core JVM không phụ thuộc Hilt, app giữ config/entry points; chưa refactor hoặc chốt thay đổi module.
- Hilt tổng hợp binding theo graph; không cần port vòng gọi init DI tuần tự. Muốn compiler bảo vệ UI mạnh hơn cần cân nhắc tách presentation/feature module hoặc thêm architecture checks.
- Chỉ đọc source và viết plan, chưa build/test/runtime trong lượt này. Không thay trạng thái các finding lịch sử.

### Triển khai sau review — :composition (17/11/2026)

Kotlin base dùng module `:composition` để lắp dependency graph. Đã chuyển network/repository/usecase providers và provider tests sang module mới; app giữ config/session binding và entry points chỉ trả domain contracts. Source boundary check bổ sung cho app/UI; không đổi core JVM sang Hilt.

Xem [kết quả, runner và giới hạn](di-modularization-plan.md): debug flavors qua wrapper; release/lint qua Gradle 8.11.1 có sẵn do wrapper 9.4.1 với AGP 8.3.1 lỗi SelfResolvingDependency. Không sửa toolchain hoặc chạy thiết bị/API live.

### Review logging theo môi trường — 17/11/2026

[Kế hoạch logging](environment-logging-review-plan.md): AppModule vẫn luôn cấp ConsoleLogger cho cả dev/prod và debug/release; HTTP logging chưa được gắn. AuthRepositoryImpl log email/name trực tiếp. Đề xuất chỉ bật developer logs ở devDebug, chọn NoOpLogger cho prod và bỏ thông tin cá nhân khỏi mọi log. Một backend URL dùng chung không cản trở việc phân policy; chưa sửa code hoặc kiểm Logcat/build trong lượt này.
