# Đặc tả nghiệp vụ ứng dụng lập kế hoạch đi chơi nhóm

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../docs/ba/product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Phiên bản 1.1 • Ngày 04 tháng 11 năm 2026 • Tên làm việc Kèo

**Cập nhật 10/11/2026:** chủ sản phẩm yêu cầu thêm đề xuất và bình chọn xuyên suốt kèo, kể cả đang đi. [Luồng kế hoạch liên tục](../docs/ba/continuous-planning.md) thay các ràng buộc giai đoạn một chiều bên dưới; giữ phiếu 2/1/0, Borda, quyền chốt của host và các quy tắc tiền. Prototype đã cập nhật; source mobile và backend chưa cập nhật.

Tài liệu dành cho chủ sản phẩm và người lập trình. Sản phẩm giúp một nhóm bạn cùng đề xuất mong muốn ẩn tên, bình chọn hoạt động, thống nhất thứ tự, sử dụng một kế hoạch chung cho một buổi gặp và đối soát chi phí nhóm. Đây là bản đặc tả để bắt đầu xây dựng, chưa phải biên bản xác nhận toàn bộ quy tắc.

## 1 Mục tiêu và quyết định đã thống nhất

- Một người tạo phòng và gửi link hoặc mã mời cho nhóm.
- Mỗi thành viên có thể thêm nhiều đề xuất về việc muốn làm hoặc nơi muốn đến. Không đặt hạn mức số đề xuất theo người trong nghiệp vụ.
- Không hiển thị tác giả đề xuất cho thành viên khác, kể cả chủ phòng.
- Thay bình chọn hủy bằng ba lựa chọn Rất muốn, Đi cùng được và Để dịp khác.
- Tách chọn hoạt động khỏi việc sắp thứ tự thực hiện.
- Có thời điểm đóng góp ý để nhóm chuyển sang quyết định.
- Cho phép hưởng ứng ý đã có bằng Mình cũng muốn để giảm trùng lặp.
- Ngân sách, giờ cần về và những hạn chế thực tế được thể hiện riêng.
- Giao diện ưu tiên thao tác nhanh và micro-interactions nhẹ khi chạm, kéo thả, lưu và chuyển trạng thái. Không dùng không gian game, avatar hay cảnh bàn giấy làm yêu cầu.
- Cần thiết kế cho điện thoại và web desktop. RN hoặc Flutter vẫn là lựa chọn mở, chưa chốt framework.

## 2 Phạm vi bản đầu

Bao gồm tạo và tham gia phòng, lưu quyền truy cập trên thiết bị, quản lý thành viên, góp ý ẩn tên, thông tin giới hạn, bình chọn, chọn danh sách hoạt động, gửi thứ tự ưu tiên, bản kế hoạch, đánh dấu hoàn thành, xử lý thay đổi và chia tiền nhóm. Chia tiền là yêu cầu bổ sung đã được chủ sản phẩm xác nhận.

Để giai đoạn sau: tìm quán từ nguồn dữ liệu bên ngoài, tự kiểm tra giá và giờ mở cửa, tối ưu đường đi, chat đầy đủ, album ảnh, AI gợi ý và thông báo đẩy. Những phần này không phải điều kiện để bản đầu có giá trị. Người dùng có thể tự nhập địa điểm hoặc gắn link.

Chỉ tiêu cần đo sau thử nghiệm: tỷ lệ người được mời tham gia; tỷ lệ hoàn tất bình chọn; thời gian từ tạo phòng đến chốt; tỷ lệ nhóm tạo buổi thứ hai. Chưa có dữ liệu để đặt lời hứa hiệu quả.

## 3 Vai trò và quyền

| Hành động | Chủ phòng | Thành viên |
| --- | --- | --- |
| Đổi tên phòng và thông tin chung | Có | Xem |
| Mời người khác | Có | Có nếu chủ phòng cho phép |
| Thêm đề xuất và bình chọn | Có như thành viên | Có |
| Sửa xóa đề xuất | Đề xuất của mình khi đang góp ý | Đề xuất của mình khi đang góp ý |
| Xem tác giả của người khác | Không | Không |
| Chuyển giai đoạn và mở lại | Có | Không |
| Chọn danh sách hoạt động thực hiện | Đề xuất bản nháp và xác nhận | Xem và gửi ý kiến |
| Gửi thứ tự mong muốn | Có | Có |
| Chỉnh lịch cuối cùng và chốt | Có | Xem |
| Đánh dấu hoàn thành | Có | Có |
| Chuyển quyền chủ phòng | Có | Nhận quyền khi đồng ý |
| Ghi khoản chi và xem số dư | Có như thành viên | Có |
| Xác nhận nhận thanh toán | Chỉ khi chính mình là người nhận | Chỉ khi chính mình là người nhận |

Quản trị kỹ thuật có thể truy cập dữ liệu vận hành theo chính sách; ẩn tên trong nhóm không đồng nghĩa vô danh tuyệt đối. API trả đề xuất không chứa authorId; quyền sửa của chính người gửi được xác định phía máy chủ. Không để chủ phòng suy ra người gửi qua lịch sử hoạt động. Bản demo chỉ mô phỏng giao diện và không cung cấp bảo mật này.

## 4 Chu trình phòng

GÓP Ý → BÌNH CHỌN → SẮP LỊCH → ĐÃ CHỐT → KẾT THÚC

GÓP Ý: được thêm sửa xóa ý của mình; thể hiện các đề xuất ngang hàng, không hiện số hưởng ứng. BÌNH CHỌN: khóa nội dung hoạt động; mỗi người trả lời ba mức cho từng ý và được sửa trước hạn. SẮP LỊCH: mở kết quả tổng hợp, chủ phòng chọn các hoạt động dự kiến và thành viên gửi thứ tự mong muốn. ĐÃ CHỐT: lịch là bản chính thức, bình chọn và thứ tự chỉ xem. KẾT THÚC: lưu lịch sử và kết quả hoàn thành.

Phòng có thể bị hủy bởi chủ phòng sau xác nhận. Chủ phòng có thể đóng giai đoạn khi đã tới hạn hoặc chủ động đóng sớm sau cảnh báo số người chưa hoàn tất. Thời hạn và đồng hồ chuyển trạng thái lấy từ máy chủ, không từ đồng hồ điện thoại. Hạn mặc định là không đặt; chủ phòng chủ động chuyển.

Người vào khi đang bình chọn vẫn được tham gia nếu giai đoạn chưa đóng. Người vào khi đang sắp lịch được gửi thứ tự trước khi chốt nhưng không sửa phiếu hoạt động đã khóa. Người vào sau chốt chỉ xem kế hoạch. Thành viên rời phòng vẫn giữ đóng góp đã gửi trong lần chốt hiện tại; không tự làm thay đổi kết quả của mọi người.

## 5 Luồng chính

### UC01 Tạo và tham gia

Chủ phòng nhập tên hiển thị và tên buổi gặp. Ngày, giờ và khu vực đều tùy chọn. Tạo thành công trả roomId, mã mời và quyền chủ phòng gắn với phiên. Người được mời mở link hoặc nhập mã, xem thông tin cơ bản và nhập tên. Tên hiển thị không phải khóa nhận dạng. Máy chủ cấp một membership duy nhất cho phiên; tải lại hoặc bấm tham gia nhiều lần không tạo nhiều thành viên.

Đề xuất bản đầu dùng phiên khách lưu trên thiết bị. Đổi máy, xóa dữ liệu hoặc mở ẩn danh có thể mất phiên; cần mã khôi phục riêng nếu muốn tiếp tục quyền cũ. Link mời không cấp quyền chủ phòng. Chủ phòng có thể khóa hoặc tạo lại link mời. Các giới hạn này phải được giải thích trước khi người dùng xóa dữ liệu.

### UC02 Đề xuất

Người dùng chạm Mình muốn rồi nhập tiêu đề. Ghi chú, thời lượng, chi phí mỗi người và link địa điểm là tùy chọn. Sau lưu thành công hiện thẻ ẩn tên; đề xuất của chính mình có nhãn Chỉ bạn thấy và nút sửa xóa. Một người không được tự hưởng ứng ý của mình; những người khác chỉ hưởng ứng một lần và có thể bỏ hưởng ứng. Hưởng ứng là dấu hiệu quan tâm, không tự thay phiếu bình chọn sau này.

Không giới hạn số ý theo người ở cấp sản phẩm; phân trang và hạn chế tần suất gửi để tránh spam là giới hạn kỹ thuật, cần thông báo khi gặp. Tìm trùng gợi ý theo tiêu đề, không tự gộp hoặc xóa nội dung. Nếu chủ phòng yêu cầu gộp, người gửi quyết định sửa trong giai đoạn góp ý.

### UC03 Thông tin cần lưu ý

Mỗi thành viên khai báo tùy chọn ngân sách trần, giờ phải về, hạn chế ăn uống hoặc khả năng tham gia. Chọn hiển thị có tên hoặc ẩn tên cho ghi chú nhạy cảm. Dữ liệu được trình bày trong bảng lưu ý chung; không tính thành phiếu phản đối. Trong bản đầu chủ phòng kiểm tra thủ công các mâu thuẫn và đánh dấu đã xử lý hoặc cần hỏi lại. App không cam kết tự hiểu văn bản hoặc tự kiểm tra địa điểm.

### UC04 Bình chọn

Mỗi membership có đúng một lựa chọn cho một hoạt động ở mỗi vòng bình chọn. Thao tác lần nữa thay lựa chọn trước, không cộng phiếu. Chưa chọn khác với Để dịp khác. Nút Xong lượt chọn chỉ hoàn tất khi đã trả lời tất cả thẻ; bản nháp được lưu từng thao tác. Nếu lưu lỗi, hiển thị Chưa lưu và cho thử lại, không báo thành công giả.

Kết quả và người hưởng ứng được ẩn khi đang bình chọn. Chỉ hiện tiến độ tổng như 4 trên 6 người đã xong. Danh sách giữ thứ tự ổn định trên mỗi thiết bị để không nhảy khi chạm; xáo thứ tự khi bắt đầu vòng để giảm thiên lệch. Danh sách người đã hoàn tất không công khai.

### UC05 Chọn hoạt động và sắp lịch

Khi đóng phiếu, hiển thị tổng từng mức và số chưa trả lời cho từng ý. Không tự loại một ý vì có phiếu Để dịp khác. Chủ phòng chọn danh sách thực hiện theo đồng thuận và các giới hạn đã trao đổi. Ý chưa chọn đi vào Để dịp khác, giữ nguyên ẩn tác giả.

Chủ phòng khóa danh sách hoạt động trước khi mọi người xếp thứ tự. Mỗi thành viên kéo thả hoặc dùng nút Lên Xuống trên toàn bộ danh sách rồi gửi thứ tự mong muốn. Đề xuất tổng hợp theo điểm Borda: với n hoạt động, hạng đầu nhận n trừ 1 điểm và hạng cuối 0 điểm. Chỉ tính phiếu thứ tự đã gửi đầy đủ. Hòa điểm giữ thứ tự danh sách do chủ phòng khóa ban đầu, đồng thời gắn nhãn Cùng mức ưu tiên. Nếu chưa ai gửi, hiển thị bản nháp ban đầu và ghi rõ chưa có phiếu.

Thứ tự tổng hợp là gợi ý ưu tiên. Chủ phòng sửa lịch cuối theo địa điểm, giờ ăn, thời lượng và giờ về; các chỉnh sửa khác gợi ý phải được ghi nhận trong thông báo chốt. Nếu đổi danh sách hoạt động trong lúc xếp lịch, hủy hiệu lực toàn bộ phiếu thứ tự và yêu cầu gửi lại; phiếu chọn hoạt động vẫn được giữ.

### UC06 Chốt và thực hiện

Chủ phòng nhập giờ cho các chặng nếu muốn, kiểm tra lưu ý chưa giải quyết và xem cảnh báo người chưa gửi thứ tự. Có thể chốt khi còn người chưa trả lời, nhưng phải xác nhận số người thiếu. Cần có ít nhất một hoạt động. Cảnh báo trùng thời gian hoặc vượt ngân sách chỉ đưa ra nếu đủ dữ liệu có cấu trúc; dữ liệu thiếu hiện Chưa có thông tin.

Kế hoạch có số phiên bản và thời điểm chốt. Mọi người xem cùng phiên bản, mở link địa điểm và đánh dấu chặng hoàn thành. Thao tác hoàn thành có thể hoàn tác; lưu người thao tác để xử lý nhầm lẫn, không liên hệ với tác giả đề xuất.

Đổi kế hoạch sau chốt phải qua Mở lại và xác nhận. Thay nội dung hoạt động hoặc bổ sung hoạt động quay lại Góp ý, tạo vòng mới, lưu phiếu cũ vào lịch sử và yêu cầu bình chọn lại. Chỉ đổi giờ hoặc thứ tự quay lại Sắp lịch; lưu lịch cũ, xóa trạng thái xác nhận cũ và yêu cầu chốt phiên bản mới. Không lặng lẽ thay đổi kế hoạch đã chốt.

## 6 Quy tắc cần duyệt

Các mục dưới đây là ĐỀ XUẤT, chưa được người dùng xác nhận riêng từng mục.

| Mã | Quy tắc đề xuất | Lý do |
| --- | --- | --- |
| BR01 | Điểm gợi ý chọn hoạt động bằng 2 × Rất muốn + 1 × Đi cùng được | Dễ giải thích; tổng từng mức vẫn hiển thị |
| BR02 | Thiếu phiếu không tính như phiếu 0; hiển thị số thiếu riêng | Tránh hiểu nhầm im lặng là không muốn |
| BR03 | Hòa điểm hoạt động giữ thứ tự cố định của vòng; chủ phòng quyết định shortlist | Không chọn ngẫu nhiên ngoài ý muốn |
| BR04 | Borda cho phiếu thứ tự đầy đủ | Chọn hoạt động khác với chọn thứ tự |
| BR05 | Chủ phòng chốt cuối, không có quyền đọc tác giả hay phiếu cá nhân | Có người chịu trách nhiệm khi thiếu phản hồi |
| BR06 | Một thành viên cũng có thể thử phòng; cảnh báo khi chốt chưa có bạn tham gia | Phục vụ học cách dùng |
| BR07 | Tiêu đề hoạt động 1 đến 100 ký tự, ghi chú tối đa 500 ký tự | Dễ đọc trên điện thoại |
| BR08 | Chi phí là VND mỗi người, thời lượng là phút; chưa biết để trống | Không biến dữ liệu thiếu thành 0 đồng |
| BR09 | Giữ phòng kết thúc 90 ngày rồi xóa hoặc cho chủ phòng xuất trước | Hạn chế lưu dữ liệu không cần thiết; cần chốt chính sách thật |

## 7 Màn hình và tương tác

M01 Trang đầu có Tạo buổi đi chơi, Tham gia bằng mã và các phòng gần đây. M02 biểu mẫu tạo hoặc tham gia. M03 phòng chung có tiêu đề, thành viên, trạng thái, danh sách đề xuất và bảng lưu ý. M04 bình chọn dùng cùng khung phòng với ba nút lựa chọn và tiến độ cá nhân. M05 sắp lịch có kết quả, shortlist, thứ tự cá nhân và bản gợi ý. M06 kế hoạch đã chốt có các chặng, giờ và trạng thái hoàn thành.

Điện thoại dùng một cột, bảng nhập trượt từ dưới lên và nút hành động bám cuối nội dung với khoảng trống an toàn. Desktop dùng điều hướng bên trái, nội dung giữa và bảng lưu ý bên phải. Không thay đổi nghiệp vụ giữa hai nền tảng. M07 Chia tiền gồm tổng quan số dư, danh sách chi, biểu mẫu chia đều hoặc riêng, gợi ý trả tiền và xác nhận thanh toán.

Phản hồi chạm 100 đến 160 ms; chuyển bảng và thẻ 180 đến 240 ms là điểm khởi đầu cần kiểm tra trên máy thật. Rung nhẹ chỉ khi nền tảng hỗ trợ và người dùng cho phép. Respect prefers-reduced-motion trên web. Không tự đổi vị trí thẻ đang vote. Luôn có nút Lên Xuống thay cho kéo, focus rõ, điều khiển bằng bàn phím, nhãn cho nút biểu tượng. Vùng chạm tối thiểu 44 CSS px cho web di động và điều chỉnh theo nền tảng native.

Trạng thái bắt buộc thiết kế: phòng trống; link sai hoặc hết hiệu lực; bị khóa tham gia; đang lưu; lưu lỗi; mất mạng; thay đổi trạng thái từ máy khác; phiếu chưa đủ; shortlist trống; dữ liệu chưa biết; phiên người dùng hết hạn; chủ phòng rời đi; kế hoạch đang được cập nhật.

## 8 Mô hình dữ liệu và ranh giới bảo mật

Room lưu id, tên, vùng giờ, lịch dự kiến, giai đoạn, deadline, phiên bản và hostMembershipId. Membership lưu định danh nội bộ, tên, vai trò và tình trạng tham gia. Invitation lưu token băm, hạn và trạng thái thu hồi. Proposal lưu id, roomId, authorMembershipId nội bộ, tiêu đề, nội dung, chi phí, thời lượng, link và phiên bản. Endorsement có khóa duy nhất theo membership và proposal. Constraint lưu chủ sở hữu nội bộ, phạm vi hiển thị và nội dung. Vote khóa duy nhất theo vòng, membership và proposal; giá trị 2, 1 hoặc 0. OrderBallot lưu danh sách proposalId duy nhất và đủ; PlanVersion lưu các chặng, thời điểm, người chốt; Completion lưu người thao tác và phiên bản kế hoạch.

Máy chủ kiểm tra membership và trạng thái trên mọi thay đổi. Không dựa vào ẩn nút để phân quyền. Không đưa tác giả hoặc phiếu của người khác vào payload rồi chỉ ẩn bằng CSS. Log ứng dụng không ghi token mời, mật khẩu hay nội dung nhạy cảm. Link địa điểm chỉ chấp nhận HTTPS hoặc HTTP và không thực thi HTML người dùng.

Một sự kiện thay đổi cần revision hoặc version để từ chối ghi đè dữ liệu mới hơn. Lưu phiếu là upsert có tính idempotent. Nếu đóng vòng và gửi phiếu đồng thời, máy chủ nhận giao dịch hợp lệ trước; phía còn lại nhận trạng thái mới và thông báo rõ.

Phiên khách không bảo đảm mỗi con người chỉ có một tài khoản. Hạn chế tạo phiên hàng loạt, khóa link khi đủ người và quy trình chấp thuận thành viên là biện pháp ban đầu. Nếu cần bầu chọn có độ tin cậy cao hơn, bổ sung xác thực tài khoản ở giai đoạn sau.

## 9 Tiêu chí chấp nhận

| Mã | Tình huống | Kết quả cần đạt |
| --- | --- | --- |
| AC01 | Nhập tên phòng hợp lệ và tạo | Có phòng, mã mời và quyền chủ; gửi lặp không tạo hai phòng |
| AC02 | Dùng link mời hợp lệ | Tạo một membership và có thể vào lại bằng phiên cũ |
| AC03 | Thành viên hoặc chủ phòng đọc đề xuất người khác | Không nhận được tác giả từ giao diện và API |
| AC04 | Gửi hơn một đề xuất | Tất cả được lưu; không có quota theo người trong nghiệp vụ |
| AC05 | Đổi Rất muốn sang Đi cùng được | Còn đúng một phiếu giá trị 1 |
| AC06 | Chưa chọn một thẻ | Không cho hoàn tất; chỉ ra thẻ còn thiếu |
| AC07 | Đóng phiếu khi còn người thiếu | Có xác nhận; tổng hợp ghi rõ thiếu, không quy thành phản đối |
| AC08 | Kéo đổi thứ tự hoặc dùng nút Lên Xuống | Cùng kết quả; thứ tự đủ và không có id trùng |
| AC09 | Chủ phòng đổi shortlist | Phiếu thứ tự cũ mất hiệu lực và nhóm được yêu cầu gửi lại |
| AC10 | Thành viên thường gọi hành động chốt | Máy chủ từ chối dù sửa request thủ công |
| AC11 | Mất mạng khi lưu | Không hiện Đã lưu; nội dung giữ để thử lại |
| AC12 | Hai thiết bị cập nhật cùng lúc | Không đếm phiếu hai lần; nhận revision mới nhất |
| AC13 | Mở lại kế hoạch đã chốt | Giữ lịch sử phiên bản và yêu cầu chốt lại |
| AC14 | Phòng chưa có hoạt động | Hiện hướng dẫn thêm ý; không cho chốt rỗng |
| AC15 | Bật giảm chuyển động và dùng bàn phím | Vẫn hoàn thành luồng; focus không bị che |
| AC16 | Xem trên phone và desktop | Cùng dữ liệu, quyền và trạng thái; không tràn ngang |

## 10 Phân phối và lựa chọn kỹ thuật

Android có thể phát APK qua GitHub Releases theo mong muốn của chủ sản phẩm. iOS native cần cơ chế ký và phân phối riêng như TestFlight hoặc sideload; không coi file IPA là một file bấm cài công khai. Web ưu tiên màn hình điện thoại và có thể bổ sung PWA để bạn bè vào phòng qua link. Chi phí hosting, backend và chính sách nền tảng cần xác minh lúc triển khai.

Chưa quyết định RN hay Flutter. Phía server, hợp đồng dữ liệu, phân quyền và quy tắc phòng nên tách khỏi UI để Android và web dùng chung. Bản mockup giao kèm dùng HTML CSS JavaScript độc lập để đánh giá trải nghiệm; không đại diện cho lựa chọn framework sản xuất.

## 11 Những điều cần chốt trước khi lập trình production

1. Chấp nhận thang điểm hoạt động và Borda hay ưu tiên chỉ hiển thị phiếu thô.
2. Có dùng phiên khách và mã khôi phục trong bản đầu hay yêu cầu đăng nhập.
3. Chủ phòng có quyền chốt khi còn thiếu phiếu; nội dung xác nhận cụ thể.
4. Giới hạn nhóm dự kiến và chính sách duyệt thành viên mới.
5. Chính sách lưu xóa phòng, trách nhiệm với thông tin ngân sách và giờ về.
6. Chọn RN hoặc Flutter và mức ưu tiên cho phiên bản web.

Các câu hỏi này không cản việc duyệt mockup. Những mặc định trong tài liệu là cơ sở thảo luận, không được gắn nhãn đã được người dùng đồng ý.

## 12 Cách sử dụng bộ mockup

Mở mockup.html bằng trình duyệt. Chuyển Phone hoặc Web để xem bố cục. Có thể đổi vai chủ phòng hoặc thành viên; thêm ý, hưởng ứng, chọn phiếu, chọn hoạt động, sắp thứ tự, chốt và đánh dấu hoàn thành. Thanh xem các giai đoạn là điều khiển trình diễn, không phải quyền chuyển trạng thái dành cho mọi người trong app thật.

Dữ liệu bạn bè trong mockup là dữ liệu mẫu. Lưu chỉ trên trình duyệt hiện tại, không có backend, phòng thật, bảo mật ẩn danh, gửi lời mời thật hay đồng bộ nhiều thiết bị. Nút link mời chỉ sao chép nội dung minh họa và thông báo rõ. Nút đặt lại xóa riêng dữ liệu mẫu của mockup.


## 13 Chia tiền nhóm

Chia tiền là một tab riêng trong cùng phòng, luôn truy cập được từ giai đoạn Góp ý đến khi kết thúc; phòng đã lưu trữ thì chỉ đọc. Tác giả đề xuất vẫn ẩn tên. Khoản chi và thanh toán có danh tính để các bên đối soát, không áp dụng ẩn danh của ý tưởng cho dữ liệu tiền.

### Phạm vi và quyền

Mỗi thành viên có thể tạo khoản chi với một người trả và một tập người tham gia. Người nhập có thể ghi hộ người khác nhưng khoản ghi hộ phải mang trạng thái chờ người trả xác nhận trước khi tính vào sổ thật. Mockup minh họa dữ liệu đã được nhóm xác nhận. Chủ phòng không được tự xác nhận đã nhận tiền thay thành viên khác. Người tạo chỉ sửa xóa khoản của mình khi chưa có thanh toán liên quan; người trả cần xác nhận thay đổi nếu là bản ghi hộ. Thành viên khác xem chi tiết và yêu cầu sửa. Sau khi phát sinh thanh toán đã xác nhận, yêu cầu điều chỉnh có lịch sử và xác nhận các bên; không xóa trực tiếp dữ liệu gốc.

### Ghi khoản chi

Tiêu đề bắt buộc tối đa 100 ký tự, số tiền nguyên VND từ 1 đến 100 triệu đồng, một người đã trả, ít nhất một người tham gia. Người trả có thể không sử dụng khoản chi. Người không tham gia không bị chia tiền. Tùy chọn gắn hoạt động; ngày giờ và người nhập được lưu. Ảnh hóa đơn và đa tiền tệ để giai đoạn sau.

Hai cách chia trong bản đầu: chia đều và nhập số tiền riêng. Chia đều lấy phần nguyên tiền chia số người, phân bổ từng đồng dư theo thứ tự membershipId cố định. Chia riêng chấp nhận phần 0 đồng, mọi phần phải là số nguyên không âm; tổng chính xác bằng khoản chi. Không làm tròn mất hoặc sinh thêm tiền. Không chia theo phần trăm hay trọng số trong bản đầu. Ngân sách dự kiến không tự biến thành khoản chi thực tế.

### Số dư và gợi ý trả tiền

Số dư của một người bằng tổng khoản đã trả trừ tổng phần được chia, cộng khoản đã chuyển được người nhận xác nhận, trừ khoản đã nhận được xác nhận. Số dương là còn được nhận, số âm là còn cần trả. Tổng mọi số dư luôn bằng 0 đồng. Chỉ khoản chi đã xác nhận và thanh toán đã xác nhận tác động vào số dư.

Gợi ý chuyển tiền bù trừ giữa người nợ và người được nhận, duyệt theo định danh cố định và ghép số tiền nhỏ hơn của hai phía. Mục tiêu là gợi ý dễ thực hiện; không cam kết số giao dịch ít nhất theo toán học. Đây là bù trừ số dư nên người trả có thể chuyển cho người không trực tiếp ứng tiền cho họ ở một hóa đơn. Nếu nhóm chưa đồng ý bù trừ thì xem sổ chi tiết để đối soát; cần duyệt mặc định này trước production.

Ví dụ sáu người: An trả 900.000 đồng tiền lẩu, Minh trả 330.000 đồng cà phê, cả sáu cùng dùng. Mỗi người chịu 205.000 đồng. An còn nhận 695.000 đồng, Minh còn nhận 125.000 đồng, bốn người còn lại mỗi người cần trả 205.000 đồng. Tổng chi là 1.230.000 đồng và tổng số dư là 0.

### Ghi nhận thanh toán

Người gửi chọn người nhận và số tiền nguyên dương không vượt khoản còn cần thanh toán trong sổ hiện hành. Có thể trả một phần. App tạo trạng thái Chờ xác nhận; số dư chưa thay đổi. Người nhận bấm Đã nhận thì giao dịch mới có hiệu lực. Người nhận có thể từ chối hoặc người gửi có thể hủy khi đang chờ. Sau xác nhận, việc sửa sai đi qua giao dịch đảo được hai bên xác nhận và giữ dấu vết, không sửa tổng tiền âm thầm. Không tự chuyển tiền, không kết nối tài khoản ngân hàng và không dùng trạng thái đã quét QR như bằng chứng nhận tiền.

Nếu có thanh toán đang chờ trùng với gợi ý, không tạo thêm yêu cầu giống hệt. Nếu số dư đã đổi do khoản chi mới hoặc thanh toán khác, kiểm tra lại khi xác nhận để cảnh báo khả năng trả thừa. Mọi thao tác ghi nhận có idempotency key, version và kiểm tra người thực hiện ở máy chủ. Chủ phòng không có quyền giả danh người nhận.

### Dữ liệu bổ sung

Expense gồm id, roomId, title, currency VND, amount, payerMembershipId, createdBy, participantShares, splitMethod, approvalStatus, activityId tùy chọn, revision và thời gian. ExpenseShare có khóa expenseId và membershipId với số tiền nguyên. Settlement gồm id, roomId, fromMembershipId, toMembershipId, amount, status pending confirmed rejected cancelled reversed, confirmedBy, revision và thời gian. Lịch sử chỉnh sửa giữ tham chiếu bản gốc và lý do; balance là số tính từ ledger, không phải trường người dùng tự sửa.

### Tiêu chí chấp nhận bổ sung

| Mã | Tình huống | Kết quả cần đạt |
| --- | --- | --- |
| AC17 | Khoản 100 đồng chia ba người | Phần chia là 34 33 33 theo định danh cố định; tổng 100 |
| AC18 | Người không tham gia một khoản | Phần của họ bằng 0 |
| AC19 | Phần chia riêng không khớp tổng | Chặn lưu và chỉ ra số chênh lệch |
| AC20 | Người trả không nằm trong tập sử dụng | Vẫn ghi đúng đã ứng và người chịu tiền |
| AC21 | Dùng ví dụ 900.000 và 330.000 cho sáu người | Ra số dư như ví dụ; tổng 0 |
| AC22 | Báo đã chuyển nhưng chưa xác nhận nhận | Số dư chưa đổi |
| AC23 | Người nhận xác nhận hai lần hoặc gửi lại request | Tính đúng một lần |
| AC24 | Người khác hoặc chủ phòng xác nhận thay | Máy chủ từ chối |
| AC25 | Sửa khoản đã có thanh toán | Đi qua điều chỉnh và lịch sử, không xóa thẳng |
| AC26 | Tiền hoặc phần chia là số âm thập phân hoặc thiếu người | Chặn lưu rõ trường lỗi |
| AC27 | Khoản do người khác ghi hộ | Chờ người trả xác nhận trước khi tính sổ |

### Phạm vi minh họa trong mockup

Tab Chia tiền có tổng chi, tiền An đã trả, số dư, danh sách khoản, bảng ai cần trả ai và lịch sử xác nhận. Thử thêm khoản chia đều hoặc chia riêng cho một tập người, xem từng phần, mô phỏng người trả báo đã chuyển và để An xác nhận khoản gửi cho An. Đây là công cụ trình diễn nên nút mô phỏng báo chuyển có thể đóng vai người trả bất kỳ. Sản phẩm thật phải kiểm tra đúng danh tính.

Chưa minh họa sửa xóa chi, phê duyệt khoản ghi hộ, trả một phần, từ chối, hủy, đảo giao dịch, lịch sử revision và phân quyền backend. Lịch trình và đề xuất vẫn giữ các giới hạn mockup đã nêu.
