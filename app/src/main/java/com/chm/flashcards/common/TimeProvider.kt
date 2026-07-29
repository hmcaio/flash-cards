package com.chm.flashcards.common

import java.time.Instant
import javax.inject.Inject

/**
 * The only source of "current time" in the data layer. Real code always
 * goes through this interface (never [Instant.now] directly) so tests can
 * substitute a deterministic [FakeTimeProvider].
 */
interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()
}
