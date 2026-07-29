package com.chm.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.chm.flashcards.data.entity.PracticeSessionEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Insert
    suspend fun insert(session: PracticeSessionEntity)

    @Transaction
    @Insert
    suspend fun insertResults(results: List<PracticeSessionResultEntity>)

    @Query("SELECT * FROM practice_sessions WHERE setId = :setId ORDER BY startedAt DESC")
    fun getSessionsBySetId(setId: Uuid): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_session_results WHERE sessionId = :sessionId")
    fun getResultsBySessionId(sessionId: Uuid): Flow<List<PracticeSessionResultEntity>>
}
