# F02 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. DAO addition
1. RED — `app/src/androidTest/java/.../data/dao/CardSetDaoTest.kt` — add
   `getAllWithCardCount_reflectsZeroAndNonZeroCounts` (insert set, assert
   count 0; insert 2 cards under it, assert count 2).
2. GREEN — Add `CardSetWithCount(id, name, createdAt, cardCount)` projection +
   `CardSetDao.getAllWithCardCount()` `@Query` (`LEFT JOIN cards ... GROUP BY`).

## 2. CardSetRepository (unit tests, fake DAO)
Files: `app/src/test/java/.../data/repository/CardSetRepositoryTest.kt`,
prod `data/repository/CardSetRepository.kt` (+ `CardSetRepositoryImpl`),
using `FakeIdGenerator`/`FakeTimeProvider` from F01.

3. RED — `createSet_generatesIdAndTimestamp_andInsertsIntoDao` (fake DAO
   captures inserted entity, assert id from `FakeIdGenerator`, createdAt
   from `FakeTimeProvider`).
4. GREEN — `CardSetRepositoryImpl.createSet`.
5. RED — `getAllSets_mapsDaoFlowToDomainModel`.
6. GREEN — `getAllSets` mapping.
7. RED — `renameSet_updatesExistingEntityName`.
8. GREEN — `renameSet` (load, copy(name=...), update).
9. RED — `deleteSet_callsDaoDeleteById`.
10. GREEN — `deleteSet`.
11. GREEN — `di/RepositoryModule.kt` — `@Binds CardSetRepository → CardSetRepositoryImpl`.

## 3. SetListViewModel (unit tests, fake repository)
Files: `app/src/test/java/.../ui/setlist/SetListViewModelTest.kt`, prod
`ui/setlist/SetListViewModel.kt`, `SetListUiState.kt`.

12. RED — `initialState_loadsSetsFromRepository` (fake repo emits list via
    `MutableStateFlow`, assert `uiState.sets` matches after `advanceUntilIdle`).
13. GREEN — `SetListViewModel` collects `repository.getAllSets()` into state.
14. RED — `onCreateNameChange_blankAfterTrim_setsError_onConfirmDoesNotCallRepository`.
15. GREEN — Validation logic + guard in `onCreateConfirm`.
16. RED — `onCreateConfirm_validName_callsRepositoryCreateSet_closesDialog`.
17. GREEN — Wire happy path.
18. RED — `onDeleteConfirm_callsRepositoryDeleteSet`.
19. GREEN — Wire.
20. RED — `onRename_callsRepositoryRenameSet`.
21. GREEN — Wire.

## 4. SetListScreen (Compose)
Files: `ui/setlist/SetListScreen.kt`; light instrumented test
`app/src/androidTest/java/.../ui/setlist/SetListScreenTest.kt` (Hilt test
module swapping in an in-memory DB per PRD §10 "critical flows only" — keep
to one flow here).

22. RED — `SetListScreenTest.createSet_appearsInList` — launch screen with
    Hilt test rule, tap FAB, type name, confirm, assert list item visible.
23. GREEN — Build `SetListScreen` composable (list, FAB → dialog, empty
    state, swipe-or-menu delete with confirm dialog) wired to `SetListViewModel`.
24. GREEN — Replace `Screen.SetList` placeholder in `MainActivity` NavHost with
    real `SetListScreen`; `onSetClick` navigates to `Screen.SetDetail(setId)`
    route (still a placeholder destination until F03).

## 5. Refactor
25. REFACTOR — Extract shared confirm-dialog composable if rename/delete duplicate
    it; confirm all F01+F02 tests still green.

## Definition of done
`./gradlew test connectedAndroidTest` green; manually create/rename/delete
a set in a debug build.
