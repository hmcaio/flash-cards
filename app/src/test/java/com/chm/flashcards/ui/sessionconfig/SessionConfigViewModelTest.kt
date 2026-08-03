package com.chm.flashcards.ui.sessionconfig

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.FakePracticeRepository
import com.chm.flashcards.ui.cardeditor.FakeCardRepository
import com.chm.flashcards.ui.navigation.Screen
import com.chm.flashcards.ui.session.PracticeSessionHolder
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCardRepository: FakeCardRepository
    private lateinit var fakePracticeRepository: FakePracticeRepository
    private lateinit var sessionHolder: PracticeSessionHolder
    private lateinit var viewModel: SessionConfigViewModel

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    private fun cardWithTags(front: String) =
        CardWithTags(Card(id = Uuid.random(), setId = setId, front = front, back = "Back", notes = null), emptyList())

    private fun createViewModel() {
        val savedStateHandle = SavedStateHandle(mapOf(Screen.ARG_SET_ID to setId.toString()))
        viewModel = SessionConfigViewModel(savedStateHandle, fakeCardRepository, fakePracticeRepository, sessionHolder)
    }

    @Before
    fun setUp() {
        fakeCardRepository = FakeCardRepository()
        fakePracticeRepository = FakePracticeRepository()
        sessionHolder = PracticeSessionHolder()
    }

    @Test
    fun initialState_defaultSelectedCount_isMinOf10AndSetSize_smallSet() = runTest {
        fakeCardRepository.cardsBySetId.value = List(5) { cardWithTags("Q$it") }
        createViewModel()

        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.setSize)
        assertEquals(5, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun initialState_defaultSelectedCount_isMinOf10AndSetSize_largeSet() = runTest {
        fakeCardRepository.cardsBySetId.value = List(25) { cardWithTags("Q$it") }
        createViewModel()

        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.setSize)
        assertEquals(10, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun onCountChange_clampsToOneAndSetSize() = runTest {
        fakeCardRepository.cardsBySetId.value = List(5) { cardWithTags("Q$it") }
        createViewModel()
        advanceUntilIdle()

        viewModel.onCountChange(0)
        assertEquals(1, viewModel.uiState.value.selectedCount)

        viewModel.onCountChange(100)
        assertEquals(5, viewModel.uiState.value.selectedCount)

        viewModel.onCountChange(3)
        assertEquals(3, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun onStartClick_callsStartSessionWithSelectedCount_emitsNavigationEvent() = runTest {
        fakeCardRepository.cardsBySetId.value = List(5) { cardWithTags("Q$it") }
        createViewModel()
        advanceUntilIdle()
        viewModel.onCountChange(3)

        var navigated = false
        val job = launch { viewModel.navigateToSessionPlay.collect { navigated = true } }
        advanceUntilIdle()

        viewModel.onStartClick()
        advanceUntilIdle()

        assertEquals(1, fakePracticeRepository.startSessionCalls.size)
        assertEquals(setId to 3, fakePracticeRepository.startSessionCalls.single())
        assertNotNull(sessionHolder.consumeDraft())
        assertTrue(navigated)
        job.cancel()
    }
}
