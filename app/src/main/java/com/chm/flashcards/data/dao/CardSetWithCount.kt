package com.chm.flashcards.data.dao

import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Projection for [CardSetDao.getAllWithCardCount] -- a [com.chm.flashcards.data.entity.CardSetEntity]
 * plus its live card count (via `LEFT JOIN` + `COUNT`), so the Set List row can show
 * "N cards" without an N+1 query per set.
 */
data class CardSetWithCount(
    val id: Uuid,
    val name: String,
    val createdAt: Instant,
    val cardCount: Int,
)
