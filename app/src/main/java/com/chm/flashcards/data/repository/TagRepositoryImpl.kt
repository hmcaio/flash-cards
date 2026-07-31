package com.chm.flashcards.data.repository

import com.chm.flashcards.data.dao.TagDao
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override fun getAllTagNames(): Flow<List<String>> =
        tagDao.getAll().map { tags -> tags.map { it.name }.distinct().sorted() }
}
