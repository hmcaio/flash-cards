package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.CardTagCrossRefDao
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.TagEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [CardTagCrossRefDao] test double for [CardRepositoryImpl] unit
 * tests -- no Room/Android dependency. [getTagsForCard] isn't exercised by
 * [CardRepositoryImpl] (it reads joined data via [FakeCardDao]'s
 * `cardsWithTagsFlow` instead), so it's a plain unused stub here.
 */
class FakeCardTagCrossRefDao : CardTagCrossRefDao {

    private val table = MutableStateFlow<List<CardTagCrossRef>>(emptyList())

    val inserted = mutableListOf<CardTagCrossRef>()
    val deletedByCardIdCalls = mutableListOf<Uuid>()

    override suspend fun insert(crossRef: CardTagCrossRef) {
        inserted += crossRef
        table.value += crossRef
    }

    override suspend fun deleteByCardId(cardId: Uuid) {
        deletedByCardIdCalls += cardId
        table.value = table.value.filterNot { it.cardId == cardId }
    }

    override fun getTagsForCard(cardId: Uuid): Flow<List<TagEntity>> = MutableStateFlow(emptyList())
}
