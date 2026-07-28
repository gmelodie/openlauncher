package com.openlauncher.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.openlauncher.app.R
import com.openlauncher.app.data.AppFont

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_light,   FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold,    FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold,    FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold,    FontWeight.Bold),
)

val SourceCodePro = FontFamily(
    Font(R.font.source_code_pro_regular, FontWeight.Normal),
    Font(R.font.source_code_pro_bold,    FontWeight.Medium),
    Font(R.font.source_code_pro_bold,    FontWeight.SemiBold),
    Font(R.font.source_code_pro_bold,    FontWeight.Bold),
)

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.SYSTEM          -> FontFamily.Default
    AppFont.JETBRAINS_MONO  -> JetBrainsMono
    AppFont.SOURCE_CODE_PRO -> SourceCodePro
}

// Sizes stay unscaled here. The launcher applies its text scale to the font
// scale of the whole composition, which reaches the many widget labels that set
// a size of their own instead of taking one from this typography.
fun launcherTypography(bold: Boolean, fontFamily: FontFamily = FontFamily.Default): Typography {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    return Typography(
        displayLarge   = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 57.sp),
        displayMedium  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 45.sp),
        headlineLarge  = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = 32.sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = 28.sp),
        headlineSmall  = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = 24.sp),
        titleLarge     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp),
        titleMedium    = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        titleSmall     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        bodyLarge      = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = 16.sp),
        bodyMedium     = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = 14.sp),
        bodySmall      = TextStyle(fontFamily = fontFamily, fontWeight = weight,            fontSize = 12.sp),
        labelLarge     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium    = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )
}
