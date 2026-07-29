package com.chm.flashcards.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.chm.flashcards.data.entity.CardSetEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardSetDaoTest : BaseRoomDaoTest() {

    private lateinit var cardSetDao: CardSetDao

    @Before
    fun setUpDao() {
        cardSetDao = database.cardSetDao()
    }

    @Test
    fun insertAndGetById_returnsInsertedSet() = runTest {
        val set = CardSetEntity(
            id = Uuid.random(),
            name = "Kotlin Basics",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        cardSetDao.insert(set)
        val result = cardSetDao.getById(set.id)

        assertEquals(set, result)
    }

    @Test
    fun getAll_emitsUpdatedListOnInsert() = runTest {
        val set = CardSetEntity(
            id = Uuid.random(),
            name = "Kotlin Basics",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        cardSetDao.getAll().test {
            assertEquals(emptyList<CardSetEntity>(), awaitItem())

            cardSetDao.insert(set)

            assertEquals(listOf(set), awaitItem())
        }
    }

    @Test
    fun delete_removesSet() = runTest {
        val set = CardSetEntity(
            id = Uuid.random(),
            name = "Kotlin Basics",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        cardSetDao.insert(set)

        cardSetDao.delete(set)

        assertNull(cardSetDao.getById(set.id))
    }
}
