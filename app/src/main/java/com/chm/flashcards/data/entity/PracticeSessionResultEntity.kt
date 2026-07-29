package com.chm.flashcards.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid

@Entity(
    tableName = "practice_session_results",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class PracticeSessionResultEntity(
    @PrimaryKey val id: Uuid,
    val sessionId: Uuid,
    val cardId: Uuid,
    val wasCorrect: Boolean,
)
