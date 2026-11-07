package com.cabincrew.academy

import android.content.Context
import com.cabincrew.academy.data.db.AppDatabase
import com.cabincrew.academy.data.repository.StudyRepository
import com.cabincrew.academy.data.settings.SettingsRepository

/** Simple manual dependency container (no reflection-based DI needed for an offline app). */
class AppContainer(val appContext: Context) {
    private val context = appContext
    val database: AppDatabase by lazy { AppDatabase.build(context) }
    val repository: StudyRepository by lazy { StudyRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(context) }
}
