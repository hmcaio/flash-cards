package com.chm.flashcards.data

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Wraps [androidx.room.RoomDatabase.withTransaction] behind an interface --
 * same rationale as `IdGenerator`/`TimeProvider`: `FlashCardsDatabase` is a
 * Room-generated class that can't be constructed as a lightweight JVM fake,
 * so `ImportExportRepositoryImpl` (F07) depends on this abstraction instead
 * of the database directly, letting its unit tests use a no-op
 * `FakeTransactionRunner` while production wiring gets the real, actually
 * atomic Room transaction. The one test that needs *real* rollback behavior
 * ("import_transactionRollsBackOnFailureMidway") is promoted to an
 * instrumented test against a real in-memory Room DB instead, per F07
 * spec.md/plan.md.
 */
interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(
    private val database: FlashCardsDatabase,
) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = database.withTransaction { block() }
}
