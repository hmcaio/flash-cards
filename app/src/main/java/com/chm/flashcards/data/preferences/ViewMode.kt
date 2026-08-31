package com.chm.flashcards.data.preferences

/**
 * List-vs-grid display mode for a card/set list. Persisted globally (one
 * value shared by every screen that offers the toggle -- currently Set List
 * and Set Detail) via [ViewModePreferences], not per-screen.
 */
enum class ViewMode {
    LIST,
    GRID,
}
