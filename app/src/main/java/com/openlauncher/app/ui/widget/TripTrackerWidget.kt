package com.openlauncher.app.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.TripState
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.METERS_TO_MILES
import com.openlauncher.app.util.speedIn
import kotlinx.coroutines.delay
import com.openlauncher.app.ui.theme.GruvLightBg1
import com.openlauncher.app.ui.theme.GruvLightBg3
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk
import com.openlauncher.app.ui.theme.widgetLine

private const val MODE_TRIP = "TRIP"
private const val MODE_ACCEL = "0-100"

private enum class AccelState { READY, RUNNING, COMPLETE }

@Composable
fun TripTrackerWidget(
    trip: TripState,
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    onToggleTrip: () -> Unit,
    onResetTrip: () -> Unit,
    onRecordAccel: (Float) -> Unit,
    onClearAccel: () -> Unit,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayColor = widgetInk(isDayMode)
    val dimDisplayColor = if (isDayMode) Color(0xFF111111).copy(alpha = 0.08f) else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    val lcdBorder = widgetLine(isDayMode)
    val labelColor = widgetSubInk(isDayMode)

    val activeAccent = accent
    val teRed = Color(0xFFFF2D55)

    var activeMode by rememberSaveable { mutableStateOf(MODE_TRIP) }
    var accelState by rememberSaveable { mutableStateOf(AccelState.READY) }
    var accelStartMs by rememberSaveable { mutableLongStateOf(0L) }
    var accelResultMs by rememberSaveable { mutableLongStateOf(0L) }
    var accelDisplay by remember { mutableStateOf("0.00") }

    val currentSpeedMps = location?.speedMps ?: 0f
    val speedDisplay = currentSpeedMps.speedIn(isMetric)
    val targetSpeed = if (isMetric) 100f else 60f
    val targetSpeedUnit = if (isMetric) "KM/H" else "MPH"

    LaunchedEffect(accelState, accelStartMs, accelResultMs) {
        when (accelState) {
            AccelState.RUNNING -> while (true) {
                accelDisplay = "%.2f".format(
                    (android.os.SystemClock.elapsedRealtime() - accelStartMs) / 1000f
                )
                delay(30)
            }
            AccelState.COMPLETE -> accelDisplay = "%.2f".format(accelResultMs / 1000f)
            AccelState.READY    -> accelDisplay = "0.00"
        }
    }

    // The run starts when the vehicle moves and stops at the target speed. There
    // is no simulated run, so a recorded time is always a measured one.
    LaunchedEffect(currentSpeedMps, activeMode, accelState) {
        if (activeMode != MODE_ACCEL) return@LaunchedEffect
        when (accelState) {
            AccelState.READY -> if (speedDisplay > 0.8f) {
                accelStartMs = android.os.SystemClock.elapsedRealtime()
                accelState = AccelState.RUNNING
            }
            AccelState.RUNNING -> if (speedDisplay >= targetSpeed) {
                accelResultMs = android.os.SystemClock.elapsedRealtime() - accelStartMs
                accelState = AccelState.COMPLETE
                onRecordAccel(accelResultMs / 1000f)
            }
            AccelState.COMPLETE -> {}
        }
    }

    val averageSpeedMps = if (trip.movingSeconds > 0) trip.speedSumMps / trip.movingSeconds else 0.0
    val avgSpeedDisplay = averageSpeedMps.speedIn(isMetric)
    val speedUnit = if (isMetric) "KM/H" else "MPH"

    val distanceDisplay = if (isMetric) trip.distanceMeters / 1000.0 else trip.distanceMeters / METERS_TO_MILES
    val distUnit = if (isMetric) "KM" else "MI"
    val hasTripData = trip.driveSeconds > 0 || trip.idleSeconds > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, lcdBorder, RoundedCornerShape(6.dp))
                .drawBehind {
                    val dotColor = displayColor.copy(alpha = 0.02f)
                    val dotSize = 1.dp.toPx()
                    val gap = 5.dp.toPx()
                    var x = 3.dp.toPx()
                    while (x < size.width) {
                        var y = 3.dp.toPx()
                        while (y < size.height) {
                            drawCircle(color = dotColor, radius = dotSize / 2, center = Offset(x, y))
                            y += gap
                        }
                        x += gap
                    }
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (activeMode == MODE_TRIP) {
                Column(
                    modifier = Modifier.weight(0.38f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DISTANCE // DIST",
                        color = labelColor,
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    GhostValue(
                        ghost = "888.88",
                        value = "%.2f".format(distanceDisplay),
                        unit = distUnit,
                        valueSize = 24.sp,
                        unitSize = 9.sp,
                        displayColor = displayColor,
                        dimColor = dimDisplayColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "[RUNNING]",
                            color = if (trip.running) activeAccent else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[STOPPED]",
                            color = if (!trip.running && hasTripData) teRed else dimDisplayColor,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.30f)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimerReadout("DRIVE [TIME]", formatDuration(trip.driveSeconds), labelColor, displayColor, dimDisplayColor)
                    TimerReadout("IDLE [TIME]", formatDuration(trip.idleSeconds), labelColor, displayColor, dimDisplayColor)
                }

                Column(
                    modifier = Modifier.weight(0.32f),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "AVG SPEED // SPD",
                            color = labelColor,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        GhostValue(
                            ghost = "888.8",
                            value = "%.1f".format(avgSpeedDisplay),
                            unit = speedUnit,
                            valueSize = 15.sp,
                            unitSize = 7.sp,
                            displayColor = displayColor,
                            dimColor = dimDisplayColor,
                            alignEnd = true
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("SYS STAT", color = labelColor, fontSize = 6.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (trip.running) "A" else "I",
                            color = if (trip.running) activeAccent else displayColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(0.48f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isMetric) "ACCEL TEST // 0-100" else "ACCEL TEST // 0-60",
                        color = labelColor,
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    GhostValue(
                        ghost = "88.88",
                        value = accelDisplay,
                        unit = "SEC",
                        valueSize = 24.sp,
                        unitSize = 9.sp,
                        displayColor = displayColor,
                        dimColor = dimDisplayColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccelFlag("[READY]", accelState == AccelState.READY, activeAccent, dimDisplayColor)
                        AccelFlag("[RUNNING]", accelState == AccelState.RUNNING, Color(0xFFE6A23C), dimDisplayColor)
                        AccelFlag("[COMPLETE]", accelState == AccelState.COMPLETE, activeAccent, dimDisplayColor)
                    }
                }

                Column(
                    modifier = Modifier.weight(0.52f),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SPEED // TARGET %d".format(targetSpeed.toInt()),
                            color = labelColor,
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        GhostValue(
                            ghost = "888.8",
                            value = "%.1f".format(speedDisplay),
                            unit = targetSpeedUnit,
                            valueSize = 15.sp,
                            unitSize = 7.sp,
                            displayColor = displayColor,
                            dimColor = dimDisplayColor,
                            alignEnd = true
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BEST RECORD",
                            color = labelColor,
                            fontSize = 5.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = trip.bestAccelSeconds?.let { "%.2fs".format(it) } ?: "--.--s",
                            color = if (trip.bestAccelSeconds != null) activeAccent else displayColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeTactileButton(
                label = "OPR",
                keyColor = activeAccent,
                active = if (activeMode == MODE_ACCEL) accelState == AccelState.RUNNING else trip.running,
                onClick = {
                    if (activeMode == MODE_ACCEL) {
                        accelState = AccelState.READY
                        accelStartMs = 0L
                        accelResultMs = 0L
                    } else {
                        onToggleTrip()
                    }
                },
                isDayMode = isDayMode
            )

            TeTactileButton(
                label = "RST",
                keyColor = teRed,
                active = false,
                enabled = if (activeMode == MODE_ACCEL) {
                    accelState == AccelState.COMPLETE || trip.bestAccelSeconds != null
                } else {
                    !trip.running && hasTripData
                },
                onClick = {
                    if (activeMode == MODE_ACCEL) {
                        accelState = AccelState.READY
                        accelStartMs = 0L
                        accelResultMs = 0L
                        onClearAccel()
                    } else {
                        onResetTrip()
                    }
                },
                isDayMode = isDayMode
            )

            TeTactileButton(
                label = "EXT",
                keyColor = activeAccent,
                active = activeMode == MODE_ACCEL,
                onClick = { activeMode = if (activeMode == MODE_TRIP) MODE_ACCEL else MODE_TRIP },
                isDayMode = isDayMode
            )
        }
    }
}

@Composable
private fun AccelFlag(label: String, active: Boolean, activeColor: Color, dimColor: Color) {
    Text(
        text = label,
        color = if (active) activeColor else dimColor,
        fontSize = 6.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun TimerReadout(
    label: String,
    value: String,
    labelColor: Color,
    displayColor: Color,
    dimColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = labelColor, fontSize = 6.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Box {
            Text("88:88:88", color = dimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = displayColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// The ghost digits sit behind the reading like an LCD segment mask, so the mask
// must be at least as wide as the value it backs.
@Composable
private fun GhostValue(
    ghost: String,
    value: String,
    unit: String,
    valueSize: androidx.compose.ui.unit.TextUnit,
    unitSize: androidx.compose.ui.unit.TextUnit,
    displayColor: Color,
    dimColor: Color,
    alignEnd: Boolean = false
) {
    val padded = value.padStart(ghost.length, ' ')
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(contentAlignment = if (alignEnd) Alignment.BottomEnd else Alignment.BottomStart) {
            Text(
                text = ghost,
                color = dimColor,
                fontSize = valueSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = padded,
                color = displayColor,
                fontSize = valueSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = unit,
            color = displayColor,
            fontSize = unitSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

private fun formatDuration(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0L)
    return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}

@Composable
private fun TeTactileButton(
    label: String,
    keyColor: Color,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    isDayMode: Boolean
) {
    val printedLabelColor = widgetSubInk(isDayMode)

    val buttonBg = when {
        !enabled  -> Color.Transparent
        active    -> keyColor
        isDayMode -> GruvLightBg1
        else      -> Color(0xFF1D2024)
    }

    val buttonBorder = if (isDayMode) GruvLightBg3 else Color(0xFF2E3238)
    val dotColor = when {
        active  -> if (isDayMode) Color.White else Color.Black
        enabled -> keyColor
        else    -> keyColor.copy(alpha = 0.2f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = printedLabelColor,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(1.dp, buttonBorder, CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
