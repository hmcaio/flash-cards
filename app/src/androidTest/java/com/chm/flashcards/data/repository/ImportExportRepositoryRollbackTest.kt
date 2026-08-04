package com.chm.flashcards.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chm.flashcards.common.SystemTimeProvider
import com.chm.flashcards.common.UuidIdGenerator
import com.chm.flashcards.data.RoomTransactionRunner
import com.chm.flashcards.data.dao.BaseRoomDaoTest
import com.chm.flashcards.data.dao.CardDao
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.importexport.CardExportDto
import com.chm.flashcards.data.importexport.ImportValidator
import com.chm.flashcards.data.importexport.LibraryExportDto
import com.chm.flashcards.data.importexport.SetExportDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * F07 plan.md step 27: transactional rollback on a mid-import failure needs
 * a *real* Room DB (rollback is genuine SQLite/Room behavior, not something
 * a fully-faked DAO can meaningfully exercise) -- promoted from the unit
 * test suite to here, per plan.md. Uses a real in-memory [database]
 * (`BaseRoomDaoTest`) with a real [RoomTransactionRunner], and a
 * [ThrowingOnNthInsertCardDao] wrapper around the real [CardDao] to force a
 * failure partway through a two-set import.
 */
@RunWith(AndroidJUnit4::class)
class ImportExportRepositoryRollbackTest : BaseRoomDaoTest() {

    private fun rawCount(table: String): Int {
        database.query("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    @Test
    fun import_transactionRollsBackOnFailureMidway() = runTest {
        val idGenerator = UuidIdGenerator()
        val timeProvider = SystemTimeProvider()
        val cardSetRepository = CardSetRepositoryImpl(database.cardSetDao(), idGenerator, timeProvider)
        val throwingCardDao = ThrowingOnNthInsertCardDao(database.cardDao(), failOnCallNumber = 2)
        val cardRepository = CardRepositoryImpl(throwingCardDao, database.tagDao(), database.cardTagCrossRefDao(), idGenerator)
        val repository: ImportExportRepository = ImportExportRepositoryImpl(
            cardSetRepository = cardSetRepository,
            cardRepository = cardRepository,
            timeProvider = timeProvider,
            importValidator = ImportValidator(),
            transactionRunner = RoomTransactionRunner(database),
        )
        val library = LibraryExportDto(
            exportedAt = "2026-07-28T12:00:00Z",
            sets = listOf(
                SetExportDto(name = "Set 1", cards = listOf(CardExportDto(front = "F1", back = "B1"))),
                SetExportDto(name = "Set 2", cards = listOf(CardExportDto(front = "F2", back = "B2"))),
            ),
        )

        try {
            repository.importLibrary(library, ImportMode.AddAsNewSets)
            fail("Expected the forced exception on the 2nd card insert to propagate")
        } catch (e: IllegalStateException) {
            assertEquals("Simulated mid-import failure", e.message)
        }

        // Nothing persisted -- not "Set 1" + its card (which succeeded before the failure), and
        // not "Set 2"'s set row (inserted just before its card insert threw) either.
        assertEquals(0, rawCount("card_sets"))
        assertEquals(0, rawCount("cards"))
    }

    /** Delegates every [CardDao] call to [delegate] except [insert], which throws on the [failOnCallNumber]th call. */
    private class ThrowingOnNthInsertCardDao(
        private val delegate: CardDao,
        private val failOnCallNumber: Int,
    ) : CardDao by delegate {
        private var insertCallCount = 0

        override suspend fun insert(card: CardEntity) {
            insertCallCount++
            if (insertCallCount == failOnCallNumber) {
                throw IllegalStateException("Simulated mid-import failure")
            }
            delegate.insert(card)
        }
    }
}
