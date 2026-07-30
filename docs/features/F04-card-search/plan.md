# F04 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. DAO query
Files: `androidTest/.../data/dao/CardDaoTest.kt` (extend).

1. RED — `searchCards_emptyQueryNoTagFilter_returnsAllCardsInSet`.
2. GREEN — Add `CardDao.searchCards(setId, query, tagId): Flow<List<CardEntity>>`
   per spec.md SQL.
3. RED — `searchCards_queryMatchesFrontCaseInsensitive_returnsMatchingCard`.
4. GREEN — Fix `COLLATE NOCASE` / `LIKE` if red.
5. RED — `searchCards_queryMatchesBackOrNotes_returnsMatchingCard`.
6. GREEN — (should already pass if query built correctly — fix if not).
7. RED — `searchCards_tagIdFilter_returnsOnlyCardsWithThatTag`.
8. GREEN — Verify join condition.
9. RED — `searchCards_queryAndTagIdCombined_appliesBoth`.
10. GREEN — Fix combined predicate if red.

## 2. CardRepository.searchCards (unit, fake DAO)
Files: `test/.../data/repository/CardRepositoryTest.kt` (extend).

11. RED — `searchCards_delegatesToDaoAndMapsToCardWithTags`.
12. GREEN — Implement, reusing the tag-join mapping helper extracted from
    `getCardsBySetId` (extract-method refactor if not already shared).

## 3. SetDetailViewModel search state (unit, fake repo)
Files: `test/.../ui/setdetail/SetDetailViewModelTest.kt` (extend).

13. RED — `onSearchQueryChange_updatesCardsFromSearchResults`.
14. GREEN — `combine`/`flatMapLatest` over `searchQuery` + `selectedTagFilter`
    driving `searchCards` instead of the plain `getCardsBySetId` collector.
15. RED — `onTagFilterSelect_filtersToTag`.
16. GREEN — Wire.
17. RED — `blankSearchQuery_treatedAsEmpty_returnsFullList`.
18. GREEN — Trim before passing to repository.
19. RED — `availableTagFilters_onlyIncludesTagsPresentInThisSet`.
20. GREEN — Derive from loaded cards' tags (distinct), not global `TagRepository`.

## 4. Screen (Compose)
21. RED — `androidTest/.../ui/setdetail/SetDetailScreenTest.kt` —
    `typingInSearchBox_filtersVisibleCards`.
22. GREEN — Add search `TextField` + tag filter chip row to `SetDetailScreen`
    above the card list.

## 5. Refactor
23. REFACTOR — Confirm no duplicate front/back/notes-matching logic exists outside
    the DAO query; all F01–F04 tests green.
24. REFACTOR — Update `SetDetailScreen`'s existing `@Preview`s (from F03) to
    cover the added search bar and tag-filter-chip row (query typed,
    tag selected, no-results state), per the project's Compose preview
    convention (`docs/features/README.md`).

## Definition of done
`./gradlew test connectedAndroidTest` green; manually search a set with
10+ cards and a tag filter.
