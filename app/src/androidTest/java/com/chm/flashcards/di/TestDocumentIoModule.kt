package com.chm.flashcards.di

import com.chm.flashcards.data.importexport.DocumentReader
import com.chm.flashcards.data.importexport.DocumentWriter
import com.chm.flashcards.data.importexport.InMemoryDocumentStore
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Swaps the real `ContentResolver`-backed [DocumentWriter]/[DocumentReader]
 * for a shared in-memory [InMemoryDocumentStore] in every `@HiltAndroidTest`
 * -- same precedent as [TestDatabaseModule] for the database. Kept as its
 * own replaced module (mirroring [com.chm.flashcards.di.DocumentIoModule])
 * so it doesn't have to re-declare [RepositoryModule]'s other bindings.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DocumentIoModule::class])
abstract class TestDocumentIoModule {

    @Binds
    @Singleton
    abstract fun bindDocumentWriter(impl: InMemoryDocumentStore): DocumentWriter

    @Binds
    @Singleton
    abstract fun bindDocumentReader(impl: InMemoryDocumentStore): DocumentReader
}
