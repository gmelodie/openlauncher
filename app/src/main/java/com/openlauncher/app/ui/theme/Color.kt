package com.openlauncher.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Gruvbox palette ─────────────────────────────────────────────────────────
// The launcher's native look. Light surfaces drive the default day theme;
// the dark ramp backs forced/auto night mode. Names follow the upstream
// gruvbox scale (bg0 = base, higher = further from the base).

// Light ramp (cream base, dark ink)
val GruvLightBg0   = Color(0xFFFBF1C7) // base background
val GruvLightBg1   = Color(0xFFEBDBB2) // raised surface
val GruvLightBg2   = Color(0xFFD5C4A1) // higher surface
val GruvLightBg3   = Color(0xFFBDAE93) // borders / outlines
val GruvLightFg1   = Color(0xFF3C3836) // body text
val GruvLightFg3   = Color(0xFF665C54) // muted text

// Dark ramp (charcoal base, cream ink)
val GruvDarkBg0    = Color(0xFF282828)
val GruvDarkBg1    = Color(0xFF3C3836)
val GruvDarkBg2    = Color(0xFF504945)
val GruvDarkGray   = Color(0xFF928374)

// Neutral (dark) surfaces reused by dropdowns and the always-dark colour picker
val DimSurface  = GruvDarkBg1
val CardSurface = GruvDarkBg1
val DividerGray = GruvDarkBg2
val TextMuted   = GruvDarkGray

// Accents — light-ramp variants read cleanly on the cream day background
val AccentOrange = Color(0xFFD65D0E)
val AccentRed    = Color(0xFFCC241D)
val AccentYellow = Color(0xFFD79921)
val AccentGreen  = Color(0xFF98971A)
val AccentAqua   = Color(0xFF689D6A)
val AccentBlue   = Color(0xFF458588)

val accentPresets      = listOf(AccentOrange, AccentRed, AccentYellow, AccentGreen, AccentAqua, AccentBlue)
val accentPresetLabels = listOf("Orange", "Red", "Yellow", "Green", "Aqua", "Blue")
