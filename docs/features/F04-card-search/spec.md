# F04 — Card Search

Status: done
Depends on: F01, F03
PRD refs: §3 req 6, §7 Search

## Scope

**In scope**
- `CardDao` search query: text match on front/back/notes + optional tag
  filter, bound params (no string concatenation — PRD §9).
- `CardRepository.searchCards(setId, query, tagFilter): Flow<List<CardWithTags>>`.
- Extend `SetDetailViewModel`/`SetDetailScreen` (from F03) with a search
  text field and tag filter chips row above the card list; empty query +
  no filter = full list (delegates to F03's existing `getCardsBySetId`).

**Out of scope**
- Cross-set search (search is always scoped to the currently open set, per
  PRD req 6 wording "search for cards in a set").
- Fuzzy/typo-tolerant matching — plain case-insensitive substring `LIKE`.

## Data

```kotlin
@Query("""
  SELECT DISTINCT c.* FROM cards c
  LEFT JOIN card_tag_cross_ref x ON x.cardId = c.id
  WHERE c.setId = :setId
    AND (:query = '' OR c.front LIKE '%' || :query || '%' COLLATE NOCASE
                     OR c.back LIKE '%' || :query || '%' COLLATE NOCASE
                     OR c.notes LIKE '%' || :query || '%' COLLATE NOCASE)
    AND (:tagId IS NULL OR x.tagId = :tagId)
""")
fun searchCards(setId: Uuid, query: String, tagId: Uuid?): Flow<List<CardEntity>>
```
(Then joined with tags in the repository the same way F03's `getCardsBySetId`
is, to return `CardWithTags` — reuse that mapping helper rather than
duplicating it.)

Single tag filter for v1 (one chip selectable at a time) — matches PRD's
"tags can be added to cards" without requiring multi-tag AND/OR logic,
which the PRD doesn't ask for.

## ViewModel state additions (`SetDetailUiState`)

```kotlin
val searchQuery: String = "",
val selectedTagFilter: Uuid? = null, // tag id
val availableTagFilters: List<Tag> = emptyList(), // tags present in this set's cards
```
`onSearchQueryChange(text)` and `onTagFilterSelect(tagId?)` re-trigger the
`searchCards` flow (`combine(searchQuery, selectedTagFilter) { ... }.flatMapLatest`).
No debounce — local SQLite query on a personal dataset is fast enough that
debouncing would be premature optimization.

## Edge cases
- Query with only whitespace treated as empty (full list, not "no results").
- Selecting a tag filter with an already-active search text combines both
  (AND, per PRD §7).
- `availableTagFilters` only lists tags actually used by cards in *this*
  set (not the global tag list from F03's `TagRepository`), so a chip
  never shows zero results.

## Acceptance criteria
- [ ] Typing in the search box filters the visible card list by front/back/notes.
- [ ] Selecting a tag chip filters to cards with that tag; combined with
      text search narrows further.
- [ ] Clearing search text and deselecting the tag chip restores the full list.
- [ ] `SetDetailScreen`'s `@Preview`s (from F03) cover the search/tag-filter states.

## Implementation notes (post-hoc, added after F01–F07 shipped)
`CardDao.searchCards` returns `Flow<List<CardWithTagsEntity>>` (via
`@Transaction`, reusing the same `@Relation` projection F03's
`getCardsWithTagsBySetId` uses), not `Flow<List<CardEntity>>` as shown
above — so results already carry each card's full tag list rather than
needing a second join step in the repository.
