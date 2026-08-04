# F03 — Card & Tag Management

Status: done
Depends on: F01, F02
PRD refs: §3 req 2 & 3, §5 screens "Set Detail", "Card Editor"

## Scope

**In scope**
- `CardRepository`: CRUD for cards under a set (front/back/notes),
  ID/timestamp via F01 utils.
- `TagRepository`: get-or-create by name (case-insensitive reuse — typing
  "Kotlin" twice must not create two tags), list all tags (for autocomplete),
  attach/detach tags on a card.
- `SetDetailViewModel` + `SetDetailScreen`: replaces F01's placeholder,
  shows the set's cards (front + tag chips), tap → `CardEditor`, swipe/menu
  delete with confirm, FAB → `CardEditor` in create mode, "Start Practice"
  button (navigates to `Screen.SessionConfig(setId)`, still a placeholder
  until F05).
- `CardEditorViewModel` + `CardEditorScreen`: front/back/notes text fields,
  tag input with autocomplete-from-existing + create-on-type (comma or
  enter commits a chip), save/cancel, validation.

**Out of scope**
- Search/tag-filter UI on Set Detail (F04 adds the search bar + filter
  chips on top of the card list built here).
- Practice session itself (F05).

## Data

```kotlin
interface CardRepository {
    fun getCardsBySetId(setId: Uuid): Flow<List<CardWithTags>>
    suspend fun getCard(id: Uuid): CardWithTags?
    suspend fun createCard(setId: Uuid, front: String, back: String, notes: String?, tagNames: List<String>): Card
    suspend fun updateCard(id: Uuid, front: String, back: String, notes: String?, tagNames: List<String>)
    suspend fun deleteCard(id: Uuid)
}

interface TagRepository {
    fun getAllTagNames(): Flow<List<String>>
}
```

`CardWithTags(card: Card, tags: List<Tag>)` — Room `@Relation` or manual
join query on `CardDao`/`CardTagCrossRefDao`.

Tag resolution on save: for each `tagName` in the editor's chip list,
`TagDao.getByName(trim, lowercase-compare)` → reuse `id` if found, else
`insert` new `TagEntity` (id via `IdGenerator`). Then replace the card's
cross-refs: `CardTagCrossRefDao.deleteByCardId(cardId)` +
insert new rows — simplest correct approach for "avoid overcomplicating"
(no diffing added/removed tags individually).

Validation (in `CardEditorViewModel`, boundary per PRD §9):
- `front` and `back`: required, trimmed non-blank, max 1000 chars.
- `notes`: optional, max 2000 chars.
- tag name: trimmed non-blank, max 50 chars, max 10 tags per card.

## ViewModel contracts

```kotlin
data class SetDetailUiState(
    val setName: String = "",
    val cards: List<CardWithTags> = emptyList(),
    val isLoading: Boolean = true,
)

data class CardEditorUiState(
    val front: String = "",
    val back: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val frontError: String? = null,
    val backError: String? = null,
    val isSaveEnabled: Boolean = false,
)
```
`CardEditorViewModel` takes an optional `cardId` nav arg — `null` = create
mode (loads nothing), non-null = edit mode (loads existing card + tags).
Nav Compose route args are always `String`; the screen/ViewModel parses
`Uuid.parse(arg)` immediately at the navigation boundary so `Uuid` is used
everywhere below that point, same as F05/F06's session/history nav args.

## Edge cases
- Deleting a card removes its cross-refs (F01 cascade) and any
  `PracticeSessionResult` rows referencing it (documented as accepted
  limitation in F06 spec).
- Tag becomes orphaned (no cards reference it) after last card using it is
  deleted/retagged: left in the `tags` table (harmless, still shows in
  autocomplete for reuse) — no cleanup job, keeps this feature simple.
- Saving with an empty tag list is valid (tags are optional).

## Acceptance criteria
- [ ] Create/edit/delete a card from Set Detail, list updates reactively.
- [ ] Front/back required — save disabled and inline error shown when blank.
- [ ] Typing an existing tag name reuses it (verified: tag count in DB
      doesn't grow on reuse); typing a new name creates it.
- [ ] Tag chips render on the Set Detail card list.
- [ ] `CardEditorScreen` and `SetDetailScreen` have `@Preview`s per the
      project's Compose preview convention.

## Implementation notes (post-hoc, added after F01–F07 shipped)
- `CardWithTags` shipped as two types, not one: `data.dao.CardWithTagsEntity`
  (Room `@Relation`/`@Junction` projection used by `CardDao`'s query) and
  `data.repository.CardWithTags` (domain model holding `Card` + `List<Tag>`).
  `CardRepositoryImpl` maps between them at the repository boundary — same
  split precedent as `CardSetEntity`/`CardSet` in F02, just not shown in
  this doc's original snippet.
- `SetDetailViewModel` takes `CardSetRepository` as an extra constructor
  dependency (not in the snippet above), purely to read the set's name for
  the screen title via `getAllSets()` — no new method was added to F02's
  repository for this.
- `common/CardValidationRules.kt` (front/back/notes length, tag name length,
  max-tags-per-card) was extracted out of `CardEditorViewModel` *later*,
  during F07, so import validation could share the same limits instead of
  duplicating magic numbers. See F07's spec.md for that side of the story.
