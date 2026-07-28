# F07 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. DTOs + serialization round-trip (pure unit tests)
Files: `test/.../data/importexport/LibraryExportDtoTest.kt`, prod
`data/importexport/LibraryExportDto.kt` (+ Set/Card DTOs).

1. RED — `roundTrip_encodeThenDecode_producesEquivalentDto`.
2. GREEN — Write the three `@Serializable` data classes per spec.md.

## 2. Validation (pure unit tests)
Files: `test/.../data/importexport/ImportValidatorTest.kt`, prod
`data/importexport/ImportValidator.kt`.

3. RED — `malformedJson_returnsMalformed`.
4. GREEN — `ImportValidator.validate(json: String)` — try/catch decode.
5. RED — `wrongSchemaVersion_returnsUnsupportedVersion`.
6. GREEN — Version check.
7. RED — `tooManySets_returnsTooLarge` / `tooManyCardsInASet_returnsTooLarge`.
8. GREEN — Size cap checks.
9. RED — `blankFrontOrBack_returnsInvalidContent`.
10. GREEN — Content validation, reusing F03's length/tag-count constants
    (extract those constants to a shared `CardValidationRules` object if
    still local to `CardEditorViewModel` — small refactor here).
11. RED — `tooManyTagsOnACard_returnsInvalidContent`.
12. GREEN — Tag-count check.
13. RED — `validMinimalDto_returnsValid`.
14. GREEN — (should already pass once above checks are correct).

## 3. ImportExportRepository — export (unit, fake DAOs)
Files: `test/.../data/repository/ImportExportRepositoryTest.kt`.

15. RED — `exportLibrary_emptyDb_returnsSchemaWithEmptySetsList`.
16. GREEN — `ImportExportRepositoryImpl.exportLibrary` minimal.
17. RED — `exportLibrary_mapsSetsCardsAndTagsFromDb`.
18. GREEN — Full mapping via F03's `CardWithTags` query.

## 4. ImportExportRepository — import (unit, fake DAOs + fake IdGenerator)
19. RED — `importReplaceAll_deletesExistingSetsBeforeInserting`.
20. GREEN — `importLibrary(..., ReplaceAll)`: delete-all then insert, in one
    `withTransaction` block.
21. RED — `importAddAsNewSets_doesNotDeleteExistingData`.
22. GREEN — `AddAsNewSets` path.
23. RED — `import_generatesFreshUuidsForAllRows_neverReusesFileIds` (DTO has
    no IDs at all per schema — this test really asserts every inserted
    entity's id comes from `FakeIdGenerator`'s sequence, not e.g. derived
    from name/index).
24. GREEN — Confirm/fix ID generation call sites.
25. RED — `import_reusesExistingTagsByName_sameAsManualCardCreation` (import
    two cards sharing a tag name → only one `TagEntity` row).
26. GREEN — Reuse F03's tag get-or-create path rather than reimplementing.
27. RED — `import_transactionRollsBackOnFailureMidway` (fake DAO throws on the
    Nth insert, assert no earlier inserts remain — needs an in-memory Room
    DB here rather than a fully-faked DAO, since transactional rollback is
    a real Room behavior; promote this one test to `androidTest`).
28. GREEN — Wrap the whole import in `db.withTransaction { }`.

## 5. ImportExportViewModel (unit, fake repo)
Files: `test/.../ui/importexport/ImportExportViewModelTest.kt`.

29. RED — `onExportUriSelected_writesRepositoryExportOutputToUri`.
30. GREEN — Wire (Uri writing itself goes through an injected small
    `DocumentWriter`/`DocumentReader` abstraction over `ContentResolver` so
    this stays a fake-based unit test, not requiring real SAF).
31. RED — `onImportUriSelected_invalidJson_showsErrorState_doesNotPromptModeChoice`.
32. GREEN — Wire validation failure path.
33. RED — `onImportUriSelected_validJson_showsModeChoiceDialog`.
34. GREEN — Wire.
35. RED — `onImportModeConfirmed_callsRepositoryImportLibrary`.
36. GREEN — Wire.

## 6. Screen (Compose) + nav wiring
37. RED — `androidTest/.../ui/importexport/ImportExportScreenTest.kt` —
    `exportThenImportAddAsNewSets_duplicatesLibrary` (export to a temp
    file via a test `DocumentWriter` fake, clear nothing, import same file
    with Add-as-new-sets, assert set count doubled with matching content).
38. GREEN — Build `ImportExportScreen` (Export button, Import button, mode
    choice dialog, error snackbar/text).
39. GREEN — Replace `Screen.ImportExport` placeholder in `MainActivity` NavHost;
    add an entry point (e.g. overflow menu on Set List).

## 7. Refactor
40. REFACTOR — Confirm validation constants shared with F03 (not duplicated), all
    F01–F07 tests green.

## Definition of done
`./gradlew test connectedAndroidTest` green; manual export → import
(both modes) round trip on a real device/emulator.
