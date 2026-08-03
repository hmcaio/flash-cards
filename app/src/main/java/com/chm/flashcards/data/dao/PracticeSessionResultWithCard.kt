package com.chm.flashcards.data.dao

import androidx.room.Embedded
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity

/**
 * Projection for [PracticeSessionDao.getResultsWithCardsBySessionId] -- each
 * [PracticeSessionResultEntity] joined (`INNER JOIN cards`) with the full
 * [CardEntity] it refers to, so [com.chm.flashcards.data.repository.PracticeRepositoryImpl]
 * can reuse the same entity-to-domain `Card` mapping used everywhere else
 * rather than a partial front/back-only shape. Because it's an inner join, a
 * card deleted after the session (F01 FK `cardId -> cards.id ON DELETE
 * CASCADE` also removes the result row) never shows up here -- the accepted
 * edge case from spec.md, not special-cased.
 */
data class PracticeSessionResultWithCard(
    @Embedded val result: PracticeSessionResultEntity,
    @Embedded(prefix = "card_") val card: CardEntity,
)
