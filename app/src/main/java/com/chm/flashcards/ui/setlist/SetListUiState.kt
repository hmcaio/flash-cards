package com.chm.flashcards.ui.setlist

import com.chm.flashcards.data.dao.CardSetWithCount

/**
 * UI state for [SetListScreen]. `createNameInput`/`pendingDelete` aren't in
 * spec.md's illustrative state snippet but are needed to actually drive the
 * create dialog's text field and the delete-confirmation dialog's copy
 * ("This set and its N cards will be deleted") without a redundant lookup.
 */
data class SetListUiState(
    val sets: List<CardSetWithCount> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val createNameInput: String = "",
    val createNameError: String? = null,
    val pendingDelete: CardSetWithCount? = null,
)
