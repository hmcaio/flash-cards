package com.chm.flashcards.ui.setlist

import com.chm.flashcards.data.dao.CardSetWithCount
import com.chm.flashcards.data.preferences.ViewMode

/**
 * UI state for [SetListScreen]. `createNameInput`/`pendingDelete` aren't in
 * spec.md's illustrative state snippet but are needed to actually drive the
 * create dialog's text field and the delete-confirmation dialog's copy
 * ("This set and its N cards will be deleted") without a redundant lookup.
 *
 * C004 addition: [viewMode] reflects the global, DataStore-backed
 * list-vs-grid preference (shared with [com.chm.flashcards.ui.setdetail.SetDetailUiState.viewMode],
 * not a per-screen setting) -- defaults to [ViewMode.GRID] before the first
 * persisted value loads, matching this chore's default.
 */
data class SetListUiState(
    val sets: List<CardSetWithCount> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val createNameInput: String = "",
    val createNameError: String? = null,
    val pendingDelete: CardSetWithCount? = null,
    val viewMode: ViewMode = ViewMode.GRID,
)
