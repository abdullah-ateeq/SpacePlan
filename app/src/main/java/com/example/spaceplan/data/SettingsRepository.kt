package com.example.spaceplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.spaceplan.domain.SpacePlanCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toAppSettings())) }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val DEFAULT_AVAILABLE = stringPreferencesKey("default_available")
        val DEFAULT_OBJECT = stringPreferencesKey("default_object")
        val DEFAULT_GAP = stringPreferencesKey("default_gap")
        val DEFAULT_COUNT = intPreferencesKey("default_count")
        val ORIENTATION = stringPreferencesKey("orientation")
        val DECIMALS = intPreferencesKey("decimal_places")
        val SHOW_FORMULA = booleanPreferencesKey("show_formula")
        val SHOW_BREAKDOWN = booleanPreferencesKey("show_breakdown")
        val SHOW_VISUALIZATION = booleanPreferencesKey("show_visualization")
        val HAPTICS = booleanPreferencesKey("haptic_feedback")
    }

    companion object {
        internal fun Preferences.toAppSettings(): AppSettings {
            val d = AppSettings()
            return AppSettings(
                themeMode = enumOrDefault(this[Keys.THEME], d.themeMode),
                dynamicColor = this[Keys.DYNAMIC_COLOR] ?: d.dynamicColor,
                defaultMode = enumOrDefault(this[Keys.DEFAULT_MODE], d.defaultMode),
                defaultUnit = enumOrDefault(this[Keys.DEFAULT_UNIT], d.defaultUnit),
                defaultAvailable = this[Keys.DEFAULT_AVAILABLE] ?: d.defaultAvailable,
                defaultObjectSize = this[Keys.DEFAULT_OBJECT] ?: d.defaultObjectSize,
                defaultGap = this[Keys.DEFAULT_GAP] ?: d.defaultGap,
                defaultObjectCount = (this[Keys.DEFAULT_COUNT] ?: d.defaultObjectCount)
                    .coerceIn(SpacePlanCalculator.MIN_OBJECT_COUNT, SpacePlanCalculator.MAX_OBJECT_COUNT),
                orientation = enumOrDefault(this[Keys.ORIENTATION], d.orientation),
                decimalPlaces = (this[Keys.DECIMALS] ?: d.decimalPlaces).coerceIn(1, 4),
                showFormula = this[Keys.SHOW_FORMULA] ?: d.showFormula,
                showBreakdown = this[Keys.SHOW_BREAKDOWN] ?: d.showBreakdown,
                showVisualization = this[Keys.SHOW_VISUALIZATION] ?: d.showVisualization,
                hapticFeedback = this[Keys.HAPTICS] ?: d.hapticFeedback,
            )
        }

        internal fun MutablePreferences.write(s: AppSettings) {
            this[Keys.THEME] = s.themeMode.name
            this[Keys.DYNAMIC_COLOR] = s.dynamicColor
            this[Keys.DEFAULT_MODE] = s.defaultMode.name
            this[Keys.DEFAULT_UNIT] = s.defaultUnit.name
            this[Keys.DEFAULT_AVAILABLE] = s.defaultAvailable
            this[Keys.DEFAULT_OBJECT] = s.defaultObjectSize
            this[Keys.DEFAULT_GAP] = s.defaultGap
            this[Keys.DEFAULT_COUNT] = s.defaultObjectCount
            this[Keys.ORIENTATION] = s.orientation.name
            this[Keys.DECIMALS] = s.decimalPlaces
            this[Keys.SHOW_FORMULA] = s.showFormula
            this[Keys.SHOW_BREAKDOWN] = s.showBreakdown
            this[Keys.SHOW_VISUALIZATION] = s.showVisualization
            this[Keys.HAPTICS] = s.hapticFeedback
        }

        private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}
