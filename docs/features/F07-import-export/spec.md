# F07 — Import / Export

Status: done
Depends on: F01, F02, F03
PRD refs: §3 req 7, §5 screen "Import/Export", §8, §9

## Scope

**In scope**
- kotlinx.serialization DTOs matching PRD §8 JSON schema
  (`LibraryExportDto`, `SetExportDto`, `CardExportDto`).
- `ImportExportRepository`: `exportLibrary(): String` (JSON),
  `validateImport(json: String): ImportValidationResult`,
  `importLibrary(parsed: LibraryExportDto, mode: ImportMode)` where
  `ImportMode = ReplaceAll | AddAsNewSets`.
- `ImportExportViewModel`/`Screen`: Export button → SAF
  `ACTION_CREATE_DOCUMENT` launcher, writes JSON to chosen `Uri`. Import
  button → SAF `ACTION_OPEN_DOCUMENT` launcher, reads + validates, on
  validation success shows a Replace-all/Add-as-new-sets choice dialog,
  then imports; validation failure shows an error message with the reason.

**Out of scope**
- Any partial/selective import (choose which sets) — whole-file only, per
  PRD §8.
- Practice stats/history in the export file (PRD §8 explicitly excludes them).

## JSON schema (from PRD §8, restated for DTO field-naming)

```kotlin
@Serializable
data class LibraryExportDto(
    val schemaVersion: Int = 1,
    val exportedAt: String, // ISO-8601, via TimeProvider
    val sets: List<SetExportDto>,
)

@Serializable
data class SetExportDto(val name: String, val cards: List<CardExportDto>)

@Serializable
data class CardExportDto(val front: String, val back: String, val notes: String? = null, val tags: List<String> = emptyList())
```

## Validation (before any DB write — PRD §9)
`validateImport` checks, in order, returning the first failure:
1. JSON parses at all (`Json.decodeFromString` wrapped in try/catch →
   `ImportValidationResult.Malformed`).
2. `schemaVersion == 1` (unknown version → `UnsupportedVersion`).
3. `sets.size <= 500` and each `cards.size <= 10_000` (guard against
   pathological files — `TooLarge`).
4. Each `front`/`back` non-blank and `<= 1000` chars, `notes <= 2000` chars,
   each tag name `<= 50` chars and `<= 10` tags per card (same limits as
   F03's editor validation — reuse the constants, don't duplicate magic
   numbers) → `InvalidContent(details)`.
5. All checks pass → `ImportValidationResult.Valid(parsed)`.

## Import execution
Single `@Transaction` DAO method (new `LibraryDao.replaceOrAppendLibrary`
or composed at the repository level via existing DAOs inside a
`db.withTransaction { }` block — repository-level is simpler here since it
spans `CardSetDao`/`CardDao`/`TagDao`/`CardTagCrossRefDao`):
- `ReplaceAll`: delete all existing `CardSetEntity` rows first (cascades
  cards/sessions per F01 FKs), then insert everything from the DTO.
- `AddAsNewSets`: insert DTO sets as brand-new `CardSetEntity` rows
  (new UUIDs) alongside existing data, no deletion.
- Tags: reuse F03's get-or-create-by-name logic (same
  `TagRepository`/`CardTagCrossRefDao` calls) rather than reimplementing it.
- All new rows get fresh UUIDs via `IdGenerator` — imported data never
  reuses IDs from the file, avoiding collisions with existing rows.

## Export execution
Read all sets via `CardSetDao.getAll()` + cards+tags via F03's existing
`CardWithTags` query, map to DTOs, `Json.encodeToString`. No streaming
needed at personal-app data volumes.

## Edge cases
- Empty library (no sets) exports `{"schemaVersion":1,...,"sets":[]}` —
  valid, re-importable no-op.
- Import file with a set name colliding with an existing set name (under
  `AddAsNewSets`): allowed, duplicate names permitted (per F02 spec — same
  rule as manual set creation).
- Mid-import failure (e.g. disk error) inside the transaction: rolled back
  entirely, nothing persisted — verified with a forced-exception test.

## Acceptance criteria
- [ ] Export produces a JSON file via the system file picker matching the
      documented schema.
- [ ] Re-importing an exported file with "Add as new sets" duplicates the
      library (new IDs, same content).
- [ ] "Replace all" import clears existing data first.
- [ ] Malformed/oversized/invalid-content JSON is rejected with a clear
      error and zero DB writes.
- [ ] `ImportExportScreen` has `@Preview`s per the project's Compose preview
      convention.

## Implementation notes (post-hoc, added after F01–F07 shipped)
- `ImportExportRepositoryImpl` depends on `CardSetRepository`/`CardRepository`
  rather than raw DAOs — stronger reuse than "reuse F03's get-or-create-by-name
  logic" above implied: tag resolution, id generation, and validation
  boundaries are all inherited by going through the repository layer, not
  just the tag lookup.
- Import/export transactions go through a new `data/TransactionRunner.kt`
  abstraction (`TransactionRunner`/`RoomTransactionRunner`/`FakeTransactionRunner`)
  wrapping `RoomDatabase.withTransaction`, rather than a bare
  `db.withTransaction {}` block inline — same fakeable-dependency shape as
  `IdGenerator`/`TimeProvider`, so `ImportExportRepositoryImpl` stays
  unit-testable.
- SAF `DocumentWriter`/`DocumentReader` bindings live in their own
  `di/DocumentIoModule.kt`, separate from `di/RepositoryModule.kt`, so
  androidTest can swap the whole module via `@TestInstallIn` without
  re-declaring every other binding.
- The Set List entry point to this screen is a plain `TextButton`
  ("Import / Export"), not an actual overflow/dropdown menu — the app has
  no icon/menu infrastructure anywhere else yet, so a plain button matches
  existing conventions (e.g. `SetDetailScreen`'s "History" button) better
  than introducing the first `DropdownMenu` in the codebase for one entry.
