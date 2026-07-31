package com.chm.flashcards.data.repository

import app.cash.turbine.test
import com.chm.flashcards.common.FakeIdGenerator
import com.chm.flashcards.data.dao.CardWithTagsEntity
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.TagEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardRepositoryTest {

    private lateinit var fakeCardDao: FakeCardDao
    private lateinit var fakeTagDao: FakeTagDao
    private lateinit var fakeCrossRefDao: FakeCardTagCrossRefDao
    private lateinit var fakeIdGenerator: FakeIdGenerator
    private lateinit var repository: CardRepository

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000010")
    private val cardId = Uuid.parse("00000000-0000-0000-0000-000000000020")
    private val tagId1 = Uuid.parse("00000000-0000-0000-0000-000000000030")
    private val tagId2 = Uuid.parse("00000000-0000-0000-0000-000000000031")

    @Before
    fun setUp() {
        fakeCardDao = FakeCardDao()
        fakeTagDao = FakeTagDao()
        fakeCrossRefDao = FakeCardTagCrossRefDao()
        fakeIdGenerator = FakeIdGenerator(listOf(cardId, tagId1, tagId2))
        repository = CardRepositoryImpl(fakeCardDao, fakeTagDao, fakeCrossRefDao, fakeIdGenerator)
    }

    @Test
    fun createCard_newTags_createsTagsAndCrossRefs_returnsCard() = runTest {
        val result = repository.createCard(setId, "Front", "Back", "Notes", listOf("Kotlin", "Compose"))

        assertEquals(cardId, result.id)
        assertEquals(setId, result.setId)
        assertEquals("Front", result.front)
        assertEquals("Back", result.back)
        assertEquals("Notes", result.notes)

        assertEquals(1, fakeCardDao.inserted.size)
        assertEquals(2, fakeTagDao.inserted.size)
        assertEquals(setOf("Kotlin", "Compose"), fakeTagDao.inserted.map { it.name }.toSet())

        val crossRefTagIds = fakeCrossRefDao.inserted.map { it.tagId }.toSet()
        assertEquals(setOf(tagId1, tagId2), crossRefTagIds)
        assertTrue(fakeCrossRefDao.inserted.all { it.cardId == cardId })
    }

    @Test
    fun createCard_existingTagName_reusesExistingTagId_doesNotInsertDuplicateTag() = runTest {
        fakeTagDao.seed(TagEntity(id = tagId1, name = "Kotlin"))

        repository.createCard(setId, "Front", "Back", null, listOf("kotlin"))

        assertTrue(fakeTagDao.inserted.isEmpty())
        val crossRef = fakeCrossRefDao.inserted.single()
        assertEquals(tagId1, crossRef.tagId)
    }

    @Test
    fun updateCard_replacesTagCrossRefs() = runTest {
        val existing = CardEntity(id = cardId, setId = setId, front = "Old", back = "Old", notes = null)
        fakeCardDao.insert(existing)

        repository.updateCard(cardId, "New front", "New back", null, listOf("NewTag"))

        assertEquals(listOf(cardId), fakeCrossRefDao.deletedByCardIdCalls)
        val crossRef = fakeCrossRefDao.inserted.single()
        assertEquals(cardId, crossRef.cardId)
        val updatedEntity = fakeCardDao.updated.single()
        assertEquals("New front", updatedEntity.front)
        assertEquals("New back", updatedEntity.back)
    }

    @Test
    fun deleteCard_callsDaoDelete() = runTest {
        val existing = CardEntity(id = cardId, setId = setId, front = "Front", back = "Back", notes = null)
        fakeCardDao.insert(existing)

        repository.deleteCard(cardId)

        assertEquals(existing, fakeCardDao.deleted.single())
        assertNull(fakeCardDao.getById(cardId))
    }

    @Test
    fun getCardsBySetId_mapsToDomainCardWithTags() = runTest {
        val cardEntity = CardEntity(id = cardId, setId = setId, front = "Front", back = "Back", notes = null)
        val tagEntity = TagEntity(id = tagId1, name = "Kotlin")
        fakeCardDao.cardsWithTagsFlow.value = listOf(CardWithTagsEntity(card = cardEntity, tags = listOf(tagEntity)))

        repository.getCardsBySetId(setId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(cardId, result[0].card.id)
            assertEquals("Front", result[0].card.front)
            assertEquals(listOf(Tag(tagId1, "Kotlin")), result[0].tags)
        }
    }
}
