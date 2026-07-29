package com.chm.flashcards.data

import androidx.room.TypeConverter
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Room [TypeConverter]s for the two value types used across every entity's
 * PK/FK and timestamp columns. Room never sees a raw [Uuid]/[Instant] on
 * disk -- these conversions are the only place those types touch a
 * primitive column type ([String]/[Long]).
 */
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromUuid(value: Uuid?): String? = value?.toString()

    @TypeConverter
    fun toUuid(value: String?): Uuid? = value?.let(Uuid::parse)
}
