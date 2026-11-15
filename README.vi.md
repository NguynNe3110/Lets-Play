# Let’s Play

Ứng dụng Android Kotlin để lên kế hoạch đi chơi, chia chi phí và lưu khoảnh khắc. Đây là project đích chuyển từ React Native, được fork từ [Kotlin-multi-module](https://github.com/NguynNe3110/Kotlin-multi-module).

[English](README.md) · [Kế hoạch chuyển đổi](docs/migration/README.md) · [Tài liệu sản phẩm](docs/README.md) · [Đóng góp](CONTRIBUTING.md)

## Hiện trạng

Project đã đổi nhận diện sang LetsPlay. App đang chạy vẫn chứa các ví dụ Todo/Users, khung session và client JSONPlaceholder kế thừa. Nghiệp vụ LetsPlay và tích hợp backend nằm trong [kế hoạch chuyển đổi](docs/migration/README.md), chưa được triển khai.

## Cấu hình project

- Tên Gradle project: `LetsPlay`; tên app: **Let’s Play** ở cả tiếng Anh và tiếng Việt.
- Namespace app và tiền tố package Kotlin: `com.uzuu.letplay`.
- Namespace resources: `com.uzuu.letplay.resources`.
- Source và test dùng thư mục `com/uzuu/letplay`; giữ hạ tầng chung như `BaseViewModel` để tái sử dụng.

| Variant | Application ID |
| --- | --- |
| `devDebug` | `com.uzuu.letplay.dev.debug` |
| `devRelease` | `com.uzuu.letplay.dev` |
| `prodDebug` | `com.uzuu.letplay.debug` |
| `prodRelease` | `com.uzuu.letplay` |

Dùng `devDebug` khi phát triển: ID này tách biệt với ID release/debug của app React Native đã ghi trong tài liệu. ID production theo source app; cần xử lý signing, version và chuyển dữ liệu trước khi phát hành bản cập nhật đè app React Native.

## Bắt đầu

Dùng **JDK 21** chạy Gradle; bytecode Java/Kotlin target 17. Cài Android SDK **34**, Build Tools **34.0.0**; thiết bị cần API **24** trở lên. Project dùng Gradle **8.11.1**, AGP **8.3.1**, Kotlin **1.9.23**. Xem [version catalog](gradle/libs.versions.toml).

```bash
git clone https://github.com/NguynNe3110/Lets-Play.git
cd Lets-Play
```

Mở thư mục gốc trong Android Studio, chọn Gradle JDK 21 rồi sync. Đường dẫn SDK/JDK theo máy chỉ lưu trong cấu hình local. Chọn cấu hình chạy `app` và build variant `devDebug`.

```powershell
.\gradlew.bat verifyModuleBoundaries :app:assembleDevDebug
```

Trên macOS/Linux dùng `./gradlew`. APK: `app/build/outputs/apk/dev/debug/app-dev-debug.apk`.

## Kiến trúc

Giữ 5 module: `app` (Compose, ViewModel, navigation, adapter session Android, Hilt), `domain` (contract/use case nghiệp vụ), `data` (API/DTO/repository), `shared` (Result/lỗi/tiện ích coroutine), `resources` (theme/màu/string).

Xem [kiến trúc hiện tại](docs/architecture.md) và [kiến trúc đích](docs/migration/02-kotlin-architecture.md).

## API và phát hành

Truyền `DEV_API_BASE_URL` / `PROD_API_BASE_URL` qua Gradle property hoặc biến môi trường. Giá trị mặc định hiện vẫn là JSONPlaceholder cho màn Users mẫu. Chỉ đổi URL chưa tạo ra tích hợp API LetsPlay.

Kiểm tra prodRelease hiện chặn endpoint demo nếu chưa cho phép rõ ràng bản demo. Bản phát hành thật cần tích hợp backend sản phẩm và cấu hình release/signing. Việc đổi package không đăng ký project Firebase hoặc tạo credential production.

## Kiểm tra

```powershell
.\gradlew.bat verifyModuleBoundaries :domain:test :data:test :shared:test
.\gradlew.bat :app:testDevDebugUnitTest :app:testProdDebugUnitTest :app:assembleDebug
```

Xem [testing](docs/testing.md) để biết report và giới hạn kiểm chứng. CI kiểm tra ranh giới module, unit test và cả hai variant debug.

## License và nguồn gốc

Giữ [MIT license notice](LICENSE) cùng mã tái sử dụng. Các sơ đồ/tài liệu kiến trúc kế thừa từ Kotlin-multi-module mô tả hạ tầng base. Tài liệu trong `docs/react-native-source` là tham chiếu cho chuyển đổi.
