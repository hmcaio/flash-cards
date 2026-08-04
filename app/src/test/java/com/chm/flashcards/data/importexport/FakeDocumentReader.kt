package com.chm.flashcards.data.importexport

import android.net.Uri

/** In-memory [DocumentReader] test double for [com.chm.flashcards.ui.importexport.ImportExportViewModelTest]. */
class FakeDocumentReader : DocumentReader {

    var contentToReturn: String = ""
    val readCalls = mutableListOf<Uri>()

    override suspend fun read(uri: Uri): String {
        readCalls += uri
        return contentToReturn
    }
}
