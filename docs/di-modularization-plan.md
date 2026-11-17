# Review và kế hoạch tổ chức DI theo module

Ngày: **17/11/2026**. Trạng thái: **Đã triển khai phương án C — module :composition**; kết quả và giới hạn kiểm chứng ghi dưới đây.

## Quyết định triển khai

Người dùng đã chọn một module chuyên wiring DI. Tên chốt là **:composition**, package com.example.base.composition: lắp dependency graph, không phải chạy startup tuần tự. Quyết định này thay thế ưu tiên B trong phần thảo luận bên dưới; các phương án được giữ làm lịch sử thiết kế.

- [x] Baseline verifyModuleBoundaries và :app:testDevDebugUnitTest thành công (test task UP-TO-DATE; không tuyên bố chạy mới từng case).
- [x] Tạo Android library :composition; giữ domain/data/shared không phụ thuộc Hilt.
- [x] Chuyển wiring network/repository/usecase; app giữ BuildConfig/runtime config và session adapter.
- [x] Chuyển provider tests; graph tests app chỉ sử dụng domain contracts.
- [x] Bỏ app dependency trực tiếp data/network SDK, thêm source boundary check; Hilt aggregation compile được khi data là implementation dependency của composition. Không tuyên bố chặn mọi truy cập bằng compiler.
- [x] Chạy kiểm tra, cập nhật README en/vi, architecture, testing và CI với bằng chứng thực tế.

Đích dependency: app → composition/domain/shared/resources; composition → data/domain/shared; data → domain/shared. Không có composition → app.

Phạm vi không bao gồm thay đổi logic auth, thay endpoint Practice đang dùng, đổi toolchain hoặc tách UI thành feature modules.

[Review v1](base-v1-review-plan.md)

## Kết quả triển khai 17/11/2026

Module được tạo: **:composition**, namespace/package **com.example.base.composition**. Năm module cũ giữ vai trò cốt lõi; tổng cộng sáu Gradle modules.

- app/di giữ AppModule cùng AuthEntryPoint/ProductEntryPoint chỉ expose domain contracts.
- composition/network: NetworkModule và cấu hình từ app qua composition/config/NetworkConfig.
- composition/auth và composition/product: network providers/qualifiers theo backend.
- composition/repository: TodoUserRepositoryModule, ProductRepositoryModule, AuthRepositoryModule.
- composition/usecase: TodoUserUseCaseModule, ProductUseCaseModule, AuthUseCaseModule.
- 16 provider tests chuyển khỏi app, thêm 1 MockWebServer test chứng minh base path/query từ config app đến service.
- App không khai báo dependency data/Retrofit/OkHttp/Gson. Core JVM không thêm Hilt dependency.
- verifyModuleBoundaries thêm kiểm app source references và UI không gọi composition. Đây là text-based source check, không thay thế semantic review.
- App bật enableAggregatingTask để Hilt nhìn thấy bindings qua implementation dependencies.
- CI chạy composition debug tests và upload report; chưa chạy CI remote.

### Kiểm chứng thực tế

| Hạng mục | Kết quả | Runner |
| --- | --- | --- |
| Gradle sync | Thành công | Android Studio |
| Boundaries, app devDebug/prodDebug tests và APKs | Thành công | Wrapper Gradle 9.4.1 |
| Composition debug | 17 tests, 0 failures/skipped | Wrapper Gradle 9.4.1 |
| Core JVM tasks | Qua, các task test UP-TO-DATE; reports domain 39/data 54/shared 37 | Wrapper Gradle 9.4.1 |
| App reports | 87 tests ở mỗi debug flavor, 0 failures/skipped; gồm 2 graph tests Hilt thật | Wrapper Gradle 9.4.1 |
| Composition release | 17 tests qua | Gradle 8.11.1 đã có trên máy |
| devRelease/prodRelease APK | Qua; dùng -PALLOW_DEMO_PROD_API=true | Gradle 8.11.1 |
| Composition lintDebug | 0 errors, 0 warnings | Gradle 8.11.1 |
| App lintDevDebug/lintProdRelease | 0 errors; lần lượt 72/49 warnings | Gradle 8.11.1 |

Reports hiện có **234 case khác nhau**: 39 domain + 54 data + 37 shared + 17 composition + 87 app. Chạy lại theo flavor/build type không làm tăng số case khác nhau. Không nói toàn bộ 234 case đã chạy mới vì core tasks tái sử dụng kết quả.

**Giới hạn toolchain:** thử release/lint qua wrapper 9.4.1 dừng khi AGP 8.3.1 load class SelfResolvingDependency không còn tồn tại. Lệnh lint riêng với --stacktrace xác nhận lỗi ở AGP VariantDependencies. Dùng executable Gradle 8.11.1 sẵn có để kiểm bổ sung, không sửa wrapper/version catalog. Vì vậy không tuyên bố release/lint đã qua với wrapper hiện tại; CI chỉ thêm debug composition test phù hợp các task đã chạy qua wrapper.

Không chạy UI trên thiết bị/emulator, business API live, signing/phát hành hay đo startup/FPS. Không thay logic auth/session/search hoặc endpoint Practice.

> Các mục 1–9 bên dưới lưu lại phần khảo sát và phương án trước triển khai; quyết định và kết quả phía trên là trạng thái hiện tại.

## 1. Kết luận và phạm vi

Lo ngại của người dùng có cơ sở: app/di đang sở hữu cả wiring của API client, repository implementation và use case. Cấu trúc hiện tại hợp lệ để lắp ứng dụng, nhưng app phải biết quá nhiều chi tiết và là nơi sửa chung khi thêm feature.

Sau trao đổi bổ sung ngày 17/11/2026, phương án **DI do từng module sở hữu (B)** là ứng viên ưu tiên theo mục tiêu của người dùng. :initializerModule có thể tập hợp dependencies nhưng không phải nơi phải viết lại mọi binding hoặc gọi init tuần tự. Chưa chốt cấu hình hay thay đổi implementation.

Phương án **:di adapter (C)** trước đó vẫn hợp lý nếu yêu cầu là core hoàn toàn độc lập framework DI; đó là lựa chọn thiết kế, không phải giới hạn bắt buộc của Kotlin/JVM.

Đính chính: module JVM có thể dùng javax.inject hoặc Dagger/Hilt core phù hợp. Không đồng nhất phụ thuộc DI với phụ thuộc Android, và không yêu cầu tạo một Gradle module chỉ vì mỗi lớp cần injection.

Lượt này đọc source, Gradle và tài liệu Hilt; chỉ thêm/cập nhật plan. Không build/test/run, không kiểm tra API live hoặc đóng các finding lịch sử.

## 2. Bằng chứng Flutter đã đọc

Root: D:/AppData/Code/base/flutter_bloc_clean. Không đọc bản .kilo/worktrees.

| File | Điều đã thấy |
| --- | --- |
| app/lib/main.dart:17 | Await AppInitializer(AppConfig.getInstance()).init() trước load initial resource và runApp |
| app/pubspec.yaml | App có dependency domain/shared/resources/initializer; không khai báo trực tiếp data |
| initializer/pubspec.yaml | Initializer có dependency data/domain/shared |
| initializer/lib/src/app_initializer.dart:12 | EnvConstants.init(); await SharedConfig.init(), DataConfig.init(), DomainConfig.init(), rồi applicationConfig.init() |
| shared/lib/src/config/config.dart | Config.init() dùng AsyncMemoizer.runOnce(config) trên instance |
| data/lib/src/config/data_config.dart | DataConfig gọi DI của data |
| domain/lib/src/config/domain_config.dart | DomainConfig gọi DI của domain |
| app/lib/config/app_config.dart | AppConfig gọi DI app và làm setup UI/notification |
| */di/di.dart | Mỗi package có configureInjection(), dùng GetIt.instance và injectable-generated init |
| data/lib/src/di/di.dart | ServiceModule có preResolve cho SharedPreferences, secure storage và ObjectBox |

Như vậy, Flutter thực sự phân tán chỗ đăng ký DI và có package điều phối startup. Các package dùng cùng GetIt.instance; không phải mỗi package có một container hoàn toàn cách ly.

Tách initializer không làm data biến mất khỏi ứng dụng; nó chuyển dependency trực tiếp và kiến thức wiring sang package điều phối. Không kết luận Dart đã cấm UI import implementation chỉ từ việc pubspec app không liệt kê trực tiếp data.

Không tìm thấy di.config.dart được generate trong checkout bằng công cụ tìm file. Các wrapper configureInjection() đang trả void trong source; chưa kiểm chứng việc chờ preResolve bất đồng bộ hoặc graph generated. Thứ tự await các Config đã đọc không đủ chứng minh mọi service đã khởi tạo xong. Không coi đây là lỗi runtime đã tái hiện.

## 3. Bằng chứng Kotlin hiện tại

| File | Trách nhiệm hiện tại |
| --- | --- |
| app/di/AppModule.kt — 50 dòng | Dispatcher, logger, application scope, DataStore session |
| app/di/DataModule.kt — 73 dòng | Gson/error parser, config, HTTP/Retrofit, Todo/User repository |
| app/di/DomainModule.kt — 44 dòng | Providers của Todo/User use cases |
| app/di/ProductModule.kt — 114 dòng | Practice Retrofit/API, ProductRepositoryImpl và Products use cases |
| app/di/AuthModule.kt — 233 dòng | Public/protected clients, interceptor/token storage, repository, use cases và API delegate |
| app/build.gradle.kts | App dependency trực tiếp data/domain/shared/resources và network SDK |
| data/build.gradle.kts, domain/build.gradle.kts | Kotlin/JVM, chưa dependency Hilt |
| app/MainApplication.kt | @HiltAndroidApp |
| build.gradle.kts | verifyModuleBoundaries kiểm tra project dependencies; chưa kiểm tra source import theo package |

Có 5 file, khoảng 514 dòng trong app/di. Số dòng không tự chứng minh hiệu năng kém; vấn đề là ownership, phạm vi thay đổi và khả năng ngăn UI dùng implementation.

AuthModule và ProductModule trộn wiring network/data/domain trong cùng file. AppModule còn gắn với DataStoreSessionManager nằm trong app, nên không thể chuyển tất cả file sang :di rồi để :di dependency ngược app.

## 4. Ba vấn đề phải tách riêng

1. **Nơi khai báo binding:** file nào biết cách tạo API/repository/use case.
2. **Khởi tạo runtime:** công việc có side effect hoặc bất đồng bộ nào phải chạy trước/sau UI.
3. **Ranh giới truy cập:** compiler/static analysis có ngăn Screen/ViewModel import implementation hay không.

Với Hilt, binding nằm trong module dependency có @Module/@InstallIn được tổng hợp vào graph; không cần app gọi từng initDataDi()/initDomainDi() theo thứ tự thủ công. Việc tạo instance phụ thuộc vào request và scope của graph; @Singleton không có nghĩa mọi object đều được tạo ngay lúc app mở.

Công việc như restore session, mở database hoặc chuẩn bị SDK bất đồng bộ vẫn cần hợp đồng startup riêng nếu có nhu cầu. Không dùng DI providers để thực hiện network request hoặc chặn main thread chờ startup.

Nguồn: [Hilt modules](https://dagger.dev/hilt/modules.html), [Hilt multi-module](https://developer.android.com/training/dependency-injection/hilt-multi-module). Catalog hiện dùng Hilt 2.51.1; cách cấu hình aggregation/codegen phải kiểm chứng trên toolchain thực tế, không tự nâng phiên bản trong refactor.

## 5. Các phương án

| Phương án | Ưu điểm | Đánh đổi |
| --- | --- | --- |
| A. Giữ 5 module, chia app/di thành file/package nhỏ | Ít thay đổi build; core tiếp tục độc lập framework DI | App vẫn dependency data, UI vẫn thấy implementation public |
| B. Đặt DI trực tiếp trong từng module sở hữu — ứng viên ưu tiên | Binding gần code; app không phải tạo từng repository/use case | Chấp nhận DI annotations/core và processing phù hợp; JVM vẫn có thể giữ không Android; cần kiểm chứng graph/toolchain |
| C. Thêm :di làm wiring adapter — phương án giữ core không DI | App source không cần tạo data implementation; domain/data giữ Kotlin/JVM không Hilt | Thêm module thứ sáu, cần xử lý config/test/codegen/classpath; không tự bảo đảm mọi public data type bị che |
| D. Wiring adapters riêng theo feature/layer | Ownership phân tán, thay bindings theo feature dễ hơn | Nhiều module và build configuration; chưa cần tạo hàng loạt adapter khi base còn nhỏ |

Không có yêu cầu bắt buộc chọn Hilt thay GetIt hoặc ngược lại. Giữ Hilt hiện tại; học cách phân trách nhiệm từ Flutter.

## 6. Phương án B — DI thuộc module sở hữu

Người dùng đề xuất domain/data tự khai báo DI; app giữ binding cấp ứng dụng, có thể thêm :initializerModule làm nơi tập hợp dependency. Hướng này hợp lý và phù hợp nhu cầu giảm ownership tập trung trong app/di. Chưa có thay đổi implementation hoặc kết luận build.

### Phân công binding

| Nơi sở hữu | Binding hoặc contract |
| --- | --- |
| :domain | Repository interfaces và use cases. Ưu tiên constructor injection qua javax.inject.Inject cho class tự tạo được; provider/module riêng khi có lý do cụ thể |
| :data | RepositoryImpl, binding Repository interface → implementation, API services/data sources và factory cần biết data types |
| :app | @HiltAndroidApp, BuildConfig/runtime config, binding đặc thù app; có thể giữ HTTP client dùng chung nếu đây là lựa chọn cấp app |
| :shared hoặc contract DI phù hợp | Qualifier/config abstractions dùng chung nếu cần; không biến shared thành nơi chứa implementation |
| :initializerModule — tùy chọn | Tập hợp dependency/chọn wiring theo variant. Hilt tự tổng hợp modules có @InstallIn; không cần class liệt kê lệnh init từng module |

Domain giữ interface UserRepository; binding sang UserRepositoryImpl phải nằm ở data hoặc adapter biết implementation. Nếu domain import implementation của data để tự bind repository thì tạo cạnh phụ thuộc sai và có thể thành cycle.

### Mức phụ thuộc DI là một lựa chọn

- javax.inject.Inject/Qualifier/Singleton là annotation DI tiêu chuẩn, không phải Android SDK. Domain có thể chỉ dùng constructor injection và chưa cần DomainHiltModule.
- Nếu thật sự cần @Module/@Provides/@InstallIn trong module JVM, cấu hình Dagger/Hilt core và annotation processing tương ứng. Không mặc định module JVM phải đổi sang Android library.
- Không áp com.google.dagger.hilt.android plugin vào module Kotlin/JVM chỉ vì nó dùng một annotation DI; phân biệt hilt-core với Android entry-point integration.
- Dùng constructor injection không làm mất khả năng tạo object thủ công trong test.
- Chọn B nghĩa là chấp nhận phụ thuộc DI tiêu chuẩn hoặc Hilt/Dagger core ở module sở hữu; không tiếp tục tuyên bố chúng hoàn toàn độc lập mọi framework DI.
- Ưu tiên mức dependency nhỏ nhất đáp ứng nhu cầu. Nếu use case cần DispatcherProvider/logger/errorMapper, graph vẫn phải cung cấp đủ các dependency đó; default constructor argument không thay thế binding.

Bằng chứng tham khảo chính thức: [Now in Android HiltConventionPlugin](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/HiltConventionPlugin.kt) có nhánh JVM dùng hilt-core và nhánh Android dùng hilt-android/plugin. Đây là bằng chứng khả năng tổ chức, không phải cấu hình đã kiểm chứng cho Hilt 2.51.1 của base.

### Vai trò :initializerModule

Có thể có dependency app → initializerModule → data/domain; data → domain vẫn giữ nguyên. App cũng sử dụng domain cho UI. Sơ đồ chỉ nói dependency dự kiến, không phải lời gọi init runtime.

Nếu module chỉ gom dependency Gradle, có thể giữ nó mỏng hoặc bỏ để app dependency các module cần thiết trực tiếp. Chỉ thêm khi việc chọn tập module/variant hoặc đóng gói wiring đem lại lợi ích cụ thể. Không cần @Module(includes = ...) liệt kê lại tất cả Hilt modules đã có @InstallIn.

HTTP client chung do app cung cấp có thể được data sử dụng qua graph mà data không dependency app: graph supply object khác với cạnh dependency Gradle. Tuy nhiên factory tạo PracticeAuthApi/DTO-specific delegate nên ở data, tránh app vẫn phải import data types.

Binding registration và startup bất đồng bộ vẫn là hai việc riêng. Tên initializerModule không có nghĩa nó tự chạy tất cả providers hoặc buộc có startup task.

## 7. Phương án C tham khảo — wiring adapter độc lập

Sơ đồ rút gọn, mũi tên là project dependency dự kiến; chưa phải cấu trúc hiện có:

~~~mermaid
flowchart LR
    App[":app — UI, config, entry points"] --> Domain[":domain"]
    App --> DI[":di — Hilt wiring"]
    DI --> Data[":data"]
    DI --> Domain
    Data --> Domain
~~~

Shared/resources và các cạnh liên quan được lược để dễ đọc.

- :di là Android library tương thích Hilt hiện tại.
- Chia packages/files trong :di theo network, repository, usecase và từng năng lực như auth/products; không gom toàn bộ vào một DiInitializer khổng lồ.
- :domain, :data, :shared tiếp tục không import Hilt/Android DI types.
- :di không dependency :app và không tham chiếu app.BuildConfig hoặc app.AppConfig.
- App chuyển BuildConfig thành runtime config contract phù hợp, rồi cung cấp qua Hilt. Contract chứa giá trị cấu hình cần thiết, không chứa DTO/data implementation.
- AppModule có thể giữ session implementation nằm trong app và cung cấp SessionManager của domain. Các binding từ :di tiêu thụ interface; không import DataStoreSessionManager ngược từ app.
- Khi chuyển config, giữ rõ các endpoint JSONPlaceholder/Practice và dev/prod; không làm mất qualifier public/protected hoặc vô tình tạo client refresh đệ quy.
- Không mở rộng refactor thành thay đổi hành vi auth/session/search hay đổi DI framework.

### Ranh giới UI và data

Bỏ dòng dependency data trực tiếp trong app không đủ để tuyên bố app không thể thấy data:

- Phân biệt implementation/api trong Gradle và kiểm tra dependency compileClasspath/runtimeClasspath.
- Hilt cần các module/binding trong transitive dependencies của nơi compile Application. Kiểm chứng codegen của project thay vì hứa che data tuyệt đối.
- Nếu app test đang import data hoặc gọi provider trực tiếp, chuyển test phù hợp sang :di; không thêm api(data) chỉ để mọi test cũ tiếp tục nhìn thấy data.
- Thêm kiểm tra source architecture cấm UI/ViewModel import data/Retrofit/OkHttp hoặc lấy implementation qua locator; kiểm tra cả package nằm ngoài ui nếu có ViewModel tại đó.
- Muốn compiler bảo vệ UI mạnh hơn: tách :presentation hoặc regular Android feature modules chỉ dependency domain/UI contracts. App tiếp tục lắp :di/data trong binary cuối; Hilt không buộc presentation dependency implementation.
- Tách UI là bước riêng theo quy mô; không đồng thời tạo nhiều feature modules chỉ để làm folder app/di ngắn hơn.

## 8. Các bước triển khai có thể review

### DI-0 — Chốt ownership và baseline

- [ ] So sánh B (module sở hữu DI, ứng viên ưu tiên) và C (adapter giữ core không DI); ghi rõ mức dependency được chấp nhận.
- [ ] Chốt :initializerModule có lợi ích tập hợp dependency/variant cụ thể hay không; không mặc định phải có.
- [ ] Ghi revision và baseline; liệt kê binding/qualifier/scope, các provider default parameters và test gọi provider trực tiếp.
- [ ] Chốt runtime config boundary, app-owned session binding và dependency matrix không cycle.

### DI-1 — Thử một lát cắt theo B

- [ ] Chọn Products hoặc Users để thử module-owned DI.
- [ ] Domain dùng constructor injection hoặc provider cần thiết; cấu hình processor/dependency tối thiểu cho module JVM.
- [ ] Data sở hữu binding repository và API factory; không để domain import implementation.
- [ ] App cung cấp runtime config và binding cấp app; data không import app.BuildConfig hoặc class app.
- [ ] Compile Hilt graph trên toolchain hiện tại trước khi di chuyển tiếp; không tự nâng Hilt/Kotlin/Gradle trong cùng thay đổi.

### DI-2 — Di chuyển phần còn lại

- [ ] Chuyển auth/network/repository/usecase wiring về đúng nơi sở hữu, bảo toàn public/protected qualifiers, scope và hành vi.
- [ ] Tránh khai báo providers lặp hoặc giữ cả provider cũ và mới ngoài ý muốn.
- [ ] Tập hợp dependencies qua :initializerModule nếu đã chốt; không tạo vòng gọi init thủ công.
- [ ] Chuyển test phù hợp, giữ khả năng tạo use case/repository bằng constructor trong JVM tests.
- [ ] Chỉnh module boundaries và docs; không sửa generated Hilt source.
- [ ] Nếu chọn C thay B, thực hiện adapter theo mục 7; không trộn hai chiến lược mà không giải thích ownership.

### DI-3 — Bảo vệ ranh giới và nghiệm thu

- [ ] Source architecture check phát hiện UI import implementation hoặc network SDK bị cấm.
- [ ] Compile graph app cho devDebug/prodDebug và kiểm các release variants theo config phù hợp.
- [ ] Chạy regression domain/data/shared/app và module wiring nếu có; verifyModuleBoundaries/lint.
- [ ] Graph cung cấp Product/Auth/Todo/User use cases, đúng public/protected clients và Singleton scope.
- [ ] Smoke test navigation, session restore, Products, Login/Profile trên emulator/dev phù hợp; không tự gọi mutation live chỉ để refactor DI.
- [ ] Kiểm compileClasspath/runtimeClasspath và aggregation trước khi ghi mức bảo vệ compiler; tách presentation/feature là bước riêng nếu cần.
- [ ] Không thêm GlobalScope hoặc startup blocking không có nhu cầu.
- [ ] Ghi số tests/builds thực chạy, không kế thừa trạng thái Verified từ plan cũ.

## 9. Điều kiện hoàn thành và giới hạn

Hoàn thành refactor khi app source chỉ giữ wiring đặc thù ứng dụng, binding còn lại có nơi sở hữu rõ, không có cycle và graph/behavior checks phù hợp qua. Với B, core giữ JVM không Android và mức DI dependency đã chốt; với C, core tiếp tục độc lập Hilt.

Nếu chỉ chia package trong app/di, ghi rõ đó là phương án A và chưa thay đổi compile-time visibility. Nếu chỉ thêm :di, không gọi đó là hoàn tất việc module hóa toàn bộ feature hoặc chặn tuyệt đối UI truy cập implementation.

Chưa có thay đổi implementation, số test mới hoặc kết quả runtime từ lượt review này. Không suy ra kiến trúc chậm vì có nhiều file DI; chưa đo startup/memory.
