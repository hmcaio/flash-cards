package com.chm.flashcards.domain

import com.chm.flashcards.data.repository.Card
import kotlin.random.Random
import kotlin.uuid.Uuid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests -- zero Android/Room imports, runs on the plain JVM
 * (see plan.md §1/§7). [EfraimidisSpirakisCardSelector] is the concrete
 * implementation under test throughout.
 */
class WeightedCardSelectorTest {

    private val selector: WeightedCardSelector = EfraimidisSpirakisCardSelector()

    private fun card(
        timesCorrect: Int = 0,
        timesIncorrect: Int = 0,
        setId: Uuid = Uuid.random(),
    ) = Card(
        id = Uuid.random(),
        setId = setId,
        front = "Front",
        back = "Back",
        notes = null,
        timesCorrect = timesCorrect,
        timesIncorrect = timesIncorrect,
    )

    @Test
    fun select_countEqualsListSize_returnsAllCardsAnyOrder() {
        val cards = List(5) { card() }

        val result = selector.select(cards, count = cards.size, random = Random(1))

        assertEquals(cards.toSet(), result.toSet())
        assertEquals(cards.size, result.size)
    }

    @Test
    fun select_countGreaterThanListSize_returnsAllCards() {
        val cards = List(3) { card() }

        val result = selector.select(cards, count = 10, random = Random(1))

        assertEquals(cards.toSet(), result.toSet())
    }

    @Test
    fun select_uniformWeights_countLessThanSize_returnsDistinctSubsetOfCorrectSize() {
        val cards = List(10) { card() }

        val result = selector.select(cards, count = 4, random = Random(7))

        assertEquals(4, result.size)
        assertEquals(4, result.map { it.id }.toSet().size)
        assertTrue(result.all { it in cards })
    }

    @Test
    fun select_deterministicWithSeededRandom_sameSeedSameResult() {
        val cards = List(10) { card() }

        val first = selector.select(cards, count = 4, random = Random(42))
        val second = selector.select(cards, count = 4, random = Random(42))

        assertEquals(first, second)
    }

    @Test
    fun select_cardWithHigherIncorrectWeight_selectedMoreOftenOverManyTrials() {
        val weak = card(timesCorrect = 0, timesIncorrect = 10)
        val strong = card(timesCorrect = 10, timesIncorrect = 0)
        val cards = listOf(weak, strong)
        val random = Random(1234)

        var weakSelectedCount = 0
        val trials = 2000
        repeat(trials) {
            val result = selector.select(cards, count = 1, random = random)
            if (result.single().id == weak.id) weakSelectedCount++
        }

        val weakFrequency = weakSelectedCount.toDouble() / trials
        // weak has weight 11, strong has weight 1 -- weak should win roughly 11/12 (~92%) of
        // draws; a generous tolerance band keeps this from being flaky while still failing if
        // the weighting logic regresses to ~50/50 (uniform) or inverted.
        assertTrue("expected weak-card frequency well above 50%, was $weakFrequency", weakFrequency > 0.75)
    }

    @Test
    fun select_uniformWeights_selectsEachCardRoughlyEquallyOverManyTrials() {
        val cardA = card()
        val cardB = card()
        val cards = listOf(cardA, cardB)
        val random = Random(99)

        var aSelectedCount = 0
        val trials = 2000
        repeat(trials) {
            val result = selector.select(cards, count = 1, random = random)
            if (result.single().id == cardA.id) aSelectedCount++
        }

        val frequency = aSelectedCount.toDouble() / trials
        assertTrue("expected roughly uniform frequency, was $frequency", frequency in 0.40..0.60)
    }
}
