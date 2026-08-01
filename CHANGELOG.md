# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/hmcaio/flash-cards/commits/main
