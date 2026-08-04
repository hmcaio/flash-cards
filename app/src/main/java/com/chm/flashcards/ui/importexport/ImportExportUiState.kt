package com.chm.flashcards.ui.importexport

import com.chm.flashcards.data.importexport.LibraryExportDto

/**
 * UI state for [ImportExportScreen]. [pendingImport] non-null is what drives
 * the Replace-all/Add-as-new-sets mode choice dialog -- it holds the
 * already-[com.chm.flashcards.data.importexport.ImportValidationResult.Valid]
 * parsed library waiting on the user's mode pick.
 */
data class ImportExportUiState(
    val isBusy: Boolean = false,
    val pendingImport: LibraryExportDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
