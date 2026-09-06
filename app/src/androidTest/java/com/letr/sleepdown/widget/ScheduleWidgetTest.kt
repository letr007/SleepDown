package com.letr.sleepdown.widget

import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.After
import com.letr.sleepdown.domain.*
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleWidgetTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private val widgetId = 900001
    private val otherWidgetId = 900002
    private val monday = LocalDate.of(2026, 8, 31).toEpochDay()

    @After
    fun clearPreferences() {
        WidgetPreferences.clear(context, intArrayOf(widgetId, otherWidgetId))
    }

    @Test
    fun allWidgetLayoutsCanBeInflatedByRemoteViews() {
        instrumentation.runOnMainSync {
            listOf(
                R.layout.widget_today_compact,
                R.layout.widget_today,
                R.layout.widget_today_next,
                R.layout.widget_week,
                R.layout.widget_course_item,
                R.layout.widget_course_classic_item,
                R.layout.widget_week_grid_item,
                R.layout.widget_today_modern,
                R.layout.widget_week_day_item,
                R.layout.widget_loading,
                R.layout.widget_preview_classic,
                R.layout.widget_preview_compact,
                R.layout.widget_preview_wide,
                R.layout.widget_preview_two_days,
                R.layout.widget_preview_week,
            ).forEach { layout ->
                assertNotNull(RemoteViews(context.packageName, layout).apply(context, FrameLayout(context)))
            }
        }
    }

    @Test
    fun widgetStylesApplyToEveryProviderLayout() {
        instrumentation.runOnMainSync {
            listOf(true, false).forEach { background ->
                WidgetPreferences.setShowBackground(context, widgetId, background)
                WidgetKind.entries.forEach { kind ->
                    val views = RemoteViews(context.packageName, layoutFor(kind))
                    applyWidgetStyle(context, views, widgetId, kind)
                    assertNotNull(views.apply(context, FrameLayout(context)))
                }
            }
        }
    }

    @Test
    fun dailyWidgetsShowRemainingTodayAndKeepAllTomorrowCourses() {
        val table = timetable(listOf(course("Today", 1), course("Tomorrow", 2)))
        for (minute in listOf(479, 539, 540, 541, 1439)) {
            val data = LoadedWidgetData(table, WidgetSnapshotBuilder.buildAll(table, monday, minute), monday, minute)
            for (kind in listOf(WidgetKind.NEXT, WidgetKind.TODAY, WidgetKind.TODAY_MODERN, WidgetKind.TODAY_AND_NEXT_DAY)) {
                assertEquals("$kind at $minute", if (minute <= 540) 1 else 0,
                    buildEntries(context, data, widgetId, kind, 0).size)
            }
            for (kind in listOf(WidgetKind.TODAY, WidgetKind.TODAY_AND_NEXT_DAY)) {
                val tomorrow = buildEntries(context, data, widgetId, kind, 1)
                    .filterIsInstance<WidgetEntry.Course>().single().item
                assertEquals("Tomorrow", tomorrow.courseName)
            }
            assertEquals(2, table.courses.size)
            assertEquals(1, data.snapshots.today.items.size)
        }
    }

    @Test
    fun weekWidgetsFilterByDisplayedDateIncludingOtherWeekHints() {
        val futureHint = course("OtherToday", 3).let { item -> item.copy(slots = item.slots.map { slot ->
            slot.copy(recurrenceSegments = listOf(RecurrenceSegment("other", startWeek = 2, endWeek = 2)))
        }) }
        val table = timetable(listOf(course("Past", 1), course("Today", 3), course("Tomorrow", 4), futureHint))
        for (minute in listOf(539, 540, 541)) {
            val data = LoadedWidgetData(table, WidgetSnapshotBuilder.buildAll(table, monday + 2, minute), monday + 2, minute)
            val expected = if (minute <= 540) setOf("Today", "Tomorrow", "OtherToday") else setOf("Tomorrow")
            for (sundayFirst in listOf(false, true)) {
                val ordered = data.copy(timetable = table.copy(sundayFirst = sundayFirst))
                assertEquals(expected, remainingWeekWidgetCourses(ordered, monday, WidgetStyle())
                    .map { it.occurrence.courseName }.toSet())
            }
            assertEquals(table.courses.map { it.name }.toSet(),
                remainingWeekWidgetCourses(data, monday + 7, WidgetStyle()).map { it.occurrence.courseName }.toSet())
            assertTrue(remainingWeekWidgetCourses(data, monday - 7, WidgetStyle()).isEmpty())
            val entries = buildEntries(context, data, widgetId, WidgetKind.WEEK, 0).filterIsInstance<WidgetEntry.Day>()
            assertTrue(entries.single { it.date.toEpochDay() == monday }.courses.isEmpty())
        }
    }

    @Test
    fun finishedWeekCoursesDisappearFromBothBitmapAndAccessibilityText() {
        val table = timetable(listOf(course("Finished", 1)))
        val data = LoadedWidgetData(table, WidgetSnapshotBuilder.buildAll(table, monday, 541), monday, 541)
        val snapshot = selectedWeekSnapshot(context, data, widgetId)
        val emptyTable = table.copy(courses = emptyList())
        val emptyData = data.copy(timetable = emptyTable,
            snapshots = WidgetSnapshotBuilder.buildAll(emptyTable, monday, 541))
        val actual = renderWeekWidget(context, data, snapshot, widgetId)
        val empty = renderWeekWidget(context, emptyData, selectedWeekSnapshot(context, emptyData, widgetId), widgetId)
        try {
            assertTrue(actual.sameAs(empty))
            assertFalse(weekWidgetDescription(context, data, snapshot, widgetId).contains("Finished"))
            assertEquals(1, table.courses.size)
        } finally {
            actual.recycle()
            empty.recycle()
        }
    }

    @Test
    fun weekGridBitmapIsBoundedAndHasAccessibleCourseText() {
        val data = loaded(timetable(listOf(course("Math", 1))), monday)
        val snapshot = selectedWeekSnapshot(context, data, widgetId)
        val bitmap = renderWeekWidget(context, data, snapshot, widgetId)
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        assertTrue(bitmap.allocationByteCount < 4_000_000)
        assertTrue(weekWidgetDescription(context, data, snapshot).contains("Math"))
        bitmap.recycle()
    }

    @Test
    fun classicStyleControlsPersistPerWidgetAndClearWithTheInstance() {
        val style = WidgetStyle(showHeader = false, showButtons = false, showDate = false,
            headerTextSize = 24, courseTextSize = 32, backgroundOpacity = 30,
            courseOpacity = 80, showTeacher = false, rowHeight = 96, radius = 12)
        style.save(context, widgetId)
        assertEquals(style, WidgetStyle.read(context, widgetId))
        assertEquals(WidgetStyle(), WidgetStyle.read(context, otherWidgetId))
        instrumentation.runOnMainSync {
            for (kind in listOf(WidgetKind.TODAY, WidgetKind.WEEK)) {
                val views = RemoteViews(context.packageName, layoutFor(kind))
                applyWidgetStyle(context, views, widgetId, kind)
                val root = views.apply(context, FrameLayout(context))
                assertEquals(android.view.View.GONE, root.findViewById<android.view.View>(R.id.widget_header).visibility)
                assertEquals(26f * context.resources.displayMetrics.scaledDensity,
                    root.findViewById<android.widget.TextView>(R.id.widget_date).textSize, 1f)
                assertEquals(76, root.findViewById<android.widget.ImageView>(R.id.widget_background).imageAlpha)
            }
        }
        WidgetPreferences.clear(context, intArrayOf(widgetId))
        assertEquals(WidgetStyle(), WidgetStyle.read(context, widgetId))
    }

    @Test
    fun clearedStyleIsVisibleToExistingReadersAndPersistsAfterQueuedWrites() {
        val preferences = context.getSharedPreferences("widget_style_$widgetId", android.content.Context.MODE_PRIVATE)
        repeat(20) { index ->
            WidgetStyle(headerTextSize = 8 + index).save(context, widgetId)
            WidgetPreferences.clear(context, intArrayOf(widgetId))
            assertTrue(preferences.all.isEmpty())
            assertEquals(WidgetStyle(), WidgetStyle.read(context, widgetId))
        }
        assertTrue(preferences.edit().commit())
        assertTrue(context.deleteSharedPreferences("widget_style_$widgetId"))
        assertEquals(WidgetStyle(), WidgetStyle.read(context, widgetId))
    }

    @Test
    fun classicHeaderKeepsTitleAndNavigationSeparateAtLargeTextSizes() {
        instrumentation.runOnMainSync {
            for (size in listOf(11, 24, 32)) for (day in 0..1) {
                WidgetStyle(headerTextSize = size).save(context, widgetId)
                WidgetPreferences.setDayOffset(context, widgetId, day)
                val views = RemoteViews(context.packageName, layoutFor(WidgetKind.TODAY))
                applyWidgetStyle(context, views, widgetId, WidgetKind.TODAY)
                views.setTextViewText(R.id.widget_date, "9月5日")
                views.setTextViewText(R.id.widget_table, "这是一张名字特别长的课表")
                views.setTextViewText(R.id.widget_week, "| 第1周 周六")
                val root = views.apply(context, FrameLayout(context))
                val width = (250 * context.resources.displayMetrics.density).toInt()
                root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                val title = root.findViewById<android.view.View>(R.id.widget_table)
                val week = root.findViewById<android.view.View>(R.id.widget_week)
                val settings = root.findViewById<android.view.View>(R.id.widget_settings)
                val weekBounds = android.graphics.Rect()
                val settingsBounds = android.graphics.Rect()
                week.getGlobalVisibleRect(weekBounds)
                settings.getGlobalVisibleRect(settingsBounds)
                assertTrue(title.measuredWidth > 0 && week.measuredWidth > 0)
                assertTrue(weekBounds.right <= settingsBounds.left)
                assertEquals(if (day == 0) android.view.View.VISIBLE else android.view.View.INVISIBLE,
                    root.findViewById<android.view.View>(R.id.widget_next).visibility)
                assertEquals(if (day == 1) android.view.View.VISIBLE else android.view.View.INVISIBLE,
                    root.findViewById<android.view.View>(R.id.widget_previous).visibility)
            }
        }
        WidgetPreferences.setDayOffset(context, widgetId, -5)
        assertEquals(0, WidgetPreferences.dayOffset(context, widgetId))
    }

    @Test
    fun modernHeaderHasNoNavigationButtonsAndIgnoresClassicStyling() {
        WidgetStyle(showHeader = false, headerTextSize = 32).save(context, widgetId)
        instrumentation.runOnMainSync {
            for (kind in listOf(WidgetKind.NEXT, WidgetKind.TODAY_MODERN, WidgetKind.TODAY_AND_NEXT_DAY)) {
                val views = RemoteViews(context.packageName, layoutFor(kind))
                applyWidgetStyle(context, views, widgetId, kind)
                val root = views.apply(context, FrameLayout(context))
                assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_header).visibility)
                assertEquals(null, root.findViewById<android.view.View>(R.id.widget_settings))
                assertEquals(null, root.findViewById<android.view.View>(R.id.widget_previous))
                assertEquals(if (kind == WidgetKind.NEXT) android.view.View.GONE else android.view.View.VISIBLE,
                    root.findViewById<android.view.View>(R.id.widget_week_count).visibility)
            }
        }
    }

    @Test
    fun overlappingWeekCoursesKeepFullWidthAndShowACornerMarker() {
        WidgetStyle(courseOpacity = 100).save(context, widgetId)
        val first = course("A", 1).let { it.copy(color = android.graphics.Color.RED,
            slots = it.slots.map { slot -> slot.copy(customTime = null) }) }
        val second = course("B", 1).let { it.copy(color = android.graphics.Color.RED,
            slots = it.slots.map { slot -> slot.copy(customTime = null) }) }
        fun corner(courses: List<Course>): Int {
            val data = loaded(timetable(courses), monday)
            val snapshot = selectedWeekSnapshot(context, data, widgetId)
            val bitmap = renderWeekWidget(context, data, snapshot, widgetId)
            val columnWidth = 296f / (displayedDays(data.timetable).size + 0.64f)
            val x = columnWidth * 1.64f - 1f - 8f
            val y = 40f + 64f - 1f - 7f
            val pixel = bitmap.getPixel((bitmap.width * x / 296f).toInt(), (bitmap.height * y / 104f).toInt())
            bitmap.recycle()
            return pixel
        }
        assertEquals(android.graphics.Color.RED, corner(listOf(first)))
        assertEquals(android.graphics.Color.WHITE, corner(listOf(first, second)))
        val data = loaded(timetable(listOf(first, second)), monday)
        val description = weekWidgetDescription(context, data, selectedWeekSnapshot(context, data, widgetId))
        assertTrue(description.contains("A") && description.contains("B"))
        assertTrue(description.contains(context.getString(R.string.conflict_count, 2)))
    }

    @Test
    fun customTimeUsesTheMatchingPeriodRatherThanItsStoredNode() {
        WidgetStyle(courseOpacity = 100).save(context, widgetId)
        val item = course("Custom", 1).let { it.copy(color = android.graphics.Color.RED,
            slots = it.slots.map { slot -> slot.copy(customTime = MinuteRange(600, 650)) }) }
        val table = timetable(listOf(item)).copy(timeTable = TimeTable("day", "Day",
            listOf(TimeTableNode(1, 480, 530), TimeTableNode(2, 600, 650))))
        val data = loaded(table, monday)
        val bitmap = renderWeekWidget(context, data, selectedWeekSnapshot(context, data, widgetId), widgetId)
        val axis = 296f * 0.64f / (displayedDays(data.timetable).size + 0.64f)
        val x = (bitmap.width * (axis + 7f) / 296f).toInt()
        assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(x, (bitmap.height * 70f / 168f).toInt())))
        assertEquals(android.graphics.Color.RED, bitmap.getPixel(x, (bitmap.height * 130f / 168f).toInt()))
        bitmap.recycle()
    }

    @Test
    fun otherWeekCoursesShowFutureSegmentsButNotPastOrCancelledCurrentOccurrences() {
        val future = course("Future", 2).let { it.copy(slots = it.slots.map { slot -> slot.copy(
            recurrenceSegments = listOf(RecurrenceSegment("future", startWeek = 3, endWeek = 4))) }) }
        val past = course("Past", 3).let { it.copy(slots = it.slots.map { slot -> slot.copy(
            recurrenceSegments = listOf(RecurrenceSegment("past", endWeek = 1))) }) }
        val table = timetable(listOf(course("Current", 1), future, past)).copy(maxWeek = 4)
        val entries = weekWidgetCourses(table, monday + 7, WidgetStyle())
        assertEquals(setOf("Current", "Future"), entries.map { it.occurrence.courseName }.toSet())
        assertTrue(entries.single { it.occurrence.courseName == "Future" }.otherWeek)
        assertFalse(entries.single { it.occurrence.courseName == "Current" }.otherWeek)
        assertEquals(listOf("Current"), weekWidgetCourses(table, monday + 7, WidgetStyle(showOtherWeekCourses = false)).map { it.occurrence.courseName })
        val cancelled = TimetableCommands.apply(table, TimetableCommand.DeleteOccurrence("Current-slot", monday + 7))
        assertEquals(listOf("Future"), weekWidgetCourses(cancelled, monday + 7, WidgetStyle()).map { it.occurrence.courseName })
        assertTrue(weekWidgetCourses(table, monday - 7, WidgetStyle()).isEmpty())
        assertTrue(weekWidgetCourses(table, monday + 28, WidgetStyle()).isEmpty())
    }

    @Test
    fun otherWeekCoursesApplyFutureDateExceptions() {
        val future = course("Future", 2).let { it.copy(slots = it.slots.map { slot -> slot.copy(
            recurrenceSegments = listOf(RecurrenceSegment("future", startWeek = 3, endWeek = 3))) }) }
        val table = timetable(listOf(future)).copy(maxWeek = 4)
        val cancelled = TimetableCommands.apply(table, TimetableCommand.DeleteOccurrence("Future-slot", monday + 15))
        assertTrue(weekWidgetCourses(cancelled, monday + 7, WidgetStyle()).isEmpty())
        val moved = TimetableCommands.apply(table, TimetableCommand.RescheduleOccurrence(
            logicalSlotId = "Future-slot", originalEpochDay = monday + 15, targetEpochDay = monday + 16,
            targetStartNode = 1, targetCustomTime = MinuteRange(600, 660)))
        val entry = weekWidgetCourses(moved, monday + 7, WidgetStyle()).single()
        assertTrue(entry.otherWeek)
        assertEquals(3, entry.occurrence.dayOfWeek)
        assertEquals(600, entry.occurrence.startMinuteOfDay)
    }

    @Test
    fun weekGridAndWeekendVisibilityUseTheInstanceStyle() {
        val table = timetable(listOf(course("Saturday", 6), course("Sunday", 7)))
        val style = WidgetStyle(showSaturday = false, showSunday = false, showGrid = true,
            dottedBorder = true, textColor = android.graphics.Color.GREEN,
            strokeColor = 0x60FF0000, strokeColorCompose = true, textColorCompose = true, otherWeekOpacity = 20)
        style.save(context, widgetId)
        assertEquals(style, WidgetStyle.read(context, widgetId))
        assertEquals((1..5).toList(), widgetWeekDays(table, style))
        assertTrue(weekWidgetCourses(table, monday, style).isEmpty())
        assertEquals(listOf(1, 2, 3, 4, 5, 7), widgetWeekDays(table, style.copy(showSunday = true)))
        val data = loaded(timetable(), monday)
        val snapshot = selectedWeekSnapshot(context, data, widgetId)
        val bitmap = renderWeekWidget(context, data, snapshot, widgetId)
        val pixel = bitmap.getPixel((bitmap.width * 100f / 296).toInt(), (bitmap.height * 40f / 104).toInt())
        assertTrue(android.graphics.Color.alpha(pixel) > 0)
        assertEquals(255, android.graphics.Color.green(pixel))
        bitmap.recycle()
    }

    @Test
    fun collectionIdentityIsStableAndIsolatedByWidgetAndDay() {
        val first = collectionIntent(context, widgetId, WidgetKind.TODAY_AND_NEXT_DAY, 0)
        val refresh = collectionIntent(context, widgetId, WidgetKind.TODAY_AND_NEXT_DAY, 0)
        val tomorrow = collectionIntent(context, widgetId, WidgetKind.TODAY_AND_NEXT_DAY, 1)
        val other = collectionIntent(context, otherWidgetId, WidgetKind.TODAY_AND_NEXT_DAY, 0)
        assertTrue(first.filterEquals(refresh))
        assertFalse(first.filterEquals(tomorrow))
        assertFalse(first.filterEquals(other))
    }

    @Test
    fun widgetSettingsAndNavigationStayIndependent() {
        WidgetPreferences.setTableId(context, widgetId, 10)
        WidgetPreferences.setTableId(context, otherWidgetId, 20)
        WidgetPreferences.setWeek(context, widgetId, 3)
        WidgetPreferences.setDayOffset(context, widgetId, 99)
        WidgetPreferences.setShowBackground(context, widgetId, false)
        assertEquals(10L, WidgetPreferences.tableId(context, widgetId))
        assertEquals(20L, WidgetPreferences.tableId(context, otherWidgetId))
        assertEquals(1, WidgetPreferences.dayOffset(context, widgetId))
        assertEquals(0, WidgetPreferences.week(context, otherWidgetId))
        assertTrue(WidgetPreferences.showBackground(context, otherWidgetId))
        WidgetPreferences.clear(context, intArrayOf(widgetId))
        assertEquals(0L, WidgetPreferences.tableId(context, widgetId))
        assertEquals(20L, WidgetPreferences.tableId(context, otherWidgetId))
    }

    @Test
    fun automaticWeekUsesTheActualDateOutsideTheTerm() {
        val table = timetable(listOf(course("Math", 1)))
        for (day in listOf(monday - 7, monday + 14)) {
            val data = loaded(table, day)
            val snapshot = selectedWeekSnapshot(context, data, widgetId)
            assertEquals(day, snapshot.anchorEpochDay)
            assertTrue(snapshot.items.isEmpty())
            assertTrue(buildEntries(context, data, widgetId, WidgetKind.WEEK, 0).isEmpty())
        }
        val data = loaded(table, monday + 14)
        WidgetPreferences.setWeek(context, widgetId, 1)
        assertEquals(monday, selectedWeekSnapshot(context, data, widgetId).anchorEpochDay)
        assertEquals(1, selectedWeekSnapshot(context, data, widgetId).items.size)
        WidgetPreferences.setWeek(context, widgetId, 0)
        assertTrue(selectedWeekSnapshot(context, data, widgetId).items.isEmpty())
    }

    @Test
    fun automaticWeekKeepsIsoDatesWithEitherDisplayOrder() {
        for (sundayFirst in listOf(false, true)) {
            val table = timetable(listOf(course("Monday", 1), course("Sunday", 7)))
                .copy(sundayFirst = sundayFirst, showSaturday = true, showSunday = true)
            for (offset in 0L..6L) {
                val data = loaded(table, monday + offset)
                assertEquals(monday, selectedWeekSnapshot(context, data, widgetId).anchorEpochDay)
                val days = buildEntries(context, data, widgetId, WidgetKind.WEEK, 0)
                    .filterIsInstance<WidgetEntry.Day>()
                val expectedOrder = if (sundayFirst) listOf(6L, 0L, 1L, 2L, 3L, 4L, 5L) else (0L..6L).toList()
                assertEquals(expectedOrder.map { monday + it }, days.map { it.date.toEpochDay() })
                val mondayCourses = days.single { it.date.toEpochDay() == monday }.courses
                assertEquals(if (offset == 0L) listOf("Monday") else emptyList<String>(), mondayCourses.map { it.courseName })
                assertEquals("Sunday", days.single { it.date.toEpochDay() == monday + 6 }.courses.single().courseName)
            }
        }
    }

    @Test
    fun emptyDayMessageNamesTheDisplayedDate() {
        val data = loaded(timetable(), monday)
        assertEquals(context.getString(R.string.widget_no_remaining_courses),
            emptyMessage(context, data, LocalDate.ofEpochDay(monday)))
        assertEquals(context.getString(R.string.widget_no_courses_on_day,
            context.getString(R.string.widget_tomorrow_short)),
            emptyMessage(context, data, LocalDate.ofEpochDay(monday + 1)))
        assertEquals(context.getString(R.string.widget_no_courses_on_day,
            context.getString(R.string.widget_date_month_day, 9, 2)),
            emptyMessage(context, data, LocalDate.ofEpochDay(monday + 2)))
    }

    @Test
    fun todayAndTomorrowUseTheirOwnWeeksAcrossSunday() {
        val table = timetable(listOf(course("Sunday", 7), course("Monday", 1)))
        val data = loaded(table, monday + 6)
        val today = buildEntries(context, data, widgetId, WidgetKind.TODAY_AND_NEXT_DAY, 0)
            .filterIsInstance<WidgetEntry.Course>().single().item
        val tomorrow = buildEntries(context, data, widgetId, WidgetKind.TODAY_AND_NEXT_DAY, 1)
            .filterIsInstance<WidgetEntry.Course>().single().item
        assertEquals("Sunday", today.courseName)
        assertEquals(monday + 6, today.epochDay)
        assertEquals("Monday", tomorrow.courseName)
        assertEquals(monday + 7, tomorrow.epochDay)
    }

    @Test
    fun nextClassChangesExactlyAtTheEndOfACustomTime() {
        val table = timetable(listOf(course("Current", 1), course("Next", 2)))
        val during = WidgetSnapshotBuilder.buildNext(table, monday, 539).items.single()
        assertEquals("Current", during.courseName)
        assertTrue(during.isCurrent)
        val after = WidgetSnapshotBuilder.buildNext(table, monday, 540).items.single()
        assertEquals("Next", after.courseName)
        assertEquals(monday + 1, after.epochDay)
    }

    @Test
    fun widgetSnapshotsRespectSingleDateMovesAndCancellations() {
        val table = timetable(listOf(course("Math", 1)))
        val moved = TimetableCommands.apply(table, TimetableCommand.RescheduleOccurrence(
            logicalSlotId = "Math-slot", originalEpochDay = monday,
            targetEpochDay = monday + 1, targetStartNode = 1,
        ))
        assertTrue(WidgetSnapshotBuilder.buildToday(moved, monday, 0).items.isEmpty())
        assertEquals("Math", WidgetSnapshotBuilder.buildToday(moved, monday + 1, 0).items.single().courseName)
        val cancelled = TimetableCommands.apply(table, TimetableCommand.DeleteOccurrence(
            logicalSlotId = "Math-slot", originalEpochDay = monday,
        ))
        assertEquals(monday + 7, WidgetSnapshotBuilder.buildNext(cancelled, monday, 0).items.single().epochDay)
    }

    private fun loaded(table: Timetable, day: Long) = LoadedWidgetData(
        table, WidgetSnapshotBuilder.buildAll(table, day, 480), day, 480,
    )

    private fun timetable(courses: List<Course> = emptyList()) = Timetable(
        id = "10", name = "Semester", firstDayEpochDay = monday, maxWeek = 2,
        timeTable = TimeTable("day", "Day", listOf(TimeTableNode(1, 480, 530))), courses = courses,
    )

    private fun course(name: String, day: Int) = Course(
        id = name, name = name, slots = listOf(LogicalCourseSlot(
            id = "$name-slot", courseId = name, dayOfWeek = day, startNode = 1, nodeCount = 1,
            customTime = MinuteRange(480, 540),
            recurrenceSegments = listOf(RecurrenceSegment("$name-weeks", endWeek = 2)),
        )),
    )
}
