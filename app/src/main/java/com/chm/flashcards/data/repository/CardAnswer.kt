package com.chm.flashcards.data.repository

import kotlin.uuid.Uuid

/** One flip+answer recorded by Session Play, buffered client-side until the session completes. */
data class CardAnswer(val cardId: Uuid, val wasCorrect: Boolean)
