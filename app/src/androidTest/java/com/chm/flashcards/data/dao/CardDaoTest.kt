package com.chm.flashcards.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardSetEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardDaoTest : BaseRoomDaoTest() {

    private lateinit var cardSetDao: CardSetDao
    private lateinit var cardDao: CardDao

    @Before
    fun setUpDao() {
        cardSetDao = database.cardSetDao()
        cardDao = database.cardDao()
    }

    private suspend fun insertSet(): CardSetEntity {
        val set = CardSetEntity(
            id = Uuid.random(),
            name = "Kotlin Basics",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        cardSetDao.insert(set)
        return set
    }

    private fun card(setId: Uuid) = CardEntity(
        id = Uuid.random(),
        setId = setId,
        front = "What is a data class?",
        back = "A class that auto-generates equals/hashCode/toString/copy",
        notes = null,
    )

    @Test
    fun insertUnderSet_getBySetId_returnsCard() = runTest {
        val set = insertSet()
        val cardEntity = card(set.id)

        cardDao.insert(cardEntity)

        cardDao.getBySetId(set.id).test {
            assertEquals(listOf(cardEntity), awaitItem())
        }
    }

    @Test
    fun deletingSet_cascadesDeleteOfCards() = runTest {
        val set = insertSet()
        val cardEntity = card(set.id)
        cardDao.insert(cardEntity)

        cardSetDao.delete(set)

        cardDao.getBySetId(set.id).test {
            assertEquals(emptyList<CardEntity>(), awaitItem())
        }
    }

    @Test
    fun updateStats_persistsCorrectAndIncorrectCounts() = runTest {
        val set = insertSet()
        val cardEntity = card(set.id)
        cardDao.insert(cardEntity)
        val practicedAt = Instant.parse("2026-02-01T12:00:00Z")

        cardDao.updateStats(
            id = cardEntity.id,
            timesCorrect = 3,
            timesIncorrect = 1,
            lastPracticedAt = practicedAt,
        )

        val updated = cardDao.getById(cardEntity.id)
        assertTrue(updated != null)
        assertEquals(3, updated!!.timesCorrect)
        assertEquals(1, updated.timesIncorrect)
        assertEquals(practicedAt, updated.lastPracticedAt)
    }
}
