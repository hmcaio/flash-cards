# F06 — Practice History

Status: done
Depends on: F01, F05
PRD refs: §3 req 5, §5 screens "History List/Detail"

## Scope

**In scope**
- `PracticeRepository` additions: `getSessionsForSet(setId): Flow<List<PracticeSessionListItem>>`
  (date, score X/Y, derived from `requestedCardCount` + count of correct
  results — via a DAO query, not loaded-then-computed in Kotlin).
- `HistoryListViewModel`/`Screen`: sessions for the current set, newest
  first, each row shows date + score, tap → `HistoryDetail`.
- `HistoryDetailViewModel`/`Screen`: reuses the same layout as
  `SessionResultsScreen` (F05) — extract a shared
  `SessionOutcomeView(correct: List<Card>, incorrect: List<Card>)`
  composable in F05... actually extracted *here* since F05 doesn't need the
  reuse itself; this feature does the extraction when it needs the second
  caller.

**Out of scope**
- Cross-set "all history" view — PRD req 5 just says "a history of practice
  sessions must be kept", and every other flow is set-scoped (Set Detail →
  Start Practice), so History List is reached from Set Detail and scoped to
  that set. Revisit only if the user actually wants a global view later.

## Data

```kotlin
data class PracticeSessionListItem(val sessionId: Uuid, val startedAt: Instant, val correctCount: Int, val totalCount: Int)
```
DAO query (`PracticeSessionDao`):
```kotlin
@Query("""
  SELECT s.id AS sessionId, s.startedAt AS startedAt,
         (SELECT COUNT(*) FROM practice_session_results r WHERE r.sessionId = s.id AND r.wasCorrect = 1) AS correctCount,
         (SELECT COUNT(*) FROM practice_session_results r WHERE r.sessionId = s.id) AS totalCount
  FROM practice_sessions s
  WHERE s.setId = :setId
  ORDER BY s.startedAt DESC
""")
fun getSessionListItems(setId: Uuid): Flow<List<PracticeSessionListItem>>
```
`HistoryDetailViewModel` loads full `PracticeSessionResultEntity` rows for
one `sessionId` joined with `Card` (front/back) to render — add
`PracticeSessionDao.getResultsWithCardsBySessionId(sessionId)`.

## Known edge case (accepted, documented per PRD "avoid overcomplicating")
Deleting a card (F03) cascades to delete its `PracticeSessionResult` rows
(FK `cardId → cards.id ON DELETE CASCADE`, per F01). Consequence: a past
session's score can silently shrink (`totalCount` drops) if a card involved
in it is later deleted. This is accepted for v1 — a personal app doesn't
need historical snapshots of deleted content. `HistoryDetailScreen` does
not need special-case UI for this; the query simply returns fewer rows.

## Acceptance criteria
- [ ] After completing a session (F05), it appears at the top of
      History List for that set with the correct score.
- [ ] Tapping a history entry shows the same correct/incorrect card lists
      as the original Session Results screen.
- [ ] History List empty state ("No sessions yet") when a set has never
      been practiced.
- [ ] `HistoryListScreen`, `HistoryDetailScreen`, and `SessionOutcomeView`
      have `@Preview`s per the project's Compose preview convention.
