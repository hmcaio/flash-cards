package com.chm.flashcards.ui.history

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.dao.PracticeSessionListItem
import com.chm.flashcards.data.repository.FakePracticeRepository
import com.chm.flashcards.ui.navigation.Screen
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePracticeRepository: FakePracticeRepository
    private lateinit var viewModel: HistoryListViewModel

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val sessionId = Uuid.parse("00000000-0000-0000-0000-0000000000aa")

    private fun createViewModel() {
        val savedStateHandle = SavedStateHandle(mapOf(Screen.ARG_SET_ID to setId.toString()))
        viewModel = HistoryListViewModel(savedStateHandle, fakePracticeRepository)
    }

    @Before
    fun setUp() {
        fakePracticeRepository = FakePracticeRepository()
    }

    @Test
    fun initialState_loadsSessionsForSetId() = runTest {
        val item = PracticeSessionListItem(
            sessionId = sessionId,
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            correctCount = 3,
            totalCount = 5,
        )
        fakePracticeRepository.sessionsForSet.value = listOf(item)

        createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(setId), fakePracticeRepository.getSessionsForSetCalls)
        assertEquals(listOf(item), viewModel.uiState.value.sessions)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSessionClick_emitsNavigationEventWithSessionId() = runTest {
        createViewModel()
        advanceUntilIdle()

        var navigatedTo: Uuid? = null
        val job = launch { viewModel.navigateToHistoryDetail.collect { navigatedTo = it } }
        advanceUntilIdle()

        viewModel.onSessionClick(sessionId)
        advanceUntilIdle()

        assertEquals(sessionId, navigatedTo)
        job.cancel()
    }
}
