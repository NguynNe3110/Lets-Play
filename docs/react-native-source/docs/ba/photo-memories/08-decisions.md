# Quyết định sản phẩm và điểm cần chốt

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Ngày cập nhật 10/11/2026. Không có lựa chọn tự động nào dưới đây được coi là người dùng đã duyệt chỉ vì tài liệu dùng nó làm baseline. Đã nhận đủ ba câu trả lời ưu tiên trong phiên làm việc và ghi tại C-06 đến C-08.

## 1 Yêu cầu đã xác nhận

| Mã | Nội dung | Bằng chứng |
| --- | --- | --- |
| C-01 | Thêm chụp ảnh khoảnh khắc làm tính năng lõi | Yêu cầu trực tiếp ngày 10/11/2026 |
| C-02 | Tùy chọn chia sẻ cho bạn bè, cảm xúc và liên lạc theo ảnh | Mô tả trải nghiệm mong muốn cùng ngày |
| C-03 | Album của nhóm kèo nhận ảnh và cho xem lại sau chuyến đi | Mô tả cải tiến so với trải nghiệm cá nhân |
| C-04 | Được xem xét bình luận chung album | Người dùng nêu “có thêm ... cũng được” |
| C-05 | BA chi tiết để trao đổi backend và file cho agent | Yêu cầu đầu ra; chưa yêu cầu viết code tính năng |
| C-06 | Bản đầu có cảm xúc, bình luận chung và trả lời riêng qua tin nhắn | Trả lời câu hỏi tương tác ngày 10/11/2026 |
| C-07 | Chủ phòng thiết lập album sau kết thúc: chỉ xem, tiếp tục thêm, hoặc bổ sung có thời hạn | Trả lời tự do về quyền thiết lập của chủ phòng ngày 10/11/2026 |
| C-08 | Chụp từ phòng chọn sẵn album phòng, dòng cá nhân tùy chọn | Chọn phương án chia sẻ mặc định ngày 10/11/2026 |
| C-09 | Có backend ngoài repo và website riêng; người dùng cung cấp OpenAPI để đối chiếu | URL Render/Vercel và JSON dán trong hội thoại; xác nhận contract có nội dung, chưa phải chứng cứ runtime pass |
| C-10 | Yêu cầu thêm prototype toàn luồng và gói tài liệu/hướng dẫn giao backend/AI | Yêu cầu tiếp theo trong hội thoại; cho phép triển khai prototype riêng, không tự sửa hệ thống đang deploy |
| C-11 | Prototype cần giao diện cho điện thoại | Bổ sung trực tiếp “giao diện cho điện thoại”; ưu tiên mobile và khung phone trên máy tính, không tự hủy yêu cầu web tương lai trong BA cũ |
| C-12 | Prototype giữ bố cục điện thoại theo ảnh Locket, theme xanh–kem, có viền và danh mục màn hình | Yêu cầu chỉnh giao diện trực tiếp ngày 10/11/2026 |
| C-13 | Thành viên thêm đề xuất và bình chọn xuyên suốt kèo ACTIVE, kể cả đang đi, không bị khóa bởi chuỗi giai đoạn | Yêu cầu trực tiếp ngày 10/11/2026; [delta BA/model/API/event/AC](../continuous-planning.md), mới triển khai trong prototype |
| C-14 | Màn xem ảnh là feed/album cuộn liên tục: xem được ảnh của bản thân và người khác khi có quyền; tải trang tiếp theo bằng cursor và dừng khi hết dữ liệu | Mô tả UI và recording chủ sản phẩm cung cấp ngày 11/11/2026; chi tiết lỗi/retry tại [09](09-ui-feed-and-capture-composer.md) |
| C-15 | Sau khi chụp mở composer ngay trong cùng ý định đăng để nhập title/caption và chọn hủy, chụp lại, tải/đăng, nơi chia sẻ và người nhận; không chuyển sang một luồng rời làm mất ngữ cảnh | Mô tả UI và recording chủ sản phẩm cung cấp ngày 11/11/2026; chi tiết state/ACL tại [09](09-ui-feed-and-capture-composer.md) |
| C-16 | Composer theo ảnh bổ sung: tiêu đề “Gửi đến...”, “Thêm một tin nhắn” trên ảnh, hủy X, nút gửi hình máy bay và hàng avatar chọn người nhận; không dùng trang biểu mẫu chú thích/nơi lưu bên dưới ảnh | Chủ sản phẩm sửa trực tiếp bằng hai screenshot ngày 11/11/2026; xem [09](09-ui-feed-and-capture-composer.md) |
| C-17 | Chế độ lướt hiển thị một ảnh trên mỗi trang màn hình; mỗi lần vuốt chuyển một ảnh và dừng đúng trang, theo Scroll-Infinity.png | Chủ sản phẩm làm rõ ngày 11/11/2026; không thay contract cursor hoặc chế độ lưới/lịch |

## 2 Các lựa chọn ảnh hưởng trực tiếp hành vi

| Mã | Câu cần chốt | Quyết định hoặc baseline đề xuất | Lựa chọn khác và tác động | Cần chốt trước |
| --- | --- | --- | --- | --- |
| D-01 | Chụp từ phòng mặc định đăng đâu | ĐÃ XÁC NHẬN phần chọn sẵn album và cá nhân tùy chọn tại C-08; UI cho bỏ chọn vẫn là đề xuất | Thay mặc định cần chủ sản phẩm đổi quyết định | UI composer và publish |
| D-02 | Album sau kết thúc kèo | ĐÃ XÁC NHẬN ba chế độ do host thiết lập tại C-07; cơ chế cụ thể ở D-19 | Không được hardcode chỉ một chế độ | Lifecycle và ACL |
| D-03 | Phạm vi tương tác bản đầu | ĐÃ XÁC NHẬN có reaction, comment chung và trả lời riêng tại C-06; giới hạn text và cách tổ chức thread vẫn đề xuất | Hoãn DM sẽ giảm phạm vi đã xác nhận, phải hỏi lại | Estimate và API inbox |
| D-04 | Có tự đăng vào mọi phòng đang tham gia không | Không; chọn tối đa một phòng mỗi khoảnh khắc, phòng hiện tại chọn sẵn | Nhiều phòng cần nhiều placement, UX và tình huống lỗi từng đích | Data constraint |
| D-05 | Người mới vào xem ảnh trước khi vào không | Thành viên ACTIVE xem toàn bộ lịch sử album còn tồn tại; thông báo khi mời và trước đăng | Chỉ ảnh từ lúc vào cần ACL theo thời gian và quy tắc cho ảnh/bình luận cũ | Album query và membership |
| D-06 | Người rời/bị loại còn xem album không | Mất quyền album ngay; ảnh đã góp vẫn lưu; tác giả xóa được từ thư viện | Giữ quyền cựu thành viên cần snapshot/archive ACL riêng | Retention và permission |
| D-07 | Dòng cá nhân nghĩa là ai xem | OWNER, ALL_FRIENDS_SNAPSHOT hoặc SELECTED_FRIENDS; không public; bạn mới không tự nhận ảnh cũ | Public hoặc tập bạn bè động thay toàn bộ nguy cơ lộ lịch sử | Publish và query feed |
| D-08 | Cảm xúc và comment có dùng chung hai nơi không | Tách theo placement; comment album tách comment ảnh; personal chỉ reaction/DM | Dùng chung có thể lộ danh tính/bình luận của nhóm cho bạn ngoài nhóm | Interaction schema |
| D-09 | Có cho ảnh thư viện máy không | Camera trong app; có thể thêm ảnh **đã đăng trong thư viện Kèo** vào phòng | Cho camera roll cần source IMPORTED, quyền thư viện và thời gian chụp đáng tin khác | Capture UI và media validation |
| D-10 | Ảnh đã đăng được sửa thế nào | Caption và pixel bất biến; được gỡ nơi đăng hoặc đăng thêm nơi mới; đổi người nhận qua gỡ và tạo lại nơi cá nhân | Sửa caption/pixel cần revision, event update và quy tắc album lưu trữ | Contract update |
| D-11 | Host có quyền gì với ảnh người khác | Gỡ nơi đăng nhóm, xóa bình luận nhóm để điều hành; không xóa media cá nhân hoặc đọc DM | Đồng sở hữu ảnh/giữ ảnh bất chấp tác giả xóa là đổi mô hình quyền | Delete và audit |
| D-12 | Chính sách lưu ảnh và bản sao | Không tự xóa sau kết thúc; xóa logic lập tức; mục tiêu purge primary trong 30 ngày, backup tối đa 90 ngày; chưa có SLA lưu vĩnh viễn | TTL/giới hạn dung lượng cần thông báo và lifecycle dọn dữ liệu | Production storage và nội dung chính sách |
| D-13 | Điều kiện nhắn riêng | Là bạn bè hiện tại hoặc cùng phòng ACTIVE membership; source ảnh phải xem được ở thời điểm gửi | Chỉ bạn bè đơn giản hơn nhưng đồng đội chưa kết bạn không nhắn được | DM authorize |
| D-14 | Hủy kết bạn và chặn | Hủy kết bạn thu hồi grant cá nhân cũ vĩnh viễn; chặn ngăn DM và ẩn nội dung hai chiều, kể cả album chung | Cho nội dung chung vẫn hiện cần cảnh báo khác; tái kết bạn phục hồi ảnh cần thêm quy tắc | Friendship integration |
| D-15 | Lưu trữ phòng là chung hay cá nhân | Backend có ARCHIVED chung do host, chỉ đọc; ẩn phòng cá nhân là preference khác | Dùng một cờ cho cả hai sẽ làm thành viên khóa album của cả nhóm | Room API và migration |
| D-16 | Giới hạn kỹ thuật | Giá trị cụ thể tại 04 và 06 là cấu hình đề xuất; không dùng “không giới hạn” | Phải đo media, chi phí và tải trước chốt SLA | Capacity và phát hành |
| D-17 | Authentication và di trú dữ liệu local | Tái dùng contract account/login/Google/guest-claim đã thấy trong Swagger; xác minh Profile.id và mapping membership; đề xuất ảnh/DM cần account ổn định; giữ local riêng | Cho guest sở hữu ảnh cần khôi phục/chuyển quyền; không xây login thứ hai hoặc tự nhận tên local | Tích hợp production |
| D-18 | Khôi phục dữ liệu sau gỡ/xóa | Không có restore ở V1; đăng lại placement mới có ID và tương tác mới; xóa khoảnh khắc là không hoàn tác | Thùng rác cần retention, quyền restore và SLA riêng | Delete UX và cleanup |
| D-19 | Chi tiết thiết lập sau kết thúc | Mặc định OPEN; host đổi trước/sau kết thúc; VIEW_ONLY, OPEN, WINDOWED; hạn 1–720 giờ tính từ endedAt, gợi ý 72 giờ; áp dụng đăng ảnh, comment và reaction nhóm; xóa của mình luôn được | Có thể chọn hạn tuyệt đối hoặc cấu hình riêng ảnh/comment; phải sửa ACL, API, event và AC cùng lúc | Room settings và deadline |

## 3 Ba câu hỏi đã gửi ưu tiên

1. Đã chọn album phòng sẵn, dòng cá nhân tùy chọn. Nguồn C-08.
2. Đã chọn cho host cấu hình cả ba chế độ sau kết thúc. Nguồn C-07. Chi tiết mặc định, đơn vị hạn và phạm vi khóa tại D-19 còn là đề xuất.
3. Đã chọn đủ cảm xúc, bình luận chung và trả lời riêng. Nguồn C-06. Chi tiết DM giới hạn tại D-13 vẫn cần review.

Đội review những lựa chọn còn lại như đề xuất nhất quán. Không được mở rộng các xác nhận trên thành đồng ý toàn bộ D, hoặc hứa scope/time production thay người dùng.

## 4 Điểm backend cần thống nhất khi review

- Provider auth, stable user ID và cơ chế chuyển quyền khi mất thiết bị; owner ảnh không thể là tên hiển thị.
- Server room lifecycle và membership event; cờ archived local hiện tại không thể dùng lại trực tiếp.
- Cách thực thi thu hồi quyền media trong giới hạn 60 giây; signed URL tĩnh chỉ hết hạn không đủ cho yêu cầu chặn mọi request mới ngay lập tức.
- Storage, xử lý ảnh, queue/outbox và retention khả thi; không coi con số đề xuất là cam kết vận hành đã có.
- Phạm vi friendship, block, report và DM có nằm trong cùng milestone ảnh hay được cung cấp bởi service khác.
- Phân loại endpoint dependency và endpoint tính năng mới; giới hạn tải, mã lỗi, polling và schema phải được FE/BE review cùng nhau.

## 5 Cách cập nhật quyết định

Mỗi lần chủ sản phẩm trả lời, ghi ngày, nội dung lựa chọn, người xác nhận và nguồn trao đổi vào dòng D tương ứng. Sửa đồng thời FR/BR, DTO/API, event và AC chịu ảnh hưởng. Với thay đổi API đang dùng, ghi chiến lược tương thích hoặc version mới. Đổi quyết định không đồng nghĩa tính năng đã được implement.
