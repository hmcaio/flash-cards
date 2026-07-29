package com.chm.flashcards.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CardSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("setId")],
)
data class CardEntity(
    @PrimaryKey val id: Uuid,
    val setId: Uuid,
    val front: String,
    val back: String,
    val notes: String?,
    val timesCorrect: Int = 0,
    val timesIncorrect: Int = 0,
    val lastPracticedAt: Instant? = null,
)
