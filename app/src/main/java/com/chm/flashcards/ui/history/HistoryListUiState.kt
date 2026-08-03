package com.chm.flashcards.ui.history

import com.chm.flashcards.data.dao.PracticeSessionListItem

/** UI state for [HistoryListScreen]: [sessions] is newest-first, per [com.chm.flashcards.data.repository.PracticeRepository.getSessionsForSet]. */
data class HistoryListUiState(
    val sessions: List<PracticeSessionListItem> = emptyList(),
    val isLoading: Boolean = true,
)
