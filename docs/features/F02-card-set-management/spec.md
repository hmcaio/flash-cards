# F02 — Card Set Management

Status: done
Depends on: F01
PRD refs: §3 req 1, §5 screen "Set List"

## Scope

**In scope**
- `CardSetRepository`: wraps `CardSetDao`, owns ID/timestamp generation via
  `IdGenerator`/`TimeProvider`, exposes suspend functions + `Flow` reads.
- `SetListViewModel` + `SetListUiState`.
- `SetListScreen` composable: list of sets (name + card count — card count
  can hardcode 0 until F03 adds cards, or join query now; see Data note),
  create-set dialog (name input, blank-name validation), rename, delete with
  confirmation dialog, empty state ("No sets yet"), tap a set → navigate to
  `Screen.SetDetail(setId)` (destination itself still a placeholder until F03).

**Out of scope**
- Card/tag CRUD (F03). Search (F04). Practice/session/history (F05/F06).

## Data

`CardSetRepository`:
```kotlin
interface CardSetRepository {
    fun getAllSets(): Flow<List<CardSet>>
    suspend fun createSet(name: String): CardSet
    suspend fun renameSet(id: Uuid, name: String)
    suspend fun deleteSet(id: Uuid)
}
```
Domain model `CardSet(id, name, createdAt)` — thin mapping over
`CardSetEntity`, kept 1:1 for now (no need for a separate UI model yet).

Card count on the list row: add `CardSetDao.getAllWithCardCount(): Flow<List<CardSetWithCount>>`
(`@Query` with `LEFT JOIN` + `COUNT`) rather than N+1 querying per row —
add this DAO method now since F02 needs it, per F01's "each feature may add
queries to F01's DAOs" note.

`createSet`/`renameSet` validation: trimmed name must be non-blank, max 100
chars — enforced in the ViewModel (UI-facing validation message) not the
repository (repository trusts already-validated input, per PRD §9 "validate
at the boundary").

## ViewModel contract

```kotlin
data class SetListUiState(
    val sets: List<CardSetWithCount> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val createNameError: String? = null,
)
```
Actions: `onCreateClick()`, `onCreateNameChange`, `onCreateConfirm()`,
`onRename(id, newName)`, `onDeleteRequest(id)` → confirm dialog →
`onDeleteConfirm(id)`, `onSetClick(id)` (emits a one-shot navigation event).

## Edge cases
- Deleting a set with cards: cascades per F01 FK — confirmation dialog
  copy must say "This set and its N cards will be deleted" (N from the
  count already loaded in state, no extra query).
- Duplicate set names: allowed (no uniqueness requirement in PRD) — do not
  add a uniqueness constraint.

## Acceptance criteria
- [ ] Create, rename, delete a set from the UI, list updates reactively (Flow).
- [ ] Blank/whitespace-only name rejected with inline error, dialog stays open.
- [ ] Deleting a set removes its cards (verified via F01 cascade, exercised
      here through the repository).
- [ ] Empty state shown when no sets exist.
- [ ] `SetListScreen` and its extracted components (row, dialogs) have
      `@Preview`s per the project's Compose preview convention.
