---
name: keo-photo-integration
description: Triển khai hoặc review mobile và hợp đồng backend của tính năng ảnh Kèo theo BA trong repo, tập trung quyền theo nơi đăng, upload nguyên tử, ba chế độ album và tin nhắn riêng. Dùng cho thay đổi ảnh, album, media hoặc DM tham chiếu ảnh; không dùng để tự dựng backend khi chỉ được yêu cầu tài liệu.
---

# Tích hợp tính năng ảnh Kèo

Bắt đầu bằng [mục lục](../../docs/ba/photo-memories/README.md), [quyết định](../../docs/ba/photo-memories/08-decisions.md) và code thực tế liên quan. Các API trong BA là hợp đồng đề xuất, không giả vờ gọi được endpoint đã tồn tại. Repo ban đầu là app local; không dùng mock role/member làm authorization online.

## Đọc theo phần đang làm

- Camera/composer/feed: [FR và màn hình](../../docs/ba/photo-memories/02-business-requirements.md), [media/data/DTO](../../docs/ba/photo-memories/04-domain-data.md).
- Server/model/ACL/delete/policy: [BR và lifecycle](../../docs/ba/photo-memories/03-permissions-and-lifecycle.md), [API](../../docs/ba/photo-memories/05-api-contract.md).
- Upload, event, notification, offline/cache: [vận hành và NFR](../../docs/ba/photo-memories/06-events-notifications-nfr.md).
- Review/testing: chọn [AC](../../docs/ba/photo-memories/07-acceptance-and-delivery.md) đúng phạm vi; không chạy toàn bộ kiểm tra native nếu chỉ sửa tài liệu.

## Ranh giới triển khai

Một asset có một Moment; một Moment có tối đa một PERSONAL và một ROOM ACTIVE. Publish các đích nguyên tử. Không để việc share cá nhân tiết lộ album, thành viên hoặc comment nhóm. Không cấp ảnh qua raw Moment/media URL cho người chỉ có quyền một placement. Author rời phòng quản lý ảnh mình qua thư viện, không đọc group comment.

Album sau kết thúc có đúng ba chế độ do HOST chọn; deadline tính từ endedAt server, `<` là còn hạn, `>=` là hết. ARCHIVED chung khóa đóng góp; hiddenForMe/local archived không khóa cả nhóm. Kiểm policy/membership tại commit, không tin capabilities hoặc state cached trên mobile. Gỡ/xóa của owner vẫn có đường hợp lệ khi album chỉ đọc.

Comment ảnh ROOM, comment album và DM là ba luồng khác nhau. DM reference chỉ là pointer có kiểm quyền, không copy thumbnail vĩnh viễn. HOST không đọc DM. Reaction/counts chỉ thuộc placement đang xem, lọc quyền và block theo BA.

## Quyết định và chứng cứ

Không tự đổi các lựa chọn C-06–08 đã xác nhận; các D còn mở phải được chỉ rõ khi ticket phụ thuộc. Nếu nhiệm vụ cho phép triển khai theo giả định, ghi giả định cụ thể cùng ticket, không đổi status thành đã xác nhận. Không mở rộng scope login/chat/media provider khi chưa cần thiết cho yêu cầu được giao.

Chọn kiểm thử chứng minh quyền giữa hai user/phòng, idempotency sau commit mất response và deadline/race nếu phần sửa chạm các bất biến đó. Test UI mock không chứng minh server authorization. Với camera/native, ghi rõ thiết bị và nền tảng đã kiểm; dependency có sẵn không chứng minh permissions/build/runtime hoạt động. Không sửa schema MMKV v1 mà không giữ đọc dữ liệu cũ hoặc có migration được nêu rõ.

Khi contract cần thay đổi, sửa file BA chuẩn và AC liên quan cùng code. Báo endpoint/server nào có thật, phần nào mock, lệnh kiểm nào đã chạy và giới hạn còn lại. Không đánh dấu FR/AC hoàn thành từ việc tạo tài liệu hoặc scaffold.
