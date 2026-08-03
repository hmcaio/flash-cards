package com.chm.flashcards.data.repository

import kotlin.uuid.Uuid

/**
 * In-memory [PracticeRepository] test double for `SessionConfigViewModel`/
 * `SessionPlayViewModel`/`SessionResultsViewModel` unit tests. Reused across
 * `ui.sessionconfig`/`ui.sessionplay` test packages, same precedent as
 * [com.chm.flashcards.ui.cardeditor.FakeCardRepository].
 */
class FakePracticeRepository : PracticeRepository {

    var startSessionResult: PracticeSessionDraft = PracticeSessionDraft(
        id = Uuid.parse("00000000-0000-0000-0000-0000000000dd"),
        setId = Uuid.parse("00000000-0000-0000-0000-0000000000ee"),
        startedAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        requestedCardCount = 0,
        cards = emptyList(),
    )
    val startSessionCalls = mutableListOf<Pair<Uuid, Int>>()

    var completeSessionResult: PracticeSessionSummary = PracticeSessionSummary(
        sessionId = Uuid.parse("00000000-0000-0000-0000-0000000000ff"),
        correct = emptyList(),
        incorrect = emptyList(),
    )
    val completeSessionCalls = mutableListOf<Pair<PracticeSessionDraft, List<CardAnswer>>>()

    override suspend fun startSession(setId: Uuid, cardCount: Int): PracticeSessionDraft {
        startSessionCalls += setId to cardCount
        return startSessionResult
    }

    override suspend fun completeSession(draft: PracticeSessionDraft, answers: List<CardAnswer>): PracticeSessionSummary {
        completeSessionCalls += draft to answers
        return completeSessionResult
    }
}
