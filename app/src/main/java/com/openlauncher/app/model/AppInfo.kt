package com.openlauncher.app.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
)
