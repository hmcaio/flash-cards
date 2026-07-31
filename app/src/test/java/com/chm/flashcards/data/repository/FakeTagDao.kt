package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.TagDao
import com.chm.flashcards.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [TagDao] test double for [CardRepositoryImpl]/[TagRepositoryImpl]
 * unit tests -- no Room/Android dependency. [getByName] matches
 * case-insensitively, same as the real DAO's `COLLATE NOCASE` query.
 */
class FakeTagDao : TagDao {

    private val table = MutableStateFlow<List<TagEntity>>(emptyList())

    val inserted = mutableListOf<TagEntity>()

    override suspend fun insert(tag: TagEntity) {
        inserted += tag
        table.value += tag
    }

    override suspend fun delete(tag: TagEntity) {
        table.value = table.value.filterNot { it.id == tag.id }
    }

    override suspend fun getByName(name: String): TagEntity? =
        table.value.find { it.name.equals(name, ignoreCase = true) }

    override fun getAll(): Flow<List<TagEntity>> = table

    /** Seeds the table directly without recording an [inserted] call -- for pre-populating "existing tag" fixtures. */
    fun seed(vararg tags: TagEntity) {
        table.value += tags
    }
}
