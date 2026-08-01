package com.chm.flashcards.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardSetEntity
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.TagEntity
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

    @Test
    fun getCardsWithTagsBySetId_returnsCardsJoinedWithTheirTags() = runTest {
        val set = insertSet()
        val cardEntity = card(set.id)
        cardDao.insert(cardEntity)
        val tagDao = database.tagDao()
        val crossRefDao = database.cardTagCrossRefDao()
        val tag1 = TagEntity(id = Uuid.random(), name = "kotlin")
        val tag2 = TagEntity(id = Uuid.random(), name = "basics")
        tagDao.insert(tag1)
        tagDao.insert(tag2)
        crossRefDao.insert(CardTagCrossRef(cardId = cardEntity.id, tagId = tag1.id))
        crossRefDao.insert(CardTagCrossRef(cardId = cardEntity.id, tagId = tag2.id))

        cardDao.getCardsWithTagsBySetId(set.id).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(cardEntity, result[0].card)
            assertEquals(setOf(tag1, tag2), result[0].tags.toSet())
        }
    }

    private fun card(setId: Uuid, front: String, back: String, notes: String? = null) = CardEntity(
        id = Uuid.random(),
        setId = setId,
        front = front,
        back = back,
        notes = notes,
    )

    @Test
    fun searchCards_emptyQueryNoTagFilter_returnsAllCardsInSet() = runTest {
        val set = insertSet()
        val card1 = card(set.id, "What is a data class?", "Auto equals/hashCode/toString/copy")
        val card2 = card(set.id, "What is Compose?", "A declarative UI toolkit")
        cardDao.insert(card1)
        cardDao.insert(card2)

        cardDao.searchCards(set.id, "", null).test {
            val result = awaitItem()
            assertEquals(setOf(card1, card2), result.map { it.card }.toSet())
        }
    }

    @Test
    fun searchCards_queryMatchesFrontCaseInsensitive_returnsMatchingCard() = runTest {
        val set = insertSet()
        val card1 = card(set.id, "What is a data class?", "Auto equals/hashCode/toString/copy")
        val card2 = card(set.id, "What is Compose?", "A declarative UI toolkit")
        cardDao.insert(card1)
        cardDao.insert(card2)

        cardDao.searchCards(set.id, "COMPOSE", null).test {
            val result = awaitItem()
            assertEquals(listOf(card2), result.map { it.card })
        }
    }

    @Test
    fun searchCards_queryMatchesBackOrNotes_returnsMatchingCard() = runTest {
        val set = insertSet()
        val card1 = card(set.id, "What is a data class?", "Auto equals/hashCode/toString/copy")
        val card2 = card(set.id, "What is Compose?", "A declarative UI toolkit", notes = "Jetpack Compose")
        cardDao.insert(card1)
        cardDao.insert(card2)

        cardDao.searchCards(set.id, "toolkit", null).test {
            assertEquals(listOf(card2), awaitItem().map { it.card })
        }

        cardDao.searchCards(set.id, "jetpack", null).test {
            assertEquals(listOf(card2), awaitItem().map { it.card })
        }
    }

    @Test
    fun searchCards_tagIdFilter_returnsOnlyCardsWithThatTag() = runTest {
        val set = insertSet()
        val card1 = card(set.id, "What is a data class?", "Auto equals/hashCode/toString/copy")
        val card2 = card(set.id, "What is Compose?", "A declarative UI toolkit")
        cardDao.insert(card1)
        cardDao.insert(card2)
        val tagDao = database.tagDao()
        val crossRefDao = database.cardTagCrossRefDao()
        val tag = TagEntity(id = Uuid.random(), name = "kotlin")
        tagDao.insert(tag)
        crossRefDao.insert(CardTagCrossRef(cardId = card1.id, tagId = tag.id))

        cardDao.searchCards(set.id, "", tag.id).test {
            val result = awaitItem()
            assertEquals(listOf(card1), result.map { it.card })
        }
    }

    @Test
    fun searchCards_queryAndTagIdCombined_appliesBoth() = runTest {
        val set = insertSet()
        val card1 = card(set.id, "What is a data class?", "Auto equals/hashCode/toString/copy")
        val card2 = card(set.id, "What is Compose?", "A declarative UI toolkit")
        cardDao.insert(card1)
        cardDao.insert(card2)
        val tagDao = database.tagDao()
        val crossRefDao = database.cardTagCrossRefDao()
        val tag = TagEntity(id = Uuid.random(), name = "kotlin")
        tagDao.insert(tag)
        crossRefDao.insert(CardTagCrossRef(cardId = card1.id, tagId = tag.id))
        crossRefDao.insert(CardTagCrossRef(cardId = card2.id, tagId = tag.id))

        cardDao.searchCards(set.id, "compose", tag.id).test {
            val result = awaitItem()
            assertEquals(listOf(card2), result.map { it.card })
        }
    }
}
