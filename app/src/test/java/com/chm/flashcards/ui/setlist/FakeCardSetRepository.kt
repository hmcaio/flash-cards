package com.chm.flashcards.ui.setlist

import com.chm.flashcards.data.dao.CardSetWithCount
import com.chm.flashcards.data.repository.CardSet
import com.chm.flashcards.data.repository.CardSetRepository
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [CardSetRepository] test double for [SetListViewModel] unit tests. */
class FakeCardSetRepository : CardSetRepository {

    val setsWithCount = MutableStateFlow<List<CardSetWithCount>>(emptyList())
    val sets = MutableStateFlow<List<CardSet>>(emptyList())

    val createSetCalls = mutableListOf<String>()
    val renameSetCalls = mutableListOf<Pair<Uuid, String>>()
    val deleteSetCalls = mutableListOf<Uuid>()

    var createSetResult: CardSet = CardSet(
        id = Uuid.parse("00000000-0000-0000-0000-0000000000aa"),
        name = "",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    override fun getAllSets(): Flow<List<CardSet>> = sets

    override fun getAllSetsWithCount(): Flow<List<CardSetWithCount>> = setsWithCount

    override suspend fun createSet(name: String): CardSet {
        createSetCalls += name
        return createSetResult.copy(name = name)
    }

    override suspend fun renameSet(id: Uuid, name: String) {
        renameSetCalls += id to name
    }

    override suspend fun deleteSet(id: Uuid) {
        deleteSetCalls += id
    }
}
