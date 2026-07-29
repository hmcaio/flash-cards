package com.chm.flashcards.common

import org.junit.Assert.assertNotEquals
import org.junit.Test

class UuidIdGeneratorTest {

    @Test
    fun newId_returnsDistinctValuesAcrossCalls() {
        val generator: IdGenerator = UuidIdGenerator()

        val first = generator.newId()
        val second = generator.newId()

        assertNotEquals(first, second)
    }
}
