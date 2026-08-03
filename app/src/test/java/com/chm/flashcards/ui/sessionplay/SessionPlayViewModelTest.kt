package com.chm.flashcards.ui.sessionplay

import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.FakePracticeRepository
import com.chm.flashcards.data.repository.PracticeSessionDraft
import com.chm.flashcards.data.repository.PracticeSessionSummary
import com.chm.flashcards.ui.session.PracticeSessionHolder
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakePracticeRepository: FakePracticeRepository
    private lateinit var sessionHolder: PracticeSessionHolder

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    private fun card(front: String) =
        Card(id = Uuid.random(), setId = setId, front = front, back = "Back for $front", notes = null)

    private fun draft(cards: List<Card>) = PracticeSessionDraft(
        id = Uuid.parse("00000000-0000-0000-0000-000000000002"),
        setId = setId,
        startedAt = Instant.parse("2026-01-01T00:00:00Z"),
        requestedCardCount = cards.size,
        cards = cards,
    )

    @Before
    fun setUp() {
        fakePracticeRepository = FakePracticeRepository()
        sessionHolder = PracticeSessionHolder()
    }

    private fun createViewModel() = SessionPlayViewModel(fakePracticeRepository, sessionHolder)

    @Test
    fun initialState_showsFirstCardFrontNotFlipped() {
        val cards = listOf(card("Q1"), card("Q2"))
        sessionHolder.setDraft(draft(cards))

        val viewModel = createViewModel()

        assertEquals(cards[0], viewModel.uiState.value.currentCard)
        assertEquals(0, viewModel.uiState.value.currentIndex)
        assertEquals(2, viewModel.uiState.value.totalCount)
        assertFalse(viewModel.uiState.value.isFlipped)
        assertFalse(viewModel.uiState.value.isSessionComplete)
    }

    @Test
    fun onFlip_showsBack() {
        sessionHolder.setDraft(draft(listOf(card("Q1"))))
        val viewModel = createViewModel()

        viewModel.onFlip()

        assertTrue(viewModel.uiState.value.isFlipped)
    }

    @Test
    fun onAnswer_advancesToNextCard_resetsFlipped() {
        val cards = listOf(card("Q1"), card("Q2"))
        sessionHolder.setDraft(draft(cards))
        val viewModel = createViewModel()
        viewModel.onFlip()

        viewModel.onAnswer(true)

        assertEquals(1, viewModel.uiState.value.currentIndex)
        assertEquals(cards[1], viewModel.uiState.value.currentCard)
        assertFalse(viewModel.uiState.value.isFlipped)
        assertFalse(viewModel.uiState.value.isSessionComplete)
    }

    @Test
    fun onAnswer_lastCard_setsSessionCompleteAndCallsCompleteSession() = runTest {
        val cards = listOf(card("Q1"))
        val theDraft = draft(cards)
        sessionHolder.setDraft(theDraft)
        val summary = PracticeSessionSummary(sessionId = theDraft.id, correct = cards, incorrect = emptyList())
        fakePracticeRepository.completeSessionResult = summary
        val viewModel = createViewModel()
        viewModel.onFlip()

        var navigatedSessionId: Uuid? = null
        val job = launch { viewModel.navigateToResults.collect { navigatedSessionId = it } }
        advanceUntilIdle()

        viewModel.onAnswer(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSessionComplete)
        assertEquals(1, fakePracticeRepository.completeSessionCalls.size)
        val (calledDraft, calledAnswers) = fakePracticeRepository.completeSessionCalls.single()
        assertEquals(theDraft, calledDraft)
        assertEquals(listOf(cards[0].id), calledAnswers.map { it.cardId })
        assertEquals(theDraft.id, navigatedSessionId)
        job.cancel()
    }

    @Test
    fun abandoningSession_neverCallsCompleteSession() {
        val cards = listOf(card("Q1"), card("Q2"))
        sessionHolder.setDraft(draft(cards))
        val viewModel = createViewModel()
        viewModel.onFlip()
        viewModel.onAnswer(true) // advances to card 2, doesn't finish the session

        // Simulate the user backing out mid-session: the ViewModel is simply
        // dropped/cleared here (no explicit onCleared hook exists that would
        // persist anything), so nothing further should happen.
        assertEquals(0, fakePracticeRepository.completeSessionCalls.size)
        assertNull(sessionHolder.consumeSummary())
    }
}
