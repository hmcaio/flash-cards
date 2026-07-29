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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardTagCrossRefDaoTest : BaseRoomDaoTest() {

    private lateinit var cardSetDao: CardSetDao
    private lateinit var cardDao: CardDao
    private lateinit var tagDao: TagDao
    private lateinit var crossRefDao: CardTagCrossRefDao

    @Before
    fun setUpDao() {
        cardSetDao = database.cardSetDao()
        cardDao = database.cardDao()
        tagDao = database.tagDao()
        crossRefDao = database.cardTagCrossRefDao()
    }

    private suspend fun insertSetAndCard(): CardEntity {
        val set = CardSetEntity(id = Uuid.random(), name = "Set", createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        cardSetDao.insert(set)
        val cardEntity = CardEntity(id = Uuid.random(), setId = set.id, front = "F", back = "B", notes = null)
        cardDao.insert(cardEntity)
        return cardEntity
    }

    @Test
    fun attachTagToCard_getTagsForCard_returnsTag() = runTest {
        val cardEntity = insertSetAndCard()
        val tag = TagEntity(id = Uuid.random(), name = "kotlin")
        tagDao.insert(tag)

        crossRefDao.insert(CardTagCrossRef(cardId = cardEntity.id, tagId = tag.id))

        crossRefDao.getTagsForCard(cardEntity.id).test {
            assertEquals(listOf(tag), awaitItem())
        }
    }

    @Test
    fun deletingCard_cascadesDeleteOfCrossRef() = runTest {
        val cardEntity = insertSetAndCard()
        val tag = TagEntity(id = Uuid.random(), name = "kotlin")
        tagDao.insert(tag)
        crossRefDao.insert(CardTagCrossRef(cardId = cardEntity.id, tagId = tag.id))

        cardDao.delete(cardEntity)

        crossRefDao.getTagsForCard(cardEntity.id).test {
            assertEquals(emptyList<TagEntity>(), awaitItem())
        }
    }

    @Test
    fun deletingTag_cascadesDeleteOfCrossRef() = runTest {
        val cardEntity = insertSetAndCard()
        val tag = TagEntity(id = Uuid.random(), name = "kotlin")
        tagDao.insert(tag)
        crossRefDao.insert(CardTagCrossRef(cardId = cardEntity.id, tagId = tag.id))

        tagDao.delete(tag)

        crossRefDao.getTagsForCard(cardEntity.id).test {
            assertEquals(emptyList<TagEntity>(), awaitItem())
        }
    }
}
