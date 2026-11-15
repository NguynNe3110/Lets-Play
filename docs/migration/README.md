# Chuyển Let’s Play từ React Native sang Kotlin

Ngày lập: 15/11/2026. Phạm vi lượt này: **mapping + kế hoạch chi tiết trước**, theo câu trả lời của chủ dự án. Chưa triển khai tính năng Kotlin hoặc thay cấu hình build.

## Đọc theo thứ tự

1. [Bảng mapping source và tính năng](01-source-mapping.md).
2. [Kiến trúc Kotlin và lựa chọn công nghệ](02-kotlin-architecture.md).
3. [Kế hoạch triển khai theo giai đoạn](03-implementation-plan.md).
4. [Nghiệm thu, dữ liệu và quyết định còn mở](04-validation-and-decisions.md).
5. [Tài liệu React Native đã chuyển](../react-native-source/TRANSFER.md).

## Nguồn khảo sát

| Repository | Commit khảo sát | Vai trò |
| --- | --- | --- |
| let_play | e0e5f541f2efcde30acb0160431b949dcd7913f3 | Source React Native, nghiệp vụ và tài liệu sản phẩm |
| LetsPlay | ca9230044ab7ec34299bdb5c001eaf6a589538b5 | HEAD kiểm lại sau khi tiếp tục; base Kotlin và bản docs đã chuyển |

Source RN sạch tại kiểm tra cuối. Sau quãng ngắt, HEAD LetsPlay đã đổi và có sẵn 37 file tài liệu chuyển sang; đã đọc lại 8 file build/navigation/CI chủ chốt để xác nhận mô tả base. Bảng mapping dựa trên source, schema và tên test; không phải kết quả chạy ứng dụng hay backend. Các thay đổi code sau khảo sát cần đối chiếu lại.

## Hiện trạng cần hiểu đúng

- LetsPlay đang là base với Todo/Users. Đã có Compose Material 3, Hilt, Coroutines, Retrofit/OkHttp, DataStore, navigation và bộ Result/error/loading. Chưa có nghiệp vụ Let’s Play.
- let_play có hai hệ phòng: local được lưu MMKV và online gọi REST. Navigation hiện vào Home camera, có các tab Kèo, Tin nhắn và Thời gian.
- OnlineProvider và api/client.ts đã triển khai lời gọi account/phòng/đề xuất/lịch rảnh/chi tiêu. README và một số BA còn nói mobile chưa nối backend: thông tin này đã cũ so với source.
- Ảnh, reaction và comment hiện lưu trên máy. PhotoProvider gắn trạng thái LOCAL_ONLY. Composer có bố cục và adapter seam; runtime chưa có publish adapter/danh sách bạn bè thật.
- MessagesScreen là placeholder. Không có bằng chứng DM hoạt động giữa hai người.
- Continuous planning đã có reducer và test local. API online vẫn là phiếu boolean, chốt một đề xuất và giới hạn theo phase. Không áp kết luận “chưa làm” của tài liệu cũ cho mọi phần source.
- Các kết quả test/build đã ghi trong tài liệu chuyển sang là **kết quả lịch sử của React Native**, không phải nghiệm thu Kotlin.

## Phân biệt hai đích đến

**A — Tương đương source React Native:** chuyển hành vi hiện có sang Kotlin, gồm local room, legacy online room và ảnh local. Đây là các mốc kỹ thuật để kiểm tiến độ.

**B — Sản phẩm theo BA đã xác nhận:** bổ sung account bắt buộc, kế hoạch nhiều chặng online, duyệt/điều chỉnh tiền, chia sẻ ảnh theo quyền, bạn bè, DM và thông báo. Nhiều phần cần backend ngoài workspace.

Mốc A không đồng nghĩa đã đạt B. Không âm thầm bỏ DM hoặc tính năng BA vì source hiện chưa có. Ngược lại, không ghi “đã có” vì tài liệu đã mô tả. Các mốc trung gian dùng guest là tương thích nội bộ; sản phẩm đích tuân theo xác nhận bắt buộc đăng nhập.

## Thứ tự ưu tiên khi tài liệu khác nhau

1. Yêu cầu mới của chủ dự án trong phiên hiện tại.
2. [Biên bản xác nhận 11/11](../react-native-source/docs/ba/product-confirmation-2026-11-11.md) và [delta PC-01–14](../react-native-source/docs/ba/product-rules-2026-11-11.md).
3. [Quyết định ảnh](../react-native-source/docs/ba/photo-memories/08-decisions.md), [continuous planning](../react-native-source/docs/ba/continuous-planning.md) và [delta composer/feed](../react-native-source/docs/ba/photo-memories/09-ui-feed-and-capture-composer.md), trong phần chưa bị mục 2 thay thế.
4. BA nền, kế hoạch cũ và prototype làm tham chiếu; các đề xuất chưa duyệt giữ nguyên trạng thái.
5. Source quyết định **hành vi hiện có**; source không tự thay thế quyết định sản phẩm.

Ví dụ: yêu cầu đăng nhập bắt buộc thay baseline guest; cho nhập ảnh máy thay baseline camera-only; lookback 30 ngày là cutoff cố định khi kết bạn/tham gia, không phải TTL ảnh. Block không ẩn album chung; đóng và lưu lịch sử phòng khác xóa quyền đọc toàn bộ.

## Phạm vi đã xác nhận và đề xuất

| Mã | Nội dung | Trạng thái |
| --- | --- | --- |
| MIG-D01 | Đích là LetsPlay, fork từ Kotlin multi-module | XÁC NHẬN — yêu cầu người dùng |
| MIG-D02 | Dùng Jetpack cho UI và Hilt | XÁC NHẬN — yêu cầu người dùng |
| MIG-D03 | Lượt này mapping/kế hoạch và chuyển docs, chưa code app | XÁC NHẬN — câu trả lời trong phiên |
| MIG-D04 | Jetpack UI được triển khai bằng Compose Material 3 | ĐỀ XUẤT — phù hợp base đang có |
| MIG-D05 | Giữ module theo layer, thêm data-android cho adapter Android | ĐỀ XUẤT — chi tiết ở kiến trúc |
| MIG-D06 | Ưu tiên Android; không chuyển iOS trong kế hoạch này | GIẢ ĐỊNH — đích hiện tại là Android Kotlin |
| MIG-D07 | Giữ giao diện xanh–kem cho phòng, theme tối cho ảnh | ĐỀ XUẤT BẢO TOÀN — căn cứ theme source, không thiết kế lại |

Kế hoạch không cam kết ngày phát hành. Thời gian phụ thuộc contract backend, quyết định phát hành/cài đè, thiết bị test và số người thực hiện. Không cần giải quyết hết câu hỏi này để bắt đầu các ticket nền tảng.

## Kết quả của lượt lập kế hoạch

Tài liệu sản phẩm được đặt trong docs/react-native-source để không ghi đè tài liệu base. Bản chuyển giữ nội dung nghiệp vụ, điều chỉnh liên kết và ghi provenance trong TRANSFER.md. Mã Kotlin, Gradle, manifest, CI và source React Native không bị thay đổi.
