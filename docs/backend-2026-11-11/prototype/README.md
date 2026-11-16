# Prototype Kèo cho điện thoại

Mở [dist/index.html](dist/index.html). Trên máy tính chọn viewport **375 × 812, 390 × 844 hoặc 430 × 932**; toàn bộ khung thu phóng theo không gian có sẵn, giữ nguyên tỉ lệ và kích thước layout bên trong. Mở thẳng `app.html` trên máy tính cũng tự vào preview có viền điện thoại. Trên điện thoại vào `app.html` toàn màn hình. Không cần cài dependency để xem; toàn bộ HTML/CSS/JS và ảnh SVG minh họa trong dist, không gọi backend thật.

Giữ bố cục điện thoại tham chiếu `context/locket-images/`, dùng lại **theme xanh–kem** của app (`#245C49`, `#F7F6F2`, `#EAF1E9`): ảnh vuông bo lớn, shutter xanh và bottom nav nổi. Bốn tab: **Home** để chụp (Lịch sử ảnh bên trong), **Kèo**, **Tin nhắn**, **Thời gian** với lịch/lưới ảnh riêng. Kế hoạch, hẹn giờ, chia tiền, album và thành viên ở trong từng kèo.

Sidebar **Toàn bộ màn hình** có **65 mục**: màn chính, sheet và trạng thái mẫu; hỗ trợ tìm tiếng Việt không dấu và deep link `?screen=ideas`. [Danh mục đầy đủ](SCREENS.md) liệt kê từng mục. Chọn một mục dùng fixture/storage `.catalog` riêng; bấm **Trải nghiệm tự do** để trở lại dữ liệu thao tác bình thường. Không tự đưa fixture màn mẫu vào bộ dữ liệu này.

**Logic kế hoạch mới:** mọi thành viên thêm ý và đặt/đổi phiếu trong suốt kèo ACTIVE, kể cả đang đi. Ba phần **Lịch đang đi / Đề xuất / Sắp lịch** truy cập song song. Đề xuất/phiếu mới không làm mất lịch hoặc chặng đã xong. Chủ phòng chốt bản cập nhật mới thay lịch; lưu snapshot phiên bản cũ và giữ completion chặng còn lại. Xem [BA và delta hợp đồng](../docs/ba/continuous-planning.md); source React Native/backend chưa cập nhật luồng này.

Nếu cần camera, dùng HTTPS hoặc phục vụ bằng localhost. Từ thư mục này có thể chạy `py -3 -m http.server 4173 --bind 127.0.0.1 --directory dist`, rồi mở http://127.0.0.1:4173/. Chỉ nhấn **Dùng camera** khi muốn xin quyền; ảnh không được upload tới server. Ảnh mẫu luôn dùng được để thử luồng khi camera không khả dụng.

## Các luồng có thể thử

| Nhóm | Thao tác mẫu |
| --- | --- |
| Phòng | Tạo phòng rỗng; join mã DALAT26 / TOINAY / BIEN26 trên cùng bộ dữ liệu; khóa mã; quản lý thành viên; chuyển host cần người nhận đồng ý; rời/lưu trữ/khôi phục/xóa phòng |
| Đề xuất | Góp ý ẩn tên mọi lúc khi ACTIVE; sửa/xóa ý riêng chưa vào lịch; sửa nội dung reset đúng phiếu ý đó; hưởng ứng và lưu ý ẩn/có tên |
| Phiếu / lịch | Đặt/đổi 2/1/0 mọi lúc khi ACTIVE; đánh dấu xem hết là tùy chọn; shortlist và Borda tách lịch đã công bố; cập nhật phiên bản, giữ chặng đã xong và snapshot lịch cũ |
| Lịch rảnh / đi lại | Multi-select giờ, thêm giờ, thảo luận ẩn tên, host chọn giờ; attendance, budget/preferences và xe/đón; tải .ics local |
| Chia tiền | Chia đều/phần riêng; người ứng ngoài tập sử dụng; nhiều payer; duyệt ghi hộ; số dư/bù trừ; thanh toán một phần chờ receiver; reject/cancel; đề nghị đảo hai bên; điều chỉnh khoản sau thanh toán cần các bên |
| Chụp / chia sẻ | Camera mẫu hoặc camera browser; preview; caption; chọn cá nhân/nhóm/cả hai; chụp từ phòng chọn sẵn album, cá nhân tùy chọn; nháp, upload lỗi và mất phản hồi mô phỏng; đăng nguyên tử, không trùng khi retry |
| Ảnh / album | Feed cá nhân có grant; thư viện owner không trùng; lưới album, lọc tác giả/tháng; thêm ảnh của mình từ thư viện; comment ảnh, comment album, reaction theo nơi đăng; gỡ một nơi khác xóa mọi nơi |
| Ba chế độ sau kết thúc | Host đặt VIEW_ONLY/OPEN/WINDOWED; hạn từ endedAt; archive khóa, restore giữ hạn cũ; dùng đồng hồ mô phỏng để thử |
| Riêng tư / DM | Reply ảnh qua hội thoại hai người, text/reference riêng; read/unread và recall; reference mất quyền không mở được; hủy bạn/block/re-friend không hồi sinh grant cũ |
| Thông báo / báo cáo | In-app mẫu, deep link kiểm lại quyền; dedup ảnh đăng hai nơi; report lưu local, không gửi vận hành thật |

Chạm avatar góc phải → **Công cụ prototype → Xem với vai** để thử quyền trong mô hình. Công cụ mô phỏng mạng, thời gian và lỗi cũng nằm ở đây. An là host Đà Lạt; Bình host Tối nay; Chi là thành viên chưa phải bạn An, vẫn đọc được album chung nhưng không đọc phần cá nhân chưa share. Trong Đà Lạt có sổ tiền sẵn để thử; phòng mới luôn rỗng.

## Giới hạn cần hiểu

Đây là prototype nghiệp vụ theo [BA mục tiêu](../docs/ba/photo-memories/README.md), không phải app React Native mới, không phải frontend đã nối API, và không chứng minh server bảo mật. Người dùng có thể inspect toàn bộ dữ liệu giả local. Camera/flash native, auth/Google thật, push, upload/private CDN, TTL cache server, SSE đa máy, backup/purge, khóa DB/revision transaction và moderation vận hành chưa triển khai. Các phần ops trong BA không được coi pass từ prototype.

Chưa có quy trình thu hồi ảnh đã tải ngoài app hoặc bảo đảm ảnh chụp thật không chỉnh sửa. Rời phòng vẫn giữ số dư trong mô hình; quy trình former-member đối soát khi mất membership cần backend làm rõ, prototype không cung cấp luồng thanh toán cho người đã rời. Không nhập thông tin cá nhân hoặc khoản tiền thật để dùng prototype như app production.

Giới hạn windowHours, mặc định OPEN, quy tắc người join sau, rời phòng, chia sẻ và block là các đề xuất còn cần review ở BA/08, ngoại trừ ba lựa chọn người dùng đã xác nhận. Lịch rảnh/đi lại và nhiều payer được thêm để review khả năng đã thấy trong Swagger, không thay ba mức phiếu/Borda.

## Kiểm tra

Chạy `node tests/domain.test.cjs` cho các bất biến quyền, thời hạn, retry, phiếu và tiền; `node tests/render.test.cjs` cho các đường render. Không cần package. Kiểm tra cú pháp bằng `node --check` với các file JavaScript trong `dist/` và `tests/mobile-browser.js`.

[tests/mobile-browser.html](tests/mobile-browser.html) dùng storage `.test` riêng. Phục vụ thư mục `keo-full` bằng localhost, mở `/tests/mobile-browser.html?width=390&height=844&scene=camera`. `scene` nhận `camera`, `rooms`, `inbox`, `library`, `feed`, `chat`, `room`; `tab` chọn tab phòng. Harness chạy **122 assertion**, gồm điều hướng/layout, chuỗi thêm ý → vote → cập nhật lịch khi đang đi và render toàn bộ 65 mục catalogue. [tests/preview-browser.html](tests/preview-browser.html) kiểm viền, tìm kiếm, mở màn/sheet, chuyển viewport và tách dữ liệu. Không xin quyền camera. [tests/mobile-review.html](tests/mobile-review.html) xem bốn tab cạnh nhau.

Đã chạy Chrome headless cho 320 × 568, 375 × 812, 390 × 844 và 430 × 932. Camera thật, bàn phím ảo, safe area trên iOS/Android, TalkBack/VoiceOver và cỡ chữ hệ thống vẫn cần kiểm trên thiết bị. Xem [báo cáo kiểm chứng](../docs/integration/prototype-verification.md).
