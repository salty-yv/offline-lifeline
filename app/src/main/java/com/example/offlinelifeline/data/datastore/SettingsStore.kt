package com.example.offlinelifeline.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "offline_lifeline_settings"
)

class SettingsStore(context: Context) {
    private val dataStore = context.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            languageTag = preferences[Keys.LanguageTag] ?: "zh-CN",
            useMockEngine = preferences[Keys.UseMockEngine] ?: true,
            debugModeEnabled = preferences[Keys.DebugModeEnabled] ?: false
        )
    }

    suspend fun setLanguageTag(languageTag: String) {
        dataStore.edit { preferences ->
            preferences[Keys.LanguageTag] = languageTag
        }
    }

    suspend fun setUseMockEngine(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.UseMockEngine] = enabled
        }
    }

    suspend fun setDebugModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DebugModeEnabled] = enabled
        }
    }

    private object Keys {
        val LanguageTag = stringPreferencesKey("language_tag")
        val UseMockEngine = booleanPreferencesKey("use_mock_engine")
        val DebugModeEnabled = booleanPreferencesKey("debug_mode_enabled")
    }
}

data class AppSettings(
    val languageTag: String = "zh-CN",
    val useMockEngine: Boolean = true,
    val debugModeEnabled: Boolean = false
)
