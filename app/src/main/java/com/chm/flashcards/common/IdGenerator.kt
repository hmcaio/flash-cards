package com.chm.flashcards.common

import javax.inject.Inject
import kotlin.uuid.Uuid

/**
 * The only source of new [Uuid]s in the data layer. Real code always goes
 * through this interface (never [Uuid.random] directly) so tests can
 * substitute a deterministic [FakeIdGenerator].
 */
interface IdGenerator {
    fun newId(): Uuid
}

class UuidIdGenerator @Inject constructor() : IdGenerator {
    override fun newId(): Uuid = Uuid.random()
}
