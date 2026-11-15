# Kế hoạch triển khai tính năng Cảm xúc & Bình luận ảnh

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện tính năng Cảm xúc (5 reactions chuẩn) và Bình luận ảnh phẳng cho ảnh trong Kèo trên `PhotoDetailScreen`, kèm lưu trữ an toàn trong MMKV và tự động dọn dẹp khi xóa ảnh.

**Architecture:** Tích hợp state reactions và comments vào `PhotoProvider.tsx` lưu trong MMKV `letplay-moments` (key `interactions.v1`); hỗ trợ cơ chế toggle reaction một chạm và quản lý bình luận phẳng (1–1.000 ký tự); render thanh emoji và danh sách bình luận trên `PhotoDetailScreen` tuân thủ đúng BA UC-05 & D-08.

**Tech Stack:** React Native 0.79, React 19, TypeScript, `react-native-mmkv`, Zod, Jest.

**Spec:** [`docs/superpowers/specs/2026-11-11-photo-reactions-and-comments-design.md`](../specs/2026-11-11-photo-reactions-and-comments-design.md)

## Global Constraints

- React Native 0.79, React 19; không sửa đổi `package.json`.
- Giữ nguyên vẹn khả năng đọc dữ liệu MMKV v1 của phòng local (`let-play-rooms`); không sửa schema migration phòng local.
- Không dùng danh tính client (`guestId`, role local) làm quyền online; không giả mạo backend API khi chưa có endpoint.
- Ranh giới BA: Cảm xúc áp dụng cho mọi ảnh; Bình luận chỉ dành cho ảnh gắn Kèo (`photo.intendedRoomId !== undefined`).
- Toàn bộ lệnh kiểm tra: `yarn typecheck`, `yarn test --runInBand`, `yarn lint` phải đạt 100% không lỗi.

## Review Focus

1. **Cơ chế Toggle Reaction**: Bấm vào cảm xúc mới sẽ đổi loại (không tăng gấp đôi số lượng); bấm vào cảm xúc đang chọn sẽ thu hồi (bỏ cảm xúc).
2. **Bình luận hợp lệ & Bất biến**: Từ chối bình luận rỗng, chỉ khoảng trắng hoặc vượt 1.000 ký tự. Không cho sửa trực tiếp; chỉ tác giả mới có quyền xóa bình luận của mình.
3. **Phân định rõ ràng ảnh cá nhân vs ảnh Kèo**: Ảnh cá nhân (`!intendedRoomId`) không mở khung nhập bình luận, chỉ nhận cảm xúc; ảnh Kèo (`intendedRoomId`) cho phép bình luận nhóm.
4. **Dọn dẹp rác khi xóa ảnh (Cascading Deletion)**: Khi gọi `photos.remove(photoId)`, toàn bộ reaction và comment liên quan đến `photoId` phải được xóa sạch khỏi MMKV.
5. **Clean Test Output**: Không có bất kỳ cảnh báo `act(...)` hoặc console warning nào khi chạy Jest.

---

### Task 1: Model & Storage Layer cho Cảm xúc & Bình luận (`PhotoProvider.tsx`)

**Files:**
- Create: `src/modules/keo/__tests__/photoInteractions.test.ts`
- Modify: `src/modules/keo/store/PhotoProvider.tsx`

**Interfaces:**
- Consumes: `storage` từ MMKV `letplay-moments`, `useOnline().scope`, `useOnline().account`
- Produces: `toggleReaction(photoId, type)`, `getPhotoReactions(photoId)`, `addComment(photoId, text)`, `deleteComment(commentId)`, `getPhotoComments(photoId)`, `REACTION_EMOJIS`

- [ ] **Step 1: Viết test kiểm tra tương tác cảm xúc và bình luận**

```typescript
// src/modules/keo/__tests__/photoInteractions.test.ts
import { photoReactionSchema, photoCommentSchema } from '../store/PhotoProvider';

describe('Photo interactions schemas', () => {
  it('validates a photo reaction schema correctly', () => {
    const reaction = {
      photoId: '11111111-1111-4111-8111-111111111111',
      userScope: 'guest:123',
      type: 'HEART',
      createdAt: 1700000000,
    };
    const parsed = photoReactionSchema.parse(reaction);
    expect(parsed.type).toBe('HEART');
  });

  it('validates a photo comment schema correctly', () => {
    const comment = {
      id: '22222222-2222-4222-8222-222222222222',
      photoId: '11111111-1111-4111-8111-111111111111',
      authorScope: 'guest:123',
      authorName: 'An',
      text: 'Ảnh đẹp quá!',
      createdAt: 1700000000,
    };
    const parsed = photoCommentSchema.parse(comment);
    expect(parsed.text).toBe('Ảnh đẹp quá!');
  });

  it('rejects empty or whitespace-only comments', () => {
    const invalid = {
      id: '22222222-2222-4222-8222-222222222222',
      photoId: '11111111-1111-4111-8111-111111111111',
      authorScope: 'guest:123',
      authorName: 'An',
      text: '   ',
      createdAt: 1700000000,
    };
    expect(() => photoCommentSchema.parse(invalid)).toThrow();
  });
});
```

- [ ] **Step 2: Chạy test để xác nhận test thất bại (do chưa có schema)**
Run: `yarn jest src/modules/keo/__tests__/photoInteractions.test.ts`
Expected: FAIL

- [ ] **Step 3: Cập nhật `PhotoProvider.tsx` với state và methods tương tác**
Trong `src/modules/keo/store/PhotoProvider.tsx`:
- Export `ReactionType`, `REACTION_EMOJIS`, `photoReactionSchema`, `photoCommentSchema`.
- Quản lý `reactions: PhotoReaction[]` và `comments: PhotoComment[]` lưu trữ trong MMKV `KEY_INTERACTIONS = 'interactions.v1'`.
- Thêm methods vào context `Photos`:
  - `toggleReaction: (photoId: string, type: ReactionType) => Promise<void>`
  - `getPhotoReactions: (photoId: string) => { counts: Record<ReactionType, number>; myReaction: ReactionType | null }`
  - `addComment: (photoId: string, text: string) => Promise<string>`
  - `deleteComment: (commentId: string) => Promise<void>`
  - `getPhotoComments: (photoId: string) => PhotoComment[]`
- Trong hàm `remove(id)`: lọc bỏ đồng thời các reactions và comments thuộc `photoId === id`.

- [ ] **Step 4: Chạy lại test để xác nhận test PASS**
Run: `yarn jest src/modules/keo/__tests__/photoInteractions.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**
`git add src/modules/keo/__tests__/photoInteractions.test.ts src/modules/keo/store/PhotoProvider.tsx`
`git commit -m "feat(photo): add reactions and comments data layer with cascading cleanup"`

---

### Task 2: Giao diện Cảm xúc & Bình luận trên `PhotoDetailScreen` (`PhotoScreens.tsx`)

**Files:**
- Modify: `src/modules/keo/screens/PhotoScreens.tsx`

**Interfaces:**
- Consumes: `usePhotos()`, `REACTION_EMOJIS`, `ReactionType`, `PhotoComment`, `useOnline()`
- Produces: Thanh reaction 5 emoji và danh sách bình luận với ô nhập trên `PhotoDetailScreen`

- [ ] **Step 1: Thêm thanh cảm xúc trên `PhotoDetailScreen`**
- Gọi `photos.getPhotoReactions(photo.id)`.
- Render hàng ngang 5 nút emoji:
  - Bấm vào gọi `photos.toggleReaction(photo.id, type)`.
  - Nút được highlight viền/màu xanh `t.softGreen` khi `myReaction === type`.
  - Hiển thị số đếm nhỏ nếu `counts[type] > 0`.

- [ ] **Step 2: Thêm khu vực bình luận trên `PhotoDetailScreen`**
- Nếu `photo.intendedRoomId`:
  - Hiển thị danh sách bình luận lấy từ `photos.getPhotoComments(photo.id)`.
  - Từng bình luận hiển thị: `authorName`, thời gian, nội dung `text`.
  - Nếu `comment.authorScope === online.scope`: hiển thị icon thùng rác nhỏ để tác giả tự xóa bình luận.
  - Thêm ô nhập `TextInput` (multiline, maxLength 1000) và nút bấm "Gửi".
- Nếu `!photo.intendedRoomId` (ảnh cá nhân):
  - Hiển thị thông báo `Notice`: "Ảnh dòng cá nhân nhận cảm xúc hoặc tin nhắn riêng từ bạn bè."

- [ ] **Step 3: Kiểm tra biên dịch TypeScript**
Run: `yarn typecheck`
Expected: PASS

- [ ] **Step 4: Commit**
`git add src/modules/keo/screens/PhotoScreens.tsx`
`git commit -m "feat(photo): add reactions bar and comments section to photo detail screen"`

---

### Task 3: Kiểm chứng toàn diện (Full Verification)

**Files:**
- Toàn bộ repo

- [ ] **Step 1: Kiểm tra biên dịch TypeScript**
Run: `yarn typecheck`
Expected: PASS (0 errors)

- [ ] **Step 2: Chạy toàn bộ Unit Tests**
Run: `yarn test --runInBand`
Expected: 8/8 test suites PASS (30+ tests, 0 warnings)

- [ ] **Step 3: Kiểm tra linter**
Run: `yarn lint --quiet`
Expected: 0 errors

- [ ] **Step 4: Kiểm tra trạng thái Git**
Run: `git status`
Expected: Cây làm việc sạch sẽ
