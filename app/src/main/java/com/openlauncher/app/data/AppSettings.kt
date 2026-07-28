package com.openlauncher.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable

private val GRUVBOX_ORANGE    = Color(0xFFD65D0E).toArgb()
private val GRUVBOX_CREAM     = Color(0xFFFBF1C7).toArgb()
private val GRUVBOX_LIGHT_INK = Color(0xFFEBDBB2).toArgb()
private val BLACK_ARGB        = Color.Black.toArgb()

@Serializable
enum class ClockStyle { DIGITAL, ANALOG }

@Serializable
enum class UnitSystem { METRIC, IMPERIAL }

@Serializable
enum class AppFont { SYSTEM, JETBRAINS_MONO, SOURCE_CODE_PRO }

@Serializable
enum class DayNightMode { DARK, LIGHT, AUTO, SYSTEM }

@Serializable
enum class SidebarPosition { LEFT, RIGHT, BOTTOM }

@Serializable
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT, DIAGONAL, RADIAL }

@Serializable
enum class DefaultShortcutIcon {
    NONE,
    // Navigation & vehicle
    RADIO, CAMERA, PHONE, MAP, NAVIGATION, CAR, GAS_STATION, DASHBOARD,
    // Audio & media
    MUSIC, SPEAKER, HEADSET, EQUALIZER, VOLUME_UP,
    // Connectivity
    BLUETOOTH, WIFI,
    // Lighting & climate
    LIGHTBULB, BRIGHTNESS, AC, THERMOSTAT,
    // General utility
    TV, VIDEOCAM, MOVIE, STAR, MESSAGE, TIMER, LOCK, SETTINGS, FAVORITE,
    // Web / location
    GLOBE
}

@Serializable
data class SoundPadConfig(
    val label: String = "+",
    val audioUri: String = "",
    val soundName: String = ""
) {
    val isAssigned: Boolean get() = audioUri.isNotEmpty() || soundName.isNotEmpty()
}

fun defaultSoundboardPads() = listOf(
    SoundPadConfig("mario_jump", soundName = "mario_jump"),
    SoundPadConfig("mario_coin", soundName = "mario_coin"),
    SoundPadConfig("boom", soundName = "boom"),
    SoundPadConfig("loud_fart", soundName = "loud_fart"),
    SoundPadConfig(),
    SoundPadConfig()
)

@Serializable
data class ShortcutConfig(
    val packageName: String = "",
    val label: String = "",
    val isDefault: Boolean = false,
    val defaultIcon: DefaultShortcutIcon = DefaultShortcutIcon.NONE,
    // null = native app icon; non-null = override with this vector icon
    val customIconOverride: DefaultShortcutIcon? = null
)

const val GRID_COLS = 3
const val GRID_ROWS = 2

@Serializable
data class WidgetConfig(
    val id: String = "",
    val gridX: Int = 0,
    val gridY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val enabled: Boolean = true
)

// Stored layouts come from disk and can hold any number. Clamp them here so the
// grid math downstream always gets a cell that fits.
fun WidgetConfig.clampToGrid(): WidgetConfig {
    val x = gridX.coerceIn(0, GRID_COLS - 1)
    val y = gridY.coerceIn(0, GRID_ROWS - 1)
    return copy(
        gridX = x,
        gridY = y,
        spanX = spanX.coerceIn(1, GRID_COLS - x),
        spanY = spanY.coerceIn(1, GRID_ROWS - y)
    )
}

@Serializable
data class AppSettings(
    val vehicleName: String = "HB20",
    // Gruvbox defaults — mirror ui.theme.Color. Accent (orange) shows in both
    // modes; fontColor is the night-mode ink (cream), so it must stay light —
    // day mode draws its own dark ink and never reads fontColor.
    val accentColor: Int = GRUVBOX_ORANGE,
    val backgroundColor: Int = GRUVBOX_CREAM,
    val fontColor: Int = GRUVBOX_LIGHT_INK,
    val wallpaperUri: String = "",
    val fontBold: Boolean = false,
    // textScale drives the font scale of the whole launcher, so it reaches every
    // label, not only the ones styled from MaterialTheme.typography.
    val textScale: Float = 1.3f,
    val uiScale: Float = 1.0f,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appFont: AppFont = AppFont.JETBRAINS_MONO,
    val showWeather: Boolean = true,
    val showClock: Boolean = true,
    val showMap: Boolean = true,
    val showNowPlaying: Boolean = true,
    val shortcuts: List<ShortcutConfig> = defaultShortcuts(),
    val widgetLayout: List<WidgetConfig> = defaultWidgetLayout(),
    val carPlayPackage: String = "",
    val androidAutoPackage: String = "",
    val useGradient: Boolean = false,
    val gradientEndColor: Int = BLACK_ARGB,
    val wallpaperDim: Float = 0.55f,
    val sidebarPosition: SidebarPosition = SidebarPosition.LEFT,
    val bottomBarShortcutsRight: Boolean = false,
    val showAltimeter: Boolean = false,
    val showSpeedometer: Boolean = false,
    val dayNightMode: DayNightMode = DayNightMode.LIGHT,
    val showPip: Boolean = false,
    val pipAppPackage: String = "",
    // Head unit's radio app — mirrored & controlled via its MediaSession
    val radioPackage: String = "",
    val onboardingCompleted: Boolean = false,
    val showVitals: Boolean = false,
    val showTripTracker: Boolean = false,
    val showSoundboard: Boolean = false,
    val soundboardPads: List<SoundPadConfig> = defaultSoundboardPads(),
    val vitalsAsBars: Boolean = false,
    val speedometerDigitalOnly: Boolean = false,
    val gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
    val useCustomBackgroundColor: Boolean = false,
    val trip: TripState = TripState(),
    val radioPresets: RadioPresets = RadioPresets(),
    // Gravity vector recorded while the vehicle stands level. All zero means the
    // altimeter must capture a fresh reference on its next reading.
    val levelReference: LevelReference = LevelReference(),
    val mapZoom: Int = 16,
    val navPackage: String = "com.waze",
    // Raised by SettingsRepository when a stored value needs a one-off rewrite.
    val settingsVersion: Int = 1
)

const val CURRENT_SETTINGS_VERSION = 2
const val MIN_MAP_ZOOM = 12
const val MAX_MAP_ZOOM = 18

@Serializable
data class TripState(
    val running: Boolean = false,
    val distanceMeters: Double = 0.0,
    val driveSeconds: Double = 0.0,
    val idleSeconds: Double = 0.0,
    val speedSumMps: Double = 0.0,
    val movingSeconds: Double = 0.0,
    val bestAccelSeconds: Float? = null
)

@Serializable
data class RadioPresets(
    val fm: List<Float> = listOf(88.5f, 91.5f, 98.1f, 101.9f, 104.3f, 107.5f),
    val am: List<Float> = listOf(540f, 680f, 820f, 1040f, 1260f, 1420f)
)

@Serializable
data class LevelReference(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    val isSet: Boolean get() = x != 0f || y != 0f || z != 0f
}

// Pre-wired to the driver's most-used apps. packageName launches directly when
// the app is installed; when it is not, the slot falls back to its default
// vector icon and a long-press lets the driver rebind it to any installed app.
fun defaultShortcuts() = listOf(
    ShortcutConfig(packageName = "com.waze",                 label = "Waze",    isDefault = true, defaultIcon = DefaultShortcutIcon.NAVIGATION),
    ShortcutConfig(packageName = "com.spotify.music",        label = "Spotify", isDefault = true, defaultIcon = DefaultShortcutIcon.MUSIC),
    ShortcutConfig(packageName = "org.schabi.newpipe",       label = "NewPipe", isDefault = true, defaultIcon = DefaultShortcutIcon.VIDEOCAM),
    ShortcutConfig(packageName = "com.google.android.youtube", label = "YouTube", isDefault = true, defaultIcon = DefaultShortcutIcon.TV),
    ShortcutConfig(packageName = "dev.jdtech.jellyfin",      label = "Jellyfin", isDefault = true, defaultIcon = DefaultShortcutIcon.MOVIE)
)

fun defaultWidgetLayout() = listOf(
    WidgetConfig("CLOCK",       gridX = 0, gridY = 0, spanX = 1, spanY = 1),
    WidgetConfig("WEATHER",     gridX = 1, gridY = 0, spanX = 1, spanY = 1),
    WidgetConfig("MAP",         gridX = 2, gridY = 0, spanX = 1, spanY = 2),
    WidgetConfig("NOW_PLAYING", gridX = 0, gridY = 1, spanX = 2, spanY = 1)
)

fun AppSettings.activeWidgetIds(): Set<String> = buildSet {
    if (showClock) add("CLOCK")
    if (showWeather) add("WEATHER")
    if (showNowPlaying) add("NOW_PLAYING")
    if (showMap) add("MAP")
    if (showAltimeter) add("ALTIMETER")
    if (showSpeedometer) add("SPEEDOMETER")
    if (showVitals) add("VITALS")
    if (showTripTracker) add("TRIP_TRACKER")
    if (showSoundboard) add("SOUNDBOARD")
}

fun AppSettings.activeWidgets(): List<WidgetConfig> {
    val ids = activeWidgetIds()
    return widgetLayout.filter { it.enabled && it.id in ids }
}

/**
 * Moves [movingId] to ([targetX], [targetY]) and pushes any displaced widgets to the
 * first available free cell, cascading until all conflicts are resolved.
 * Operates only on the supplied [layout] list — callers should pass only active/enabled widgets.
 */
fun computeWidgetMove(
    layout: List<WidgetConfig>,
    movingId: String,
    targetX: Int,
    targetY: Int
): List<WidgetConfig> {
    val moving = layout.find { it.id == movingId } ?: return layout
    val placed = moving.copy(
        gridX = targetX.coerceIn(0, GRID_COLS - moving.spanX),
        gridY = targetY.coerceIn(0, GRID_ROWS - moving.spanY)
    )

    val others  = layout.filter { it.id != movingId }
    val result  = mutableListOf(placed)
    val occupied = buildOccupied(result).toMutableSet()

    // Stable widgets that don't conflict go first; displaced ones are pushed afterwards
    val (stable, displaced) = others.partition { w -> result.none { widgetsOverlap(it, w) } }

    for (w in stable) {
        result.add(w)
        for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) occupied.add(w.gridX + dx to w.gridY + dy)
    }

    for (w in displaced) {
        val pos = firstFreeGridPos(w.spanX, w.spanY, occupied)
        val resolved = if (pos != null) w.copy(gridX = pos.first, gridY = pos.second) else w
        result.add(resolved)
        for (dx in 0 until resolved.spanX) for (dy in 0 until resolved.spanY) occupied.add(resolved.gridX + dx to resolved.gridY + dy)
    }

    return result
}

fun freeGridArea(layout: List<WidgetConfig>, spanX: Int, spanY: Int): Pair<Int, Int>? =
    firstFreeGridPos(spanX, spanY, buildOccupied(layout))

private fun buildOccupied(widgets: List<WidgetConfig>) = buildSet<Pair<Int, Int>> {
    widgets.forEach { w -> for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy) }
}

private fun widgetsOverlap(a: WidgetConfig, b: WidgetConfig): Boolean =
    a.gridX < b.gridX + b.spanX && a.gridX + a.spanX > b.gridX &&
    a.gridY < b.gridY + b.spanY && a.gridY + a.spanY > b.gridY

private fun firstFreeGridPos(spanX: Int, spanY: Int, occupied: Set<Pair<Int, Int>>): Pair<Int, Int>? {
    for (row in 0 until GRID_ROWS) for (col in 0 until GRID_COLS) {
        if (col + spanX > GRID_COLS || row + spanY > GRID_ROWS) continue
        if ((0 until spanX).all { dx -> (0 until spanY).all { dy -> (col + dx to row + dy) !in occupied } })
            return col to row
    }
    return null
}
