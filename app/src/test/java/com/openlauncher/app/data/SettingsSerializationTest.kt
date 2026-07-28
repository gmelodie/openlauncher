package com.openlauncher.app.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun `round trip keeps every field`() {
        val original = AppSettings(
            vehicleName = "HB20",
            compassOffset = -12.5f,
            trip = TripState(running = true, distanceMeters = 1234.5, bestAccelSeconds = 9.87f),
            radioPresets = RadioPresets(fm = listOf(1f, 2f, 3f, 4f, 5f, 6f)),
            levelReference = LevelReference(0.1f, 9.7f, 0.3f)
        )
        val decoded = json.decodeFromString<AppSettings>(json.encodeToString(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `unknown enum name falls back to the default`() {
        val decoded = json.decodeFromString<AppSettings>("""{"dayNightMode":"TWILIGHT"}""")
        assertEquals(AppSettings().dayNightMode, decoded.dayNightMode)
    }

    @Test
    fun `null in a non-null field falls back to the default`() {
        val decoded = json.decodeFromString<AppSettings>("""{"vehicleName":null,"textScale":null}""")
        assertEquals(AppSettings().vehicleName, decoded.vehicleName)
        assertEquals(AppSettings().textScale, decoded.textScale, 0.001f)
    }

    @Test
    fun `missing fields use the declared defaults`() {
        val decoded = json.decodeFromString<AppSettings>("""{"vehicleName":"Uno"}""")
        assertEquals("Uno", decoded.vehicleName)
        assertEquals(AppSettings().shortcuts, decoded.shortcuts)
        assertEquals(TripState(), decoded.trip)
    }

    @Test
    fun `a stored widget outside the grid is clamped`() {
        val clamped = WidgetConfig(id = "CLOCK", gridX = 9, gridY = 7, spanX = 5, spanY = 4).clampToGrid()
        assertTrue(clamped.gridX in 0 until GRID_COLS)
        assertTrue(clamped.gridY in 0 until GRID_ROWS)
        assertTrue(clamped.gridX + clamped.spanX <= GRID_COLS)
        assertTrue(clamped.gridY + clamped.spanY <= GRID_ROWS)
    }

    @Test
    fun `a negative span is clamped to one cell`() {
        val clamped = WidgetConfig(id = "CLOCK", gridX = 0, gridY = 0, spanX = -3, spanY = 0).clampToGrid()
        assertEquals(1, clamped.spanX)
        assertEquals(1, clamped.spanY)
    }

    @Test
    fun `legacy preferences migrate into the document`() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("vehicle_name") to "Fusca",
            booleanPreferencesKey("font_bold") to true,
            floatPreferencesKey("compass_offset") to 15f,
            stringPreferencesKey("day_night_mode") to "DARK",
            stringPreferencesKey("soundboard_pads_json") to
                """[{"label":"horn","audioUri":"","synthType":"boom"}]""",
            stringPreferencesKey("widget_layout_json") to
                """[{"id":"CLOCK","gridX":2,"gridY":0,"spanX":1,"spanY":1,"enabled":true}]"""
        )
        val migrated = LegacySettings.read(prefs)

        assertEquals("Fusca", migrated.vehicleName)
        assertTrue(migrated.fontBold)
        assertEquals(15f, migrated.compassOffset, 0.001f)
        assertEquals(DayNightMode.DARK, migrated.dayNightMode)
        assertEquals("boom", migrated.soundboardPads.first().soundName)
        assertTrue(migrated.soundboardPads.first().isAssigned)
        assertEquals(1, migrated.widgetLayout.size)
        assertEquals("CLOCK", migrated.widgetLayout.first().id)
    }

    @Test
    fun `right hand drive migrates to a right sidebar`() {
        val prefs = mutablePreferencesOf(booleanPreferencesKey("right_hand_drive") to true)
        assertEquals(SidebarPosition.RIGHT, LegacySettings.read(prefs).sidebarPosition)
    }

    @Test
    fun `damaged legacy json falls back to the defaults`() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("shortcuts_json") to "{not json",
            stringPreferencesKey("widget_layout_json") to "[[["
        )
        val migrated = LegacySettings.read(prefs)
        assertEquals(defaultShortcuts(), migrated.shortcuts)
        assertNotNull(migrated.widgetLayout)
        assertEquals(defaultWidgetLayout(), migrated.widgetLayout)
    }

    @Test
    fun `an empty pad is not assigned`() {
        assertTrue(!SoundPadConfig().isAssigned)
        assertTrue(SoundPadConfig(label = "x", soundName = "boom").isAssigned)
        assertTrue(SoundPadConfig(label = "x", audioUri = "content://x").isAssigned)
    }
}
