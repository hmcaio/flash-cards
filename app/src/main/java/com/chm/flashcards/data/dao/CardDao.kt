package com.chm.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chm.flashcards.data.entity.CardEntity
import java.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity)

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Uuid): CardEntity?

    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getBySetId(setId: Uuid): Flow<List<CardEntity>>

    @Query(
        "UPDATE cards SET timesCorrect = :timesCorrect, timesIncorrect = :timesIncorrect, " +
            "lastPracticedAt = :lastPracticedAt WHERE id = :id",
    )
    suspend fun updateStats(id: Uuid, timesCorrect: Int, timesIncorrect: Int, lastPracticedAt: Instant?)

    /**
     * F03: each card joined with its tags via [com.chm.flashcards.data.entity.CardTagCrossRef]
     * ([CardWithTagsEntity], a Room `@Relation`). `@Transaction` so the card
     * query and the per-row tag lookup run atomically.
     */
    @Transaction
    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getCardsWithTagsBySetId(setId: Uuid): Flow<List<CardWithTagsEntity>>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardWithTagsById(id: Uuid): CardWithTagsEntity?

    /**
     * F04: cards in [setId] matching [query] (case-insensitive substring on
     * front/back/notes; empty string matches everything) and, if [tagId] is
     * non-null, restricted to cards tagged with it (AND semantics when both
     * are supplied). Bound params throughout -- no string concatenation
     * (PRD §9). Returns [CardWithTagsEntity] rather than a bare [CardEntity]
     * list so the repository can reuse the exact same relation-to-domain
     * mapping as [getCardsWithTagsBySetId] instead of a second, duplicate
     * join: the `LEFT JOIN` against `card_tag_cross_ref` below is only used
     * to *filter* by tagId, while the full tag list per card still comes
     * from the `@Relation` in [CardWithTagsEntity] -- a card matching the
     * tag filter should still display all of its tags, not just the
     * matched one.
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT c.* FROM cards c
        LEFT JOIN card_tag_cross_ref x ON x.cardId = c.id
        WHERE c.setId = :setId
          AND (:query = '' OR c.front LIKE '%' || :query || '%' COLLATE NOCASE
                           OR c.back LIKE '%' || :query || '%' COLLATE NOCASE
                           OR c.notes LIKE '%' || :query || '%' COLLATE NOCASE)
          AND (:tagId IS NULL OR x.tagId = :tagId)
        """,
    )
    fun searchCards(setId: Uuid, query: String, tagId: Uuid?): Flow<List<CardWithTagsEntity>>
}
