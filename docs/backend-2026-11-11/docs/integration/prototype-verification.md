# Kiểm chứng prototype điện thoại và giới hạn

Ngày 10/11/2026. Source `prototypes/keo-full`, entry `dist/index.html`; mở thẳng `app.html` trên desktop cũng chuyển vào khung điện thoại. Bố cục tham chiếu Locket, dùng lại theme xanh–kem của app và nav nổi **Home / Kèo / Tin nhắn / Thời gian**. Sidebar có 65 màn/sheet/trạng thái mẫu, tìm kiếm và deep link. Preview giữ viewport 375 × 812, 390 × 844 hoặc 430 × 932 rồi scale cả thiết bị. Điện thoại dùng app toàn màn hình. Đã cập nhật [luồng đề xuất liên tục](../ba/continuous-planning.md) trong prototype; chưa thay source React Native hoặc website/backend ngoài repo.

## Đã kiểm

- Kiểm tra cú pháp JavaScript của domain, app, scenes và preview bằng `node --check`.
- `node prototypes/keo-full/tests/domain.test.cjs`: **42/42 đạt**. Có quyền ảnh, publish/retry, deadline/policy, leave/block, DM, phiếu/Borda và tiền; thêm 8 test cho góp ý/vote mọi phase ACTIVE, shortlist không đổi lịch chung, publish có snapshot/completion, sửa/xóa ý chưa vào lịch, validation/host và khóa sau kết thúc.
- `node prototypes/keo-full/tests/render.test.cjs`: **100 đường render đạt** cho các user/page/tab bằng DOM surface tối thiểu; kiểm không lỗi và không render HTML người dùng thành script, gồm bước host sắp lịch cuối và trạng thái kết quả đăng chưa rõ. Đây **không phải trình duyệt**, không khẳng định bố cục/focus/touch đã pass.
- **Chrome headless thật** qua `tests/mobile-browser.html`: **122/122 assertion đạt** ở từng viewport **320 × 568, 375 × 812, 390 × 844, 430 × 932**. Mỗi lần chạy đủ màn chính/năm tab phòng, form chụp/chụp lại, mở sheet/chat, lịch/lưới, chuỗi thêm ý sau chốt → vote → sửa shortlist → publish bản lịch mới. Kiểm lịch cũ/completion không mất, ENDED khóa thao tác, và toàn bộ 65 màn mẫu render/không tràn ngang/sheet trong viewport.
- `tests/preview-browser.html`: **10/10 đạt** trong Chrome thật: viền, đủ 65 mục, tìm không dấu, mở Đề xuất/sheet, deep link, viewport 430 × 932, khung không vượt vùng preview và trở lại dữ liệu tự do. Đã kiểm riêng entry `app.html` trên desktop chuyển đúng preview có viền.
- Đã xem screenshot Home, Kèo, phòng, lịch sử, inbox, chat và Thời gian; đã render trực tiếp URL `app.html?screen=ideas` và thấy khung điện thoại + sidebar + theme xanh. Screenshot/log ở `tmp/qa/`. Harness dùng storage `.test`, catalogue dùng `.catalog`; không ghi đè dữ liệu thường. Bộ ảnh mẫu là SVG có sẵn; không dùng ảnh riêng trong screenshot Locket làm fixture.
- Đã sửa hai lỗi tìm thấy qua browser: tab phòng tràn 4 px ở màn 320 px; nút Chụp lại/Để sau trong form bị coi là submit do thiếu `type="button"`. Regression có assertion cho Chụp lại không submit ảnh.
- HTML/JS/CSS là tệp tĩnh cùng thư mục, ảnh minh họa SVG tự tạo; không phụ thuộc CDN/font/image bên ngoài, không có request backend thật hoặc token trong source.
- Thao tác với fixture mẫu giữ ở localStorage, không được sao chép vào source hay gói bàn giao. Thay người dùng làm mới quyền hiển thị theo mô hình. Công cụ đổi vai/thử tình huống nằm trong avatar → Công cụ prototype.

## Chưa kiểm hoặc bị môi trường chặn

Browser host và native computer-use pipe không khả dụng. Đã dùng Chrome headless với profile QA riêng để render, chụp và chạy harness cục bộ; không dùng kết quả này thay cho iOS/Android thật. Camera permission/flash/đổi camera thật, bàn phím ảo, safe area thiết bị, TalkBack/VoiceOver, font hệ thống lớn, xoay màn hình và dialog focus bằng công nghệ hỗ trợ chưa được kiểm. Trên thiết bị cần thử: chụp → preview → chụp lại/đăng, mở keyboard trong chat, cuộn đến cuối từng tab phòng, quay lại và đổi người dùng.

Đã đăng ký một Site riêng và lưu project ID vào `.openai/hosting.json`; source helper không chạy được bước Git con vì Node child process bị `EPERM`. Đồng thời request HTTPS từ workspace lỗi proxy. Vì vậy **chưa có bằng chứng publish thành công**, không đưa expected URL thành URL đã deploy. Bản local và gói ZIP vẫn có thể mở để review. Giữ nguyên project ID để lần sau tiếp tục, không tạo Site thứ hai chỉ vì lần publish chưa hoàn tất.

Backend runtime và website Vercel chưa test. Swagger do người dùng gửi đã được review tĩnh riêng tại `backend-gap-review.md`; không dùng kết quả prototype để đổi các GAP thành VERIFIED_PASS.

## Những phần chỉ mô phỏng

Upload/processing, đối tượng ảnh và audience được chạy trong domain local; không có object storage/private CDN, DB transaction/If-Match thật hoặc SSE đa máy. Notification/report là dữ liệu mẫu, chưa gửi push hoặc chuyển report tới người thật. Tải .ics là thao tác local; không có Google Calendar OAuth/sync. Camera web có nhánh gọi getUserMedia theo hành động người dùng, chưa phải camera native/flash app.

Chi tiết đề xuất còn mở trong BA/08 vẫn cần review. Rời phòng giữ số dư nhưng luồng cựu thành viên đối soát chưa được cung cấp trong prototype. Không nhập dữ liệu thật hoặc dùng bản này thay app ghi sổ production.
