package com.chm.flashcards.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chm.flashcards.data.entity.TagEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDaoTest : BaseRoomDaoTest() {

    private lateinit var tagDao: TagDao

    @Before
    fun setUpDao() {
        tagDao = database.tagDao()
    }

    @Test
    fun insertAndGetByName_returnsTag() = runTest {
        val tag = TagEntity(id = Uuid.random(), name = "kotlin")

        tagDao.insert(tag)
        val result = tagDao.getByName("kotlin")

        assertEquals(tag, result)
    }

    @Test
    fun getByName_caseInsensitiveMatch_returnsExistingTag() = runTest {
        val tag = TagEntity(id = Uuid.random(), name = "Kotlin")
        tagDao.insert(tag)

        val result = tagDao.getByName("kotlin")

        assertEquals(tag, result)
    }
}
