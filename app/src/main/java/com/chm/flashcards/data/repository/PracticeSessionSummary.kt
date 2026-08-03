package com.chm.flashcards.data.repository

import kotlin.uuid.Uuid

/** Result of [PracticeRepository.completeSession] -- feeds Session Results' two lists. */
data class PracticeSessionSummary(
    val sessionId: Uuid,
    val correct: List<Card>,
    val incorrect: List<Card>,
)
