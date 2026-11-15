# Changelog

Project changes are recorded here. No release version has been assigned yet.

## Unreleased

### Added

- English and Vietnamese project documentation with setup instructions and architecture guidance.
- Original project structure, module dependency, data flow, and session-state diagrams in SVG and PNG.
- Header artwork and a GitHub social preview image, with a reproducible generation script.
- MIT license, contribution guide, issue forms, pull request template, and GitHub About metadata notes.

### Documented baseline

- Five Gradle modules organized by layer: app, domain, data, shared, and resources.
- Hilt composition root, Compose MVVM presentation, Retrofit Users sample, and observable Todo sample.
- Session restoration with Loading/Ready/Error, saved Todo drafts, explicit retry, and cancellation propagation.
- Local JDK 21 validation: debug build and module checks pass; 35 tests pass; lint reports 0 errors and 72 warnings.
