package com.chm.flashcards.common

/**
 * Shared content-length/count limits for a card's fields, enforced both by
 * `CardEditorViewModel` (manual create/edit, F03) and
 * `com.chm.flashcards.data.importexport.ImportValidator` (bulk import, F07)
 * so the two validation paths can never silently drift apart -- see F07
 * spec.md "reuse the constants, don't duplicate magic numbers".
 */
object CardValidationRules {
    const val MAX_FRONT_BACK_LENGTH = 1000
    const val MAX_NOTES_LENGTH = 2000
    const val MAX_TAG_NAME_LENGTH = 50
    const val MAX_TAGS = 10
}
