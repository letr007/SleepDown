package com.letr.sleepdown.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
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
import com.letr.sleepdown.data.CourseItem
import com.letr.sleepdown.data.CourseTimeEntity
import com.letr.sleepdown.data.TableWithMeta
import com.letr.sleepdown.logic.CourseColors
import java.time.LocalDate

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

private class ScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = AppContainer.repo(context)
        val selectedTableId = AppContainer.currentTableId(context)
        val defaultTableId = repository.ensureDefaultTable()
        val tableId = selectedTableId.takeIf { it > 0 } ?: defaultTableId
        val snapshot = repository.tableSnapshot(tableId)
            ?: repository.tableSnapshot(defaultTableId)
        val today = LocalDate.now()

        provideContent {
            ScheduleWidgetContent(snapshot, today)
        }
    }
}

/** Refresh all placed schedule widgets from an app or UI coroutine. */
public suspend fun refreshScheduleWidget(context: Context) {
    ScheduleWidget().updateAll(context.applicationContext)
}

@Composable
private fun ScheduleWidgetContent(snapshot: TableWithMeta?, today: LocalDate) {
    val courses = snapshot?.let { todayCourses(it, today) }.orEmpty()
    val dayName = DAY_NAMES[today.dayOfWeek.value - 1]
    val week = snapshot?.let { currentWeek(it, today) } ?: 1
    val tableName = snapshot?.table?.name?.trim().orEmpty().ifBlank { "我的课表" }
    val weekLabel = when {
        snapshot == null -> "暂无课表"
        week < 1 -> "未开学 · 第${week.coerceAtLeast(1)}周"
        week > snapshot.table.maxWeek -> "学期已结束 · 第${week}周"
        else -> "第${week}周"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BACKGROUND)
            .cornerRadius(18.dp)
            .padding(12.dp),
    ) {
        Text(
            text = "${today.monthValue}月${today.dayOfMonth}日",
            style = widgetTextStyle(16.sp, PRIMARY_TEXT, FontWeight.Bold),
        )
        Text(
            text = "今日课程 · $weekLabel · $dayName",
            style = widgetTextStyle(12.sp, PRIMARY_TEXT, FontWeight.Bold),
        )
        Text(
            text = tableName,
            style = widgetTextStyle(11.sp, SECONDARY_TEXT),
        )
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .height(1.dp)
                .background(DIVIDER),
        )

        if (courses.isEmpty()) {
            Text(
                text = if (snapshot == null) "暂无可显示的课表" else "今天没有课程",
                style = widgetTextStyle(13.sp, SECONDARY_TEXT),
            )
        } else {
            courses.take(MAX_VISIBLE_COURSES).forEachIndexed { index, item ->
                if (index > 0) Spacer(modifier = GlanceModifier.height(6.dp))
                CourseCard(item, snapshot)
            }
            if (courses.size > MAX_VISIBLE_COURSES) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "还有 ${courses.size - MAX_VISIBLE_COURSES} 节课",
                    style = widgetTextStyle(11.sp, SECONDARY_TEXT),
                )
            }
        }
    }
}

@Composable
private fun CourseCard(item: CourseItem, snapshot: TableWithMeta?) {
    val courseColor = courseColor(item)
    val room = item.time.room.trim().ifBlank { "教室未设置" }
    val teacher = item.course.teacher.trim()
    val details = if (teacher.isBlank()) room else "$room · $teacher"

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(CARD_BACKGROUND)
            .cornerRadius(10.dp)
            .padding(8.dp),
    ) {
        Spacer(
            modifier = GlanceModifier
                .width(4.dp)
                .height(48.dp)
                .background(courseColor)
                .cornerRadius(3.dp),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = item.course.name,
                style = widgetTextStyle(14.sp, PRIMARY_TEXT, FontWeight.Bold),
            )
            Text(
                text = details,
                style = widgetTextStyle(11.sp, SECONDARY_TEXT),
            )
            Text(
                text = timeLabel(snapshot, item),
                style = widgetTextStyle(11.sp, SECONDARY_TEXT),
            )
        }
    }
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

private fun currentWeek(snapshot: TableWithMeta, today: LocalDate): Int =
    Math.floorDiv(today.toEpochDay() - snapshot.table.startDate, 7L).toInt() + 1

private fun todayCourses(snapshot: TableWithMeta, today: LocalDate): List<CourseItem> {
    val week = currentWeek(snapshot, today)
    if (week !in 1..snapshot.table.maxWeek) return emptyList()

    return snapshot.items
        .filter { item ->
            val time = item.time
            time.day == today.dayOfWeek.value &&
                week in time.startWeek..time.endWeek &&
                when (time.weekType) {
                    CourseTimeEntity.TYPE_ODD -> week % 2 == 1
                    CourseTimeEntity.TYPE_EVEN -> week % 2 == 0
                    else -> true
                }
        }
        .sortedBy { it.time.startNode }
}

private fun courseColor(item: CourseItem): Color {
    val argb = item.course.color.takeUnless { it == 0 }
        ?: CourseColors.colorFor(item.course.name)
    return Color(argb).copy(alpha = 1f)
}

private fun timeLabel(snapshot: TableWithMeta?, item: CourseItem): String {
    val time = item.time
    if (time.ownTime && time.startTime.isNotBlank() && time.endTime.isNotBlank()) {
        return "${time.startTime}-${time.endTime}"
    }

    val endNode = time.startNode + time.step.coerceAtLeast(1) - 1
    val start = snapshot?.nodeTimes?.firstOrNull { it.node == time.startNode }?.start
        ?.takeIf(String::isNotBlank)
        ?: "第${time.startNode}节"
    val end = snapshot?.nodeTimes?.firstOrNull { it.node == endNode }?.end
        ?.takeIf(String::isNotBlank)
        ?: "第${endNode}节"
    return "$start-$end"
}

private val WIDGET_BACKGROUND = Color(0xFFF4F5F9)
private val CARD_BACKGROUND = Color(0xFFFFFFFF)
private val PRIMARY_TEXT = Color(0xFF1E1F24)
private val SECONDARY_TEXT = Color(0xFF737782)
private val DIVIDER = Color(0xFFDDE0E8)
private const val MAX_VISIBLE_COURSES = 5
private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
