package com.chm.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.TagEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface CardTagCrossRefDao {
    @Insert
    suspend fun insert(crossRef: CardTagCrossRef)

    @Query("DELETE FROM card_tag_cross_ref WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: Uuid)

    @Query(
        "SELECT tags.* FROM tags " +
            "INNER JOIN card_tag_cross_ref ON tags.id = card_tag_cross_ref.tagId " +
            "WHERE card_tag_cross_ref.cardId = :cardId",
    )
    fun getTagsForCard(cardId: Uuid): Flow<List<TagEntity>>
}
