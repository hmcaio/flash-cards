package com.chm.flashcards.ui.importexport

import com.chm.flashcards.data.importexport.ImportValidationResult
import com.chm.flashcards.data.importexport.LibraryExportDto
import com.chm.flashcards.data.repository.ImportExportRepository
import com.chm.flashcards.data.repository.ImportMode

/** In-memory [ImportExportRepository] test double for [ImportExportViewModelTest]. */
class FakeImportExportRepository : ImportExportRepository {

    var exportLibraryResult: String = "{}"
    var validateImportResult: ImportValidationResult = ImportValidationResult.Malformed

    val exportLibraryCalls = mutableListOf<Unit>()
    val validateImportCalls = mutableListOf<String>()
    val importLibraryCalls = mutableListOf<Pair<LibraryExportDto, ImportMode>>()

    override suspend fun exportLibrary(): String {
        exportLibraryCalls += Unit
        return exportLibraryResult
    }

    override fun validateImport(json: String): ImportValidationResult {
        validateImportCalls += json
        return validateImportResult
    }

    override suspend fun importLibrary(parsed: LibraryExportDto, mode: ImportMode) {
        importLibraryCalls += parsed to mode
    }
}
