package com.chm.flashcards.ui.cardeditor

import androidx.lifecycle.SavedStateHandle
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardWithTags
import com.chm.flashcards.data.repository.Tag
import com.chm.flashcards.ui.navigation.Screen
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CardEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCardRepository: FakeCardRepository
    private lateinit var fakeTagRepository: FakeTagRepository

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val cardId = Uuid.parse("00000000-0000-0000-0000-000000000002")

    @Before
    fun setUp() {
        fakeCardRepository = FakeCardRepository()
        fakeTagRepository = FakeTagRepository()
    }

    private fun createViewModel(withCardId: Uuid? = null): CardEditorViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                Screen.ARG_SET_ID to setId.toString(),
                Screen.ARG_CARD_ID to (withCardId?.toString() ?: ""),
            ),
        )
        return CardEditorViewModel(savedStateHandle, fakeCardRepository, fakeTagRepository)
    }

    @Test
    fun blankFront_saveDisabled_showsError() = runTest {
        val viewModel = createViewModel()

        viewModel.onBackChange("Valid back")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.frontError)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun blankBack_saveDisabled_showsError() = runTest {
        val viewModel = createViewModel()

        viewModel.onFrontChange("Valid front")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.backError)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun frontOver1000Chars_showsError() = runTest {
        val viewModel = createViewModel()

        viewModel.onFrontChange("a".repeat(1001))
        viewModel.onBackChange("Valid back")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.frontError)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun addTagChip_appendsToTagsList_dedupesCaseInsensitive() = runTest {
        val viewModel = createViewModel()

        viewModel.onTagInputChange("Kotlin,")
        viewModel.onTagInputChange("kotlin,")
        viewModel.onTagInputChange("Compose,")
        advanceUntilIdle()

        assertEquals(listOf("Kotlin", "Compose"), viewModel.uiState.value.tags)
    }

    @Test
    fun moreThan10Tags_rejectsAdditionalChip() = runTest {
        val viewModel = createViewModel()

        repeat(10) { i -> viewModel.onTagInputChange("Tag$i,") }
        viewModel.onTagInputChange("Eleventh,")
        advanceUntilIdle()

        assertEquals(10, viewModel.uiState.value.tags.size)
        assertFalse(viewModel.uiState.value.tags.contains("Eleventh"))
    }

    @Test
    fun save_validInput_callsRepositoryCreateCard_createMode() = runTest {
        val viewModel = createViewModel()

        viewModel.onFrontChange("Q")
        viewModel.onBackChange("A")
        viewModel.onTagInputChange("Kotlin,")
        advanceUntilIdle()

        viewModel.onSave()
        advanceUntilIdle()

        val call = fakeCardRepository.createCardCalls.single()
        assertEquals(setId, call.setId)
        assertEquals("Q", call.front)
        assertEquals("A", call.back)
        assertEquals(listOf("Kotlin"), call.tagNames)
        assertTrue(fakeCardRepository.updateCardCalls.isEmpty())
    }

    @Test
    fun loadForEdit_existingCardId_populatesFieldsFromRepository() = runTest {
        val existingCard = Card(id = cardId, setId = setId, front = "Q", back = "A", notes = "note")
        fakeCardRepository.getCardResult = CardWithTags(existingCard, listOf(Tag(Uuid.random(), "Kotlin")))

        val viewModel = createViewModel(withCardId = cardId)
        advanceUntilIdle()

        assertEquals("Q", viewModel.uiState.value.front)
        assertEquals("A", viewModel.uiState.value.back)
        assertEquals("note", viewModel.uiState.value.notes)
        assertEquals(listOf("Kotlin"), viewModel.uiState.value.tags)
        assertNull(viewModel.uiState.value.frontError)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
        assertTrue(viewModel.isEditMode)
    }

    @Test
    fun save_editMode_callsRepositoryUpdateCard() = runTest {
        val existingCard = Card(id = cardId, setId = setId, front = "Q", back = "A", notes = null)
        fakeCardRepository.getCardResult = CardWithTags(existingCard, emptyList())
        val viewModel = createViewModel(withCardId = cardId)
        advanceUntilIdle()

        viewModel.onFrontChange("New front")
        viewModel.onBackChange("New back")
        advanceUntilIdle()

        viewModel.onSave()
        advanceUntilIdle()

        val call = fakeCardRepository.updateCardCalls.single()
        assertEquals(cardId, call.id)
        assertEquals("New front", call.front)
        assertEquals("New back", call.back)
        assertTrue(fakeCardRepository.createCardCalls.isEmpty())
    }
}
