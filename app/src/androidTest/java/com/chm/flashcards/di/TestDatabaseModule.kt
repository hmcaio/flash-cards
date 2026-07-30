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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Swaps the real file-backed [FlashCardsDatabase] for an in-memory one in
 * every `@HiltAndroidTest` (Compose screen tests that go through
 * [com.chm.flashcards.MainActivity]/Hilt DI, e.g.
 * [com.chm.flashcards.ui.setlist.SetListScreenTest]) -- per F02 spec.md,
 * so these tests don't touch a real on-disk database. Room DAO tests under
 * `data/dao` don't go through Hilt at all and keep using `BaseRoomDaoTest`'s
 * own in-memory database.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlashCardsDatabase =
        Room.inMemoryDatabaseBuilder(context, FlashCardsDatabase::class.java).build()

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
