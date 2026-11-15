# Tài liệu được chuyển từ React Native

[Mở kế hoạch Kotlin](../migration/README.md) · [Đối chiếu hiện trạng source](../migration/01-source-mapping.md)

Ngày chuyển: 15/11/2026. Nguồn: let_play tại commit e0e5f541f2efcde30acb0160431b949dcd7913f3. Đích: LetsPlay/docs/react-native-source.

## Phạm vi

Đã chuyển **27/27 file trong let_play/docs**, gồm Markdown và JSON evidence. Kèm 10 file văn bản hỗ trợ: 4 context Markdown, 3 skill cục bộ, README, README_BASE và bản chụp AGENTS. Tổng cộng **37 file nguồn**.

Nội dung nghiệp vụ và các kết quả kiểm chứng lịch sử được giữ. Không đổi status BA/AC sang pass, không hợp nhất những quyết định cũ mâu thuẫn một cách âm thầm. Đọc biên bản 11/11 và delta PC trước các baseline cũ.

AGENTS.md được đặt tên AGENTS.source.md để giữ làm bằng chứng hướng dẫn RN, không kích hoạt nó thành hướng dẫn cho project Kotlin. Các skill ở đây là tài liệu nguồn, chưa được cài vào thư viện skill cá nhân. Không chạy script/bootstrap/hook vì có tên trong tài liệu.

## Điều chỉnh đường dẫn

- Đổi link AGENTS.md trong README snapshot sang AGENTS.source.md.
- Sửa 4 link sai có sẵn trong hai tài liệu nguồn: ba link của docs/ba/photo-memories/backend-gap-review.md trỏ evidence/UI delta/API contract, và một link GAP review trong spec online-flow.
- Các link đến package.json, source TS/TSX, 7 ảnh PNG tham chiếu và prototype README trỏ về **repo let_play bên cạnh**. Không tạo bản code RN thứ hai trong Kotlin.
- Ảnh mockup, prototype chạy được, source, handoff bundle, native RN/iOS và node_modules không nằm trong bản chuyển tài liệu này. Các link đến chúng vẫn dùng repo nguồn; không được coi bản này là gói asset tự chứa.

**Tính di động:** giữ LetsPlay và let_play cạnh nhau trong workspace để mở các link source/ảnh. Khi cần tách LetsPlay thành repo độc lập để triển khai UI, copy asset tham chiếu vào docs/references và cập nhật link trong ticket P1/P5. Không xóa repo nguồn trước khi hoàn tất bước đó.

Các đường dẫn được viết như code text trong kế hoạch RN cũ vẫn có nghĩa tương đối với let_play gốc. Các lệnh yarn/Metro/EAS trong snapshot không phải lệnh build LetsPlay. Link HTTPS là nguồn ngoài; không ghi nhận server/website hoạt động chỉ vì link có mặt.

## Danh sách file

| Nguồn tương đối từ let_play | Bản chuyển trong thư mục này |
| --- | --- |
| docs/ba/photo-memories/README.md | [docs/ba/photo-memories/README.md](docs/ba/photo-memories/README.md) |
| docs/ba/photo-memories/08-decisions.md | [docs/ba/photo-memories/08-decisions.md](docs/ba/photo-memories/08-decisions.md) |
| docs/ba/photo-memories/verification.md | [docs/ba/photo-memories/verification.md](docs/ba/photo-memories/verification.md) |
| docs/ba/photo-memories/04-domain-data.md | [docs/ba/photo-memories/04-domain-data.md](docs/ba/photo-memories/04-domain-data.md) |
| docs/ba/photo-memories/05-api-contract.md | [docs/ba/photo-memories/05-api-contract.md](docs/ba/photo-memories/05-api-contract.md) |
| docs/ba/photo-memories/backend-gap-review.md | [docs/ba/photo-memories/backend-gap-review.md](docs/ba/photo-memories/backend-gap-review.md) |
| docs/ba/photo-memories/01-context-and-scope.md | [docs/ba/photo-memories/01-context-and-scope.md](docs/ba/photo-memories/01-context-and-scope.md) |
| docs/ba/photo-memories/02-business-requirements.md | [docs/ba/photo-memories/02-business-requirements.md](docs/ba/photo-memories/02-business-requirements.md) |
| docs/ba/photo-memories/07-acceptance-and-delivery.md | [docs/ba/photo-memories/07-acceptance-and-delivery.md](docs/ba/photo-memories/07-acceptance-and-delivery.md) |
| docs/ba/photo-memories/06-events-notifications-nfr.md | [docs/ba/photo-memories/06-events-notifications-nfr.md](docs/ba/photo-memories/06-events-notifications-nfr.md) |
| docs/ba/photo-memories/03-permissions-and-lifecycle.md | [docs/ba/photo-memories/03-permissions-and-lifecycle.md](docs/ba/photo-memories/03-permissions-and-lifecycle.md) |
| docs/ba/photo-memories/09-ui-feed-and-capture-composer.md | [docs/ba/photo-memories/09-ui-feed-and-capture-composer.md](docs/ba/photo-memories/09-ui-feed-and-capture-composer.md) |
| docs/ba/continuous-planning.md | [docs/ba/continuous-planning.md](docs/ba/continuous-planning.md) |
| docs/ba/product-rules-2026-11-11.md | [docs/ba/product-rules-2026-11-11.md](docs/ba/product-rules-2026-11-11.md) |
| docs/ba/product-confirmation-2026-11-11.md | [docs/ba/product-confirmation-2026-11-11.md](docs/ba/product-confirmation-2026-11-11.md) |
| docs/design.md | [docs/design.md](docs/design.md) |
| docs/integration/backend-ai-start.md | [docs/integration/backend-ai-start.md](docs/integration/backend-ai-start.md) |
| docs/integration/backend-gap-review.md | [docs/integration/backend-gap-review.md](docs/integration/backend-gap-review.md) |
| docs/integration/api-compatibility-map.md | [docs/integration/api-compatibility-map.md](docs/integration/api-compatibility-map.md) |
| docs/integration/prototype-verification.md | [docs/integration/prototype-verification.md](docs/integration/prototype-verification.md) |
| docs/integration/swagger-evidence-2026-11-10.json | [docs/integration/swagger-evidence-2026-11-10.json](docs/integration/swagger-evidence-2026-11-10.json) |
| docs/superpowers/plans/2026-11-04-keo-di-choi.md | [docs/superpowers/plans/2026-11-04-keo-di-choi.md](docs/superpowers/plans/2026-11-04-keo-di-choi.md) |
| docs/superpowers/plans/2026-11-11-photo-reactions-and-comments.md | [docs/superpowers/plans/2026-11-11-photo-reactions-and-comments.md](docs/superpowers/plans/2026-11-11-photo-reactions-and-comments.md) |
| docs/superpowers/plans/2026-11-11-photo-memories-and-online-flow.md | [docs/superpowers/plans/2026-11-11-photo-memories-and-online-flow.md](docs/superpowers/plans/2026-11-11-photo-memories-and-online-flow.md) |
| docs/superpowers/specs/2026-11-11-photo-reactions-and-comments-design.md | [docs/superpowers/specs/2026-11-11-photo-reactions-and-comments-design.md](docs/superpowers/specs/2026-11-11-photo-reactions-and-comments-design.md) |
| docs/superpowers/specs/2026-11-11-photo-memories-and-online-flow-design.md | [docs/superpowers/specs/2026-11-11-photo-memories-and-online-flow-design.md](docs/superpowers/specs/2026-11-11-photo-memories-and-online-flow-design.md) |
| docs/verification.md | [docs/verification.md](docs/verification.md) |
| context/BA.md | [context/BA.md](context/BA.md) |
| context/QA.md | [context/QA.md](context/QA.md) |
| context/README.md | [context/README.md](context/README.md) |
| context/BOOTSTRAP-NOTES.md | [context/BOOTSTRAP-NOTES.md](context/BOOTSTRAP-NOTES.md) |
| README.md | [README.md](README.md) |
| agent-skills/keo-photo-ba/SKILL.md | [agent-skills/keo-photo-ba/SKILL.md](agent-skills/keo-photo-ba/SKILL.md) |
| agent-skills/keo-backend-contract/SKILL.md | [agent-skills/keo-backend-contract/SKILL.md](agent-skills/keo-backend-contract/SKILL.md) |
| agent-skills/keo-photo-integration/SKILL.md | [agent-skills/keo-photo-integration/SKILL.md](agent-skills/keo-photo-integration/SKILL.md) |
| README_BASE.md | [README_BASE.md](README_BASE.md) |
| AGENTS.md | [AGENTS.source.md](AGENTS.source.md) |

## Kiểm chứng

Kết quả kiểm nội dung, đường dẫn và phạm vi thay đổi được ghi tại [verification.json](../migration/verification.json). Build/test Kotlin, API runtime và camera không được chạy trong lượt tài liệu. Các PASS trong docs/verification hoặc BA ảnh là lịch sử RN của tác giả nguồn, không phải kết quả mới.
