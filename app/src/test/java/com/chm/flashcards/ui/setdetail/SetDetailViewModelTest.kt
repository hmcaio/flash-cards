package com.chm.flashcards.ui.setdetail

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.Tag
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

    @Test
    fun onSearchQueryChange_updatesCardsFromSearchResults() = runTest {
        fakeCardRepository.cardsBySetId.value = listOf(cardWithTags("Q1"))
        advanceUntilIdle()
        val searchResult = listOf(cardWithTags("Matched"))
        fakeCardRepository.searchResults.value = searchResult

        viewModel.onSearchQueryChange("match")
        advanceUntilIdle()

        assertEquals("match", viewModel.uiState.value.searchQuery)
        assertEquals(searchResult, viewModel.uiState.value.cards)
        assertEquals(
            listOf(FakeCardRepository.SearchCardsCall(setId, "match", null)),
            fakeCardRepository.searchCardsCalls,
        )
    }

    @Test
    fun onTagFilterSelect_filtersToTag() = runTest {
        val tagId = Uuid.parse("00000000-0000-0000-0000-000000000099")
        val searchResult = listOf(cardWithTags("Tagged"))
        fakeCardRepository.searchResults.value = searchResult

        viewModel.onTagFilterSelect(tagId)
        advanceUntilIdle()

        assertEquals(tagId, viewModel.uiState.value.selectedTagFilter)
        assertEquals(searchResult, viewModel.uiState.value.cards)
        assertEquals(
            listOf(FakeCardRepository.SearchCardsCall(setId, "", tagId)),
            fakeCardRepository.searchCardsCalls,
        )
    }

    @Test
    fun blankSearchQuery_treatedAsEmpty_returnsFullList() = runTest {
        val fullList = listOf(cardWithTags("Q1"), cardWithTags("Q2"))
        fakeCardRepository.cardsBySetId.value = fullList
        advanceUntilIdle()

        viewModel.onSearchQueryChange("   ")
        advanceUntilIdle()

        assertEquals(fullList, viewModel.uiState.value.cards)
        assertEquals(emptyList<FakeCardRepository.SearchCardsCall>(), fakeCardRepository.searchCardsCalls)
    }

    @Test
    fun availableTagFilters_onlyIncludesTagsPresentInThisSet() = runTest {
        val tagA = Tag(Uuid.parse("00000000-0000-0000-0000-0000000000a1"), "Kotlin")
        val tagB = Tag(Uuid.parse("00000000-0000-0000-0000-0000000000a2"), "Compose")
        val cards = listOf(
            CardWithTags(Card(Uuid.random(), setId, "Q1", "A1", null), listOf(tagA)),
            CardWithTags(Card(Uuid.random(), setId, "Q2", "A2", null), listOf(tagA, tagB)),
        )
        fakeCardRepository.cardsBySetId.value = cards

        advanceUntilIdle()

        assertEquals(setOf(tagA, tagB), viewModel.uiState.value.availableTagFilters.toSet())
    }
}
