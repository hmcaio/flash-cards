package com.chm.flashcards.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Wraps [com.chm.flashcards.data.dao.TagDao] for the tag-name autocomplete
 * list. Get-or-create-by-name resolution lives in `CardRepositoryImpl` (it's
 * only ever needed alongside a card save), not here.
 */
interface TagRepository {
    fun getAllTagNames(): Flow<List<String>>
}
