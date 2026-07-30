# F05 — Practice Session

Status: not started
Depends on: F01, F02, F03
PRD refs: §3 req 4, §5 screens "Session Config/Play/Results", §6 algorithm

## Scope

**In scope**
- `WeightedCardSelector`: pure function, weighted-without-replacement
  sampling per PRD §6 formula.
- `PracticeRepository`: `startSession` (selects cards), `completeSession`
  (transactional write of `PracticeSession` + `PracticeSessionResult` rows +
  per-card stat updates).
- `SessionConfigViewModel`/`Screen`: card-count slider (1..setSize, default
  `min(10, setSize)`), start button.
- `SessionPlayViewModel`/`Screen`: one card at a time, tap-to-flip
  (front→back+notes), Correct/Incorrect buttons, progress indicator.
- `SessionResultsViewModel`/`Screen`: score summary, correct list,
  incorrect list.
- The end-to-end Compose UI test named in PRD §10
  ("create set → add card → run session → see result") — this is the first
  feature where the full chain is exercisable, so it lands here.

**Out of scope**
- Persisting/browsing past sessions beyond the just-finished one (F06).

## Weighted selection algorithm

```kotlin
fun interface WeightedCardSelector {
    fun select(cards: List<Card>, count: Int, random: Random = Random.Default): List<Card>
}
```
Per card: `weight = max(1, 1 + card.timesIncorrect - min(card.timesCorrect, card.timesIncorrect))`.
Sampling: weighted shuffle — assign each card a key `-ln(u) / weight` for
`u = random.nextDouble()` (Efraimidis-Spirakis), sort ascending, take first
`count`. Pure, seedable via injected `Random` → deterministic in tests.
`count` is clamped to `cards.size` by the caller (`SessionConfigViewModel`),
not inside the selector (selector assumes valid input, per PRD §9 boundary
validation).

## PracticeRepository

```kotlin
interface PracticeRepository {
    suspend fun startSession(setId: Uuid, cardCount: Int): PracticeSessionDraft // in-memory, not yet persisted
    suspend fun completeSession(draft: PracticeSessionDraft, answers: List<CardAnswer>): PracticeSessionSummary
}

data class CardAnswer(val cardId: Uuid, val wasCorrect: Boolean)
data class PracticeSessionSummary(val sessionId: Uuid, val correct: List<Card>, val incorrect: List<Card>)
```
`startSession` loads the set's cards, runs `WeightedCardSelector`, returns
an in-memory draft (id pre-generated via `IdGenerator`, `startedAt` via
`TimeProvider`) — nothing written to DB until the session actually finishes,
so an abandoned session (user backs out mid-play) leaves no trace.
`completeSession` runs in one `@Transaction`: insert `PracticeSessionEntity`,
`insertResults`, then for each answer call `CardDao.updateStats` with
incremented `timesCorrect`/`timesIncorrect` and new `lastPracticedAt`.

## ViewModel contracts

```kotlin
data class SessionConfigUiState(val setSize: Int = 0, val selectedCount: Int = 0)

data class SessionPlayUiState(
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentCard: Card? = null,
    val isFlipped: Boolean = false,
    val isSessionComplete: Boolean = false,
)

data class SessionResultsUiState(val correct: List<Card> = emptyList(), val incorrect: List<Card> = emptyList())
```
`SessionPlayViewModel` holds the `PracticeSessionDraft`'s card list +
mutable in-memory answer buffer; `onFlip()`, `onAnswer(correct: Boolean)`
(records answer, advances index or sets `isSessionComplete`), on completion
calls `practiceRepository.completeSession(...)` and emits a one-shot
navigation event to `SessionResults` carrying the summary (via nav
`SavedStateHandle` or a shared session-scoped ViewModel — avoid a global
singleton just to pass this list).

## Edge cases
- `setSize == 0` (no cards): "Start Practice" button disabled on Set Detail
  with a hint, `SessionConfig` unreachable — don't build a "0 cards" empty
  state for a screen that shouldn't be reachable.
- User backs out of `SessionPlay` before finishing: nothing persisted (see
  `startSession` note above) — no partial session in history.
- All cards weight-tied (fresh set, no history): falls back to uniform
  random sampling, which the Efraimidis-Spirakis method does naturally
  when all weights are equal.

## Acceptance criteria
- [ ] `WeightedCardSelector` unit-tested: uniform weights ≈ uniform sampling
      distribution (statistical tolerance test over many seeded draws);
      cards with higher `timesIncorrect` are selected more often on average.
- [ ] Config → Play → Results flow works end to end for a real set.
- [ ] Finishing a session persists exactly one `PracticeSession` +
      N `PracticeSessionResult` rows and updates each touched card's stats.
- [ ] Backing out mid-session persists nothing.
- [ ] `SessionConfigScreen`, `SessionPlayScreen`, and `SessionResultsScreen`
      have `@Preview`s per the project's Compose preview convention.
