package com.chm.flashcards.ui.setdetail

import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.Tag
import kotlin.uuid.Uuid

/**
 * UI state for [SetDetailScreen]. `pendingDelete` isn't in spec.md's
 * illustrative state snippet but is needed to drive the delete-confirmation
 * dialog's copy (the card's front text) without a redundant lookup -- same
 * precedent as [com.chm.flashcards.ui.setlist.SetListUiState.pendingDelete].
 *
 * F04 additions: [searchQuery]/[selectedTagFilter] are the search bar's/tag
 * chip row's current input; [cards] already reflects them (filtered), per
 * [SetDetailViewModel]. [availableTagFilters] only lists tags actually used
 * by cards in *this* set (not the global tag list from F03's
 * `TagRepository`), so a chip never shows zero results (spec.md "Edge
 * cases").
 *
 * F05 addition: [hasCards] reflects the set's *unfiltered* card count (not
 * [cards].isNotEmpty(), which reflects the current search/tag filter) --
 * drives disabling the "Start Practice" button when the set has zero cards
 * (F05 spec.md edge case: Session Config shouldn't be reachable with no
 * cards to select from). Defaults `false` so the button doesn't flash
 * enabled before the first load completes.
 */
data class SetDetailUiState(
    val setName: String = "",
    val cards: List<CardWithTags> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDelete: CardWithTags? = null,
    val searchQuery: String = "",
    val selectedTagFilter: Uuid? = null,
    val availableTagFilters: List<Tag> = emptyList(),
    val hasCards: Boolean = false,
)
