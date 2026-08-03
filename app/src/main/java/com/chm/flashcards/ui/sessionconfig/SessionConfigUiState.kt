package com.chm.flashcards.ui.sessionconfig

/**
 * UI state for [SessionConfigScreen]. `setSize` is the total number of cards
 * in the set (drives the slider's range); `selectedCount` is the current
 * pick, clamped to `1..setSize`, defaulting to `min(10, setSize)` per
 * spec.md.
 */
data class SessionConfigUiState(
    val setSize: Int = 0,
    val selectedCount: Int = 0,
)
