# Let’s Play

Ứng dụng React Native để lên kế hoạch đi chơi và ghi sổ chi chung. Mở app là vào danh sách kèo, không có màn đăng nhập HRM hoặc module sản phẩm mẫu.

## Chạy Android

```powershell
yarn install
yarn start
# Terminal khác:
yarn android
```

Nếu có nhiều thiết bị, dùng `yarn android --device <serial>` (lấy serial bằng `adb devices`). Metro đang chạy đúng project ở cổng 8081 thì dùng tiếp phiên đó.

- React Native 0.79, React 19, Node >=18; dùng SDK/JDK đã cấu hình trên máy.
- Android application ID: `com.uzuu.letplay`; debug: `com.uzuu.letplay.debug`.
- Namespace Kotlin `com.vdx.alphrm` là tên nội bộ còn giữ từ base; Android nhận diện ứng dụng cài đặt bằng application ID riêng ở trên.
- Firebase hiện vẫn được liên kết trong native project. `android/app/google-services.json` phải có client khớp ID của bản build; debug đã dùng ID mới. Không sửa tên package trong cấu hình Firebase cũ để giả lập đăng ký app.
- OTA của dự án HRM cũ đã tắt và bỏ URL. Chỉ bật lại khi có dự án cập nhật riêng cho Let’s Play.

## Các luồng hiện có

- Tạo nhiều phòng với tên của bạn và danh sách bạn bè; phòng mới không tự chèn thành viên, ý tưởng, phiếu hay khoản chi mẫu.
- Tìm theo tên, mở lại bằng mã trên cùng thiết bị, lưu trữ/khôi phục và xóa phòng sau xác nhận.
- Góp ý, sửa/xóa ý của mình, bình chọn ba mức, chọn hoạt động, sắp thứ tự, nhập giờ bắt đầu và chốt lịch; có thể đánh dấu hoàn thành từng chặng.
- Ghi khoản chi theo người trả, chia đều đến từng đồng hoặc nhập phần riêng; xem số dư và gợi ý bù trừ.
- Ghi nhận thanh toán ở trạng thái chờ. Trong phòng cục bộ, người ghi sổ đối chiếu với người nhận rồi xác nhận; trong phòng mẫu vẫn minh họa quyền xác nhận của An.
- Chia sẻ nội dung kế hoạch qua bảng chia sẻ hệ thống.
- MMKV lưu riêng từng phòng, kế hoạch và sổ chi khi đóng/mở lại app. Phòng mẫu chỉ được thêm khi người dùng chọn trải nghiệm.

## Phạm vi hiện tại

Đây là bản dùng trên một thiết bị. Chưa có máy chủ, đăng nhập, lời mời liên thiết bị, quyền người dùng do server kiểm tra hoặc đồng bộ thời gian thực. Mã phòng không tạo một phòng online. Tên “ẩn” và vai trò trong phòng mẫu minh họa UI, không cung cấp bảo mật ẩn danh. App chỉ ghi sổ, không chuyển tiền thật.

Cập nhật phạm vi: đoạn trên mô tả **source mobile trong repo này**. Backend ngoài repo đã được người dùng cung cấp tại Render, với contract account/phòng/tiền/SSE; website ngoài repo ở Vercel. Xem [báo cáo đối chiếu Swagger](docs/integration/backend-gap-review.md) để biết những phần đúng hướng, lệch BA và chưa kiểm runtime. Chưa nối source mobile này vào các API đó.

Tài liệu nghiệp vụ dài hạn và mockup nằm trong `context/`; một số mô tả trong đó là yêu cầu tương lai, không phải tính năng đã hoàn tất.

Đặc tả mới cho chụp khoảnh khắc, chia sẻ cá nhân, album kèo và tương tác nằm tại [docs/ba/photo-memories](docs/ba/photo-memories/README.md). Bao gồm quyền/vòng đời, mô hình dữ liệu, API, sự kiện, nghiệm thu và các quyết định đã xác nhận/còn đề xuất. Đây là tài liệu bàn giao, chưa phải tính năng ảnh đã triển khai. Hướng dẫn cho agent bắt đầu tại [AGENTS.md](AGENTS.source.md).

Delta BA UI mới nhất về feed ảnh cuộn theo cursor và composer sau khi chụp nằm tại [09-ui-feed-and-capture-composer.md](docs/ba/photo-memories/09-ui-feed-and-capture-composer.md). Phần này phân biệt rõ layout có thể mock độc lập với pagination, ACL, upload và publish cần backend.

Prototype riêng **giao diện điện thoại** nằm tại [prototypes/keo-full](../../../let_play/prototypes/keo-full/README.md), mở `dist/index.html` để thử. Gói giao backend/AI được mô tả tại [hướng dẫn bàn giao](docs/integration/backend-ai-start.md), tạo lại bằng `node scripts/build-backend-handoff.cjs`. Prototype dùng dữ liệu giả local, không thay app mobile hoặc website đang deploy.

## Cấu trúc

```text
App.tsx                   # Safe area, gesture root, store và navigation
src/design/               # Theme và bộ icon nét tròn tự thiết kế
src/modules/keo/          # Screens, components, hooks, store, types và nghiệp vụ
src/navigators/           # Hai route: danh sách và phòng chi tiết
assets/brand/             # Icon ứng dụng SVG + PNG
```

Đã xóa các module `home`, `product`, `user` và mã HRM phụ thuộc không còn dùng. Các dependency native của base chưa được cắt giảm toàn bộ; việc gỡ SDK cần kiểm tra native Android/iOS riêng.

## Kiểm tra

Chạy ba lệnh đầu từ thư mục gốc để kiểm tra TypeScript, unit test và lint. Bước build Android chạy riêng trong thư mục `android` và cần SDK/JDK đã cấu hình.

```powershell
yarn typecheck
yarn test --runInBand
yarn lint
cd android
.\gradlew.bat :app:assembleDebug
```

Kiểm thử bao gồm chia tiền và phiếu bầu, lưu/khôi phục nhiều phòng, cô lập sổ chi, xác thực dữ liệu và khởi động app.

## Thiết kế và icon

Icon lấy cảm hứng từ nét mở, đầu tròn của tham chiếu Pulsar Line; được vẽ mới bằng `react-native-svg`, không phụ thuộc font icon. Màu xanh trầm, kem, đào và tím nhạt, góc bo mềm, vùng chạm tối thiểu 48 dp.

`assets/brand/app-icon.svg` là nguồn vector. Tạo lại PNG cho Android/iOS trên Windows:

```powershell
.\scripts\generate-app-icons.ps1
```

iOS đã cập nhật tên hiển thị, bundle ID trong các xcconfig và icon; cần macOS/Xcode, cấu hình Firebase iOS riêng và kiểm tra native trước phát hành.
