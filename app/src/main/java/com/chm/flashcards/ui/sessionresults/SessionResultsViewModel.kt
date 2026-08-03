package com.chm.flashcards.ui.sessionresults

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.chm.flashcards.ui.navigation.Screen
import com.chm.flashcards.ui.session.PracticeSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin -- just exposes the [com.chm.flashcards.data.repository.PracticeSessionSummary]
 * that `SessionPlayViewModel` stashed in [sessionHolder] right after persisting
 * the finished session, mapped to [SessionResultsUiState]. `sessionId` nav arg
 * (parsed per [Screen]'s convention) is asserted against the consumed
 * summary rather than used to re-query the database -- F05 only ever reaches
 * this screen immediately after finishing a session; re-loading historical
 * sessions by id is F06's job (out of scope here, see spec.md).
 */
@HiltViewModel
class SessionResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionHolder: PracticeSessionHolder,
) : ViewModel() {

    private val sessionId: Uuid = Uuid.parse(checkNotNull(savedStateHandle[Screen.ARG_SESSION_ID]))

    private val _uiState = MutableStateFlow(SessionResultsUiState())
    val uiState: StateFlow<SessionResultsUiState> = _uiState.asStateFlow()

    init {
        val summary = sessionHolder.consumeSummary()?.takeIf { it.sessionId == sessionId }
        if (summary != null) {
            _uiState.value = SessionResultsUiState(correct = summary.correct, incorrect = summary.incorrect)
        }
    }
}
