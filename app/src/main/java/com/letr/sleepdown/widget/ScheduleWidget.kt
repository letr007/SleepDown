package com.letr.sleepdown.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.layout.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.letr.sleepdown.AppContainer
import com.letr.sleepdown.MainActivity
import com.letr.sleepdown.data.CourseItem
import com.letr.sleepdown.data.CourseTimeEntity
import com.letr.sleepdown.data.TableWithMeta
import com.letr.sleepdown.logic.CourseColors
import java.time.LocalDate

internal enum class WidgetKind {
    WEEK,
    TODAY,
    TODAY_COMPACT,
    TODAY_MODERN,
    TODAY_AND_NEXT_DAY,
}

internal const val WIDGET_TABLE_ID_EXTRA = "widget_table_id"

internal object WidgetPreferences {
    private const val PREFS = "schedule_widget"
    private const val TABLE_PREFIX = "table_"
    private const val WEEK_PREFIX = "week_"

    fun tableId(context: Context, appWidgetId: Int): Long {
        if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return 0L
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(TABLE_PREFIX + appWidgetId, 0L)
    }

    fun setTableId(context: Context, appWidgetId: Int, tableId: Long) {
        if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(TABLE_PREFIX + appWidgetId, tableId)
            .apply()
    }

    fun week(context: Context, appWidgetId: Int): Int {
        if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return 0
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(WEEK_PREFIX + appWidgetId, 0)
    }

    fun setWeek(context: Context, appWidgetId: Int, week: Int) {
        if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (week > 0) putInt(WEEK_PREFIX + appWidgetId, week)
                else remove(WEEK_PREFIX + appWidgetId)
            }
            .apply()
    }

    fun clear(context: Context, appWidgetIds: IntArray) {
        val editor = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
        appWidgetIds.forEach {
            editor.remove(TABLE_PREFIX + it)
            editor.remove(WEEK_PREFIX + it)
        }
        editor.apply()
    }
}

class WeekWidgetPreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        shiftWeek(context, glanceId, -1)
    }
}

class WeekWidgetNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        shiftWeek(context, glanceId, 1)
    }
}

private suspend fun shiftWeek(context: Context, glanceId: GlanceId, delta: Int) {
    val appContext = context.applicationContext
    try {
        val manager = GlanceAppWidgetManager(appContext)
        val appWidgetId = manager.getAppWidgetId(glanceId)
        val repository = AppContainer.repo(appContext)
        val configuredTableId = WidgetPreferences.tableId(appContext, appWidgetId)
        val tableId = configuredTableId.takeIf { it > 0L }
            ?: AppContainer.currentTableId(appContext)
        val table = repository.getTable(tableId) ?: return
        val maxWeek = table.maxWeek.coerceAtLeast(1)
        val currentWeek = Math.floorDiv(LocalDate.now().toEpochDay() - table.startDate, 7L).toInt() + 1
        val selectedWeek = WidgetPreferences.week(appContext, appWidgetId)
            .takeIf { it > 0 }
            ?: currentWeek.coerceIn(1, maxWeek)
        val nextWeek = (selectedWeek + delta).coerceIn(1, maxWeek)
        WidgetPreferences.setWeek(appContext, appWidgetId, nextWeek)
        WeekScheduleWidget().update(appContext, glanceId)
    } catch (_: Throwable) {
        // A widget may be removed while its action is being delivered.
    }
}

internal fun widgetKindForProvider(providerName: String): WidgetKind = when {
    providerName.contains("WeekSchedule", ignoreCase = true) -> WidgetKind.WEEK
    providerName.contains("TodayAndNext", ignoreCase = true) -> WidgetKind.TODAY_AND_NEXT_DAY
    providerName.contains("TodayModern", ignoreCase = true) -> WidgetKind.TODAY_MODERN
    providerName.contains("TodayCourse", ignoreCase = true) -> WidgetKind.TODAY
    else -> WidgetKind.TODAY_COMPACT
}

internal fun widgetForKind(kind: WidgetKind): GlanceAppWidget = when (kind) {
    WidgetKind.WEEK -> WeekScheduleWidget()
    WidgetKind.TODAY -> TodayCourseWidget()
    WidgetKind.TODAY_COMPACT -> ScheduleWidget()
    WidgetKind.TODAY_MODERN -> TodayModernWidget()
    WidgetKind.TODAY_AND_NEXT_DAY -> TodayAndNextDayWidget()
}

/** Existing provider retained so previously placed SleepDown widgets keep working. */
class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetPreferences.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class WeekScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekScheduleWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetPreferences.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class TodayCourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCourseWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetPreferences.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class TodayModernWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayModernWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetPreferences.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class TodayAndNextDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayAndNextDayWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetPreferences.clear(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

internal abstract class SleepDownWidget(
    private val kind: WidgetKind,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        val appWidgetId = manager.getAppWidgetId(id)
        val snapshot = loadSnapshot(appContext, id, appWidgetId)
        val today = LocalDate.now()
        val tableId = WidgetPreferences.tableId(appContext, appWidgetId)
            .takeIf { it > 0L }
            ?: snapshot?.table?.id
            ?: 0L
        val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
            Intent(appContext, MainActivity::class.java).putExtra(WIDGET_TABLE_ID_EXTRA, tableId),
        )
        val configAction = androidx.glance.appwidget.action.actionStartActivity(
            Intent(appContext, WidgetConfigActivity::class.java)
                .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        provideContent {
            when (kind) {
                WidgetKind.WEEK -> {
                    val currentWeek = snapshot?.let { currentWeek(it, today) } ?: 1
                    val selectedWeek = WidgetPreferences.week(appContext, appWidgetId)
                        .takeIf { it > 0 }
                        ?.coerceIn(1, snapshot?.table?.maxWeek?.coerceAtLeast(1) ?: 1)
                        ?: currentWeek.coerceAtLeast(1)
                    WeekWidgetContent(snapshot, today, selectedWeek, openAppAction, configAction)
                }
                WidgetKind.TODAY_AND_NEXT_DAY -> TodayAndNextDayWidgetContent(snapshot, today, openAppAction, configAction)
                WidgetKind.TODAY_COMPACT -> TodayWidgetContent(
                    snapshot,
                    today,
                    compact = true,
                    openAction = openAppAction,
                    configAction = configAction,
                )
                WidgetKind.TODAY,
                WidgetKind.TODAY_MODERN -> TodayWidgetContent(
                    snapshot,
                    today,
                    compact = false,
                    openAction = openAppAction,
                    configAction = configAction,
                )
            }
        }
    }
}

internal class ScheduleWidget : SleepDownWidget(WidgetKind.TODAY_COMPACT)

internal class WeekScheduleWidget : SleepDownWidget(WidgetKind.WEEK)

internal class TodayCourseWidget : SleepDownWidget(WidgetKind.TODAY)

internal class TodayModernWidget : SleepDownWidget(WidgetKind.TODAY_MODERN)

internal class TodayAndNextDayWidget : SleepDownWidget(WidgetKind.TODAY_AND_NEXT_DAY)

private suspend fun loadSnapshot(context: Context, id: GlanceId, appWidgetId: Int): TableWithMeta? {
    val appContext = context.applicationContext
    val repository = AppContainer.repo(appContext)
    val defaultTableId = repository.ensureDefaultTable()
    val configuredTableId = WidgetPreferences.tableId(appContext, appWidgetId)
    val selectedTableId = configuredTableId.takeIf { it > 0L }
        ?: AppContainer.currentTableId(appContext)
    return repository.tableSnapshot(selectedTableId)
        ?: repository.tableSnapshot(defaultTableId)
}

@Composable
private fun TodayWidgetContent(
    snapshot: TableWithMeta?,
    today: LocalDate,
    compact: Boolean,
    openAction: Action,
    configAction: Action,
) {
    val courses = snapshot?.let { coursesForDate(it, today) }.orEmpty()
    val weekLabel = snapshot?.let { weekLabel(it, today) } ?: "暂无课表"
    val tableName = snapshot?.table?.name?.trim().orEmpty().ifBlank { "我的课表" }
    val maxVisible = if (compact) 3 else 5

    WidgetSurface(openAction) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (compact) "今日课程" else "${today.monthValue}月${today.dayOfMonth}日 · 今日课程",
                    style = widgetTextStyle(if (compact) 14.sp else 16.sp, PRIMARY_TEXT, FontWeight.Bold),
                )
                Text(
                    text = "${weekdayName(today.dayOfWeek.value)} · $weekLabel",
                    style = widgetTextStyle(11.sp, SECONDARY_TEXT),
                )
            }
            if (!compact) {
                Text(
                    text = tableName,
                    style = widgetTextStyle(11.sp, SECONDARY_TEXT),
                )
            }
            Text(
                text = "设置",
                modifier = GlanceModifier
                    .padding(start = 6.dp)
                    .clickable(configAction),
                style = widgetTextStyle(10.sp, SECONDARY_TEXT),
            )
        }
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 7.dp, bottom = 4.dp)
                .height(1.dp)
                .background(DIVIDER),
        )

        if (courses.isEmpty()) {
            Text(
                text = if (snapshot == null) "暂无可显示的课表" else "今天没有课程",
                style = widgetTextStyle(13.sp, SECONDARY_TEXT),
            )
        } else {
            val data = snapshot
            if (data == null) {
                Text("暂无可显示的课表", style = widgetTextStyle(13.sp, SECONDARY_TEXT))
            } else {
                courses.take(maxVisible).forEachIndexed { index, item ->
                    if (index > 0) Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 6.dp))
                    TodayCourseRow(item, data, compact)
                }
                if (courses.size > maxVisible) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "还有 ${courses.size - maxVisible} 节课",
                        style = widgetTextStyle(10.sp, SECONDARY_TEXT),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayCourseRow(item: CourseItem, snapshot: TableWithMeta, compact: Boolean) {
    val room = item.time.room.trim()
    val teacher = item.course.teacher.trim()
    val details = when {
        room.isNotBlank() && teacher.isNotBlank() -> "$room · $teacher"
        room.isNotBlank() -> room
        teacher.isNotBlank() -> teacher
        else -> ""
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(CARD_BACKGROUND)
            .cornerRadius(9.dp)
            .padding(if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = GlanceModifier
                .width(4.dp)
                .height(if (compact) 34.dp else 46.dp)
                .background(courseColor(item))
                .cornerRadius(3.dp),
        )
        Spacer(modifier = GlanceModifier.width(7.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.course.name,
                style = widgetTextStyle(if (compact) 12.sp else 14.sp, PRIMARY_TEXT, FontWeight.Bold),
            )
            Text(
                text = timeLabel(snapshot, item),
                style = widgetTextStyle(10.sp, SECONDARY_TEXT),
            )
            if (!compact && details.isNotBlank()) {
                Text(
                    text = details,
                    style = widgetTextStyle(10.sp, SECONDARY_TEXT),
                )
            }
        }
    }
}

@Composable
private fun TodayAndNextDayWidgetContent(
    snapshot: TableWithMeta?,
    today: LocalDate,
    openAction: Action,
    configAction: Action,
) {
    WidgetSurface(openAction) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "近日课程",
                    style = widgetTextStyle(16.sp, PRIMARY_TEXT, FontWeight.Bold),
                )
                Text(
                    text = snapshot?.table?.name?.trim().orEmpty().ifBlank { "我的课表" },
                    style = widgetTextStyle(11.sp, SECONDARY_TEXT),
                )
            }
            Text(
                text = "设置",
                modifier = GlanceModifier.clickable(configAction),
                style = widgetTextStyle(10.sp, SECONDARY_TEXT),
            )
        }
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 7.dp, bottom = 4.dp)
                .height(1.dp)
                .background(DIVIDER),
        )
        if (snapshot == null) {
            Text("暂无可显示的课表", style = widgetTextStyle(13.sp, SECONDARY_TEXT))
        } else {
            TodaySection(snapshot, today, "今天")
            Spacer(modifier = GlanceModifier.height(6.dp))
            TodaySection(snapshot, today.plusDays(1), "明天")
        }
    }
}

@Composable
private fun TodaySection(snapshot: TableWithMeta, date: LocalDate, label: String) {
    val courses = coursesForDate(snapshot, date)
    Text(
        text = "$label · ${weekdayName(date.dayOfWeek.value)}",
        style = widgetTextStyle(12.sp, PRIMARY_TEXT, FontWeight.Bold),
    )
    if (courses.isEmpty()) {
        Text("没有课程", style = widgetTextStyle(10.sp, SECONDARY_TEXT))
    } else {
        courses.take(2).forEachIndexed { index, item ->
            if (index > 0) Spacer(modifier = GlanceModifier.height(3.dp))
            TodayCourseRow(item, snapshot, compact = true)
        }
        if (courses.size > 2) {
            Text("还有 ${courses.size - 2} 节课", style = widgetTextStyle(10.sp, SECONDARY_TEXT))
        }
    }
}

@Composable
private fun WeekWidgetContent(
    snapshot: TableWithMeta?,
    today: LocalDate,
    week: Int,
    openAction: Action,
    configAction: Action,
) {
    val weekCourses = snapshot?.let { coursesForWeek(it, week) }.orEmpty()
    val tableName = snapshot?.table?.name?.trim().orEmpty().ifBlank { "我的课表" }

    WidgetSurface(openAction) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                modifier = GlanceModifier
                    .padding(end = 4.dp)
                    .clickable(actionRunCallback(WeekWidgetPreviousAction::class.java)),
                style = widgetTextStyle(22.sp, PRIMARY_TEXT),
            )
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("周课表", style = widgetTextStyle(16.sp, PRIMARY_TEXT, FontWeight.Bold))
                Text(
                    text = "$tableName · 第${week}周",
                    style = widgetTextStyle(10.sp, SECONDARY_TEXT),
                )
            }
            Text(
                text = todayDateLabel(today),
                modifier = GlanceModifier.padding(end = 6.dp),
                style = widgetTextStyle(10.sp, SECONDARY_TEXT),
            )
            Text(
                text = "设置",
                modifier = GlanceModifier
                    .padding(end = 4.dp)
                    .clickable(configAction),
                style = widgetTextStyle(10.sp, SECONDARY_TEXT),
            )
            Text(
                text = "›",
                modifier = GlanceModifier
                    .clickable(actionRunCallback(WeekWidgetNextAction::class.java)),
                style = widgetTextStyle(22.sp, PRIMARY_TEXT),
            )
        }
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp)
                .height(1.dp)
                .background(DIVIDER),
        )
        if (snapshot == null) {
            Text("暂无可显示的课表", style = widgetTextStyle(13.sp, SECONDARY_TEXT))
        } else if (week !in 1..snapshot.table.maxWeek) {
            Text("当前不在课程周范围内", style = widgetTextStyle(13.sp, SECONDARY_TEXT))
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                displayedDays(snapshot).forEach { day ->
                    val date = dateFor(snapshot, week, day)
                    val courses = weekCourses.filter { it.time.day == day }
                    Column(
                        modifier = GlanceModifier.defaultWeight().padding(horizontal = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${weekdayName(day)} ${date.dayOfMonth}",
                            style = widgetTextStyle(9.sp, PRIMARY_TEXT, FontWeight.Bold),
                        )
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        if (courses.isEmpty()) {
                            Text("—", style = widgetTextStyle(10.sp, SECONDARY_TEXT))
                        } else {
                            courses.take(4).forEach { item ->
                                Text(
                                    text = item.course.name,
                                    style = widgetTextStyle(9.sp, PRIMARY_TEXT, FontWeight.Bold),
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(bottom = 2.dp)
                                        .background(courseColor(item).copy(alpha = 0.75f))
                                        .cornerRadius(4.dp)
                                        .padding(horizontal = 2.dp, vertical = 3.dp),
                                )
                            }
                            if (courses.size > 4) {
                                Text("+${courses.size - 4}", style = widgetTextStyle(9.sp, SECONDARY_TEXT))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetSurface(
    openAction: Action,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openAction)
            .background(WIDGET_BACKGROUND)
            .cornerRadius(18.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
        content = content,
    )
}

private fun widgetTextStyle(
    size: TextUnit,
    color: Color,
    weight: FontWeight? = null,
): TextStyle = TextStyle(
    color = ColorProvider(color),
    fontSize = size,
    fontWeight = weight,
)

private fun currentWeek(snapshot: TableWithMeta, date: LocalDate): Int =
    Math.floorDiv(date.toEpochDay() - snapshot.table.startDate, 7L).toInt() + 1

private fun weekLabel(snapshot: TableWithMeta?, date: LocalDate): String {
    if (snapshot == null) return "暂无课表"
    val week = currentWeek(snapshot, date)
    return when {
        week < 1 -> "未开学 · 第${week.coerceAtLeast(1)}周"
        week > snapshot.table.maxWeek -> "学期已结束 · 第${week}周"
        else -> "第${week}周"
    }
}

private fun coursesForDate(snapshot: TableWithMeta, date: LocalDate): List<CourseItem> {
    val week = currentWeek(snapshot, date)
    if (week !in 1..snapshot.table.maxWeek) return emptyList()
    return snapshot.items
        .filter { item ->
            val time = item.time
            time.day == date.dayOfWeek.value &&
                week in time.startWeek..time.endWeek &&
                when (time.weekType) {
                    CourseTimeEntity.TYPE_ODD -> week % 2 == 1
                    CourseTimeEntity.TYPE_EVEN -> week % 2 == 0
                    else -> true
                }
        }
        .sortedBy { it.time.startNode }
}

private fun coursesForWeek(snapshot: TableWithMeta, week: Int): List<CourseItem> =
    snapshot.items
        .filter { item ->
            WeeksMatch.matches(item.time.startWeek, item.time.endWeek, item.time.weekType, week)
        }
        .sortedWith(compareBy({ it.time.day }, { it.time.startNode }))

private object WeeksMatch {
    fun matches(startWeek: Int, endWeek: Int, weekType: Int, week: Int): Boolean =
        week in startWeek..endWeek && when (weekType) {
            CourseTimeEntity.TYPE_ODD -> week % 2 == 1
            CourseTimeEntity.TYPE_EVEN -> week % 2 == 0
            else -> true
        }
}

private fun displayedDays(snapshot: TableWithMeta): List<Int> =
    if (snapshot.table.sundayFirst) listOf(7, 1, 2, 3, 4, 5, 6) else (1..7).toList()

private fun dateFor(snapshot: TableWithMeta, week: Int, day: Int): LocalDate {
    val offset = if (snapshot.table.sundayFirst) {
        if (day == 7) 0 else day
    } else {
        day - 1
    }
    return LocalDate.ofEpochDay(snapshot.table.startDate)
        .plusWeeks((week - 1).toLong())
        .plusDays(offset.toLong())
}

private fun weekdayName(day: Int): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[((day - 1) % 7 + 7) % 7]

private fun todayDateLabel(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

private fun courseColor(item: CourseItem): Color {
    val argb = item.course.color.takeUnless { it == 0 }
        ?: CourseColors.colorFor(item.course.name)
    return Color(argb).copy(alpha = 1f)
}

private fun timeLabel(snapshot: TableWithMeta, item: CourseItem): String {
    val time = item.time
    if (time.ownTime && time.startTime.isNotBlank() && time.endTime.isNotBlank()) {
        return "${time.startTime}-${time.endTime}"
    }

    val endNode = time.startNode + time.step.coerceAtLeast(1) - 1
    val start = snapshot.nodeTimes.firstOrNull { it.node == time.startNode }?.start
        ?.takeIf(String::isNotBlank)
        ?: "第${time.startNode}节"
    val end = snapshot.nodeTimes.firstOrNull { it.node == endNode }?.end
        ?.takeIf(String::isNotBlank)
        ?: "第${endNode}节"
    return "$start-$end"
}

/** Refresh every placed SleepDown widget after a timetable change. */
public suspend fun refreshScheduleWidgets(context: Context) {
    val appContext = context.applicationContext
    listOf(
        ScheduleWidget(),
        WeekScheduleWidget(),
        TodayCourseWidget(),
        TodayModernWidget(),
        TodayAndNextDayWidget(),
    ).forEach { it.updateAll(appContext) }
}

/** Backward-compatible name for callers that only knew the original widget. */
public suspend fun refreshScheduleWidget(context: Context) {
    refreshScheduleWidgets(context)
}

private val WIDGET_BACKGROUND = Color(0xFFF4F5F9)
private val CARD_BACKGROUND = Color(0xFFFFFFFF)
private val PRIMARY_TEXT = Color(0xFF1E1F24)
private val SECONDARY_TEXT = Color(0xFF737782)
private val DIVIDER = Color(0xFFDDE0E8)
