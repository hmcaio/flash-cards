# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Session Config tag filter (C002): optional multi-select, OR-semantics tag
  filter chip row on Session Config, narrowing which cards
  `WeightedCardSelector` can pick for a practice session.

### Changed
- Session Results back navigation (C001): a "Back to Set" button and the
  system back gesture both now land directly on Set Detail instead of back
  on Session Play/Config, by collapsing that sub-stack with `popUpTo` when
  navigating to Session Results.

## [1.0.0] - 2026-08-04

### Added
- Data layer & app shell (F01): Room entities/DAOs for card sets, cards,
  tags, and practice sessions (`kotlin.uuid.Uuid` ids via a Room
  `TypeConverter`), Hilt DI, `IdGenerator`/`TimeProvider` abstractions, and a
  Navigation Compose scaffold with placeholder screens for every route.
- Card set management (F02): create, rename, and delete card sets from a new
  Set List screen, backed by `CardSetRepository` and `CardSetDao.getAllWithCardCount()`
  (live card counts, deletion cascades to a set's cards).
- Card & tag management (F03): create, edit, and delete cards with tags
  (case-insensitive get-or-create, autocomplete) from new Set Detail and Card
  Editor screens, backed by `CardRepository` and `TagRepository`.
- Card search (F04): search text field and single-select tag filter chip row
  on Set Detail, filtering the card list by front/back/notes and/or tag via
  `CardDao.searchCards`/`CardRepository.searchCards`.
- Practice session (F05): weighted practice sessions with new Session Config,
  Session Play, and Session Results screens, backed by `PracticeRepository`
  and a weighted-without-replacement `WeightedCardSelector`.
- Practice history (F06): new History List and History Detail screens showing
  past practice sessions per set (date, score) and their full correct/incorrect
  breakdown, reached from a new "History" button on Set Detail.
- Import/export (F07): whole-library JSON export/import via the system file
  picker, with schema/size/content validation and a Replace-all vs.
  Add-as-new-sets choice, reached from a new "Import / Export" button on Set List.

[Unreleased]: https://github.com/hmcaio/flash-cards/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hmcaio/flash-cards/compare/a51d4f1...v1.0.0
