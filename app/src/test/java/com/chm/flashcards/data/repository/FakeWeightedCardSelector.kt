package com.chm.flashcards.data.repository

import com.chm.flashcards.domain.WeightedCardSelector
import kotlin.random.Random

/**
 * Deterministic [WeightedCardSelector] test double for [PracticeRepositoryImpl]
 * unit tests -- just takes the first [count] cards rather than doing any real
 * weighting, so repository tests can assert on a known, fixed selection
 * without depending on [com.chm.flashcards.domain.EfraimidisSpirakisCardSelector]'s
 * actual algorithm (covered separately by `WeightedCardSelectorTest`).
 */
class FakeWeightedCardSelector : WeightedCardSelector {
    val selectCalls = mutableListOf<Pair<List<Card>, Int>>()

    override fun select(cards: List<Card>, count: Int, random: Random): List<Card> {
        selectCalls += cards to count
        return cards.take(count)
    }
}
