package com.chm.flashcards.data.repository

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * Wraps [com.chm.flashcards.data.dao.CardDao]/[com.chm.flashcards.data.dao.TagDao]/
 * [com.chm.flashcards.data.dao.CardTagCrossRefDao], owning id generation via
 * [com.chm.flashcards.common.IdGenerator]. Trusts already-validated input --
 * front/back/notes/tag-name validation is the caller's (`CardEditorViewModel`'s)
 * job, per PRD §9 "validate at the boundary".
 *
 * Tag resolution on create/update: each tag name is looked up
 * case-insensitively (reusing the existing tag if found, else creating one),
 * then the card's cross-refs are fully replaced (delete all + re-insert)
 * rather than diffed added/removed tags individually -- simplest correct
 * approach per spec.md.
 */
interface CardRepository {
    fun getCardsBySetId(setId: Uuid): Flow<List<CardWithTags>>
    suspend fun getCard(id: Uuid): CardWithTags?
    suspend fun createCard(setId: Uuid, front: String, back: String, notes: String?, tagNames: List<String>): Card
    suspend fun updateCard(id: Uuid, front: String, back: String, notes: String?, tagNames: List<String>)
    suspend fun deleteCard(id: Uuid)

    /**
     * F04: cards in [setId] matching [query] (case-insensitive substring on
     * front/back/notes) and, if [tagId] is non-null, restricted to cards
     * carrying that tag. Callers decide when to use this vs.
     * [getCardsBySetId] -- an empty query with no tag filter is the plain
     * unfiltered list, so `SetDetailViewModel` delegates to
     * [getCardsBySetId] in that case rather than routing an empty search
     * through here.
     */
    fun searchCards(setId: Uuid, query: String, tagId: Uuid?): Flow<List<CardWithTags>>
}
