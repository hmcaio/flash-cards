package com.chm.flashcards.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardSetEntity
import com.chm.flashcards.data.entity.PracticeSessionEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PracticeSessionDaoTest : BaseRoomDaoTest() {

    private lateinit var cardSetDao: CardSetDao
    private lateinit var cardDao: CardDao
    private lateinit var practiceSessionDao: PracticeSessionDao

    @Before
    fun setUpDao() {
        cardSetDao = database.cardSetDao()
        cardDao = database.cardDao()
        practiceSessionDao = database.practiceSessionDao()
    }

    private suspend fun insertSetAndCard(): Pair<CardSetEntity, CardEntity> {
        val set = CardSetEntity(id = Uuid.random(), name = "Set", createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        cardSetDao.insert(set)
        val cardEntity = CardEntity(id = Uuid.random(), setId = set.id, front = "F", back = "B", notes = null)
        cardDao.insert(cardEntity)
        return set to cardEntity
    }

    private fun session(setId: Uuid, startedAt: Instant) = PracticeSessionEntity(
        id = Uuid.random(),
        setId = setId,
        startedAt = startedAt,
        finishedAt = startedAt.plusSeconds(60),
        requestedCardCount = 1,
    )

    @Test
    fun insertSessionWithResults_getResultsBySessionId_returnsAll() = runTest {
        val (set, cardEntity) = insertSetAndCard()
        val practiceSession = session(set.id, Instant.parse("2026-02-01T00:00:00Z"))
        practiceSessionDao.insert(practiceSession)
        val result = PracticeSessionResultEntity(
            id = Uuid.random(),
            sessionId = practiceSession.id,
            cardId = cardEntity.id,
            wasCorrect = true,
        )

        practiceSessionDao.insertResults(listOf(result))

        practiceSessionDao.getResultsBySessionId(practiceSession.id).test {
            assertEquals(listOf(result), awaitItem())
        }
    }

    @Test
    fun deletingSession_cascadesDeleteOfResults() = runTest {
        val (set, cardEntity) = insertSetAndCard()
        val practiceSession = session(set.id, Instant.parse("2026-02-01T00:00:00Z"))
        practiceSessionDao.insert(practiceSession)
        val result = PracticeSessionResultEntity(
            id = Uuid.random(),
            sessionId = practiceSession.id,
            cardId = cardEntity.id,
            wasCorrect = true,
        )
        practiceSessionDao.insertResults(listOf(result))

        cardSetDao.delete(set)

        practiceSessionDao.getResultsBySessionId(practiceSession.id).test {
            assertEquals(emptyList<PracticeSessionResultEntity>(), awaitItem())
        }
    }

    @Test
    fun getSessionsBySetId_ordersByStartedAtDescending() = runTest {
        val (set, _) = insertSetAndCard()
        val earlier = session(set.id, Instant.parse("2026-01-10T00:00:00Z"))
        val later = session(set.id, Instant.parse("2026-01-20T00:00:00Z"))
        practiceSessionDao.insert(earlier)
        practiceSessionDao.insert(later)

        practiceSessionDao.getSessionsBySetId(set.id).test {
            assertEquals(listOf(later, earlier), awaitItem())
        }
    }

    /** F05: [PracticeSessionDao.insertSessionWithResults] writes both the session and its result rows in one call. */
    @Test
    fun insertSessionWithResults_persistsBothSessionAndResultRows() = runTest {
        val (set, cardEntity) = insertSetAndCard()
        val practiceSession = session(set.id, Instant.parse("2026-02-01T00:00:00Z"))
        val result = PracticeSessionResultEntity(
            id = Uuid.random(),
            sessionId = practiceSession.id,
            cardId = cardEntity.id,
            wasCorrect = true,
        )

        practiceSessionDao.insertSessionWithResults(practiceSession, listOf(result))

        practiceSessionDao.getSessionsBySetId(set.id).test {
            assertEquals(listOf(practiceSession), awaitItem())
        }
        practiceSessionDao.getResultsBySessionId(practiceSession.id).test {
            assertEquals(listOf(result), awaitItem())
        }
    }

    /** F06: newest session first, score derived from correct/total result counts, not just the requested card count. */
    @Test
    fun getSessionListItems_returnsScoreAndOrdersByStartedAtDescending() = runTest {
        val (set, cardEntity) = insertSetAndCard()
        val earlier = session(set.id, Instant.parse("2026-01-10T00:00:00Z")).copy(requestedCardCount = 5)
        val later = session(set.id, Instant.parse("2026-01-20T00:00:00Z")).copy(requestedCardCount = 2)
        practiceSessionDao.insertSessionWithResults(
            earlier,
            List(5) { index ->
                PracticeSessionResultEntity(
                    id = Uuid.random(),
                    sessionId = earlier.id,
                    cardId = cardEntity.id,
                    wasCorrect = index < 3,
                )
            },
        )
        practiceSessionDao.insertSessionWithResults(
            later,
            List(2) { index ->
                PracticeSessionResultEntity(
                    id = Uuid.random(),
                    sessionId = later.id,
                    cardId = cardEntity.id,
                    wasCorrect = index < 1,
                )
            },
        )

        practiceSessionDao.getSessionListItems(set.id).test {
            val items = awaitItem()
            assertEquals(
                listOf(
                    PracticeSessionListItem(later.id, later.startedAt, correctCount = 1, totalCount = 2),
                    PracticeSessionListItem(earlier.id, earlier.startedAt, correctCount = 3, totalCount = 5),
                ),
                items,
            )
        }
    }

    /** F06: each result row joined with its card's front/back (and the rest of [CardEntity]). */
    @Test
    fun getResultsWithCardsBySessionId_returnsResultsJoinedWithCardFrontBack() = runTest {
        val (set, cardEntity) = insertSetAndCard()
        val practiceSession = session(set.id, Instant.parse("2026-02-01T00:00:00Z"))
        val result = PracticeSessionResultEntity(
            id = Uuid.random(),
            sessionId = practiceSession.id,
            cardId = cardEntity.id,
            wasCorrect = true,
        )
        practiceSessionDao.insertSessionWithResults(practiceSession, listOf(result))

        practiceSessionDao.getResultsWithCardsBySessionId(practiceSession.id).test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals(result, rows.single().result)
            assertEquals(cardEntity, rows.single().card)
        }
    }

    /**
     * F06 / spec.md accepted edge case: deleting a card cascades to delete its
     * [PracticeSessionResultEntity] row (F01 FK), so a past session's score can
     * shrink. This locks in that behavior as a regression test rather than
     * driving new production code -- it should already pass from F01's FK.
     */
    @Test
    fun getResultsWithCardsBySessionId_deletedCard_excludesThatResult() = runTest {
        val (set, cardEntity) = insertSetAndCard()
        val practiceSession = session(set.id, Instant.parse("2026-02-01T00:00:00Z"))
        val result = PracticeSessionResultEntity(
            id = Uuid.random(),
            sessionId = practiceSession.id,
            cardId = cardEntity.id,
            wasCorrect = true,
        )
        practiceSessionDao.insertSessionWithResults(practiceSession, listOf(result))

        cardDao.delete(cardEntity)

        practiceSessionDao.getResultsWithCardsBySessionId(practiceSession.id).test {
            assertEquals(emptyList<PracticeSessionResultWithCard>(), awaitItem())
        }
    }
}
