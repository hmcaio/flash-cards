package com.chm.flashcards.ui.sessionresults

import com.chm.flashcards.data.repository.Card

/** UI state for [SessionResultsScreen]: score summary derives from `correct.size`/`incorrect.size`. */
data class SessionResultsUiState(
    val correct: List<Card> = emptyList(),
    val incorrect: List<Card> = emptyList(),
)
