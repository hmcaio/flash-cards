package com.chm.flashcards.ui.sessionconfig

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.repository.CardRepository
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.PracticeRepository
import com.chm.flashcards.ui.navigation.Screen
import com.chm.flashcards.ui.session.PracticeSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `setId` nav arg parsed once at this ViewModel entry point (per [Screen]'s
 * nav-arg convention). Reads the set's full card+tag list once (not a live
 * `collect`) to seed `setSize`/the default `selectedCount` -- a card being
 * added/removed elsewhere while this screen is open isn't a case spec.md
 * calls out, and re-clamping a user's in-progress slider drag out from under
 * them would be worse UX than a possibly-stale count.
 *
 * C002: the fetched list is kept around in [allCards] so [onTagToggle] can
 * recompute the filtered pool in-memory (no extra repository reads) --
 * `setSize`/`selectedCount` in [uiState] always reflect the CURRENT tag
 * filter, empty selection meaning "no filter" (all of [allCards]).
 */
@HiltViewModel
class SessionConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val practiceRepository: PracticeRepository,
    private val sessionHolder: PracticeSessionHolder,
) : ViewModel() {

    private val setId: Uuid = Uuid.parse(checkNotNull(savedStateHandle[Screen.ARG_SET_ID]))

    private val _uiState = MutableStateFlow(SessionConfigUiState())
    val uiState: StateFlow<SessionConfigUiState> = _uiState.asStateFlow()

    private val _navigateToSessionPlay = MutableSharedFlow<Unit>()
    val navigateToSessionPlay: SharedFlow<Unit> = _navigateToSessionPlay.asSharedFlow()

    private var allCards: List<CardWithTags> = emptyList()

    init {
        viewModelScope.launch {
            allCards = cardRepository.getCardsBySetId(setId).first()
            val setSize = allCards.size
            _uiState.update {
                it.copy(
                    setSize = setSize,
                    selectedCount = minOf(10, setSize),
                    availableTags = allCards.flatMap { cardWithTags -> cardWithTags.tags }.distinctBy { tag -> tag.id },
                )
            }
        }
    }

    fun onCountChange(count: Int) {
        val setSize = _uiState.value.setSize
        val clamped = count.coerceIn(1, maxOf(1, setSize))
        _uiState.update { it.copy(selectedCount = clamped) }
    }

    /** Flips [tagId]'s membership in the OR-filter selection, then re-derives `setSize` and re-clamps (not resets) `selectedCount` against the newly filtered pool. */
    fun onTagToggle(tagId: Uuid) {
        val newSelection = _uiState.value.selectedTagIds.let { current ->
            if (tagId in current) current - tagId else current + tagId
        }
        val filteredSize = filteredCards(newSelection).size
        _uiState.update {
            it.copy(
                selectedTagIds = newSelection,
                setSize = filteredSize,
                selectedCount = it.selectedCount.coerceIn(1, maxOf(1, filteredSize)),
            )
        }
    }

    private fun filteredCards(tagIds: Set<Uuid>): List<CardWithTags> =
        allCards.filter { cardWithTags -> tagIds.isEmpty() || cardWithTags.tags.any { it.id in tagIds } }

    fun onStartClick() {
        viewModelScope.launch {
            val state = _uiState.value
            val draft = practiceRepository.startSession(setId, state.selectedCount, state.selectedTagIds)
            sessionHolder.setDraft(draft)
            _navigateToSessionPlay.emit(Unit)
        }
    }
}
