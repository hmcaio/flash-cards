package com.chm.flashcards.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.repository.PracticeRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `setId` nav arg parsed once at this ViewModel entry point (per [Screen]'s
 * nav-arg convention). Live-collects [PracticeRepository.getSessionsForSet]
 * so a session completed elsewhere (Session Results -> back -> History List)
 * shows up without a manual refresh.
 */
@HiltViewModel
class HistoryListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val practiceRepository: PracticeRepository,
) : ViewModel() {

    private val setId: Uuid = Uuid.parse(checkNotNull(savedStateHandle[Screen.ARG_SET_ID]))

    private val _uiState = MutableStateFlow(HistoryListUiState())
    val uiState: StateFlow<HistoryListUiState> = _uiState.asStateFlow()

    private val _navigateToHistoryDetail = MutableSharedFlow<Uuid>()
    val navigateToHistoryDetail: SharedFlow<Uuid> = _navigateToHistoryDetail.asSharedFlow()

    init {
        viewModelScope.launch {
            practiceRepository.getSessionsForSet(setId).collect { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }
    }

    fun onSessionClick(sessionId: Uuid) {
        viewModelScope.launch { _navigateToHistoryDetail.emit(sessionId) }
    }
}
