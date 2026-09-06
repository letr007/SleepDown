package com.letr.sleepdown.widget

import android.content.Context
import android.graphics.Color
import java.io.Serializable

internal data class WidgetStyle(
    val showHeader: Boolean = true,
    val showButtons: Boolean = true,
    val showDate: Boolean = true,
    val headerTextSize: Int = 11,
    val courseTextSize: Int = 12,
    val textColor: Int = Color.BLACK,
    val courseTextColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.WHITE,
    val backgroundImage: String = "",
    val backgroundOpacity: Int = 85,
    val courseOpacity: Int = 50,
    val showLocation: Boolean = true,
    val showTeacher: Boolean = true,
    val showTime: Boolean = false,
    val showTimeBar: Boolean = true,
    val rowHeight: Int = 64,
    val radius: Int = 4,
    val centerHorizontal: Boolean = false,
    val centerVertical: Boolean = false,
    val showGrid: Boolean = false,
    val dottedBorder: Boolean = false,
    val strokeColor: Int = 0x80FFFFFF.toInt(),
    val strokeColorCompose: Boolean = false,
    val textColorCompose: Boolean = false,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true,
    val showOtherWeekCourses: Boolean = true,
    val otherWeekOpacity: Int = 50,
) : Serializable {
    fun save(context: Context, widgetId: Int) {
        context.getSharedPreferences("widget_style_$widgetId", Context.MODE_PRIVATE).edit()
            .putBoolean("showHeader", showHeader)
            .putBoolean("showButtons", showButtons)
            .putBoolean("showDate", showDate)
            .putInt("headerTextSize", headerTextSize)
            .putInt("courseTextSize", courseTextSize)
            .putInt("textColor", textColor)
            .putInt("courseTextColor", courseTextColor)
            .putInt("backgroundColor", backgroundColor)
            .putString("backgroundImage", backgroundImage)
            .putInt("backgroundOpacity", backgroundOpacity)
            .putInt("courseOpacity", courseOpacity)
            .putBoolean("showLocation", showLocation)
            .putBoolean("showTeacher", showTeacher)
            .putBoolean("showTime", showTime)
            .putBoolean("showTimeBar", showTimeBar)
            .putInt("rowHeight", rowHeight)
            .putInt("radius", radius)
            .putBoolean("centerHorizontal", centerHorizontal)
            .putBoolean("centerVertical", centerVertical)
            .putBoolean("showGrid", showGrid)
            .putBoolean("dottedBorder", dottedBorder)
            .putInt("strokeColor", strokeColor)
            .putBoolean("strokeColorCompose", strokeColorCompose)
            .putBoolean("textColorCompose", textColorCompose)
            .putBoolean("showSaturday", showSaturday)
            .putBoolean("showSunday", showSunday)
            .putBoolean("showOtherWeekCourses", showOtherWeekCourses)
            .putInt("otherWeekOpacity", otherWeekOpacity)
            .apply()
    }

    companion object {
        fun read(context: Context, widgetId: Int): WidgetStyle {
            val prefs = context.getSharedPreferences("widget_style_$widgetId", Context.MODE_PRIVATE)
            return WidgetStyle(
                showHeader = prefs.getBoolean("showHeader", true),
                showButtons = prefs.getBoolean("showButtons", true),
                showDate = prefs.getBoolean("showDate", true),
                headerTextSize = prefs.getInt("headerTextSize", 11).coerceIn(8, 32),
                courseTextSize = prefs.getInt("courseTextSize", 12).coerceIn(8, 32),
                textColor = prefs.getInt("textColor", Color.BLACK),
                courseTextColor = prefs.getInt("courseTextColor", Color.WHITE),
                backgroundColor = prefs.getInt("backgroundColor", Color.WHITE),
                backgroundImage = prefs.getString("backgroundImage", "").orEmpty(),
                backgroundOpacity = prefs.getInt("backgroundOpacity", 85).coerceIn(0, 100),
                courseOpacity = prefs.getInt("courseOpacity", 50).coerceIn(0, 100),
                showLocation = prefs.getBoolean("showLocation", true),
                showTeacher = prefs.getBoolean("showTeacher", true),
                showTime = prefs.getBoolean("showTime", false),
                showTimeBar = prefs.getBoolean("showTimeBar", true),
                rowHeight = prefs.getInt("rowHeight", 64).coerceIn(32, 128),
                radius = prefs.getInt("radius", 4).coerceIn(0, 32),
                centerHorizontal = prefs.getBoolean("centerHorizontal", false),
                centerVertical = prefs.getBoolean("centerVertical", false),
                showGrid = prefs.getBoolean("showGrid", false),
                dottedBorder = prefs.getBoolean("dottedBorder", false),
                strokeColor = prefs.getInt("strokeColor", 0x80FFFFFF.toInt()),
                strokeColorCompose = prefs.getBoolean("strokeColorCompose", false),
                textColorCompose = prefs.getBoolean("textColorCompose", false),
                showSaturday = prefs.getBoolean("showSaturday", true),
                showSunday = prefs.getBoolean("showSunday", true),
                showOtherWeekCourses = prefs.getBoolean("showOtherWeekCourses", true),
                otherWeekOpacity = prefs.getInt("otherWeekOpacity", 50).coerceIn(0, 100),
            )
        }
    }
}
