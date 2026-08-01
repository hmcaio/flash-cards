package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.dao.CardWithTagsEntity
import com.chm.flashcards.data.entity.CardEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [CardDao] test double for [CardRepositoryImpl] unit tests -- no
 * Room/Android dependency, so these tests run on the plain JVM.
 *
 * [cardsWithTagsFlow] is a separately-settable table rather than something
 * derived from [insert]/[update] -- replicating Room's actual relational
 * join in a fake would just duplicate what [com.chm.flashcards.data.dao.CardDaoTest]
 * (instrumented, real Room) already verifies. Here it only needs to isolate
 * [CardRepositoryImpl]'s own mapping logic (entity -> domain [CardWithTags]).
 */
class FakeCardDao : CardDao {

    private val table = MutableStateFlow<List<CardEntity>>(emptyList())
    val cardsWithTagsFlow = MutableStateFlow<List<CardWithTagsEntity>>(emptyList())

    /** Settable result + call log for [searchCards] -- a plain delegation fake, same precedent as [cardsWithTagsFlow]. */
    val searchCardsFlow = MutableStateFlow<List<CardWithTagsEntity>>(emptyList())
    val searchCardsCalls = mutableListOf<Triple<Uuid, String, Uuid?>>()

    val inserted = mutableListOf<CardEntity>()
    val updated = mutableListOf<CardEntity>()
    val deleted = mutableListOf<CardEntity>()

    override suspend fun insert(card: CardEntity) {
        inserted += card
        table.value += card
    }

    override suspend fun update(card: CardEntity) {
        updated += card
        table.value = table.value.map { if (it.id == card.id) card else it }
    }

    override suspend fun delete(card: CardEntity) {
        deleted += card
        table.value = table.value.filterNot { it.id == card.id }
    }

    override suspend fun getById(id: Uuid): CardEntity? = table.value.find { it.id == id }

    override fun getBySetId(setId: Uuid): Flow<List<CardEntity>> =
        table.map { entities -> entities.filter { it.setId == setId } }

    override suspend fun updateStats(id: Uuid, timesCorrect: Int, timesIncorrect: Int, lastPracticedAt: Instant?) {
        table.value = table.value.map {
            if (it.id == id) {
                it.copy(timesCorrect = timesCorrect, timesIncorrect = timesIncorrect, lastPracticedAt = lastPracticedAt)
            } else {
                it
            }
        }
    }

    override fun getCardsWithTagsBySetId(setId: Uuid): Flow<List<CardWithTagsEntity>> =
        cardsWithTagsFlow.map { rows -> rows.filter { it.card.setId == setId } }

    override suspend fun getCardWithTagsById(id: Uuid): CardWithTagsEntity? =
        cardsWithTagsFlow.value.find { it.card.id == id }

    override fun searchCards(setId: Uuid, query: String, tagId: Uuid?): Flow<List<CardWithTagsEntity>> {
        searchCardsCalls += Triple(setId, query, tagId)
        return searchCardsFlow
    }
}
