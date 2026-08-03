package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.PracticeSessionListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * Owns a practice session's lifecycle end to end: [startSession] selects
 * cards (via [com.chm.flashcards.domain.WeightedCardSelector]) and builds an
 * in-memory [PracticeSessionDraft] -- nothing is written to the database
 * yet, so a session abandoned mid-play (`SessionPlayViewModel` cleared
 * before the last card is answered) leaves no trace. [completeSession]
 * writes the whole result transactionally: one `PracticeSessionEntity`, one
 * `PracticeSessionResultEntity` per answer, and an updated
 * `timesCorrect`/`timesIncorrect`/`lastPracticedAt` on each touched card.
 *
 * F06 additions: [getSessionsForSet] backs History List, [getSessionDetail]
 * backs History Detail (reusing the same [PracticeSessionSummary] shape
 * [completeSession] already returns, per spec.md).
 */
interface PracticeRepository {
    suspend fun startSession(setId: Uuid, cardCount: Int): PracticeSessionDraft
    suspend fun completeSession(draft: PracticeSessionDraft, answers: List<CardAnswer>): PracticeSessionSummary

    /** Sessions for [setId], newest first, each with its derived score -- see [PracticeSessionListItem]. */
    fun getSessionsForSet(setId: Uuid): Flow<List<PracticeSessionListItem>>

    /** One past session's full correct/incorrect card lists, for History Detail. */
    fun getSessionDetail(sessionId: Uuid): Flow<PracticeSessionSummary>
}
