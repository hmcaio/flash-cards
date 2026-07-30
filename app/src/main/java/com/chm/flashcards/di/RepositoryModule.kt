package com.chm.flashcards.di

import com.chm.flashcards.data.repository.CardSetRepository
import com.chm.flashcards.data.repository.CardSetRepositoryImpl
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
}
