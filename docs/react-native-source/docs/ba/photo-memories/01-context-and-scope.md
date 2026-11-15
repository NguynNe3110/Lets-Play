# Bối cảnh và phạm vi Khoảnh khắc

## 1 Vấn đề sản phẩm

Kèo hiện giúp nhóm thống nhất buổi đi chơi và đối soát chi phí. Sau buổi đi, nhóm chưa có nơi lưu chung ảnh trong ngữ cảnh của chính phòng đó. Ảnh gửi rải rác cho từng người khó xem lại như một chuyến đi. Tính năng mới tạo chu trình: thống nhất kèo → đi chơi và góp ảnh → đối soát → xem lại kỷ niệm.

Người chụp có hai nhu cầu độc lập: chia sẻ cuộc sống với bạn bè và đóng góp kỷ niệm cho nhóm của một kèo. Đặc tả dùng một khoảnh khắc gốc và các nơi đăng riêng để đáp ứng cả hai, không buộc tải hai bản ảnh hoặc trộn hai tập người xem.

## 2 Hiện trạng đã kiểm tra trong repo

| Bằng chứng | Điều quan sát được | Ý nghĩa cho tính năng ảnh |
| --- | --- | --- |
| [README](../../../README.md) | Bản chạy một thiết bị; chưa backend, login, mời liên thiết bị | Phải xây nền online trước khi có chia sẻ thật |
| [package.json](../../../package.json) | React Native 0.79, React 19; có Vision Camera và image picker trong dependencies | Có package không đồng nghĩa camera đã tích hợp hoặc hoạt động |
| [AppNavigator](../../../src/navigators/AppNavigator.tsx), [route](../../../src/configs/navigation.ts) | Hai route danh sách và chi tiết phòng | Chưa có route camera, feed ảnh, album hoặc DM |
| [types](../../../src/modules/keo/types/index.ts) | `Phase = ideas / vote / order / plan`, `host / member` | Chưa có trạng thái server ENDED hoặc album |
| [roomStorage](../../../src/modules/keo/utils/roomStorage.ts) | `members: string[]`, `currentUserIndex`, code local, schema v1 | Không dùng tên hoặc chỉ số mảng làm danh tính online |
| [KeoProvider](../../../src/modules/keo/store/KeoProvider.tsx) | MMKV `let-play-rooms`, key `rooms.v1`; xóa/lưu trữ là thao tác local | Không ánh xạ thao tác local này thành xóa/lưu trữ chung trên server |
| [BA cũ](../../../context/BA.md) | Album và chat từng nằm ngoài bản đầu; có đề xuất phòng khách và lưu 90 ngày | Yêu cầu ảnh mới thay đổi phạm vi; auth và retention phải quyết định lại |

Tính năng ghi sổ tiền không thực hiện chuyển tiền. Tên thành viên được nhập sẵn không đại diện tài khoản đã chấp nhận lời mời. Cần giữ dữ liệu local cũ đọc được; không tự gán tài khoản online cho những tên này khi migration.

Cập nhật sau khi nhận Swagger ngoài repo: backend có contract account/guest-claim/room/tiền/SSE, xem [đối chiếu tích hợp](../../integration/backend-gap-review.md). Những nhận định “chưa backend/login” trong bảng là về implementation mobile đã khảo sát, không còn được áp thành “toàn sản phẩm chưa có backend”. Nền DEP-01–03 cần tích hợp/xác minh và bổ sung phần còn thiếu, không mặc định xây lại toàn bộ.

## 3 Nghiên cứu Locket

Đã đọc nguồn chính thức ngày 10/11/2026 và xem trực tiếp toàn bộ bảy PNG do người dùng đặt trong `context/locket-images`. Ảnh là tham chiếu tại một thời điểm, không chứng minh mọi nền tảng hay phiên bản Locket hoạt động giống nhau.

Trong workspace hiện tại, `context/` bị bỏ qua bởi cấu hình Git local `.git/info/exclude`, nên ảnh và BA cũ không tự đi theo một bản clone. Các bảng dưới ghi lại đủ quan sát để hiểu đặc tả mà không cần các file đó. Nếu muốn người nhận xem ảnh gốc, gửi kèm riêng thư mục tham chiếu; không coi link tồn tại trên máy này là bằng chứng chúng đã được commit.

### 3.1 Nguồn chính thức

| Nguồn | Phát hiện dùng trong phân tích |
| --- | --- |
| [Mô tả Locket Widget do nhà phát triển đăng trên App Store](https://apps.apple.com/us/app/locket-widget/id1600525061) | Chụp và gửi cho bạn thân, widget, cảm xúc và lịch sử ảnh là phần trải nghiệm chính |
| [Thêm bạn bè](https://help.locket.com/en/articles/7914880-how-do-i-add-friends-on-locket) | Có lời mời/đồng ý kết bạn; không coi việc biết tên là đã thành bạn |
| [Xóa một Locket](https://help.locket.com/en/articles/10926929-how-to-delete-a-locket) | Locket mô tả xóa ảnh đã gửi ở cả phía người gửi và người nhận |
| [Chia sẻ ảnh cũ cho bạn mới](https://help.locket.com/en/articles/10927024-how-do-i-share-past-lockets-with-new-friends) | Nguồn ngày 05/05/2026 nói có lựa chọn chia sẻ 30 ngày gần nhất của ảnh gửi tất cả bạn bè; không được khẳng định bạn mới luôn không xem ảnh cũ |
| [Hủy kết bạn](https://help.locket.com/en/articles/7914985-how-do-i-unfriend-someone) | Có tác động tới ảnh và liên lạc; chính sách Kèo phải tự định nghĩa, không sao chép mặc định |

Không kết luận Locket hiện không có nhóm, bình luận, hoặc những tính năng ngoài bảy ảnh. Nghiên cứu này dùng để hiểu thao tác chụp, chọn người nhận và trả lời theo ảnh; không phải khảo sát đầy đủ đối thủ.

### 3.2 Quan sát ảnh được cung cấp

| Tệp | Quan sát trực tiếp | Học cho Kèo | Chưa được ảnh chứng minh |
| --- | --- | --- | --- |
| [home-screen.png](../../../context/locket-images/home-screen.png) | Preview camera lớn, flash, đổi camera, nút chụp, lối vào lịch sử, bạn bè và chat | Chụp nhanh, mở lịch sử thuận tiện | Quyền camera, nơi đăng mặc định, xử lý upload |
| [Scroll-Infinity.png](../../../context/locket-images/Scroll-Infinity.png) | Xem một ảnh, tác giả, thời gian, caption, bộ lọc bạn bè, emoji và ô gửi tin | Giữ ngữ cảnh ảnh khi tương tác | Reaction công khai hay riêng, cơ chế phân trang |
| [Grid-Scroll-Infinity.png](../../../context/locket-images/Grid-Scroll-Infinity.png) | Lưới nhiều ảnh và bộ lọc | Album có chế độ lưới và mở xem chi tiết | Thuật toán sắp xếp và quyền truy cập |
| [my-time-screen.png](../../../context/locket-images/my-time-screen.png) | Màn Kỷ niệm gom theo tháng, nhiều thumbnail, chỉ số chuỗi | Lịch sử có thể xem theo tháng | Cách tính chuỗi và điều kiện ảnh được đưa vào |
| [reply-friend.png](../../../context/locket-images/reply-friend.png) | Overlay “Replying to”, ô trả lời tên người và bàn phím | Nhãn Trả lời riêng cho tên tác giả cần rõ | Ai khác đọc được câu trả lời |
| [message-screen.png](../../../context/locket-images/message-screen.png) | Danh sách hội thoại, preview tin và chỉ dấu chưa đọc | Có inbox và badge đồng nhất | Quy tắc xóa, retention, block |
| [detail-message-screen.png](../../../context/locket-images/detail-message-screen.png) | Hội thoại hai người chứa ảnh và các đoạn văn bản | Tin nhắn có tham chiếu ảnh gốc | Ảnh được copy hay tham chiếu, thu hồi quyền |

Các quy tắc còn thiếu ở cột cuối là thiết kế **đề xuất của Kèo** trong các tài liệu tiếp theo, không phải kết quả suy đoán về backend Locket.

## 4 Yêu cầu xác nhận từ người dùng

`C-01`: tính năng chụp ảnh là tính năng lõi mới của Kèo.

`C-02`: người chụp được tùy chọn chia sẻ cho bạn bè; có tương tác cảm xúc và trả lời/liên lạc theo ảnh.

`C-03`: sau khi tham gia phòng, ảnh có thể góp vào album của kèo đó và đồng thời phục vụ chia sẻ cá nhân; nhóm xem lại sau chuyến đi.

`C-04`: có thể bổ sung bình luận chung album; đây là ý tưởng người dùng cho phép xem xét, chưa phải xác nhận chi tiết phạm vi.

`C-05`: cần BA kỹ, không mơ hồ với backend, và tài liệu/skill để agent đọc. Không có yêu cầu triển khai tính năng ở lượt này.

`C-06` đến `C-08`: đã được người dùng xác nhận trong phiên: có đủ cảm xúc/comment chung/trả lời riêng; host thiết lập cả ba chế độ đóng góp sau kết thúc; chụp từ phòng chọn sẵn album, cá nhân tùy chọn. Chi tiết xem 08.

## 5 Phạm vi baseline đề xuất

| Mức | Bao gồm | Điều kiện |
| --- | --- | --- |
| Nền bắt buộc | Danh tính online, phiên, membership thật, lời mời phòng, quan hệ bạn bè, kiểm tra quyền server | Chọn giải pháp auth riêng; hợp đồng tối thiểu ở mục 6 |
| Lõi ảnh | Chụp ảnh tĩnh, preview, caption, chọn cá nhân/nhóm/cả hai, upload, feed bạn bè, thư viện của tôi, album phòng | Một media cho mỗi lần đăng; quyền tách theo nơi đăng |
| Tương tác | Reaction ảnh cá nhân/nhóm; bình luận ảnh trong nhóm; bình luận album | Không tự biến bình luận thành tin nhắn |
| Trả lời riêng | Hội thoại hai người, văn bản và tham chiếu ảnh, unread, thu hồi tin của mình | Phạm vi cần chọn tại D-03; không chat nhóm, gọi hay file |
| Quản lý | Gỡ nơi đăng, xóa toàn bộ ảnh của mình, host gỡ nội dung nhóm, báo cáo, chặn | Tách quyền host với quyền sở hữu ảnh |
| Xem lại | Lưới, chi tiết, lọc theo người và tháng; album của phòng đã kết thúc | Không tự xóa sau kết thúc |

Ngoài bản này: video/live photo, widget hệ điều hành, AI recap, streak, filter AR, nhận diện khuôn mặt, tự gắn vị trí, ảnh hóa đơn tự chia tiền, public feed, tìm người lạ, link album công khai, tải cả album/ZIP, xuất bản ra mạng xã hội, upload từ thư viện hệ điều hành, mention và thread bình luận nhiều tầng. Chủ sản phẩm có thể chọn lại, nhưng không đưa ngầm vào MVP.

## 6 Hợp đồng phụ thuộc

| Mã | Bên cung cấp | Hợp đồng tối thiểu cần có | Khi chưa có |
| --- | --- | --- | --- |
| DEP-01 | Identity/auth | Token kiểm chứng được; `userId` ổn định; user ACTIVE/SUSPENDED/DELETED; logout/thu hồi phiên | Không phát hành chia sẻ thật; không dùng `currentUserIndex` |
| DEP-02 | Rooms/membership | `roomId`, phase, lifecycle, timezone, revision; membership ACTIVE/LEFT/REMOVED, role HOST/MEMBER; join được server chấp nhận; quyền host | Không dùng mã local để cấp quyền ảnh |
| DEP-03 | Friendship/block | Quan hệ hai chiều đã đồng ý; thêm/hủy kết bạn; directed block; sharingEpoch tăng khi hủy/chặn, không reset khi kết bạn lại; truy vấn và sự kiện hiện tại | Không mở chia sẻ cá nhân ALL/SELECTED |
| DEP-04 | Media/storage | Upload private, xử lý ảnh, checksum, thumbnail, URL/stream có kiểm quyền, dọn orphan | Không trả object URL công khai |
| DEP-05 | Notification | Đăng ký thiết bị theo user; in-app event; push tùy chọn, preferences | Push có thể hoãn; dữ liệu và unread vẫn đúng |
| DEP-06 | Moderation/ops | Nhận report, queue nội bộ, thao tác hide có audit; cơ chế phản hồi | Phải có đường xử lý thực tế trước mở rộng người dùng |

Khi user mới online, không tự chuyển các “bạn” dạng chuỗi trong phòng local thành tài khoản thật. Phương án tối thiểu là giữ phòng local riêng và tạo phòng online mới; migration có ánh xạ từng người là hạng mục độc lập cần chủ sản phẩm chọn.

## 7 Đo hiệu quả

Đo tỷ lệ phòng có ít nhất hai người góp ảnh, số người góp ảnh riêng biệt trên mỗi phòng, tỷ lệ người quay lại album trong 7 và 30 ngày sau kết thúc, tỷ lệ đăng thành công và thời gian từ chạm Đăng đến nhận xác nhận. Chưa có baseline để cam kết tăng trưởng. Không đặt thành công là số ảnh tối đa vì có thể khuyến khích spam; phân biệt ảnh đăng với upload thử/thất bại.
