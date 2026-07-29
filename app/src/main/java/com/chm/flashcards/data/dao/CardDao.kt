package com.chm.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chm.flashcards.data.entity.CardEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity)

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Uuid): CardEntity?

    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getBySetId(setId: Uuid): Flow<List<CardEntity>>

    @Query(
        "UPDATE cards SET timesCorrect = :timesCorrect, timesIncorrect = :timesIncorrect, " +
            "lastPracticedAt = :lastPracticedAt WHERE id = :id",
    )
    suspend fun updateStats(id: Uuid, timesCorrect: Int, timesIncorrect: Int, lastPracticedAt: Instant?)
}
