package com.chm.flashcards.ui.setdetail

import com.chm.flashcards.data.repository.CardWithTags

/**
 * UI state for [SetDetailScreen]. `pendingDelete` isn't in spec.md's
 * illustrative state snippet but is needed to drive the delete-confirmation
 * dialog's copy (the card's front text) without a redundant lookup -- same
 * precedent as [com.chm.flashcards.ui.setlist.SetListUiState.pendingDelete].
 */
data class SetDetailUiState(
    val setName: String = "",
    val cards: List<CardWithTags> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDelete: CardWithTags? = null,
)
