package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk

@Composable
fun WeatherWidget(
    state: WeatherState?,
    accent: Color,
    metric: Boolean,
    error: String? = null,
    // City behind an IP-address lookup. Null while a GPS fix drives the reading.
    place: String? = null,
    isDayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = widgetInk(isDayMode)
    val subColor     = widgetSubInk(isDayMode)

    Box(modifier = modifier) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(start = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            if (state != null) {
                Text(text = state.conditionIcon, fontSize = 34.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text          = state.temperatureDisplay(metric),
                    color         = contentColor,
                    fontSize      = 32.sp,
                    fontWeight    = FontWeight.Light,
                    letterSpacing = 1.sp
                )
                Text(
                    text          = state.conditionLabel.uppercase(),
                    color         = subColor,
                    fontSize      = 12.sp,
                    letterSpacing = 1.sp
                )
                if (place != null) {
                    Text(
                        text          = "≈ ${place.uppercase()}",
                        color         = subColor,
                        fontSize      = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
                return@Column
            }
            // The cell used to stay blank on a failed request, which reads the same
            // as "no data yet".
            Text(text = "—", color = subColor, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Text(
                text          = if (error != null) "WEATHER UNAVAILABLE" else "LOADING",
                color         = subColor,
                fontSize      = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
