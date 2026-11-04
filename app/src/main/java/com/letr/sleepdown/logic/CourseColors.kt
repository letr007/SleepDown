package com.letr.sleepdown.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.letr.sleepdown.shared.SleepDownPalette

/** Shared WakeUp-style course colors. */
object CourseColors {
    fun colorFor(name: String): Int = SleepDownPalette.colorFor(name)

    fun all(): List<Int> = SleepDownPalette.colors

    /** Hue families, each ordered deep → vivid → soft. */
    fun families(): List<List<Int>> = SleepDownPalette.families

    fun asColor(argb: Int): Color = Color(argb)

    /** Foreground for a swatch, picked by WCAG contrast against the swatch itself. */
    fun onColor(argb: Int): Color {
        val luminance = asColor(argb).luminance()
        val againstWhite = 1.05f / (luminance + 0.05f)
        val againstBlack = (luminance + 0.05f) / 0.05f
        return if (againstBlack >= againstWhite) Color(0xFF101010) else Color.White
    }
}
