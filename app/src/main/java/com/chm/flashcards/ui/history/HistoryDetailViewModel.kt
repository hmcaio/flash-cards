package com.chm.flashcards.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.repository.PracticeRepository
import com.chm.flashcards.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * `sessionId` nav arg parsed once at this ViewModel entry point (per
 * [Screen]'s nav-arg convention). Unlike F05's `SessionResultsViewModel`
 * (which reads a just-finished session's summary out of `PracticeSessionHolder`),
 * this re-queries the database by id via [PracticeRepository.getSessionDetail]
 * -- History Detail is reached for *any* past session, not just the one just
 * played.
 */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    practiceRepository: PracticeRepository,
) : ViewModel() {

    private val sessionId: Uuid = Uuid.parse(checkNotNull(savedStateHandle[Screen.ARG_SESSION_ID]))

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            practiceRepository.getSessionDetail(sessionId).collect { summary ->
                _uiState.update { it.copy(correct = summary.correct, incorrect = summary.incorrect, isLoading = false) }
            }
        }
    }
}
