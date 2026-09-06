package com.letr.sleepdown.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun SleepDownAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = rememberSleepDownUiSettings(context).value
    val dark = when (settings.themeMode) {
        SleepDownThemeMode.SYSTEM -> isSystemInDarkTheme()
        SleepDownThemeMode.LIGHT -> false
        SleepDownThemeMode.DARK -> true
    }
    val colorScheme = if (dark) {
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
    CompositionLocalProvider(
        LocalContext provides context,
        LocalSleepDownUiSettings provides settings,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
