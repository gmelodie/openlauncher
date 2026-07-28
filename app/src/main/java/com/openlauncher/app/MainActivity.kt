package com.openlauncher.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.GradientDirection
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.ui.components.Sidebar
import com.openlauncher.app.ui.screen.*
import com.openlauncher.app.ui.theme.GruvDarkBg0
import com.openlauncher.app.ui.theme.GruvDarkBg2
import com.openlauncher.app.ui.theme.GruvLightBg0
import com.openlauncher.app.ui.theme.GruvLightFg1
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import com.openlauncher.app.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private var askedForLocation = false

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            vm.startLocationUpdates()
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        setContent { LauncherRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshConnectivity()
        vm.refreshMedia()
    }

    override fun onStop() {
        super.onStop()
        vm.stopLocationUpdates()
    }

    // Ask once per process. Repeating the request on every start annoys a driver
    // who chose to run without GPS.
    override fun onStart() {
        super.onStart()
        if (hasLocationPermission()) {
            vm.startLocationUpdates()
            return
        }
        if (askedForLocation) return
        askedForLocation = true
        locationPermissions.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
}

@Composable
private fun LauncherRoot(vm: LauncherViewModel) {
    val settingsLoaded by vm.settingsLoaded.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val isDayModeVM by vm.isDayMode.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    // SYSTEM mode reads the composition value so the theme follows an immediate
    // change of the system setting.
    val isDayMode = if (settings.dayNightMode == DayNightMode.SYSTEM) !systemIsDark else isDayModeVM

    val accent = Color(settings.accentColor)
    val background = when {
        settings.useCustomBackgroundColor -> Color(settings.backgroundColor)
        isDayMode -> GruvLightBg0
        else -> GruvDarkBg0
    }
    val textColor = if (isDayMode) GruvLightFg1 else Color(settings.fontColor)

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * settings.uiScale,
            fontScale = baseDensity.fontScale * settings.textScale
        )
    ) {
        if (!settingsLoaded) {
            Box(modifier = Modifier.fillMaxSize().background(GruvLightBg0))
            return@CompositionLocalProvider
        }
        OpenLauncherTheme(
            accent = accent,
            background = background,
            textColor = textColor,
            fontBold = settings.fontBold,
            appFont = settings.appFont,
            isDayMode = isDayMode,
            useCustomBg = settings.useCustomBackgroundColor
        ) {
            if (!settings.onboardingCompleted) {
                OnboardingScreen(
                    accent = accent,
                    isDayMode = isDayMode,
                    onComplete = {
                        vm.updateSettings { copy(onboardingCompleted = true) }
                        vm.startLocationUpdates()
                    }
                )
                return@OpenLauncherTheme
            }
            LauncherShell(vm = vm, settings = settings, accent = accent, background = background, isDayMode = isDayMode)
        }
    }
}

@Composable
private fun LauncherShell(
    vm: LauncherViewModel,
    settings: AppSettings,
    accent: Color,
    background: Color,
    isDayMode: Boolean
) {
    val nav by vm.nav.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()

    val baseDensity = LocalDensity.current
    val isBottomBar = settings.sidebarPosition == SidebarPosition.BOTTOM
    val dividerColor = if (isDayMode) Color(0xFFCCCCCC) else GruvDarkBg2

    Box(modifier = Modifier.fillMaxSize().backgroundOf(settings, background)) {
        if (settings.wallpaperUri.isNotEmpty()) {
            AsyncImage(
                model = android.net.Uri.parse(settings.wallpaperUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.wallpaperDim))
            )
        }

        val sidebar: @Composable () -> Unit = {
            // The sidebar scales more gently than the panes so its touch targets
            // stay reachable at every UI scale.
            val sidebarDensity = Density(
                density = baseDensity.density * (1.0f + (settings.uiScale - 1.0f) * 0.35f),
                fontScale = baseDensity.fontScale
            )
            CompositionLocalProvider(LocalDensity provides sidebarDensity) {
                Sidebar(
                    currentDest = nav,
                    settings = settings,
                    isHorizontal = isBottomBar,
                    installedIconFor = { pkg -> vm.iconFor(pkg) },
                    onNavigate = { dest ->
                        vm.cancelShortcutPicker()
                        vm.cancelCarPlayPicker()
                        vm.exitRearrangeMode()
                        vm.navigate(dest)
                    },
                    onShortcutClick = { slot ->
                        // An unbound slot has nothing to launch, so a tap picks
                        // the app instead of doing nothing.
                        val pkg = settings.shortcuts.getOrNull(slot)?.packageName.orEmpty()
                        if (pkg.isEmpty()) vm.startShortcutPicker(slot) else vm.launchApp(pkg)
                    },
                    onShortcutLongPress = { slot -> vm.startShortcutPicker(slot) },
                    onShortcutRemove = { slot -> vm.removeShortcut(slot) },
                    onShortcutSetIcon = { slot, icon -> vm.setShortcutIcon(slot, icon) },
                    onReorder = { from, to -> vm.reorderShortcut(from, to) },
                    onAddShortcut = { vm.startShortcutPicker(settings.shortcuts.size) }
                )
            }
        }

        val mainPane: @Composable (Modifier) -> Unit = { paneModifier ->
            AnimatedContent(
                targetState = nav,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 10 } togetherWith
                        fadeOut() + slideOutHorizontally { -it / 10 }
                },
                modifier = paneModifier,
                label = "pane_transition"
            ) { destination ->
                when (destination) {
                    NavDestination.HOME -> HomePane(vm, settings, accent, isDayMode)
                    NavDestination.APP_LIBRARY -> AppLibraryPane(vm, apps, accent)
                    NavDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        accent = accent,
                        onUpdate = { block -> vm.updateSettings(block) },
                        onReset = { vm.resetSettings() },
                        onRecalibrateLevel = { vm.clearLevelReference() }
                    )
                }
            }
        }

        if (isBottomBar) {
            Column(modifier = Modifier.fillMaxSize()) {
                mainPane(Modifier.weight(1f).fillMaxWidth())
                HorizontalDivider(color = dividerColor)
                sidebar()
            }
            return@Box
        }
        Row(modifier = Modifier.fillMaxSize()) {
            if (settings.sidebarPosition == SidebarPosition.LEFT) {
                sidebar()
                VerticalDivider(modifier = Modifier.fillMaxHeight(), color = dividerColor)
            }
            mainPane(Modifier.weight(1f).fillMaxHeight())
            if (settings.sidebarPosition == SidebarPosition.RIGHT) {
                VerticalDivider(modifier = Modifier.fillMaxHeight(), color = dividerColor)
                sidebar()
            }
        }
    }
}

@Composable
private fun HomePane(
    vm: LauncherViewModel,
    settings: AppSettings,
    accent: Color,
    isDayMode: Boolean
) {
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val weatherError by vm.weatherError.collectAsStateWithLifecycle()
    val weatherPlace by vm.weatherPlace.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val gravity by vm.gravity.collectAsStateWithLifecycle()
    val trip by vm.trip.collectAsStateWithLifecycle()
    val bearing by vm.compassBearing.collectAsStateWithLifecycle()
    val isWifi by vm.isWifi.collectAsStateWithLifecycle()
    val isData by vm.isData.collectAsStateWithLifecycle()
    val hardwareRadio by vm.hardwareRadio.collectAsStateWithLifecycle()
    val navLabel = remember(settings.navPackage) { vm.navAppLabel() }

    HomeScreen(
        settings = settings,
        weather = weather,
        weatherError = weatherError,
        weatherPlace = weatherPlace,
        nowPlaying = nowPlaying,
        location = location,
        gravity = gravity,
        trip = trip,
        bearing = bearing,
        isWifi = isWifi,
        isData = isData,
        isDayMode = isDayMode,
        onPlayPause = vm::playPause,
        onNext = vm::skipNext,
        onPrev = vm::skipPrev,
        onLaunchCarPlay = { vm.launchApp(settings.carPlayPackage) },
        onLaunchAndroidAuto = { vm.launchApp(settings.androidAutoPackage) },
        onAssignCarPlay = { vm.startCarPlayPicker() },
        onAssignAndroidAuto = { vm.startAndroidAutoPicker() },
        onClearCarPlay = { vm.clearCarPlayApp() },
        onClearAndroidAuto = { vm.clearAndroidAutoApp() },
        onAssignPip = { vm.startPipPicker() },
        onClearPip = { vm.clearPipApp() },
        onLaunchPip = { vm.launchApp(settings.pipAppPackage) },
        onTapNowPlaying = {
            nowPlaying?.controller?.packageName
                ?.takeIf { it.isNotEmpty() }
                ?.let { vm.launchApp(it) }
        },
        onUpdateWidget = { id, sx, sy -> vm.updateWidgetConfig(id, sx, sy) },
        onMoveWidget = { id, gx, gy -> vm.moveWidgetConfig(id, gx, gy) },
        onAddWidget = { id -> vm.addWidget(id) },
        onRemoveWidget = { id -> vm.removeWidget(id) },
        onSetClockStyle = { style -> vm.updateSettings { copy(clockStyle = style) } },
        onOpenNav = { vm.launchNavApp() },
        onAssignNav = { vm.startNavPicker() },
        onSetMapZoom = { zoom -> vm.updateSettings { copy(mapZoom = zoom) } },
        navLabel = navLabel,
        onSetVitalsAsBars = { asBars -> vm.updateSettings { copy(vitalsAsBars = asBars) } },
        onSetSpeedometerDigitalOnly = { digital -> vm.updateSettings { copy(speedometerDigitalOnly = digital) } },
        onUpdateSoundPad = { idx, pad -> vm.updateSoundboardPad(idx, pad) },
        onToggleTrip = { vm.toggleTrip() },
        onResetTrip = { vm.resetTrip() },
        onRecordAccel = { seconds -> vm.recordAccelTime(seconds) },
        onClearAccel = { vm.clearAccelRecord() },
        onCaptureLevel = { reference -> vm.setLevelReference(reference) },
        onSetRadioPreset = { index, isFm, freq -> vm.setRadioPreset(index, isFm, freq) },
        hardwareRadio = hardwareRadio,
        onLaunchHardwareRadio = { vm.launchHardwareRadioApp() },
        onStopHardwareRadio = { vm.stopHardwareRadioApp() },
        onRadioSeekUp = { vm.radioSeekUp() },
        onRadioSeekDown = { vm.radioSeekDown() },
        onRadioCycleFm = { vm.radioCycleFm() },
        onRadioSwitchAm = { vm.radioSwitchAm() },
        onRadioTune = { band, freq -> vm.radioTune(band, freq) },
        onAssignRadio = { vm.startRadioPicker() }
    )
}

@Composable
private fun AppLibraryPane(
    vm: LauncherViewModel,
    apps: List<com.openlauncher.app.model.AppInfo>,
    accent: Color
) {
    val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()
    val pickerSlot by vm.shortcutPickerSlot.collectAsStateWithLifecycle()
    val appPickerTarget by vm.appPickerTarget.collectAsStateWithLifecycle()

    AppLibraryScreen(
        apps = apps,
        isLoading = appsLoading,
        isPickerMode = pickerSlot != null,
        pickerSlot = pickerSlot,
        isCarPlayPickerMode = appPickerTarget != null,
        carPlayPickerLabel = when (appPickerTarget) {
            LauncherViewModel.AppPickerTarget.ANDROID_AUTO -> "CHOOSE ANDROID AUTO APP"
            LauncherViewModel.AppPickerTarget.PIP -> "CHOOSE PIP APP"
            LauncherViewModel.AppPickerTarget.RADIO -> "CHOOSE RADIO APP"
            LauncherViewModel.AppPickerTarget.NAV -> "CHOOSE NAVIGATION APP"
            else -> "CHOOSE CARPLAY APP"
        },
        accent = accent,
        iconFor = { pkg -> vm.iconFor(pkg) },
        onAppClick = { app -> vm.launchApp(app.packageName) },
        onPickerSelect = { slot, app -> vm.assignShortcut(slot, app) },
        onCarPlaySelect = { app -> vm.assignPickerApp(app) }
    )
}

private fun Modifier.backgroundOf(settings: AppSettings, color: Color): Modifier {
    if (!settings.useCustomBackgroundColor || !settings.useGradient) return background(color)
    val colors = listOf(color, Color(settings.gradientEndColor))
    val brush = when (settings.gradientDirection) {
        GradientDirection.TOP_TO_BOTTOM -> Brush.verticalGradient(colors)
        GradientDirection.LEFT_TO_RIGHT -> Brush.horizontalGradient(colors)
        GradientDirection.DIAGONAL -> Brush.linearGradient(colors)
        GradientDirection.RADIAL -> Brush.radialGradient(colors)
    }
    return background(brush)
}
