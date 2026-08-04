package com.chm.flashcards.ui.importexport

import android.net.Uri
import com.chm.flashcards.common.MainDispatcherRule
import com.chm.flashcards.data.importexport.FakeDocumentReader
import com.chm.flashcards.data.importexport.FakeDocumentWriter
import com.chm.flashcards.data.importexport.ImportValidationResult
import com.chm.flashcards.data.importexport.LibraryExportDto
import com.chm.flashcards.data.importexport.SetExportDto
import com.chm.flashcards.data.repository.ImportMode
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class ImportExportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeImportExportRepository
    private lateinit var fakeDocumentWriter: FakeDocumentWriter
    private lateinit var fakeDocumentReader: FakeDocumentReader
    private lateinit var viewModel: ImportExportViewModel

    private val uri: Uri = mock(Uri::class.java)

    @Before
    fun setUp() {
        fakeRepository = FakeImportExportRepository()
        fakeDocumentWriter = FakeDocumentWriter()
        fakeDocumentReader = FakeDocumentReader()
        viewModel = ImportExportViewModel(fakeRepository, fakeDocumentWriter, fakeDocumentReader)
    }

    @Test
    fun onExportUriSelected_writesRepositoryExportOutputToUri() = runTest {
        fakeRepository.exportLibraryResult = """{"schemaVersion":1,"exportedAt":"now","sets":[]}"""

        viewModel.onExportUriSelected(uri)
        advanceUntilIdle()

        assertEquals(1, fakeDocumentWriter.writeCalls.size)
        assertEquals(uri, fakeDocumentWriter.writeCalls.single().first)
        assertEquals(fakeRepository.exportLibraryResult, fakeDocumentWriter.writeCalls.single().second)
        assertEquals("Library exported", viewModel.uiState.value.successMessage)
    }

    @Test
    fun onImportUriSelected_invalidJson_showsErrorState_doesNotPromptModeChoice() = runTest {
        fakeDocumentReader.contentToReturn = "not valid json"
        fakeRepository.validateImportResult = ImportValidationResult.Malformed

        viewModel.onImportUriSelected(uri)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)
        assertNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun onImportUriSelected_validJson_showsModeChoiceDialog() = runTest {
        val library = LibraryExportDto(exportedAt = "now", sets = listOf(SetExportDto(name = "Set", cards = emptyList())))
        fakeDocumentReader.contentToReturn = "irrelevant, repository.validateImport is faked"
        fakeRepository.validateImportResult = ImportValidationResult.Valid(library)

        viewModel.onImportUriSelected(uri)
        advanceUntilIdle()

        assertEquals(library, viewModel.uiState.value.pendingImport)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onImportModeConfirmed_callsRepositoryImportLibrary() = runTest {
        val library = LibraryExportDto(exportedAt = "now", sets = listOf(SetExportDto(name = "Set", cards = emptyList())))
        fakeRepository.validateImportResult = ImportValidationResult.Valid(library)
        viewModel.onImportUriSelected(uri)
        advanceUntilIdle()

        viewModel.onImportModeConfirmed(ImportMode.AddAsNewSets)
        advanceUntilIdle()

        assertEquals(1, fakeRepository.importLibraryCalls.size)
        assertEquals(library to ImportMode.AddAsNewSets, fakeRepository.importLibraryCalls.single())
        assertNull(viewModel.uiState.value.pendingImport)
        assertEquals("Library imported", viewModel.uiState.value.successMessage)
    }

    @Test
    fun onImportModeCancelled_clearsPendingImport_withoutCallingRepository() = runTest {
        val library = LibraryExportDto(exportedAt = "now", sets = emptyList())
        fakeRepository.validateImportResult = ImportValidationResult.Valid(library)
        viewModel.onImportUriSelected(uri)
        advanceUntilIdle()

        viewModel.onImportModeCancelled()

        assertNull(viewModel.uiState.value.pendingImport)
        assertTrue(fakeRepository.importLibraryCalls.isEmpty())
    }
}
