package com.openlauncher.app.ui.widget

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.ui.theme.GruvDarkBg2
import com.openlauncher.app.ui.theme.GruvLightBg2
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk
import com.openlauncher.app.ui.theme.widgetLine

@Composable
fun ClockWidget(
    style: ClockStyle,
    accent: Color,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    // The analog face shows seconds, the digital face shows minutes. A slower
    // tick for the digital face drops 59 of every 60 recompositions.
    val tickMs = if (style == ClockStyle.ANALOG) 1_000L else 15_000L
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(tickMs) {
        while (true) {
            now = Date()
            delay(tickMs)
        }
    }

    val contentColor = widgetInk(isDayMode)
    val subColor     = widgetSubInk(isDayMode)

    Box(modifier = modifier) {
        when (style) {
            ClockStyle.DIGITAL -> DigitalClock(now, contentColor, subColor)
            ClockStyle.ANALOG  -> AnalogClock(now, accent, isDayMode)
        }
    }
}

@Composable
fun currentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
    }
}

@Composable
private fun DigitalClock(now: Date, contentColor: Color, subColor: Color) {
    val context = LocalContext.current
    val locale = currentLocale()
    val use24Hour = DateFormat.is24HourFormat(context)
    val timeFormat = remember(locale, use24Hour) {
        SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm a", locale)
    }
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, MMMM d", locale) }

    Column(
        modifier            = Modifier.fillMaxSize().padding(start = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text          = timeFormat.format(now),
            color         = contentColor,
            fontSize      = 48.sp,
            fontWeight    = androidx.compose.ui.text.font.FontWeight.Light,
            letterSpacing = 1.sp
        )
        Text(
            text     = dateFormat.format(now),
            color    = subColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AnalogClock(now: Date, accent: Color, isDayMode: Boolean = false) {
    val locale = currentLocale()
    val cal = remember(now, locale) { Calendar.getInstance(locale).apply { time = now } }
    val hour   = cal.get(Calendar.HOUR).toFloat()
    val minute = cal.get(Calendar.MINUTE).toFloat()
    val second = cal.get(Calendar.SECOND).toFloat()

    val shortDateFormat = remember(locale) { SimpleDateFormat("EEE d", locale) }

    val ringColor = widgetLine(isDayMode)
    val tickColor = widgetSubInk(isDayMode)
    val minuteHandColor = widgetInk(isDayMode)
    val pivotBg = if (isDayMode) GruvLightBg2 else GruvDarkBg2

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx     = size.width / 2f
            val cy     = size.height / 2f
            val radius = size.minDimension / 2f * 0.82f

            drawCircle(
                color  = ringColor,
                radius = radius,
                center = Offset(cx, cy),
                style  = Stroke(2.dp.toPx())
            )

            for (i in 0 until 60) {
                val angle = (Math.PI * 2 / 60 * i - Math.PI / 2).toFloat()
                val isHour = i % 5 == 0
                val isQuarter = i % 15 == 0
                val inner = when {
                    isQuarter -> 0.80f
                    isHour    -> 0.85f
                    else      -> 0.90f
                }
                drawLine(
                    color       = when {
                        isQuarter -> accent
                        isHour    -> tickColor
                        else      -> tickColor.copy(alpha = 0.55f)
                    },
                    start       = Offset(cx + cos(angle) * radius * inner, cy + sin(angle) * radius * inner),
                    end         = Offset(cx + cos(angle) * radius * 0.96f, cy + sin(angle) * radius * 0.96f),
                    strokeWidth = if (isQuarter) 2.6.dp.toPx() else 1.6.dp.toPx(),
                    cap         = StrokeCap.Round
                )
            }

            val hAngle = ((hour / 12f + minute / 720f) * 2 * Math.PI - Math.PI / 2).toFloat()
            drawLine(
                color       = accent,
                start       = Offset(cx - cos(hAngle) * radius * 0.14f, cy - sin(hAngle) * radius * 0.14f),
                end         = Offset(cx + cos(hAngle) * radius * 0.50f, cy + sin(hAngle) * radius * 0.50f),
                strokeWidth = 4.dp.toPx(),
                cap         = StrokeCap.Round
            )

            val mAngle = ((minute / 60f) * 2 * Math.PI - Math.PI / 2).toFloat()
            drawLine(
                color       = minuteHandColor,
                start       = Offset(cx - cos(mAngle) * radius * 0.14f, cy - sin(mAngle) * radius * 0.14f),
                end         = Offset(cx + cos(mAngle) * radius * 0.74f, cy + sin(mAngle) * radius * 0.74f),
                strokeWidth = 2.6.dp.toPx(),
                cap         = StrokeCap.Round
            )

            val sAngle = ((second / 60f) * 2 * Math.PI - Math.PI / 2).toFloat()
            drawLine(
                color       = accent.copy(alpha = 0.75f),
                start       = Offset(cx - cos(sAngle) * radius * 0.22f, cy - sin(sAngle) * radius * 0.22f),
                end         = Offset(cx + cos(sAngle) * radius * 0.88f, cy + sin(sAngle) * radius * 0.88f),
                strokeWidth = 1.4.dp.toPx(),
                cap         = StrokeCap.Round
            )

            drawCircle(color = pivotBg, radius = 4.dp.toPx(), center = Offset(cx, cy))
            drawCircle(
                color  = accent,
                radius = 2.5.dp.toPx(),
                center = Offset(cx, cy),
                style  = Stroke(1.dp.toPx())
            )
        }

        Text(
            text      = shortDateFormat.format(now).uppercase(locale),
            color     = widgetSubInk(isDayMode),
            fontSize  = 11.sp,
            letterSpacing = 1.5.sp,
            modifier  = Modifier
                .align(Alignment.Center)
                .padding(bottom = 44.dp)
        )
    }
}

fun clockTimeLabel(now: Date, locale: Locale): String {
    val cal = Calendar.getInstance(locale).apply { time = now }
    return when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> "MORNING"
        in 12..16 -> "AFTERNOON"
        in 17..20 -> "EVENING"
        else      -> "NIGHT"
    }
}
