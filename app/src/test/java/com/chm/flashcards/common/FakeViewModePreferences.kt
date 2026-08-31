package com.chm.flashcards.common

import com.chm.flashcards.data.preferences.ViewMode
import com.chm.flashcards.data.preferences.ViewModePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [ViewModePreferences] test double, backed by a
 * [MutableStateFlow] -- same precedent as [FakeIdGenerator]/[FakeTimeProvider].
 * Records every [setViewMode] call so tests can assert the ViewModel wrote
 * through to the (fake) persisted preference, not just its own UI state.
 */
class FakeViewModePreferences(initial: ViewMode = ViewMode.GRID) : ViewModePreferences {

    private val _viewMode = MutableStateFlow(initial)
    override val viewMode: Flow<ViewMode> = _viewMode

    val setViewModeCalls = mutableListOf<ViewMode>()

    override suspend fun setViewMode(mode: ViewMode) {
        setViewModeCalls.add(mode)
        _viewMode.value = mode
    }
}
