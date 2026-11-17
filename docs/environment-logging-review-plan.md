# Review và kế hoạch logging theo môi trường

Ngày: **17/11/2026**. Trạng thái: **Đang triển khai theo yêu cầu người dùng** — policy chỉ bật ở devDebug, lọc dữ liệu auth và kiểm thử theo variant.

[Review v1](base-v1-review-plan.md) · [Wiring DI hiện tại](di-modularization-plan.md)

## 1. Yêu cầu và kết luận

Người dùng chỉ có một backend URL/database, muốn bản phát triển có log để chẩn đoán, prod không ghi developer logs. Không cần tạo thêm backend dev hoặc database dev để làm được điều này.

Base đã có abstraction AppLogger và NoOpLogger, nhưng **chưa áp dụng chính sách logging theo môi trường**. DI luôn cấp ConsoleLogger nên các đường dùng logger được inject vẫn có thể ghi log ở prod/release.

Có phát hiện riêng cần xử lý: AuthRepositoryImpl đưa email/tên vào log message. Tắt prod logs chưa đủ để coi nội dung log dev đã phù hợp, đặc biệt khi dev cũng dùng backend/dữ liệu thật.

Lượt này chỉ đọc source, viết plan; không đổi cấu hình, endpoint, code logger hoặc gọi API. Không khẳng định đã quan sát log trên thiết bị.

## 2. Bằng chứng source

| Vị trí | Hiện trạng |
| --- | --- |
| app/.../di/AppModule.kt:42 | logger(): AppLogger luôn trả ConsoleLogger(), không nhận AppConfig |
| shared/.../logging/AppLogger.kt | Có ConsoleLogger và NoOpLogger; ConsoleLogger gọi println ở mọi level |
| app/.../config/AppConfig.kt | Có environment và isDebug, nhưng chưa có logging policy |
| app/build.gradle.kts | Có environment flavors và DEBUG theo build type; chưa có logging flag/policy; release minify=false |
| composition/.../network/NetworkModule.kt | Public OkHttp client không có HttpLoggingInterceptor |
| composition/.../auth/AuthNetworkModule.kt | Protected client kế thừa public client và thêm AuthInterceptor; chưa thêm HTTP logging |
| data/.../repository/AuthRepositoryImpl.kt | Login log email; register log email/name; failure messages cũng có email; chưa thấy password/token bị in trực tiếp trong các dòng đã đọc |
| data/.../repository/TodoRepositoryImpl.kt | Log titleLength/id; không log nguyên title |
| Source search main Kotlin | Không tìm thấy HttpLoggingInterceptor, Timber, android.util.Log hoặc System.out ngoài logger println đã đọc |

Default NoOpLogger trong constructor use case/repository không bảo vệ runtime khi DI truyền ConsoleLogger vào. Không coi chữ release tự động xóa println; minification đang tắt và chưa thấy rule strip logs tương ứng.

## 3. Phân biệt hai trục cấu hình

- dev/prod là product flavor: có thể quyết định endpoint, feature/config và logging policy.
- debug/release là build type: quyết định khả năng debug, signing và các lựa chọn build.
- Chỉ kiểm BuildConfig.DEBUG sẽ bật log cả prodDebug; điều đó không đáp ứng chính sách mọi bản prod đều tắt log.
- Cùng URL vẫn có thể dùng config/logging/app ID khác nhau. App ID khác nhau tách app/local storage nhưng không tách dữ liệu backend hoặc biến mutation trên backend chung thành an toàn.
- Không tự xóa flavors hoặc tạo thêm deployment/database trong lượt này.
- Base còn hai sample contract JSONPlaceholder và Practice API. Không đổi mọi client về một URL bằng cách thay chuỗi, vì DTO/contract khác nhau.

Nguồn: [Android build variants](https://developer.android.com/build/build-variants).

## 4. Policy đề xuất

Ưu tiên **developerLoggingEnabled = environment.isDev && isDebug** cho base này: chỉ devDebug ghi developer logs. Đây là đề xuất để triển khai, chưa phải hành vi hiện tại.

| Variant | AppLogger đang được DI cấp | AppLogger đề xuất |
| --- | --- | --- |
| devDebug | ConsoleLogger | ConsoleLogger với nội dung đã lọc |
| devRelease | ConsoleLogger | NoOpLogger |
| prodDebug | ConsoleLogger | NoOpLogger |
| prodRelease | ConsoleLogger | NoOpLogger |

Nếu sau này cần devRelease có log để kiểm nội bộ, có thể chọn policy environment.isDev. Ghi quyết định và kiểm thử rõ; không dùng DEBUG một mình khi yêu cầu prod luôn tắt log.

- AppConfig sở hữu quyết định môi trường/build type.
- AppModule.logger(config) chọn ConsoleLogger hoặc NoOpLogger từ policy.
- Composition tiếp tục nhận AppLogger; không đọc BuildConfig của library hoặc import app ngược vào composition.
- Domain/data/shared không phụ thuộc BuildConfig hay flavor Android.
- Không thêm logging framework/SDK mới nếu AppLogger hiện có đã đáp ứng.
- Chọn NoOpLogger tắt output của AppLogger, không chứng minh mọi SDK/Android OS đều im lặng trong Logcat.
- Crash/error reporting production nếu có nhu cầu là một hợp đồng riêng; không mặc định thêm Crashlytics hoặc giữ raw console errors ở prod.

## 5. Nội dung log và HTTP

- [ ] Bỏ email/tên khỏi string interpolation trong AuthRepositoryImpl; dùng event names và metadata cho phép như operation/error code, không log request DTO/session.
- [ ] Không log password, access/refresh token, Authorization/Cookie, auth request/response body hoặc dữ liệu người dùng.
- [ ] Không chỉ redact metadata keys: email hiện nằm ngay trong message string, logger chung không biết cấu trúc để tự che chính xác.
- [ ] Kiểm throwable.message trước khi đưa ra output: lỗi từ server/thư viện có thể mang dữ liệu ngoài dự kiến.
- [ ] Giữ HTTP logging **tắt mặc định**; hiện tại app chưa có HTTP log để bật/tắt theo variant.
- [ ] Nếu bổ sung HTTP diagnostics, dùng opt-in cùng policy; ưu tiên method/path không chứa query nhạy cảm, status và duration, không bật BODY đại trà.
- [ ] Cùng một policy phải áp dụng cho public/protected clients; không để protected client kế thừa interceptor logging đang bật ngoài ý muốn.
- [ ] Không dựa hoàn toàn vào R8 để quyết định có log; nếu dùng stripping sau này, kiểm chứng riêng và không xóa side effects ngoài logging.

Nguồn: [Android: log information disclosure](https://developer.android.com/privacy-and-security/risks/log-info-disclosure).

## 6. Các bước triển khai

### LOG-1 — Tắt logger theo policy

- [ ] Thêm policy rõ ràng vào AppConfig hoặc helper thuần có input environment/isDebug.
- [ ] AppModule cấp đúng ConsoleLogger/NoOpLogger; giữ chữ ký AppLogger cho các layer còn lại.
- [ ] Không thay URLs, tạo database hoặc thay logic nghiệp vụ.

### LOG-2 — Loại thông tin cá nhân khỏi log

- [ ] Sửa AuthRepositoryImpl log messages và rà call sites dùng AppLogger.
- [ ] Dùng metadata có kiểm soát, không đưa credentials vào fixtures/output.
- [ ] Quyết định HTTP diagnostics sau; không thêm interceptor chỉ để được gọi là base đầy đủ.

### LOG-3 — Kiểm chứng

- [ ] Unit test đủ 4 tổ hợp dev/prod × debug/release, nhất là prodDebug không log.
- [ ] Test sink/output: devDebug có event chẩn đoán; các variant bị tắt không phát output qua AppLogger.
- [ ] Test auth success/failure bằng fake/MockWebServer và giá trị giả: captured logs không chứa email/name/password/token trong message, metadata hoặc throwable output.
- [ ] Hilt graph tests chứng minh variant nhận logger theo policy; không chỉ test constructor default.
- [ ] Nếu thêm HTTP diagnostics, kiểm cả public/protected client và header/body/query không rò dữ liệu giả.
- [ ] Chạy tests/boundaries và các build phù hợp. Theo ghi nhận DI trước, wrapper 9.4.1 + AGP 8.3.1 có giới hạn release/lint; ghi đúng runner thay vì đổi toolchain ngầm.
- [ ] Nếu khẳng định log thực tế trên thiết bị đã tắt, kiểm Logcat của APK đúng variant; tách AppLogger khỏi log của hệ thống/SDK.

## 7. Điều kiện hoàn thành và giới hạn hiện tại

Hoàn thành khi policy được cài đặt, prod không phát AppLogger output, auth logs không chứa dữ liệu cá nhân, và các kiểm tra theo variant qua. Dùng một backend URL là cấu hình hợp lệ, không yêu cầu thêm backend dev để nghiệm thu logging.

Hiện mới có bằng chứng source; chưa sửa code, thêm test, build lại hoặc đọc Logcat. Findings logging chưa được đóng. Không dùng kết quả build/test của lượt tách composition để tuyên bố logging policy đã được kiểm chứng.
