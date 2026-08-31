package com.chm.flashcards.ui.setdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.preferences.ViewMode
import com.chm.flashcards.data.preferences.ViewModePreferences
import com.chm.flashcards.data.repository.CardRepository
import com.chm.flashcards.data.repository.CardSetRepository
import com.chm.flashcards.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `setId` nav arg parsed once at this ViewModel entry point (per [Screen]'s
 * nav-arg convention) -- `Uuid` is used everywhere below this point.
 * `cardSetRepository` is only used to read the set's name for the screen
 * title (via [CardSetRepository.getAllSets], the only read already exposed
 * by F02 -- no new method added to that interface).
 */
@HiltViewModel
class SetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val cardSetRepository: CardSetRepository,
    private val viewModePreferences: ViewModePreferences,
) : ViewModel() {

    private val setId: Uuid = Uuid.parse(checkNotNull(savedStateHandle[Screen.ARG_SET_ID]))

    private val _uiState = MutableStateFlow(SetDetailUiState())
    val uiState: StateFlow<SetDetailUiState> = _uiState.asStateFlow()

    /** `null` cardId means "create" (FAB tap); non-null means "edit" (row tap). */
    private val _navigateToCardEditor = MutableSharedFlow<Uuid?>()
    val navigateToCardEditor: SharedFlow<Uuid?> = _navigateToCardEditor.asSharedFlow()

    private val _navigateToSessionConfig = MutableSharedFlow<Unit>()
    val navigateToSessionConfig: SharedFlow<Unit> = _navigateToSessionConfig.asSharedFlow()

    private val _navigateToHistoryList = MutableSharedFlow<Unit>()
    val navigateToHistoryList: SharedFlow<Unit> = _navigateToHistoryList.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTagFilter = MutableStateFlow<Uuid?>(null)

    init {
        viewModelScope.launch {
            cardSetRepository.getAllSets().collect { sets ->
                _uiState.update { it.copy(setName = sets.find { set -> set.id == setId }?.name.orEmpty()) }
            }
        }
        // Independent subscription (not derived from the filtered `cards` below) so a tag
        // chip stays visible/selectable even while a search/filter is narrowing the visible
        // list -- spec.md: "a chip never shows zero results".
        viewModelScope.launch {
            cardRepository.getCardsBySetId(setId).collect { cards ->
                _uiState.update { state ->
                    state.copy(
                        availableTagFilters = cards.flatMap { it.tags }.distinctBy { it.id },
                        hasCards = cards.isNotEmpty(),
                    )
                }
            }
        }
        // Drives the visible card list from the current search query + tag filter. Blank
        // query + no tag filter is treated as "no search" and delegates straight to
        // getCardsBySetId (per spec.md) rather than routing an empty string through
        // searchCards -- functionally equivalent (the DAO query already no-ops on an empty
        // string) but keeps the plain, filter-free read path explicit.
        viewModelScope.launch {
            combine(_searchQuery, _selectedTagFilter) { query, tagId -> query.trim() to tagId }
                .flatMapLatest { (trimmedQuery, tagId) ->
                    if (trimmedQuery.isEmpty() && tagId == null) {
                        cardRepository.getCardsBySetId(setId)
                    } else {
                        cardRepository.searchCards(setId, trimmedQuery, tagId)
                    }
                }
                .collect { cards ->
                    _uiState.update { it.copy(cards = cards, isLoading = false) }
                }
        }
        viewModelScope.launch {
            viewModePreferences.viewMode.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }
    }

    fun onViewModeToggle() {
        val newMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewModelScope.launch { viewModePreferences.setViewMode(newMode) }
    }

    fun onSearchQueryChange(text: String) {
        _searchQuery.value = text
        _uiState.update { it.copy(searchQuery = text) }
    }

    fun onTagFilterSelect(tagId: Uuid?) {
        _selectedTagFilter.value = tagId
        _uiState.update { it.copy(selectedTagFilter = tagId) }
    }

    fun onAddCardClick() {
        viewModelScope.launch { _navigateToCardEditor.emit(null) }
    }

    fun onCardClick(cardId: Uuid) {
        viewModelScope.launch { _navigateToCardEditor.emit(cardId) }
    }

    fun onStartPracticeClick() {
        viewModelScope.launch { _navigateToSessionConfig.emit(Unit) }
    }

    fun onHistoryClick() {
        viewModelScope.launch { _navigateToHistoryList.emit(Unit) }
    }

    fun onDeleteRequest(cardId: Uuid) {
        val target = _uiState.value.cards.find { it.card.id == cardId } ?: return
        _uiState.update { it.copy(pendingDelete = target) }
    }

    fun onDeleteConfirm(cardId: Uuid) {
        viewModelScope.launch {
            cardRepository.deleteCard(cardId)
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    fun onDeleteCancel() {
        _uiState.update { it.copy(pendingDelete = null) }
    }
}
