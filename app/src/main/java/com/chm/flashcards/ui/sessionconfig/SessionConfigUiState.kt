package com.chm.flashcards.ui.sessionconfig

import com.chm.flashcards.data.repository.Tag
import kotlin.uuid.Uuid

/**
 * UI state for [SessionConfigScreen]. `setSize` is the total number of cards
 * in the set (drives the slider's range); `selectedCount` is the current
 * pick, clamped to `1..setSize`, defaulting to `min(10, setSize)` per
 * spec.md.
 *
 * C002 additions: [availableTags] lists every distinct tag used by cards in
 * this set (drives the multi-select tag filter chip row, OR semantics);
 * [selectedTagIds] is the current filter selection -- empty means "no
 * filter" (all cards eligible, the pre-existing default behavior). Once any
 * tag is selected, `setSize`/`selectedCount` above reflect the FILTERED card
 * pool rather than the set's total size (see [SessionConfigViewModel]).
 */
data class SessionConfigUiState(
    val setSize: Int = 0,
    val selectedCount: Int = 0,
    val availableTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Uuid> = emptySet(),
)
