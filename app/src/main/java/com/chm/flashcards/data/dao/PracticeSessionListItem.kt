package com.chm.flashcards.data.dao

import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Projection for [PracticeSessionDao.getSessionListItems] -- one row per
 * practice session with its date and derived score (correct/total), counted
 * via correlated subqueries against `practice_session_results` rather than
 * loading every result row and counting in Kotlin. No domain equivalent
 * (same precedent as [CardSetWithCount]) -- passed straight through by
 * [com.chm.flashcards.data.repository.PracticeRepositoryImpl.getSessionsForSet].
 */
data class PracticeSessionListItem(
    val sessionId: Uuid,
    val startedAt: Instant,
    val correctCount: Int,
    val totalCount: Int,
)
