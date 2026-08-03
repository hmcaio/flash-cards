package com.chm.flashcards.ui.history

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.FakePracticeRepository
import com.chm.flashcards.data.repository.PracticeSessionSummary
import com.chm.flashcards.ui.navigation.Screen
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePracticeRepository: FakePracticeRepository
    private lateinit var viewModel: HistoryDetailViewModel

    private val sessionId = Uuid.parse("00000000-0000-0000-0000-0000000000aa")

    private fun card(front: String) =
        Card(id = Uuid.random(), setId = Uuid.random(), front = front, back = "Back for $front", notes = null)

    private fun createViewModel() {
        val savedStateHandle = SavedStateHandle(mapOf(Screen.ARG_SESSION_ID to sessionId.toString()))
        viewModel = HistoryDetailViewModel(savedStateHandle, fakePracticeRepository)
    }

    @Before
    fun setUp() {
        fakePracticeRepository = FakePracticeRepository()
    }

    @Test
    fun initialState_loadsSessionDetailBySessionId() = runTest {
        val correctCard = card("Q1")
        val incorrectCard = card("Q2")
        fakePracticeRepository.sessionDetailResult = PracticeSessionSummary(
            sessionId = sessionId,
            correct = listOf(correctCard),
            incorrect = listOf(incorrectCard),
        )

        createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(sessionId), fakePracticeRepository.getSessionDetailCalls)
        assertEquals(listOf(correctCard), viewModel.uiState.value.correct)
        assertEquals(listOf(incorrectCard), viewModel.uiState.value.incorrect)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
