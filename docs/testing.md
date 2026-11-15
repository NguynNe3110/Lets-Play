# Testing and verification

[Back to README](../README.md) · [Architecture](architecture.md)

## Toolchain

Use JDK **21**, Android SDK **34**, and the included Gradle wrapper.
Java/Kotlin compilation targets JVM 17. Robolectric tests currently select simulated Android API 36;
Robolectric retrieves its Android runtime artifact independently of the project's compile SDK.

The first run requires dependency downloads. No real authentication backend or API credentials are needed.
MockWebServer tests exercise networking locally.

## Commands

```bash
./gradlew verifyModuleBoundaries
./gradlew :domain:test :data:test :shared:test
./gradlew :app:testDevDebugUnitTest :app:testProdDebugUnitTest
./gradlew :app:assembleDevDebug :app:assembleProdDebug
./gradlew :app:lintDevDebug :app:lintProdRelease
```

On Windows, replace `./gradlew` with `.\gradlew.bat`.
For a targeted regression test:

```bash
./gradlew :app:testDevDebugUnitTest --tests com.example.base.session.DataStoreSessionFailureTest
```

Gradle can reuse UP-TO-DATE results. To deliberately re-execute the selected tasks and their prerequisites,
add `--rerun-tasks`. This is useful when validating an environment change or collecting a fresh verification result.

## Test coverage

| Area | Examples protected by tests |
| --- | --- |
| Domain use cases | Thrown failures, cancellation propagation, errors during Flow construction, BaseInput/BaseOutput contracts (17 tests) |
| Network boundary & Non-REST integrations | Request query parameters, DTO conversion, HTTP error envelopes, empty response bodies, malformed JSON; repository contracts & fake adapters for GraphQL, Supabase, Firebase, and MockWebServer REST switching (29 unit tests; live cloud SDKs remain roadmap backlog) |
| In-memory data | Concurrent additions and toggles preserve data |
| Flow policies & Concurrency | debounceWithLatest, throttleFirst, droppable, sequential with suspending enqueue and non-blocking tryEnqueue (15 tests) |
| Retry | Retryable/non-retryable failures, exponential backoff, cancellation |
| ViewModel operations | Retained error effects, manual read retry, write replay disabled, overlapping loading counts, composition with Android ViewModel, search submit & debounce coordination with request ID cancellation |
| Session persistence | Replacing optional credential fields, expiration restoration, clearing expiration after save, DataStore retryable restoration |
| Session recovery | Initial read error followed by Retry; late read errors unblock save, clear, and expire |
| Todo state | Draft restoration, failed writes preserve text, successful writes clear submitted text, newer drafts survive |
| Todo observation | Committed writes remain successful when subsequent list observation fails; list retry does not repeat a write |
| Navigation | IDs round-trip with spaces, `+`, `/`, percent signs, query/fragment characters, and Unicode |

## Build Variants and Flavors

The project configures two product flavors (`dev`, `prod`) under dimension `environment`, combined with two build types (`debug`, `release`), yielding four distinct build variants:

| Variant | Environment | Application ID | Purpose |
| --- | --- | --- | --- |
| `devDebug` | dev | `com.example.base.dev.debug` | Daily local development with dev endpoints |
| `devRelease` | dev | `com.example.base.dev` | Release behavior validation against dev environment |
| `prodDebug` | prod | `com.example.base.debug` | Intentional diagnostic build targeting production endpoint |
| `prodRelease` | prod | `com.example.base` | Production release artifact |

Each flavor generates `BuildConfig.ENVIRONMENT` and `BuildConfig.API_BASE_URL` (configurable via `DEV_API_BASE_URL` and `PROD_API_BASE_URL` properties/environment variables, with public JSONPlaceholder default). `AppConfig` validates scheme and trailing slash at runtime.

## Local verification snapshot

The following describes the revision checked on **15 November 2026**, using JDK 21:

| Check | Result |
| --- | --- |
| Module dependency boundaries | Passed (`verifyModuleBoundaries`) |
| Variant APK builds | Passed (`assembleDevDebug`, `assembleProdDebug`) |
| JVM and Robolectric test suite | **140 unique tests passed**, 0 failed (`domain`: 17, `data`: 36, `shared`: 37, `app`: 50) |
| Android lint | **0 errors**, **72 warnings** |
| Real device / emulator validation | Not performed |
| Instrumented UI test suite | Not implemented |
| Runtime profiling / benchmarks | Not performed |
| CI automation | Configured & validated via `.github/workflows/ci.yml` (ready for GitHub Actions remote execution; local verification runs local Gradle tasks) |

Lint warnings include older dependency/SDK declarations, a missing application icon, and a Compose lint API
compatibility issue. The incompatible Compose checks are skipped by lint, so a successful lint task does not
mean those checks ran. Review toolchain compatibility before claiming comprehensive lint coverage.

These results are a local snapshot. Re-run checks after changing code, dependencies, or build configuration.
Continuous integration is defined and automated via GitHub Actions in `.github/workflows/ci.yml` (testing module boundaries, multi-variant unit tests, and APK compilation). Local verification executes corresponding Gradle tasks directly. Non-REST backends are verified locally and on CI via repository contracts and fake adapters (29 tests); live cloud SDK integration remains in the roadmap backlog.

## Reports

| Output | Location |
| --- | --- |
| Domain tests | `domain/build/reports/tests/test/index.html` |
| Data tests | `data/build/reports/tests/test/index.html` |
| Shared tests | `shared/build/reports/tests/test/index.html` |
| App devDebug tests | `app/build/reports/tests/testDevDebugUnitTest/index.html` |
| App prodDebug tests | `app/build/reports/tests/testProdDebugUnitTest/index.html` |
| Lint | `app/build/reports/lint-results-devDebug.html` |
| DevDebug APK | `app/build/outputs/apk/dev/debug/app-dev-debug.apk` |
| ProdDebug APK | `app/build/outputs/apk/prod/debug/app-prod-debug.apk` |

Build outputs are ignored by Git. A fresh clone will not contain them until you run the corresponding task.

## Writing tests

Inject coroutine test dispatchers for deterministic scheduling. Prefer behavior checks at the boundary where a
failure matters: a saved draft, a completed write, a waiting session operation, or a navigation argument.
Use MockWebServer for transport behavior and Robolectric when Android framework behavior is required on the JVM.

Robolectric does not establish real-device rendering, navigation gestures, memory use, or performance.
Add device tests and profiling for those claims when adapting the starter to a product.
