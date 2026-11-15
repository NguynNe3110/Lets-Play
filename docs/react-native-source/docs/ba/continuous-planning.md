# Đề xuất và bình chọn xuyên suốt kèo

> Cập nhật xác nhận 11/11/2026: đọc [delta BA PC-01–14, model/API, event và AC](product-rules-2026-11-11.md) trước các baseline bên dưới. Trong phạm vi được liệt kê, delta thay quy tắc cũ mâu thuẫn về account, quyền host sửa ý, lịch đã chốt, nhập ảnh máy, lịch sử cho người mới, block trong album, duyệt tiền, former-member và đóng/lưu lịch sử phòng. Không suy ra endpoint đã có hoặc AC đã pass; các phần không bị thay thế vẫn giữ trạng thái xác nhận/đề xuất hiện có.

Cập nhật ngày 10/11/2026 theo yêu cầu trực tiếp của chủ sản phẩm: trong lúc đi chơi mọi người vẫn có thể nhớ ra điều muốn làm, thêm đề xuất và bình chọn nếu cần. Tài liệu này **thay các ràng buộc một chiều** Góp ý → Bình chọn → Sắp lịch → Đã chốt trong [BA cũ](../../context/BA.md). Chỉ prototype `prototypes/keo-full` đã được cập nhật; source React Native và backend ngoài repo chưa triển khai thay đổi này.

## Quy tắc được cập nhật

- Khi phòng `ACTIVE`, mọi thành viên hiện tại được thêm ý và đặt/đổi phiếu 2/1/0 cho từng ý, kể cả sau khi lịch đã chốt hoặc đang thực hiện. Phiếu lưu ngay; không phải đợi host mở vòng. Chưa chọn khác 0. Đánh dấu đã xem hết là tùy chọn và không là điều kiện chốt lịch.
- Góp ý, bình chọn và lịch là các khu vực truy cập song song. `planningPhase` cũ có thể giữ để đọc dữ liệu/client cũ nhưng không được dùng làm cổng quyền thêm ý hoặc bình chọn.
- Thêm ý hoặc đổi phiếu không thay lịch đang đi, completion, phiếu các ý khác hay phiên bản lịch. Danh sách ổn định, không nhảy thứ tự theo điểm khi đang chạm.
- Chủ phòng vẫn chọn hoạt động và xác nhận lịch chung. Shortlist dự kiến và phiếu thứ tự Borda độc lập với lịch đã công bố. Đổi shortlist yêu cầu gửi lại phiếu thứ tự; giữ phiếu 2/1/0 và lịch đã công bố.
- Chốt bản cập nhật lưu snapshot phiên bản trước, thời gian và nội dung các chặng. Chặng giữ lại được giữ trạng thái hoàn thành; chặng mới chưa hoàn thành. Chỉ bản cập nhật thành công mới thay lịch chung.
- Kết thúc/lưu trữ phòng khóa thao tác kế hoạch; xem lịch và lịch sử vẫn theo quyền thành viên. Chính sách album sau kết thúc và sổ tiền giữ quy tắc riêng.

Chi tiết prototype để review trước triển khai online: hiển thị tổng từng mức, không hiện danh tính người bỏ phiếu; tác giả chỉ sửa/xóa ý của mình chưa nằm trong lịch đang đi; sửa nội dung làm mất hiệu lực phiếu của riêng ý đó. Ý đang trong lịch không sửa trực tiếp để tránh đổi lịch âm thầm; thành viên thêm ý thay thế rồi host cập nhật lịch.

## Model và hợp đồng online cần cập nhật

Đây là delta hợp đồng đề xuất, không phải endpoint backend đã tồn tại.

| Thành phần | Yêu cầu |
| --- | --- |
| Room/capabilities | Quyền dựa trên membership và `lifecycle=ACTIVE`; không dựa trên `COLLECTING/VOTING/FINALIZED`. Công bố `currentPlanVersion` và `shortlistRevision` độc lập |
| Proposal | Stable ID, owner nội bộ, `contentRevision`; không lộ tác giả cho người khác. Khi sửa ý chưa vào lịch, báo rõ các phiếu cũ bị vô hiệu |
| Vote | Upsert 2/1/0 cho actor/proposal/contentRevision; không toggle, không bắt buộc hoàn tất toàn bộ để chốt lịch. Server xác minh membership |
| Shortlist / OrderBallot | Shortlist có revision; phiếu đủ/duy nhất, gắn đúng revision. Đổi shortlist vô hiệu phiếu thứ tự cũ, không sửa current plan |
| PlanVersion | Snapshot order, nội dung, giờ, người chốt và lý do nếu khác gợi ý. Publish có expected current version + shortlist revision, idempotency key; conflict giữ lịch hiện hành |
| Completion | Gắn chặng và phiên bản; chuyển tiếp completion chặng còn giữ. Không reset chỉ vì thêm ý hoặc mở phần Sắp lịch |
| Lifecycle | Host kết thúc khi có lịch đã công bố; không phụ thuộc màn hình đang mở hoặc phase cũ. Album deadline vẫn dùng server endedAt |

Nhóm command cần hỗ trợ: tạo/sửa/xóa proposal khi ACTIVE, đặt vote khi ACTIVE, cập nhật shortlist, gửi order ballot và publish plan version. Không tự đổi URL hay payload backend hiện có. Backend cần review tương thích trước triển khai.

## Event và đồng bộ

Đề xuất `proposal.created/updated/deleted`, `proposal.vote.updated`, `shortlist.updated`, `order-ballot.updated`, `plan.version-published`, `plan.completion-updated`, `room.lifecycle-changed`. Event có room/revision, chỉ gửi cho người còn quyền. Ý mới thêm thẻ và tiến độ chưa trả lời; vote cập nhật thống kê; chỉ `plan.version-published` thay lịch chung. Sửa proposal vô hiệu đúng content revision, không xóa toàn bộ phiếu phòng. Replay/idempotency không tạo thêm bản lịch.

## Nghiệm thu

| Mã | Kịch bản | Kết quả cần có |
| --- | --- | --- |
| CP-01 | Thành viên thêm ý trước/sau chốt hoặc đang thực hiện | Thành công, bình chọn được ngay |
| CP-02 | Đổi phiếu nhiều lần khi đang đi | Giá trị mới nhất, không cộng phiếu, không đổi lịch |
| CP-03 | Thêm ý sau khi hoàn thành một chặng | Giữ lịch, phiên bản, completion và phiếu các ý cũ |
| CP-04 | Host sửa shortlist rồi thành viên gửi thứ tự | Giữ lịch đang đi cho đến publish |
| CP-05 | Host publish lịch mới | Snapshot cũ, tăng version, thêm chặng mới, giữ completion chặng còn giữ |
| CP-06 | Thành viên publish, order trùng/thiếu hoặc giờ sai | Từ chối, không ghi lịch sử hoặc đổi current version |
| CP-07 | Sửa ý chưa vào lịch / sửa hoặc xóa ý đang đi | Reset đúng phiếu của ý đã sửa; không sửa/xóa trực tiếp chặng đang đi |
| CP-08 | ENDED/ARCHIVED hoặc người ngoài phòng gửi command | Từ chối góp ý/vote/shortlist/publish |
| CP-09 | Retry publish / command dùng version cũ | Backend dedup hoặc conflict, không ghi đè lịch mới hơn |

CP-01–08 có test tương ứng trong prototype, không chứng minh server authorization. CP-09 cần backend/đa thiết bị và chưa được kiểm chứng. [Review backend](../integration/backend-gap-review.md) cần đối chiếu thêm delta này; bản review cũ mô tả snapshot Swagger và BA trước yêu cầu mới.
