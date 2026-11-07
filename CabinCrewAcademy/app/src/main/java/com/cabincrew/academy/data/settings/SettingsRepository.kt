package com.cabincrew.academy.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode(val label: String) { SYSTEM("System default"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val userName: String = "",
    /** 0 = no time limit. */
    val examDurationMinutes: Int = 30,
    val examQuestionCount: Int = 20,
    val passingScore: Int = 75,
    val dailyGoal: Int = 20,
    val lastStudySession: String? = null,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val NAME = stringPreferencesKey("user_name")
        val EXAM_DURATION = intPreferencesKey("exam_duration_minutes")
        val EXAM_COUNT = intPreferencesKey("exam_question_count")
        val PASSING = intPreferencesKey("passing_score")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val LAST_STUDY = stringPreferencesKey("last_study_session")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings() = AppSettings(
        themeMode = this[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        userName = this[Keys.NAME] ?: "",
        examDurationMinutes = this[Keys.EXAM_DURATION] ?: 30,
        examQuestionCount = this[Keys.EXAM_COUNT] ?: 20,
        passingScore = this[Keys.PASSING] ?: 75,
        dailyGoal = this[Keys.DAILY_GOAL] ?: 20,
        lastStudySession = this[Keys.LAST_STUDY],
    )

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setUserName(name: String) = context.dataStore.edit { it[Keys.NAME] = name.trim() }
    suspend fun setExamDuration(minutes: Int) =
        context.dataStore.edit { it[Keys.EXAM_DURATION] = minutes.coerceIn(0, 300) }
    suspend fun setExamQuestionCount(count: Int) =
        context.dataStore.edit { it[Keys.EXAM_COUNT] = count.coerceIn(1, 500) }
    suspend fun setPassingScore(score: Int) =
        context.dataStore.edit { it[Keys.PASSING] = score.coerceIn(1, 100) }
    suspend fun setDailyGoal(goal: Int) = context.dataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(1, 1000) }
    suspend fun setLastStudySession(encoded: String?) = context.dataStore.edit {
        if (encoded == null) it.remove(Keys.LAST_STUDY) else it[Keys.LAST_STUDY] = encoded
    }
}
