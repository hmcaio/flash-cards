package com.chm.flashcards.data.importexport

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin wrapper over [android.content.ContentResolver] writes to a SAF
 * (`ACTION_CREATE_DOCUMENT`) [Uri] -- kept as its own interface (rather than
 * inlined into `ImportExportViewModel`) so ViewModel unit tests can substitute
 * a fake instead of touching real Android content URIs, per F07 spec.md.
 */
interface DocumentWriter {
    suspend fun write(uri: Uri, content: String)
}

class ContentResolverDocumentWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DocumentWriter {
    override suspend fun write(uri: Uri, content: String) {
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open output stream for $uri")
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }
    }
}
