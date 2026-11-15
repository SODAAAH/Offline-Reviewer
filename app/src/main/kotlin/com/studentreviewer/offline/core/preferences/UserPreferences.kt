package com.studentreviewer.offline.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyDefaultExamDuration = intPreferencesKey("default_exam_duration")
    private val keyDefaultExamQuestionCount = intPreferencesKey("default_exam_question_count")
    private val keyAiEnabled = booleanPreferencesKey("ai_enabled")
    private val keyAiModelPath = stringPreferencesKey("ai_model_path")
    private val keyAiContextSize = intPreferencesKey("ai_context_size")
    private val keyAiThreadCount = intPreferencesKey("ai_thread_count")
    private val keyAiTemperature = floatPreferencesKey("ai_temperature")

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyThemeMode] ?: "SYSTEM"
    }

    val defaultExamDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyDefaultExamDuration] ?: 30
    }

    val defaultExamQuestionCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyDefaultExamQuestionCount] ?: 20
    }

    val aiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyAiEnabled] ?: false
    }

    val aiModelPath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[keyAiModelPath]
    }

    val aiContextSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyAiContextSize] ?: 2048
    }

    val aiThreadCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyAiThreadCount] ?: 4
    }

    val aiTemperature: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[keyAiTemperature] ?: 0.7f
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[keyThemeMode] = mode
        }
    }

    suspend fun setDefaultExamSettings(durationMinutes: Int, questionCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyDefaultExamDuration] = durationMinutes
            preferences[keyDefaultExamQuestionCount] = questionCount
        }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyAiEnabled] = enabled
        }
    }

    suspend fun setAiModelPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) {
                preferences.remove(keyAiModelPath)
            } else {
                preferences[keyAiModelPath] = path
            }
        }
    }

    suspend fun setAiParameters(contextSize: Int, threadCount: Int, temperature: Float) {
        context.dataStore.edit { preferences ->
            preferences[keyAiContextSize] = contextSize
            preferences[keyAiThreadCount] = threadCount
            preferences[keyAiTemperature] = temperature
        }
    }
}
