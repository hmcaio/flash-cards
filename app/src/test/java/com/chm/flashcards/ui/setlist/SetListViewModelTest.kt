package com.chm.flashcards.ui.setlist

import com.chm.flashcards.common.FakeViewModePreferences
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.dao.CardSetWithCount
import com.chm.flashcards.data.preferences.ViewMode
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SetListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeCardSetRepository
    private lateinit var fakeViewModePreferences: FakeViewModePreferences
    private lateinit var viewModel: SetListViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeCardSetRepository()
        fakeViewModePreferences = FakeViewModePreferences()
        viewModel = SetListViewModel(fakeRepository, fakeViewModePreferences)
    }

    private fun cardSetWithCount(name: String, cardCount: Int = 0) = CardSetWithCount(
        id = Uuid.random(),
        name = name,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        cardCount = cardCount,
    )

    @Test
    fun initialState_loadsSetsFromRepository() = runTest {
        val sets = listOf(cardSetWithCount("Kotlin Basics"), cardSetWithCount("Compose", 3))
        fakeRepository.setsWithCount.value = sets

        advanceUntilIdle()

        assertEquals(sets, viewModel.uiState.value.sets)
    }

    @Test
    fun onCreateNameChange_blankAfterTrim_setsError_onConfirmDoesNotCallRepository() = runTest {
        viewModel.onCreateClick()
        viewModel.onCreateNameChange("   ")

        viewModel.onCreateConfirm()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.createNameError)
        assertTrue(viewModel.uiState.value.isCreateDialogOpen)
        assertTrue(fakeRepository.createSetCalls.isEmpty())
    }

    @Test
    fun onCreateConfirm_validName_callsRepositoryCreateSet_closesDialog() = runTest {
        viewModel.onCreateClick()
        viewModel.onCreateNameChange("Kotlin Basics")

        viewModel.onCreateConfirm()
        advanceUntilIdle()

        assertEquals(listOf("Kotlin Basics"), fakeRepository.createSetCalls)
        assertEquals(false, viewModel.uiState.value.isCreateDialogOpen)
        assertNull(viewModel.uiState.value.createNameError)
    }

    @Test
    fun onDeleteConfirm_callsRepositoryDeleteSet() = runTest {
        val target = cardSetWithCount("Kotlin Basics")
        fakeRepository.setsWithCount.value = listOf(target)
        advanceUntilIdle()

        viewModel.onDeleteRequest(target.id)
        viewModel.onDeleteConfirm(target.id)
        advanceUntilIdle()

        assertEquals(listOf(target.id), fakeRepository.deleteSetCalls)
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun onRename_callsRepositoryRenameSet() = runTest {
        val id = Uuid.random()

        viewModel.onRename(id, "New name")
        advanceUntilIdle()

        assertEquals(listOf(id to "New name"), fakeRepository.renameSetCalls)
    }

    @Test
    fun initialState_loadsPersistedViewModeFromPreferences() = runTest {
        val fakePreferences = FakeViewModePreferences(initial = ViewMode.LIST)
        val vm = SetListViewModel(fakeRepository, fakePreferences)

        advanceUntilIdle()

        assertEquals(ViewMode.LIST, vm.uiState.value.viewMode)
    }

    @Test
    fun onViewModeToggle_flipsListToGrid_andPersists() = runTest {
        val fakePreferences = FakeViewModePreferences(initial = ViewMode.LIST)
        val vm = SetListViewModel(fakeRepository, fakePreferences)
        advanceUntilIdle()

        vm.onViewModeToggle()
        advanceUntilIdle()

        assertEquals(ViewMode.GRID, vm.uiState.value.viewMode)
        assertEquals(listOf(ViewMode.GRID), fakePreferences.setViewModeCalls)
    }

    @Test
    fun onViewModeToggle_flipsGridToList_andPersists() = runTest {
        // default fakeViewModePreferences starts at GRID
        advanceUntilIdle()

        viewModel.onViewModeToggle()
        advanceUntilIdle()

        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
        assertEquals(listOf(ViewMode.LIST), fakeViewModePreferences.setViewModeCalls)
    }
}
