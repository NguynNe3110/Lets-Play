# Mapping React Native → Kotlin

[Trở về mục lục](README.md) · [Kiến trúc](02-kotlin-architecture.md) · [Ticket triển khai](03-implementation-plan.md)

Các tên Kotlin trong bảng là **đích đề xuất, chưa tồn tại**. Đường dẫn React Native tính từ let_play; package nghiệp vụ Kotlin đề xuất là com.uzuu.letplay. Nguồn tổng thể: [AppNavigator](../../../let_play/src/navigators/AppNavigator.tsx), [local types](../../../let_play/src/modules/keo/types/index.ts), [online schema](../../../let_play/src/modules/keo/api/schema.ts).

## 1. Màn hình và điều hướng

| Nguồn RN | Hiện trạng quan sát từ source | Đích Kotlin | Mốc |
| --- | --- | --- | --- |
| App.tsx, AppNavigator.tsx | Provider gốc, root stack, bốn bottom tab và room stack | MainActivity, LetsPlayApp, MainScaffold, AppNavHost | P1/P3 |
| configs/navigation.ts | Route room/capture/photo và nested tabs | Route/argument contracts; truyền ID thay vì nguyên object | P1 |
| CameraHomeScreen.tsx | Camera trước/sau, flash, import, composer tại chỗ, lưu local | CameraRoute, CameraViewModel, CameraPreview, CaptureComposer | P5 |
| OnlineRoomsScreen.tsx | Danh sách online, tải lại, tạo/join và account sheet | RoomsRoute, RoomsViewModel, CreateRoomSheet, JoinRoomSheet | P3 |
| OnlineRoomScreen.tsx | Sáu khu vực: plan, ideas, time, money, album, people | RoomRoute và các tab composable, RoomViewModel | P3 |
| KeoHomeScreen.tsx | Phòng local, tìm tên/mã cùng máy, demo, archive/restore/delete | LocalRoomsRoute, LocalRoomsViewModel | P4 |
| KeoRoomDetailScreen.tsx | Phòng local lớn: ý, vote, thứ tự, kế hoạch, tiền | LocalRoomRoute với Planning/Money UI state riêng | P4 |
| PhotoScreens.tsx — MemoriesScreen | Thư viện local, lịch/lưới và chế độ xem | MemoriesRoute, MemoriesViewModel | P5 |
| PhotoScreens.tsx — PhotoPreviewScreen/PhotoDetailScreen | Preview/detail và tương tác local | PhotoPreviewRoute, PhotoDetailRoute | P5 |
| PhotoScreens.tsx — RoomPhotoAlbum | Lọc ảnh local gắn intendedRoomId, không là album online chung | LocalRoomPhotosSection; RoomAlbumRoute khi server có contract | P5/P7 |
| MessagesScreen.tsx | Placeholder chờ backend | MessagesRoute; ConversationRoute ở mốc B | P3/P7 |

Navigation tab giữ back stack và vị trí cuộn. Mở camera từ phòng giữ roomId đến composer; quay lại không lưu ảnh ngoài ý muốn. Tab Thời gian của root là kỷ niệm; tab Hẹn & đi cùng của phòng là lịch rảnh, không gộp hai khái niệm.

## 2. Component

| Nhóm source | Đích Compose | Hành vi phải giữ |
| --- | --- | --- |
| design/theme.ts, tamagui.config.ts, PlayUI.tsx | resources/theme, typography, spacing tokens | Lấy token thực sự dùng; hỗ trợ tiếng Việt/font scale, tránh mang cấu hình Tamagui không còn dùng |
| design/PlayIcon.tsx, assets/brand | ImageVector/vector drawable và launcher icon | Nét bo, tên truy cập, nguồn brand gốc; không thêm icon font RN |
| KeoUI.tsx, AccountSheet.tsx, OnlineRoomForm.tsx | CommonUi, AccountSheet, sheet theo thao tác | Empty/error/busy/notice; giữ nháp khi lỗi; chống submit trùng |
| IdeaCard.tsx, IdeaFormModal.tsx | ProposalCard, ProposalEditorSheet | Giấu tác giả với người khác; quyền sửa và reset vote theo mode |
| RankRow.tsx, PhaseSteps.tsx, SealPlanModal.tsx | ShortlistRow, OrderBallotSection, PublishPlanSheet | Không dùng thanh phase local làm quyền online |
| NotesModal.tsx, InviteModal.tsx | NotesSheet, InviteSheet, Android Sharesheet | Chia sẻ văn bản/mã phòng; mã local không phải lời mời online |
| ExpenseCard.tsx, ExpenseFormModal.tsx, ExpenseDetailModal.tsx | ExpenseCard, ExpenseEditorSheet, ExpenseDetailSheet | Payer và người chịu share là hai tập riêng; tiền nguyên |
| DebtRow.tsx, TransferModal.tsx | BalanceRow, TransferSuggestionRow, SettlementSheet | Pending không đổi balance; kiểm đúng receiver ở server |
| PhotoComposer.tsx | CaptureComposer, RecipientPicker | Caption trên ảnh, X/gửi/Aa, avatar, giữ nháp khi retake, chặn gửi khi thiếu adapter |
| PhotoFeed.tsx, PhotoImage.tsx | VerticalPager, PhotoPage, bounded image loader | Một ảnh mỗi viewport; giữ photo/placement ID khi resize; giới hạn decode và retry |
| Các modal và alert | Material 3 ModalBottomSheet/AlertDialog | Back/IME/insets, confirm hành động mất dữ liệu, vùng chạm 48 dp |

Không giữ nguyên một file 1.380 dòng dưới dạng một composable. Tách màn theo trách nhiệm và state; component dùng callback thay vì tự truy cập database/API.

## 3. State, store và nghiệp vụ

| Source | Trách nhiệm hiện tại | Đích đề xuất |
| --- | --- | --- |
| store/KeoProvider.tsx, RoomProvider | Danh sách phòng local, room context, save state | LocalRoomRepository + ObserveLocalRooms/ObserveLocalRoom và ViewModel theo roomId |
| hooks/useKeoRoom.ts | Action phòng local và phase | Planning use cases; UI mode tách khỏi quyền |
| hooks/useKeoMoney.ts | Validation và ghi sổ local | Money use cases + LocalLedgerRepository |
| store/OnlineProvider.tsx | Account, room map, ledger, collaboration, scope epoch, mutation lock | AuthRepository, RoomRepository, LedgerRepository, CollaborationRepository và application session coordinator |
| hooks/useOnlineRoom.ts | Refetch theo focus/foreground, SSE, debounce 350 ms, poll 15 s | ObserveRoomUpdates + RoomSyncCoordinator gắn lifecycle màn |
| store/PhotoProvider.tsx | MMKV metadata, file ảnh, ownerScope, reaction/comment | LocalPhotoRepository + PhotoFileStore + InteractionStore |
| api/client.ts | HTTP, token selection, timeout, schema check, uncertain mutation | LetsPlayApi, token resolver, DTO mapper, remote repositories |
| api/credentials.ts | Keychain account/guest/room tokens, serialize writes | CredentialStore Android + SessionRepository domain contract |
| api/sse.ts | Parse SSE/reconnect; event chỉ invalidate | RoomEventSource dùng OkHttp, callbackFlow, cancellation |
| utils/planningUtils.ts | Pure reducer, shortlist, Borda, plan snapshot, lifecycle | domain/planning: PlanningRules, BordaCalculator, các use case |
| utils/scoringUtils.ts | Tally 2/1/0, điểm, giờ chặng | VoteTallyCalculator, TimelineCalculator |
| utils/moneyUtils.ts | Chia đều, số dư, gợi ý trả | EqualShareCalculator, BalanceCalculator, SettlementSuggester |
| utils/onlineMoney.ts | Validation/mapping tiền online | LedgerValidator và DTO mapping rõ local/online |
| utils/roomStorage.ts | SavedRoom + schema v1, decode/encode, phòng mới | LegacyRoomDecoder + local entities/DAO; giữ đọc v1 |
| utils/photoFiles.ts | Copy/delete ảnh app-private | Android PhotoFileStore; I/O ngoài main thread |
| data/mockData.ts | Phòng mẫu có chủ đích | Debug/demo fixtures, không seed vào production room |
| utils/id.ts, api/credentials.ts randomId | ID local/UUID | IdGenerator inject; giữ ID gốc khi import |

OnlineProvider không trở thành một singleton ViewModel toàn ứng dụng chứa mọi màn. Repository sở hữu cache/session scope; ViewModel chỉ điều phối state màn. Đổi account phải hủy công việc cũ và chặn response cũ quay lại cache mới.

## 4. Model và ý nghĩa dữ liệu

| RN/model hiện tại | Kotlin đề xuất | Quy tắc mapping |
| --- | --- | --- |
| SavedRoom.id/code | LocalRoomId/LocalRoomCode | Không tự đổi thành OnlineRoomId hoặc token |
| RoomState.currentUserIndex, members[] | LegacyMemberIndex trong dữ liệu local | Khi import tạo LocalMemberId ổn định theo room + index; không suy AccountId từ tên |
| OnlineRoom.member.id / Profile.id | MembershipId / AccountId | Hai type riêng; quyền online từ server |
| Idea.id number / OnlineProposal.id string | LocalProposalId / ProposalId | Giữ nguồn/mode; tránh parse ép mọi ID về số |
| votes Record + VoteValue | Map<ProposalId, VoteValue> | null/chưa có khác ZERO; 0 vẫn là phiếu |
| votedByMe boolean | LegacyVoteState | Không chuyển true thành 2 hoặc false thành 0 |
| phase ideas/vote/order/plan | PlanningSection cho UI/local legacy | Không đại diện RoomLifecycle |
| phase COLLECTING/VOTING/FINALIZED | LegacyOnlinePhase + unknown fallback | Chỉ một selectedProposalId; không tự suy itinerary |
| draftOrder/order/planHistory | PlanDraft/PublishedPlan/PlanSnapshot | Draft đổi không đổi lịch đã công bố; snapshot giữ nội dung và completion |
| Expense.amount/payer/shares[] | LocalExpense + MoneyVnd(Long) | Legacy local một payer; tổng share bằng amount |
| OnlineExpense.payers[]/shares[] | Expense + Allocation(MembershipId, MoneyVnd) | Giữ multi-payer; tổng payers = tổng shares = total |
| Transfer pending/confirmed/rejected | LocalSettlementStatus | Không tự coi string status server là cùng enum |
| status server PAID/... | Verified remote enum hoặc Unknown(raw) | Chỉ trạng thái được xác minh mới ảnh hưởng logic UI/command |
| LocalPhoto.fileName/ownerScope/status | LocalPhoto/ViewerScope/LocalPhotoStatus | LOCAL_ONLY không thành PUBLISHED, intendedRoomId không cấp quyền album |
| LocalPhoto.source | CaptureSource.Camera/Imported | V1 thiếu source mặc định CAMERA |
| PhotoReaction/PhotoComment theo photoId | LocalPhotoInteraction | Khi online dùng placementId; không gộp tương tác các nơi đăng |
| Moment/Placement/AlbumComment/Message trong BA | Các model domain riêng ở mốc B | Model mục tiêu chưa là DTO backend đã có |

Validate số tiền ở boundary trước khi map; không dùng Double/Float cho VND. Dùng giới hạn nghiệp vụ local hiện tại 100.000.000đ cho adapter legacy; giới hạn online cần contract, không áp tùy tiện trần local cho dữ liệu server. Kiểm overflow khi cộng Long.

## 5. API đang được client sử dụng

Nguồn: [api/client.ts](../../../let_play/src/modules/keo/api/client.ts). Đây là lời gọi trong source; **chưa kiểm server runtime trong lượt này**.

| Nhóm | Endpoint/payload hiện có trong client | Kotlin adapter |
| --- | --- | --- |
| Auth | POST /api/auth/login, /register, /logout | AuthRemoteDataSource |
| Guest/account room | POST /api/rooms, /preview, /join; GET /api/rooms/mine; GET /api/rooms/{id}; POST /claim | RoomRemoteDataSource + CredentialStore |
| Proposal | POST/PUT/DELETE proposals; POST vote không body; POST comments; PUT completion | LegacyPlanningRemoteDataSource |
| Plan | POST phase với selectedProposalId; PUT plan | LegacyPlanRemoteDataSource |
| Availability | PUT availability; POST slots; POST select/comments | CollaborationRemoteDataSource |
| Collaboration | GET collaboration; PUT attendance/ride/preferences | CollaborationRemoteDataSource |
| Money | GET/POST expenses; PUT/DELETE expenses/{id}; POST settlements; PUT settlements/{id} | LedgerRemoteDataSource |
| Membership | DELETE members/{membershipId}; POST leave | RoomMembershipRemoteDataSource |
| Events | GET /api/rooms/{id}/events với Bearer và Last-Event-ID | RoomEventSource |

Giữ prefix /api hiện có. Tách envelope có accessToken khỏi DTO cache/display. Claim dùng account bearer và X-Guest-Access-Token riêng. Không log các token.

Các API **chưa được runtime ảnh hiện tại sử dụng**: media upload/processing/publish, feed placement cursor, friendship/block, DM, in-app notification và các command mới về approval/amendment/versioned plan. Không generate client từ swagger-evidence vì đó chỉ là bản trích.

## 6. Dependency React Native → Android

| RN | Android đề xuất | Cách xử lý |
| --- | --- | --- |
| React Navigation | Navigation Compose sẵn trong base | Giữ ở lượt chuyển đầu; việc dùng Navigation 3 là ADR/toolchain riêng |
| Tamagui/StyleSheet | Compose Material 3 + token resources | Chuyển layout/semantics theo màn |
| React Context/hooks | Hilt + ViewModel + StateFlow | Repository giữ dữ liệu, callback UI giữ sự kiện |
| MMKV JSON | Room cho dữ liệu quan hệ; decoder legacy v1 | Import có transaction; không dùng DataStore làm ledger lớn |
| Keychain | CredentialStore dùng Android Keystore để bảo vệ secret | Mã hóa secret; DataStore thường chỉ giữ preference không nhạy cảm |
| fetch/Axios/SWR | Retrofit/OkHttp + repository Flow | Không mang cả dependency cũ khi không dùng |
| Vision Camera | CameraX | Cần đọc skill camerax khi triển khai và test thiết bị |
| Image picker | Android Photo Picker qua Activity Result | Copy URI vào app-private trước khi coi nháp là bền |
| FlatList/FlashList | LazyColumn/LazyVerticalGrid; VerticalPager cho feed | Stable key, bounded decode, giữ vị trí theo ID |
| RN filesystem/blob | ContentResolver + app-private files | Không cần quyền đọc toàn bộ thư viện cho flow Photo Picker |
| Share API | ACTION_SEND/Android Sharesheet, FileProvider khi chia file | Cấp URI quyền đọc trong phạm vi cần |
| Notifee/Firebase messaging | FCM Android nếu backend chọn FCM | Không tự thêm Firebase Auth/Firestore; kiểm SDK thật sự cần |
| Expo OTA/EAS/Metro | APK/AAB và Gradle/CI | Không chuyển cơ chế OTA JS sang native |
| Jest/RNTL | Kotlin tests, coroutines-test, MockWebServer, Compose UI tests | Port các kịch bản nghiệp vụ, không dịch assertion theo dòng |
| HTML prototype/iOS/native RN config | Tài liệu tham chiếu | Không copy thành implementation hoặc cấu hình native Kotlin |

## 7. Phần giữ từ base và phần thay

Giữ các hợp đồng Result/AppException, dispatcher, ActionRunner/LoadingManager, base use case, BaseScreen/BaseViewModel, Hilt composition root và verifyModuleBoundaries nếu còn phù hợp. Feature mới dùng error/state có nghĩa với người dùng, không trình bày lỗi mẫu tiếng Anh của base.

Thay entry graph Todo/Users bằng graph Let’s Play ở P1/P3; bỏ JSONPlaceholder khỏi wiring sản phẩm. Chỉ xóa sample sau khi có vertical slice thay thế và test không còn phụ thuộc sample. Các fake GraphQL/Firebase/Supabase trong base không phải yêu cầu tích hợp Let’s Play.

README/architecture/testing hiện có của base cần cập nhật khi code thực sự thay đổi; lượt lập kế hoạch này giữ nguyên để không mô tả sai runtime.
