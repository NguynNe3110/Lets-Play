# Thiết kế hoàn thiện tính năng Ảnh & Khoảnh khắc và Kèo Online (Phương án 1)

**Ngày lập**: 11/11/2026  
**Trạng thái**: Chờ duyệt (Pending Approval)  
**Tác giả**: Antigravity Pair Programmer & Người dùng  

---

## 1. Mục đích và Lý do (Intent & The Why)

- Đáp ứng đầy đủ các quyết định sản phẩm đã xác nhận tại [docs/ba/photo-memories/08-decisions.md](../../ba/photo-memories/08-decisions.md) (C-06 đến C-08) và bổ sung mới nhất tại [product-rules-2026-11-11.md](../../ba/product-rules-2026-11-11.md) (PC-05: Chụp camera và nhập ảnh từ thư viện máy; caption và pixel bất biến sau lưu).
- Cung cấp trải nghiệm mượt mà, trực quan, an toàn cho người dùng mobile:
  - Cho phép chụp ảnh tức thì hoặc chọn ảnh đẹp từ thư viện máy.
  - Phân loại và gắn ảnh vào Kèo Online hoặc giữ dòng cá nhân.
  - Quản lý khoảnh khắc cá nhân theo lưới và lịch thời gian với hiệu năng cuộn tối ưu.
- Hoàn thiện trải nghiệm Kèo Online kết nối backend Render song song với bảo toàn nguyên vẹn Kèo Trên máy (Local MMKV v1), kèm điều hướng chuyển đổi thuận tiện.
- Đảm bảo 100% chuẩn mực kỹ thuật: sạch lỗi ESLint, vượt qua toàn bộ unit test, không có cảnh báo runtime hay `act()`.

---

## 2. Mục tiêu, Phi mục tiêu và Ranh giới (Goals, Non-Goals, Anti-Goals)

### 2.1. Mục tiêu (Goals)
1. **Nhập ảnh từ máy (PC-05)**: Tích hợp `react-native-image-picker` (`launchImageLibrary`) trên màn hình `CameraHomeScreen`, cho phép người dùng chọn ảnh có sẵn ngoài camera.
2. **Quản lý file & metadata ảnh an toàn**:
   - Sao chép ảnh từ thư viện máy vào thư mục sandbox an toàn của ứng dụng (`photoFiles.ts`) để tránh mất ảnh khi cache hệ thống bị xóa.
   - Thêm trường `source: 'CAMERA' | 'IMPORTED'` vào model `LocalPhoto` (với default `'CAMERA'` cho dữ liệu cũ).
   - Giữ nguyên tắc bất biến: sau khi lưu, ảnh không sửa caption hoặc pixel; muốn đổi phải xóa và tạo lại.
3. **Tối ưu hóa hiệu năng (Smooth & Responsive)**:
   - Sử dụng `React.memo`, memoize callbacks và cấu hình `FlatList` tối ưu (`initialNumToRender`, `maxToRenderPerBatch`, `windowSize`, `removeClippedSubviews`) trên các màn hình Lịch/Lưới ảnh (`MemoriesScreen`, `RoomPhotoAlbum`).
   - Tối ưu hóa chuyển đổi camera trước/sau, tránh re-render thừa của VisionCamera.
4. **Trải nghiệm Kèo Online & Kèo Local liền mạch**:
   - Bổ sung nút chuyển đổi hai chiều rõ ràng: từ `OnlineRoomsScreen` sang `KeoHomeScreen` ("Phòng cũ trên thiết bị") và nút Back ("Về Kèo Online") trên header `KeoHomeScreen`.
   - Giữ nguyên vẹn khả năng đọc/ghi dữ liệu MMKV v1 của phòng local theo quy tắc `AGENTS.md`.
5. **Dọn dẹp chất lượng mã nguồn**:
   - Sửa dứt điểm 5 lỗi ESLint hiện có (`OnlineRoomForm.tsx`, `useOnlineRoom.ts`, `OnlineRoomsScreen.tsx`, `OnlineProvider.tsx`).
   - Đảm bảo `yarn typecheck`, `yarn test --runInBand`, `yarn lint` đều đạt 100% xanh sạch.

### 2.2. Phi mục tiêu (Non-Goals)
- Không can thiệp cấu trúc tab hoặc thêm Album vào màn hình `KeoRoomDetailScreen.tsx` (phòng local MMKV v1) để tránh nguy cơ hồi quy logic tính toán/kế hoạch trên máy.
- Không giả lập hoặc gọi các API backend ảo cho upload ảnh/album/DM khi server Render chưa có endpoint (theo đúng báo cáo [backend-gap-review.md](../integration/backend-gap-review.md)).
- Không xây dựng hệ thống chat riêng tư online khi chưa có API tin nhắn từ backend.

### 2.3. Ranh giới cấm (Anti-Goals)
- Không dùng danh tính client (như guestId, role local, tên hiển thị) để giả lập quyền authorization online.
- Không tự ý thu hồi hoặc xóa ảnh của người dùng trên máy nếu không có lệnh xóa chủ động từ người dùng.

---

## 3. Các phương án kỹ thuật đã cân nhắc (Approaches)

### Phương án 1 (ĐÃ CHỌN): Tích hợp Image Picker chuẩn hóa, hoàn thiện luồng Ảnh + Kèo Online song song, giữ vững an toàn dữ liệu Local
- Tích hợp `react-native-image-picker` song song với VisionCamera.
- Sao chép ảnh vào app sandbox, lưu trữ metadata trong `PhotoProvider`.
- Giữ tách bạch giữa Local Store (MMKV v1) và Online Store (Render API + SSE), bổ sung lối tắt chuyển đổi giao diện mượt mà.
- **Lý do chọn**: Đáp ứng chuẩn xác các quy tắc đã chốt (PC-05, C-06..C-08), tuyệt đối an toàn với dữ liệu local, giải quyết dứt điểm các lỗi lint/test và tối ưu hóa trải nghiệm người dùng.

### Phương án 2 (Bị loại): Hợp nhất cưỡng bức Local & Online Store
- Thử nghiệm gộp hai luồng lưu trữ vào một store chung.
- **Lý do loại**: Quá phức tạp, nguy cơ làm hỏng schema MMKV v1 hiện tại, vi phạm chỉ dẫn trong `AGENTS.md`.

### Phương án 3 (Bị loại): Chỉ sửa lỗi lint tối thiểu
- Chỉ thêm nút chọn ảnh và sửa 5 lỗi ESLint mà không tối ưu độ mượt hoặc liên kết giao diện giữa hai loại phòng.
- **Lý do loại**: Không đáp ứng yêu cầu "hoàn thiện bài bản và mượt mà" của người dùng.

---

## 4. Chi tiết kiến trúc và giải pháp (Architecture & Implementation Details)

### 4.1. Module Ảnh & Khoảnh khắc (`src/modules/keo/`)

#### Model & File Utils (`types/index.ts`, `PhotoProvider.tsx`, `photoFiles.ts`)
- Cập nhật kiểu `LocalPhoto`:
  ```typescript
  export type PhotoSource = 'CAMERA' | 'IMPORTED';
  export const photoSchema = z.object({
    id: z.string().uuid(),
    fileName: z.string().regex(/^[a-f0-9-]{36}\.jpg$/),
    ownerScope: z.string().min(1),
    caption: z.string(),
    createdAt: z.number(),
    width: z.number().positive(),
    height: z.number().positive(),
    intendedRoomId: z.string().optional(),
    personal: z.boolean(),
    status: z.literal('LOCAL_ONLY'),
    source: z.enum(['CAMERA', 'IMPORTED']).default('CAMERA'),
  });
  export type LocalPhoto = z.infer<typeof photoSchema>;
  ```
- Cập nhật `photoFiles.ts`:
  - Hàm `copyCameraPhoto` sao chép an toàn cả file chụp từ camera và file chọn từ thư viện (`content://` hoặc `file://`) vào sandbox app.
  - Xử lý dọn dẹp file tạm khi lưu thất bại.

#### Giao diện Camera & Chọn ảnh (`CameraHomeScreen.tsx`)
- Thêm nút "Chọn từ thư viện" (sử dụng icon `gallery` trong `controls`) cạnh nút chụp ảnh và nút đổi camera.
- Gọi `launchImageLibrary({ mediaType: 'photo', quality: 0.9, selectionLimit: 1 })`.
- Khi người dùng chọn ảnh:
  - Lấy URI, width, height từ asset trả về.
  - Điều hướng tới `RouteName.KeoPhotoPreview` với tham số `{ uri, width, height, roomId, capturedForScope, source: 'IMPORTED' }`.

#### Xem trước & Chi tiết ảnh (`PhotoScreens.tsx`)
- `PhotoPreviewScreen`:
  - Hiển thị nhãn nhỏ phân biệt: "Chụp trực tiếp" hoặc "Chọn từ máy".
  - Giữ nguyên các tùy chọn: Gắn kèo (chọn từ danh sách `online.rooms`), Checkbox "Giữ thêm trong dòng cá nhân", Ô nhập chú thích.
  - Lưu ảnh bất biến qua `photos.save(...)`.
- `MemoriesScreen` & `RoomPhotoAlbum`:
  - Tối ưu hóa danh sách bằng cấu hình `FlatList`:
    - `initialNumToRender={12}`
    - `maxToRenderPerBatch={12}`
    - `windowSize={5}`
    - `removeClippedSubviews={Platform.OS === 'android'}`
  - `PhotoTile` được bọc `React.memo` để tránh render lại không cần thiết khi cuộn danh sách.
- `PhotoDetailScreen`:
  - Hiển thị đầy đủ thông tin: ảnh chất lượng cao, chú thích, ngày giờ, nguồn ảnh (`Chụp camera` / `Chọn từ thư viện máy`), phòng gắn kèm (nếu có).
  - Nút "Xóa ảnh trên thiết bị" có hộp thoại xác nhận rõ ràng.

### 4.2. Trải nghiệm Kèo Online & Kèo Local
- **Phân định rõ ràng**:
  - `OnlineRoomScreen` sở hữu tab `Album` (`RoomPhotoAlbum`) và nút `Chụp cho kèo` với `roomId` chọn sẵn (đáp ứng C-08).
  - `KeoRoomDetailScreen` (phòng local) được giữ nguyên vẹn cấu trúc kế hoạch & chia tiền, không thêm tab Album để bảo toàn dữ liệu MMKV v1 theo `AGENTS.md`.
- **Điều hướng hai chiều thông suốt**:
  - `OnlineRoomsScreen.tsx`: Nút "Phòng cũ trên thiết bị" ở footer danh sách điều hướng sang `RouteName.KeoHome`.
  - `KeoHomeScreen.tsx`: Bổ sung nút Back ("Về Kèo Online") trên header góc trái cạnh logo Let's Play, giúp người dùng dễ dàng quay trở lại tab Kèo Online mà không bị kẹt màn hình.

### 4.3. Scope & Tính sẵn sàng Offline
- `sessionVault.guestId()` tự động khởi tạo và lưu trữ an toàn trong Keychain ngay trên thiết bị khi mở app, không phụ thuộc vào kết nối mạng.
- `online.scope` luôn có giá trị (`guest:<guestId>` hoặc `account:<id>`), do đó chức năng chụp ảnh, chọn ảnh máy và lưu vào thư viện cục bộ hoạt động trơn tru ngay cả khi offline hoàn toàn.

### 4.4. Khắc phục lỗi ESLint & Cảnh báo Tests
1. `src/modules/keo/components/OnlineRoomForm.tsx`:
   - Xóa bỏ import thừa `import { theme as t } from 'design/theme';`.
2. `src/modules/keo/hooks/useOnlineRoom.ts`:
   - Xóa `retry` khỏi dependency array của `useCallback` do không được sử dụng trực tiếp trong body của callback.
3. `src/modules/keo/screens/OnlineRoomsScreen.tsx`:
   - Chuẩn hóa dependency của `useFocusEffect(useCallback(...))`: bọc `refreshRooms` ổn định hoặc tham chiếu an toàn để không vi phạm `exhaustive-deps`.
4. `src/modules/keo/store/OnlineProvider.tsx`:
   - Trong `useEffect`: chụp biến `const currentEpoch = epoch.current;` trước khi trả về cleanup function.
5. `src/modules/keo/__tests__/roomSession.test.tsx`:
   - Bọc các lệnh cập nhật state trong `act(() => { ... })` để loại bỏ các cảnh báo console log trong quá trình chạy test.

---

## 5. Kế hoạch kiểm chứng (Verification Plan)
- **Kiểm tra kiểu dữ liệu**: `yarn typecheck` -> phải kết thúc với 0 lỗi.
- **Kiểm thử tự động**: `yarn test --runInBand` -> toàn bộ 6 test suites (23+ tests) phải PASS, không còn cảnh báo `act()`.
- **Kiểm tra linter**: `yarn lint` -> phải PASS với 0 lỗi và 0 cảnh báo.
- **Kiểm tra hành vi**:
  - Chọn ảnh từ thư viện máy -> mở preview -> lưu thành công -> hiển thị trong Thư viện Thời gian và Album kèo.
  - Chụp ảnh từ camera -> mở preview -> lưu thành công -> hiển thị đúng nguồn `CAMERA`.
  - Chuyển đổi giữa Kèo Local và Kèo Online mượt mà hai chiều.
