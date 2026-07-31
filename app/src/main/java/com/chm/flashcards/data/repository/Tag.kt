package com.chm.flashcards.data.repository

import kotlin.uuid.Uuid

/** Domain model for a tag -- a thin 1:1 mapping over [com.chm.flashcards.data.entity.TagEntity]. */
data class Tag(
    val id: Uuid,
    val name: String,
)
