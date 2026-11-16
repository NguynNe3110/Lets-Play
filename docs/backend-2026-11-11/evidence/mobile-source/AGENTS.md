# Hướng dẫn agent cho Kèo

## Hiện trạng cần giữ đúng

Đây là repo mobile React Native Let’s Play, module chính ở `src/modules/keo`. Đọc `README.md`, `package.json` và source trước khi mô tả tính năng đang có. Source mobile hiện tại lưu phòng/phiếu/sổ tiền local bằng MMKV, chưa có adapter kết nối backend. Tuy nhiên người dùng đã cung cấp backend ngoài repo có Swagger account/room/tiền/SSE và website Vercel. Phân biệt contract ngoài repo với implementation mobile; đọc [review tích hợp](../../docs/integration/backend-gap-review.md). Dependencies camera không chứng minh camera native đã chạy.

Không dùng tên thành viên, `members[]`, `currentUserIndex`, local role hoặc mã phòng làm danh tính/quyền online. Giữ khả năng đọc dữ liệu local hiện có; không sửa migration, module tiền/phiếu hoặc cấu hình native khi nhiệm vụ chỉ yêu cầu tài liệu.

## Tài liệu và skill theo nhiệm vụ

- Với yêu cầu BA, phạm vi, luồng hoặc chỉnh yêu cầu ảnh/album: đọc [skill BA](../../agent-skills/keo-photo-ba/SKILL.md), rồi tài liệu liên quan trong [bộ BA ảnh](../../docs/ba/photo-memories/README.md).
- Với triển khai/review camera, feed, album, API, quyền hoặc DM theo ảnh: đọc [skill tích hợp](../../agent-skills/keo-photo-integration/SKILL.md) và các file chuẩn mà skill chỉ tới.
- Với backend hiện có, Swagger hoặc giao việc AI backend: đọc [skill backend contract](../../agent-skills/keo-backend-contract/SKILL.md), [GAP review](../../docs/integration/backend-gap-review.md) và [hướng dẫn gói bàn giao](../../docs/integration/backend-ai-start.md).
- Với góp ý, bình chọn và chia tiền: [BA cũ](../../context/BA.md) là tham chiếu nghiệp vụ; source/README phản ánh mức hoàn thiện thực tế. BA cũ có một số giả định đã cũ, như chưa chọn framework hoặc album để sau.

Các skill nằm trong repo và được hướng dẫn này định tuyến; không giả định chúng đã được cài vào thư mục skill toàn cục hoặc tự discovery trên mọi harness. Khi cần, mở trực tiếp đường dẫn `SKILL.md`. `.agent-bootstrap/` là bộ mẫu tham khảo; không tự chạy bootstrap hoặc thêm hooks/CI vì nhìn thấy thư mục này.

## Quy tắc khi dùng BA ảnh

Đọc [quyết định](../../docs/ba/photo-memories/08-decisions.md) để phân biệt ĐÃ XÁC NHẬN và ĐỀ XUẤT. Người dùng đã xác nhận: chụp từ phòng chọn sẵn album, dòng cá nhân tùy chọn; có cảm xúc, bình luận chung và trả lời riêng; host chọn ba chế độ đóng góp sau kết thúc. Không hỏi lại các lựa chọn đã xác nhận nếu không có mâu thuẫn mới. Không coi những đề xuất khác là đã duyệt.

Giữ ranh giới Moment / Placement / AlbumComment / Message. Cùng một media không có nghĩa cùng ACL hoặc tương tác. Dùng server time cho hạn, server membership cho quyền. Bất kỳ đổi phạm vi nào phải sửa đồng bộ BA, DTO/API, event và AC; ghi rõ điều đã implement và điều chỉ mới đề xuất.

## Kiểm chứng theo thay đổi

Sửa tài liệu thì kiểm link, ID và nhất quán nghiệp vụ. Sửa code thì chọn các kiểm tra phù hợp từ scripts thực tế: `yarn typecheck`, `yarn test --runInBand`, `yarn lint`. Native camera/media cần kiểm Android/iOS liên quan; không khẳng định camera hay quyền liên thiết bị hoạt động nếu chỉ chạy unit test. Ghi lệnh/kết quả thật và phần chưa kiểm. Không đánh dấu các AC trong BA là pass chỉ vì đã viết case.
