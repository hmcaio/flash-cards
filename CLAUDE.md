# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Personal, fully offline Android flashcard app — a learning project for Kotlin + Jetpack Compose, Room, Hilt, and Navigation Compose. Single user, no accounts, no network access (no `INTERNET` permission). Single Gradle module: `:app`, package `com.chm.flashcards`.

Full scope lives in [docs/PRD.md](docs/PRD.md). Read it before starting a new feature.

## Commands

```bash
./gradlew assembleDebug              # build debug APK
./gradlew assembleRelease            # build release APK (R8 minify + resource shrink on)
./gradlew test                       # unit tests (app/src/test)
./gradlew test --tests "com.chm.flashcards.common.UuidIdGeneratorTest"   # single unit test
./gradlew connectedDebugAndroidTest  # instrumented tests (app/src/androidTest) -- needs an emulator/device
./gradlew connectedDebugAndroidTest --tests "com.chm.flashcards.data.dao.CardDaoTest"  # single instrumented test
./gradlew lint
./gradlew installDebug               # install on a connected device/emulator
```

On native Windows shells use `gradlew.bat` instead of `./gradlew`.

## Feature-driven workflow

Development is planned per-feature under `docs/features/F0X-*/`, each with a `spec.md` (technical spec) and `plan.md` (ordered TDD action plan: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green). [docs/features/README.md](docs/features/README.md) has the full dependency-ordered list (F01–F07) and must be built in that order — each feature depends on the ones before it.

When implementing a feature, follow its `plan.md` steps in order rather than improvising a different structure — the plan already accounts for what earlier features set up (shared fakes, DAO surface, nav conventions, etc.).

**Current state**: F01–F07 are all implemented and merged into `main` — the app is feature-complete (Set List, Set Detail with search/tags, Card Editor, Session Config/Play/Results, History List/Detail, Import/Export). No `Text("TODO: ...")` placeholders remain in the nav graph.

**Branching**: GitHub flow — `main` is the only long-lived branch, feature branches merge into it directly via PR (no `develop`).

## Architecture

**MVVM + Repository, no domain/use-case layer.** This is an intentional simplicity decision for a single-user personal app (see PRD §5/§2) — don't introduce use-case classes or a domain module. Each feature owns one repository per aggregate (e.g. `CardSetRepository`, `PracticeRepository`), backed directly by Room DAOs. The `domain/` package (`WeightedCardSelector`) is an exception in name only — it's a single pure algorithm strategy interface (practice-session card selection), not a use-case layer; don't grow it into one.

**IDs are `kotlin.uuid.Uuid`, never raw `String`, everywhere above the persistence boundary.** Every entity PK/FK is `Uuid`. A single `Converters` class (`app/src/main/java/com/chm/flashcards/data/Converters.kt`) is the only place `Uuid <-> String` (and `Instant <-> Long`) conversion happens, via Room `@TypeConverter`. Module-wide opt-in for `kotlin.uuid.ExperimentalUuidApi` is set in `app/build.gradle.kts` (`KotlinCompile` task config) — don't add per-file `@OptIn` instead.

**IDs and timestamps only come from `IdGenerator`/`TimeProvider`** (`common/IdGenerator.kt`, `common/TimeProvider.kt`), injected via Hilt (`di/UtilModule.kt`). Never call `Uuid.random()` or `Instant.now()` directly outside their real implementations (`UuidIdGenerator`, `SystemTimeProvider`) — everything else, including all repositories, takes these as constructor dependencies so tests can substitute `FakeIdGenerator`/`FakeTimeProvider` (in `app/src/test/.../common/`) for deterministic assertions.

**Navigation**: `ui/navigation/Screen.kt` is a sealed class, one object per screen route, each with a `createRoute(...)` helper for screens that take args. Nav Compose route arguments are always `String` (there's no `Uuid` `NavType`) — a screen taking an id arg parses it with `Uuid.parse(arg)` immediately at the screen/ViewModel entry point (via `SavedStateHandle` in the ViewModel constructor), so `Uuid` is the type used everywhere past that boundary. `ui/navigation/FlashCardsNavHost.kt` wires the whole graph; `MainActivity` just hosts it inside a `Scaffold`. When a multi-screen sub-flow shouldn't be revisited via back (e.g. Session Config/Play once a session finishes), collapse it with `popUpTo(...)` on the `navigate()` call that lands on the sub-flow's terminal screen — see the `SessionPlay` → `SessionResults` call in `FlashCardsNavHost.kt` — so both an explicit back button and the system back gesture reach the correct prior screen with a single pop.

**DI (Hilt)**: one `@Module` per concern rather than one giant module — `di/DatabaseModule.kt` provides the Room database + DAOs, `di/UtilModule.kt` binds `IdGenerator`/`TimeProvider`, `di/RepositoryModule.kt` binds every repository (`CardSetRepository`, `CardRepository`, `TagRepository`, `PracticeRepository`, `ImportExportRepository`) plus `WeightedCardSelector`/`TransactionRunner`, `di/DocumentIoModule.kt` binds the SAF `DocumentReader`/`DocumentWriter` used by import/export (kept separate from `RepositoryModule` so androidTest can swap it wholesale via `@TestInstallIn`), `di/PreferencesModule.kt` (added in C004) provides the app's single Preferences DataStore and binds `ViewModePreferences`. Follow this same one-module-per-concern pattern for anything added later rather than growing an existing module indefinitely.

**Cross-feature utilities added after F01**: `data/TransactionRunner.kt` (+ `RoomTransactionRunner`/`FakeTransactionRunner`) wraps `RoomDatabase.withTransaction` behind an interface, the same fakeable-dependency pattern as `IdGenerator`/`TimeProvider`, for any repository that needs an atomic multi-row write (practice session recording, import). `common/CardValidationRules.kt` centralizes field-length/tag-count limits shared by `CardEditorViewModel` (manual entry) and `ImportValidator` (import) — add new shared validation constants here rather than duplicating magic numbers per call site. `data/preferences/ViewModePreferences.kt` (added in C004) is this project's first non-Room persistence mechanism: a small persisted setting (currently the Set List/Set Detail list-vs-grid `ViewMode`) backed by Jetpack DataStore Preferences (`androidx.datastore:datastore-preferences`), following the same fakeable-dependency pattern (`ViewModePreferences` interface, `DataStoreViewModePreferences` real impl bound in `di/PreferencesModule.kt`, `FakeViewModePreferences` in `app/src/test/.../common/`). Reuse this pattern — one `DataStore<Preferences>` singleton, one interface per logical setting (or a group of related ones) — for any future simple persisted setting rather than reinventing it or reaching for Room.

**Icons**: `androidx.compose.material:material-icons-core` is available (BOM-managed, added in C003) — use `Icons.Default.*` (e.g. `Icons.Default.Add`) for any icon a screen needs, with a real `contentDescription`, rather than re-adding a dependency or falling back to text/emoji. `androidx.compose.material:material-icons-extended` was added in C004 (also BOM-managed, no `version.ref`) for icons missing from the small `material-icons-core` set (e.g. `Icons.Default.GridView`, `Icons.AutoMirrored.Filled.ViewList`) — prefer `material-icons-core` first and only reach for an extended icon when core doesn't have it.

**Compose previews**: every screen and reusable component gets an `@Preview` (added during that feature's own Refactor step, per [docs/features/README.md](docs/features/README.md)). Screens backed by `hiltViewModel()` split into a public stateful entry point + a private stateless overload (`UiState` + plain lambdas) so the stateless one can be previewed — see `ui/setlist/SetListScreen.kt` (F02).

**Shared cross-screen UI**: `ui/components/` (added in C008, `TagFilterChipRow.kt`) is for a Composable used by more than one screen — before C008 every screen owned all its own UI, including near-duplicate implementations of the same piece (e.g. Set Detail's and Session Config's tag filter chip rows). Extract into `ui/components/` (own file, own `@Preview`s) rather than re-duplicating a screen-local implementation when a later feature/chore needs the same piece elsewhere.

**Testing structure**:
- Unit tests (`app/src/test/`) use fakes, no Android/Room dependency — fast, run on the JVM. `common/MainDispatcherRule.kt` swaps `Dispatchers.Main` for ViewModel tests that use `viewModelScope`; reuse it rather than rolling a new dispatcher rule per test class.
- Instrumented tests (`app/src/androidTest/`) cover Room DAOs (in-memory database) and Compose screens. DAO tests extend `data/dao/BaseRoomDaoTest.kt`, which builds/tears down a fresh in-memory `FlashCardsDatabase` per test — don't duplicate that boilerplate in new DAO test classes. Compose+Hilt UI tests swap in test doubles via `@TestInstallIn`: `di/TestDatabaseModule.kt` (in-memory DB, replacing `DatabaseModule`) and `di/TestDocumentIoModule.kt` (in-memory document store, replacing `DocumentIoModule`) — reuse both rather than re-declaring the swap per test.
- `HiltTestRunner` (custom `testInstrumentationRunner`) boots a `HiltTestApplication` for `@HiltAndroidTest` classes instead of the real `FlashCardsApplication`.
- Compose UI tests use `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule` — the current, non-deprecated import (not the `v1` package under the same `junit4` namespace).
- Room schema is exported (`app/schemas/`) — commit the generated schema JSON whenever the schema version changes.

**Build tooling notes**:
- KSP, not KAPT, for Room and Hilt annotation processing.
- `gradle.properties` sets `android.disallowKotlinSourceSets=false` — a workaround for a known KSP/AGP-9 incompatibility ([google/ksp#2729](https://github.com/google/ksp/issues/2729)). Keep it until KSP ships a fix; don't remove it while diagnosing unrelated build issues.

## Versioning & releases

Semantic versioning via `VERSION_NAME`/`VERSION_CODE` in `gradle.properties` (see [RELEASE.md](RELEASE.md) for the full cut-a-release process). Every feature PR should add its own entry under `CHANGELOG.md`'s `[Unreleased]` section as part of that PR — since PRs merge straight into `main` with no `develop` buffer, there's no later staging point to backfill missed entries from.
