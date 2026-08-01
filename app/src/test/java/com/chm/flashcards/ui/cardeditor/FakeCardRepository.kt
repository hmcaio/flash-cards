package com.chm.flashcards.ui.cardeditor

import com.chm.flashcards.data.repository.Card
import com.chm.flashcards.data.repository.CardRepository
import com.chm.flashcards.data.repository.CardWithTags
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [CardRepository] test double for [CardEditorViewModel]/
 * `SetDetailViewModel` unit tests. Reused across `ui.cardeditor` and
 * `ui.setdetail` test packages, same precedent as
 * [com.chm.flashcards.ui.setlist.FakeCardSetRepository].
 */
class FakeCardRepository : CardRepository {

    val cardsBySetId = MutableStateFlow<List<CardWithTags>>(emptyList())
    var getCardResult: CardWithTags? = null

    /** F04: settable result + call log for [searchCards], same precedent as [cardsBySetId]. */
    val searchResults = MutableStateFlow<List<CardWithTags>>(emptyList())
    val searchCardsCalls = mutableListOf<SearchCardsCall>()

    val createCardCalls = mutableListOf<CreateCardCall>()
    val updateCardCalls = mutableListOf<UpdateCardCall>()
    val deleteCardCalls = mutableListOf<Uuid>()

    var createCardResult: Card = Card(
        id = Uuid.parse("00000000-0000-0000-0000-0000000000bb"),
        setId = Uuid.parse("00000000-0000-0000-0000-0000000000cc"),
        front = "",
        back = "",
        notes = null,
    )

    override fun getCardsBySetId(setId: Uuid): Flow<List<CardWithTags>> = cardsBySetId

    override suspend fun getCard(id: Uuid): CardWithTags? = getCardResult

    override suspend fun createCard(
        setId: Uuid,
        front: String,
        back: String,
        notes: String?,
        tagNames: List<String>,
    ): Card {
        createCardCalls += CreateCardCall(setId, front, back, notes, tagNames)
        return createCardResult.copy(setId = setId, front = front, back = back, notes = notes)
    }

    override suspend fun updateCard(id: Uuid, front: String, back: String, notes: String?, tagNames: List<String>) {
        updateCardCalls += UpdateCardCall(id, front, back, notes, tagNames)
    }

    override suspend fun deleteCard(id: Uuid) {
        deleteCardCalls += id
    }

    override fun searchCards(setId: Uuid, query: String, tagId: Uuid?): Flow<List<CardWithTags>> {
        searchCardsCalls += SearchCardsCall(setId, query, tagId)
        return searchResults
    }

    data class SearchCardsCall(val setId: Uuid, val query: String, val tagId: Uuid?)

    data class CreateCardCall(
        val setId: Uuid,
        val front: String,
        val back: String,
        val notes: String?,
        val tagNames: List<String>,
    )

    data class UpdateCardCall(
        val id: Uuid,
        val front: String,
        val back: String,
        val notes: String?,
        val tagNames: List<String>,
    )
}
