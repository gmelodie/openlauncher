package com.openlauncher.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.openlauncher.app.ui.theme.DangerRedDay
import com.openlauncher.app.ui.theme.DangerRedNight
import com.openlauncher.app.ui.theme.GruvDarkBg1
import com.openlauncher.app.ui.theme.GruvDarkFg0
import com.openlauncher.app.ui.theme.GruvDarkFg1
import com.openlauncher.app.ui.theme.GruvDarkFg3
import com.openlauncher.app.ui.theme.GruvLightBg1
import com.openlauncher.app.ui.theme.GruvLightFg1
import com.openlauncher.app.ui.theme.GruvLightFg3
import com.openlauncher.app.ui.theme.LocalDayMode

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDayMode = LocalDayMode.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (isDayMode) DangerRedDay else DangerRedNight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDayMode) GruvLightFg3 else GruvDarkFg3)
            }
        },
        containerColor    = if (isDayMode) GruvLightBg1 else GruvDarkBg1,
        titleContentColor = if (isDayMode) GruvLightFg1 else GruvDarkFg0,
        textContentColor  = if (isDayMode) GruvLightFg3 else GruvDarkFg1
    )
}
