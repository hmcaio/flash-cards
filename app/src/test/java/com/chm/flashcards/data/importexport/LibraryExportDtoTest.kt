package com.chm.flashcards.data.importexport

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM round-trip test for the F07 JSON schema DTOs (PRD §8) -- no
 * Android/Room dependency, kotlinx.serialization works fine on plain JVM.
 */
class LibraryExportDtoTest {

    @Test
    fun roundTrip_encodeThenDecode_producesEquivalentDto() {
        val original = LibraryExportDto(
            schemaVersion = 1,
            exportedAt = "2026-07-28T12:00:00Z",
            sets = listOf(
                SetExportDto(
                    name = "Kotlin Basics",
                    cards = listOf(
                        CardExportDto(
                            front = "What is a data class?",
                            back = "A class that auto-generates equals/hashCode/toString/copy",
                            notes = "Ch. 4",
                            tags = listOf("kotlin", "syntax"),
                        ),
                        CardExportDto(front = "No notes or tags", back = "Back"),
                    ),
                ),
                SetExportDto(name = "Empty set", cards = emptyList()),
            ),
        )

        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<LibraryExportDto>(json)

        assertEquals(original, decoded)
    }

    @Test
    fun cardExportDto_notesAndTags_defaultToNullAndEmpty() {
        val json = """{"front":"F","back":"B"}"""

        val decoded = Json.decodeFromString<CardExportDto>(json)

        assertEquals(CardExportDto(front = "F", back = "B", notes = null, tags = emptyList()), decoded)
    }

    @Test
    fun libraryExportDto_schemaVersion_defaultsToOne() {
        val json = """{"exportedAt":"2026-07-28T12:00:00Z","sets":[]}"""

        val decoded = Json.decodeFromString<LibraryExportDto>(json)

        assertEquals(1, decoded.schemaVersion)
    }
}
