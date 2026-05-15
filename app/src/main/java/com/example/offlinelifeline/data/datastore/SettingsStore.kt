package com.example.offlinelifeline.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
            debugModeEnabled = preferences[Keys.DebugModeEnabled] ?: false,
            activeModelId = preferences[Keys.ActiveModelId] ?: "e2b",
            chatTextSizeSp = (preferences[Keys.ChatTextSizeSp] ?: DEFAULT_CHAT_TEXT_SIZE_SP)
                .coerceIn(MIN_CHAT_TEXT_SIZE_SP, MAX_CHAT_TEXT_SIZE_SP)
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

    /** 持久化用户选择的活跃模型 ID（如 "e2b" / "e4b"） */
    suspend fun setActiveModelId(modelId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ActiveModelId] = modelId
        }
    }

    suspend fun setChatTextSizeSp(sizeSp: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.ChatTextSizeSp] = sizeSp.coerceIn(
                MIN_CHAT_TEXT_SIZE_SP,
                MAX_CHAT_TEXT_SIZE_SP
            )
        }
    }

    private object Keys {
        val LanguageTag = stringPreferencesKey("language_tag")
        val UseMockEngine = booleanPreferencesKey("use_mock_engine")
        val DebugModeEnabled = booleanPreferencesKey("debug_mode_enabled")
        val ActiveModelId = stringPreferencesKey("active_model_id")
        val ChatTextSizeSp = intPreferencesKey("chat_text_size_sp")
    }

    private companion object {
        const val DEFAULT_CHAT_TEXT_SIZE_SP = 16
        const val MIN_CHAT_TEXT_SIZE_SP = 14
        const val MAX_CHAT_TEXT_SIZE_SP = 22
    }
}

data class AppSettings(
    val languageTag: String = "zh-CN",
    val useMockEngine: Boolean = true,
    val debugModeEnabled: Boolean = false,
    val activeModelId: String = "e2b",
    val chatTextSizeSp: Int = 16
)
