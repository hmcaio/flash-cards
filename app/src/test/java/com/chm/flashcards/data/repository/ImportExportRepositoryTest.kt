package com.chm.flashcards.data.repository

import com.chm.flashcards.common.FakeIdGenerator
import com.chm.flashcards.common.FakeTimeProvider
import com.chm.flashcards.data.FakeTransactionRunner
import com.chm.flashcards.data.dao.CardWithTagsEntity
import com.chm.flashcards.data.entity.CardEntity
import com.chm.flashcards.data.entity.TagEntity
import com.chm.flashcards.data.importexport.CardExportDto
import com.chm.flashcards.data.importexport.ImportValidator
import com.chm.flashcards.data.importexport.LibraryExportDto
import com.chm.flashcards.data.importexport.SetExportDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * [ImportExportRepositoryImpl] is exercised through the real
 * [CardSetRepositoryImpl]/[CardRepositoryImpl] (backed by fake DAOs) rather
 * than raw fakes of its own, per F07 spec.md/plan.md -- this is what "reuse
 * [CardRepository.createCard]'s tag get-or-create logic, don't reimplement
 * it" means in practice: import literally calls through the same repository
 * methods manual card creation uses. [FakeTransactionRunner] is a no-op here
 * -- real rollback-on-failure is covered by the instrumented
 * `ImportExportRepositoryRollbackTest` against a real Room DB.
 */
class ImportExportRepositoryTest {

    private lateinit var fakeCardSetDao: FakeCardSetDao
    private lateinit var fakeCardDao: FakeCardDao
    private lateinit var fakeTagDao: FakeTagDao
    private lateinit var fakeCrossRefDao: FakeCardTagCrossRefDao
    private lateinit var fakeIdGenerator: FakeIdGenerator
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var cardSetRepository: CardSetRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var repository: ImportExportRepository

    private val fixedInstant: Instant = Instant.parse("2026-07-28T12:00:00Z")

    private val idSequence = listOf(
        Uuid.parse("00000000-0000-0000-0000-000000000001"),
        Uuid.parse("00000000-0000-0000-0000-000000000002"),
        Uuid.parse("00000000-0000-0000-0000-000000000003"),
        Uuid.parse("00000000-0000-0000-0000-000000000004"),
        Uuid.parse("00000000-0000-0000-0000-000000000005"),
        Uuid.parse("00000000-0000-0000-0000-000000000006"),
    )

    @Before
    fun setUp() {
        fakeCardSetDao = FakeCardSetDao()
        fakeCardDao = FakeCardDao()
        fakeTagDao = FakeTagDao()
        fakeCrossRefDao = FakeCardTagCrossRefDao()
        fakeIdGenerator = FakeIdGenerator(idSequence)
        fakeTimeProvider = FakeTimeProvider(fixedInstant)
        cardSetRepository = CardSetRepositoryImpl(fakeCardSetDao, fakeIdGenerator, fakeTimeProvider)
        cardRepository = CardRepositoryImpl(fakeCardDao, fakeTagDao, fakeCrossRefDao, fakeIdGenerator)
        repository = ImportExportRepositoryImpl(
            cardSetRepository = cardSetRepository,
            cardRepository = cardRepository,
            timeProvider = fakeTimeProvider,
            importValidator = ImportValidator(),
            transactionRunner = FakeTransactionRunner(),
        )
    }

    @Test
    fun exportLibrary_emptyDb_returnsSchemaWithEmptySetsList() = runTest {
        val json = repository.exportLibrary()

        val decoded = Json.decodeFromString<LibraryExportDto>(json)
        assertEquals(LibraryExportDto(exportedAt = fixedInstant.toString(), sets = emptyList()), decoded)
    }

    @Test
    fun exportLibrary_mapsSetsCardsAndTagsFromDb() = runTest {
        val set = cardSetRepository.createSet("Kotlin Basics")
        // FakeCardDao.cardsWithTagsFlow is a separately-settable projection, not derived from
        // insert() (see its own doc comment) -- seed it directly rather than going through
        // cardRepository.createCard, same precedent as CardRepositoryTest.
        val cardEntity = CardEntity(id = Uuid.random(), setId = set.id, front = "Front", back = "Back", notes = "Notes")
        val tags = listOf(TagEntity(id = Uuid.random(), name = "Kotlin"), TagEntity(id = Uuid.random(), name = "Compose"))
        fakeCardDao.cardsWithTagsFlow.value = listOf(CardWithTagsEntity(card = cardEntity, tags = tags))

        val json = repository.exportLibrary()

        val decoded = Json.decodeFromString<LibraryExportDto>(json)
        assertEquals(1, decoded.sets.size)
        assertEquals("Kotlin Basics", decoded.sets[0].name)
        assertEquals(1, decoded.sets[0].cards.size)
        val card = decoded.sets[0].cards[0]
        assertEquals("Front", card.front)
        assertEquals("Back", card.back)
        assertEquals("Notes", card.notes)
        assertEquals(setOf("Kotlin", "Compose"), card.tags.toSet())
    }

    @Test
    fun importReplaceAll_deletesExistingSetsBeforeInserting() = runTest {
        cardSetRepository.createSet("Existing")
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Imported", cards = emptyList())),
        )

        repository.importLibrary(library, ImportMode.ReplaceAll)

        val setNames = cardSetRepository.getAllSets().first().map { it.name }
        assertEquals(listOf("Imported"), setNames)
    }

    @Test
    fun importAddAsNewSets_doesNotDeleteExistingData() = runTest {
        cardSetRepository.createSet("Existing")
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Imported", cards = emptyList())),
        )

        repository.importLibrary(library, ImportMode.AddAsNewSets)

        val setNames = cardSetRepository.getAllSets().first().map { it.name }.toSet()
        assertEquals(setOf("Existing", "Imported"), setNames)
    }

    @Test
    fun import_generatesFreshUuidsForAllRows_neverReusesFileIds() = runTest {
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(
                SetExportDto(name = "Imported", cards = listOf(CardExportDto(front = "F", back = "B"))),
            ),
        )

        repository.importLibrary(library, ImportMode.AddAsNewSets)

        val insertedSet = fakeCardSetDao.inserted.single()
        val insertedCard = fakeCardDao.inserted.single()
        assertEquals(idSequence[0], insertedSet.id)
        assertEquals(idSequence[1], insertedCard.id)
        assertTrue(insertedSet.id in idSequence)
        assertTrue(insertedCard.id in idSequence)
    }

    @Test
    fun import_reusesExistingTagsByName_sameAsManualCardCreation() = runTest {
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(
                SetExportDto(
                    name = "Imported",
                    cards = listOf(
                        CardExportDto(front = "F1", back = "B1", tags = listOf("Kotlin")),
                        CardExportDto(front = "F2", back = "B2", tags = listOf("kotlin")),
                    ),
                ),
            ),
        )

        repository.importLibrary(library, ImportMode.AddAsNewSets)

        assertEquals(1, fakeTagDao.inserted.size)
        assertEquals(2, fakeCrossRefDao.inserted.size)
        assertEquals(setOf(fakeTagDao.inserted.single().id), fakeCrossRefDao.inserted.map { it.tagId }.toSet())
    }
}
