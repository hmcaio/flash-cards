package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.PracticeSessionDao
import com.chm.flashcards.data.dao.PracticeSessionListItem
import com.chm.flashcards.data.dao.PracticeSessionResultWithCard
import com.chm.flashcards.data.entity.PracticeSessionEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * In-memory [PracticeSessionDao] test double for [PracticeRepositoryImpl]
 * unit tests -- no Room/Android dependency, same precedent as
 * [com.chm.flashcards.data.dao.FakeCardDao] (F03).
 *
 * [sessionListItemsBySetId]/[resultsWithCardsBySessionId] are separately
 * settable tables rather than derived from [sessions]/[results] --
 * replicating Room's actual join/subquery in a fake would just duplicate
 * what [com.chm.flashcards.data.dao.PracticeSessionDaoTest] (instrumented,
 * real Room) already verifies. Here they only need to isolate
 * [PracticeRepositoryImpl]'s own mapping logic, same precedent as
 * [FakeCardDao.cardsWithTagsFlow].
 */
class FakePracticeSessionDao : PracticeSessionDao {

    val sessions = mutableListOf<PracticeSessionEntity>()
    val results = MutableStateFlow<List<PracticeSessionResultEntity>>(emptyList())

    val sessionListItemsBySetId = mutableMapOf<Uuid, List<PracticeSessionListItem>>()
    val resultsWithCardsBySessionId = mutableMapOf<Uuid, List<PracticeSessionResultWithCard>>()

    /** Records each call as a pair so tests can assert exactly one session + N results were inserted together. */
    val insertSessionWithResultsCalls = mutableListOf<Pair<PracticeSessionEntity, List<PracticeSessionResultEntity>>>()

    override suspend fun insert(session: PracticeSessionEntity) {
        sessions += session
    }

    override suspend fun insertResults(results: List<PracticeSessionResultEntity>) {
        this.results.value = this.results.value + results
    }

    override suspend fun insertSessionWithResults(session: PracticeSessionEntity, results: List<PracticeSessionResultEntity>) {
        insertSessionWithResultsCalls += session to results
        insert(session)
        insertResults(results)
    }

    override fun getSessionsBySetId(setId: Uuid) = flowOf(sessions.filter { it.setId == setId })

    override fun getResultsBySessionId(sessionId: Uuid) =
        results.map { list -> list.filter { it.sessionId == sessionId } }

    override fun getSessionListItems(setId: Uuid) =
        flowOf(sessionListItemsBySetId[setId].orEmpty())

    override fun getResultsWithCardsBySessionId(sessionId: Uuid) =
        flowOf(resultsWithCardsBySessionId[sessionId].orEmpty())
}
