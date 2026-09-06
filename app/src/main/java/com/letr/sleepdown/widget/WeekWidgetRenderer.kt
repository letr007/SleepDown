package com.letr.sleepdown.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.CourseOccurrence
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.WeekPattern
import com.letr.sleepdown.R
import androidx.core.graphics.ColorUtils
import android.graphics.Color
import android.graphics.DashPathEffect
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.WidgetSnapshot
import com.letr.sleepdown.logic.CourseColors
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.sqrt

/** RemoteViews collections carry one scrollable week image, with bounded Binder bitmap memory. */
internal fun renderWeekWidget(context: Context, data: LoadedWidgetData, snapshot: WidgetSnapshot, widgetId: Int): Bitmap {
    val table = data.timetable
    val style = WidgetStyle.read(context, widgetId)
    val days = widgetWeekDays(table, style)
    val nodes = table.timeTable.nodes.sortedBy { it.node }
    val nodeCount = (nodes.maxOfOrNull { it.node } ?: 12).coerceIn(1, 60)
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
    val width = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320) - 24).coerceIn(200, 700).toFloat()
    val textScale = style.headerTextSize / 11f
    val rowHeight = style.rowHeight.toFloat()
    val headerHeight = 40f * textScale
    val height = headerHeight + nodeCount * rowHeight
    val bitmapScale = minOf(2f, sqrt(900_000f / (width * height)))
    val bitmap = Bitmap.createBitmap(ceil(width * bitmapScale).toInt(), ceil(height * bitmapScale).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(bitmapScale, bitmapScale)
    val palette = widgetPalette(context, widgetId, WidgetKind.WEEK)
    val transparent = !WidgetPreferences.showBackground(context, widgetId)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    val columnWidth = width / (days.size + 0.64f)
    val axis = columnWidth * 0.64f
    fun label(value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, center: Boolean = true) {
        text.color = color
        text.textSize = size * textScale
        text.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        text.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        if (transparent) text.setShadowLayer(1.5f, 0f, 1f, android.graphics.Color.BLACK) else text.clearShadowLayer()
        canvas.drawText(value, x, y, text)
    }
    val start = LocalDate.ofEpochDay(snapshot.anchorEpochDay)
    val currentDay = LocalDate.ofEpochDay(data.nowEpochDay).dayOfWeek.value
    val month = if (currentDay in days) start.plusDays(currentDay - 1L).monthValue else start.monthValue
    label(context.getString(R.string.aggregate_month_label, month), axis / 2, 23f * textScale, 11f, palette.primary, true)
    days.forEachIndexed { index, day ->
        val date = start.plusDays(day - 1L)
        val ink = if (day == currentDay) style.textColor else ColorUtils.setAlphaComponent(style.textColor, 0x33)
        val x = axis + (index + 0.5f) * columnWidth
        label(weekdayName(context, day), x, 15f * textScale, 11f, ink)
        label("${date.monthValue}/${date.dayOfMonth}", x, 31f * textScale, 11f, ink)
    }
    repeat(nodeCount) { index ->
        val y = headerHeight + index * rowHeight
        label((index + 1).toString(), axis / 2, y + 15f * textScale, 11f, palette.primary, true)
        nodes.firstOrNull { style.showTimeBar && it.node == index + 1 }?.let { node ->
            label(MinuteOfDay.format(node.startMinuteOfDay), axis / 2, y + 28f * textScale, 7f, palette.secondary)
            label(MinuteOfDay.format(node.endMinuteOfDay), axis / 2, y + 39f * textScale, 7f, palette.secondary)
        }
    }
    if (style.showGrid) {
        paint.color = style.textColor
        paint.strokeWidth = 0.5f
        for (index in 0..nodeCount) canvas.drawLine(axis, headerHeight + index * rowHeight, width, headerHeight + index * rowHeight, paint)
        for (index in 0..days.size) canvas.drawLine(axis + index * columnWidth, headerHeight, axis + index * columnWidth, height, paint)
    }
    val courses = remainingWeekWidgetCourses(data, snapshot.anchorEpochDay, style)
    val occurrences = courses.filterNot { it.otherWeek }.map { it.occurrence }
    val groups = TimetableEngine.conflictGroups(occurrences, table.conflictPreferences)
    val preferred = groups.map { it.preferredOccurrenceId }.toSet()
    val conflicted = groups.filter { it.occurrences.size > 1 }.map { it.preferredOccurrenceId }.toSet()
    fun periodPosition(minute: Int): Float {
        nodes.forEachIndexed { index, node ->
            if (minute <= node.endMinuteOfDay) {
                val duration = (node.endMinuteOfDay - node.startMinuteOfDay).coerceAtLeast(1)
                return index + ((minute - node.startMinuteOfDay).toFloat() / duration).coerceIn(0f, 1f)
            }
        }
        return nodeCount.toFloat()
    }
    courses.sortedBy { if (it.otherWeek) 0 else if (it.occurrence.id in preferred) 2 else 1 }.forEach { entry ->
        val item = entry.occurrence
        val opacity = if (entry.otherWeek) style.otherWeekOpacity / 100f else 1f
        val column = days.indexOf(item.dayOfWeek)
        if (column < 0) return@forEach
        val top = if (item.usesCustomTime) periodPosition(item.startMinuteOfDay).coerceAtMost(nodeCount - 0.5f)
            else (item.startNode - 1).coerceIn(0, nodeCount - 1).toFloat()
        val bottom = if (item.usesCustomTime) periodPosition(item.endMinuteOfDay).coerceAtLeast(top + 0.5f)
            else (item.startNode + item.nodeCount - 1).coerceIn(1, nodeCount).toFloat()
        val rect = RectF(axis + column * columnWidth + 1, headerHeight + top * rowHeight + 1,
            axis + (column + 1) * columnWidth - 1, headerHeight + bottom * rowHeight - 1)
        val fill = if (WidgetPreferences.showColorBlocks(context, widgetId)) {
            if (item.color == 0) CourseColors.colorFor(item.courseName) else item.color or (0xFF shl 24)
        } else palette.background
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
        paint.color = ColorUtils.setAlphaComponent(fill, (style.courseOpacity * 255 / 100 * opacity).toInt())
        canvas.drawRoundRect(rect, style.radius.toFloat(), style.radius.toFloat(), paint)
        val stroke = if (style.strokeColorCompose) ColorUtils.setAlphaComponent(fill, Color.alpha(style.strokeColor)) else style.strokeColor
        paint.color = ColorUtils.setAlphaComponent(stroke, (Color.alpha(stroke) * opacity).toInt())
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        if (style.dottedBorder) paint.pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        canvas.drawRoundRect(rect, style.radius.toFloat(), style.radius.toFloat(), paint)
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
        text.clearShadowLayer()
        val ink = if (style.textColorCompose) ColorUtils.compositeColors(style.courseTextColor, fill) else style.courseTextColor
        text.color = ColorUtils.setAlphaComponent(ink, (Color.alpha(ink) * opacity).toInt())
        text.textAlign = Paint.Align.LEFT
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = style.courseTextSize.toFloat()
        val value = buildString {
            append(weekCourseName(context, entry))
            if (style.showLocation && item.room.isNotBlank()) append("\n@${item.room}")
            if (style.showTeacher && item.teacher.isNotBlank()) append("\n${item.teacher}")
            if (style.showTime || item.usesCustomTime) append("\n${MinuteOfDay.format(item.startMinuteOfDay)}–${MinuteOfDay.format(item.endMinuteOfDay)}")
        }
        val availableWidth = (rect.width() - 6).toInt().coerceAtLeast(1)
        val lineHeight = (text.fontMetrics.descent - text.fontMetrics.ascent).coerceAtLeast(1f)
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, text, availableWidth)
            .setAlignment(if (style.centerHorizontal) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
            .setMaxLines(((rect.height() - 6) / lineHeight).toInt().coerceAtLeast(1))
            .setEllipsize(TextUtils.TruncateAt.END).build()
        canvas.save()
        canvas.clipRect(rect)
        canvas.translate(rect.left + 3, rect.top + if (style.centerVertical) ((rect.height() - layout.height) / 2).coerceAtLeast(4f) else 4f)
        layout.draw(canvas)
        canvas.restore()
        if (item.id in conflicted) {
            paint.color = text.color
            canvas.drawPath(Path().apply {
                moveTo(rect.right - 12f, rect.bottom - 6f)
                lineTo(rect.right - 6f, rect.bottom - 6f)
                lineTo(rect.right - 6f, rect.bottom - 12f)
                close()
            }, paint)
        }
    }
    return bitmap
}

internal fun weekWidgetDescription(context: Context, data: LoadedWidgetData, snapshot: WidgetSnapshot, widgetId: Int = 0): String =
    buildString {
        val style = WidgetStyle.read(context, widgetId)
        append(data.timetable.name)
        remainingWeekWidgetCourses(data, snapshot.anchorEpochDay, style).forEach { entry ->
            val item = entry.occurrence
            append("\n${weekdayName(context, item.dayOfWeek)} ")
            append("${MinuteOfDay.format(item.startMinuteOfDay)}–${MinuteOfDay.format(item.endMinuteOfDay)} ${weekCourseName(context, entry)}")
            if (style.showLocation && item.room.isNotBlank()) append(" ${item.room}")
            val conflictCount = snapshot.items.firstOrNull { it.occurrenceId == item.id }?.conflictCount ?: 1
            if (conflictCount > 1) append(" ${context.getString(R.string.conflict_count, conflictCount)}")
        }
    }

internal data class WeekWidgetCourse(val occurrence: CourseOccurrence, val otherWeek: Boolean, val pattern: WeekPattern)

internal fun widgetWeekDays(table: Timetable, style: WidgetStyle): List<Int> =
    displayedDays(table.copy(showSaturday = style.showSaturday, showSunday = style.showSunday))

internal fun remainingWeekWidgetCourses(data: LoadedWidgetData, weekStart: Long, style: WidgetStyle): List<WeekWidgetCourse> =
    weekWidgetCourses(data.timetable, weekStart, style).filter { entry ->
        // Other-week hints occupy dates in the displayed week, not their future occurrence dates.
        val displayedDate = weekStart + entry.occurrence.dayOfWeek - 1L
        data.isRemainingCourse(displayedDate, entry.occurrence.endMinuteOfDay)
    }

internal fun weekWidgetCourses(table: Timetable, weekStart: Long, style: WidgetStyle): List<WeekWidgetCourse> {
    val range = EpochDayRange(weekStart, weekStart + 6)
    val segments = table.courses.flatMap { it.slots }.flatMap { it.recurrenceSegments }.associateBy { it.id }
    val days = widgetWeekDays(table, style)
    val current = TimetableEngine.expandOccurrences(table, range)
    val week = Math.floorDiv(weekStart - table.firstDayEpochDay, 7L).toInt() + 1
    val future = if (style.showOtherWeekCourses && week in 1 until table.maxWeek) {
        // Active recurrence segments stay suppressed when their occurrence was cancelled or moved.
        val active = TimetableEngine.recurringOccurrences(table, range).map { it.recurrenceSegmentId }.toSet()
        TimetableEngine.expandOccurrences(table, EpochDayRange(weekStart + 7, table.coverageRange.endEpochDay))
            .filter { it.recurrenceSegmentId !in active }
            .distinctBy { it.recurrenceSegmentId }
            .sortedBy { segments[it.recurrenceSegmentId]?.startWeek }
            .map { WeekWidgetCourse(it, true, segments[it.recurrenceSegmentId]?.weekPattern ?: WeekPattern.ALL) }
    } else emptyList()
    return (future + current.map { WeekWidgetCourse(it, false, segments[it.recurrenceSegmentId]?.weekPattern ?: WeekPattern.ALL) })
        .filter { it.occurrence.dayOfWeek in days }
}

private fun weekCourseName(context: Context, entry: WeekWidgetCourse): String = buildString {
    if (entry.otherWeek) append(context.getString(R.string.widget_other_week_prefix))
    append(entry.occurrence.courseName)
    when (entry.pattern) {
        WeekPattern.ODD -> append(context.getString(R.string.odd_week_marker))
        WeekPattern.EVEN -> append(context.getString(R.string.even_week_marker))
        WeekPattern.ALL -> Unit
    }
}
