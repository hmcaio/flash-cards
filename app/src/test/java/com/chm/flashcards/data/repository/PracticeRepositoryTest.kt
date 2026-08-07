package com.chm.flashcards.data.repository

import app.cash.turbine.test
import com.chm.flashcards.common.FakeIdGenerator
import com.chm.flashcards.common.FakeTimeProvider
import com.chm.flashcards.data.dao.PracticeSessionListItem
import com.chm.flashcards.data.dao.PracticeSessionResultWithCard
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity
import com.chm.flashcards.ui.cardeditor.FakeCardRepository
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PracticeRepositoryTest {

    private lateinit var fakeCardDao: FakeCardDao
    private lateinit var fakeCardRepository: FakeCardRepository
    private lateinit var fakePracticeSessionDao: FakePracticeSessionDao
    private lateinit var fakeSelector: FakeWeightedCardSelector
    private lateinit var fakeIdGenerator: FakeIdGenerator
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var repository: PracticeRepository

    private val setId = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val sessionId = Uuid.parse("00000000-0000-0000-0000-0000000000aa")

    private fun cardEntity(front: String, timesCorrect: Int = 0, timesIncorrect: Int = 0) = CardEntity(
        id = Uuid.random(),
        setId = setId,
        front = front,
        back = "Back",
        notes = null,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
    )

    /** [CardRepository.getCardsBySetId] row for [card], optionally tagged -- what [PracticeRepositoryImpl.startSession] reads from since C002. */
    private fun cardWithTags(card: CardEntity, tags: List<Tag> = emptyList()) = CardWithTags(card.toDomain(), tags)

    @Before
    fun setUp() {
        fakeCardDao = FakeCardDao()
        fakeCardRepository = FakeCardRepository()
        fakePracticeSessionDao = FakePracticeSessionDao()
        fakeSelector = FakeWeightedCardSelector()
        fakeIdGenerator = FakeIdGenerator(ids = listOf(sessionId, Uuid.parse("00000000-0000-0000-0000-0000000000bb")))
        fakeTimeProvider = FakeTimeProvider(Instant.parse("2026-01-01T00:00:00Z"))
        repository = PracticeRepositoryImpl(
            cardDao = fakeCardDao,
            cardRepository = fakeCardRepository,
            practiceSessionDao = fakePracticeSessionDao,
            weightedCardSelector = fakeSelector,
            idGenerator = fakeIdGenerator,
            timeProvider = fakeTimeProvider,
        )
    }

    @Test
    fun startSession_selectsCardsViaSelector_returnsDraftWithGeneratedId() = runTest {
        val card1 = cardEntity("Q1")
        val card2 = cardEntity("Q2")
        fakeCardRepository.cardsBySetId.value = listOf(cardWithTags(card1), cardWithTags(card2))

        val draft = repository.startSession(setId, cardCount = 1)

        assertEquals(sessionId, draft.id)
        assertEquals(setId, draft.setId)
        assertEquals(1, draft.requestedCardCount)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), draft.startedAt)
        assertEquals(1, draft.cards.size)
        assertEquals(1, fakeSelector.selectCalls.size)
        assertEquals(1, fakeSelector.selectCalls[0].second)
    }

    @Test
    fun startSession_doesNotWriteAnythingToDb() = runTest {
        fakeCardRepository.cardsBySetId.value = listOf(cardWithTags(cardEntity("Q1")))

        repository.startSession(setId, cardCount = 1)

        assertTrue(fakePracticeSessionDao.sessions.isEmpty())
        assertTrue(fakePracticeSessionDao.results.value.isEmpty())
        assertTrue(fakeCardDao.inserted.isEmpty())
    }

    // --- C002: tag filter -----------------------------------------------------

    @Test
    fun startSession_withTagIds_onlyPassesMatchingCardsToSelector() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        val composeTag = Tag(Uuid.random(), "Compose")
        val card1 = cardEntity("Q1")
        val card2 = cardEntity("Q2")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags(card1, listOf(kotlinTag)),
            cardWithTags(card2, listOf(composeTag)),
        )

        repository.startSession(setId, cardCount = 5, tagIds = setOf(kotlinTag.id))

        assertEquals(1, fakeSelector.selectCalls.size)
        val (cardsSeenBySelector, _) = fakeSelector.selectCalls.single()
        assertEquals(listOf(card1.id), cardsSeenBySelector.map { it.id })
    }

    @Test
    fun startSession_withMultipleTagIds_usesOrSemantics() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        val composeTag = Tag(Uuid.random(), "Compose")
        val otherTag = Tag(Uuid.random(), "Other")
        val card1 = cardEntity("Q1")
        val card2 = cardEntity("Q2")
        val card3 = cardEntity("Q3")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags(card1, listOf(kotlinTag)),
            cardWithTags(card2, listOf(composeTag)),
            cardWithTags(card3, listOf(otherTag)),
        )

        repository.startSession(setId, cardCount = 5, tagIds = setOf(kotlinTag.id, composeTag.id))

        val (cardsSeenBySelector, _) = fakeSelector.selectCalls.single()
        assertEquals(setOf(card1.id, card2.id), cardsSeenBySelector.map { it.id }.toSet())
    }

    @Test
    fun startSession_withEmptyTagIds_passesEveryCardToSelector_defaultBehaviorUnchanged() = runTest {
        val kotlinTag = Tag(Uuid.random(), "Kotlin")
        val card1 = cardEntity("Q1")
        val card2 = cardEntity("Q2")
        fakeCardRepository.cardsBySetId.value = listOf(
            cardWithTags(card1, listOf(kotlinTag)),
            cardWithTags(card2), // untagged
        )

        repository.startSession(setId, cardCount = 5, tagIds = emptySet())

        val (cardsSeenBySelector, _) = fakeSelector.selectCalls.single()
        assertEquals(setOf(card1.id, card2.id), cardsSeenBySelector.map { it.id }.toSet())
    }

    @Test
    fun completeSession_insertsOneSessionAndNResultRows() = runTest {
        val card1 = cardEntity("Q1")
        val card2 = cardEntity("Q2")
        fakeCardDao.insert(card1)
        fakeCardDao.insert(card2)
        val draft = PracticeSessionDraft(
            id = sessionId,
            setId = setId,
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            requestedCardCount = 2,
            cards = listOf(card1.toDomain(), card2.toDomain()),
        )
        val answers = listOf(
            CardAnswer(card1.id, wasCorrect = true),
            CardAnswer(card2.id, wasCorrect = false),
        )

        repository.completeSession(draft, answers)

        assertEquals(1, fakePracticeSessionDao.sessions.size)
        assertEquals(sessionId, fakePracticeSessionDao.sessions.single().id)
        assertEquals(2, fakePracticeSessionDao.results.value.size)
        assertEquals(1, fakePracticeSessionDao.insertSessionWithResultsCalls.size)
    }

    @Test
    fun completeSession_updatesCardStats_incrementsCorrectOrIncorrectAndSetsLastPracticedAt() = runTest {
        val card1 = cardEntity("Q1", timesCorrect = 2, timesIncorrect = 1)
        val card2 = cardEntity("Q2", timesCorrect = 0, timesIncorrect = 0)
        fakeCardDao.insert(card1)
        fakeCardDao.insert(card2)
        val draft = PracticeSessionDraft(
            id = sessionId,
            setId = setId,
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            requestedCardCount = 2,
            cards = listOf(card1.toDomain(), card2.toDomain()),
        )
        fakeTimeProvider.set(Instant.parse("2026-02-01T12:00:00Z"))

        repository.completeSession(draft, listOf(CardAnswer(card1.id, true), CardAnswer(card2.id, false)))

        val updatedCard1 = fakeCardDao.getById(card1.id)!!
        assertEquals(3, updatedCard1.timesCorrect)
        assertEquals(1, updatedCard1.timesIncorrect)
        assertEquals(Instant.parse("2026-02-01T12:00:00Z"), updatedCard1.lastPracticedAt)

        val updatedCard2 = fakeCardDao.getById(card2.id)!!
        assertEquals(0, updatedCard2.timesCorrect)
        assertEquals(1, updatedCard2.timesIncorrect)
        assertEquals(Instant.parse("2026-02-01T12:00:00Z"), updatedCard2.lastPracticedAt)
    }

    @Test
    fun completeSession_returnsSummaryWithCorrectAndIncorrectCardLists() = runTest {
        val card1 = cardEntity("Q1")
        val card2 = cardEntity("Q2")
        fakeCardDao.insert(card1)
        fakeCardDao.insert(card2)
        val draft = PracticeSessionDraft(
            id = sessionId,
            setId = setId,
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            requestedCardCount = 2,
            cards = listOf(card1.toDomain(), card2.toDomain()),
        )

        val summary = repository.completeSession(
            draft,
            listOf(CardAnswer(card1.id, true), CardAnswer(card2.id, false)),
        )

        assertEquals(sessionId, summary.sessionId)
        assertEquals(listOf(card1.id), summary.correct.map { it.id })
        assertEquals(listOf(card2.id), summary.incorrect.map { it.id })
    }

    @Test
    fun getSessionsForSet_mapsDaoProjectionToDomainListItem() = runTest {
        val item1 = PracticeSessionListItem(
            sessionId = sessionId,
            startedAt = Instant.parse("2026-02-01T00:00:00Z"),
            correctCount = 3,
            totalCount = 5,
        )
        val item2 = PracticeSessionListItem(
            sessionId = Uuid.parse("00000000-0000-0000-0000-0000000000cc"),
            startedAt = Instant.parse("2026-01-01T00:00:00Z"),
            correctCount = 1,
            totalCount = 2,
        )
        fakePracticeSessionDao.sessionListItemsBySetId[setId] = listOf(item1, item2)

        repository.getSessionsForSet(setId).test {
            assertEquals(listOf(item1, item2), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun getSessionDetail_mapsResultsWithCardsToCorrectAndIncorrectLists() = runTest {
        val correctCard = cardEntity("Q1")
        val incorrectCard = cardEntity("Q2")
        fakePracticeSessionDao.resultsWithCardsBySessionId[sessionId] = listOf(
            PracticeSessionResultWithCard(
                result = PracticeSessionResultEntity(
                    id = Uuid.random(),
                    sessionId = sessionId,
                    cardId = correctCard.id,
                    wasCorrect = true,
                ),
                card = correctCard,
            ),
            PracticeSessionResultWithCard(
                result = PracticeSessionResultEntity(
                    id = Uuid.random(),
                    sessionId = sessionId,
                    cardId = incorrectCard.id,
                    wasCorrect = false,
                ),
                card = incorrectCard,
            ),
        )

        repository.getSessionDetail(sessionId).test {
            val summary = awaitItem()
            assertEquals(sessionId, summary.sessionId)
            assertEquals(listOf(correctCard.id), summary.correct.map { it.id })
            assertEquals(listOf(incorrectCard.id), summary.incorrect.map { it.id })
            awaitComplete()
        }
    }

    private fun CardEntity.toDomain() = Card(
        id = id,
        setId = setId,
        front = front,
        back = back,
        notes = notes,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastPracticedAt = lastPracticedAt,
    )
}
