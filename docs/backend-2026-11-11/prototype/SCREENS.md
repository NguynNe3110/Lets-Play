# Danh mục màn hình prototype Kèo

Có 65 mục gồm màn chính, sheet và trạng thái mẫu. Mở index.html trên máy tính để dùng sidebar tìm kiếm và xem trong viền điện thoại. Mỗi link dưới mở fixture riêng, không dùng dữ liệu trải nghiệm tự do.

Mọi mục đều tương tác được trong phạm vi prototype local; camera, lỗi thiết bị và backend thật không được chứng minh bởi danh mục này.

## Home · Chụp ảnh

| Mã             | Màn / sheet / trạng thái                                              |
| -------------- | --------------------------------------------------------------------- |
| home           | [Home — máy ảnh](dist/index.html?screen=home)                         |
| camera-room    | [Chụp từ album kèo](dist/index.html?screen=camera-room)               |
| draft          | [Home có ảnh nháp](dist/index.html?screen=draft)                      |
| composer       | [Preview · đăng cá nhân](dist/index.html?screen=composer)             |
| composer-room  | [Preview · album chọn sẵn](dist/index.html?screen=composer-room)      |
| composer-both  | [Preview · đăng cả hai nơi](dist/index.html?screen=composer-both)     |
| uploading      | [Đang tải ảnh](dist/index.html?screen=uploading)                      |
| upload-error   | [Tải lỗi · còn bản nháp](dist/index.html?screen=upload-error)         |
| upload-unknown | [Đăng chưa rõ kết quả · retry](dist/index.html?screen=upload-unknown) |
| history        | [Lịch sử · từng khoảnh khắc](dist/index.html?screen=history)          |
| history-grid   | [Lịch sử · lưới ảnh](dist/index.html?screen=history-grid)             |
| history-filter | [Lọc theo bạn bè](dist/index.html?screen=history-filter)              |
| photo-personal | [Chi tiết ảnh cá nhân](dist/index.html?screen=photo-personal)         |

## Kèo · Đề xuất và lịch

| Mã             | Màn / sheet / trạng thái                                          |
| -------------- | ----------------------------------------------------------------- |
| rooms          | [Danh sách kèo hiện tại](dist/index.html?screen=rooms)            |
| rooms-ended    | [Kèo đã kết thúc](dist/index.html?screen=rooms-ended)             |
| rooms-archived | [Kèo lưu trữ](dist/index.html?screen=rooms-archived)              |
| room-create    | [Tạo kèo](dist/index.html?screen=room-create)                     |
| room-join      | [Tham gia bằng mã](dist/index.html?screen=room-join)              |
| room-empty     | [Kèo mới · chưa có ý](dist/index.html?screen=room-empty)          |
| room-invite    | [Mời bạn vào kèo](dist/index.html?screen=room-invite)             |
| plan           | [Lịch đang đi](dist/index.html?screen=plan)                       |
| ideas          | [Đề xuất và bình chọn liên tục](dist/index.html?screen=ideas)     |
| idea-create    | [Thêm ý khi đang đi chơi](dist/index.html?screen=idea-create)     |
| idea-edit      | [Sửa ý chưa vào lịch](dist/index.html?screen=idea-edit)           |
| order          | [Sắp lịch · danh sách dự kiến](dist/index.html?screen=order)      |
| order-ballot   | [Góp thứ tự và Borda](dist/index.html?screen=order-ballot)        |
| plan-confirm   | [Xác nhận cập nhật lịch](dist/index.html?screen=plan-confirm)     |
| plan-history   | [Lịch sử phiên bản lịch](dist/index.html?screen=plan-history)     |
| note-create    | [Thêm lưu ý · có tên hoặc ẩn](dist/index.html?screen=note-create) |

## Kèo · Hẹn và thành viên

| Mã             | Màn / sheet / trạng thái                                          |
| -------------- | ----------------------------------------------------------------- |
| schedule       | [Lịch rảnh · tham gia · đi cùng](dist/index.html?screen=schedule) |
| slot-create    | [Góp khung giờ](dist/index.html?screen=slot-create)               |
| slot-comments  | [Thảo luận khung giờ](dist/index.html?screen=slot-comments)       |
| members        | [Danh sách thành viên](dist/index.html?screen=members)            |
| member-actions | [Chuyển host · mời ra](dist/index.html?screen=member-actions)     |
| room-end       | [Xác nhận kết thúc kèo](dist/index.html?screen=room-end)          |
| room-leave     | [Xác nhận rời kèo](dist/index.html?screen=room-leave)             |

## Chia tiền

| Mã                   | Màn / sheet / trạng thái                                                   |
| -------------------- | -------------------------------------------------------------------------- |
| money                | [Tổng quan sổ chi và số dư](dist/index.html?screen=money)                  |
| expense-create       | [Ghi chi · đều/riêng · nhiều payer](dist/index.html?screen=expense-create) |
| expense-detail       | [Chi tiết khoản chi](dist/index.html?screen=expense-detail)                |
| expense-edit         | [Sửa / đề nghị điều chỉnh](dist/index.html?screen=expense-edit)            |
| expense-approval     | [Duyệt khoản được ghi hộ](dist/index.html?screen=expense-approval)         |
| settlement-create    | [Báo đã chuyển · chờ nhận](dist/index.html?screen=settlement-create)       |
| settlement-pending   | [Người nhận xác nhận tiền](dist/index.html?screen=settlement-pending)      |
| settlement-confirmed | [Đã nhận · đề nghị đảo](dist/index.html?screen=settlement-confirmed)       |

## Album kèo

| Mã            | Màn / sheet / trạng thái                                           |
| ------------- | ------------------------------------------------------------------ |
| album         | [Lưới ảnh và thảo luận album](dist/index.html?screen=album)        |
| album-empty   | [Album trống](dist/index.html?screen=album-empty)                  |
| album-closed  | [Album chỉ xem sau kết thúc](dist/index.html?screen=album-closed)  |
| photo-room    | [Chi tiết ảnh · bình luận nhóm](dist/index.html?screen=photo-room) |
| album-policy  | [Ba chế độ sau kết thúc](dist/index.html?screen=album-policy)      |
| album-library | [Thêm ảnh từ thư viện riêng](dist/index.html?screen=album-library) |
| photo-report  | [Báo cáo ảnh](dist/index.html?screen=photo-report)                 |

## Tin nhắn

| Mã               | Màn / sheet / trạng thái                                            |
| ---------------- | ------------------------------------------------------------------- |
| inbox            | [Danh sách hội thoại](dist/index.html?screen=inbox)                 |
| new-chat         | [Chọn người nhắn riêng](dist/index.html?screen=new-chat)            |
| chat             | [Hội thoại hai người](dist/index.html?screen=chat)                  |
| chat-reply       | [Trả lời riêng theo ảnh](dist/index.html?screen=chat-reply)         |
| chat-unavailable | [Tham chiếu ảnh mất quyền](dist/index.html?screen=chat-unavailable) |

## Thời gian

| Mã            | Màn / sheet / trạng thái                                        |
| ------------- | --------------------------------------------------------------- |
| memories      | [Lịch ảnh theo ngày / tháng](dist/index.html?screen=memories)   |
| memories-grid | [Toàn bộ ảnh của bạn](dist/index.html?screen=memories-grid)     |
| memory-manage | [Ảnh đang chia sẻ ở đâu?](dist/index.html?screen=memory-manage) |
| memory-delete | [Xác nhận xóa ở mọi nơi](dist/index.html?screen=memory-delete)  |

## Tài khoản và công cụ

| Mã             | Màn / sheet / trạng thái                                          |
| -------------- | ----------------------------------------------------------------- |
| profile        | [Hồ sơ · bạn bè · quyền riêng tư](dist/index.html?screen=profile) |
| person-actions | [Kết bạn · hủy bạn · chặn](dist/index.html?screen=person-actions) |
| notifications  | [Thông báo](dist/index.html?screen=notifications)                 |
| scenarios      | [Công cụ thử tình huống](dist/index.html?screen=scenarios)        |
| offline        | [Mất mạng · giữ bản nháp](dist/index.html?screen=offline)         |
