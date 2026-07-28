package com.openlauncher.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
                Text(confirmLabel, color = if (isDayMode) Color(0xFFC0392B) else Color(0xFFFF5252))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDayMode) GruvLightFg3 else Color(0xFFAAAAAA))
            }
        },
        containerColor    = if (isDayMode) GruvLightBg1 else Color(0xFF1A1A1A),
        titleContentColor = if (isDayMode) GruvLightFg1 else Color.White,
        textContentColor  = if (isDayMode) GruvLightFg3 else Color(0xFFCCCCCC)
    )
}
