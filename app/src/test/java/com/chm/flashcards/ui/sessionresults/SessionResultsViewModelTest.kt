package com.chm.flashcards.ui.sessionresults

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.PracticeSessionSummary
import com.chm.flashcards.ui.navigation.Screen
import com.chm.flashcards.ui.session.PracticeSessionHolder
import kotlin.uuid.Uuid
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Trivial mapping check per plan.md step 36 -- [SessionResultsViewModel] is
 * "thin, just exposes the summary passed via ... [PracticeSessionHolder] as
 * [SessionResultsUiState]".
 */
class SessionResultsViewModelTest {

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val sessionId = Uuid.parse("00000000-0000-0000-0000-000000000002")

    private fun card(front: String) =
        Card(id = Uuid.random(), setId = setId, front = front, back = "Back for $front", notes = null)

    private fun createViewModel(sessionHolder: PracticeSessionHolder) = SessionResultsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Screen.ARG_SESSION_ID to sessionId.toString())),
        sessionHolder = sessionHolder,
    )

    @Test
    fun initialState_mapsHeldSummaryIntoUiState() {
        val correct = listOf(card("Q1"))
        val incorrect = listOf(card("Q2"))
        val sessionHolder = PracticeSessionHolder()
        sessionHolder.setSummary(PracticeSessionSummary(sessionId = sessionId, correct = correct, incorrect = incorrect))

        val viewModel = createViewModel(sessionHolder)

        assertEquals(correct, viewModel.uiState.value.correct)
        assertEquals(incorrect, viewModel.uiState.value.incorrect)
    }

    @Test
    fun initialState_noHeldSummary_emptyUiState() {
        val sessionHolder = PracticeSessionHolder()

        val viewModel = createViewModel(sessionHolder)

        assertEquals(emptyList<Card>(), viewModel.uiState.value.correct)
        assertEquals(emptyList<Card>(), viewModel.uiState.value.incorrect)
    }
}
