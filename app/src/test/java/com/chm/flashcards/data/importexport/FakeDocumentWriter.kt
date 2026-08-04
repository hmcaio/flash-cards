package com.chm.flashcards.data.importexport

import android.net.Uri

/** In-memory [DocumentWriter] test double for [com.chm.flashcards.ui.importexport.ImportExportViewModelTest]. */
class FakeDocumentWriter : DocumentWriter {

    val writeCalls = mutableListOf<Pair<Uri, String>>()

    override suspend fun write(uri: Uri, content: String) {
        writeCalls += uri to content
    }
}
