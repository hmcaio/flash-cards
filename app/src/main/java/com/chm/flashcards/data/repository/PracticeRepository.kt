package com.chm.flashcards.data.repository

import kotlin.uuid.Uuid

/**
 * Owns a practice session's lifecycle end to end: [startSession] selects
 * cards (via [com.chm.flashcards.domain.WeightedCardSelector]) and builds an
 * in-memory [PracticeSessionDraft] -- nothing is written to the database
 * yet, so a session abandoned mid-play (`SessionPlayViewModel` cleared
 * before the last card is answered) leaves no trace. [completeSession]
 * writes the whole result transactionally: one `PracticeSessionEntity`, one
 * `PracticeSessionResultEntity` per answer, and an updated
 * `timesCorrect`/`timesIncorrect`/`lastPracticedAt` on each touched card.
 */
interface PracticeRepository {
    suspend fun startSession(setId: Uuid, cardCount: Int): PracticeSessionDraft
    suspend fun completeSession(draft: PracticeSessionDraft, answers: List<CardAnswer>): PracticeSessionSummary
}
