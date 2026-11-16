# Yêu cầu chức năng và luồng nghiệp vụ

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](../product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Đọc [08](08-decisions.md) để phân biệt phạm vi đã xác nhận với chi tiết đề xuất. “Bạn bè” bên dưới là tài khoản có quan hệ bạn bè hai chiều đã được server xác nhận, không phải tên nhập vào phòng local.

## 1 Thuật ngữ người dùng và hệ thống

| Thuật ngữ | Nghĩa duy nhất trong đặc tả |
| --- | --- |
| Khoảnh khắc / Moment | Một ảnh gốc đã đăng thành công, có chủ sở hữu, caption và thời gian đăng |
| MediaAsset | File ảnh đã được upload và xử lý; chưa đồng nghĩa ảnh đã đăng |
| Nơi đăng / Placement | Một lần đưa Moment vào ngữ cảnh PERSONAL hoặc ROOM; có ID và tương tác riêng |
| Thư viện của tôi | Danh sách mọi Moment còn tồn tại do chính tôi sở hữu; chỉ mình tôi đọc được màn này |
| Feed bạn bè | Những nơi đăng PERSONAL mà tôi được cấp quyền; không tự trộn ảnh chỉ đăng nhóm |
| Dòng cá nhân | Các nơi đăng PERSONAL của một tác giả; kết quả tùy quyền người đang xem |
| Album phòng | Bộ sưu tập các nơi đăng ROOM của đúng một phòng; mỗi phòng một album |
| Bình luận ảnh | Comment công khai với thành viên có quyền xem ảnh, gắn vào ROOM placement |
| Bình luận album | Comment chung gắn album, không bắt buộc nhắc đến một ảnh |
| Trả lời riêng | Tin nhắn trong hội thoại hai người, có thể tham chiếu một placement; không phải comment |
| Chỉ mình tôi / OWNER | Nơi đăng PERSONAL không có người nhận ngoài tác giả |

Ví dụ: An chụp M1, chia sẻ cá nhân cho Bình và góp vào phòng có Chi. Bình thấy personal placement P1 và cảm xúc ở P1. Chi thấy room placement P2 và bình luận nhóm ở P2. Bình không thấy Chi hoặc phòng nếu chưa là thành viên; Chi không thấy danh sách bạn được An chia sẻ. An thấy M1 trong thư viện một lần.

## 2 Người dùng và mục tiêu

| Vai trò | Nhu cầu | Kết quả quan sát được |
| --- | --- | --- |
| Người chụp | Chụp nhanh và biết rõ ai nhận | Preview hiển thị tên nơi đăng; upload thành công không bị đăng nhầm phòng |
| Thành viên | Góp và xem ảnh buổi đi chơi | Album có ảnh của nhiều người, lọc được người chụp và thời gian |
| Bạn bè ngoài nhóm | Xem phần tác giả chia sẻ cho mình | Không biết thông tin nhóm hoặc comment nhóm ngoài quyền |
| Chủ phòng | Quản lý kỷ niệm và quyền đóng góp sau kết thúc | Chọn được ba chế độ; thay đổi áp dụng thống nhất ở server |
| Người nhận tin nhắn | Trả lời kín theo ảnh | Chỉ hai bên đọc thread; album không nhận bản sao câu trả lời |
| Người rời nhóm | Quản lý ảnh do mình sở hữu | Vẫn tìm và xóa được ảnh mình; không đọc album sau rời |

## 3 Danh mục yêu cầu

| Mã | Yêu cầu và kết quả bắt buộc của baseline | Ưu tiên |
| --- | --- | --- |
| FR-01 | Mở camera từ khu vực cá nhân hoặc từ phòng; giữ đúng ngữ cảnh cho composer | P0 |
| FR-02 | Xin quyền camera đúng lúc; chụp trước/sau, flash khi được hỗ trợ, chụp lại và hủy | P0 |
| FR-03 | Preview ảnh, nhập caption tùy chọn, hiển thị rõ nơi đăng và tập người nhận | P0 |
| FR-04 | Chụp từ phòng chọn sẵn album; cá nhân tùy chọn. Chụp ngoài phòng không tự chọn nhóm | P0 đã xác nhận |
| FR-05 | Chọn PERSONAL, ROOM hoặc cả hai; tất cả đích hợp lệ mới publish, không đăng một phần | P0 |
| FR-06 | Upload có tiến độ, xử lý lỗi/retry/cancel; chỉ báo Đã đăng sau commit publish | P0 |
| FR-07 | Feed bạn bè và dòng cá nhân chỉ trả ảnh có quyền; infinite pagination, lọc tác giả | P0 |
| FR-08 | Thư viện chủ sở hữu hiển thị ảnh cá nhân/nhóm một lần; có bộ lọc tháng | P0 |
| FR-09 | Album tự có khi phòng online tạo thành công; rỗng vẫn mở được; lưới và chi tiết | P0 |
| FR-10 | Thêm ảnh mới từ album hoặc đưa ảnh **của mình đã đăng trong Kèo** vào album khi được phép | P0 |
| FR-11 | Một reaction hiện tại mỗi người mỗi placement; đổi hoặc bỏ; trạng thái phản hồi đúng sau retry | P0 đã xác nhận phạm vi |
| FR-12 | Bình luận ảnh nhóm dạng phẳng, có tên; tự xóa; host gỡ; không áp ẩn danh ý tưởng | P0 đã xác nhận phạm vi |
| FR-13 | Bình luận chung album có thread riêng, không bị gộp comment ảnh hoặc DM | P0 đã xác nhận phạm vi |
| FR-14 | Trả lời riêng theo ảnh tạo/dùng hội thoại hai người; chỉ họ đọc được | P0 đã xác nhận phạm vi |
| FR-15 | Inbox, unread, gửi lại an toàn, đọc tin, thu hồi tin của mình; tham chiếu ảnh có kiểm quyền | P0 |
| FR-16 | Tác giả gỡ nơi đăng hoặc xóa Moment ở mọi nơi; host chỉ gỡ nơi đăng nhóm | P0 |
| FR-17 | Chủ phòng cấu hình sau kết thúc: VIEW_ONLY, OPEN, WINDOWED; server tính hạn | P0 đã xác nhận ba chế độ |
| FR-18 | Rời/bị loại, bạn mới, hủy kết bạn, chặn, xóa tài khoản và xóa phòng có tác động rõ | P0 |
| FR-19 | In-app notification, badge và deep link kiểm tra quyền; push theo lựa chọn user | P1 |
| FR-20 | Báo cáo ảnh/comment/tin nhắn và chặn người; moderator có audit | P0 |
| FR-21 | Đồng bộ thiết bị, refetch sau reconnect; cache ảnh không giữ quyền vĩnh viễn | P0 |
| FR-22 | Chụp offline lưu nháp, chưa đăng; kết nối lại không tự mở rộng tập người nhận | P0 |
| FR-23 | Album chỉ đọc vẫn xem được ảnh/comment còn quyền; hiển thị lý do khóa và thời hạn | P0 |
| FR-24 | Không làm lộ tác giả góp ý ẩn danh và không thay số dư chia tiền | P0 |

P0 là điều kiện nghiệm thu **nếu baseline tương ứng được duyệt**. P1 push không được dùng để thay thế tính nhất quán dữ liệu. Không biến nhãn ưu tiên này thành sự cho phép hoãn phạm vi người dùng đã xác nhận.

## 4 Các luồng chi tiết

### UC-01 Chụp và chuẩn bị đăng

Tiền điều kiện: có phiên online hợp lệ để đăng; người dùng được phép dùng app. Có thể chụp offline nếu trước đó đã đăng nhập. Khởi phát: chạm Chụp khoảnh khắc từ cá nhân hoặc phòng.

1. Nếu chưa hỏi quyền camera, app xin quyền. Nếu đã từ chối, giải thích và mở cài đặt theo thao tác người dùng. Không xin quyền microphone cho ảnh tĩnh.
2. Camera cá nhân hiển thị không có phòng được chọn. Camera từ phòng giữ `sourceRoomId` bằng ID, không dựa vào tên phòng.
3. Người dùng chụp; màn preview hiển thị ảnh đúng chiều và đúng crop sẽ đăng. Chụp lại thay file nháp, không tạo post.
4. Caption là tùy chọn. Nơi đăng hiển thị bằng nhãn rõ: “Album Kèo Đà Lạt”, “Chỉ mình tôi”, “Tất cả bạn bè hiện tại” hoặc danh sách đã chọn.
5. Từ phòng còn mở đóng góp, album được chọn sẵn; cá nhân tắt. Người dùng có thể bỏ album và bật cá nhân. Từ album chỉ đọc, không mở camera bằng CTA góp ảnh; nếu link cũ mở được camera thì preview báo khóa và yêu cầu chọn lại.
6. Người dùng chọn ít nhất một đích. Nếu đích PERSONAL bật, mặc định OWNER; người dùng chủ động chuyển sang ALL_FRIENDS_SNAPSHOT hoặc SELECTED_FRIENDS.
7. App hiển thị “Thành viên tham gia phòng sau này cũng có thể xem ảnh album” theo D-05. Không ghi “chỉ những người hiện tại” khi dùng ACL membership động.
8. Chạm Đăng tạo thao tác client có ID bền vững; chuyển UC-02. Hủy preview chưa đăng không gọi publish. Thoát khi có nháp hỏi giữ hoặc bỏ nháp; không tự upload ở nền.

Hậu điều kiện: có nháp hoặc thao tác upload; chưa có ai khác thấy ảnh. Trường hợp không có bạn bè: OWNER hoặc album vẫn dùng được; ALL với 0 bạn bị chặn có hướng dẫn chọn OWNER/thêm bạn.

### UC-02 Upload và đăng nguyên tử

1. App xin UploadSession rồi upload đúng file/checksum; hiển thị tiến độ byte.
2. App gọi hoàn tất upload, đợi media READY; chưa thêm vào feed/album như post thật. Thumbnail tạm nếu có phải mang nhãn Đang đăng và chỉ nằm ở UI local của người gửi.
3. Nếu chọn ALL_FRIENDS_SNAPSHOT, app xin audience preview từ server và hiển thị số người nhận. Preview đóng băng tập người nhận, không tự bao gồm bạn mới sau đó.
4. App gửi publish với media, caption và đích đã chọn, idempotency key. Server kiểm lại user, media, bạn bè, membership, album policy tại thời điểm commit.
5. Nếu tất cả hợp lệ, một transaction tạo Moment và toàn bộ placement/grant. Trả ID, revision và kết quả từng nơi đã commit. App mới báo Đã đăng và xóa nháp sau khi lưu được kết quả.
6. Event gửi bất đồng bộ sau commit; notification lỗi không làm mất bài.

Ngoại lệ: mất kết nối/timeout sau publish → tra theo `clientMutationId` hoặc retry cùng key, không tạo post mới. Album hết hạn/rời phòng/recipient không hợp lệ → toàn bộ publish không commit, giữ media READY và nháp, hiển thị đích lỗi. Người dùng **chủ động** chọn lại nơi đăng rồi tạo thao tác mới. Không tự chuyển ảnh thất bại của nhóm sang bạn bè.

### UC-03 Xem feed và thư viện

Feed cá nhân chỉ gồm PERSONAL placement có quyền, mới trước cũ sau theo thời gian tạo nơi đăng. Chạm ảnh mở chi tiết cùng placement ID, không thay bằng một placement khác của cùng Moment. Lưới/feed không hiển thị tổng đích, tên phòng hoặc reaction/comment của nơi khác.

Thư viện của tôi dùng Moment làm đơn vị nên ảnh đăng hai nơi chỉ có một ô. Khi mở quản lý, tác giả thấy những nơi đăng của chính mình và CTA gỡ từng nơi/xóa mọi nơi. Khi cần tương tác nhóm, tác giả mở đúng placement, và vẫn phải là thành viên có quyền. Có Moment không có placement hoạt động sau gỡ; nó vẫn ở thư viện cho đến khi chủ sở hữu xóa.

Kéo tải trang tiếp có cursor. Khi hết trang trả `nextCursor=null`; không gọi vô hạn. Mất quyền hoặc ảnh bị xóa giữa lúc xem thì chuyển trạng thái Không còn khả dụng, bỏ ảnh khỏi cache hiển thị và về danh sách hợp lệ.

### UC-04 Mở album và góp ảnh có sẵn

Tiền điều kiện: membership ACTIVE trong phòng chưa DELETED. Tab Album tồn tại từ lúc tạo phòng, không cần chốt lịch trước. Album rỗng có hướng dẫn “Chụp khoảnh khắc đầu tiên” nếu đang được đóng góp, hoặc “Album chưa có ảnh” nếu chỉ đọc.

Lưới sắp theo thời điểm ảnh được **thêm vào album**. Bộ lọc người chụp và tháng không tự mở rộng quyền. Bộ lọc tháng dùng tháng thêm vào album theo `room.timezone`. Phần chi tiết vẫn hiển thị thời gian chụp do thiết bị cung cấp với ý nghĩa tham khảo, nếu có.

Chọn “Thêm từ thư viện Kèo” chỉ liệt kê Moment do tôi sở hữu, còn tồn tại và chưa có ROOM placement hoạt động. Chọn ảnh → preview tên phòng → xác nhận; tạo ROOM placement mới, không upload lại và không chia sẻ cá nhân thêm. Nếu đã có nơi đăng nhóm, chỉ cho gỡ nơi cũ trước bằng thao tác riêng. Thêm lại sau gỡ tạo ID mới và tương tác mới, không khôi phục comment cũ.

Ảnh chỉ được người khác chia sẻ cho tôi không được đem vào phòng. Một thành viên không có quyền tự đăng lại ảnh người khác bằng API này.

### UC-05 Cảm xúc và bình luận chung

Trên chi tiết PERSONAL chỉ có reaction và Trả lời riêng. Trên ROOM có reaction, Bình luận trong nhóm, Trả lời riêng; nhãn phải phân biệt rõ. Bình luận album nằm trong tab/section Thảo luận album, không hiển thị như bình luận của một ảnh bất kỳ.

Người dùng chọn reaction từ enum ở 04. Chọn loại mới thay loại cũ; chạm loại đang chọn gửi DELETE để bỏ. App có thể cập nhật tạm nhưng phải rollback/refetch khi server từ chối. Danh sách và số lượng chỉ thuộc đúng placement và chỉ gồm tài khoản người xem được phép nhìn thấy.

Bình luận ảnh/album là text thuần, phẳng. App chỉ báo đã gửi sau server xác nhận; retry cùng thao tác không nhân bản. Người tạo được xóa của mình; host được gỡ trong nhóm với lý do. Không sửa text, không mention hoặc reply nhiều tầng ở V1. Bình luận trống, chỉ whitespace hoặc vượt độ dài bị chặn cả FE/BE.

Nếu album chỉ đọc do thiết lập sau kết thúc, hết hạn hoặc lưu trữ: vẫn đọc comment, reaction hiện hữu; khóa thêm comment và đặt/đổi reaction. Xóa comment của mình, bỏ reaction của mình, báo cáo và xóa ảnh vẫn được phép nhằm quản lý dữ liệu cá nhân.

### UC-06 Trả lời riêng và inbox

1. Từ một ảnh còn xem được, chọn “Trả lời riêng cho An”. App mở composer DM, hiển thị ảnh tham chiếu và nhãn “Chỉ bạn và An đọc được”.
2. Server kiểm hai người khác nhau, quan hệ liên lạc hợp lệ, không block và cả hai được xem source placement lúc gửi. Không nhắn cho chính mình.
3. Một cặp user chỉ có một conversation; mở lặp không tạo thread mới. Tin text có `referencePlacementId` tùy chọn; không copy ảnh thành attachment độc lập.
4. Tin xuất hiện trong inbox hai bên, không có event comment hoặc notification nhóm. Thread tiếp tục gửi text không tham chiếu khi đủ điều kiện liên lạc.
5. Đọc cập nhật read watermark theo sequence server; badge giảm đồng nhất trên thiết bị.

Nếu source bị xóa/gỡ/mất quyền, ô tham chiếu trở thành “Ảnh không còn khả dụng”; tin text đã gửi vẫn đọc được bởi hai bên theo retention đề xuất. Thread không trở thành đường vòng xem ảnh nhóm sau rời. Nếu không còn bạn bè hay phòng chung hợp lệ, lịch sử text vẫn đọc được nhưng composer khóa. Block khóa gửi cả hai chiều; thu hồi tin của mình vẫn được phép. Host không có quyền đọc DM.

### UC-07 Thiết lập album sau kết thúc

Phạm vi ba chế độ và chủ phòng thiết lập đã được người dùng xác nhận. Các chi tiết dưới là baseline D-19.

1. Host mở Thiết lập album. App hiển thị lựa chọn đang lưu và revision: “Chỉ xem lại”, “Tiếp tục đóng góp”, “Cho đóng góp có thời hạn”.
2. WINDOWED yêu cầu số giờ nguyên 1–720; gợi ý 72 giờ. Đây là thời gian tính **từ lúc kèo kết thúc do server ghi nhận**, không phải từ lần mở màn hay lần sửa thiết lập.
3. Trước kết thúc, hiển thị “Cho đóng góp 72 giờ sau khi kèo kết thúc”. Sau kết thúc, hiển thị deadline đầy đủ với múi giờ, ví dụ “Đến 20:00 ngày 13/11/2026, GMT+7”.
4. Host được đổi policy khi phòng ACTIVE/ENDED; không đổi khi ARCHIVED/DELETED. Sau kết thúc, thay policy có hiệu lực ngay. Nếu deadline mới đã qua, preview nói “Album sẽ chuyển sang chỉ xem ngay”.
5. Host lưu kèm If-Match; server tính deadline/effective mode, commit revision và phát event. Thành viên không được tự gửi patch này.
6. Trước nút Kết thúc kèo, app hiển thị policy để host kiểm tra. Kết thúc không tự quyết toán tiền hoặc xóa ảnh; chỉ host và đã có lịch công bố được kết thúc. Phase không khóa đề xuất/vote theo [luồng mới](../continuous-planning.md).

Quy tắc biên: với `now == deadline`, đóng góp bị từ chối. Upload bắt đầu trước hạn nhưng publish sau hạn vẫn bị từ chối; không có ưu tiên vì đã bấm trước đó. Host mở lại rồi kết thúc lần nữa tạo endedAt mới, có xác nhận rõ hạn sẽ tính lại; lịch sử thay đổi được giữ.

### UC-08 Gỡ nơi đăng và xóa khoảnh khắc

Tác giả mở quản lý ảnh. “Gỡ khỏi album Kèo Đà Lạt” chỉ vô hiệu nơi nhóm và comment/reaction của nơi đó; “Ngừng chia sẻ cá nhân” chỉ gỡ nơi cá nhân; “Xóa ảnh ở mọi nơi” xóa Moment, mọi nơi đăng và quyền media. Xác nhận cuối phải gọi đúng tên hành động và phạm vi, không dùng một nút “Xóa” chung.

Host trên ảnh của người khác có “Gỡ khỏi album” kèm lý do; không có “Xóa ảnh ở mọi nơi”. Gỡ cuối cùng không tự xóa ảnh trong thư viện tác giả. Sau xóa/gỡ, retry cùng yêu cầu trả trạng thái đã xóa, không tạo lỗi giả khiến app hỏi người dùng đăng lại.

### UC-09 Rời phòng và thay đổi quan hệ

Khi rời hoặc bị loại, server thu hồi album, comment/reaction và media quyền nhóm; ảnh đã góp còn cho nhóm. Thông báo trước khi tự rời giải thích cả hai điều và dẫn về thư viện để người dùng tự xóa ảnh nếu muốn. Host muốn rời phải chuyển host thành công hoặc xử lý xóa phòng qua luồng riêng; không để phòng không có host ngoài quy trình đã định.

Thành viên mới ACTIVE xem lịch sử album hiện còn, không tự thành bạn bè với người trong phòng. Nếu chỉ biết mã mời mà chưa join thành công, không xem được thumbnail, cover hay comment. Hủy kết bạn chỉ cắt quyền cá nhân; nếu vẫn chung phòng thì quyền nhóm vẫn xét độc lập. Block áp dụng như 03.

### UC-10 Báo cáo và xử lý nội dung

Người có quyền đọc nội dung hiện tại được báo cáo ảnh/comment/tin nhắn, chọn reason và ghi mô tả tùy chọn. Report không đăng công khai, không gửi nội dung DM cho host. Queue moderation giữ bằng chứng theo retention riêng; đối tượng bị báo cáo không biết danh tính người báo cáo qua API.

Moderator có thể hide Moment, gỡ placement hoặc thu hồi message theo phạm vi; audit bắt buộc. Đây là quyền vận hành riêng, không cấp cho chủ phòng. Chưa chọn cơ chế tự động phân tích ảnh; không hứa có AI moderation chỉ vì Locket có.

## 5 Màn hình và trạng thái bắt buộc

| Mã | Màn hoặc khu vực | Dữ liệu và hành động | Trạng thái cần thiết kế |
| --- | --- | --- | --- |
| S-01 | Camera cá nhân/phòng | Context, flash, đổi camera, chụp | Chưa cấp quyền, từ chối vĩnh viễn, không có camera, app background |
| S-02 | Preview và nơi đăng | Ảnh/crop, caption, toggle cá nhân, chọn phòng và audience | Đích rỗng, bạn bè rỗng, phòng khóa, thành viên mới, offline |
| S-03 | Tiến trình đăng và nháp | Upload, processing, retry, cancel | Upload lỗi, xử lý lỗi, timeout không rõ commit, policy đổi |
| S-04 | Feed bạn bè | Danh sách placement, lọc người, mở chi tiết | Rỗng, trang tiếp lỗi, session hết, ảnh bị thu hồi |
| S-05 | Thư viện của tôi | Moment grid, tháng, quản lý nơi đăng | Ảnh chỉ nhóm, đã rời phòng, không còn nơi đăng |
| S-06 | Album phòng | Tên phòng, cover có quyền, count, grid, người góp | Rỗng, READ_ONLY, WINDOWED, ARCHIVED, mất membership |
| S-07 | Chi tiết ảnh nhóm | Tác giả, thời gian, caption, reaction, comment, reply riêng | Xóa/gỡ, người bị block, comment bị xóa, đã hết hạn |
| S-08 | Thảo luận album | Thread comment chung | Không comment, gửi lỗi, chỉ đọc, host gỡ |
| S-09 | Chi tiết ảnh cá nhân | Caption, reaction, reply riêng | Không có quyền; không hiện nhóm và comment nhóm |
| S-10 | Inbox | Thread, last text an toàn, unread | Rỗng, kết nối lại, ảnh tham chiếu mất quyền |
| S-11 | Hội thoại | Text, ảnh tham chiếu, read watermark, thu hồi | Bị block, không còn quan hệ, mất quyền source, retry |
| S-12 | Thiết lập album | Ba chế độ, số giờ, deadline và tác động | Không phải host, version conflict, deadline quá khứ |
| S-13 | Gỡ và xóa | Tên hành động, danh sách nơi bị ảnh hưởng dành cho owner | Nhấn lặp, offline, đang xóa, hoàn thành |
| S-14 | Báo cáo và chặn | Reason, mô tả, xác nhận | Report thành công; quyền đã đổi; gửi lỗi |

CTA dựa trên `capabilities` server và `serverTime`; không tự suy ra từ phase hoặc local role. Camera và các màn ảnh vẫn dùng token/khả năng truy cập hiện tại của app, vùng chạm tối thiểu 48 dp và nhãn cho screen reader. Kết thúc/đổi policy không tự điều hướng người đang xem ra khỏi album vì quyền đọc vẫn còn.

## 6 Validation và thông báo

| Input hoặc tình huống | Quy tắc server | Thông điệp gợi ý |
| --- | --- | --- |
| Caption | Tối đa 280 Unicode code points sau trim/NFC; có thể rỗng | “Chú thích tối đa 280 ký tự” |
| Comment | 1–1.000 code points sau trim/NFC; text thuần | “Nhập bình luận từ 1 đến 1.000 ký tự” |
| DM | 1–2.000 code points sau trim/NFC; text thuần | “Tin nhắn tối đa 2.000 ký tự” |
| Đích | Ít nhất một; tối đa một PERSONAL và một ROOM đang hoạt động | “Chọn nơi bạn muốn đăng ảnh” |
| Selected friends | 1–200 user khác mình; không trùng; đang là bạn và không bị block | “Danh sách người nhận đã thay đổi. Hãy chọn lại” |
| ALL preview | Snapshot còn hạn 10 phút và thuộc user hiện tại | “Xác nhận lại danh sách bạn bè trước khi đăng” |
| WINDOWED | `windowHours` là số nguyên 1–720; hai mode khác bắt buộc null | “Chọn thời hạn từ 1 đến 720 giờ” |
| Membership mất | Không publish hoặc truy cập phòng qua nguồn nhóm | “Bạn không còn quyền truy cập album này” |
| Hết hạn đóng góp | Server now lớn hơn hoặc bằng deadline | “Album đã hết thời gian đóng góp. Ảnh vẫn ở bản nháp” |
| Đăng chưa rõ kết quả | Giữ mutation ID, kiểm tra lại | “Đang kiểm tra trạng thái đăng ảnh” |
| Đích đã đổi | Không tự chọn đích thay thế | “Nơi đăng không còn hợp lệ. Kiểm tra lại trước khi gửi” |

Ký tự được tính theo Unicode code point sau chuẩn hóa, không phải byte hoặc JavaScript UTF-16 length. Emoji ghép có thể gồm nhiều code points; FE/BE phải dùng cùng quy ước. Không lọc dấu tiếng Việt, không render HTML từ text người dùng.
