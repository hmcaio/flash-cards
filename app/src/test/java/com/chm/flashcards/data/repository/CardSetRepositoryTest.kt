package com.chm.flashcards.data.repository

import app.cash.turbine.test
import com.chm.flashcards.common.FakeIdGenerator
import com.chm.flashcards.common.FakeTimeProvider
import com.chm.flashcards.data.entity.CardSetEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CardSetRepositoryTest {

    private lateinit var fakeDao: FakeCardSetDao
    private lateinit var fakeIdGenerator: FakeIdGenerator
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var repository: CardSetRepository

    private val fixedId = Uuid.parse("00000000-0000-0000-0000-000000000042")
    private val fixedInstant: Instant = Instant.parse("2026-03-01T10:00:00Z")

    @Before
    fun setUp() {
        fakeDao = FakeCardSetDao()
        fakeIdGenerator = FakeIdGenerator(listOf(fixedId))
        fakeTimeProvider = FakeTimeProvider(fixedInstant)
        repository = CardSetRepositoryImpl(fakeDao, fakeIdGenerator, fakeTimeProvider)
    }

    @Test
    fun createSet_generatesIdAndTimestamp_andInsertsIntoDao() = runTest {
        val result = repository.createSet("Kotlin Basics")

        assertEquals(fixedId, result.id)
        assertEquals("Kotlin Basics", result.name)
        assertEquals(fixedInstant, result.createdAt)

        val insertedEntity = fakeDao.inserted.single()
        assertEquals(fixedId, insertedEntity.id)
        assertEquals("Kotlin Basics", insertedEntity.name)
        assertEquals(fixedInstant, insertedEntity.createdAt)
    }

    @Test
    fun getAllSets_mapsDaoFlowToDomainModel() = runTest {
        repository.getAllSets().test {
            assertEquals(emptyList<CardSet>(), awaitItem())

            repository.createSet("Kotlin Basics")

            val sets = awaitItem()
            assertEquals(1, sets.size)
            assertEquals(fixedId, sets[0].id)
            assertEquals("Kotlin Basics", sets[0].name)
            assertEquals(fixedInstant, sets[0].createdAt)
        }
    }

    @Test
    fun renameSet_updatesExistingEntityName() = runTest {
        val existing = CardSetEntity(id = fixedId, name = "Old name", createdAt = fixedInstant)
        fakeDao.insert(existing)

        repository.renameSet(fixedId, "New name")

        val updatedEntity = fakeDao.updated.single()
        assertEquals(fixedId, updatedEntity.id)
        assertEquals("New name", updatedEntity.name)
        assertEquals(fixedInstant, updatedEntity.createdAt)
    }

    @Test
    fun deleteSet_callsDaoDeleteById() = runTest {
        val existing = CardSetEntity(id = fixedId, name = "Kotlin Basics", createdAt = fixedInstant)
        fakeDao.insert(existing)

        repository.deleteSet(fixedId)

        assertEquals(existing, fakeDao.deleted.single())
        assertNull(fakeDao.getById(fixedId))
    }
}
