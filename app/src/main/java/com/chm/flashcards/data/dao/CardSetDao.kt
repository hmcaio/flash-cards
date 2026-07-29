package com.chm.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chm.flashcards.data.entity.CardSetEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface CardSetDao {
    @Insert
    suspend fun insert(cardSet: CardSetEntity)

    @Update
    suspend fun update(cardSet: CardSetEntity)

    @Delete
    suspend fun delete(cardSet: CardSetEntity)

    @Query("SELECT * FROM card_sets WHERE id = :id")
    suspend fun getById(id: Uuid): CardSetEntity?

    @Query("SELECT * FROM card_sets")
    fun getAll(): Flow<List<CardSetEntity>>
}
