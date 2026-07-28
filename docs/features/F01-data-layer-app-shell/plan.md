# F01 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. Project wiring
1. GREEN — Add Hilt, Room, kotlinx-serialization, coroutines-test, Room-testing,
   Turbine deps to `gradle/libs.versions.toml` + `app/build.gradle.kts`.
   Apply KSP (`com.google.devtools.ksp`) for Room+Hilt codegen — not KAPT —
   plus the `com.google.dagger.hilt.android` plugin. Add a compiler opt-in
   for `kotlin.uuid.ExperimentalUuidApi` (`freeCompilerArgs` or
   `optIn`/`languageSettings` in `app/build.gradle.kts`) so `Uuid` can be
   used across the codebase without per-file `@OptIn` annotations.
2. GREEN — `FlashCardsApplication` (`@HiltAndroidApp`), register in
   `AndroidManifest.xml` (`android:name=".FlashCardsApplication"`), remove
   `INTERNET` permission if present (it isn't, per prior analysis — verify).
3. GREEN — Enable `isMinifyEnabled = true` / `isShrinkResources = true` on
   `release` build type in `app/build.gradle.kts`.

## 2. Shared utils (unit-testable, no Android/Room dependency)
Files: `app/src/main/java/com/chm/flashcards/common/IdGenerator.kt`,
`common/TimeProvider.kt`.

4. RED — `app/src/test/java/com/chm/flashcards/common/UuidIdGeneratorTest.kt` —
   assert two calls return distinct `Uuid` values.
5. GREEN — `IdGenerator` interface (`fun newId(): Uuid`) + `UuidIdGenerator`
   impl (`Uuid.random()`).
6. RED — `common/SystemTimeProviderTest.kt` — assert `now()` returns an
   `Instant` close to a manually captured `Instant.now()` (delta < 1s).
7. GREEN — `TimeProvider` interface + `SystemTimeProvider` impl.
8. GREEN — `common/FakeIdGenerator.kt` (returns a fixed/sequenced list of
   `Uuid`s), `common/FakeTimeProvider.kt` test doubles in
   `app/src/test/java/.../common/` (or a `testFixtures` source set) for
   reuse by every later feature's unit tests.

## 3. Entities + Converters
Files: `data/entity/*.kt`, `data/Converters.kt`.

9. GREEN — Write all 6 `@Entity` data classes (all PK/FK fields typed
   `Uuid`) + `Converters` (`Instant<->Long`, `Uuid<->String`) exactly per
   spec.md (no test — pure data classes, nothing to assert beyond
   compilation which the DAO tests below exercise).

## 4. CardSet + Card DAOs (instrumented, in-memory Room)
Files: `app/src/androidTest/java/com/chm/flashcards/data/dao/CardSetDaoTest.kt`,
`CardDaoTest.kt`; prod: `data/dao/CardSetDao.kt`, `CardDao.kt`,
`data/FlashCardsDatabase.kt` (start with just these two entities registered).

10. RED — `CardSetDaoTest.insertAndGetById_returnsInsertedSet` — build in-memory
    `FlashCardsDatabase`, insert a `CardSetEntity`, assert `getById` returns
    it. Fails: no DAO/DB class exists yet.
11. GREEN — Create `FlashCardsDatabase` (entities = [CardSetEntity, CardEntity],
    `Converters` registered), `CardSetDao` with `insert`/`getById`.
12. RED — `CardSetDaoTest.getAll_emitsUpdatedListOnInsert` (Turbine, collect
    `Flow`, insert, assert new item emitted).
13. GREEN — Add `getAll(): Flow<List<CardSetEntity>>`.
14. RED — `CardSetDaoTest.delete_removesSet`.
15. GREEN — Add `delete`.
16. RED — `CardDaoTest.insertUnderSet_getBySetId_returnsCard`.
17. GREEN — `CardDao` with FK to `CardSetEntity`, `insert`, `getBySetId`.
18. RED — `CardDaoTest.deletingSet_cascadesDeleteOfCards` — insert set + card
    under it, delete set via `CardSetDao`, assert `CardDao.getBySetId`
    emits empty list.
19. GREEN — Confirm `ForeignKey(onDelete = CASCADE)` is set (should already pass
    if entity written correctly in step 9 — if red, fix the annotation).
20. RED — `CardDaoTest.updateStats_persistsCorrectAndIncorrectCounts`.
21. GREEN — Add `updateStats` query.

## 5. Tag + CardTagCrossRef DAOs
Files: `data/dao/TagDao.kt`, `CardTagCrossRefDao.kt`, matching
`androidTest` files.

22. RED — `TagDaoTest.insertAndGetByName_returnsTag`.
23. GREEN — `TagEntity` registered in DB, `TagDao.insert`/`getByName`.
24. RED — `CardTagCrossRefDaoTest.attachTagToCard_getTagsForCard_returnsTag`.
25. GREEN — `CardTagCrossRef` registered, `CardTagCrossRefDao.insert`/`getTagsForCard`.
26. RED — `CardTagCrossRefDaoTest.deletingCard_cascadesDeleteOfCrossRef`.
27. GREEN — Verify cascade FK (fix if red).
28. RED — `CardTagCrossRefDaoTest.deletingTag_cascadesDeleteOfCrossRef`.
29. GREEN — Verify cascade FK for tag side.

## 6. PracticeSession + PracticeSessionResult DAOs
Files: `data/dao/PracticeSessionDao.kt`, matching `androidTest` file.

30. RED — `PracticeSessionDaoTest.insertSessionWithResults_getResultsBySessionId_returnsAll`
    — insert session, `insertResults(listOf(...))`, assert query returns them.
31. GREEN — Register both entities in DB, `PracticeSessionDao.insert`, `insertResults`
    (as a single `@Transaction` method), `getResultsBySessionId`.
32. RED — `PracticeSessionDaoTest.deletingSession_cascadesDeleteOfResults`.
33. GREEN — Verify cascade FK.
34. RED — `PracticeSessionDaoTest.getSessionsBySetId_ordersByStartedAtDescending`.
35. GREEN — Add `getSessionsBySetId` with `ORDER BY startedAt DESC`.

## 7. Hilt DI module
36. GREEN — `di/DatabaseModule.kt` providing `FlashCardsDatabase` (singleton,
    `Room.databaseBuilder`) + each DAO. `di/UtilModule.kt` `@Binds`
    `IdGenerator`->`UuidIdGenerator`, `TimeProvider`->`SystemTimeProvider`.
    (No dedicated test — verified indirectly once F02's Hilt-injected
    repository test runs green.)

## 8. Navigation scaffold
37. GREEN — `ui/navigation/Screen.kt` sealed class with all routes.
38. GREEN — `MainActivity` (`@AndroidEntryPoint`) hosting `NavHost` with
    placeholder composables per route.
39. RED — (light) `app/src/androidTest/java/.../MainActivityTest.kt` — launch
    activity, assert Set List placeholder text is displayed (smoke test
    that Hilt + Nav wiring doesn't crash).
40. GREEN — Make it pass (fix any DI/nav wiring issues).

## 9. Schema export + wrap-up
41. GREEN — Enable `exportSchema = true`, set `room { schemaDirectory("$projectDir/schemas") }`,
    run a build to generate `app/schemas/*.json`, commit it.
42. REFACTOR — Dedupe DAO test setup (shared `RoomDbTestRule` or
    `@Before` helper building the in-memory DB), confirm all tests green.

## Definition of done
All checkboxes in `spec.md` acceptance criteria checked; `./gradlew test connectedAndroidTest`
green.
