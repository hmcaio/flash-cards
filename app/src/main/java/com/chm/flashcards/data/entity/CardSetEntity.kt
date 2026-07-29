package com.chm.flashcards.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "card_sets")
data class CardSetEntity(
    @PrimaryKey val id: Uuid,
    val name: String,
    val createdAt: Instant,
)
