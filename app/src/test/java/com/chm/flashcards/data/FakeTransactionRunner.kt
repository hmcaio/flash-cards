package com.chm.flashcards.data

/**
 * No-op [TransactionRunner] test double -- just runs [block] directly, no
 * real atomicity. Fine for [com.chm.flashcards.data.repository.ImportExportRepositoryTest]'s
 * fake-DAO-backed tests, which only assert *what* gets written, not
 * rollback-on-failure -- that's covered by the promoted, real-Room-DB
 * `ImportExportRepositoryRollbackTest` (instrumented) instead.
 */
class FakeTransactionRunner : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
}
