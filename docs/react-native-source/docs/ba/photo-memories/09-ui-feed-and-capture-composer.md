# Delta BA UI: feed ảnh và composer sau khi chụp

> Cập nhật 11/11/2026. Nguồn: mô tả và recording màn hình do chủ sản phẩm cung cấp, đối chiếu với source mobile hiện tại. Đây là đặc tả hành vi UI; không phải bằng chứng tính năng đã triển khai hoặc backend đã có route.

**Đính chính theo screenshot bổ sung ngày 11/11/2026 (C-16–17):** composer phải có tiêu đề “Gửi đến...”, ô “Thêm một tin nhắn” đè trên ảnh, hàng nút X / máy bay gửi / Aa và hàng avatar người nhận. Không dùng trang biểu mẫu “Chú thích / Nơi lưu khoảnh khắc” dưới ảnh. Feed dùng phân trang màn hình: một ảnh trong mỗi viewport, một lần vuốt chuyển một ảnh rồi snap đúng trang, theo [Scroll-Infinity.png](../../../context/locket-images/Scroll-Infinity.png). Không hiểu “cuộn liên tục” là danh sách thẻ cho lộ hai ảnh cùng lúc.

**Trạng thái mobile sau chỉnh UI:** capture/import mở composer ngay tại camera với bố cục trên; X không lưu, chụp lại giữ tin nhắn và lựa chọn; từ phòng chọn sẵn kèo, cá nhân tắt. Nút tải xuống góc trên chỉ lưu thư viện riêng trên thiết bị. Feed local mới trước cũ sau dùng viewport paging; lịch/lưới vẫn riêng. Nguồn code: [CameraHomeScreen](../../../src/modules/keo/screens/CameraHomeScreen.tsx), [PhotoComposer](../../../src/modules/keo/components/PhotoComposer.tsx), [PhotoFeed](../../../src/modules/keo/components/PhotoFeed.tsx).

Đây là UI dùng adapter ảnh local hiện có. Composer có lựa chọn OWNER / tất cả / từng bạn theo stable ID khi nhận được danh sách từ adapter; runtime hiện chưa có danh sách bạn bè hoặc publish adapter, nên hiển thị thiếu kết nối và khóa nút gửi. Không dùng thành viên phòng làm danh sách bạn bè, không thêm bạn giả. Chưa có feed người khác theo placement/cursor, upload/processing/publish nguyên tử hoặc đồng bộ album. Không coi local save là `PUBLISHED` hay AC online đã pass.

## 1 Kết luận ngắn

Lệch hiện tại không chỉ là bố cục:

- **Màn xem** cần là một feed/album có thể cuộn liên tục để xem ảnh của chính mình và ảnh người khác mà người dùng có quyền xem.
- **Sau khi chụp** cần mở một composer ngay trong cùng ý định đăng: xem preview, nhập tiêu đề/chú thích và chọn thao tác trước khi tải/đăng.
- Trước lần chỉnh UI này, app lưu ảnh local, chuyển sang màn preview riêng rồi mới nhập chú thích. Composer hiện mở ngay tại camera; publish online và cursor pagination vẫn chưa triển khai. Đây là trạng thái source, không phải kết quả nghiệm thu đầy đủ mục tiêu.

“Cuộn vô cực” ở đây là **tải từng trang bằng cursor đến khi `nextCursor = null`**, không phải gọi API vô hạn hoặc tải toàn bộ album một lần.

## 2 Phạm vi trải nghiệm đã xác nhận

### C-UI-01 — Feed xem ảnh

Khi người dùng mở khu vực xem ảnh từ cá nhân hoặc phòng, app hiển thị danh sách ảnh theo ngữ cảnh:

- Từ **album phòng**: ảnh của các thành viên và ảnh của chính người dùng trong album đó, theo quyền ROOM hiện hành.
- Từ **dòng cá nhân/feed bạn bè**: chỉ ảnh PERSONAL mà server cấp quyền; không trộn comment, reaction, tên phòng hoặc audience của placement khác.
- Ảnh mới hơn nằm trước ảnh cũ hơn. Mỗi item giữ đúng `placementId`; cùng một Moment đăng ở hai nơi có thể xuất hiện ở hai feed khác nhau.
- Khi gần cuối danh sách, app tải trang tiếp theo và nối item đã nhận, không lặp ID. Khi không còn trang, dừng tải và hiển thị trạng thái đã xem hết.
- Mỗi item chiếm đúng vùng xem sau header và safe area; thanh điều hướng không che tương tác. Vuốt nhanh không bỏ qua nhiều item trong một lần; đổi kích thước giữ item đang xem. Nhãn hết dữ liệu nằm trong trang cuối, không tạo thêm trang trống.

### C-UI-02 — Composer ngay sau chụp

Sau khi chụp hoặc chọn ảnh từ thư viện máy, app mở composer với ảnh preview và các thao tác liên quan trong cùng luồng:

1. **Hủy**: bỏ nháp và quay lại camera/màn trước; không upload, không tạo Moment.
2. **Chụp lại/chọn lại**: thay ảnh nháp; không tạo post từ ảnh cũ.
3. **Thêm một tin nhắn**: ô nhập tùy chọn nằm trên ảnh, giữ trong nháp đến khi xác nhận; nút Aa đưa focus vào ô này. Đây là caption đính kèm ảnh, không tạo `Message` DM hoặc comment album. Chuẩn 280 code points giữ nguyên.
4. **Chọn nơi chia sẻ**: từ phòng thì album phòng được chọn sẵn; cá nhân là lựa chọn riêng. Không tự chia sẻ sang mọi phòng hoặc mọi bạn bè.
5. **Chọn người nhận cá nhân**: `OWNER`, tất cả bạn bè hiện tại hoặc danh sách bạn được chọn theo audience snapshot; chỉ hiển thị người mà server cho phép.
6. **Tải ảnh lên/Đăng**: bắt đầu upload và publish theo một ý định có trạng thái tiến trình. Nút thành công chỉ hiển thị sau khi server commit toàn bộ đích đã chọn.

Tên nút có thể là “Tải ảnh lên” hoặc “Đăng khoảnh khắc” sau khi product chốt copy; hành vi không thay đổi.

Ảnh bổ sung chốt nút gửi bằng icon máy bay, nút hủy X ở cùng hàng dưới preview; tên truy cập phải mô tả hành động. Nút tải xuống góc trên là hành động riêng, không gửi ảnh và không thay thế nút gửi. Hàng avatar hỗ trợ tất cả hoặc nhiều bạn cụ thể; khi chọn tất cả, bỏ lựa chọn riêng và ngược lại. Kèo là đích độc lập, tối đa một kèo. Tập người nhận phải tới từ server; UI chọn người nhận không tự cấp ACL.

Đây là làm rõ bố cục và cách lướt; không thêm trường API, không thay event hoặc cơ chế publish/cursor ở 04–06. Tin nhắn đính kèm vẫn map `caption`, còn DM vẫn map `Message` riêng.

## 3 Trạng thái UI bắt buộc

### Feed

| State | Hành vi |
| --- | --- |
| `INITIAL_LOADING` | Skeleton/indicator; không hiển thị “hết ảnh” khi chưa có response |
| `READY` | Hiển thị item, pull-to-refresh hoặc refresh khi vào lại |
| `LOADING_NEXT` | Giữ item cũ, spinner cuối danh sách; serialize request cùng cursor |
| `EMPTY` | Hướng dẫn chụp/góp ảnh phù hợp với context và policy |
| `END_REACHED` | Không gọi tiếp; hiển thị đã xem hết nếu cần |
| `NEXT_PAGE_ERROR` | Giữ các item đã xem, cho “Thử lại” đúng cursor; không reset về trang đầu |
| `ACCESS_CHANGED` | Xóa/đánh dấu item mất quyền, refetch count/cover; không render media từ cache như còn quyền |
| `OFFLINE` | Hiển thị dữ liệu cache có lease hợp lệ; không giả định cache là quyền và không tự publish |

`onEndReached` chỉ là tín hiệu UI. Client phải dedup theo `placementId`, giữ filter/context cùng cursor và bỏ cursor khi refresh hoặc filter đổi.

### Composer

| State | Hành vi |
| --- | --- |
| `DRAFT` | Chưa upload/publish; cho sửa tiêu đề, đích và hủy |
| `UPLOADING` | Hiển thị byte progress; khóa thao tác gây tạo upload trùng, vẫn cho hủy nếu API hỗ trợ |
| `PROCESSING` | Chờ MediaAsset `READY`; không báo đã đăng |
| `PUBLISHING` | Kiểm tra lại audience/membership/policy tại commit; không đăng từng phần |
| `PUBLISHED` | Hiển thị receipt, xóa nháp local và đưa item vào feed khi query đó có quyền |
| `FAILED_RETRYABLE` | Nêu lỗi, giữ ảnh/tiêu đề/đích; retry cùng mutation khi kết quả chưa rõ |
| `FAILED_RESELECT` | Đích không còn hợp lệ hoặc quyền đổi; yêu cầu người dùng chọn lại, không tự chuyển sang đích khác |
| `CANCELLED` | Không có Moment/placement mới; asset chưa gắn được hủy hoặc hết hạn theo server |

## 4 Luồng chuẩn và ngoại lệ

### UI-UC-01: Xem feed/album

**Given** người dùng có phiên hợp lệ và quyền đọc context, **when** mở feed:

1. Client gọi list đầu tiên với `limit` và không có cursor.
2. Client render item theo thứ tự server trả về.
3. Khi gần cuối, gọi `nextCursor` một lần; request đang chạy không được gọi lại do nhiều event scroll.
4. Nếu quyền thay đổi giữa hai trang, server lọc lại; client không tự lấp bằng ảnh chỉ còn trong local cache.
5. Refresh tạo watermark/cursor mới, giữ filter hiện tại và loại các item trùng.

Nếu album đang `READ_ONLY`, người dùng vẫn xem ảnh/comment/reaction hiện có theo quyền; CTA góp ảnh bị khóa và giải thích lý do. Nếu mất membership, màn chuyển sang trạng thái không còn quyền, không chỉ ẩn nút.

### UI-UC-02: Chụp → composer → đăng

**Given** người dùng mở camera từ phòng hoặc cá nhân:

1. Camera giữ `sourceRoomId` nếu được mở từ phòng; không dùng tên phòng, index thành viên hoặc role local làm quyền.
2. Sau capture, app chuyển sang composer ngay. Ảnh chỉ là nháp local.
3. Composer hiển thị title/caption, preview nơi đăng và audience. Với phòng, album đã chọn sẵn nhưng người dùng vẫn phải thấy và có thể bỏ chọn nếu policy cho phép.
4. Chọn “Đăng” tạo upload session, upload file, complete/process và publish. UI hiển thị từng trạng thái.
5. Publish thành công khi **tất cả** đích đã chọn commit trong một transaction. Lỗi một đích giữ nháp/media READY; không âm thầm đăng phần còn lại.
6. Timeout sau publish tra `clientMutationId` hoặc retry cùng `Idempotency-Key`; không tạo post mới.

## 5 Quan hệ với backend

### Chỉ là UI, không cần đổi backend

- Thứ tự component, khoảng cách, animation, skeleton, tên nút và cách đặt composer trên màn hình.
- Việc dùng `FlatList`/`FlashList`, prefetch trước cuối danh sách và cách giữ scroll position.
- Hiển thị thumbnail đã có quyền và layout lưới/list.

### Là contract backend hoặc cần adapter

- Feed/album phải trả `items`, `nextCursor`, `hasMore` hoặc quy ước tương đương; cursor phải gắn user, filter, context và không dùng offset.
- Mỗi item phải có `placementId`, `momentId`, owner reference được phép hiển thị, caption, timestamp, media variant và capabilities theo viewer.
- List/detail/media/count/cover đều phải re-authorize; không trả toàn bộ ảnh rồi chỉ ẩn ở UI.
- Publish phải có upload lifecycle, `Idempotency-Key`, audience snapshot, transaction nguyên tử và lỗi cụ thể cho membership/policy/audience đổi.
- Event/reconnect phải invalidate feed/album đúng context. Event không được chứa media URL riêng tư, caption/DM hoặc audience toàn cục.

Vì vậy câu trả lời là: **UI không độc lập hoàn toàn với backend**. Layout có thể làm độc lập bằng mock; nhưng feed cuộn được ảnh của người khác, chọn người chia sẻ và báo “đã đăng” đúng lúc phụ thuộc trực tiếp vào ACL, pagination, upload và publish contract.

## 6 Mapping với tài liệu chuẩn

- FR-03, FR-05, FR-06, FR-07, FR-09 và UC-01–03 trong [02-business-requirements.md](02-business-requirements.md).
- BR-05, BR-06, BR-10, BR-18, BR-26, BR-31 và BR-32 trong [03-permissions-and-lifecycle.md](03-permissions-and-lifecycle.md).
- Pagination tại mục 3, upload/publish tại mục 4–7 của [05-api-contract.md](05-api-contract.md).
- Event invalidate, offline và media lease tại [06-events-notifications-nfr.md](06-events-notifications-nfr.md).
- AC bổ sung cho feed/composer tại [07-acceptance-and-delivery.md](07-acceptance-and-delivery.md).

Phạm vi UI mobile đã thực hiện và phần backend còn thiếu được ghi riêng ở đầu file; không suy ra toàn bộ BA đã triển khai từ bố cục mới.

## 7 Kiểm chứng phần UI ngày 12/11/2026

- `yarn typecheck`: pass; `yarn test --runInBand`: 51 test pass; `yarn lint`: không lỗi, còn cảnh báo của repo.
- `android/gradlew.bat :app:assembleDebug -PreactNativeArchitectures=x86 --console=plain`: build thành công cho Pixel_4, Android 10/API 29.
- Đã mở composer qua thư viện ảnh trên máy ảo, kiểm ô tin nhắn nằm trên ảnh, X / gửi / Aa, lựa chọn đích và lưu riêng; trực tiếp vuốt feed hai ảnh từ `1/2` sang `2/2` và ngược lại. Thanh tương tác nằm trên thanh điều hướng. Kiểm trạng thái lỗi đọc ảnh và đường thử lại; không báo gửi thành công khi thiếu adapter.
- Đã tái hiện giới hạn Fresco bitmap pool 8 MiB trên máy ảo khi chọn lại ảnh. Preview/feed tự thử giải mã nhỏ hơn tối đa hai lần khi đúng lỗi pool; ảnh gốc không đổi. Đã nhìn lại preview hiển thị được sau fallback. Lỗi file khác không retry vô hạn và không cho lưu/gửi preview không đọc được.
- Chưa xác nhận camera giả lập ổn định, iOS, gửi giữa hai user, ACL/cursor/upload/publish thật. Test chọn người nhận dùng adapter được inject trong test, không phải API online. Các UI-AC liên quan server không được đánh dấu pass từ kết quả này.
