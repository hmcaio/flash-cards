package com.chm.flashcards.data.repository

import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Domain model for a card -- a thin 1:1 mapping over
 * [com.chm.flashcards.data.entity.CardEntity], same convention as [CardSet].
 * A card's tags aren't part of this type (not every read path needs them,
 * e.g. stats updates) -- see [CardWithTags] for the joined shape.
 */
data class Card(
    val id: Uuid,
    val setId: Uuid,
    val front: String,
    val back: String,
    val notes: String?,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val lastPracticedAt: Instant? = null,
)
