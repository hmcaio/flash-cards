# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Room data layer: `CardSet`, `Card`, `Tag`, `CardTagCrossRef`,
  `PracticeSession`, and `PracticeSessionResult` entities, their DAOs, and
  `FlashCardsDatabase`.
- `IdGenerator` / `TimeProvider` abstractions (Hilt-injected) as the sole
  source of UUIDs and timestamps in the data layer.
- Hilt DI setup (`FlashCardsApplication`, `DatabaseModule`, `UtilModule`).
- Navigation Compose scaffold (`Screen` routes, nav host) with placeholder
  destinations for every screen in the PRD.
- Release build config with R8 minification and resource shrinking enabled.

[Unreleased]: https://github.com/hmcaio/flash-cards/commits/main
