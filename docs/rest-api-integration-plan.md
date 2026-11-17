# Kế hoạch tích hợp REST — Practice API của chủ dự án

> **Nguồn hiện hành 16/11/2026:** [Đặc tả và hiện trạng thống nhất](LetsPlay-Spec/UNIFIED-SPEC.md). Trang này giữ lịch sử/baseline; các trạng thái, yêu cầu hoặc số test cũ mâu thuẫn dùng bản thống nhất. Báo cáo backend mới được phân biệt với source Android và mô phỏng prototype.


Ngày cập nhật: **16/11/2026**. Trạng thái hiện hành: **Đã có source Products/Auth mẫu Practice API; còn giới hạn production** — xem [bản thống nhất](LetsPlay-Spec/UNIFIED-SPEC.md). Các checklist và số test bên dưới giữ theo lịch sử, không phải kết quả chạy lại trong lượt hợp nhất.

- [Swagger UI](https://templetezuu.netlify.app/docs/)
- [OpenAPI JSON](https://templetezuu.netlify.app/openapi.json)
- [Kế hoạch tổng thể](base-evolution-plan.md), phần P6/P7 và P9/P9.1.
- [Các việc ổn định core trước đó](base-v1-review-plan.md).

## 1. Mục tiêu và giới hạn xác nhận

Dùng API có sẵn của người dùng làm ví dụ REST thực tế cho products, server-side search/filter/pagination, đăng nhập và token refresh. Giữ JSONPlaceholder là demo riêng cho tới khi migration có kiểm chứng.

Đã đọc Swagger HTML và OpenAPI 3.0.3, Practice API version 1.0.0, gồm 18 operations. Trình duyệt nghiên cứu không mở được website; đã đọc tài liệu bằng HTTP GET qua công cụ IDE. Không gọi login/register/refresh hoặc mutation; không xác nhận server runtime tuân thủ mọi mô tả trong schema.

Không tự tạo tài khoản, đổi mật khẩu, tạo/xóa dữ liệu hay đăng xuất các phiên hiện có. Trong lượt này chỉ cập nhật tài liệu; không thay endpoint mặc định hoặc cấu hình production.

## 2. Base URL và môi trường

OpenAPI khai báo server cùng origin là `/`; các business path bắt đầu bằng `/api/v1/`.

**Đề xuất Retrofit business base URL:** `https://templetezuu.netlify.app/api/v1/`.
Annotations tương đối là `products`, `auth/login`, `users/{id}`... Không dùng `/docs/` làm API base, không lặp prefix thành `/api/v1/api/v1/`.

- `/health` nằm ở root origin; dùng route root rõ ràng hoặc service origin riêng nếu bổ sung health check.
- Docs còn liệt kê `http://localhost:18080` và `http://10.0.2.2:18080`. Chúng chỉ dùng khi backend local thực sự chạy; localhost trên Android trỏ vào thiết bị, không phải máy phát triển.
- HTTP local nếu cần phải giới hạn vào debug/dev theo cấu hình network phù hợp.
- Domain public này là Practice API tham chiếu, chưa được người dùng xác nhận là deployment production của một sản phẩm.
- Khi triển khai, cấu hình môi trường qua AppConfig/Gradle properties, không hard-code trong repository/use case.
- Không tự dùng cùng deployment cho hai môi trường rồi tuyên bố backend dev/prod đã tách biệt.
- Rà release guard để nhận diện nguồn sample được khai báo; hiện guard demo của base tập trung vào JSONPlaceholder, không chứng minh một host khác là production thật.
- Xác thực URL/host/scheme theo R2 của review v1 trước khi migration client.

## 3. Endpoint đã có trong tài liệu

| Nhóm | Method/path | Quyền và hành vi |
| --- | --- | --- |
| System | GET /health | Public; 200 hoặc lỗi 503; không có prefix api/v1 |
| Auth | POST /api/v1/auth/register | Public; tạo user thường và đăng nhập; trả 201 AuthResponse |
| Auth | POST /api/v1/auth/login | Public; trả AuthResponse gồm user + tokens |
| Auth | POST /api/v1/auth/refresh | Public theo nghĩa không cần access token; bắt buộc refresh token mới nhất |
| Auth | GET /api/v1/auth/me | Bearer; lấy user hiện tại |
| Auth | POST /api/v1/auth/logout | Bearer còn hạn; trả 204, thu hồi phiên hiện tại |
| Auth | POST /api/v1/auth/logout-all | Bearer; trả 204, thu hồi các phiên của user |
| Auth | POST /api/v1/auth/change-password | Bearer; trả 204; sau đó phải đăng nhập lại |
| Categories | GET /api/v1/categories | Public; ba category cố định theo tài liệu |
| Products | GET /api/v1/products | Public; search/filter/sort/page |
| Products | GET /api/v1/products/{id} | Public; có 404 |
| Products | POST /api/v1/products | Admin; trả 201 |
| Products | PATCH /api/v1/products/{id} | Admin; trả product đã cập nhật |
| Products | DELETE /api/v1/products/{id} | Admin; trả 204 |
| Users | GET /api/v1/users | Admin; danh sách phân trang/tìm kiếm/lọc role |
| Users | GET /api/v1/users/{id} | Chính user hoặc admin |
| Users | PATCH /api/v1/users/{id} | Chính user hoặc admin; không sửa email/role qua API này |
| Users | DELETE /api/v1/users/{id} | Admin; xóa user khác và các phiên; có thể trả 409 |

Không thay public Users sample hiện tại bằng endpoint admin này chỉ bằng cách đổi base URL. Nên thêm Products làm ví dụ public đầu tiên; Users quản trị là feature có xác thực/phân quyền riêng.

## 4. Contract DTO, model và response mapping

### 4.1. Response envelopes

- AuthResponse: `data.user`, `data.tokens`, `meta.requestId`.
- RefreshResponse: `data.tokens`, `meta.requestId`.
- Product/User detail: `data.product` hoặc `data.user`, kèm meta.
- Danh sách: `data.items`, `meta.pagination`, `links.next`, `links.previous`.
- Error: `error.code`, `error.message`, `error.details` và `meta.requestId`.
- DELETE/logout/change-password có response thành công 204 **không có body**.

- [ ] Tạo DTO/envelope riêng cho Practice API; map ở data boundary, không đưa Retrofit Response/DTO vào UI.
- [ ] Thêm xử lý no-content success riêng. Không gọi requireBody() trên 204; giữ hành vi báo lỗi cho 200 thiếu body ở những contract yêu cầu body.
- [ ] Giữ requestId để chẩn đoán lỗi; log metadata đã lọc, không log password/token.
- [ ] Chỉ theo links pagination cùng origin/path được phép nếu dùng chúng; có thể dùng page metadata thay cho URL do server trả.

### 4.2. Model cần chú ý

- User ID của Practice API là **String**, khác model User(id: Int) từ JSONPlaceholder. Đề xuất Account/PracticeUser model riêng hoặc migration tường minh.
- Role là user/admin; UI có thể ẩn action không được phép, nhưng quyền vẫn do backend quyết định. Role mới/không nhận biết không mặc định được cấp quyền admin.
- Product có nested price/category/inventory/media/variants/rating, attributes và tags.
- Giá là số nguyên VND dạng int64, tối đa 1.000.000.000.000: dùng **Long**, không dùng Int hoặc Double cho số tiền gốc.
- Thời gian là UTC/date-time; parse có kiểm soát, format theo locale/timezone ở UI. Nếu dùng java.time, kiểm tra tương thích API 24/desugaring.
- Null hợp lệ và field bị thiếu phải được phân biệt theo schema.

- [ ] Use case có Input/Output riêng: GetProductsInput/Output, GetProductInput/Output, LoginInput/Output, RefreshSessionInput/Output, UpdateProfileInput/Output...
- [ ] Repository contract bám nghiệp vụ; không ép toàn bộ backend vào một universal HTTP client.
- [ ] Không bắt entity/DTO kế thừa BaseOutput.

## 5. Search, filter và pagination thực tế

GET products hỗ trợ:

- page từ 1, tối đa 1.000.000; pageSize 1–100, mặc định 10.
- q tối đa 200 ký tự, tìm literal substring trong name/description.
- categoryId, minPrice/maxPrice (VND), inStock.
- sort: name, -name, price, -price, createdAt, -createdAt; mặc định -createdAt.

GET users hỗ trợ page/pageSize/q và role user/admin, nhưng cần quyền admin.

**Quy tắc tích hợp:**

- [ ] Dùng `meta.pagination.hasNextPage`, totalItems/totalPages/page/pageSize. Không suy đoán hasNext bằng items.size == pageSize khi API đã có metadata.
- [ ] Empty response là items:[], không phải null; test empty và trang cuối.
- [ ] Search gửi q lên server; không chỉ lọc một trang dữ liệu đã tải ở client.
- [ ] Debounce + latest-request policy; sửa R1 trước khi tái sử dụng sample UserViewModel.
- [ ] Dataset key gồm query, filters và sort; thay key phải reset pagination/generation và loại kết quả cũ.
- [ ] Ràng buộc minPrice <= maxPrice, pageSize và query; dùng Long cho giá.
- [ ] Bảo toàn dấu và ký tự tiếng Việt. Docs nêu SQLite case folding chỉ ASCII; không hứa tìm kiếm không dấu hoặc không phân biệt hoa/thường Unicode nếu backend chưa hỗ trợ.
- [ ] Encode q đúng bằng Retrofit query parameters; % và _ là ký tự literal theo contract.
- [ ] Kết nối vào list/grid P9; skeleton ban đầu chỉ khi chưa có item, refresh/append giữ dữ liệu cũ.

## 6. Lỗi và form validation

Parser hiện tại của base đọc code/message/errors từ root. Với nested `error` object của API này, cần adapter/parser tương ứng, không chỉ đổi endpoint.

- [ ] Parse error.code/message/details và requestId; details là map field → string, chuyển thành field error model của UI khi cần.
- [ ] Giữ tên field/path để hiển thị đúng ô nhập; hỗ trợ nhiều lỗi field trong một response.
- [ ] 401 của login là lỗi đăng nhập, không khởi động refresh interceptor.
- [ ] 401 ở protected request chỉ đi vào coordinator khi đủ điều kiện; xử lý theo error code thực tế được xác minh.
- [ ] 403 là thiếu quyền, không tự refresh/expire session như mọi lỗi 401.
- [ ] 404 là không tìm thấy; 409 là conflict; 422 là validation; 415 cần kiểm tra Content-Type/payload.
- [ ] 429 cần cooldown/backoff có giới hạn. Retry-After chỉ dùng nếu response thực tế có header; OpenAPI đã đọc chưa quy định header đó.
- [ ] 5xx/network retry chỉ cho thao tác có thể thực thi lại an toàn; không retry mutation theo mặc định.
- [ ] UI map mã lỗi sang en/vi; server message chỉ là fallback có policy, không coi là bản dịch sẵn.
- [ ] Không tự bịa danh sách code như TOKEN_EXPIRED/REFRESH_REUSED nếu chưa được spec/response xác nhận.

## 7. Auth và refresh-token rotation — ưu tiên đúng semantics

TokenPair có accessToken, refreshToken, tokenType, expiresIn và refreshExpiresIn. Docs nêu access mặc định 600 giây và absolute refresh session mặc định 604800 giây; client dùng giá trị server thực trả, không hard-code thời gian để quyết định quyền.

**Điểm bắt buộc:** refresh token là opaque token 43 ký tự, chỉ dùng một lần. Refresh thành công thay cả token pair; dùng lại token cũ sẽ thu hồi cả session và access token. Refresh không kéo dài absolute session lifetime.

- [ ] Tách public/auth client và protected client. Refresh không cần access token và không được tự đi qua vòng refresh đệ quy.
- [ ] Single-flight: các request cùng hết hạn chờ chung một refresh. Sau khi lấy lock, kiểm tra token/session đã đổi chưa trước khi tạo refresh mới.
- [ ] Lưu access + refresh token mới **nguyên tử**; xử lý lỗi lưu để không tiếp tục dùng token đã tiêu thụ.
- [ ] Không retry refresh bằng refresh token cũ sau timeout/mất response; trạng thái server có thể đã đổi. Khi chưa có recovery protocol, dùng policy yêu cầu đăng nhập lại thay vì replay mù.
- [ ] Mỗi protected request chỉ có số lần replay hữu hạn; mutation cần kiểm tra tính an toàn/idempotency, không blanket-retry POST/PATCH.
- [ ] Session generation ngăn refresh hoàn tất sau logout/account switch khôi phục credentials cũ.
- [ ] Một caller hủy request không vô tình hủy shared refresh của các caller còn chờ.
- [ ] Logout đòi access token còn hạn theo docs. Thiết kế remote revoke và local clear có chủ đích; mô tả kết quả khi offline/revoke thất bại, không để refresh phục hồi phiên sau ý định logout.
- [ ] Change-password/logout-all thành công xóa session phù hợp và chuyển về Login.
- [ ] Chọn cơ chế bảo vệ credentials trên Android; Preferences DataStore hiện tại không được coi là storage đã mã hóa.
- [ ] Không đưa password/token hoặc tài khoản demo vào source app, BuildConfig, log hoặc fixture công khai; tài khoản thử đọc từ hướng dẫn/config local khi có yêu cầu test.

## 8. PATCH, dữ liệu nhạy cảm và CRUD quản trị

- Omitted field giữ nguyên; nullable field có thể được xóa bằng null theo schema.
- Product PATCH: description:null xóa mô tả; arrays thay toàn bộ array, attributes thay map, media thay object. Một số field như ID/rating/inStock/timestamps là read-only.
- User PATCH merge profile đệ quy; avatarUrl/phone/address có thể được clear bằng null; email/role không được sửa.
- Request schemas không chấp nhận additionalProperties; PATCH cần ít nhất một field.
- Register/new password: ít nhất 8 Unicode characters và tối đa 72 UTF-8 bytes theo docs, không nhầm byte limit với số ký tự Kotlin String.length.

- [ ] Thiết kế tri-state Unchanged/Set/Clear hoặc serializer có semantics tương đương; không dùng một nullable data class làm mất khác biệt omitted/null.
- [ ] Test JSON thực sự gửi lên mạng, không chỉ test object DTO.
- [ ] Sau mutation, repository cập nhật/invalidate nguồn dữ liệu theo policy; không biến mutation đã commit thành failure chỉ vì refresh list lỗi.
- [ ] Chỉ chạy live CRUD với dữ liệu/tài khoản test được chỉ định; không dùng demo admin để xóa dữ liệu chia sẻ khi review.

## 9. Lộ trình triển khai

1. **REST-0 — Ổn định:** (Đã hoàn thành) Sửa các lỗi review v1 liên quan search/config/lifecycle (R1–R5), xác thực URL và guard production.
2. **REST-1 — Public Products:** (Đã hoàn thành ngày 16/11/2026) DTO/envelope + repository + use cases + categories/detail; 6 MockWebServer tests kiểm chứng paging metadata, query params, nested 422 errors, Long VND price và Hilt DI bindings.
3. **REST-2 — UI collection:** (Đã hoàn thành ngày 16/11/2026) Products list/grid Compose (`ProductListScreen`, `ProductItemCard`), server search/filter/sort, phân trang append kèm footer retry, empty state, và P9.1 Skeleton shimmer (`ShimmerLoading.kt`) hỗ trợ motion reduction.
4. **REST-3 — Auth:** (Đã hoàn thành ngày 16/11/2026) Login/register/me, atomic `TokenStorage`, và `AuthInterceptor` single-flight mutex refresh-token rotation chống race condition khi nhiều request 401 đồng thời.
5. **REST-4 — Protected features:** (Đã hoàn thành ngày 16/11/2026) `LoginScreen`, `ProfileScreen` hiển thị thông tin user, đổi mật khẩu, đăng xuất an toàn xử lý 204 No-Content, và tích hợp route `"products"`, `"login"`, `"profile"` vào `AppNavHost`.
6. **REST-5 — Kiểm chứng:** (Đã hoàn thành ngày 16/11/2026) Nâng cấp thành công Gradle 9.4.1 và Kotlin 2.0.21, toàn bộ 243 bài unit test độc lập (476 lượt test trên các build variants) PASS 100%, 0 failures, 0 errors, và `verifyModuleBoundaries` PASS.

Không tích hợp một lượt toàn bộ 18 operations nếu chưa cần. Public Products và Auth core là các bước đầu dùng được đầy đủ.

## 10. Test và điều kiện hoàn thành

MockWebServer/fakes cho PR/CI:

- [x] Parse envelopes/nested models, String IDs, Long VND và null fields.
- [x] Query/filter/sort đúng, hasNextPage từ metadata, empty và append error giữ items.
- [x] 204 success không bị map thành ParseError.
- [x] Nested 422 details hiển thị lỗi đúng field; 401 login không refresh; 403 không refresh.
- [x] N request expired chỉ tạo một refresh; lưu đúng pair; request đã dùng token mới không refresh thừa.
- [x] Refresh reuse/timeout, logout-race, cancellation và session expiration có test.
- [x] PATCH omitted/null/replacement semantics đúng trên wire.
- [x] Không có password/token trong logs.
- [x] API delay/fault để kiểm tra skeleton/retry do MockWebServer/fake tạo; chưa thấy tham số delay/fault injection được công bố trong OpenAPI này.

Smoke test live là bước riêng, với cấu hình/tài khoản dev phù hợp. Không tự suy ra đây là server production cần dùng. Lượt viết plan chỉ đọc docs/OpenAPI, chưa có bằng chứng live business flow, chưa đổi số test hoặc trạng thái Verified của base.

**Hoàn thành khi:** public sample chạy đúng contract, auth có kiểm thử rotation thật theo môi trường được chọn, error mapping/i18n hoạt động và P9/P9.1 được kiểm chứng riêng. Nếu chỉ hoàn thành Public Products, ghi rõ auth/admin còn trong backlog.
