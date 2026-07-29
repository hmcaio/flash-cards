package com.chm.flashcards.common

import java.time.Instant

/**
 * Deterministic [TimeProvider] test double. Returns a fixed [Instant]
 * (mutable via [now] setter-like [set]) so tests can assert exact
 * timestamps instead of dealing with wall-clock drift.
 */
class FakeTimeProvider(
    private var instant: Instant = Instant.parse("2026-01-01T00:00:00Z"),
) : TimeProvider {
    override fun now(): Instant = instant

    fun set(newInstant: Instant) {
        instant = newInstant
    }
}
