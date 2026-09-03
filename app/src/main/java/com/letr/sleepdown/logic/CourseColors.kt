package com.letr.sleepdown.logic

import androidx.compose.ui.graphics.Color
import com.letr.sleepdown.shared.SleepDownPalette

/** Shared WakeUp-style course colors. */
object CourseColors {
    fun colorFor(name: String): Int = SleepDownPalette.colorFor(name)

    fun all(): List<Int> = SleepDownPalette.colors

    fun asColor(argb: Int): Color = Color(argb)

    fun textColorFor(@Suppress("UNUSED_PARAMETER") argb: Int): Color = Color.White
}
