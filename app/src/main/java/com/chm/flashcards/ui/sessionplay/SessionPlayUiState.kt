package com.chm.flashcards.ui.sessionplay

import com.chm.flashcards.data.repository.Card

/**
 * UI state for [SessionPlayScreen]: one card shown at a time, front-first,
 * tap to flip -> back + notes, then Correct/Incorrect. `isSessionComplete`
 * flips true right before the one-shot navigation event to Session Results
 * fires -- the screen doesn't render a distinct "complete" state of its own.
 */
data class SessionPlayUiState(
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentCard: Card? = null,
    val isFlipped: Boolean = false,
    val isSessionComplete: Boolean = false,
)
