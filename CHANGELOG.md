# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Icon replacements (C003): the "+" FABs on Set List/Set Detail and the
  "Correct"/"Incorrect" text on Session Play now use Material icons
  (`Icons.Default.Add`/`Check`/`Close`) instead of text.
- Set List/Set Detail card layout (C004): rows on both screens are now
  Material3 `Card`s, with a list/grid toggle (grid is the new default) backed
  by a single global, Jetpack DataStore Preferences-persisted preference
  shared by both screens, and per-row Rename/Delete actions moved from
  inline buttons into a `MoreVert` dropdown menu.
- Lock orientation to portrait (C005): `MainActivity` now declares
  `android:screenOrientation="portrait"` in `AndroidManifest.xml`, so the app
  no longer rotates to landscape.
- Session Play layout (C006): the flashcard `Card` on Session Play is now
  vertically centered in the space below the progress text, with the
  Correct/Incorrect buttons (or the "Tap the card to flip" hint) anchored to
  the bottom of the screen instead of trailing directly under the card. The
  Correct/Incorrect buttons are now larger (72dp) with fixed green/red
  colors, and backing out of an in-progress session (system back gesture)
  now shows a "Leave session?" confirm dialog instead of silently
  discarding the session.
- Tag filter chip row refactor (C008): Set Detail's single-select and
  Session Config's multi-select tag filter chip rows now share one
  `TagFilterChipRow` composable (new `ui/components/` package) built on
  `FlowRow`, so chips wrap onto multiple lines -- bounded to a max height with
  its own internal vertical scroll -- instead of an unbounded single-line
  horizontal scroll that broke down with many tags.

## [1.1.0] - 2026-08-07

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

[Unreleased]: https://github.com/hmcaio/flash-cards/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/hmcaio/flash-cards/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/hmcaio/flash-cards/compare/a51d4f1...v1.0.0
