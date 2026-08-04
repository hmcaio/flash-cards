package com.chm.flashcards.data.repository

/** Whole-library import strategy chosen by the user after validation succeeds -- see [ImportExportRepository.importLibrary]. */
enum class ImportMode {
    /** Deletes every existing [CardSet] (cascades cards/tags/practice history) before inserting the imported sets. */
    ReplaceAll,

    /** Inserts the imported sets as brand-new sets alongside existing data -- no deletion. */
    AddAsNewSets,
}
