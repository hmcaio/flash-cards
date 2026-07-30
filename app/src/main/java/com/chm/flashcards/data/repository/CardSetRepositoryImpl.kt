package com.chm.flashcards.data.repository

import com.chm.flashcards.common.IdGenerator
import com.chm.flashcards.common.TimeProvider
import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.dao.CardSetWithCount
import com.chm.flashcards.data.entity.CardSetEntity
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardSetRepositoryImpl @Inject constructor(
    private val cardSetDao: CardSetDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : CardSetRepository {

    override fun getAllSets(): Flow<List<CardSet>> =
        cardSetDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getAllSetsWithCount(): Flow<List<CardSetWithCount>> =
        cardSetDao.getAllWithCardCount()

    override suspend fun createSet(name: String): CardSet {
        val entity = CardSetEntity(
            id = idGenerator.newId(),
            name = name,
            createdAt = timeProvider.now(),
        )
        cardSetDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun renameSet(id: Uuid, name: String) {
        val existing = cardSetDao.getById(id) ?: return
        cardSetDao.update(existing.copy(name = name))
    }

    override suspend fun deleteSet(id: Uuid) {
        val existing = cardSetDao.getById(id) ?: return
        cardSetDao.delete(existing)
    }

    private fun CardSetEntity.toDomain() = CardSet(id = id, name = name, createdAt = createdAt)
}
