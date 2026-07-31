package com.chm.flashcards.ui.cardeditor

/**
 * UI state for [CardEditorScreen]. `tagInput` isn't in spec.md's illustrative
 * state snippet but is needed to drive the tag input text field itself,
 * separate from the already-committed [tags] chip list.
 */
data class CardEditorUiState(
    val front: String = "",
    val back: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val tagSuggestions: List<String> = emptyList(),
    val frontError: String? = null,
    val backError: String? = null,
    val isSaveEnabled: Boolean = false,
)
