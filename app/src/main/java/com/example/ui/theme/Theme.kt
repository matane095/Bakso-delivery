package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SambalRed,
    secondary = WarmOrange,
    tertiary = ScallionGreen,
    background = WarmCreamBg,
    surface = CardWarmCream,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = CharcoalText,
    onSurface = CharcoalText,
    primaryContainer = CardWarmCream,
    onPrimaryContainer = SambalRed,
    secondaryContainer = EggGold,
    onSecondaryContainer = CharcoalText,
    outline = SoftGreyBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmOrange,
    secondary = EggGold,
    tertiary = ScallionGreen,
    background = NightDarkBg,
    surface = NightCardBg,
    onPrimary = NightDarkBg,
    onSecondary = NightDarkBg,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = NightSoftText,
    onSurface = NightSoftText,
    primaryContainer = NightCardBg,
    onPrimaryContainer = WarmOrange,
    secondaryContainer = SambalRed,
    onSecondaryContainer = NightSoftText,
    outline = NightCardBg
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our mouth-watering custom branding!
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content
    )
}
