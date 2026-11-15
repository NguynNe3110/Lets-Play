# Thiết kế tính năng Cảm xúc (Reactions) và Bình luận ảnh (Phương án 1)

**Ngày lập**: 11/11/2026  
**Trạng thái**: Chờ duyệt (Pending Approval)  
**Tác giả**: Antigravity Pair Programmer & Người dùng  

---

## 1. Mục đích và Lý do (Intent & The Why)

- Hiện thực hóa yêu cầu đã xác nhận tại [docs/ba/photo-memories/08-decisions.md](../../ba/photo-memories/08-decisions.md) (C-06: Bản đầu có cảm xúc, bình luận chung và trả lời riêng) và đặc tả tại [02-business-requirements.md](../../ba/photo-memories/02-business-requirements.md) (UC-05), [04-domain-data.md](../../ba/photo-memories/04-domain-data.md) (mục 8).
- Nâng cao tính tương tác và trải nghiệm người dùng khi xem lại khoảnh khắc:
  - Cho phép người xem bày tỏ 5 cảm xúc chuẩn: `HEART` ❤️, `LAUGH` 😂, `WOW` 😮, `FIRE` 🔥, `SAD` 😢.
  - Cho phép gửi bình luận văn bản thuần (1–1.000 ký tự) gắn với ảnh trong Kèo (`intendedRoomId !== undefined`).
  - Phân định rõ ràng theo BA UC-05 & D-08: Ảnh cá nhân chỉ có Cảm xúc và Tin nhắn riêng; Bình luận chỉ dành cho ảnh thuộc Kèo.
  - Người dùng có thể tự xóa bình luận của mình; khi xóa ảnh, toàn bộ cảm xúc và bình luận của ảnh đó tự động được dọn dẹp sạch sẽ.
- Lưu trữ an toàn trong MMKV cục bộ theo scope tài khoản/khách hiện tại, sẵn sàng tích hợp khi backend công bố endpoint online.

---

## 2. Mục tiêu, Phi mục tiêu và Ranh giới (Goals, Non-Goals, Anti-Goals)

### 2.1. Mục tiêu (Goals)
1. **Mô hình hóa dữ liệu chuẩn xác**:
   - Định nghĩa enum 5 cảm xúc `ReactionType`: `HEART`, `LAUGH`, `WOW`, `FIRE`, `SAD`.
   - Mỗi người dùng chỉ có tối đa 1 cảm xúc trên mỗi ảnh: bấm cảm xúc mới sẽ đổi loại; bấm cảm xúc đang chọn sẽ thu hồi (toggle).
   - Bình luận phẳng (`PhotoComment`): gồm `id`, `photoId`, `authorScope`, `authorName`, `text` (1–1.000 ký tự), `createdAt`.
2. **Ranh giới nghiệp vụ (BA Alignment - D-08 & UC-05)**:
   - **Cảm xúc**: Áp dụng cho mọi ảnh (cả ảnh cá nhân và ảnh gắn Kèo).
   - **Bình luận**: Chỉ hiển thị và cho phép bình luận trên ảnh được gắn vào một Kèo (`intendedRoomId !== undefined`). Đối với ảnh chỉ giữ dòng cá nhân, hiển thị thông báo rõ ràng: "Ảnh cá nhân chỉ nhận cảm xúc hoặc tin nhắn riêng."
3. **Lưu trữ & Vòng đời an toàn (Storage & Lifecycle)**:
   - Lưu trữ trong MMKV `letplay-moments` với schema Zod chuẩn.
   - Khi xóa ảnh qua `photos.remove(id)`: tự động dọn dẹp tất cả reactions và comments liên quan đến ảnh đó.
4. **Giao diện trực quan trên `PhotoDetailScreen`**:
   - Thanh chọn cảm xúc nhanh với 5 biểu tượng emoji, hiển thị số lượng và trạng thái đang chọn của người dùng.
   - Danh sách bình luận rõ ràng kèm tên người gửi, thời gian và nút xóa bình luận của chính mình.
   - Ô nhập bình luận có giới hạn ký tự và nút gửi mượt mà.
5. **Kiểm thử & Chất lượng**:
   - Viết unit test đầy đủ cho luồng toggle reaction, thêm bình luận, xóa bình luận và dọn dẹp khi xóa ảnh.
   - Đảm bảo 100% `yarn typecheck`, `yarn test --runInBand`, `yarn lint` xanh sạch.

### 2.2. Phi mục tiêu (Non-Goals)
- Không hỗ trợ bình luận đa cấp (nested replies/threads) ở V1 (bị cấm rõ ràng tại BA UC-05: bình luận phẳng, không parentCommentId).
- Không hỗ trợ sửa bình luận sau khi gửi (theo BA: text bất biến, muốn đổi thì xóa và gửi lại).
- Không hỗ trợ emoji tự do ngoài 5 reaction chuẩn đã xác nhận.

### 2.3. Ranh giới cấm (Anti-Goals)
- Không cho phép gửi bình luận rỗng hoặc toàn khoảng trắng.
- Không cho phép người dùng xóa bình luận của người khác.

---

## 3. Các phương án kỹ thuật đã cân nhắc (Approaches)

### Phương án 1 (ĐÃ CHỌN): Tích hợp trực tiếp vào `PhotoProvider` và MMKV `letplay-moments`
- Tích hợp state và methods vào `PhotoProvider.tsx` (`letplay-moments` MMKV store).
- **Lý do chọn**: Tự nhiên, tối ưu, bảo đảm tính nhất quán nguyên tử giữa ảnh và tương tác (khi xóa ảnh thì tương tác bị xóa kèm tự động), không cần tạo thêm Provider làm rối cây component.

### Phương án 2 (Bị loại): Tách thành `PhotoInteractionProvider` riêng
- **Lý do loại**: Tăng số lượng Provider ở root, khó đồng bộ việc xóa dữ liệu tương tác khi một ảnh bị xóa từ `PhotoProvider`.

---

## 4. Chi tiết kiến trúc và giải pháp (Architecture & Implementation Details)

### 4.1. Data Models (`src/modules/keo/store/PhotoProvider.tsx`)
```typescript
export type ReactionType = 'HEART' | 'LAUGH' | 'WOW' | 'FIRE' | 'SAD';

export const REACTION_EMOJIS: Record<ReactionType, string> = {
  HEART: '❤️',
  LAUGH: '😂',
  WOW: '😮',
  FIRE: '🔥',
  SAD: '😢',
};

export const photoReactionSchema = z.object({
  photoId: z.string().uuid(),
  userScope: z.string().min(1),
  type: z.enum(['HEART', 'LAUGH', 'WOW', 'FIRE', 'SAD']),
  createdAt: z.number(),
});
export type PhotoReaction = z.infer<typeof photoReactionSchema>;

export const photoCommentSchema = z.object({
  id: z.string().uuid(),
  photoId: z.string().uuid(),
  authorScope: z.string().min(1),
  authorName: z.string().min(1).max(60),
  text: z.string().min(1).max(1000),
  createdAt: z.number(),
});
export type PhotoComment = z.infer<typeof photoCommentSchema>;
```

### 4.2. API & Methods trong `PhotoProvider`
- `toggleReaction(photoId: string, type: ReactionType): Promise<void>`:
  - Nếu đã có reaction cùng type của `scope` hiện tại -> xóa bỏ.
  - Nếu đã có reaction khác type -> đổi sang type mới.
  - Nếu chưa có -> thêm mới.
- `getPhotoReactions(photoId: string)`:
  - Trả về `{ counts: Record<ReactionType, number>, myReaction: ReactionType | null }`.
- `addComment(photoId: string, text: string): Promise<string>`:
  - Kiểm tra ảnh tồn tại và có `intendedRoomId` (chỉ cho phép bình luận ảnh trong kèo).
  - Kiểm tra `text.trim().length >= 1` và `<= 1000`.
  - Lấy `authorName` từ `online.account?.displayName` hoặc fallback `'Bạn'`.
  - Sinh UUID, lưu vào danh sách và MMKV.
- `deleteComment(commentId: string): Promise<void>`:
  - Kiểm tra `authorScope === scope` (chỉ tác giả mới được xóa).
- `getPhotoComments(photoId: string)`:
  - Trả về danh sách comments sắp xếp theo `createdAt` tăng dần.

### 4.3. Giao diện `PhotoDetailScreen` (`PhotoScreens.tsx`)
- Thêm thanh **Cảm xúc** (dành cho mọi ảnh):
  - Dãy 5 nút cảm xúc xếp ngang, bo tròn mềm mại.
  - Hiển thị emoji kèm số lượng người thả (nếu > 0).
  - Nút sáng màu / viền đậm khi người dùng đang thả cảm xúc đó.
- Thêm khu vực **Bình luận**:
  - Nếu ảnh thuộc Kèo (`photo.intendedRoomId` có giá trị):
    - Tiêu đề: "Bình luận trong kèo (N)".
    - Danh sách bình luận: hiển thị tên tác giả (tô màu nhấn nếu là 'Bạn'), thời gian (format giờ/ngày) và nội dung văn bản.
    - Nút icon thùng rác nhỏ bên cạnh nếu bình luận do chính người dùng gửi.
    - Thanh nhập ở cuối: `TextInput` (multiline nhẹ, tối đa 1.000 ký tự) + nút bấm "Gửi".
  - Nếu ảnh chỉ giữ dòng cá nhân:
    - Hiển thị thông báo nhỏ: "Ảnh dòng cá nhân nhận cảm xúc hoặc tin nhắn riêng từ bạn bè."

---

## 5. Kế hoạch kiểm chứng (Verification Plan)
- **Unit Tests (`photoInteractions.test.ts`)**:
  - Test toggle reaction (thêm mới, đổi loại, thu hồi).
  - Test add comment (bình luận hợp lệ, từ chối bình luận rỗng / quá 1000 ký tự, từ chối bình luận trên ảnh không gắn kèo).
  - Test delete comment (chỉ xóa được bình luận của chính mình).
  - Test cascading delete (xóa ảnh sẽ xóa toàn bộ reaction và comment của ảnh đó).
- **Linter & Typecheck**:
  - `yarn typecheck` -> PASS (0 errors).
  - `yarn test --runInBand` -> PASS (toàn bộ test suites xanh sạch).
  - `yarn lint --quiet` -> PASS (0 errors).
