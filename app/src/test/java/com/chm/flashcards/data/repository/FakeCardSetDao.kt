package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.dao.CardSetWithCount
import com.chm.flashcards.data.entity.CardSetEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [CardSetDao] test double for [CardSetRepositoryImpl] unit tests
 * -- no Room/Android dependency, so these tests run on the plain JVM. Backed
 * by a [MutableStateFlow] of the current table contents so [getAll]/
 * [getAllWithCardCount] behave like Room's observable queries (re-emit on
 * every mutation).
 */
class FakeCardSetDao : CardSetDao {

    private val table = MutableStateFlow<List<CardSetEntity>>(emptyList())

    val inserted = mutableListOf<CardSetEntity>()
    val updated = mutableListOf<CardSetEntity>()
    val deleted = mutableListOf<CardSetEntity>()

    override suspend fun insert(cardSet: CardSetEntity) {
        inserted += cardSet
        table.value = table.value + cardSet
    }

    override suspend fun update(cardSet: CardSetEntity) {
        updated += cardSet
        table.value = table.value.map { if (it.id == cardSet.id) cardSet else it }
    }

    override suspend fun delete(cardSet: CardSetEntity) {
        deleted += cardSet
        table.value = table.value.filterNot { it.id == cardSet.id }
    }

    override suspend fun getById(id: Uuid): CardSetEntity? = table.value.find { it.id == id }

    override fun getAll() = table

    override fun getAllWithCardCount() = table.map { entities ->
        entities.map { CardSetWithCount(id = it.id, name = it.name, createdAt = it.createdAt, cardCount = 0) }
    }
}
