package com.chm.flashcards.ui.history

import com.chm.flashcards.data.repository.Card

/** UI state for [HistoryDetailScreen] -- same shape as F05's `SessionResultsUiState`, per spec.md. */
data class HistoryDetailUiState(
    val correct: List<Card> = emptyList(),
    val incorrect: List<Card> = emptyList(),
    val isLoading: Boolean = true,
)
