package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.LevelReference
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.METERS_TO_FEET
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk

private const val NO_READING = "—"

@Composable
fun AltimeterWidget(
    location: LocationData?,
    gravity: FloatArray?,
    levelReference: LevelReference,
    isMetric: Boolean,
    accent: Color,
    onCaptureLevel: (LevelReference) -> Unit,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isDayMode) Color(0xFF333333) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    val labelColor = widgetSubInk(isDayMode)
    val valueColor = widgetInk(isDayMode)

    // The unit sits at a fixed angle in the dash. Tilt is therefore measured
    // against the gravity vector recorded while the vehicle stands level, not
    // against the device axes.
    LaunchedEffect(gravity, levelReference.isSet) {
        if (levelReference.isSet) return@LaunchedEffect
        val sample = gravity ?: return@LaunchedEffect
        if (sample.magnitude() < 1f) return@LaunchedEffect
        onCaptureLevel(LevelReference(sample[0], sample[1], sample[2]))
    }

    val tilt = remember(gravity, levelReference) { tiltFrom(gravity, levelReference) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALTITUDE",
                color = labelColor,
                fontSize = 7.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = altitudeText(location, isMetric),
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Icon(
            imageVector        = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier
                .size(56.dp)
                .padding(top = 10.dp)
                .graphicsLayer { rotationZ = tilt?.roll ?: 0f }
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text("ROLL", color = labelColor, fontSize = 7.sp, letterSpacing = 1.sp)
                Text(
                    text = tilt?.let { "%.1f°".format(it.roll) } ?: NO_READING,
                    color = if (tilt == null) labelColor else accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PITCH", color = labelColor, fontSize = 7.sp, letterSpacing = 1.sp, textAlign = TextAlign.End)
                Text(
                    text = tilt?.let { "%.1f°".format(it.pitch) } ?: NO_READING,
                    color = if (tilt == null) labelColor else accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private data class Tilt(val roll: Float, val pitch: Float)

private fun altitudeText(location: LocationData?, isMetric: Boolean): String {
    if (location == null) return NO_READING
    val value = if (isMetric) location.altitude else location.altitude * METERS_TO_FEET
    return "%,.0f %s".format(value, if (isMetric) "m" else "ft")
}

private fun FloatArray.magnitude(): Float = sqrt(this[0] * this[0] + this[1] * this[1] + this[2] * this[2])

private fun tiltFrom(gravity: FloatArray?, reference: LevelReference): Tilt? {
    if (gravity == null || !reference.isSet) return null
    val magnitude = gravity.magnitude()
    if (magnitude < 1f) return null

    val up = normalize(floatArrayOf(reference.x, reference.y, reference.z)) ?: return null
    // Lateral axis: the device x axis with its component along "up" removed. A
    // unit mounted with x near vertical falls back to the z axis.
    val lateral = orthogonal(floatArrayOf(1f, 0f, 0f), up)
        ?: orthogonal(floatArrayOf(0f, 0f, 1f), up)
        ?: return null
    val forward = cross(up, lateral)

    val g = floatArrayOf(gravity[0] / magnitude, gravity[1] / magnitude, gravity[2] / magnitude)
    val roll = Math.toDegrees(asin(dot(g, lateral).coerceIn(-1f, 1f).toDouble())).toFloat()
    val pitch = Math.toDegrees(asin(dot(g, forward).coerceIn(-1f, 1f).toDouble())).toFloat()
    return Tilt(roll = roll, pitch = pitch)
}

private fun normalize(v: FloatArray): FloatArray? {
    val length = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    if (length < 1e-3f) return null
    return floatArrayOf(v[0] / length, v[1] / length, v[2] / length)
}

private fun orthogonal(axis: FloatArray, up: FloatArray): FloatArray? {
    val projection = dot(axis, up)
    if (abs(projection) > 0.98f) return null
    return normalize(
        floatArrayOf(
            axis[0] - projection * up[0],
            axis[1] - projection * up[1],
            axis[2] - projection * up[2]
        )
    )
}

private fun dot(a: FloatArray, b: FloatArray): Float = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

private fun cross(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0]
)
