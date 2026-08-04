package com.chm.flashcards.data.importexport

/**
 * Outcome of [ImportValidator.validate], checked in the fixed order laid out
 * in F07 spec.md so the first failure encountered is always the one
 * reported (e.g. a malformed file is never also reported as "too large").
 */
sealed class ImportValidationResult {
    /** [library] is safe to hand to `ImportExportRepository.importLibrary` as-is. */
    data class Valid(val library: LibraryExportDto) : ImportValidationResult()

    /** The input wasn't valid JSON, or didn't match [LibraryExportDto]'s shape at all. */
    data object Malformed : ImportValidationResult()

    /** Parsed fine, but `schemaVersion` isn't one this app version knows how to import. */
    data class UnsupportedVersion(val version: Int) : ImportValidationResult()

    /** Parsed fine, but exceeds a sane size cap (guards against pathological files). */
    data class TooLarge(val reason: String) : ImportValidationResult()

    /** Parsed fine and within size caps, but a field violates content rules (blank/length/tag-count). */
    data class InvalidContent(val details: String) : ImportValidationResult()
}
