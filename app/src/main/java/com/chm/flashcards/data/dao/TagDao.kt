package com.chm.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.chm.flashcards.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert
    suspend fun insert(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    /** Case-insensitive match (`COLLATE NOCASE`) so "Kotlin" and "kotlin" reuse the same tag. */
    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT * FROM tags")
    fun getAll(): Flow<List<TagEntity>>
}
