package com.chm.flashcards.di

import com.chm.flashcards.data.importexport.ContentResolverDocumentReader
import com.chm.flashcards.data.importexport.ContentResolverDocumentWriter
import com.chm.flashcards.data.importexport.DocumentReader
import com.chm.flashcards.data.importexport.DocumentWriter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * F07: binds the SAF/`ContentResolver`-backed [DocumentWriter]/[DocumentReader]
 * used by `ImportExportViewModel`. Kept as its own module (rather than folded
 * into [RepositoryModule]) so instrumented tests can swap in an in-memory fake
 * via `@TestInstallIn(replaces = [DocumentIoModule::class])` without having to
 * re-declare every other binding [RepositoryModule] owns.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentIoModule {

    @Binds
    @Singleton
    abstract fun bindDocumentWriter(impl: ContentResolverDocumentWriter): DocumentWriter

    @Binds
    @Singleton
    abstract fun bindDocumentReader(impl: ContentResolverDocumentReader): DocumentReader
}
