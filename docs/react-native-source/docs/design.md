# Let’s Play — hướng thiết kế

Giao diện dành cho nhóm bạn lên kế hoạch đi chơi. Giữ nền kem và xanh trầm từ mockup nghiệp vụ, thêm bề mặt đào và tím nhạt. Tránh giao diện quản trị và chi tiết HRM.

## Icon

Nguồn: `src/design/PlayIcon.tsx`, bộ biểu tượng SVG tự vẽ trên lưới 24 × 24. Nét 1,75; `strokeLinecap=round`, `strokeLinejoin=round`; không tô đặc ở lớp icon điều khiển. Nét mở và góc bo lấy cảm hứng từ ảnh tham chiếu Pulsar Line. Không sử dụng emoji làm icon cấu trúc.

Icon ứng dụng: nút Play nét bo trong vòng tròn mở, ngôi sao màu đào ở góc trên, nền xanh. Có SVG nguồn, PNG 1024, các mật độ Android và AppIcon iOS.

## Màu và bố cục

- Nền `#F7F6F2`, bề mặt trắng, chữ chính `#24372F`.
- Hành động chính `#245C49`, bề mặt xanh `#EAF1E9`.
- Điểm nhấn `#AE422C`, đào `#FAEDE3`, tím nhạt `#EEEAF6`.
- Nhịp cách 4/8; bo thẻ 20–28; nội dung tối đa 720 dp.
- Font hệ thống hỗ trợ tiếng Việt. Nội dung chữ được phép phóng theo cài đặt hệ thống.
- Safe area bao quanh màn hình. Vùng chạm tối thiểu 48 dp, trạng thái chọn có ngữ nghĩa truy cập, điều khiển icon có tên.

Đã đọc hướng dẫn và tìm kiếm bằng skill ui-ux-pro-max. Gợi ý design-system tự động thiên về trang landing/fashion nên không dùng làm đặc tả sản phẩm. Áp dụng hướng dẫn vùng chạm, phản hồi thao tác và safe area phù hợp React Native; palette và cấu trúc lấy từ yêu cầu và mockup của Let’s Play.
