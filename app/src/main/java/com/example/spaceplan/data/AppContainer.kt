package com.example.spaceplan.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsDataStore by preferencesDataStore(name = "spaceplan_settings")
private val Context.historyDataStore by preferencesDataStore(name = "spaceplan_history")

/** Simple manual dependency container held by the Application. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settingsRepository = SettingsRepository(appContext.settingsDataStore)
    val historyRepository = HistoryRepository(appContext.historyDataStore)
}
