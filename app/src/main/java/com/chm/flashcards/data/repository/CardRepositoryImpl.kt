package com.chm.flashcards.data.repository

import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.dao.CardTagCrossRefDao
import com.chm.flashcards.data.dao.CardWithTagsEntity
import com.chm.flashcards.data.dao.TagDao
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.TagEntity
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val tagDao: TagDao,
    private val crossRefDao: CardTagCrossRefDao,
    private val idGenerator: IdGenerator,
) : CardRepository {

    override fun getCardsBySetId(setId: Uuid): Flow<List<CardWithTags>> =
        cardDao.getCardsWithTagsBySetId(setId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCard(id: Uuid): CardWithTags? =
        cardDao.getCardWithTagsById(id)?.toDomain()

    override suspend fun createCard(
        setId: Uuid,
        front: String,
        back: String,
        notes: String?,
        tagNames: List<String>,
    ): Card {
        val entity = CardEntity(id = idGenerator.newId(), setId = setId, front = front, back = back, notes = notes)
        cardDao.insert(entity)
        replaceTags(entity.id, tagNames)
        return entity.toDomain()
    }

    override suspend fun updateCard(id: Uuid, front: String, back: String, notes: String?, tagNames: List<String>) {
        val existing = cardDao.getById(id) ?: return
        cardDao.update(existing.copy(front = front, back = back, notes = notes))
        replaceTags(id, tagNames)
    }

    override suspend fun deleteCard(id: Uuid) {
        val existing = cardDao.getById(id) ?: return
        cardDao.delete(existing)
    }

    override fun searchCards(setId: Uuid, query: String, tagId: Uuid?): Flow<List<CardWithTags>> =
        cardDao.searchCards(setId, query, tagId).map { rows -> rows.map { it.toDomain() } }

    /** Replaces all of [cardId]'s tag cross-refs with [tagNames], resolving each name to an existing or new tag id. */
    private suspend fun replaceTags(cardId: Uuid, tagNames: List<String>) {
        crossRefDao.deleteByCardId(cardId)
        tagNames.forEach { name ->
            val tagId = resolveTagId(name)
            crossRefDao.insert(CardTagCrossRef(cardId = cardId, tagId = tagId))
        }
    }

    private suspend fun resolveTagId(name: String): Uuid {
        tagDao.getByName(name)?.let { return it.id }
        val tag = TagEntity(id = idGenerator.newId(), name = name)
        tagDao.insert(tag)
        return tag.id
    }

    private fun CardEntity.toDomain() = Card(
        id = id,
        setId = setId,
        front = front,
        back = back,
        notes = notes,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
        lastPracticedAt = lastPracticedAt,
    )

    private fun CardWithTagsEntity.toDomain() = CardWithTags(
        card = card.toDomain(),
        tags = tags.map { Tag(id = it.id, name = it.name) },
    )
}
