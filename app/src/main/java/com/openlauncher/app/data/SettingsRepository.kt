package com.openlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

private val SETTINGS_JSON = stringPreferencesKey("settings_json")

private val json = Json {
    ignoreUnknownKeys = true
    // A stored value of the wrong type or an unknown enum name falls back to the
    // property default instead of failing the whole document.
    coerceInputValues = true
    encodeDefaults = true
}

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> readSettings(prefs) }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS_JSON] = json.encodeToString(transform(readSettings(prefs)).sanitized())
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }

    private fun readSettings(prefs: Preferences): AppSettings {
        val stored = prefs[SETTINGS_JSON]
            ?: return LegacySettings.read(prefs).migrated().sanitized()
        return runCatching { json.decodeFromString<AppSettings>(stored) }
            .getOrDefault(AppSettings())
            .migrated()
            .sanitized()
    }
}

// Version 2 turned the compass cell into a map and made textScale reach every
// label. A scale picked against the old behaviour reads far too large now, so it
// goes back to the default once.
internal fun AppSettings.migrated(): AppSettings {
    if (settingsVersion >= CURRENT_SETTINGS_VERSION) return this
    return copy(
        widgetLayout = widgetLayout.map { if (it.id == "TELEMETRY") it.copy(id = "MAP") else it },
        textScale = AppSettings().textScale,
        settingsVersion = CURRENT_SETTINGS_VERSION
    )
}

private fun AppSettings.sanitized(): AppSettings {
    val layout = widgetLayout
        .filter { it.id.isNotEmpty() }
        .distinctBy { it.id }
        .map { it.clampToGrid() }
    return copy(
        // shortcuts is left alone: an empty sidebar is a choice the driver can
        // make, and the add slot brings shortcuts back.
        widgetLayout = layout.ifEmpty { defaultWidgetLayout() },
        soundboardPads = soundboardPads.ifEmpty { defaultSoundboardPads() },
        textScale = textScale.coerceIn(0.8f, 1.8f),
        uiScale = uiScale.coerceIn(0.5f, 2.0f),
        wallpaperDim = wallpaperDim.coerceIn(0f, 1f),
        mapZoom = mapZoom.coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
    )
}
