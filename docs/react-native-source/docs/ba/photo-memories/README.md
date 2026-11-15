# Đặc tả BA Khoảnh khắc và album kèo

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Phiên bản 0.9 • Ngày 10 tháng 11 năm 2026 • Trạng thái bản đề xuất để review

Bộ tài liệu dành cho chủ sản phẩm, backend, mobile và QA của Kèo / Let’s Play. Mục đích là bổ sung chụp ảnh khoảnh khắc, chia sẻ cho bạn bè và lưu ảnh vào album chung của phòng đi chơi. Backend có thể dùng bộ này để phân tích dữ liệu, ước lượng và thống nhất hợp đồng; các quyết định mang nhãn **ĐỀ XUẤT** chưa phải quyết định đã được chủ sản phẩm duyệt.

Yêu cầu cốt lõi đã rõ: người dùng chụp ảnh, chủ động chia sẻ, tương tác với ảnh và cùng lưu lại kỷ niệm của một kèo. Một ảnh được đăng cho bạn bè không có nghĩa bạn bè được xem album nhóm. Một ảnh xuất hiện ở album không tự trở thành ảnh chia sẻ cho toàn bộ bạn bè của người chụp.

## 1 Cách đọc và nguồn chuẩn

| Tài liệu | Người đọc chính | Nội dung phải lấy từ đây |
| --- | --- | --- |
| [01 Bối cảnh và phạm vi](01-context-and-scope.md) | Tất cả | Hiện trạng có bằng chứng, nghiên cứu Locket, phạm vi và phụ thuộc |
| [02 Yêu cầu và luồng nghiệp vụ](02-business-requirements.md) | Product, mobile, backend | FR, UC, màn hình, trạng thái giao diện và validation |
| [03 Quyền và vòng đời](03-permissions-and-lifecycle.md) | Backend, QA | BR, công thức cấp quyền, các trạng thái và tác động xóa |
| [04 Mô hình dữ liệu](04-domain-data.md) | Backend, mobile | Entity, dictionary, ràng buộc và DTO |
| [05 Hợp đồng API](05-api-contract.md) | Backend, mobile | Endpoint, request, response, mã lỗi, retry và concurrency |
| [06 Sự kiện và vận hành](06-events-notifications-nfr.md) | Backend, mobile, vận hành | Event, notification, offline, media, NFR và số đo |
| [07 Nghiệm thu và bàn giao](07-acceptance-and-delivery.md) | QA, cả đội | Kịch bản Given When Then, traceability và thứ tự triển khai |
| [08 Quyết định cần chốt](08-decisions.md) | Chủ sản phẩm, cả đội | Tách xác nhận với đề xuất; lựa chọn và tác động khi đổi |
| [09 Delta UI feed và composer](09-ui-feed-and-capture-composer.md) | Product, mobile, backend, QA | Feed cuộn theo cursor và composer sau chụp |

Thứ tự ưu tiên: yêu cầu mới nhất của chủ sản phẩm → quyết định đã xác nhận trong 08 → quy tắc của bộ tài liệu này → tài liệu cũ. Mâu thuẫn phải được ghi nhận và sửa đồng bộ, không tự lựa một cách hiểu khi triển khai. Source code là bằng chứng về tính năng **đang có**, không phải bằng chứng rằng yêu cầu tương lai đã được duyệt.

Tài liệu cũ [context/BA.md](../../../context/BA.md) vẫn tham chiếu góp ý và chia tiền; yêu cầu mới ngày 10/11 về thêm ý/bình chọn mọi lúc khi ACTIVE được ghi tại [luồng kế hoạch liên tục](../continuous-planning.md), thay ràng buộc giai đoạn một chiều. Giữ cách tính phiếu 2/1/0, Borda và công nợ. Bộ ảnh thay phạm vi “album để sau” bằng tính năng lõi. Các phát biểu cũ “chưa chọn RN/Flutter”, “chỉ có mockup”, “giữ phòng 90 ngày” không được coi là hiện trạng hoặc chính sách lưu album đã chốt.

## 2 Quy ước đọc đặc tả

- **ĐÃ XÁC NHẬN**: có trong yêu cầu của người dùng, ký hiệu `C-xx` trong 08.
- **ĐỀ XUẤT**: lựa chọn cụ thể dùng để bộ đặc tả nhất quán, ký hiệu `D-xx`; có thể review, chưa có nghĩa được phê duyệt. Khi nói “phải” trong FR/BR, đó là nghĩa vụ **nếu baseline đề xuất này được chọn**.
- **PHỤ THUỘC**: hợp đồng hệ thống khác phải cung cấp, ký hiệu `DEP-xx`; chưa có implementation trong repo.
- `FR-xx`: yêu cầu chức năng; `BR-xx`: quy tắc nghiệp vụ; `UC-xx`: luồng; `AC-xx`: ca nghiệm thu; `NFR-xx`: yêu cầu phi chức năng. Mã không đổi khi đổi thứ tự tài liệu.
- “Ngay khi mất quyền” là từ thời điểm transaction đổi quyền commit trên server. Bản tải xuống thiết bị ngoài app không thể bị thu hồi; cửa sổ cache/URL được quy định tại 06.
- Mọi ID, ngày giờ và người trong ví dụ là dữ liệu giả. Tên endpoint là hợp đồng **đề xuất**, không phải API hiện có.

## 3 Baseline đang dùng để thảo luận

Một phòng có một album. Chụp từ phòng thì chọn sẵn album đó và hiển thị rõ trước khi đăng; người chụp chọn thêm chia sẻ cá nhân nếu muốn. Chụp ở ngoài phòng không tự chọn bất kỳ phòng nào. Một khoảnh khắc dùng một media, tối đa một nơi đăng cá nhân đang hoạt động và một nơi đăng nhóm đang hoạt động. Thư viện của tôi chứa ảnh của tôi, kể cả ảnh chỉ đăng nhóm.

**Đã xác nhận trong phiên làm việc:** chụp từ phòng chọn sẵn album, cá nhân tùy chọn; bản đầu có cảm xúc, bình luận chung và trả lời riêng qua tin nhắn. Chủ phòng thiết lập album sau kết thúc theo ba chế độ: chỉ xem, tiếp tục đóng góp, hoặc đóng góp có thời hạn. Chi tiết mặc định và cách tính hạn vẫn là đề xuất tại D-19.

Lưu trữ chung phòng chuyển album sang chỉ đọc. Rời/bị xóa khỏi phòng mất quyền album nhưng ảnh đã góp vẫn được giữ cho nhóm, trừ khi tác giả xóa ảnh. Tác giả luôn có đường xóa ảnh do mình sở hữu từ thư viện, dù đã rời phòng. Tương tác trong album có tên, không dùng cơ chế ẩn tên của đề xuất đi chơi.

Phần cá nhân có chia sẻ theo bạn bè, cảm xúc và trả lời riêng. Trong nhóm có cảm xúc, bình luận ảnh và bình luận chung album. Tin nhắn riêng được đặc tả như module giới hạn văn bản và tham chiếu ảnh, không bao gồm chat nhóm đầy đủ. Phân biệt phạm vi đã xác nhận với chi tiết baseline `D-01` đến `D-19` tại 08 trước khi giao lập trình.

## 4 Mức sẵn sàng bàn giao

Tài liệu đủ để review nghiệp vụ và hợp đồng, chưa phải cam kết phát hành. Trước triển khai production phải chốt các lựa chọn ảnh hưởng mô hình quyền, phạm vi tin nhắn, danh tính online và lưu dữ liệu tại 08. Có thể tiếp tục các việc độc lập như mô hình, mock API, prototype quyền và thiết kế upload mà không báo các đề xuất là đã duyệt.

Trong lần bàn giao này chỉ thêm tài liệu và hướng dẫn agent. Không có camera, API, DB, authentication hoặc đồng bộ mới được triển khai. Không coi danh sách AC là kết quả test đã chạy.

Kết quả kiểm tra liên kết, định danh, ví dụ JSON và skill nằm tại [biên bản kiểm tra tài liệu](verification.md).
