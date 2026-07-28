# Flash Cards — Product Requirements Document

## 1. Overview
Personal, fully offline Android flashcard app for study/practice. Built as a
learning project for Kotlin + Jetpack Compose, Room, Hilt, and Navigation
Compose. Single user, no accounts, no network access.

## 2. Goals / Non-goals
**Goals**
- Clean, idiomatic MVVM + Repository architecture using recommended Jetpack
  libraries, without overengineering for a single-user app.
- Cover the 7 functional requirements below end-to-end with tests.

**Non-goals**
- No multi-user / auth / cloud sync.
- No full spaced-repetition algorithm (SM-2 etc.) — only a light
  "weight toward mistakes" selection heuristic (see §6).
- No rich media (images/audio) on cards — text only, v1.

## 3. Functional requirements

| # | Requirement |
|---|---|
| 1 | User can create multiple sets of cards |
| 2 | Cards have front text, back text, optional notes |
| 3 | Tags can be attached to cards |
| 4 | User starts a practice session on a set, picks how many cards to include; session ends with a results screen listing correct/incorrect cards |
| 5 | Practice session history is persisted and browsable |
| 6 | User can search cards within a set |
| 7 | User can import/export all card sets as a single JSON file |

## 4. Data model (Room)

All primary/foreign keys are `kotlin.uuid.Uuid` (`Uuid.random()`), generated
in the repository layer at creation time (not Room autogenerate, which only
applies to `Long`). A Room `TypeConverter` maps `Uuid` <-> `String` at the
persistence boundary, so the DB column is `TEXT` but Kotlin code everywhere
above the converter works with `Uuid`, not raw strings. UUIDs avoid ID
collisions across export/import/merge and don't leak row-count/creation-order
information.

**CardSet**
- `id: Uuid` (PK)
- `name: String`
- `createdAt: Instant`

**Card**
- `id: Uuid` (PK)
- `setId: Uuid` (FK → CardSet, cascade delete)
- `front: String`
- `back: String`
- `notes: String?`
- `timesCorrect: Int` (default 0)
- `timesIncorrect: Int` (default 0)
- `lastPracticedAt: Instant?`

**Tag**
- `id: Uuid` (PK)
- `name: String` (unique)

**CardTagCrossRef** (many-to-many)
- `cardId: Uuid` (FK → Card)
- `tagId: Uuid` (FK → Tag)
- composite PK (`cardId`, `tagId`)

**PracticeSession**
- `id: Uuid` (PK)
- `setId: Uuid` (FK → CardSet)
- `startedAt: Instant`
- `finishedAt: Instant`
- `requestedCardCount: Int`

**PracticeSessionResult** (one row per card seen in a session)
- `id: Uuid` (PK)
- `sessionId: Uuid` (FK → PracticeSession, cascade delete)
- `cardId: Uuid` (FK → Card)
- `wasCorrect: Boolean`

Indices: `Card.setId`, `PracticeSession.setId`, `PracticeSessionResult.sessionId`,
`CardTagCrossRef(cardId, tagId)`.

Per-card `timesCorrect`/`timesIncorrect`/`lastPracticedAt` are denormalized
onto `Card` for cheap weighted-selection queries (§6) — full per-attempt
history still lives in `PracticeSessionResult`.

## 5. Architecture
- **Pattern**: MVVM + Repository, single `:app` module (no need for
  multi-module in a personal project).
- **DI**: Hilt. One `Repository` per aggregate (`CardSetRepository`,
  `PracticeRepository`) backed by Room DAOs.
- **UI**: Jetpack Compose, Material 3, single Activity + Navigation Compose.
- **State**: `ViewModel` + `StateFlow`/`UiState` data classes per screen,
  collected via `collectAsStateWithLifecycle`.
- **Async**: Kotlin coroutines/Flow throughout; Room DAOs return `Flow` for
  observable queries.

### Screens (Navigation Compose graph)
1. **Set List** — list of sets, create/rename/delete, tap → Set Detail.
2. **Set Detail** — card list for a set, search bar (req. 6), add/edit/delete
   card, tag filter chips, "Start Practice" button.
3. **Card Editor** — front/back/notes fields, tag picker (create-on-type).
4. **Session Config** — slider/stepper for card count (clamped to set size),
   start button.
5. **Session Play** — one card at a time: front shown, tap to flip → back +
   notes, then Correct/Incorrect buttons; progress indicator (e.g. 4/10).
6. **Session Results** — summary (X/Y correct), two lists: correct cards,
   incorrect cards.
7. **History List** — past sessions per set (or all), date, score.
8. **History Detail** — reuses Session Results layout for a past session.
9. **Import/Export** — export button (writes JSON via SAF file picker),
   import button (pick JSON file via SAF, validate, merge/replace choice).

## 6. Practice session algorithm

**Card count**: user picks N via slider (1..setSize), default = min(10, setSize).

**Selection (weighted toward past mistakes)**:
- Each card gets a weight: `weight = 1 + timesIncorrect - min(timesCorrect, timesIncorrect)`,
  floored at a minimum of `1`. Cards never practiced default to weight `1`
  (neutral — still eligible, not penalized).
- Draw N cards via weighted random sampling without replacement
  (e.g. A-ES / bucketed weighted reservoir sampling — small N and set sizes,
  so a simple weighted shuffle is fine, no need for a fancy algorithm).

**Recording results**:
- On each flip+answer, buffer the result client-side in the ViewModel.
- On session completion, write one `PracticeSession` row + N
  `PracticeSessionResult` rows in a single Room transaction, and update
  `timesCorrect`/`timesIncorrect`/`lastPracticedAt` on each touched `Card`.

## 7. Search (req. 6)
- Search box on Set Detail filters the set's cards client-side (Room query
  `WHERE setId = :id AND (front LIKE :q OR back LIKE :q OR notes LIKE :q)`,
  bound parameters — no string concatenation).
- Optional tag-chip filter combined with text search (AND).

## 8. Import / Export (req. 7)

**Format**: whole-library JSON, one file = full backup.

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-07-28T12:00:00Z",
  "sets": [
    {
      "name": "Kotlin Basics",
      "cards": [
        {
          "front": "What is a data class?",
          "back": "A class that auto-generates equals/hashCode/toString/copy",
          "notes": "Ch. 4",
          "tags": ["kotlin", "syntax"]
        }
      ]
    }
  ]
}
```

- Practice stats/history are **not** exported — import always starts cards
  at zero stats. Keeps the format simple and avoids merge conflicts with
  local history.
- **Export**: serialize with kotlinx.serialization, write via Storage Access
  Framework `ACTION_CREATE_DOCUMENT` (no broad storage permission needed).
- **Import**: read via SAF `ACTION_OPEN_DOCUMENT`, deserialize with strict
  schema validation (reject unknown/missing required fields, cap
  string lengths, cap set/card counts to a sane max e.g. 10k cards) before
  touching the DB. On success, ask user Replace-all vs Add-as-new-sets.
  Whole import runs in one Room transaction (all-or-nothing).

## 9. Security & good-practice notes (personal app, right-sized)
- No `INTERNET` permission — fully offline, nothing to request.
- Import/export via SAF, not legacy broad storage permissions.
- All DB access through Room typed DAOs/`@Query` with bound params — no raw
  SQL concatenation.
- Import JSON validated (size caps, type checks) before DB writes; malformed
  file fails cleanly with a user-facing error, never partial-writes.
- R8/ProGuard minification + resource shrinking enabled for release builds.
- Default Android backup rules reviewed once (already scaffolded in
  `res/xml/backup_rules.xml` / `data_extraction_rules.xml`) — fine as-is for
  a personal, non-sensitive-data app.
- No hardcoded secrets (none needed — no network/API keys).

## 10. Testing strategy
- **Unit tests (JUnit + coroutines-test)**: ViewModels (session config,
  session play scoring, search filtering), weighted-selection algorithm,
  import JSON validation/mapping.
- **Room DAO tests**: in-memory Room DB, instrumented, covering CRUD +
  cascade deletes + search query + weighted-selection query.
- **UI**: light Compose UI tests for critical flows only (create set → add
  card → run session → see result) — skip exhaustive screen-by-screen
  coverage, this is a personal project.

## 11. Tech stack
Kotlin, Jetpack Compose, Navigation Compose, Room, Hilt, Material 3,
kotlinx.serialization (JSON), kotlinx.coroutines, JUnit4 +
kotlinx-coroutines-test + Room testing artifacts.

## 12. Suggested build order (milestones)
1. Data layer: Room entities/DAOs + Hilt DB module.
2. Set List + Set Detail (CRUD for sets/cards/tags) + search.
3. Session Config + Session Play + Session Results (weighted selection).
4. History List/Detail.
5. Import/Export.
6. Polish: empty states, validation, tests backfilled per milestone (don't
   defer all testing to the end).
