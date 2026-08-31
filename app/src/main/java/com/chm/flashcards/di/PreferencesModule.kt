package com.chm.flashcards.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.chm.flashcards.data.preferences.DataStoreViewModePreferences
import com.chm.flashcards.data.preferences.ViewModePreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREFERENCES_DATASTORE_NAME = "flashcards_preferences"

private val Context.viewModePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_DATASTORE_NAME,
)

/**
 * Provides the app's single Preferences DataStore (currently backing only
 * [ViewModePreferences], but shared across any future simple persisted
 * setting rather than one DataStore file per setting) and binds
 * [ViewModePreferences] to its real, DataStore-backed implementation. Kept
 * as its own module (rather than folded into [UtilModule]) per this
 * project's one-module-per-concern convention.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindViewModePreferences(impl: DataStoreViewModePreferences): ViewModePreferences

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.viewModePreferencesDataStore
    }
}
