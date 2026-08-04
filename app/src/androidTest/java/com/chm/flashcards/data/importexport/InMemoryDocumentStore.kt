package com.chm.flashcards.data.importexport

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory [DocumentWriter]/[DocumentReader] test double, bound in place of
 * the real `ContentResolver`-backed implementations for every
 * `@HiltAndroidTest` (see [com.chm.flashcards.di.TestDocumentIoModule]) --
 * per F07 plan.md, so Compose UI tests exercise the real Export/Import
 * buttons (and, via Espresso-Intents, a stubbed SAF picker result) without
 * touching a real on-disk file.
 */
@Singleton
class InMemoryDocumentStore @Inject constructor() : DocumentWriter, DocumentReader {

    private val contents = mutableMapOf<Uri, String>()

    override suspend fun write(uri: Uri, content: String) {
        contents[uri] = content
    }

    override suspend fun read(uri: Uri): String =
        contents[uri] ?: error("InMemoryDocumentStore has no content written for $uri")
}
