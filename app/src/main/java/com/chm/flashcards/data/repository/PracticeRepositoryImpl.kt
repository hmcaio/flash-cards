package com.chm.flashcards.data.repository

import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.dao.PracticeSessionDao
import com.chm.flashcards.data.dao.PracticeSessionListItem
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.PracticeSessionEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity
import com.chm.flashcards.domain.WeightedCardSelector
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PracticeRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val cardRepository: CardRepository,
    private val practiceSessionDao: PracticeSessionDao,
    private val weightedCardSelector: WeightedCardSelector,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : PracticeRepository {

    /**
     * C002: fetches via [CardRepository.getCardsBySetId] (not raw [cardDao])
     * specifically so each card's tags are available to filter by --
     * `tagIds.isEmpty()` short-circuits to "no filter", the pre-existing
     * behavior. The selector only ever sees cards that survive this filter,
     * so a card outside the active tag selection can never be chosen.
     */
    override suspend fun startSession(setId: Uuid, cardCount: Int, tagIds: Set<Uuid>): PracticeSessionDraft {
        val eligibleCards = cardRepository.getCardsBySetId(setId).first()
            .filter { cardWithTags -> tagIds.isEmpty() || cardWithTags.tags.any { it.id in tagIds } }
            .map { it.card }
        val selected = weightedCardSelector.select(eligibleCards, cardCount)
        return PracticeSessionDraft(
            id = idGenerator.newId(),
            setId = setId,
            startedAt = timeProvider.now(),
            requestedCardCount = cardCount,
            cards = selected,
        )
    }

    override suspend fun completeSession(draft: PracticeSessionDraft, answers: List<CardAnswer>): PracticeSessionSummary {
        val finishedAt = timeProvider.now()
        val sessionEntity = PracticeSessionEntity(
            id = draft.id,
            setId = draft.setId,
            startedAt = draft.startedAt,
            finishedAt = finishedAt,
            requestedCardCount = draft.requestedCardCount,
        )
        val resultEntities = answers.map { answer ->
            PracticeSessionResultEntity(
                id = idGenerator.newId(),
                sessionId = draft.id,
                cardId = answer.cardId,
                wasCorrect = answer.wasCorrect,
            )
        }
        practiceSessionDao.insertSessionWithResults(sessionEntity, resultEntities)

        val cardsById = draft.cards.associateBy { it.id }
        answers.forEach { answer ->
            val card = cardsById[answer.cardId] ?: return@forEach
            val timesCorrect = card.timesCorrect + if (answer.wasCorrect) 1 else 0
            val timesIncorrect = card.timesIncorrect + if (answer.wasCorrect) 0 else 1
            cardDao.updateStats(
                id = card.id,
                timesCorrect = timesCorrect,
                timesIncorrect = timesIncorrect,
                lastPracticedAt = finishedAt,
            )
        }

        val correct = answers.filter { it.wasCorrect }.mapNotNull { cardsById[it.cardId] }
        val incorrect = answers.filterNot { it.wasCorrect }.mapNotNull { cardsById[it.cardId] }
        return PracticeSessionSummary(sessionId = draft.id, correct = correct, incorrect = incorrect)
    }

    override fun getSessionsForSet(setId: Uuid): Flow<List<PracticeSessionListItem>> =
        practiceSessionDao.getSessionListItems(setId)

    override fun getSessionDetail(sessionId: Uuid): Flow<PracticeSessionSummary> =
        practiceSessionDao.getResultsWithCardsBySessionId(sessionId).map { rows ->
            PracticeSessionSummary(
                sessionId = sessionId,
                correct = rows.filter { it.result.wasCorrect }.map { it.card.toDomain() },
                incorrect = rows.filterNot { it.result.wasCorrect }.map { it.card.toDomain() },
            )
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
