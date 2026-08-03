package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.PracticeSessionListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [PracticeRepository] test double for `SessionConfigViewModel`/
 * `SessionPlayViewModel`/`SessionResultsViewModel` unit tests. Reused across
 * `ui.sessionconfig`/`ui.sessionplay`/`ui.history` test packages, same
 * precedent as [com.chm.flashcards.ui.cardeditor.FakeCardRepository].
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

    /** F06: settable result for [getSessionsForSet] -- a `MutableStateFlow` so `HistoryListViewModelTest` can push new emissions mid-test. */
    val sessionsForSet = MutableStateFlow<List<PracticeSessionListItem>>(emptyList())
    val getSessionsForSetCalls = mutableListOf<Uuid>()

    /** F06: settable result for [getSessionDetail] -- a single value (not keyed by id) since a test only ever queries one sessionId at a time. */
    var sessionDetailResult: PracticeSessionSummary = PracticeSessionSummary(
        sessionId = Uuid.parse("00000000-0000-0000-0000-000000000000"),
        correct = emptyList(),
        incorrect = emptyList(),
    )
    val getSessionDetailCalls = mutableListOf<Uuid>()

    override fun getSessionsForSet(setId: Uuid): Flow<List<PracticeSessionListItem>> {
        getSessionsForSetCalls += setId
        return sessionsForSet
    }

    override fun getSessionDetail(sessionId: Uuid): Flow<PracticeSessionSummary> {
        getSessionDetailCalls += sessionId
        return flowOf(sessionDetailResult)
    }
}
