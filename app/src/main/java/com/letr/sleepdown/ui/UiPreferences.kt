package com.letr.sleepdown.ui

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.core.Transition
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

enum class SleepDownThemeMode { SYSTEM, LIGHT, DARK }

data class SleepDownUiSettings(
    val themeMode: SleepDownThemeMode = SleepDownThemeMode.SYSTEM,
    val showEmptyImage: Boolean = true,
    val emptyImageUri: String = "",
    val bottomSpacingDp: Int = 48,
)

val LocalSleepDownUiSettings = staticCompositionLocalOf { SleepDownUiSettings() }

object SleepDownUiPreferences {
    private const val PREFS = "sleepdown_ui"
    private const val THEME = "theme"
    private const val SHOW_EMPTY = "show_empty"
    private const val EMPTY_URI = "empty_uri"
    private const val BOTTOM_SPACING = "bottom_spacing"

    fun read(context: Context): SleepDownUiSettings {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return SleepDownUiSettings(
            themeMode = prefs.getString(THEME, null).toEnumOr(SleepDownThemeMode.SYSTEM),
            showEmptyImage = prefs.getBoolean(SHOW_EMPTY, true),
            emptyImageUri = prefs.getString(EMPTY_URI, "").orEmpty(),
            bottomSpacingDp = prefs.getInt(BOTTOM_SPACING, 48).coerceIn(0, 240),
        )
    }

    fun setTheme(context: Context, mode: SleepDownThemeMode) = edit(context) { putString(THEME, mode.name) }
    fun setShowEmptyImage(context: Context, show: Boolean) = edit(context) { putBoolean(SHOW_EMPTY, show) }
    fun setEmptyImageUri(context: Context, uri: String) = edit(context) { putString(EMPTY_URI, uri) }
    fun setBottomSpacing(context: Context, value: Int) = edit(context) { putInt(BOTTOM_SPACING, value.coerceIn(0, 240)) }

    private inline fun edit(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply(block).apply()
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOr(fallback: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: fallback
}

internal fun localizedUiError(context: Context, error: Throwable, @StringRes fallback: Int): String {
    val message = error.message?.trim().orEmpty()
    val containsHan = message.any { it.code in 0x3400..0x9FFF }
    return message.takeIf { it.isNotEmpty() && !containsHan } ?: context.getString(fallback)
}

@Composable
fun rememberSleepDownUiSettings(context: Context = LocalContext.current): State<SleepDownUiSettings> {
    val state = remember(context.applicationContext) { mutableStateOf(SleepDownUiPreferences.read(context)) }
    DisposableEffect(context.applicationContext) {
        val preferences = context.applicationContext.getSharedPreferences("sleepdown_ui", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state.value = SleepDownUiPreferences.read(context)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

private const val COLOR_SCHEME_ANIMATION_MILLIS = 300

@Composable
private fun Transition<ColorScheme>.animateSchemeColor(
    label: String,
    targetValueByState: (ColorScheme) -> Color,
): Color = animateColor(
    transitionSpec = { tween(COLOR_SCHEME_ANIMATION_MILLIS) },
    label = label,
    targetValueByState = { targetValueByState(it) },
).value

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val transition = updateTransition(targetState = target, label = "colorScheme")
    return target.copy(
        primary = transition.animateSchemeColor("primary") { it.primary },
        onPrimary = transition.animateSchemeColor("onPrimary") { it.onPrimary },
        primaryContainer = transition.animateSchemeColor("primaryContainer") { it.primaryContainer },
        onPrimaryContainer = transition.animateSchemeColor("onPrimaryContainer") { it.onPrimaryContainer },
        inversePrimary = transition.animateSchemeColor("inversePrimary") { it.inversePrimary },
        secondary = transition.animateSchemeColor("secondary") { it.secondary },
        onSecondary = transition.animateSchemeColor("onSecondary") { it.onSecondary },
        secondaryContainer = transition.animateSchemeColor("secondaryContainer") { it.secondaryContainer },
        onSecondaryContainer = transition.animateSchemeColor("onSecondaryContainer") { it.onSecondaryContainer },
        tertiary = transition.animateSchemeColor("tertiary") { it.tertiary },
        onTertiary = transition.animateSchemeColor("onTertiary") { it.onTertiary },
        tertiaryContainer = transition.animateSchemeColor("tertiaryContainer") { it.tertiaryContainer },
        onTertiaryContainer = transition.animateSchemeColor("onTertiaryContainer") { it.onTertiaryContainer },
        background = transition.animateSchemeColor("background") { it.background },
        onBackground = transition.animateSchemeColor("onBackground") { it.onBackground },
        surface = transition.animateSchemeColor("surface") { it.surface },
        onSurface = transition.animateSchemeColor("onSurface") { it.onSurface },
        surfaceVariant = transition.animateSchemeColor("surfaceVariant") { it.surfaceVariant },
        onSurfaceVariant = transition.animateSchemeColor("onSurfaceVariant") { it.onSurfaceVariant },
        inverseSurface = transition.animateSchemeColor("inverseSurface") { it.inverseSurface },
        inverseOnSurface = transition.animateSchemeColor("inverseOnSurface") { it.inverseOnSurface },
        error = transition.animateSchemeColor("error") { it.error },
        onError = transition.animateSchemeColor("onError") { it.onError },
        errorContainer = transition.animateSchemeColor("errorContainer") { it.errorContainer },
        onErrorContainer = transition.animateSchemeColor("onErrorContainer") { it.onErrorContainer },
        outline = transition.animateSchemeColor("outline") { it.outline },
        outlineVariant = transition.animateSchemeColor("outlineVariant") { it.outlineVariant },
        scrim = transition.animateSchemeColor("scrim") { it.scrim },
        surfaceTint = transition.animateSchemeColor("surfaceTint") { it.surfaceTint },
        surfaceBright = transition.animateSchemeColor("surfaceBright") { it.surfaceBright },
        surfaceDim = transition.animateSchemeColor("surfaceDim") { it.surfaceDim },
        surfaceContainer = transition.animateSchemeColor("surfaceContainer") { it.surfaceContainer },
        surfaceContainerHigh = transition.animateSchemeColor("surfaceContainerHigh") { it.surfaceContainerHigh },
        surfaceContainerLow = transition.animateSchemeColor("surfaceContainerLow") { it.surfaceContainerLow },
        surfaceContainerHighest = transition.animateSchemeColor("surfaceContainerHighest") { it.surfaceContainerHighest },
        surfaceContainerLowest = transition.animateSchemeColor("surfaceContainerLowest") { it.surfaceContainerLowest },
    )
}

@Composable
fun SleepDownAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = rememberSleepDownUiSettings(context).value
    val dark = when (settings.themeMode) {
        SleepDownThemeMode.SYSTEM -> isSystemInDarkTheme()
        SleepDownThemeMode.LIGHT -> false
        SleepDownThemeMode.DARK -> true
    }
    val targetColorScheme = remember(dark) { if (dark) {
        darkColorScheme(
            primary = Color(0xFFFF6B85),
            onPrimary = Color.Black,
            background = Color(0xFF151419),
            surface = Color(0xFF211F25),
            onSurface = Color(0xFFF3EFF5),
            onSurfaceVariant = Color(0xFFC9C2CC),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFFF2D55),
            onPrimary = Color.White,
            background = Color(0xFFF7F7F7),
            surface = Color.White,
            onSurface = Color(0xFF141414),
            onSurfaceVariant = Color(0xFF626466),
        )
    }
    }
    val colorScheme = animateColorScheme(targetColorScheme)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalSleepDownUiSettings provides settings,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
