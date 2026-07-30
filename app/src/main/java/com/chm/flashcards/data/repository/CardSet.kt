package com.chm.flashcards.data.repository

import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Domain model for a card set -- a thin 1:1 mapping over
 * [com.chm.flashcards.data.entity.CardSetEntity]. No separate UI model yet;
 * introduce one only if a future feature needs UI-only fields.
 */
data class CardSet(
    val id: Uuid,
    val name: String,
    val createdAt: Instant,
)
