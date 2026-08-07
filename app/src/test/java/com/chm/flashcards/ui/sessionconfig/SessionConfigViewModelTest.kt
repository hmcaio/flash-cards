package com.chm.flashcards.ui.sessionconfig

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.FakePracticeRepository
import com.chm.flashcards.data.repository.Tag
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

    private fun cardWithTags(front: String, tags: List<Tag> = emptyList()) =
        CardWithTags(Card(id = Uuid.random(), setId = setId, front = front, back = "Back", notes = null), tags)

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
        val call = fakePracticeRepository.startSessionCalls.single()
        assertEquals(setId, call.setId)
        assertEquals(3, call.cardCount)
        assertEquals(emptySet<Uuid>(), call.tagIds)
        assertNotNull(sessionHolder.consumeDraft())
        assertTrue(navigated)
        job.cancel()
    }

    // --- C002: tag filter ---------------------------------------------------

    @Test
    fun initialState_derivesAvailableTagsFromAllCards_distinctById() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        val basicsTag = Tag(Uuid.random(), "Basics")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags("Q1", listOf(kotlinTag, basicsTag)),
            cardWithTags("Q2", listOf(kotlinTag)),
            cardWithTags("Q3"),
        )
        createViewModel()

        advanceUntilIdle()

        assertEquals(listOf(kotlinTag, basicsTag), viewModel.uiState.value.availableTags)
        assertEquals(emptySet<Uuid>(), viewModel.uiState.value.selectedTagIds)
        assertEquals(3, viewModel.uiState.value.setSize)
    }

    @Test
    fun onTagToggle_narrowsSetSizeToMatchingCards_orSemantics() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        val basicsTag = Tag(Uuid.random(), "Basics")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags("Q1", listOf(kotlinTag)),
            cardWithTags("Q2", listOf(basicsTag)),
            cardWithTags("Q3", listOf(kotlinTag, basicsTag)),
            cardWithTags("Q4"),
        )
        createViewModel()
        advanceUntilIdle()

        viewModel.onTagToggle(kotlinTag.id)
        assertEquals(setOf(kotlinTag.id), viewModel.uiState.value.selectedTagIds)
        assertEquals(2, viewModel.uiState.value.setSize) // Q1, Q3

        viewModel.onTagToggle(basicsTag.id)
        assertEquals(setOf(kotlinTag.id, basicsTag.id), viewModel.uiState.value.selectedTagIds)
        assertEquals(3, viewModel.uiState.value.setSize) // Q1, Q2, Q3 (OR)

        // Toggling a selected tag back off removes it from the filter.
        viewModel.onTagToggle(kotlinTag.id)
        assertEquals(setOf(basicsTag.id), viewModel.uiState.value.selectedTagIds)
        assertEquals(2, viewModel.uiState.value.setSize) // Q2, Q3
    }

    @Test
    fun onTagToggle_clearingAllTags_restoresFullSetSize_defaultBehaviorUnchanged() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags("Q1", listOf(kotlinTag)),
            cardWithTags("Q2"),
        )
        createViewModel()
        advanceUntilIdle()

        viewModel.onTagToggle(kotlinTag.id)
        assertEquals(1, viewModel.uiState.value.setSize)

        viewModel.onTagToggle(kotlinTag.id) // toggle back off -> empty selection
        assertEquals(emptySet<Uuid>(), viewModel.uiState.value.selectedTagIds)
        assertEquals(2, viewModel.uiState.value.setSize)
    }

    @Test
    fun onTagToggle_reClampsSelectedCount_whenOutOfNewFilteredRange_withoutResettingToDefault() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags("Q1", listOf(kotlinTag)),
            cardWithTags("Q2"),
            cardWithTags("Q3"),
        )
        createViewModel()
        advanceUntilIdle()
        viewModel.onCountChange(3) // user picked the full unfiltered set size

        viewModel.onTagToggle(kotlinTag.id) // only 1 card matches now -> clamp, don't reset to a default

        assertEquals(1, viewModel.uiState.value.setSize)
        assertEquals(1, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun onTagToggle_keepsInRangeSelectedCount_untouched() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags("Q1", listOf(kotlinTag)),
            cardWithTags("Q2", listOf(kotlinTag)),
            cardWithTags("Q3"),
        )
        createViewModel()
        advanceUntilIdle()
        viewModel.onCountChange(2)

        viewModel.onTagToggle(kotlinTag.id) // 2 cards still match -- selectedCount of 2 stays valid, untouched

        assertEquals(2, viewModel.uiState.value.setSize)
        assertEquals(2, viewModel.uiState.value.selectedCount)
    }

    @Test
    fun onStartClick_passesCurrentSelectedTagIds_toPracticeRepository() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags("Q1", listOf(kotlinTag)),
            cardWithTags("Q2"),
        )
        createViewModel()
        advanceUntilIdle()
        viewModel.onTagToggle(kotlinTag.id)

        viewModel.onStartClick()
        advanceUntilIdle()

        val call = fakePracticeRepository.startSessionCalls.single()
        assertEquals(setOf(kotlinTag.id), call.tagIds)
        assertEquals(1, call.cardCount)
    }
}
