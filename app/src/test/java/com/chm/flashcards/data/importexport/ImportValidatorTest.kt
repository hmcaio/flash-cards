package com.chm.flashcards.data.importexport

import com.chm.flashcards.common.CardValidationRules
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportValidatorTest {

    private lateinit var validator: ImportValidator

    @Before
    fun setUp() {
        validator = ImportValidator()
    }

    private fun libraryJson(library: LibraryExportDto): String = Json.encodeToString(library)

    private fun validCard(front: String = "Front", back: String = "Back") =
        CardExportDto(front = front, back = back)

    @Test
    fun malformedJson_returnsMalformed() {
        val result = validator.validate("not json at all {{{")

        assertEquals(ImportValidationResult.Malformed, result)
    }

    @Test
    fun jsonMissingRequiredFields_returnsMalformed() {
        // "cards" is required on SetExportDto but omitted here.
        val result = validator.validate("""{"schemaVersion":1,"exportedAt":"now","sets":[{"name":"S"}]}""")

        assertEquals(ImportValidationResult.Malformed, result)
    }

    @Test
    fun wrongSchemaVersion_returnsUnsupportedVersion() {
        val library = LibraryExportDto(schemaVersion = 2, exportedAt = "now", sets = emptyList())

        val result = validator.validate(libraryJson(library))

        assertEquals(ImportValidationResult.UnsupportedVersion(2), result)
    }

    @Test
    fun tooManySets_returnsTooLarge() {
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = List(501) { SetExportDto(name = "Set $it", cards = emptyList()) },
        )

        val result = validator.validate(libraryJson(library))

        assertTrue(result is ImportValidationResult.TooLarge)
    }

    @Test
    fun tooManyCardsInASet_returnsTooLarge() {
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = List(10_001) { validCard() })),
        )

        val result = validator.validate(libraryJson(library))

        assertTrue(result is ImportValidationResult.TooLarge)
    }

    @Test
    fun blankFrontOrBack_returnsInvalidContent() {
        val blankFront = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = listOf(validCard(front = "   ")))),
        )
        val blankBack = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = listOf(validCard(back = "")))),
        )

        assertTrue(validator.validate(libraryJson(blankFront)) is ImportValidationResult.InvalidContent)
        assertTrue(validator.validate(libraryJson(blankBack)) is ImportValidationResult.InvalidContent)
    }

    @Test
    fun frontOrBackTooLong_returnsInvalidContent() {
        val tooLong = "x".repeat(CardValidationRules.MAX_FRONT_BACK_LENGTH + 1)
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = listOf(validCard(front = tooLong)))),
        )

        assertTrue(validator.validate(libraryJson(library)) is ImportValidationResult.InvalidContent)
    }

    @Test
    fun notesTooLong_returnsInvalidContent() {
        val tooLong = "x".repeat(CardValidationRules.MAX_NOTES_LENGTH + 1)
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = listOf(validCard().copy(notes = tooLong)))),
        )

        assertTrue(validator.validate(libraryJson(library)) is ImportValidationResult.InvalidContent)
    }

    @Test
    fun tagNameTooLong_returnsInvalidContent() {
        val tooLong = "x".repeat(CardValidationRules.MAX_TAG_NAME_LENGTH + 1)
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = listOf(validCard().copy(tags = listOf(tooLong))))),
        )

        assertTrue(validator.validate(libraryJson(library)) is ImportValidationResult.InvalidContent)
    }

    @Test
    fun tooManyTagsOnACard_returnsInvalidContent() {
        val tooManyTags = List(CardValidationRules.MAX_TAGS + 1) { "tag$it" }
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(SetExportDto(name = "Set", cards = listOf(validCard().copy(tags = tooManyTags)))),
        )

        assertTrue(validator.validate(libraryJson(library)) is ImportValidationResult.InvalidContent)
    }

    @Test
    fun validMinimalDto_returnsValid() {
        val library = LibraryExportDto(exportedAt = "now", sets = emptyList())

        val result = validator.validate(libraryJson(library))

        assertEquals(ImportValidationResult.Valid(library), result)
    }

    @Test
    fun validPopulatedDto_returnsValid() {
        val library = LibraryExportDto(
            exportedAt = "now",
            sets = listOf(
                SetExportDto(
                    name = "Kotlin Basics",
                    cards = listOf(CardExportDto(front = "F", back = "B", notes = "N", tags = listOf("kotlin"))),
                ),
            ),
        )

        val result = validator.validate(libraryJson(library))

        assertEquals(ImportValidationResult.Valid(library), result)
    }
}
