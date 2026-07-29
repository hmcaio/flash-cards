package com.chm.flashcards.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.dao.CardSetDao
import com.chm.flashcards.data.dao.CardTagCrossRefDao
import com.chm.flashcards.data.dao.PracticeSessionDao
import com.chm.flashcards.data.dao.TagDao
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.CardSetEntity
import com.chm.flashcards.data.entity.CardTagCrossRef
import com.chm.flashcards.data.entity.PracticeSessionEntity
import com.chm.flashcards.data.entity.PracticeSessionResultEntity
import com.chm.flashcards.data.entity.TagEntity

@Database(
    entities = [
        CardSetEntity::class,
        CardEntity::class,
        TagEntity::class,
        CardTagCrossRef::class,
        PracticeSessionEntity::class,
        PracticeSessionResultEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FlashCardsDatabase : RoomDatabase() {
    abstract fun cardSetDao(): CardSetDao
    abstract fun cardDao(): CardDao
    abstract fun tagDao(): TagDao
    abstract fun cardTagCrossRefDao(): CardTagCrossRefDao
    abstract fun practiceSessionDao(): PracticeSessionDao
}
