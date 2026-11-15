# Contributing

Contributions to LetsPlay should follow the [migration plan](docs/migration/README.md) and confirmed product rules.
Keep changes focused and distinguish implemented Kotlin behavior from React Native reference documents.

## Before starting

Read the [README](README.md) and [architecture guide](docs/architecture.md). Check existing
[issues](https://github.com/NguynNe3110/Lets-Play/issues) before opening a new one.
For a larger change, describe the problem and proposed scope in an issue first.

## Development setup

Fork the repository, clone your fork, and create a branch from `main`.
Use JDK 21 and Android SDK 34 as described in [getting started](README.md#getting-started).

```bash
git switch -c fix/describe-the-change
./gradlew verifyModuleBoundaries :app:assembleDebug
```

On Windows, replace `./gradlew` with `.\gradlew.bat`.

## Architecture expectations

- Keep the five existing modules organized by layer.
- Put domain models, business rules, and repository contracts in `domain`.
- Put implementations and technical data mapping in `data`.
- Keep Android presentation, navigation, and Hilt provider wiring in `app`.
- Preserve cancellation. Do not map `CancellationException` into an application failure.
- Make operation replay explicit. A failed network request does not automatically make a write safe to retry.
- Preserve the repository's observable source-of-truth contract when adding persistence or network writes.
- Add abstractions when LetsPlay features need them; preserve existing infrastructure contracts.

## Validation

Run the checks relevant to your change. For application or architecture changes, use:

```bash
./gradlew verifyModuleBoundaries
./gradlew :domain:test :data:test :shared:test :app:testDevDebugUnitTest :app:testProdDebugUnitTest
./gradlew :app:assembleDebug :app:lintDebug
```

Add a regression test for a behavior fix when it protects a meaningful failure case.
Include device testing results when changing interactions that need a real Android environment.
Existing lint warnings are documented in [testing](docs/testing.md); avoid introducing unrelated warnings.

## Documentation and diagrams

Keep [README.md](README.md) and [README.vi.md](README.vi.md) consistent when changing documented behavior.
Use relative links for repository files. Describe capabilities that exist in the code.

Diagram source: [tools/docs/generate_diagrams.py](tools/docs/generate_diagrams.py).

```bash
python -m pip install Pillow
python tools/docs/generate_diagrams.py
```

On Windows, `py -3` can replace `python`. Commit matching SVG/PNG outputs and visually inspect the images.
See [image notes](docs/images/README.md) for font requirements.

## Pull requests

Use a title that describes the resulting behavior. In the description, explain the problem, the change,
and the validation performed. Link a related issue when there is one.

Keep unrelated refactors out of a bug fix. Update documentation when public contracts or setup steps change.
Include screenshots for visual UI changes and note any validation that was not performed.

## License

Contributions are provided under the repository's [MIT License](LICENSE).
Retain required notices when incorporating third-party material and identify its source.
