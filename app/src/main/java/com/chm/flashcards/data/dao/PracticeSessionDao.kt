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

    /**
     * F05: [insert]s the session row and [insertResults] the per-card result
     * rows in one atomic transaction (spec.md "completeSession runs in one
     * @Transaction") -- `PracticeRepositoryImpl.completeSession` then updates
     * each touched card's stats via `CardDao.updateStats` as a separate,
     * non-transactional loop afterward (same looser-consistency precedent as
     * `CardRepositoryImpl`'s tag replace, reasonable for a single-user
     * offline app).
     */
    @Transaction
    suspend fun insertSessionWithResults(session: PracticeSessionEntity, results: List<PracticeSessionResultEntity>) {
        insert(session)
        insertResults(results)
    }

    @Query("SELECT * FROM practice_sessions WHERE setId = :setId ORDER BY startedAt DESC")
    fun getSessionsBySetId(setId: Uuid): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_session_results WHERE sessionId = :sessionId")
    fun getResultsBySessionId(sessionId: Uuid): Flow<List<PracticeSessionResultEntity>>

    /**
     * F06: one row per session in [setId], newest first, with its score
     * (correct/total) derived via correlated subqueries against
     * `practice_session_results` rather than loaded-then-computed in Kotlin --
     * see spec.md.
     */
    @Query(
        """
        SELECT s.id AS sessionId, s.startedAt AS startedAt,
               (SELECT COUNT(*) FROM practice_session_results r WHERE r.sessionId = s.id AND r.wasCorrect = 1) AS correctCount,
               (SELECT COUNT(*) FROM practice_session_results r WHERE r.sessionId = s.id) AS totalCount
        FROM practice_sessions s
        WHERE s.setId = :setId
        ORDER BY s.startedAt DESC
        """,
    )
    fun getSessionListItems(setId: Uuid): Flow<List<PracticeSessionListItem>>

    /**
     * F06: every result row for [sessionId] joined with the [com.chm.flashcards.data.entity.CardEntity]
     * it refers to (`INNER JOIN cards`), for History Detail -- see
     * [PracticeSessionResultWithCard].
     */
    @Query(
        """
        SELECT r.*,
               c.id AS card_id, c.setId AS card_setId, c.front AS card_front, c.back AS card_back,
               c.notes AS card_notes, c.timesCorrect AS card_timesCorrect, c.timesIncorrect AS card_timesIncorrect,
               c.lastPracticedAt AS card_lastPracticedAt
        FROM practice_session_results r
        INNER JOIN cards c ON c.id = r.cardId
        WHERE r.sessionId = :sessionId
        """,
    )
    fun getResultsWithCardsBySessionId(sessionId: Uuid): Flow<List<PracticeSessionResultWithCard>>
}
