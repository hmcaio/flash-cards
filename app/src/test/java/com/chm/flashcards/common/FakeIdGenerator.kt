package com.chm.flashcards.common

import kotlin.uuid.Uuid

/**
 * Deterministic [IdGenerator] test double. Returns ids from a fixed
 * sequence supplied at construction, cycling back to the start if
 * exhausted so tests don't have to size the sequence exactly. Defaults to
 * a single fixed id when no sequence is supplied.
 */
class FakeIdGenerator(
    private val ids: List<Uuid> = listOf(Uuid.parse("00000000-0000-0000-0000-000000000001")),
) : IdGenerator {
    private var index = 0

    override fun newId(): Uuid {
        val id = ids[index % ids.size]
        index++
        return id
    }
}
