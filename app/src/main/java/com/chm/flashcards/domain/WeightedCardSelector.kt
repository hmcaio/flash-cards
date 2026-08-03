package com.chm.flashcards.domain

import com.chm.flashcards.data.repository.Card
import javax.inject.Inject
import kotlin.math.ln
import kotlin.random.Random

/**
 * Weighted-without-replacement sampling for practice session card selection
 * (PRD §6). Pure -- no Room/Android dependency, so [WeightedCardSelectorTest]
 * runs on the plain JVM. `count` is assumed already clamped to `cards.size`
 * by the caller ([com.chm.flashcards.ui.sessionconfig.SessionConfigViewModel]) --
 * this function doesn't re-validate it.
 *
 * spec.md sketches this as a `fun interface` (SAM), but Kotlin doesn't allow
 * a default parameter value on a functional interface's single abstract
 * method ("Functional interface abstract method cannot have a default
 * value") -- a plain `interface` is the minimal change that keeps
 * `random: Random = Random.Default` while still exposing exactly one
 * abstract method for [EfraimidisSpirakisCardSelector] to implement.
 */
interface WeightedCardSelector {
    fun select(cards: List<Card>, count: Int, random: Random = Random.Default): List<Card>
}

/**
 * Efraimidis-Spirakis weighted reservoir sampling: each card gets a key
 * `-ln(u) / weight` for `u = random.nextDouble()`; sorting ascending and
 * taking the first `count` yields a weighted-without-replacement sample --
 * a higher weight pushes a card's key lower (more likely to sort first)
 * without needing an explicit reservoir/bucket structure, small N/set sizes
 * here making the plain sort-based approach plenty fast (PRD §6, spec.md).
 */
class EfraimidisSpirakisCardSelector @Inject constructor() : WeightedCardSelector {

    override fun select(cards: List<Card>, count: Int, random: Random): List<Card> {
        if (count >= cards.size) return cards
        return cards
            .map { card -> card to key(card, random) }
            .sortedBy { (_, key) -> key }
            .take(count)
            .map { (card, _) -> card }
    }

    private fun key(card: Card, random: Random): Double {
        val weight = weightOf(card)
        val u = random.nextDouble()
        return -ln(u) / weight
    }

    /** `weight = max(1, 1 + timesIncorrect - min(timesCorrect, timesIncorrect))` -- PRD §6/spec.md. */
    private fun weightOf(card: Card): Int =
        maxOf(1, 1 + card.timesIncorrect - minOf(card.timesCorrect, card.timesIncorrect))
}
