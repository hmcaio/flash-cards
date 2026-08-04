package com.chm.flashcards.data.repository

import com.chm.flashcards.data.importexport.ImportValidationResult
import com.chm.flashcards.data.importexport.LibraryExportDto

/**
 * Whole-library JSON import/export (F07, PRD §8/§9). Wraps
 * [com.chm.flashcards.data.importexport.ImportValidator] for [validateImport]
 * and delegates the actual DB reads/writes to [CardSetRepository]/
 * [CardRepository] so set creation, card creation, and tag get-or-create
 * resolution all go through the exact same code paths as manual entry
 * (F02/F03) -- see `ImportExportRepositoryImpl`.
 */
interface ImportExportRepository {
    /** Serializes every set/card/tag in the library to the PRD §8 JSON schema. Practice stats/history are never included. */
    suspend fun exportLibrary(): String

    /** Parses and validates [json] against the schema -- always safe to call, never touches the DB. */
    fun validateImport(json: String): ImportValidationResult

    /**
     * Writes an already-[validateImport]-ed [parsed] library into the DB
     * inside a single transaction (all-or-nothing -- a mid-import failure
     * rolls back entirely). Every inserted row gets a fresh id; the DTO
     * itself carries none.
     */
    suspend fun importLibrary(parsed: LibraryExportDto, mode: ImportMode)
}
