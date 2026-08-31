package com.chm.flashcards.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single global list-vs-grid preference shared by every screen that offers
 * the toggle (Set List, Set Detail) -- not a per-screen setting. Same
 * fakeable-dependency shape as [com.chm.flashcards.common.IdGenerator]/
 * [com.chm.flashcards.common.TimeProvider]/[com.chm.flashcards.data.TransactionRunner]:
 * an interface real ViewModels depend on, backed here by Jetpack DataStore
 * Preferences (this project's first non-Room persistence mechanism), with a
 * `FakeViewModePreferences` (`app/src/test/.../common/`) standing in for
 * unit tests. Bound in `di/PreferencesModule.kt`.
 */
interface ViewModePreferences {
    val viewMode: Flow<ViewMode>
    suspend fun setViewMode(mode: ViewMode)
}

private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")

/** Grid is the default when nothing has been stored yet (this chore's requirement). */
private val DEFAULT_VIEW_MODE = ViewMode.GRID

class DataStoreViewModePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModePreferences {

    override val viewMode: Flow<ViewMode> = dataStore.data.map { prefs ->
        prefs[VIEW_MODE_KEY]?.let { stored ->
            runCatching { ViewMode.valueOf(stored) }.getOrDefault(DEFAULT_VIEW_MODE)
        } ?: DEFAULT_VIEW_MODE
    }

    override suspend fun setViewMode(mode: ViewMode) {
        dataStore.edit { prefs -> prefs[VIEW_MODE_KEY] = mode.name }
    }
}
