package com.chm.flashcards.di

import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.common.SystemTimeProvider
import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.common.UuidIdGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilModule {

    @Binds
    @Singleton
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
