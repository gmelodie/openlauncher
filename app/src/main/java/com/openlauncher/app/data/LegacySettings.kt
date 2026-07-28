package com.openlauncher.app.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Versions up to 0.0.5 stored one preference key per field. This reads that
// layout once, so an installed head unit keeps its configuration after the
// upgrade to the single JSON document.
internal object LegacySettings {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val VEHICLE_NAME       = stringPreferencesKey("vehicle_name")
    private val ACCENT_COLOR       = intPreferencesKey("accent_color")
    private val BG_COLOR           = intPreferencesKey("bg_color")
    private val FONT_COLOR         = intPreferencesKey("font_color")
    private val WALLPAPER_URI      = stringPreferencesKey("wallpaper_uri")
    private val FONT_BOLD          = booleanPreferencesKey("font_bold")
    private val TEXT_SCALE         = floatPreferencesKey("text_scale")
    private val UI_SCALE           = floatPreferencesKey("ui_scale")
    private val CLOCK_STYLE        = stringPreferencesKey("clock_style")
    private val UNIT_SYSTEM        = stringPreferencesKey("unit_system")
    private val APP_FONT           = stringPreferencesKey("app_font")
    private val SHOW_WEATHER       = booleanPreferencesKey("show_weather")
    private val SHOW_CLOCK         = booleanPreferencesKey("show_clock")
    private val SHOW_MAP           = booleanPreferencesKey("show_telemetry")
    private val SHOW_NOW_PLAYING   = booleanPreferencesKey("show_now_playing")
    private val SHOW_ALTIMETER     = booleanPreferencesKey("show_altimeter")
    private val SHOW_SPEEDOMETER   = booleanPreferencesKey("show_speedometer")
    private val SHORTCUTS_JSON     = stringPreferencesKey("shortcuts_json")
    private val WIDGET_LAYOUT_JSON = stringPreferencesKey("widget_layout_json")
    private val CAR_PLAY_PACKAGE     = stringPreferencesKey("car_play_package")
    private val ANDROID_AUTO_PACKAGE = stringPreferencesKey("android_auto_package")
    private val USE_GRADIENT         = booleanPreferencesKey("use_gradient")
    private val GRADIENT_END_COLOR   = intPreferencesKey("gradient_end_color")
    private val WALLPAPER_DIM        = floatPreferencesKey("wallpaper_dim")
    private val RIGHT_HAND_DRIVE     = booleanPreferencesKey("right_hand_drive")
    private val SIDEBAR_POSITION           = stringPreferencesKey("sidebar_position")
    private val BOTTOM_BAR_SHORTCUTS_RIGHT = booleanPreferencesKey("bottom_bar_shortcuts_right")
    private val DAY_NIGHT_MODE       = stringPreferencesKey("day_night_mode")
    private val SHOW_PIP             = booleanPreferencesKey("show_pip")
    private val PIP_APP_PACKAGE      = stringPreferencesKey("pip_app_package")
    private val RADIO_PACKAGE        = stringPreferencesKey("radio_package")
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val SHOW_VITALS          = booleanPreferencesKey("show_vitals")
    private val SHOW_TRIP_TRACKER    = booleanPreferencesKey("show_trip_tracker")
    private val SHOW_SOUNDBOARD      = booleanPreferencesKey("show_soundboard")
    private val SOUNDBOARD_PADS_JSON = stringPreferencesKey("soundboard_pads_json")
    private val VITALS_AS_BARS       = booleanPreferencesKey("vitals_as_bars")
    private val SPEEDOMETER_DIGITAL_ONLY = booleanPreferencesKey("speedometer_digital_only")
    private val GRADIENT_DIRECTION   = stringPreferencesKey("gradient_direction")
    private val USE_CUSTOM_BG_COLOR  = booleanPreferencesKey("use_custom_bg_color")

    @Serializable
    private data class LegacyPad(
        val label: String = "+",
        val audioUri: String = "",
        val synthType: String = ""
    )

    fun read(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return defaults.copy(
            vehicleName     = prefs[VEHICLE_NAME] ?: defaults.vehicleName,
            accentColor     = prefs[ACCENT_COLOR] ?: defaults.accentColor,
            backgroundColor = prefs[BG_COLOR] ?: defaults.backgroundColor,
            fontColor       = prefs[FONT_COLOR] ?: defaults.fontColor,
            wallpaperUri    = prefs[WALLPAPER_URI] ?: defaults.wallpaperUri,
            fontBold        = prefs[FONT_BOLD] ?: defaults.fontBold,
            textScale       = prefs[TEXT_SCALE] ?: defaults.textScale,
            uiScale         = prefs[UI_SCALE] ?: defaults.uiScale,
            clockStyle      = prefs[CLOCK_STYLE].toEnum(defaults.clockStyle),
            unitSystem      = prefs[UNIT_SYSTEM].toEnum(defaults.unitSystem),
            appFont         = prefs[APP_FONT].toEnum(defaults.appFont),
            showWeather     = prefs[SHOW_WEATHER] ?: defaults.showWeather,
            showClock       = prefs[SHOW_CLOCK] ?: defaults.showClock,
            showMap         = prefs[SHOW_MAP] ?: defaults.showMap,
            showNowPlaying  = prefs[SHOW_NOW_PLAYING] ?: defaults.showNowPlaying,
            showAltimeter   = prefs[SHOW_ALTIMETER] ?: defaults.showAltimeter,
            showSpeedometer = prefs[SHOW_SPEEDOMETER] ?: defaults.showSpeedometer,
            shortcuts       = decodeList<ShortcutConfig>(prefs[SHORTCUTS_JSON]) ?: defaults.shortcuts,
            widgetLayout    = readLayout(prefs, defaults),
            carPlayPackage     = prefs[CAR_PLAY_PACKAGE] ?: defaults.carPlayPackage,
            androidAutoPackage = prefs[ANDROID_AUTO_PACKAGE] ?: defaults.androidAutoPackage,
            useGradient      = prefs[USE_GRADIENT] ?: defaults.useGradient,
            gradientEndColor = prefs[GRADIENT_END_COLOR] ?: defaults.gradientEndColor,
            wallpaperDim     = prefs[WALLPAPER_DIM] ?: defaults.wallpaperDim,
            sidebarPosition  = prefs[SIDEBAR_POSITION].toEnum(
                if (prefs[RIGHT_HAND_DRIVE] == true) SidebarPosition.RIGHT else defaults.sidebarPosition
            ),
            bottomBarShortcutsRight = prefs[BOTTOM_BAR_SHORTCUTS_RIGHT] ?: defaults.bottomBarShortcutsRight,
            dayNightMode     = prefs[DAY_NIGHT_MODE].toEnum(defaults.dayNightMode),
            showPip          = prefs[SHOW_PIP] ?: defaults.showPip,
            pipAppPackage    = prefs[PIP_APP_PACKAGE] ?: defaults.pipAppPackage,
            radioPackage     = prefs[RADIO_PACKAGE] ?: defaults.radioPackage,
            onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            showVitals       = prefs[SHOW_VITALS] ?: defaults.showVitals,
            showTripTracker  = prefs[SHOW_TRIP_TRACKER] ?: defaults.showTripTracker,
            showSoundboard   = prefs[SHOW_SOUNDBOARD] ?: defaults.showSoundboard,
            soundboardPads   = readPads(prefs) ?: defaults.soundboardPads,
            vitalsAsBars     = prefs[VITALS_AS_BARS] ?: defaults.vitalsAsBars,
            speedometerDigitalOnly = prefs[SPEEDOMETER_DIGITAL_ONLY] ?: defaults.speedometerDigitalOnly,
            gradientDirection = prefs[GRADIENT_DIRECTION].toEnum(defaults.gradientDirection),
            useCustomBackgroundColor = prefs[USE_CUSTOM_BG_COLOR] ?: defaults.useCustomBackgroundColor
        )
    }

    private fun readLayout(prefs: Preferences, defaults: AppSettings): List<WidgetConfig> {
        val loaded = decodeList<WidgetConfig>(prefs[WIDGET_LAYOUT_JSON]) ?: return defaults.widgetLayout
        // The 2x2 layout of older versions has no widget past column 1.
        if (loaded.none { it.gridX >= 2 }) return defaults.widgetLayout
        return loaded
    }

    private fun readPads(prefs: Preferences): List<SoundPadConfig>? =
        decodeList<LegacyPad>(prefs[SOUNDBOARD_PADS_JSON])
            ?.map { SoundPadConfig(label = it.label, audioUri = it.audioUri, soundName = it.synthType) }

    private inline fun <reified T> decodeList(raw: String?): List<T>? {
        if (raw.isNullOrEmpty()) return null
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T {
        if (this == null) return fallback
        return runCatching { enumValueOf<T>(this) }.getOrDefault(fallback)
    }
}
