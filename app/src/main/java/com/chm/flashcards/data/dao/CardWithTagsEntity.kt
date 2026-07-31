package com.chm.flashcards.data.dao

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.TagEntity

/**
 * Room relation projection for [CardDao.getCardsWithTagsBySetId]/
 * [CardDao.getCardWithTagsById] -- a [CardEntity] joined with its [TagEntity]
 * rows via [CardTagCrossRef]. Mapped to the domain-level
 * [com.chm.flashcards.data.repository.CardWithTags] by
 * `CardRepositoryImpl` so entities don't leak past the repository boundary
 * (unlike [CardSetWithCount], which has no domain equivalent since it's a
 * simple read-only projection).
 */
data class CardWithTagsEntity(
    @Embedded val card: CardEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CardTagCrossRef::class,
            parentColumn = "cardId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)
