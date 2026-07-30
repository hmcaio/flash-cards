# F05 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. WeightedCardSelector (pure unit tests, no Android)
Files: `test/.../domain/WeightedCardSelectorTest.kt`, prod
`domain/WeightedCardSelector.kt` (a `common`/`domain` package is fine here
since it's the one piece of real algorithmic logic worth isolating).

1. RED — `select_countEqualsListSize_returnsAllCardsAnyOrder`.
2. GREEN — Minimal impl: if `count >= cards.size`, return `cards`.
3. RED — `select_uniformWeights_countLessThanSize_returnsDistinctSubsetOfCorrectSize`.
4. GREEN — Implement Efraimidis-Spirakis weighted-without-replacement
   sampling with `weight = 1` fallback.
5. RED — `select_deterministicWithSeededRandom_sameSeedSameResult` (pass a
   `Random(42)` twice, assert identical output — locks in determinism for
   the statistical test below and for future debugging).
6. GREEN — (should already pass — confirms `Random` is threaded through, not
   `Random.Default` captured internally).
7. RED — `select_cardWithHigherIncorrectWeight_selectedMoreOftenOverManyTrials`
   (seeded loop of e.g. 2000 draws of count=1 from a 2-card list, one with
   `timesIncorrect=10`, assert its selection frequency is meaningfully
   higher than 50% within a tolerance band).
8. GREEN — Implement the real `weight = max(1, 1 + timesIncorrect - min(timesCorrect, timesIncorrect))`
   formula (steps 1-6 could've used a stub weight=1 always; this is where
   the real formula is required to go green).

## 2. PracticeRepository (unit, fake DAOs + fake IdGenerator/TimeProvider)
Files: `test/.../data/repository/PracticeRepositoryTest.kt`.

9. RED — `startSession_selectsCardsViaSelector_returnsDraftWithGeneratedId`.
10. GREEN — `PracticeRepositoryImpl.startSession` (load cards for set via
    `CardDao`, call injected `WeightedCardSelector`, build draft).
11. RED — `startSession_doesNotWriteAnythingToDb` (fake DAO's insert methods
    asserted not called).
12. GREEN — (should already hold — confirms no premature writes).
13. RED — `completeSession_insertsOneSessionAndNResultRows`.
14. GREEN — `completeSession`: build entities, call `PracticeSessionDao.insert`
    + `insertResults` inside the DAO's `@Transaction` method.
15. RED — `completeSession_updatesCardStats_incrementsCorrectOrIncorrectAndSetsLastPracticedAt`.
16. GREEN — Loop answers, call `CardDao.updateStats` per card.
17. RED — `completeSession_returnsSummaryWithCorrectAndIncorrectCardLists`.
18. GREEN — Build `PracticeSessionSummary` from the answers + original card list.
19. GREEN — Bind in `RepositoryModule`; bind `WeightedCardSelector` too.

## 3. SessionConfigViewModel (unit, fake repo)
20. RED — `initialState_defaultSelectedCount_isMinOf10AndSetSize`.
21. GREEN — Compute default in `init`.
22. RED — `onCountChange_clampsToOneAndSetSize`.
23. GREEN — Clamp logic.
24. RED — `onStartClick_callsStartSessionWithSelectedCount_emitsNavigationEvent`.
25. GREEN — Wire.

## 4. SessionPlayViewModel (unit, fake repo, pre-built draft)
26. RED — `initialState_showsFirstCardFrontNotFlipped`.
27. GREEN — Seed state from draft's card list.
28. RED — `onFlip_showsBack`.
29. GREEN — Toggle `isFlipped`.
30. RED — `onAnswer_advancesToNextCard_resetsFlipped`.
31. GREEN — Advance index, reset flip, buffer answer.
32. RED — `onAnswer_lastCard_setsSessionCompleteAndCallsCompleteSession`.
33. GREEN — On last index, call `practiceRepository.completeSession`, store
    summary for navigation to Results.
34. RED — `abandoningSession_neverCallsCompleteSession` (viewmodel cleared /
    back-press path — assert fake repo's `completeSession` not invoked).
35. GREEN — Confirm no auto-save-on-clear exists (should already hold).

## 5. SessionResultsViewModel + Screens (Compose)
36. GREEN — `SessionResultsViewModel` — thin, just exposes the summary passed
    via nav args/SavedStateHandle as `SessionResultsUiState` (no test
    needed beyond a trivial mapping check if desired).
37. GREEN — Build `SessionConfigScreen` (slider bound to `selectedCount`, Start
    button), `SessionPlayScreen` (card flip animation optional/simple,
    Correct/Incorrect buttons, progress "3/10" text), `SessionResultsScreen`
    (score header, two `LazyColumn` sections).
38. GREEN — Replace `Screen.SessionConfig/SessionPlay/SessionResults`
    placeholders in `MainActivity` NavHost; wire "Start Practice" button on
    `SetDetailScreen` (F03) to navigate here for real (`setId` nav arg
    parsed to `Uuid` at the boundary, per F01 spec's nav-arg convention).

## 6. End-to-end acceptance test (PRD §10)
39. RED — `androidTest/.../EndToEndPracticeFlowTest.kt` —
    `createSetAddCardRunSessionSeeResult` — full Compose flow: create set
    → add one card → start practice (count=1) → flip → mark correct → land
    on results screen showing 1/1 correct.
40. GREEN — Fix any wiring gaps surfaced by this test until green.

## 7. Refactor
41. REFACTOR — Confirm `WeightedCardSelector` has zero Android/Room imports (stays
    a pure/fast unit test); all F01–F05 tests green.
42. REFACTOR — Add `@Preview`s per the project's Compose preview convention
    (`docs/features/README.md`) for `SessionConfigScreen` (slider states),
    `SessionPlayScreen` (front shown, flipped to back, near-end progress),
    and `SessionResultsScreen` (mixed correct/incorrect results), each
    split into a stateless overload first if not already done.

## Definition of done
`./gradlew test connectedAndroidTest` green including the E2E test; manual
run of a full practice session on a real set.
