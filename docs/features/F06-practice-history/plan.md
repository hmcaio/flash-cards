# F06 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. DAO queries
Files: `androidTest/.../data/dao/PracticeSessionDaoTest.kt` (extend).

1. RED — `getSessionListItems_returnsScoreAndOrdersByStartedAtDescending`
   (insert 2 sessions with results, one 3/5 one 1/2, assert order + counts).
2. GREEN — Add `PracticeSessionDao.getSessionListItems(setId)` per spec.md SQL.
3. RED — `getResultsWithCardsBySessionId_returnsResultsJoinedWithCardFrontBack`.
4. GREEN — Add `PracticeSessionResultWithCard` projection + DAO query
   (`INNER JOIN cards`).
5. RED — `getResultsWithCardsBySessionId_deletedCard_excludesThatResult`
   (insert result referencing a card, delete the card, assert result gone
   — locks in the accepted cascade behavior from spec.md as a regression test).
6. GREEN — (should already pass from F01's cascade FK — this test documents the
   behavior rather than driving new code; if it fails, the FK is missing).

## 2. PracticeRepository additions (unit, fake DAO)
Files: `test/.../data/repository/PracticeRepositoryTest.kt` (extend).

7. RED — `getSessionsForSet_mapsDaoProjectionToDomainListItem`.
8. GREEN — `PracticeRepositoryImpl.getSessionsForSet`.
9. RED — `getSessionDetail_mapsResultsWithCardsToCorrectAndIncorrectLists`.
10. GREEN — `getSessionDetail(sessionId): Flow<PracticeSessionSummary>` (reuse
    the `PracticeSessionSummary` type from F05, partitioned by `wasCorrect`).

## 3. HistoryListViewModel (unit, fake repo)
11. RED — `initialState_loadsSessionsForSetId`.
12. GREEN — Collect `getSessionsForSet(setId)`.
13. RED — `onSessionClick_emitsNavigationEventWithSessionId`.
14. GREEN — Wire.

## 4. HistoryDetailViewModel (unit, fake repo)
15. RED — `initialState_loadsSessionDetailBySessionId`.
16. GREEN — Collect `getSessionDetail(sessionId)`.

## 5. Shared results view extraction
17. REFACTOR — Extract `SessionOutcomeView(correct, incorrect)` composable out of
    F05's `SessionResultsScreen` into `ui/session/SessionOutcomeView.kt` —
    run F05's existing Compose test for `SessionResultsScreen` to confirm
    no visual/behavioral regression before adding a second caller.

## 6. Screens (Compose) + nav wiring
18. RED — `androidTest/.../ui/history/HistoryListScreenTest.kt` —
    `afterCompletingSession_appearsInHistoryList` (run a session via F05's
    flow, navigate to History List, assert row visible with correct score).
19. GREEN — Build `HistoryListScreen` (LazyColumn of date+score rows, empty
    state, tap → `HistoryDetail`).
20. GREEN — Build `HistoryDetailScreen` using `SessionOutcomeView`.
21. GREEN — Replace `Screen.HistoryList`/`Screen.HistoryDetail` placeholders in
    `MainActivity` NavHost (`sessionId` nav arg parsed to `Uuid` at the
    boundary, per F01 spec's nav-arg convention); add a "History" entry
    point button on `SetDetailScreen`.

## 7. Refactor
22. REFACTOR — Confirm `SessionResultsScreen` (F05) and `HistoryDetailScreen` share
    `SessionOutcomeView` with no duplicated layout code; all tests green.
23. REFACTOR — Add `@Preview`s per the project's Compose preview convention
    (`docs/features/README.md`) for `HistoryListScreen` (empty/populated),
    `HistoryDetailScreen`, and `SessionOutcomeView` itself, splitting each
    into a stateless overload first if not already done.

## Definition of done
`./gradlew test connectedAndroidTest` green; manually run 2+ sessions on a
set and confirm History List/Detail reflect them.
