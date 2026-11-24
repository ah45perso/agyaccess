package com.agyaccess.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette AgyAccess : sobre, haute lisibilité pour code et streaming
val AgyPrimaryDark = Color(0xFF7CC6FE)
val AgyOnPrimaryDark = Color(0xFF003250)
val AgyPrimaryContainerDark = Color(0xFF004A73)
val AgySecondaryDark = Color(0xFF81E6D9)
val AgySurfaceDark = Color(0xFF0F141A)
val AgySurfaceVariantDark = Color(0xFF1A222C)
val AgyOnSurfaceDark = Color(0xFFE6EDF3)
val AgyErrorDark = Color(0xFFFF7B72)

val AgyPrimaryLight = Color(0xFF005FA3)
val AgyOnPrimaryLight = Color(0xFFFFFFFF)
val AgyPrimaryContainerLight = Color(0xFFD3E9FF)
val AgySecondaryLight = Color(0xFF006B60)
val AgySurfaceLight = Color(0xFFF7F9FC)
val AgySurfaceVariantLight = Color(0xFFE9EFF6)
val AgyOnSurfaceLight = Color(0xFF121820)

val CodeSurfaceColor = Color(0xFF0D1117)
val CodeHeaderColor = Color(0xFF161B22)
val CodeBorderColor = Color(0xFF30363D)
val StatusRunningColor = Color(0xFF3FB950)
val StatusWaitingColor = Color(0xFFD29922)
val StatusErrorColor = Color(0xFFF85149)

private val DarkColorScheme = darkColorScheme(
    primary = AgyPrimaryDark,
    onPrimary = AgyOnPrimaryDark,
    primaryContainer = AgyPrimaryContainerDark,
    secondary = AgySecondaryDark,
    background = AgySurfaceDark,
    surface = AgySurfaceDark,
    surfaceVariant = AgySurfaceVariantDark,
    onSurface = AgyOnSurfaceDark,
    error = AgyErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = AgyPrimaryLight,
    onPrimary = AgyOnPrimaryLight,
    primaryContainer = AgyPrimaryContainerLight,
    secondary = AgySecondaryLight,
    background = AgySurfaceLight,
    surface = AgySurfaceLight,
    surfaceVariant = AgySurfaceVariantLight,
    onSurface = AgyOnSurfaceLight
)

val AgyTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun AgyAccessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgyTypography,
        content = content
    )
}
