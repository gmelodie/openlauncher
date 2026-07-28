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
val GruvLightFg0   = Color(0xFF282828) // strongest ink
val GruvLightFg1   = Color(0xFF3C3836) // body text
val GruvLightFg3   = Color(0xFF665C54) // muted text

// Dark ramp (charcoal base, cream ink). Night surfaces and ink both live here:
// the night background is #282828, not black, so anything darker than bg2 or
// dimmer than the gray disappears into it.
val GruvDarkBg0    = Color(0xFF282828) // base background
val GruvDarkBg1    = Color(0xFF3C3836) // raised surface
val GruvDarkBg2    = Color(0xFF504945) // higher surface
val GruvDarkBg3    = Color(0xFF665C54) // borders / outlines
val GruvDarkGray   = Color(0xFF928374) // faint ink, disabled controls
val GruvDarkFg0    = Color(0xFFFBF1C7) // strongest ink
val GruvDarkFg1    = Color(0xFFEBDBB2) // body text
val GruvDarkFg3    = Color(0xFFBDAE93) // muted text

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

// Warnings. The day red is muted for the cream background; the night red is the
// bright gruvbox red, which is the dimmest red that still reads on charcoal.
val DangerRedDay   = Color(0xFF993333)
val DangerRedNight = Color(0xFFFB4934)

val accentPresets      = listOf(AccentOrange, AccentRed, AccentYellow, AccentGreen, AccentAqua, AccentBlue)
val accentPresetLabels = listOf("Orange", "Red", "Yellow", "Green", "Aqua", "Blue")
