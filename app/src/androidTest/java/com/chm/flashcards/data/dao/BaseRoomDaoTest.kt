package com.chm.flashcards.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chm.flashcards.data.FlashCardsDatabase
import org.junit.After
import org.junit.Before

/**
 * Shared setup for Room DAO instrumented tests: builds a fresh in-memory
 * [FlashCardsDatabase] before each test and closes it after, so individual
 * DAO test classes only declare which DAOs they need rather than
 * repeating the `Room.inMemoryDatabaseBuilder` boilerplate.
 */
abstract class BaseRoomDaoTest {

    protected lateinit var database: FlashCardsDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, FlashCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }
}
