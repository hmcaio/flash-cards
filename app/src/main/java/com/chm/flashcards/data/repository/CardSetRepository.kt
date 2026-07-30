package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.CardSetWithCount
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * Wraps [com.chm.flashcards.data.dao.CardSetDao], owning id/timestamp
 * generation via [com.chm.flashcards.common.IdGenerator]/
 * [com.chm.flashcards.common.TimeProvider]. Trusts already-validated input --
 * name validation (blank/length) is the caller's (ViewModel's) job, per
 * PRD §9 "validate at the boundary".
 */
interface CardSetRepository {
    fun getAllSets(): Flow<List<CardSet>>

    /**
     * Same rows as [getAllSets] but joined with each set's live card count,
     * for the Set List row ("N cards") without an N+1 query per row. Not
     * part of F02's spec.md interface snippet verbatim, but required so
     * [com.chm.flashcards.ui.setlist.SetListUiState.sets] can be populated
     * without querying per-set counts separately.
     */
    fun getAllSetsWithCount(): Flow<List<CardSetWithCount>>

    suspend fun createSet(name: String): CardSet
    suspend fun renameSet(id: Uuid, name: String)
    suspend fun deleteSet(id: Uuid)
}
