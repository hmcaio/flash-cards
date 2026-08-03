package com.chm.flashcards.data.repository

import java.time.Instant
import kotlin.uuid.Uuid

/**
 * In-memory result of [PracticeRepository.startSession] -- nothing is
 * written to the database yet (see spec.md "an abandoned session leaves no
 * trace"). `id`/`startedAt` are pre-generated here (via `IdGenerator`/
 * `TimeProvider`) so [PracticeRepository.completeSession] just persists
 * this draft verbatim rather than generating a new session id at the end.
 * `cards` is the already-weighted-selected subset (via
 * [com.chm.flashcards.domain.WeightedCardSelector]) that Session Play shows
 * one at a time.
 */
data class PracticeSessionDraft(
    val id: Uuid,
    val setId: Uuid,
    val startedAt: Instant,
    val requestedCardCount: Int,
    val cards: List<Card>,
)
