package com.openlauncher.app.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

// The system home-role dialog exists from API 29. Vendor ROMs sometimes ship
// without it, so both call sites share one fallback chain.
fun homeRoleIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return null
    return runCatching { roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME) }.getOrNull()
}

fun openHomeSettings(context: Context) {
    val opened = runCatching {
        context.startActivity(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess
    if (opened) return
    runCatching {
        context.startActivity(
            Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
