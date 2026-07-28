package com.openlauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.openlauncher.app.ui.theme.GruvDarkBg1
import com.openlauncher.app.ui.theme.GruvDarkBg3
import com.openlauncher.app.ui.theme.GruvDarkFg0
import com.openlauncher.app.ui.theme.GruvDarkFg1
import com.openlauncher.app.ui.theme.GruvDarkFg3
import com.openlauncher.app.ui.theme.GruvLightBg1
import com.openlauncher.app.ui.theme.GruvLightBg3
import com.openlauncher.app.ui.theme.GruvLightFg1
import com.openlauncher.app.ui.theme.GruvLightFg3
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.accentPresetLabels
import com.openlauncher.app.ui.theme.accentPresets

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Seed HSV state from the initial color synchronously — initializing in a
    // LaunchedEffect made the sliders flash wrong positions on the first frame
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    val isDayMode = LocalDayMode.current
    val surface     = if (isDayMode) GruvLightBg1 else GruvDarkBg1
    val titleColor  = if (isDayMode) GruvLightFg1 else GruvDarkFg0
    val bodyColor   = if (isDayMode) GruvLightFg3 else GruvDarkFg1
    val labelColor  = if (isDayMode) GruvLightFg3 else GruvDarkFg3
    val subLabelColor = if (isDayMode) GruvLightFg3 else GruvDarkFg3
    val outline     = if (isDayMode) GruvLightBg3 else GruvDarkBg3
    val swatchEdge  = if (isDayMode) GruvLightFg1 else GruvDarkFg0

    var selectedColor by remember { mutableStateOf(initialColor) }
    var hue   by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat   by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun rebuildColor() {
        selectedColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }

    fun syncFrom(color: Color) {
        selectedColor = color
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor    = surface,
        titleContentColor = titleColor,
        textContentColor  = bodyColor,
        title = { Text(title) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Preset swatches
                Text("Presets", style = MaterialTheme.typography.labelMedium, color = labelColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accentPresets.forEachIndexed { i, color ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedColor == color) swatchEdge else outline,
                                        shape = CircleShape
                                    )
                                    .clickable { syncFrom(color) }
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(accentPresetLabels[i], style = MaterialTheme.typography.labelSmall, color = subLabelColor, fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp))
                        }
                    }
                }

                HorizontalDivider(color = outline)

                // Custom HSV sliders
                Text("Custom", style = MaterialTheme.typography.labelMedium, color = labelColor)

                // Hue slider
                Text("Hue", style = MaterialTheme.typography.labelSmall, color = subLabelColor)
                Slider(
                    value = hue / 360f,
                    onValueChange = { hue = it * 360f; rebuildColor() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(
                            colors = (0..6).map { i ->
                                Color(android.graphics.Color.HSVToColor(floatArrayOf(i * 60f, 1f, 1f)))
                            }
                        )),
                    colors = SliderDefaults.colors(
                        thumbColor = swatchEdge,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )

                // Saturation slider
                Text("Saturation", style = MaterialTheme.typography.labelSmall, color = subLabelColor)
                Slider(
                    value = sat,
                    onValueChange = { sat = it; rebuildColor() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value)))
                        ))),
                    colors = SliderDefaults.colors(
                        thumbColor = swatchEdge,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )

                // Brightness slider
                Text("Brightness", style = MaterialTheme.typography.labelSmall, color = subLabelColor)
                Slider(
                    value = value,
                    onValueChange = { value = it; rebuildColor() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(
                            Color.Black,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))
                        ))),
                    colors = SliderDefaults.colors(
                        thumbColor = swatchEdge,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )

                // Preview swatch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedColor)
                )
            }
        },
        confirmButton = {
            // Filled with the chosen color + auto-contrast label, so Apply stays
            // visible no matter how dark or light the selection is
            Button(
                onClick = { onColorSelected(selectedColor); onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedColor,
                    contentColor   = if (selectedColor.luminance() > 0.5f) Color.Black else Color.White
                )
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = bodyColor) }
        }
    )
}
