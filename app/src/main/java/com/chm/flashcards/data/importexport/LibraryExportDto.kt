package com.chm.flashcards.data.importexport

import kotlinx.serialization.Serializable

/**
 * Whole-library JSON export format (F07, PRD §8). Deliberately has no id
 * fields anywhere -- ids are a persistence-layer concern (Room `Uuid` PK/FK),
 * not part of the portable file format. Importing always mints fresh ids via
 * `IdGenerator` (see `ImportExportRepositoryImpl.importLibrary`), so a
 * re-imported file never collides with existing rows. Practice stats/history
 * are intentionally excluded (PRD §8) -- imported cards always start at zero
 * stats.
 */
@Serializable
data class LibraryExportDto(
    val schemaVersion: Int = 1,
    val exportedAt: String,
    val sets: List<SetExportDto>,
)

@Serializable
data class SetExportDto(
    val name: String,
    val cards: List<CardExportDto>,
)

@Serializable
data class CardExportDto(
    val front: String,
    val back: String,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
)
