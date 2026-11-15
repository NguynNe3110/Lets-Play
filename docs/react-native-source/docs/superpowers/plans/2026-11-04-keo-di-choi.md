# Kèo Đi Chơi (Outing Planner & Expense Splitter) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện toàn diện module `keo` cho ứng dụng "Kèo đi chơi" theo tài liệu BA và prototype trong thư mục `context` trên nền tảng React Native (Tamagui), bao gồm 4 chu trình phòng (Góp ý, Bình chọn, Sắp lịch, Đã chốt) và tab Chia tiền nhóm thông minh.

**Architecture:** Áp dụng kiến trúc Feature-Driven Module (`src/modules/keo`) độc lập, tái sử dụng các base component (Tamagui View/Text/Button, Modal, Input). Tách bạch lớp xử lý thuật toán tiền tệ và điểm số (`moneyUtils`, `scoringUtils`) cùng hai custom hooks (`useKeoRoom`, `useKeoMoney`) để quản lý trạng thái, đi kèm bộ UI components chuyên biệt và hai màn hình chính (`KeoHomeScreen`, `KeoRoomDetailScreen`).

**Tech Stack:** React Native 0.79, TypeScript, Tamagui (`@tamagui/core`), React Navigation, Node/Jest testing.

**Spec:** `context/BA.md`, `context/money.js`, `context/mockup.html`

## Global Constraints

- Mọi tệp code CHỈ được viết bên trong workspace `d:\AppData\Code\ReactNative\let_play`, tuyệt đối KHÔNG đụng đến `mobile-hrm`.
- Màu sắc chủ đạo: Xanh trầm rừng `#245C49`, nền kem `#F7F6F2`, điểm nhấn đất nung `#AE422C`, nền thẻ trắng `#FFFFFF`, đường kẻ `#E3E6DF`.
- Tác giả đề xuất phải được ẩn danh hoàn toàn (AC03). Đề xuất của chính người dùng đánh dấu "Của bạn · chỉ bạn thấy".
- Điểm gợi ý hoạt động theo BR01: `2 × Rất muốn + 1 × Đi cùng được`. Không quy phiếu thiếu thành phản đối (BR02).
- Chia đều tiền tệ phải xử lý số dư lẻ nguyên vẹn đến từng đồng VND theo thứ tự thành viên (AC17, BR08).
- Số dư nhóm luôn có tổng bằng 0. Tiền ghi nhận chuyển đi phải chờ người nhận xác nhận mới cập nhật số dư (AC22).

## Review Focus

- Chia tiền lẻ: Khoản 100đ chia 3 người phải chia chính xác 34đ, 33đ, 33đ, không phát sinh hay làm mất tiền.
- Bù trừ công nợ: Thuật toán tham lam (greedy balance netting) tạo danh sách ai trả ai ngắn gọn, không sót dư nợ.
- Bình chọn chưa hoàn tất: Nút hoàn tất bình chọn chỉ cho phép lưu khi đã đánh giá đủ tất cả các thẻ hoạt động.
- Sắp xếp thứ tự: Hỗ trợ nút Lên / Xuống mượt mà, đổi vị trí tức thì và đồng bộ với Borda score.
- Đổi giai đoạn an toàn: Chỉ Chủ phòng mới có quyền chuyển giai đoạn hoặc chốt kế hoạch sau khi xác nhận lưu ý.

---

## File Structure Map

```text
src/modules/keo/
├── types/
│   └── index.ts                 # Toàn bộ type definitions (Room, Idea, Vote, Expense, Transfer, Debt)
├── data/
│   └── mockData.ts              # Fixture mẫu: 6 thành viên, 4 hoạt động, 2 khoản chi, 3 ghi chú
├── utils/
│   ├── moneyUtils.ts            # Thuật toán chia tiền, tính balances, tính debts bù trừ
│   └── scoringUtils.ts          # Tính điểm tally (2/1/0), điểm Borda, timeline chặng
├── hooks/
│   ├── useKeoRoom.ts            # Quản lý vòng đời phòng, bình chọn, đề xuất, sắp thứ tự
│   └── useKeoMoney.ts           # Quản lý danh sách chi tiêu, tạo khoản chi, xác nhận thanh toán
├── components/
│   ├── PhaseSteps.tsx           # Thanh tiến trình 4 giai đoạn (Góp ý -> Bình chọn -> Sắp lịch -> Đã chốt)
│   ├── IdeaCard.tsx             # Thẻ hiển thị ý tưởng theo từng phase (hưởng ứng, vote 3 mức, hoặc checkbox shortlist)
│   ├── RankRow.tsx              # Hàng danh sách sắp thứ tự (nút lên/xuống) hoặc timeline chặng (checkbox xong)
│   ├── DebtRow.tsx              # Hàng công nợ ai cần trả ai (nút Ghi nhận / Chờ xác nhận)
│   ├── ExpenseCard.tsx          # Thẻ thông tin khoản chi tiêu (người trả, số người, số tiền)
│   ├── IdeaFormModal.tsx        # Modal thêm / sửa đề xuất ẩn danh
│   ├── ExpenseFormModal.tsx     # Modal thêm khoản chi (chia đều hoặc chia riêng)
│   ├── ExpenseDetailModal.tsx   # Modal xem chi tiết phân bổ một khoản chi
│   ├── TransferModal.tsx        # Modal ghi nhận đã chuyển và người nhận bấm xác nhận
│   ├── NotesModal.tsx           # Modal thêm ghi chú lưu ý chung cho nhóm
│   ├── InviteModal.tsx          # Modal sao chép mã phòng và link mời KEO726
│   ├── SealPlanModal.tsx        # Modal xác nhận chốt kế hoạch dành cho Chủ phòng
│   └── index.ts                 # Re-export components
├── screens/
│   ├── KeoHomeScreen.tsx        # Màn hình trang chủ Kèo (Tạo phòng, Nhập mã KEO726, Phòng gần đây)
│   ├── KeoRoomDetailScreen.tsx  # Màn hình chi tiết phòng (Thanh Demo, Hero, Tab Kế hoạch & Chia tiền)
│   └── index.ts                 # Export screens
├── __tests__/
│   ├── money.test.ts            # Test thuật toán chia tiền và bù trừ số dư
│   └── scoring.test.ts          # Test tính điểm vote và timeline
└── index.ts                     # Entry point module keo
```

---

## Task Structure

### Task 1: Type Definitions & Mock Fixtures

**Files:**
- Create: `src/modules/keo/types/index.ts`
- Create: `src/modules/keo/data/mockData.ts`

**Interfaces:**
- Produces: `Phase`, `Role`, `Idea`, `VoteValue`, `Expense`, `Transfer`, `Debt`, `RoomState`, `MEMBERS`, `INITIAL_IDEAS`, `INITIAL_EXPENSES`, `INITIAL_NOTES`

- [ ] **Step 1: Tạo types định nghĩa dữ liệu đầy đủ theo BA.md**
  - Định nghĩa `Phase = 'ideas' | 'vote' | 'order' | 'plan'`.
  - Định nghĩa `Role = 'host' | 'member'`.
  - Định nghĩa `Idea` (id, title, note, cost, min, type, mine, base, authorName).
  - Định nghĩa `VoteValue = 2 | 1 | 0`.
  - Định nghĩa `Expense` (id, title, amount, payer, participants, shares, method).
  - Định nghĩa `Transfer` (id, from, to, amount, status: 'pending' | 'confirmed').
  - Định nghĩa `Debt` (from, to, amount).

- [ ] **Step 2: Tạo mockData fixture chuẩn**
  - Khởi tạo 6 thành viên: `['An', 'Minh', 'Linh', 'Hà', 'Nam', 'Vy']`.
  - Khởi tạo 4 ý tưởng mẫu ban đầu: Lẩu (150k, 90m), Nhà sách (chưa rõ, 30m), Chụp ảnh (0đ, 15m), Cà phê (55k, 60m).
  - Khởi tạo 2 khoản chi mẫu: Lẩu tối thứ Bảy (900k, payer An), Cà phê (330k, payer Minh).
  - Khởi tạo 3 ghi chú lưu ý ban đầu (Ngân sách 250k, về trước 22:00, lưu ý hải sản).

- [ ] **Step 3: Commit**
  - `git add src/modules/keo/types src/modules/keo/data`
  - `git commit -m "feat(keo): define types and mock fixtures for outing planner"`

---

### Task 2: Pure Utilities (Money & Scoring) with Unit Tests

**Files:**
- Create: `src/modules/keo/utils/moneyUtils.ts`
- Create: `src/modules/keo/utils/scoringUtils.ts`
- Create: `src/modules/keo/__tests__/money.test.ts`
- Create: `src/modules/keo/__tests__/scoring.test.ts`

**Interfaces:**
- Produces: `equalShares()`, `calculateBalances()`, `calculateDebts()`, `tally()`, `calculateScore()`, `calculateTimeline()`, `formatCurrency()`

- [ ] **Step 1: Viết test cho logic tiền tệ và điểm số**
  - Test `equalShares(100, [0, 1, 2], 6)` -> `[34, 33, 33, 0, 0, 0]`, tổng = 100.
  - Test `calculateBalances` với 900.000đ (An trả) và 330.000đ (Minh trả), cả 6 người dùng: An nhận +695.000đ, Minh nhận +125.000đ, 4 bạn còn lại nợ -205.000đ. Tổng = 0.
  - Test `calculateDebts`: sinh ra danh sách bù trừ hợp lý, chuyển tiền pending không làm đổi số dư, confirmed thì trừ nợ.
  - Test `calculateScore`: `2 * Rất muốn + 1 * Đi cùng được`.
  - Test `calculateTimeline`: chặng đầu bắt đầu 18:00, cộng phút chặng + 15 phút di chuyển.

- [ ] **Step 2: Chạy kiểm chứng logic bằng Node**
  - Chạy runner node script để xác thực tất cả assertions đều PASS.

- [ ] **Step 3: Hoàn thiện `moneyUtils.ts` và `scoringUtils.ts`**
  - Cài đặt thuật toán chia tiền làm tròn chính xác từng đồng lẻ.
  - Cài đặt thuật toán bù trừ công nợ.
  - Cài đặt tính điểm và format VND `Number(n).toLocaleString('vi-VN') + 'đ'`.

- [ ] **Step 4: Commit**
  - `git add src/modules/keo/utils src/modules/keo/__tests__`
  - `git commit -m "feat(keo): implement money and scoring calculation algorithms with tests"`

---

### Task 3: State Management Custom Hooks (`useKeoRoom` & `useKeoMoney`)

**Files:**
- Create: `src/modules/keo/hooks/useKeoRoom.ts`
- Create: `src/modules/keo/hooks/useKeoMoney.ts`
- Create: `src/modules/keo/hooks/index.ts`

**Interfaces:**
- Consumes: types, `mockData`, `moneyUtils`, `scoringUtils`
- Produces:
  - `useKeoRoom()`: trả về state phòng, các actions: `setPhase`, `setRole`, `vote`, `submitVotes`, `addIdea`, `updateIdea`, `removeIdea`, `endorseIdea`, `selectIdea`, `moveOrder`, `submitOrder`, `completeStage`, `addNote`, `resetRoom`.
  - `useKeoMoney()`: trả về `expenses`, `transfers`, `balances`, `debts`, `totalSpent`, `userPaid`, `userBalance`, và actions: `addExpense`, `recordTransfer`, `confirmTransfer`.

- [ ] **Step 1: Cài đặt `useKeoRoom.ts`**
  - Lưu trạng thái phòng đầy đủ (`roomName`, `phase`, `role`, `ideas`, `votes`, `endorsed`, `selected`, `order`, `completed`, `notes`).
  - Hỗ trợ đổi vai Host / Member, đổi preview phase để test.
  - Kiểm tra điều kiện hoàn tất phiếu vote trước khi cho submit.
  - Xử lý sắp xếp thứ tự: di chuyển lên/xuống `moveOrder(id, -1 | 1)`.

- [ ] **Step 2: Cài đặt `useKeoMoney.ts`**
  - Quản lý `expenses` và `transfers`.
  - Tích hợp hàm `equalShares` cho chia đều, kiểm tra tổng tiền cho chia riêng.
  - Ghi nhận `recordTransfer` trạng thái pending (mô phỏng báo đã chuyển).
  - Xác nhận `confirmTransfer` chỉ khi người nhận là người dùng hiện tại (An).

- [ ] **Step 3: Commit**
  - `git add src/modules/keo/hooks`
  - `git commit -m "feat(keo): implement useKeoRoom and useKeoMoney state hooks"`

---

### Task 4: UI Components Development

**Files:**
- Create: `src/modules/keo/components/PhaseSteps.tsx`
- Create: `src/modules/keo/components/IdeaCard.tsx`
- Create: `src/modules/keo/components/RankRow.tsx`
- Create: `src/modules/keo/components/DebtRow.tsx`
- Create: `src/modules/keo/components/ExpenseCard.tsx`
- Create: `src/modules/keo/components/index.ts`

**Interfaces:**
- Consumes: Tamagui primitives, `assets/svgs`, `utils`
- Produces: Visual components tái sử dụng cho 2 tab.

- [ ] **Step 1: Xây dựng `PhaseSteps.tsx`**
  - Hiển thị 4 bước: 1. Góp ý, 2. Bình chọn, 3. Sắp lịch, 4. Đã chốt.
  - Đánh dấu bước hiện tại (thanh gạch dưới xanh đậm), bước đã qua (icon check tròn xanh), bước chưa tới.

- [ ] **Step 2: Xây dựng `IdeaCard.tsx`**
  - Icon loại hình (Ăn uống, Nhà sách, Chụp ảnh, Cà phê, Khác).
  - Badge ẩn danh: "Của bạn · chỉ bạn thấy" hoặc "Đề xuất ẩn tên".
  - Tiêu đề, ghi chú, chi phí dự kiến, thời lượng.
  - Phase `ideas`: Nút "Mình cũng muốn" / "Đã hưởng ứng", nút Sửa/Xóa cho bài của mình.
  - Phase `vote`: 3 nút lựa chọn ("Rất muốn", "Đi cùng được", "Để dịp khác") với trạng thái active rõ ràng.
  - Phase `order`: Hiển thị số lượt vote từng mức và checkbox chọn vào shortlist (chỉ Host sửa được).

- [ ] **Step 3: Xây dựng `RankRow.tsx`**
  - Hiển thị số thứ tự trong khung bo tròn.
  - Tiêu đề hoạt động, thời gian dự kiến (trong phase plan).
  - Phase `order`: Cặp nút Mũi tên Lên / Xuống để hoán đổi thứ tự.
  - Phase `plan`: Nút tròn check hoàn thành (có gạch ngang chữ khi đã xong).

- [ ] **Step 4: Xây dựng `ExpenseCard.tsx` & `DebtRow.tsx`**
  - `ExpenseCard`: Thẻ chi tiêu nền trắng, icon ví màu cam, tiêu đề, người đã trả, số người tham gia, tổng tiền VND.
  - `DebtRow`: Avatar chữ cái đầu, người nợ -> người nhận, số tiền, nút "Ghi nhận" hoặc nút "Chờ xác nhận".

- [ ] **Step 5: Commit**
  - `git add src/modules/keo/components`
  - `git commit -m "feat(keo): build core visual components"`

---

### Task 5: Interactive Modals Development

**Files:**
- Create: `src/modules/keo/components/IdeaFormModal.tsx`
- Create: `src/modules/keo/components/ExpenseFormModal.tsx`
- Create: `src/modules/keo/components/ExpenseDetailModal.tsx`
- Create: `src/modules/keo/components/TransferModal.tsx`
- Create: `src/modules/keo/components/NotesModal.tsx`
- Create: `src/modules/keo/components/InviteModal.tsx`
- Create: `src/modules/keo/components/SealPlanModal.tsx`

**Interfaces:**
- Consumes: Tamagui `Modal` / `Sheet` / base Dialog, inputs
- Produces: Form modals cho mọi thao tác nhập liệu

- [ ] **Step 1: Xây dựng `IdeaFormModal.tsx`**
  - Form thêm/sửa ý tưởng: Tiêu đề (bắt buộc), ghi chú (max 500 ký tự), chi phí mỗi người, thời lượng (phút).
  - Validation báo lỗi rõ ràng.

- [ ] **Step 2: Xây dựng `ExpenseFormModal.tsx` & `ExpenseDetailModal.tsx`**
  - Form thêm khoản chi: Tiêu đề, số tiền VND, dropdown chọn người đã trả, toggle cách chia (Chia đều / Chia riêng).
  - Danh sách checkbox chọn thành viên tham gia, input nhập tiền riêng khi chọn chia riêng.
  - Modal xem chi tiết phân bổ từng thành viên của một khoản chi.

- [ ] **Step 3: Xây dựng `TransferModal.tsx`**
  - Hai trạng thái: (1) Mô phỏng người nợ báo đã chuyển tiền; (2) Người nhận xem và bấm "Xác nhận đã nhận" để trừ nợ thật.

- [ ] **Step 4: Xây dựng `NotesModal.tsx`, `InviteModal.tsx`, `SealPlanModal.tsx`**
  - `NotesModal`: Xem danh sách lưu ý và thêm ghi chú ẩn danh mới.
  - `InviteModal`: Sao chép link mời và mã tham gia mẫu `KEO726`.
  - `SealPlanModal`: Cảnh báo và checkbox "Tôi đã trao đổi lưu ý với nhóm" dành cho Host trước khi chốt lịch.

- [ ] **Step 5: Commit**
  - `git add src/modules/keo/components`
  - `git commit -m "feat(keo): implement interactive dialogs and form modals"`

---

### Task 6: Screens & Navigation Wiring

**Files:**
- Create: `src/modules/keo/screens/KeoHomeScreen.tsx`
- Create: `src/modules/keo/screens/KeoRoomDetailScreen.tsx`
- Create: `src/modules/keo/screens/index.ts`
- Create: `src/modules/keo/index.ts`
- Modify: `src/configs/navigation.ts`
- Modify: `src/navigators/AppNavigator.tsx`
- Modify: `src/modules/home/screens/Home.tsx`

**Interfaces:**
- Consumes: Components, hooks, React Navigation
- Produces: Hoàn thiện trải nghiệm người dùng từ màn hình Home vào thẳng phòng Kèo.

- [ ] **Step 1: Hoàn thiện `KeoRoomDetailScreen.tsx`**
  - Thanh demo trên cùng: Chuyển vai (Chủ phòng / Thành viên), Xem nhanh màn (Góp ý / Bình chọn / Sắp lịch / Đã chốt), Đặt lại dữ liệu mẫu.
  - Hero header: Tên phòng ("Tối thứ Bảy của tụi mình"), ngày đi chơi, danh sách avatar bạn bè, nút "Mời bạn".
  - Tab chuyển đổi: `[Kế hoạch]` và `[Chia tiền]`.
  - Tab Kế hoạch hiển thị tương ứng theo phase với nút hành động bám chân trang (Sticky action bar).
  - Tab Chia tiền hiển thị 3 chỉ số thống kê (Tổng chi, Bạn đã trả, Bạn còn nhận/cần trả), danh sách khoản chi, danh sách ai cần trả ai, và lịch sử thanh toán.

- [ ] **Step 2: Hoàn thiện `KeoHomeScreen.tsx`**
  - Giao diện thân thiện chào đón: Nút "+ Tạo buổi đi chơi mới", nút "Tham gia bằng mã" (hỗ trợ nhập mã mẫu `KEO726`), danh sách các phòng gần đây.

- [ ] **Step 3: Khai báo Routes và Navigation**
  - Thêm `RouteName.KeoHome = 'KeoHome'` và `RouteName.KeoRoomDetail = 'KeoRoomDetail'` vào `src/configs/navigation.ts`.
  - Đăng ký màn hình vào `Stack.Navigator` trong `src/navigators/AppNavigator.tsx`.
  - Bổ sung nút bấm / Card nổi bật dẫn tới Kèo Đi Chơi ngay tại màn hình chính `src/modules/home/screens/Home.tsx`.

- [ ] **Step 4: Commit**
  - `git add src/modules/keo src/configs/navigation.ts src/navigators/AppNavigator.tsx src/modules/home/screens/Home.tsx`
  - `git commit -m "feat(keo): wire screens, routes, and home entrypoint"`

---

### Task 7: Verification & Polishing

- [ ] **Step 1: Chạy test verification script cho toàn bộ module nghiệp vụ**
  - Đảm bảo các kịch bản tính điểm, chia đều, chia riêng, bù trừ nợ, xác nhận thanh toán chạy không lỗi.
- [ ] **Step 2: Kiểm tra TypeScript compilation**
  - Chạy `npx tsc --noEmit` để đảm bảo không có lỗi type trong module `src/modules/keo`.
- [ ] **Step 3: Rà soát không chạm vào `mobile-hrm`**
  - Xác nhận sạch sẽ chỉ nằm trong `d:\AppData\Code\ReactNative\let_play`.
- [ ] **Step 4: Final commit**
  - `git status` và commit hoàn thiện.

