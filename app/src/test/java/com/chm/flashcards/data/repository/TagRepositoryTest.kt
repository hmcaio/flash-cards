package com.chm.flashcards.data.repository

import com.chm.flashcards.data.entity.TagEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TagRepositoryTest {

    private lateinit var fakeTagDao: FakeTagDao
    private lateinit var repository: TagRepository

    @Before
    fun setUp() {
        fakeTagDao = FakeTagDao()
        repository = TagRepositoryImpl(fakeTagDao)
    }

    @Test
    fun getAllTagNames_returnsSortedDistinctNames() = runTest {
        fakeTagDao.seed(
            TagEntity(id = Uuid.random(), name = "Zebra"),
            TagEntity(id = Uuid.random(), name = "apple"),
            TagEntity(id = Uuid.random(), name = "Mango"),
        )

        val result = repository.getAllTagNames().first()

        assertEquals(listOf("Mango", "Zebra", "apple"), result)
    }
}
