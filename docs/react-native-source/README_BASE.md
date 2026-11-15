# HƯỚNG DẪN KIẾN TRÚC BASE & BLUEPRINT (REACT NATIVE FEATURE-DRIVEN)

Tài liệu này giải thích toàn bộ kiến trúc nền tảng (**Base Architecture**) của dự án `react_native_feature_module`. Dự án đã được:
1. **Tách biệt hoàn toàn khỏi backend cũ** (không gọi bất kỳ API nào tới server ERP cũ `dev-alp.vdx.vn` hay `erp.hongvugroup.com`).
2. **Tích hợp toàn diện hạ tầng Backend theo tài liệu `BACKEND_INFO.md`**:
   - URL chính: `https://templetezuu.netlify.app/api/v1/`
   - Mock Test URL: `https://jsonplaceholder.typicode.com`
   - Quản lý Token: Mã hóa phần cứng qua `react-native-keychain` với bộ `Tokens` (`accessToken`, `refreshToken`, `expiresIn`, `refreshExpiresIn`).
   - Chuẩn đóng gói: Envelope `BaseResponse<T, M>`.
   - Feature Module mẫu chuẩn: **Quản lý Sản phẩm (`product`)** (List, Detail, RESTful API và Offline Mock Fallback).
   - Module thử nghiệm: **JSONPlaceholder Users (`JsonPlaceholderUsers`)**.

Dự án hiện tại đạt trạng thái **0 lỗi TypeScript (`yarn tsc --noEmit` PASS 100%)** và **0 lỗi ESLint**.

---

## 1. Bản đồ tổng thể cấu trúc thư mục (Folder Structure)

```text
react_native_feature_module/
├── App.tsx                     # Entry point khởi tạo toàn bộ Provider (Tamagui, SWR, Context, Gesture, Portals)
├── tamagui.config.ts           # Cấu hình Design System, Tokens (màu sắc, spacing, font)
├── babel.config.js             # Cấu hình Path Aliases và Plugins
├── tsconfig.json               # Cấu hình TypeScript compiler & Path mappings
├── .env                        # Môi trường kết nối Netlify Backend (https://templetezuu.netlify.app/api/v1/)
├── .env.dev                    # Môi trường dev test (https://jsonplaceholder.typicode.com)
├── .env.example                # File môi trường mẫu an toàn khi push git
│
├── src/
│   ├── assets/                 # Vector SVGs (IcHome, IcUser, IcDocument...) và hình ảnh tĩnh
│   ├── common/
│   │   ├── model/              # Interface dữ liệu (BaseResponse, Tokens)
│   │   └── styles/             # Global styles dùng chung
│   │
│   ├── components/
│   │   ├── base/               # UI Nguyên tử (Atomic Design): Button, Input, Form, Modal, Text, FlatList...
│   │   ├── file-upload/        # Hook & Component upload file
│   │   └── Screen.tsx, Header.tsx, Toast.tsx, EmptyBox.tsx, SearchInput.tsx
│   │
│   ├── configs/
│   │   ├── navigation.ts       # Định nghĩa RouteName và AppStackParamList (Type-safe Navigation)
│   │   ├── storage.ts          # Định nghĩa cấu trúc lưu trữ cục bộ với Zod validation
│   │   ├── contansts.tsx       # Hằng số toàn cục
│   │   └── env.ts              # Biến môi trường (apiUrl, host)
│   │
│   ├── hooks/
│   │   ├── api.ts              # Core API Client (Fetch + SWR + Token injection + 401 handling)
│   │   ├── useAppContext.ts    # React Context quản trị User, Token, Theme toàn app
│   │   ├── useStorage.ts       # Hook đọc/ghi dữ liệu cục bộ MMKV với Zod validation
│   │   └── useUnauthorized.ts # Xử lý khi bị hết hạn phiên / 401
│   │
│   ├── navigators/
│   │   ├── AppNavigator.tsx    # Cây điều hướng chính (NativeStack: Auth + Tabs + Feature screens)
│   │   ├── BottomTabBar.tsx    # Thanh điều hướng Tab dưới (Home, ProductList, Setting)
│   │   └── navigationUtilities.ts # Helper navigate(), resetRoot(), goBack() dùng toàn cục
│   │
│   ├── utils/
│   │   ├── client.ts           # Lớp HTTP Client trừu tượng với URL normalization và timeout 10s
│   │   ├── tokenStorage.ts     # Keychain Keystore (saveTokens, getAccessToken, clearTokens)
│   │   ├── AppError.ts         # Chuẩn hóa lỗi AppError (http, network, timeout, cancelled)
│   │   ├── scaling-utils.ts    # scale(), verticalScale(), horizontalScale() (Responsive UI)
│   │   └── utils.ts            # Các tiện ích định dạng ngày tháng, chuỗi, số
│   │
│   └── modules/                # CÁC MODULE TÍNH NĂNG (Feature-Driven)
│       ├── home/               # Màn hình Trang chủ (Dashboard hiển thị module mẫu và phím tắt)
│       ├── user/               # Luồng Xác thực (Login, Register, ForgotPassword, Setting/Profile, Đổi mật khẩu, JSONPlaceholder)
│       └── product/            # KHUÔN MẪU TÍNH NĂNG CHUẨN (List, Detail, productApi, productMockApi)
```

---

## 2. Các tầng cốt lõi của Base (Core Architecture Layers)

### Tầng 1: Cấu hình Môi trường & Mạng (Network & Environment Layer)
- **File**: `.env`, `.env.dev`, `src/configs/env.ts`, `src/utils/client.ts`, `src/hooks/api.ts`
- **Cơ chế**:
  - `apiUrl`: Lấy từ biến môi trường `API_URL` (`https://templetezuu.netlify.app/api/v1/`).
  - `Client.request`: Tự động chuẩn hóa URL (chặn duplicate slashes), timeout mặc định 10.000ms.
  - Hỗ trợ cả URL tương đối (`/products`, `/auth/login`) lẫn URL tuyệt đối (`https://jsonplaceholder.typicode.com/users`).

### Tầng 2: Quản lý Token An Toàn (Secure Token Management)
- **File**: `src/utils/tokenStorage.ts`, `src/common/model/tokens.ts`
- **Cơ chế**:
  - Sử dụng **`react-native-keychain`** để mã hóa an toàn trên phần cứng thiết bị (Android Keystore & iOS Keychain).
  - Lưu toàn bộ cấu trúc:
    ```ts
    export interface Tokens {
      accessToken: string;
      refreshToken: string;
      expiresIn?: number;
      refreshExpiresIn?: number;
    }
    ```
  - `api.ts` tự động đọc `accessToken` và gắn vào header:
    ```http
    Authorization: Bearer <accessToken>
    ```
  - Khi người dùng đăng xuất: `tokenStorage.clearTokens()` xóa sạch dữ liệu khỏi Keychain.

### Tầng 3: Chuẩn đóng gói dữ liệu phản hồi (Response Envelope)
- **File**: `src/common/model/base-response.ts`
- **Interface**:
  ```ts
  export interface BaseResponse<T = any, M = any> {
    data?: T;
    meta?: M;
    message?: string;
    success?: boolean;
    code?: number;
  }
  ```

---

## 3. Khuôn mẫu Module Tính năng Chuẩn (Feature Blueprint): `product`

Module `src/modules/product` là khuôn mẫu chuẩn để phát triển các tính năng tiếp theo trong ứng dụng:

```text
src/modules/product/
├── types.ts                     # Định nghĩa Product, Category, Price, Inventory, Media
├── api/
│   ├── productApi.ts            # getProductsApi(), getProductByIdApi() (RESTful GET)
│   └── productMockApi.ts        # Dữ liệu Mock offline khi chưa có internet/backend dev
├── screens/
│   ├── ProductListScreen.tsx    # Giao diện danh sách sản phẩm (SearchInput, FlatList, pull-to-refresh)
│   └── ProductDetailScreen.tsx  # Giao diện chi tiết sản phẩm (Hero image, specs, add to cart)
└── index.ts                     # Thư viện xuất gọn gàng
```

### Cách gọi API trong màn hình:
```tsx
import useSWR from 'swr';
import { getProductsApi } from 'modules/product';

export const MyComponent = () => {
  const { data: products, isLoading, mutate } = useSWR('products_list', getProductsApi);
  // ...
};
```

---

## 4. Kiểm thử & Chạy ứng dụng

1. **Kiểm tra TypeScript**:
   ```bash
   yarn tsc --noEmit
   ```
2. **Kiểm tra Linter**:
   ```bash
   npx eslint . --quiet
   ```
3. **Chạy Metro Bundler**:
   ```bash
   yarn start
   ```
4. **Build Android**:
   ```bash
   npx react-native run-android
   ```
