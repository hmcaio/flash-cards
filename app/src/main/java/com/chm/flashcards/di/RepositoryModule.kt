package com.chm.flashcards.di

import com.chm.flashcards.data.repository.CardRepository
import com.chm.flashcards.data.repository.CardRepositoryImpl
import com.chm.flashcards.data.repository.CardSetRepository
import com.chm.flashcards.data.repository.CardSetRepositoryImpl
import com.chm.flashcards.data.repository.TagRepository
import com.chm.flashcards.data.repository.TagRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCardSetRepository(impl: CardSetRepositoryImpl): CardSetRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
