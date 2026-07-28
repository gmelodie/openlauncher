package com.openlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// One ink ramp for every widget. Each widget used to pick its own greys, and the
// muted ones — mid grey on the cream day background, 0.3 to 0.5 alpha at night —
// disappear behind a windscreen in daylight.

@Composable
fun widgetInk(isDayMode: Boolean): Color =
    if (isDayMode) GruvLightFg0 else MaterialTheme.colorScheme.onBackground

// Unit suffixes, captions and field labels.
@Composable
fun widgetSubInk(isDayMode: Boolean): Color =
    if (isDayMode) GruvLightFg3 else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)

// Rings, gauge tracks and hairlines drawn inside a widget.
@Composable
fun widgetLine(isDayMode: Boolean): Color =
    if (isDayMode) GruvLightBg3 else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.34f)
