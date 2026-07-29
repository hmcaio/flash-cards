package com.chm.flashcards.common

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTimeProviderTest {

    @Test
    fun now_returnsInstantCloseToWallClock() {
        val provider: TimeProvider = SystemTimeProvider()

        val before = Instant.now()
        val result = provider.now()
        val after = Instant.now()

        assertTrue(!result.isBefore(before) || Duration.between(result, before).abs() < Duration.ofSeconds(1))
        assertTrue(!result.isAfter(after) || Duration.between(result, after).abs() < Duration.ofSeconds(1))
    }
}
