package com.chm.flashcards.data.repository

/**
 * A [Card] joined with the [Tag]s attached to it. Built from
 * [com.chm.flashcards.data.dao.CardWithTagsEntity] (Room's `@Relation`
 * projection over entities) by `CardRepositoryImpl` so entities don't leak
 * past the repository boundary into ViewModels/UI.
 */
data class CardWithTags(
    val card: Card,
    val tags: List<Tag>,
)
