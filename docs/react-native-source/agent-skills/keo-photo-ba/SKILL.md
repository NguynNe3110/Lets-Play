---
name: keo-photo-ba
description: Viết hoặc cập nhật BA tính năng chụp khoảnh khắc, chia sẻ cá nhân, album phòng và tương tác theo ảnh của Kèo, với truy vết quyết định, quyền, API và nghiệm thu. Dùng khi làm rõ nghiệp vụ hoặc bàn giao backend; không dùng cho BA không liên quan ảnh.
---

# BA ảnh và album Kèo

Viết tiếng Việt để product, mobile, backend và QA dùng chung. Giữ định danh dữ liệu/API bằng tiếng Anh. Đầu ra phải mô tả hành vi xác định được: ai làm, điều kiện nào, server thay đổi gì, ai được xem, lỗi nào và kết quả retry/race. Không biến nhiệm vụ tài liệu thành triển khai tính năng.

## Nguồn cần đọc

Bắt đầu từ [mục lục BA](../../docs/ba/photo-memories/README.md) và [quyết định](../../docs/ba/photo-memories/08-decisions.md). Đọc source/README để xác minh hiện trạng; BA mới không có nghĩa tính năng đã chạy.

- Đổi trải nghiệm/phạm vi: [bối cảnh](../../docs/ba/photo-memories/01-context-and-scope.md), [FR và UC](../../docs/ba/photo-memories/02-business-requirements.md).
- Đổi quyền, người nhận, kết thúc hoặc xóa: [BR và lifecycle](../../docs/ba/photo-memories/03-permissions-and-lifecycle.md).
- Giao backend hoặc thay field/endpoint: [data](../../docs/ba/photo-memories/04-domain-data.md), [API](../../docs/ba/photo-memories/05-api-contract.md).
- Đổi đồng bộ/offline/notification: [event và NFR](../../docs/ba/photo-memories/06-events-notifications-nfr.md).
- Bổ sung hoặc đổi bất kỳ hành vi: cập nhật [AC và traceability](../../docs/ba/photo-memories/07-acceptance-and-delivery.md).

Chỉ đọc các phần chịu ảnh hưởng sau mục lục/quyết định; không copy lại toàn bộ đặc tả vào skill. Tham chiếu ảnh Locket tại `context/locket-images` là bằng chứng giao diện; không suy ra backend/ACL từ ảnh. Muốn khẳng định tính năng Locket hiện tại phải kiểm nguồn chính thức và ghi ngày.

## Các điểm không được làm lệch

Người dùng đã xác nhận C-06–08: đủ reaction/comment chung/DM riêng; camera từ phòng chọn sẵn album, cá nhân tùy chọn; host chọn VIEW_ONLY/OPEN/WINDOWED sau kết thúc. Chi tiết mặc định và deadline vẫn có phần đề xuất D-19. Không hỏi lại xác nhận này chỉ vì tiếp tục ở phiên mới.

Moment là ảnh gốc của owner; placement là nơi chia sẻ với ACL và tương tác riêng. Comment ảnh nhóm khác comment album và DM. Thư viện owner khác feed bạn bè. Không trộn quyền nhóm với friendship, không áp ẩn danh ý tưởng lên ảnh có tên, không làm thay số dư/phiếu khi xử lý ảnh.

## Khi viết hoặc sửa

Định vị yêu cầu mới là xác nhận, đề xuất hay phụ thuộc; giữ mã FR/BR/UC/AC đã có. Nếu người dùng chưa quyết chi tiết cần thiết, đưa phương án cụ thể và đánh dấu ĐỀ XUẤT; hỏi khi thiếu thông tin thật sự đổi kết quả, tiếp tục phần độc lập. Không tự ghi “đã thống nhất” cho lựa chọn của agent.

Cho mỗi luồng thay đổi, xác định success, invalid input, mất quyền, offline, retry và tác động xóa khi có liên quan. Với thời hạn dùng server time, nêu rõ mốc tính và dấu `<`/`>=`. Với audience nêu rõ người vào sau, người rời, block và revoke; với đồng thời nêu điểm kiểm tại commit. Viết lỗi/response cụ thể thay các câu “backend xử lý phù hợp”.

Cập nhật nguồn chuẩn cùng lúc: quyết định → FR/BR → data/API → event/NFR → AC. Không nhân bản policy thành nhiều mô tả trái nhau. Kiểm link tương đối, mã tham chiếu và status sau sửa. Báo rõ việc kiểm tài liệu và việc test app là hai kết quả khác nhau.
