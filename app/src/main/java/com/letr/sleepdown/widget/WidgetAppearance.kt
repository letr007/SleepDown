package com.letr.sleepdown.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import com.letr.sleepdown.ui.SleepDownThemeMode
import com.letr.sleepdown.ui.SleepDownUiPreferences

internal data class WidgetPalette(val background: Int, val primary: Int, val secondary: Int, val accent: Int)

internal fun widgetPalette(context: Context, widgetId: Int, kind: WidgetKind): WidgetPalette {
    if (kind == WidgetKind.TODAY || kind == WidgetKind.WEEK) {
        val style = WidgetStyle.read(context, widgetId)
        return WidgetPalette(style.backgroundColor, style.textColor, style.textColor, style.textColor)
    }
    val dark = when (SleepDownUiPreferences.read(context).themeMode) {
        SleepDownThemeMode.SYSTEM -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        SleepDownThemeMode.DARK -> true
        SleepDownThemeMode.LIGHT -> false
    }
    return if (dark) {
        WidgetPalette(Color.rgb(32, 32, 34), Color.WHITE, Color.rgb(215, 215, 218), Color.rgb(255, 107, 133))
    } else {
        WidgetPalette(Color.rgb(250, 250, 250), Color.rgb(32, 33, 36), Color.rgb(112, 112, 115), Color.rgb(220, 42, 80))
    }
}
