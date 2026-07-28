# F03 — Action plan (TDD)

Legend: RED = failing test first, GREEN = minimum code to pass, REFACTOR = clean up with tests green

## 1. DAO additions
Files: `androidTest/.../data/dao/CardDaoTest.kt`, `CardTagCrossRefDaoTest.kt`.

1. RED — `CardDaoTest.getBySetIdWithTags_returnsCardsJoinedWithTheirTags`
   (insert card + 2 tags + cross-refs, assert `CardWithTags` shape).
2. GREEN — `CardWithTags` (`@Relation`) + `CardDao.getCardsWithTagsBySetId(setId): Flow<List<CardWithTags>>`.
3. RED — `TagDaoTest.getByName_caseInsensitiveMatch_returnsExistingTag`.
4. GREEN — `TagDao.getByName` using `COLLATE NOCASE` in the `@Query`.

## 2. CardRepository (unit, fake DAOs)
Files: `test/.../data/repository/CardRepositoryTest.kt`, prod
`data/repository/CardRepository.kt` + impl.

5. RED — `createCard_newTags_createsTagsAndCrossRefs_returnsCard`.
6. GREEN — `CardRepositoryImpl.createCard` (resolve-or-create tags, insert card,
   insert cross-refs) — minimum to pass.
7. RED — `createCard_existingTagName_reusesExistingTagId_doesNotInsertDuplicateTag`.
8. GREEN — Fix tag resolution to check `getByName` first.
9. RED — `updateCard_replacesTagCrossRefs`.
10. GREEN — `updateCard`: `deleteByCardId` then re-insert.
11. RED — `deleteCard_callsDaoDelete`.
12. GREEN — `deleteCard`.
13. RED — `getCardsBySetId_mapsToDomainCardWithTags`.
14. GREEN — `getCardsBySetId` mapping.
15. GREEN — Bind in `RepositoryModule`.

## 3. TagRepository (unit, fake DAO)
16. RED — `getAllTagNames_returnsSortedDistinctNames`.
17. GREEN — `TagRepositoryImpl.getAllTagNames`.

## 4. CardEditorViewModel (unit, fake repos)
Files: `test/.../ui/cardeditor/CardEditorViewModelTest.kt`.

18. RED — `blankFront_saveDisabled_showsError`.
19. GREEN — Validation producing `frontError`/`isSaveEnabled`.
20. RED — `blankBack_saveDisabled_showsError`.
21. GREEN — Same for back.
22. RED — `frontOver1000Chars_showsError`.
23. GREEN — Length validation.
24. RED — `addTagChip_appendsToTagsList_dedupesCaseInsensitive`.
25. GREEN — Chip-add logic.
26. RED — `moreThan10Tags_rejectsAdditionalChip`.
27. GREEN — Cap enforcement.
28. RED — `save_validInput_callsRepositoryCreateCard_createMode`.
29. GREEN — Wire create path.
30. RED — `loadForEdit_existingCardId_populatesFieldsFromRepository`.
31. GREEN — Edit-mode load.
32. RED — `save_editMode_callsRepositoryUpdateCard`.
33. GREEN — Wire update path.

## 5. SetDetailViewModel (unit, fake repo)
Files: `test/.../ui/setdetail/SetDetailViewModelTest.kt`.

34. RED — `initialState_loadsCardsForSetId`.
35. GREEN — Collect `repository.getCardsBySetId(setId)`.
36. RED — `onDeleteConfirm_callsRepositoryDeleteCard`.
37. GREEN — Wire.

## 6. Screens (Compose) + nav wiring
38. RED — `androidTest/.../ui/cardeditor/CardEditorScreenTest.kt` —
    `createCard_appearsOnSetDetail` (Hilt test DB, navigate SetDetail →
    CardEditor, fill front/back, save, assert back on SetDetail with new card visible).
39. GREEN — Build `CardEditorScreen` (fields, tag chip input w/ suggestions
    dropdown, save/cancel top bar actions) and `SetDetailScreen` (card list
    with tag chips, FAB, delete-with-confirm, "Start Practice" button
    navigating to still-placeholder `SessionConfig`).
40. GREEN — Replace `Screen.SetDetail` and `Screen.CardEditor` placeholders in
    `MainActivity` NavHost with real screens, passing `setId`/`cardId` nav args
    (parsed to `Uuid` at the boundary, per F01 spec's nav-arg convention).

## 7. Refactor
41. REFACTOR — Extract tag-chip-input composable if reused elsewhere later;
    confirm F01–F03 tests all green.

## Definition of done
`./gradlew test connectedAndroidTest` green; manually add a card with tags,
edit it, delete it.
