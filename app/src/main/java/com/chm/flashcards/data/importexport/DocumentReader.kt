package com.chm.flashcards.data.importexport

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin wrapper over [android.content.ContentResolver] reads from a SAF
 * (`ACTION_OPEN_DOCUMENT`) [Uri] -- see [DocumentWriter] for why this is its
 * own interface rather than inlined into `ImportExportViewModel`.
 */
interface DocumentReader {
    suspend fun read(uri: Uri): String
}

class ContentResolverDocumentReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DocumentReader {
    override suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open input stream for $uri")
        stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
