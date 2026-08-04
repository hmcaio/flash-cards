package com.chm.flashcards.ui.importexport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chm.flashcards.data.importexport.DocumentReader
import com.chm.flashcards.data.importexport.DocumentWriter
import com.chm.flashcards.data.importexport.ImportValidationResult
import com.chm.flashcards.data.repository.ImportExportRepository
import com.chm.flashcards.data.repository.ImportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Export: writes [ImportExportRepository.exportLibrary]'s JSON to the
 * SAF-picked [Uri] via [documentWriter]. Import: reads the SAF-picked [Uri]
 * via [documentReader], validates it, and on success surfaces the
 * Replace-all/Add-as-new-sets choice ([ImportExportUiState.pendingImport])
 * rather than importing immediately -- [onImportModeConfirmed] runs the
 * actual write once the user picks a mode. [documentWriter]/[documentReader]
 * are injected abstractions over `ContentResolver` (not called directly) so
 * this ViewModel's tests use fakes instead of real content URIs.
 */
@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val repository: ImportExportRepository,
    private val documentWriter: DocumentWriter,
    private val documentReader: DocumentReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportExportUiState())
    val uiState: StateFlow<ImportExportUiState> = _uiState.asStateFlow()

    fun onExportUriSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            val json = repository.exportLibrary()
            documentWriter.write(uri, json)
            _uiState.update { it.copy(isBusy = false, successMessage = "Library exported") }
        }
    }

    fun onImportUriSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null, pendingImport = null) }
            val json = documentReader.read(uri)
            when (val result = repository.validateImport(json)) {
                is ImportValidationResult.Valid ->
                    _uiState.update { it.copy(isBusy = false, pendingImport = result.library) }
                is ImportValidationResult.Malformed ->
                    showError("The selected file isn't a valid export (malformed JSON)")
                is ImportValidationResult.UnsupportedVersion ->
                    showError("Unsupported file version (${result.version})")
                is ImportValidationResult.TooLarge ->
                    showError(result.reason)
                is ImportValidationResult.InvalidContent ->
                    showError(result.details)
            }
        }
    }

    fun onImportModeConfirmed(mode: ImportMode) {
        val library = _uiState.value.pendingImport ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, pendingImport = null) }
            repository.importLibrary(library, mode)
            _uiState.update { it.copy(isBusy = false, successMessage = "Library imported") }
        }
    }

    fun onImportModeCancelled() {
        _uiState.update { it.copy(pendingImport = null) }
    }

    fun onMessageDismissed() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isBusy = false, errorMessage = message, pendingImport = null) }
    }
}
