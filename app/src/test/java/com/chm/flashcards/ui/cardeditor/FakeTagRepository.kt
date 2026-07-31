package com.chm.flashcards.ui.cardeditor

import com.chm.flashcards.data.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [TagRepository] test double for [CardEditorViewModel] unit tests. */
class FakeTagRepository : TagRepository {

    val allTagNames = MutableStateFlow<List<String>>(emptyList())

    override fun getAllTagNames(): Flow<List<String>> = allTagNames
}
