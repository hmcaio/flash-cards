package com.chm.flashcards.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "practice_sessions",
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
data class PracticeSessionEntity(
    @PrimaryKey val id: Uuid,
    val setId: Uuid,
    val startedAt: Instant,
    val finishedAt: Instant,
    val requestedCardCount: Int,
)
