# Biên bản xác nhận ý tưởng sản phẩm — 11/11/2026

Nguồn: câu trả lời trực tiếp của chủ sản phẩm trong phiên rà soát BA. Đây là biên bản phỏng vấn, không phải bằng chứng implementation. Xem [delta đặc tả](product-rules-2026-11-11.md) cho quy tắc thay thế, tác động model/API/event và AC cần triển khai. Chưa có source backend hoặc kiểm thử runtime chứng minh các yêu cầu mới.

## Phòng và kế hoạch đã xác nhận

- Một phòng đại diện một buổi/chuyến đi; chuyến mới tạo phòng mới.
- Bắt buộc đăng nhập. Host chọn tham gia ngay bằng lời mời hoặc chờ duyệt; được khóa/đổi mã mời.
- Host chuyển quyền, người nhận đồng ý rồi host mới rời. Trường hợp chỉ còn một người cần đặc tả riêng.
- Thành viên mới xem lịch sử đề xuất, lịch, sổ chi; không tự chịu khoản cũ, không xem DM người khác. Quyền lịch sử ảnh có chính sách riêng bên dưới.
- Đề xuất và danh tính người vote ẩn với nhóm, kể cả host; chỉ công khai thống kê phiếu. Tổng từng mức hiển thị ngay.
- Phiếu tham khảo; host chọn nhiều hoạt động và chốt. Mọi người xếp đủ thứ tự, tổng hợp Borda n-1 đến 0; hòa giữ thứ tự shortlist ban đầu. Host có thể điều chỉnh.
- Host được chốt khi chưa đủ phản hồi nhưng phải thấy số người chưa phản hồi.
- Trong lúc đi vẫn thêm đề xuất/vote; host cập nhật phần lịch còn lại, giữ chặng hoàn thành và lịch sử.
- Thành viên sửa ý của mình ở màn Đề xuất khi chưa thuộc lịch chốt. Host sửa ý của bất kỳ ai chưa thuộc lịch chốt ở màn Vote, không tiết lộ tác giả. Màn Sắp lịch không sửa nội dung.
- Khóa nội dung khi chốt thành công, không phải khi vào bản nháp Sắp lịch. Sau chốt, nội dung bị khóa ở mọi màn; muốn thay phải tạo đề xuất mới. Được quay từ nháp về Vote để sửa.
- Sửa nội dung vô hiệu phiếu riêng ý đó, thông báo nhóm về đề xuất và phần nội dung đã sửa, yêu cầu vote lại; không lộ tác giả.
- Host được đổi giờ/thứ tự qua bản lịch mới, giữ lịch sử và completion.
- Mọi thành viên đánh dấu hoàn thành; chỉ host đánh dấu bỏ qua; cho hoàn tác, lưu actor và lịch sử.

## Ảnh và quan hệ đã xác nhận

- Hỗ trợ chụp trong app và nhập từ thư viện điện thoại; thay đề xuất chỉ camera trong app trước đây.
- Giữ xác nhận trước: chụp từ phòng chọn sẵn album, cá nhân tùy chọn; reaction, bình luận chung và trả lời riêng; host chọn ba chế độ đóng góp album sau kết thúc.
- Xác nhận cuối 1B: mỗi bài ảnh cá nhân chọn tất cả bạn bè hoặc một số bạn cụ thể; không public. Ảnh chỉ đăng album không tự chia cho bạn ngoài phòng.
- Làm rõ mới nhất của chủ sản phẩm thay cách hiểu cửa sổ trượt đã ghi sai trước đó: chính sách 30 ngày CHỈ giới hạn lịch sử được mở khi kết bạn mới, không làm ảnh hết quyền xem sau 30 ngày. Hai chế độ: không mở ảnh trước kết bạn, hoặc mở ảnh trong 30 ngày trước mốc kết bạn. Mốc kết bạn cố định cho lần quan hệ đó; ảnh được cấp không mất quyền chỉ vì già thêm. Ảnh sau kết bạn xem theo quyền chia sẻ bình thường; vẫn chịu thu hồi khi gỡ ảnh, hủy bạn, block hoặc hành động quyền hợp lệ khác.
- Ví dụ kết bạn ngày 1/12 và cho xem lịch sử 30 ngày: mở ảnh hợp lệ trong 30 ngày trước 1/12; ngày 1/1 không tự thu hồi ảnh đã mở vì qua 30 ngày. Không mở ảnh cũ hơn ranh giới lịch sử. Xác nhận trước chọn ngày đăng làm timestamp ảnh; cần mô tả timestamp publish server và kiểm tra chính xác biên 30 ngày trong đặc tả, không dùng EXIF làm quyền.
- Đổi chính sách cá nhân chỉ áp dụng người kết bạn sau thay đổi; bạn hiện tại giữ chính sách cũ.
- Hủy bạn thu hồi quyền cá nhân ngay; kết bạn lại xét như bạn mới theo chính sách hiện tại. Đây là điều chỉnh lựa chọn ban đầu không tự phục hồi ảnh cũ.
- Album phòng có ba chế độ xem lịch sử: từ lúc tham gia / 30 ngày trước lúc tham gia / toàn bộ. Host thiết lập; đổi chỉ áp dụng thành viên tham gia sau đó. Mặc định toàn bộ lịch sử. Xác nhận cuối 2A: 30 ngày là lookback tại mốc tham gia, không trượt theo thời gian và không thu hồi ảnh chỉ vì quá 30 ngày tuổi.
- Xác nhận cuối 3A: DM giữa bạn bè hoặc hai người có membership hiện hành trong cùng phòng; tham chiếu ảnh phải còn quyền cho cả hai.
- Xác nhận cuối 4A: block ngăn DM và ảnh cá nhân hai chiều, nhưng ảnh/bình luận trong album chung vẫn thấy theo quyền phòng. Không áp block để ẩn công nợ hoặc xóa nghĩa vụ đối soát. Thay baseline D-14 cũ ẩn nội dung trong album.
- Sau đăng khóa cả file ảnh và chú thích; muốn đổi thì xóa và đăng lại, không sửa trực tiếp.
- Quyền xem lịch sử độc lập quyền đóng góp sau kết thúc.
- Rời/bị xóa khỏi phòng mất album; ảnh đã góp giữ cho nhóm; owner xem/xóa ảnh mình qua thư viện. Quyền tiền còn lại độc lập.

## Tiền đã xác nhận

- Nhiều payer cho một khoản. Chia đều hoặc nhập số tiền riêng. Người không dùng không chịu phần chia; payer không bắt buộc là người dùng.
- Ai cũng ghi được; tự ghi tiền mình ứng tính ngay; ghi hộ chờ payer xác nhận; nhiều payer chờ tất cả xác nhận trước khi tính khoản.
- Quyền sửa trước thanh toán: host sửa ngay không cần duyệt. Thành viên sửa bản ghi của mình nhưng thay đổi phần tiền người ứng khác cần host hoặc người ứng đó xác nhận. Sửa bản ghi người khác cần host hoặc payer duyệt. Quy tắc chi tiết nhiều payer/phần share cần hoàn thiện.
- Xác nhận bổ sung 2B: thành viên sửa tăng phần tiền người khác phải chịu cần người đó hoặc host duyệt, kể cả người sửa là creator và payer. Giữ quyền host sửa ngay trước thanh toán. Cần phản ánh cả payer và người chịu share trong model approval.
- Khoản đã có thanh toán liên quan: kể cả host phải tạo điều chỉnh có lý do, giữ bản gốc/lịch sử; những người bị thay đổi phần tiền phải đồng ý trước khi đổi số dư. Quyền host sửa ngay chỉ áp dụng trước thanh toán liên quan.
- Người gửi báo đã chuyển -> pending chưa đổi số dư -> đúng receiver xác nhận mới có hiệu lực. Trả một phần; receiver từ chối; sender hủy khi pending. App ghi sổ, không chuyển tiền thật.
- Sửa xác nhận nhận tiền nhầm: một bên yêu cầu, bên kia đồng ý đảo; hoặc host đảo ngay kèm lý do và thông báo hai bên, giữ giao dịch gốc. Không thực hiện hoàn tiền ngân hàng.
- Kết thúc chuyến đi vẫn ghi thêm chi phí và thanh toán; không tự khóa sổ tiền.
- Người còn nợ/được nhận vẫn được rời/bị xóa; giữ công nợ và quyền giới hạn để xem/đối soát/xác nhận tiền của mình, không cấp lại album/kế hoạch.
- Không xóa phòng khi còn công nợ hoặc yêu cầu tiền pending. Khi được xóa vẫn giữ toàn bộ lịch sử bản ghi. Xác nhận cuối: thành viên đọc lại lịch, đề xuất, album và sổ tiền theo quyền đã có; không tạo nội dung mới. Cần mô tả thao tác này là đóng và lưu lịch sử, không dùng semantics DELETED thu hồi toàn bộ album như baseline cũ. Người đã rời không tự lấy lại quyền album. Chưa chốt retention; không suy ra lưu vĩnh viễn.
- Xác nhận cuối 5A: bản đầu không tự hết hạn lịch sử; sau đóng owner vẫn được xóa ảnh mình, lịch sử tiền không bị xóa theo. Đây không phải cam kết hạ tầng lưu vĩnh viễn hoặc override yêu cầu xóa dữ liệu hợp lệ.
- Xác nhận cuối 6A: người rời/bị xóa chỉ đọc khoản chi mình ứng/chịu, giao dịch của mình và thông tin cần thiết để đối soát; không được đọc toàn sổ hoặc khoản mới không liên quan. Quyền xác nhận tiền của mình giữ riêng khỏi membership album/kế hoạch.

## Thông báo đã xác nhận

- Lưu thông báo trong app và gửi push nếu người dùng cho phép. Tắt push không làm mất lịch sử thông báo trong app.
- Phạm vi đã hỏi và duyệt: sửa đề xuất phải vote lại, chốt/cập nhật lịch, yêu cầu duyệt tiền, host sửa/đảo giao dịch. Event không tiết lộ tác giả đề xuất ẩn danh.

## Câu còn mở và công việc hợp nhất

- Timestamp theo từng nơi đăng khi chia sẻ ảnh có sẵn và cách người dùng nhận biết ảnh cũ được chia lại. Chế độ 30 ngày cả cá nhân và album đã xác nhận là lookback, không phải TTL.
- Quyền owner, host và người thứ ba trong mọi chế độ; chính sách cá nhân theo từng lần kết bạn và thu hồi cần model/API/event/AC mới.
- Lỗi publish nhiều đích và chi tiết contract tích hợp cần review backend; giữ các bất biến đã có về publish nguyên tử và ACL riêng từng nơi đăng.
- Số người duyệt khoản nhiều payer/share, người rời phòng và giao dịch đang chờ khi điều chỉnh; không bỏ xác nhận 2B về tăng share.
- Không tự hết hạn lịch sử trong bản đầu; cần thiết kế dung lượng/backup và quy trình xóa tài khoản riêng, không tự coi là cam kết lưu vĩnh viễn.
- Các yêu cầu thông báo phải có notification model, audience, deep link, retry và AC; in-app + push theo permission đã xác nhận.
- Hợp nhất context/BA.md, continuous-planning, photo-memories 02–08, integration mapping/GAP và AC. Biên bản này chưa thay cho bước đó; không đánh dấu AC pass.
