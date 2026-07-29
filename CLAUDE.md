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

**Current state**: F01 (data layer + app shell) is implemented and merged into `main`. Every other screen is a `Text("TODO: ...")` placeholder wired into the nav graph — no CRUD or business logic exists yet outside F01.

**Branching**: GitHub flow — `main` is the only long-lived branch, feature branches merge into it directly via PR (no `develop`).

## Architecture

**MVVM + Repository, no domain/use-case layer.** This is an intentional simplicity decision for a single-user personal app (see PRD §5/§2) — don't introduce use-case classes or a domain module. Each feature owns one repository per aggregate (e.g. `CardSetRepository`, `PracticeRepository`), backed directly by Room DAOs.

**IDs are `kotlin.uuid.Uuid`, never raw `String`, everywhere above the persistence boundary.** Every entity PK/FK is `Uuid`. A single `Converters` class (`app/src/main/java/com/chm/flashcards/data/Converters.kt`) is the only place `Uuid <-> String` (and `Instant <-> Long`) conversion happens, via Room `@TypeConverter`. Module-wide opt-in for `kotlin.uuid.ExperimentalUuidApi` is set in `app/build.gradle.kts` (`KotlinCompile` task config) — don't add per-file `@OptIn` instead.

**IDs and timestamps only come from `IdGenerator`/`TimeProvider`** (`common/IdGenerator.kt`, `common/TimeProvider.kt`), injected via Hilt (`di/UtilModule.kt`). Never call `Uuid.random()` or `Instant.now()` directly outside their real implementations (`UuidIdGenerator`, `SystemTimeProvider`) — everything else, including all repositories, takes these as constructor dependencies so tests can substitute `FakeIdGenerator`/`FakeTimeProvider` (in `app/src/test/.../common/`) for deterministic assertions.

**Navigation**: `ui/navigation/Screen.kt` is a sealed class, one object per screen route, each with a `createRoute(...)` helper for screens that take args. Nav Compose route arguments are always `String` (there's no `Uuid` `NavType`) — a screen taking an id arg parses it with `Uuid.parse(arg)` immediately at the screen/ViewModel entry point, so `Uuid` is the type used everywhere past that boundary. `ui/navigation/FlashCardsNavHost.kt` wires the whole graph; `MainActivity` just hosts it inside a `Scaffold`.

**DI (Hilt)**: one `@Module` per concern rather than one giant module — `di/DatabaseModule.kt` provides the Room database + DAOs, `di/UtilModule.kt` binds `IdGenerator`/`TimeProvider`. Later features add their own repository-binding modules following the same pattern rather than growing these two.

**Testing structure**:
- Unit tests (`app/src/test/`) use fakes, no Android/Room dependency — fast, run on the JVM.
- Instrumented tests (`app/src/androidTest/`) cover Room DAOs (in-memory database) and Compose screens. DAO tests extend `data/dao/BaseRoomDaoTest.kt`, which builds/tears down a fresh in-memory `FlashCardsDatabase` per test — don't duplicate that boilerplate in new DAO test classes.
- `HiltTestRunner` (custom `testInstrumentationRunner`) boots a `HiltTestApplication` for `@HiltAndroidTest` classes instead of the real `FlashCardsApplication`.
- Room schema is exported (`app/schemas/`) — commit the generated schema JSON whenever the schema version changes.

**Build tooling notes**:
- KSP, not KAPT, for Room and Hilt annotation processing.
- `gradle.properties` sets `android.disallowKotlinSourceSets=false` — a workaround for a known KSP/AGP-9 incompatibility ([google/ksp#2729](https://github.com/google/ksp/issues/2729)). Keep it until KSP ships a fix; don't remove it while diagnosing unrelated build issues.

## Versioning & releases

Semantic versioning via `VERSION_NAME`/`VERSION_CODE` in `gradle.properties` (see [RELEASE.md](RELEASE.md) for the full cut-a-release process). Every feature PR should add its own entry under `CHANGELOG.md`'s `[Unreleased]` section as part of that PR — since PRs merge straight into `main` with no `develop` buffer, there's no later staging point to backfill missed entries from.
