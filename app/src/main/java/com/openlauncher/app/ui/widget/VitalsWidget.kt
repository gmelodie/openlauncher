package com.openlauncher.app.ui.widget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk
import com.openlauncher.app.ui.theme.widgetLine

private const val NO_READING = "—"

private val THERMAL_PATHS = listOf(
    "/sys/class/thermal/thermal_zone0/temp",
    "/sys/class/thermal/thermal_zone1/temp",
    "/sys/devices/virtual/thermal/thermal_zone0/temp",
    "/sys/class/hwmon/hwmon0/device/temp1_input"
)

private data class CpuSample(val active: Long, val idle: Long)

private data class Vitals(
    val cpuPercent: Float? = null,
    val ramPercent: Float? = null,
    val ramUsedGb: Double? = null,
    val temperatureC: Float? = null
)

@Composable
fun VitalsWidget(
    accent: Color,
    isDayMode: Boolean = false,
    asBars: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var vitals by remember { mutableStateOf(Vitals()) }

    LaunchedEffect(Unit) {
        var previous: CpuSample? = null
        while (true) {
            val reading = withContext(Dispatchers.IO) {
                val next = readCpuSample()
                val ram = readRam(context)
                val sampled = Vitals(
                    cpuPercent   = cpuUsage(previous, next),
                    ramPercent   = ram?.first,
                    ramUsedGb    = ram?.second,
                    temperatureC = readTemperature(context)
                )
                if (next != null) previous = next
                sampled
            }
            vitals = reading
            delay(2500)
        }
    }

    val cpuColor  = warningColor(vitals.cpuPercent, 65f, 85f, accent, isDayMode)
    val ramColor  = warningColor(vitals.ramPercent, 75f, 90f, accent, isDayMode)
    val tempColor = warningColor(vitals.temperatureC, 60f, 75f, accent, isDayMode)

    val gauges = listOf(
        GaugeData("CPU", vitals.cpuPercent, vitals.cpuPercent.percentText(), cpuColor),
        GaugeData("RAM", vitals.ramPercent, vitals.ramUsedGb?.let { "%.1fG".format(it) } ?: NO_READING, ramColor),
        GaugeData("TEMP", vitals.temperatureC, vitals.temperatureC?.let { "%.0f°".format(it) } ?: NO_READING, tempColor)
    )

    Column(
        modifier = modifier.padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (asBars) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                gauges.forEach { gauge ->
                    BarGauge(gauge = gauge, isDayMode = isDayMode, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                gauges.forEach { gauge ->
                    DialGauge(
                        gauge = gauge,
                        isDayMode = isDayMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

private data class GaugeData(
    val label: String,
    val value: Float?,
    val display: String,
    val color: Color
)

private fun Float?.percentText(): String = this?.let { "%.0f%%".format(it) } ?: NO_READING

private fun warningColor(
    value: Float?,
    warn: Float,
    alert: Float,
    accent: Color,
    isDayMode: Boolean
): Color = when {
    value == null    -> if (isDayMode) Color(0xFFAAAAAA) else Color(0xFF555555)
    value > alert    -> Color(0xFFDD5555)
    value > warn     -> Color(0xFFE6A23C)
    else             -> accent
}

// /proc/stat is unreadable for apps under the SELinux policy of Android 8 and
// later. There is no substitute, so the gauge reports no reading.
private fun readCpuSample(): CpuSample? = runCatching {
    val file = File("/proc/stat")
    if (!file.exists() || !file.canRead()) return null
    val line = file.useLines { it.firstOrNull() } ?: return null
    if (!line.startsWith("cpu ")) return null
    val parts = line.trim().split("\\s+".toRegex())
    if (parts.size < 5) return null
    val user    = parts[1].toLong()
    val nice    = parts[2].toLong()
    val system  = parts[3].toLong()
    val idle    = parts[4].toLong()
    val ioWait  = parts.getOrNull(5)?.toLongOrNull() ?: 0L
    val irq     = parts.getOrNull(6)?.toLongOrNull() ?: 0L
    val softIrq = parts.getOrNull(7)?.toLongOrNull() ?: 0L
    CpuSample(active = user + nice + system + ioWait + irq + softIrq, idle = idle)
}.getOrNull()

private fun cpuUsage(previous: CpuSample?, next: CpuSample?): Float? {
    if (previous == null || next == null) return null
    val deltaTotal = (next.active + next.idle) - (previous.active + previous.idle)
    if (deltaTotal <= 0L) return null
    val deltaIdle = next.idle - previous.idle
    return ((deltaTotal - deltaIdle).toFloat() / deltaTotal.toFloat() * 100f).coerceIn(0f, 100f)
}

private fun readRam(context: Context): Pair<Float, Double>? = runCatching {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val gib = 1024.0 * 1024.0 * 1024.0
    val totalGb = memInfo.totalMem / gib
    if (totalGb <= 0.0) return null
    val usedGb = totalGb - memInfo.availMem / gib
    ((usedGb / totalGb) * 100.0).toFloat().coerceIn(0f, 100f) to usedGb
}.getOrNull()

private fun readTemperature(context: Context): Float? {
    THERMAL_PATHS.forEach { path ->
        val reading = runCatching {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return@runCatching null
            var value = file.readText().trim().toFloatOrNull() ?: return@runCatching null
            if (value > 1000f) value /= 1000f
            value.takeIf { it in 10f..150f }
        }.getOrNull()
        if (reading != null) return reading
    }
    return runCatching {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        if (raw > 0) raw / 10f else null
    }.getOrNull()
}

@Composable
private fun BarGauge(
    gauge: GaugeData,
    isDayMode: Boolean,
    modifier: Modifier = Modifier
) {
    val trackColor = widgetLine(isDayMode)
    val contentColor = widgetInk(isDayMode)
    val labelColor = widgetSubInk(isDayMode)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gauge.label,
                color = labelColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = gauge.display,
                color = if (gauge.value == null) labelColor else contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(3.dp))
        val barBorder = if (isDayMode) Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(2.dp)) else Modifier
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor)
                .then(barBorder)
        ) {
            if (gauge.value != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (gauge.value / 100f).coerceIn(0f, 1f))
                        .background(gauge.color)
                )
            }
        }
    }
}

@Composable
private fun DialGauge(
    gauge: GaugeData,
    isDayMode: Boolean,
    modifier: Modifier = Modifier
) {
    val trackColor = widgetLine(isDayMode)
    val contentColor = widgetInk(isDayMode)
    val labelColor = widgetSubInk(isDayMode)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val sizePx = minOf(maxWidth, maxHeight)
        val strokeWidth = 4.5.dp

        Box(
            modifier = Modifier.size(sizePx),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
                val sw = strokeWidth.toPx()
                val value = gauge.value

                if (isDayMode) {
                    drawArc(
                        color = Color.Black.copy(alpha = 0.16f),
                        startAngle = 148f,
                        sweepAngle = 244f,
                        useCenter = false,
                        style = Stroke(width = sw + 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    color = trackColor,
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )

                if (value != null) {
                    val sweep = 240f * (value / 100f).coerceIn(0f, 1f)
                    if (isDayMode && value > 0f) {
                        drawArc(
                            color = Color.Black.copy(alpha = 0.22f),
                            startAngle = 149f,
                            sweepAngle = sweep + 2f,
                            useCenter = false,
                            style = Stroke(width = sw + 0.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawArc(
                        color = gauge.color,
                        startAngle = 150f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = sw, cap = StrokeCap.Round)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = gauge.display,
                    color = if (gauge.value == null) labelColor else contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = gauge.label,
                    color = labelColor,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
