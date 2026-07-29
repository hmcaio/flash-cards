package com.chm.flashcards.di

import android.content.Context
import androidx.room.Room
import com.chm.flashcards.data.FlashCardsDatabase
import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.dao.CardTagCrossRefDao
import com.chm.flashcards.data.dao.PracticeSessionDao
import com.chm.flashcards.data.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlashCardsDatabase =
        Room.databaseBuilder(
            context,
            FlashCardsDatabase::class.java,
            "flash-cards.db",
        ).build()

    @Provides
    fun provideCardSetDao(database: FlashCardsDatabase): CardSetDao = database.cardSetDao()

    @Provides
    fun provideCardDao(database: FlashCardsDatabase): CardDao = database.cardDao()

    @Provides
    fun provideTagDao(database: FlashCardsDatabase): TagDao = database.tagDao()

    @Provides
    fun provideCardTagCrossRefDao(database: FlashCardsDatabase): CardTagCrossRefDao =
        database.cardTagCrossRefDao()

    @Provides
    fun providePracticeSessionDao(database: FlashCardsDatabase): PracticeSessionDao =
        database.practiceSessionDao()
}
