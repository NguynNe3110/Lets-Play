# Let’s Play

Native Android app for planning outings, shared expenses, and photo memories. This is the Kotlin migration target for the React Native app, forked from [Kotlin-multi-module](https://github.com/NguynNe3110/Kotlin-multi-module).

[Tiếng Việt](README.vi.md) · [Migration plan](docs/migration/README.md) · [Product documents](docs/README.md) · [Contributing](CONTRIBUTING.md)

## Current status

LetsPlay includes a main tab shell and domain rules for outing plans and shared expenses. It also includes the upstream Todo/Users examples and [REST practice flows for auth and products](docs/rest-api-integration-plan.md). The LetsPlay backend integration is tracked in the [migration plan](docs/migration/README.md).

## Project configuration

- Gradle project: `LetsPlay`; app label: **Let’s Play** in English and Vietnamese.
- App namespace and Kotlin package prefix: `com.uzuu.letplay`.
- Resources namespace: `com.uzuu.letplay.resources`.
- Source and test folders: `com/uzuu/letplay`; infrastructure classes such as `BaseViewModel` remain reusable.

| Variant | Application ID |
| --- | --- |
| `devDebug` | `com.uzuu.letplay.dev.debug` |
| `devRelease` | `com.uzuu.letplay.dev` |
| `prodDebug` | `com.uzuu.letplay.debug` |
| `prodRelease` | `com.uzuu.letplay` |

Use `devDebug` for local development. Its ID is separate from the documented React Native release/debug IDs. Production IDs follow the source app; signing, versioning, and data migration must be resolved before distributing an update over the React Native app.

## Getting started

Use **JDK 21** for Gradle; Java/Kotlin bytecode targets 17. Install Android SDK **34**, Build Tools **34.0.0**; devices need API **24** or later. The project uses Gradle **9.4.1**, AGP **8.3.1**, Kotlin **2.0.21**. See the [version catalog](gradle/libs.versions.toml).

```bash
git clone https://github.com/NguynNe3110/Lets-Play.git
cd Lets-Play
```

Open the root in Android Studio, select Gradle JDK 21, and sync. Machine-specific paths stay in local configuration. Select run configuration `app` and build variant `devDebug`.

```powershell
.\gradlew.bat verifyModuleBoundaries :app:assembleDevDebug
```

On macOS/Linux use `./gradlew`. APK: `app/build/outputs/apk/dev/debug/app-dev-debug.apk`.

## Architecture

Five modules remain: `app` (Compose, ViewModels, navigation, Android session adapter, Hilt), `domain` (business contracts/use cases), `data` (API/DTO/repositories), `shared` (Result/errors/coroutine utilities), and `resources` (theme/colors/strings).

Read the [current architecture](docs/architecture.md) and [target architecture](docs/migration/02-kotlin-architecture.md).

## API and release configuration

Supply `DEV_API_BASE_URL` / `PROD_API_BASE_URL` via Gradle properties or environment variables. Their current fallback is JSONPlaceholder for the inherited Users sample. Changing a URL alone does not implement the LetsPlay API contract.

The existing production release check rejects the demo endpoint unless explicitly authorized for a demo build. Real releases require product backend integration and release/signing setup. Renaming packages does not register a Firebase project or configure production credentials.

## Verification

```powershell
.\gradlew.bat verifyModuleBoundaries :domain:test :data:test :shared:test
.\gradlew.bat :app:testDevDebugUnitTest :app:testProdDebugUnitTest :app:assembleDebug
```

See [testing](docs/testing.md) for reports and validation limits. CI checks module boundaries, unit tests, and both debug variants.

## License and origin

Keep the inherited [MIT license notice](LICENSE) with reused code. Architecture notes and diagrams inherited from Kotlin-multi-module describe starter infrastructure. Documents under `docs/react-native-source` are migration references.
