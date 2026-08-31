package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DefaultLightScheme = lightColorScheme(
    primary           = MemoirViolet,
    onPrimary         = Color.White,
    primaryContainer  = MemoirVioletLight,
    onPrimaryContainer = MemoirViolet,
    secondary         = MemoirPink,
    onSecondary       = Color.White,
    secondaryContainer = MemoirPinkLight,
    onSecondaryContainer = MemoirPink,
    tertiary          = Color(0xFF8B5CF6),
    onTertiary        = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = LightSurfaceVariant,
    onSurfaceVariant  = Color(0xFF6B7280),
    outline           = LightOutline,
    outlineVariant    = Color(0xFFE5E7EB)
)

private val DefaultDarkScheme = darkColorScheme(
    primary           = Color(0xFFA78BFA),
    onPrimary         = Color(0xFF1A0050),
    primaryContainer  = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary         = Color(0xFFF9A8D4),
    onSecondary       = Color(0xFF3D0020),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary          = Color(0xFFC4B5FD),
    onTertiary        = Color(0xFF200060),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    onSurfaceVariant  = Color(0xFF9CA3AF),
    outline           = DarkOutline,
    outlineVariant    = Color(0xFF2A2A3A)
)


private val OceanLightScheme = lightColorScheme(
    primary           = OceanPrimary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary         = Color(0xFF0EA5E9),
    onSecondary       = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFEFF6FF),
    outline           = LightOutline
)
private val OceanDarkScheme = darkColorScheme(
    primary           = OceanPrimaryDark,
    onPrimary         = Color(0xFF003280),
    primaryContainer  = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary         = Color(0xFF7DD3FC),
    onSecondary       = Color(0xFF003A6E),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    outline           = DarkOutline
)


private val EmeraldLightScheme = lightColorScheme(
    primary           = EmeraldPrimary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary         = Color(0xFF10B981),
    onSecondary       = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFF0FDF4),
    outline           = LightOutline
)
private val EmeraldDarkScheme = darkColorScheme(
    primary           = EmeraldPrimaryDark,
    onPrimary         = Color(0xFF002D1E),
    primaryContainer  = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary         = Color(0xFF6EE7B7),
    onSecondary       = Color(0xFF002D1E),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    outline           = DarkOutline
)


private val SunsetLightScheme = lightColorScheme(
    primary           = SunsetPrimary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary         = Color(0xFFF59E0B),
    onSecondary       = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFFFFBEB),
    outline           = LightOutline
)
private val SunsetDarkScheme = darkColorScheme(
    primary           = SunsetPrimaryDark,
    onPrimary         = Color(0xFF3D1A00),
    primaryContainer  = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary         = Color(0xFFFCD34D),
    onSecondary       = Color(0xFF3D2400),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    outline           = DarkOutline
)


private val RoseLightScheme = lightColorScheme(
    primary           = RosePrimary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF831843),
    secondary         = Color(0xFFEC4899),
    onSecondary       = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFFDF2F8),
    outline           = LightOutline
)
private val RoseDarkScheme = darkColorScheme(
    primary           = RosePrimaryDark,
    onPrimary         = Color(0xFF3D0020),
    primaryContainer  = Color(0xFF9D174D),
    onPrimaryContainer = Color(0xFFFCE7F3),
    secondary         = Color(0xFFF9A8D4),
    onSecondary       = Color(0xFF3D0020),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    outline           = DarkOutline
)


private val MonoLightScheme = lightColorScheme(
    primary           = MonoPrimary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFF3F4F6),
    onPrimaryContainer = Color(0xFF111827),
    secondary         = Color(0xFF6B7280),
    onSecondary       = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFF9FAFB),
    outline           = Color(0xFFD1D5DB)
)
private val MonoDarkScheme = darkColorScheme(
    primary           = MonoPrimaryDark,
    onPrimary         = Color(0xFF111827),
    primaryContainer  = Color(0xFF374151),
    onPrimaryContainer = Color(0xFFF9FAFB),
    secondary         = Color(0xFF9CA3AF),
    onSecondary       = Color(0xFF111827),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = Color(0xFF1F2937),
    outline           = Color(0xFF374151)
)


private val NavyLightScheme = lightColorScheme(
    primary           = NavyPrimary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary         = Color(0xFF3B82F6),
    onSecondary       = Color.White,
    background        = LightBackground,
    onBackground      = LightOnBackground,
    surface           = LightSurface,
    onSurface         = LightOnSurface,
    surfaceVariant    = Color(0xFFEFF6FF),
    outline           = LightOutline
)
private val NavyDarkScheme = darkColorScheme(
    primary           = NavyPrimaryDark,
    onPrimary         = Color(0xFF172554),
    primaryContainer  = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary         = Color(0xFF60A5FA),
    onSecondary       = Color(0xFF172554),
    background        = DarkBackground,
    onBackground      = DarkOnBackground,
    surface           = DarkSurface,
    onSurface         = DarkOnSurface,
    surfaceVariant    = DarkSurfaceVariant,
    outline           = DarkOutline
)


private fun getLightScheme(colorTheme: String) = when (colorTheme) {
    "MONOCHROME"  -> MonoLightScheme
    "NAVY"        -> NavyLightScheme
    "DEFAULT_ROSE" -> RoseLightScheme
    "OCEAN"       -> OceanLightScheme
    "EMERALD"     -> EmeraldLightScheme
    "SUNSET"      -> SunsetLightScheme
    "PURPLE"      -> lightColorScheme(
        primary           = Color(0xFF7C3AED),
        onPrimary         = Color.White,
        primaryContainer  = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF4C1D95),
        secondary         = Color(0xFFA855F7),
        onSecondary       = Color.White,
        background        = LightBackground,
        onBackground      = LightOnBackground,
        surface           = LightSurface,
        onSurface         = LightOnSurface,
        surfaceVariant    = Color(0xFFF5F3FF),
        outline           = LightOutline
    )
    else -> DefaultLightScheme
}

private fun getDarkScheme(colorTheme: String) = when (colorTheme) {
    "MONOCHROME"  -> MonoDarkScheme
    "NAVY"        -> NavyDarkScheme
    "DEFAULT_ROSE" -> RoseDarkScheme
    "OCEAN"       -> OceanDarkScheme
    "EMERALD"     -> EmeraldDarkScheme
    "SUNSET"      -> SunsetDarkScheme
    "PURPLE"      -> darkColorScheme(
        primary           = Color(0xFFC4B5FD),
        onPrimary         = Color(0xFF2D0070),
        primaryContainer  = Color(0xFF4C1D95),
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary         = Color(0xFFE879F9),
        onSecondary       = Color(0xFF3A0050),
        background        = DarkBackground,
        onBackground      = DarkOnBackground,
        surface           = DarkSurface,
        onSurface         = DarkOnSurface,
        surfaceVariant    = DarkSurfaceVariant,
        outline           = DarkOutline
    )
    else -> DefaultDarkScheme
}


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: String = "DEFAULT",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkScheme(colorTheme)
        else      -> getLightScheme(colorTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
