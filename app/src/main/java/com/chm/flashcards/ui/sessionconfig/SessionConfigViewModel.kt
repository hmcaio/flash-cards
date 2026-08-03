package com.chm.flashcards.ui.sessionconfig

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.repository.CardRepository
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
 * nav-arg convention). Reads the set's current card count once (not a live
 * `collect`) to seed `setSize`/the default `selectedCount` -- a card being
 * added/removed elsewhere while this screen is open isn't a case spec.md
 * calls out, and re-clamping a user's in-progress slider drag out from under
 * them would be worse UX than a possibly-stale count.
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

    init {
        viewModelScope.launch {
            val setSize = cardRepository.getCardsBySetId(setId).first().size
            _uiState.update { it.copy(setSize = setSize, selectedCount = minOf(10, setSize)) }
        }
    }

    fun onCountChange(count: Int) {
        val setSize = _uiState.value.setSize
        val clamped = count.coerceIn(1, maxOf(1, setSize))
        _uiState.update { it.copy(selectedCount = clamped) }
    }

    fun onStartClick() {
        viewModelScope.launch {
            val draft = practiceRepository.startSession(setId, _uiState.value.selectedCount)
            sessionHolder.setDraft(draft)
            _navigateToSessionPlay.emit(Unit)
        }
    }
}
