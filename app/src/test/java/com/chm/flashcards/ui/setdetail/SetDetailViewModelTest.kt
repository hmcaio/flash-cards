package com.chm.flashcards.ui.setdetail

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.ui.cardeditor.FakeCardRepository
import com.chm.flashcards.ui.navigation.Screen
import com.chm.flashcards.ui.setlist.FakeCardSetRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SetDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCardRepository: FakeCardRepository
    private lateinit var fakeCardSetRepository: FakeCardSetRepository
    private lateinit var viewModel: SetDetailViewModel

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    @Before
    fun setUp() {
        fakeCardRepository = FakeCardRepository()
        fakeCardSetRepository = FakeCardSetRepository()
        val savedStateHandle = SavedStateHandle(mapOf(Screen.ARG_SET_ID to setId.toString()))
        viewModel = SetDetailViewModel(savedStateHandle, fakeCardRepository, fakeCardSetRepository)
    }

    private fun cardWithTags(front: String) =
        CardWithTags(Card(id = Uuid.random(), setId = setId, front = front, back = "Back", notes = null), emptyList())

    @Test
    fun initialState_loadsCardsForSetId() = runTest {
        val cards = listOf(cardWithTags("Q1"), cardWithTags("Q2"))
        fakeCardRepository.cardsBySetId.value = cards

        advanceUntilIdle()

        assertEquals(cards, viewModel.uiState.value.cards)
    }

    @Test
    fun onDeleteConfirm_callsRepositoryDeleteCard() = runTest {
        val target = cardWithTags("Q1")
        fakeCardRepository.cardsBySetId.value = listOf(target)
        advanceUntilIdle()

        viewModel.onDeleteRequest(target.card.id)
        viewModel.onDeleteConfirm(target.card.id)
        advanceUntilIdle()

        assertEquals(listOf(target.card.id), fakeCardRepository.deleteCardCalls)
        assertNull(viewModel.uiState.value.pendingDelete)
    }
}
