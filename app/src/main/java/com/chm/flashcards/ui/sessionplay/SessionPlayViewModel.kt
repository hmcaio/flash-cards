package com.chm.flashcards.ui.sessionplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.repository.CardAnswer
import com.chm.flashcards.data.repository.PracticeRepository
import com.chm.flashcards.data.repository.PracticeSessionDraft
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Consumes the [PracticeSessionDraft] that `SessionConfigViewModel` built and
 * stashed in [sessionHolder] -- the `setId` nav arg isn't needed past that
 * hand-off (the draft already carries the selected cards), so unlike the
 * other two session ViewModels this one doesn't take a `SavedStateHandle`.
 * Buffers each flip+answer in memory ([answers]) and only calls
 * [PracticeRepository.completeSession] once, on the last card -- backing out
 * before then (this ViewModel just getting cleared, e.g. system back press)
 * never calls it, so an abandoned session leaves no trace, per spec.md.
 */
@HiltViewModel
class SessionPlayViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val sessionHolder: PracticeSessionHolder,
) : ViewModel() {

    private val draft: PracticeSessionDraft? = sessionHolder.consumeDraft()
    private val cards = draft?.cards.orEmpty()
    private val answers = mutableListOf<CardAnswer>()

    private val _uiState = MutableStateFlow(
        SessionPlayUiState(
            currentIndex = 0,
            totalCount = cards.size,
            currentCard = cards.firstOrNull(),
            isFlipped = false,
            isSessionComplete = false,
        ),
    )
    val uiState: StateFlow<SessionPlayUiState> = _uiState.asStateFlow()

    private val _navigateToResults = MutableSharedFlow<Uuid>()
    val navigateToResults: SharedFlow<Uuid> = _navigateToResults.asSharedFlow()

    fun onFlip() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun onAnswer(correct: Boolean) {
        val state = _uiState.value
        val currentCard = state.currentCard ?: return
        answers += CardAnswer(currentCard.id, correct)

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= cards.size) {
            _uiState.update { it.copy(isSessionComplete = true) }
            finishSession()
        } else {
            _uiState.update {
                it.copy(currentIndex = nextIndex, currentCard = cards[nextIndex], isFlipped = false)
            }
        }
    }

    private fun finishSession() {
        val currentDraft = draft ?: return
        viewModelScope.launch {
            val summary = practiceRepository.completeSession(currentDraft, answers.toList())
            sessionHolder.setSummary(summary)
            _navigateToResults.emit(summary.sessionId)
        }
    }
}
