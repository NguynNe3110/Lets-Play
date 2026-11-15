# Kế hoạch triển khai hoàn thiện tính năng Ảnh & Khoảnh khắc và Kèo Online (Phương án 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện tính năng Ảnh & Khoảnh khắc (chụp camera + chọn ảnh máy theo PC-05, lưu trữ bất biến và xem mượt mà) song song với luồng Kèo Online và kết nối hai chiều với Kèo Local, đạt 100% sạch lỗi lint và test.

**Architecture:** Bổ sung `react-native-image-picker` tích hợp vào luồng chụp và lưu trữ cục bộ có tag nguồn (`CAMERA` / `IMPORTED`) trong `PhotoProvider`; tối ưu hóa hiệu năng render danh sách ảnh với `React.memo` và `FlatList` windowing; hoàn thiện nút quay lại Kèo Online trên `KeoHomeScreen`; khắc phục triệt để 5 lỗi ESLint và cảnh báo `act()` trong test.

**Tech Stack:** React Native 0.79, React 19, TypeScript, `react-native-vision-camera`, `react-native-image-picker`, `react-native-mmkv`, `react-native-fs`, Zod, Jest.

**Spec:** [`docs/superpowers/specs/2026-11-11-photo-memories-and-online-flow-design.md`](../specs/2026-11-11-photo-memories-and-online-flow-design.md)

## Global Constraints

- React Native 0.79, React 19; không thay đổi phiên bản dependencies trong `package.json`.
- Giữ nguyên vẹn khả năng đọc dữ liệu MMKV v1 của phòng local (`let-play-rooms`); không sửa schema migration phòng local.
- Không dùng danh tính client (`guestId`, role local) làm quyền online; không giả mạo backend API khi chưa có endpoint.
- Quyền sở hữu và chú thích ảnh bất biến sau lưu: không cho sửa caption hoặc pixel; đổi thì xóa và tạo lại.
- Toàn bộ lệnh kiểm tra: `yarn typecheck`, `yarn test --runInBand`, `yarn lint` phải đạt 100% không lỗi.

## Review Focus

1. **Khả năng tương thích ngược của dữ liệu ảnh cũ**: Ảnh v1 đã lưu trong MMKV trước đó không có trường `source` phải tự động nhận mặc định `'CAMERA'` mà không gây crash `zod.parse`.
2. **Quyền truy cập thư viện ảnh trên thiết bị**: Khi người dùng từ chối cấp quyền thư viện ảnh hoặc hủy chọn ảnh trong picker, app phải xử lý êm thuận, không crash và hiển thị thông báo nếu cần.
3. **Độ an toàn của file URI**: File ảnh chọn từ thư viện máy (`content://` trên Android hoặc `ph://` trên iOS) phải được copy thành công vào thư mục lưu trữ riêng của app (`photoDirectory()`) trước khi lưu record; nếu copy lỗi thì hủy thao tác lưu sạch sẽ.
4. **Không bị kẹt màn hình khi chuyển giữa Kèo Online và Kèo Local**: Người dùng di chuyển từ `OnlineRoomsScreen` -> `KeoHomeScreen` luôn có nút quay lại `OnlineRoomsScreen`.
5. **Clean test output**: Chạy `yarn test --runInBand` không in bất kỳ warning `not wrapped in act(...)` nào ra console.

---

### Task 1: Model & Storage Layer cho nguồn ảnh (`PhotoProvider.tsx` & `photoFiles.ts`)

**Files:**
- Create: `src/modules/keo/__tests__/photoSource.test.ts`
- Modify: `src/modules/keo/store/PhotoProvider.tsx`
- Modify: `src/modules/keo/utils/photoFiles.ts`

**Interfaces:**
- Consumes: `sessionVault` từ `src/modules/keo/api/credentials.ts`, `react-native-mmkv`
- Produces: `LocalPhoto` với trường `source: 'CAMERA' | 'IMPORTED'`, `photos.save(input: Capture)` nhận `source?: PhotoSource`

- [ ] **Step 1: Viết test kiểm tra schema backward compatibility và lưu ảnh với source**

```typescript
// src/modules/keo/__tests__/photoSource.test.ts
import { photoSchema } from '../store/PhotoProvider';

describe('Photo schema and source tagging', () => {
  it('parses legacy photo record without source as CAMERA by default', () => {
    const legacy = {
      id: '11111111-1111-4111-8111-111111111111',
      fileName: '11111111-1111-4111-8111-111111111111.jpg',
      ownerScope: 'guest:123',
      caption: 'Ảnh cũ',
      createdAt: 1700000000,
      width: 1000,
      height: 1000,
      personal: true,
      status: 'LOCAL_ONLY',
    };
    const parsed = photoSchema.parse(legacy);
    expect(parsed.source).toBe('CAMERA');
  });

  it('accepts IMPORTED as photo source', () => {
    const imported = {
      id: '22222222-2222-4222-8222-222222222222',
      fileName: '22222222-2222-4222-8222-222222222222.jpg',
      ownerScope: 'guest:123',
      caption: 'Ảnh chọn từ máy',
      createdAt: 1700000000,
      width: 1200,
      height: 900,
      personal: true,
      status: 'LOCAL_ONLY',
      source: 'IMPORTED',
    };
    const parsed = photoSchema.parse(imported);
    expect(parsed.source).toBe('IMPORTED');
  });
});
```

- [ ] **Step 2: Chạy test để xác nhận test thất bại (do `photoSchema` chưa có `source`)**

Chạy: `yarn jest src/modules/keo/__tests__/photoSource.test.ts`
Kỳ vọng: FAIL với lỗi validation `photoSchema.parse`

- [ ] **Step 3: Cập nhật `photoSchema` và `PhotoProvider.tsx`**

Trong `src/modules/keo/store/PhotoProvider.tsx`:
- Export `photoSchema`
- Thêm `source: z.enum(['CAMERA', 'IMPORTED']).default('CAMERA')` vào `photoSchema`.
- Mở rộng type `Capture`:
  ```typescript
  export type PhotoSource = 'CAMERA' | 'IMPORTED';
  type Capture = { uri: string; width: number; height: number; caption: string; roomId?: string; personal: boolean; source?: PhotoSource };
  ```
- Trong hàm `save`: truyền `source: input.source || 'CAMERA'` vào object khi parse `photoSchema.parse(...)`.

- [ ] **Step 4: Chạy lại test để xác nhận test PASS**

Chạy: `yarn jest src/modules/keo/__tests__/photoSource.test.ts`
Kỳ vọng: PASS

- [ ] **Step 5: Commit task 1**

```bash
git add src/modules/keo/__tests__/photoSource.test.ts src/modules/keo/store/PhotoProvider.tsx
git commit -m "feat(photo): add source tagging and backward compatibility for local photos"
```

---

### Task 2: Tích hợp Image Picker vào `CameraHomeScreen.tsx`

**Files:**
- Modify: `src/modules/keo/screens/CameraHomeScreen.tsx`
- Modify: `src/configs/navigation.ts`

**Interfaces:**
- Consumes: `launchImageLibrary` từ `react-native-image-picker`, `navigate` từ `navigators`
- Produces: Mở bộ chọn ảnh, xử lý kết quả, điều hướng tới `RouteName.KeoPhotoPreview` với tham số `{ uri, width, height, roomId, capturedForScope, source: 'IMPORTED' }`.

- [ ] **Step 1: Cập nhật `AppStackParamList[RouteName.KeoPhotoPreview]` trong `navigation.ts`**

Trong `src/configs/navigation.ts`:
Thêm trường `source?: 'CAMERA' | 'IMPORTED'` vào param list của `RouteName.KeoPhotoPreview`.

- [ ] **Step 2: Cập nhật `CameraHomeScreen.tsx` tích hợp nút chọn ảnh từ máy**

Trong `src/modules/keo/screens/CameraHomeScreen.tsx`:
- Import `launchImageLibrary` từ `react-native-image-picker`.
- Tạo hàm `pickFromGallery`:
  ```typescript
  const pickFromGallery = async () => {
    if (busy || !online.scope) return;
    try {
      const response = await launchImageLibrary({
        mediaType: 'photo',
        quality: 0.9,
        selectionLimit: 1,
      });
      if (response.didCancel || !response.assets?.[0]?.uri) return;
      const asset = response.assets[0];
      const capturedForScope = online.scope;
      if (mounted.current && latest.current.focused && latest.current.scope === capturedForScope) {
        navigate(RouteName.KeoPhotoPreview, {
          uri: asset.uri,
          width: asset.width || 1080,
          height: asset.height || 1080,
          roomId,
          capturedForScope,
          source: 'IMPORTED',
        });
      }
    } catch {
      if (mounted.current) setError('Không mở được thư viện ảnh. Hãy thử lại.');
    }
  };
  ```
- Thêm nút IconButton với icon `"gallery"` và nhãn `"Chọn từ thư viện"` vào vùng điều khiển (`styles.controls`) bên cạnh nút chụp ảnh.

- [ ] **Step 3: Kiểm tra biên dịch TypeScript**

Chạy: `yarn typecheck`
Kỳ vọng: PASS

- [ ] **Step 4: Commit task 2**

```bash
git add src/configs/navigation.ts src/modules/keo/screens/CameraHomeScreen.tsx
git commit -m "feat(camera): integrate gallery image picker for moment creation"
```

---

### Task 3: Cập nhật Xem trước, Chi tiết & Tối ưu hiệu năng (`PhotoScreens.tsx`)

**Files:**
- Modify: `src/modules/keo/screens/PhotoScreens.tsx`

**Interfaces:**
- Consumes: `LocalPhoto`, `usePhotos`, `photoUri`
- Produces: `PhotoPreviewScreen` hỗ trợ badge nguồn và lưu nguồn ảnh; `PhotoDetailScreen` hiển thị nguồn; `PhotoTile` bọc `React.memo` và `FlatList` cấu hình tối ưu.

- [ ] **Step 1: Cập nhật `PhotoPreviewScreen`**

Trong `src/modules/keo/screens/PhotoScreens.tsx`:
- Lấy `source = params.source || 'CAMERA'` từ route params.
- Hiển thị badge nhỏ: `source === 'IMPORTED' ? 'Ảnh từ thư viện máy' : 'Chụp trực tiếp'`.
- Khi ấn "Lưu ảnh trên thiết bị", gọi `photos.save({ ..., source })`.

- [ ] **Step 2: Cập nhật `PhotoDetailScreen`**

Trong `PhotoDetailScreen`:
- Đọc `photo.source` (mặc định `'CAMERA'` nếu undefined).
- Hiển thị nhãn nguồn: `photo.source === 'IMPORTED' ? 'Chọn từ thư viện máy' : 'Chụp bằng camera'`.

- [ ] **Step 3: Tối ưu hoá `PhotoTile` và `FlatList` trong `MemoriesScreen` & `RoomPhotoAlbum`**

- Bọc `PhotoTile` bằng `React.memo(PhotoTile)`.
- Bổ sung các props hiệu năng cho `FlatList` của `MemoriesScreen` và `RoomPhotoAlbum`:
  - `initialNumToRender={12}`
  - `maxToRenderPerBatch={12}`
  - `windowSize={5}`
  - `removeClippedSubviews={Platform.OS === 'android'}`

- [ ] **Step 4: Kiểm tra biên dịch TypeScript**

Chạy: `yarn typecheck`
Kỳ vọng: PASS

- [ ] **Step 5: Commit task 3**

```bash
git add src/modules/keo/screens/PhotoScreens.tsx
git commit -m "perf(photo): optimize photo list rendering and display photo source badge"
```

---

### Task 4: Hoàn thiện điều hướng hai chiều Kèo Online & Kèo Local (`KeoHomeScreen.tsx`)

**Files:**
- Modify: `src/modules/keo/screens/KeoHomeScreen.tsx`

**Interfaces:**
- Consumes: `navigate` từ `navigators`, `RouteName`
- Produces: Nút "Về Kèo Online" trên header `KeoHomeScreen` cho phép quay lại `OnlineRoomsScreen`.

- [ ] **Step 1: Bổ sung nút quay lại trên header `KeoHomeScreen.tsx`**

Trong `src/modules/keo/screens/KeoHomeScreen.tsx`:
- Trong `<View style={styles.header}>`, thêm nút quay lại bên trái logo:
  ```tsx
  <Pressable
    accessibilityRole="button"
    accessibilityLabel="Về danh sách Kèo Online"
    style={styles.iconButton}
    onPress={() => navigate(RouteName.KeoMain, { screen: 'Rooms' })}
  >
    <PlayIcon name="back" size={22} color={t.green} />
  </Pressable>
  ```
- Giữ vững thương hiệu `let's play.` và nút info hiện có.

- [ ] **Step 2: Kiểm tra biên dịch TypeScript**

Chạy: `yarn typecheck`
Kỳ vọng: PASS

- [ ] **Step 3: Commit task 4**

```bash
git add src/modules/keo/screens/KeoHomeScreen.tsx
git commit -m "feat(navigation): add return button to online rooms from local rooms home screen"
```

---

### Task 5: Sửa dứt điểm 5 lỗi ESLint & Cảnh báo Tests

**Files:**
- Modify: `src/modules/keo/components/OnlineRoomForm.tsx`
- Modify: `src/modules/keo/hooks/useOnlineRoom.ts`
- Modify: `src/modules/keo/screens/OnlineRoomsScreen.tsx`
- Modify: `src/modules/keo/store/OnlineProvider.tsx`
- Modify: `src/modules/keo/__tests__/roomSession.test.tsx`

- [ ] **Step 1: Sửa lỗi unused import trong `OnlineRoomForm.tsx`**

Xóa bỏ `theme as t` không sử dụng ở dòng 5 trong `src/modules/keo/components/OnlineRoomForm.tsx`.

- [ ] **Step 2: Sửa lỗi dependency hook trong `useOnlineRoom.ts`**

Trong `src/modules/keo/hooks/useOnlineRoom.ts`:
Xóa bỏ `retry` ở dependency array của `useCallback` (dòng 44):
Đổi thành: `}, [id, refreshRoom, eventToken]);`

- [ ] **Step 3: Sửa lỗi dependency hook trong `OnlineRoomsScreen.tsx`**

Trong `src/modules/keo/screens/OnlineRoomsScreen.tsx`:
Tại dòng 34:
```typescript
const refreshRooms = online.refreshRooms;
useFocusEffect(useCallback(() => { void refreshRooms(); }, [refreshRooms]));
```

- [ ] **Step 4: Sửa lỗi ref cleanup trong `OnlineProvider.tsx`**

Tại dòng 39 của `src/modules/keo/store/OnlineProvider.tsx`:
```typescript
useEffect(() => {
  mounted.current = true;
  const currentEpoch = epoch.current;
  return () => {
    mounted.current = false;
    epoch.current = currentEpoch + 1;
  };
}, []);
```

- [ ] **Step 5: Bọc `act(...)` trong `roomSession.test.tsx`**

Trong `src/modules/keo/__tests__/roomSession.test.tsx`:
Import `act` từ `react-test-renderer` và bọc các lệnh gọi cập nhật state (`result.current.setStartTime(...)`, `result.current.submitVotes(...)`, v.v.) bên trong `act(() => { ... })`.

- [ ] **Step 6: Kiểm tra chạy linter `yarn lint`**

Chạy: `yarn lint --quiet`
Kỳ vọng: 0 errors!

- [ ] **Step 7: Commit task 5**

```bash
git add src/modules/keo/components/OnlineRoomForm.tsx src/modules/keo/hooks/useOnlineRoom.ts src/modules/keo/screens/OnlineRoomsScreen.tsx src/modules/keo/store/OnlineProvider.tsx src/modules/keo/__tests__/roomSession.test.tsx
git commit -m "fix(lint): resolve react-hooks exhaustive-deps, unused imports and test act warnings"
```

---

### Task 6: Kiểm chứng toàn diện (Full Verification)

**Files:**
- Toàn bộ repo

- [ ] **Step 1: Kiểm tra biên dịch TypeScript**

Chạy: `yarn typecheck`
Kỳ vọng: PASS không có bất kỳ lỗi nào.

- [ ] **Step 2: Chạy toàn bộ Unit Tests**

Chạy: `yarn test --runInBand`
Kỳ vọng: 7/7 test suites PASS (25+ tests), không còn cảnh báo `act()` trong log.

- [ ] **Step 3: Kiểm tra toàn bộ Linter**

Chạy: `yarn lint`
Kỳ vọng: 0 lỗi.

- [ ] **Step 4: Kiểm tra trạng thái Git**

Chạy: `git status`
Kỳ vọng: Làm việc trên cây git sạch sẽ, các commit được ghi nhận rõ ràng.
