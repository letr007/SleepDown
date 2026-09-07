package com.letr.sleepdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.letr.sleepdown.AppContainer
import com.letr.sleepdown.MainActivity
import com.letr.sleepdown.R
import com.letr.sleepdown.withSleepDownAppLocales
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.WidgetSnapshot
import com.letr.sleepdown.domain.WidgetSnapshotBuilder
import com.letr.sleepdown.domain.WidgetSnapshotItem
import com.letr.sleepdown.domain.WidgetSnapshotKind
import com.letr.sleepdown.domain.WidgetSnapshots
import com.letr.sleepdown.logic.CourseColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/** The presentation variant and shared snapshot contract used by a provider. */
enum class WidgetKind {
    NEXT,
    TODAY,
    TODAY_MODERN,
    TODAY_AND_NEXT_DAY,
    WEEK;

    val snapshotKind: WidgetSnapshotKind
        get() = when (this) {
            NEXT,
            TODAY,
            TODAY_MODERN,
            TODAY_AND_NEXT_DAY -> WidgetSnapshotKind.TODAY
            WEEK -> WidgetSnapshotKind.WEEK
        }
}

internal const val WIDGET_TABLE_ID_EXTRA = "widget_table_id"

private const val EXTRA_WIDGET_ID = "sleepdown_widget_id"
private const val EXTRA_WIDGET_KIND = "sleepdown_widget_kind"
private const val EXTRA_DAY_OFFSET = "sleepdown_day_offset"
private const val ACTION_WEEK_PREVIOUS = "com.letr.sleepdown.widget.WEEK_PREVIOUS"
private const val ACTION_WEEK_NEXT = "com.letr.sleepdown.widget.WEEK_NEXT"
private const val ACTION_DAY_PREVIOUS = "com.letr.sleepdown.widget.DAY_PREVIOUS"
private const val ACTION_DAY_NEXT = "com.letr.sleepdown.widget.DAY_NEXT"
private const val ACTION_RESET_DATE = "com.letr.sleepdown.widget.RESET_DATE"
private const val PREFS = "schedule_widget"
private const val TABLE_PREFIX = "table_"
private const val WEEK_PREFIX = "week_"
private const val DAY_PREFIX = "day_"
private const val BACKGROUND_PREFIX = "background_"
private const val COLOR_BLOCK_PREFIX = "color_block_"
private const val TEXT_SIZE_PREFIX = "text_size_"
private const val DEFAULT_TEXT_SIZE_SCALE = 1f
private const val WIDE_COURSE_MIN_WIDTH_DP = 280
private const val WIDE_DOUBLE_COLUMN_MIN_WIDTH_DP = 140
private const val WIDGET_HORIZONTAL_PADDING_DP = 24
private const val DOUBLE_DAY_GAP_DP = 16

private val widgetUpdateMutex = Mutex()

internal suspend fun <T> withWidgetUpdate(block: suspend () -> T): T =
    widgetUpdateMutex.withLock { withContext(Dispatchers.IO) { block() } }

internal object WidgetPreferences {
    fun tableId(context: Context, appWidgetId: Int): Long {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return 0L
        return preferences(context).getLong(TABLE_PREFIX + appWidgetId, 0L)
    }

    fun setTableId(context: Context, appWidgetId: Int, tableId: Long) {
        if (!isValidId(appWidgetId)) return
        preferences(context).edit().putLong(TABLE_PREFIX + appWidgetId, tableId).apply()
    }

    fun week(context: Context, appWidgetId: Int): Int {
        if (!isValidId(appWidgetId)) return 0
        return preferences(context).getInt(WEEK_PREFIX + appWidgetId, 0)
    }

    fun setWeek(context: Context, appWidgetId: Int, week: Int) {
        if (!isValidId(appWidgetId)) return
        preferences(context).edit().apply {
            if (week > 0) putInt(WEEK_PREFIX + appWidgetId, week) else remove(WEEK_PREFIX + appWidgetId)
        }.apply()
    }

    fun dayOffset(context: Context, appWidgetId: Int): Int {
        if (!isValidId(appWidgetId)) return 0
        return preferences(context).getInt(DAY_PREFIX + appWidgetId, 0).coerceIn(0, 1)
    }

    fun setDayOffset(context: Context, appWidgetId: Int, offset: Int) {
        if (!isValidId(appWidgetId)) return
        preferences(context).edit()
            .putInt(DAY_PREFIX + appWidgetId, offset.coerceIn(0, 1))
            .apply()
    }

    fun showBackground(context: Context, appWidgetId: Int): Boolean =
        if (isValidId(appWidgetId)) preferences(context).getBoolean(BACKGROUND_PREFIX + appWidgetId, true) else true

    fun setShowBackground(context: Context, appWidgetId: Int, visible: Boolean) {
        if (!isValidId(appWidgetId)) return
        preferences(context).edit().putBoolean(BACKGROUND_PREFIX + appWidgetId, visible).apply()
    }

    fun showColorBlocks(context: Context, appWidgetId: Int): Boolean =
        if (isValidId(appWidgetId)) preferences(context).getBoolean(COLOR_BLOCK_PREFIX + appWidgetId, true) else true

    fun setShowColorBlocks(context: Context, appWidgetId: Int, visible: Boolean) {
        if (!isValidId(appWidgetId)) return
        preferences(context).edit().putBoolean(COLOR_BLOCK_PREFIX + appWidgetId, visible).apply()
    }

    fun textSizeScale(context: Context, appWidgetId: Int): Float =
        if (isValidId(appWidgetId)) {
            preferences(context).getFloat(TEXT_SIZE_PREFIX + appWidgetId, DEFAULT_TEXT_SIZE_SCALE)
                .coerceIn(0.85f, 1.25f)
        } else {
            DEFAULT_TEXT_SIZE_SCALE
        }

    fun setTextSizeScale(context: Context, appWidgetId: Int, scale: Float) {
        if (!isValidId(appWidgetId)) return
        preferences(context).edit()
            .putFloat(TEXT_SIZE_PREFIX + appWidgetId, scale.coerceIn(0.85f, 1.25f))
            .apply()
    }

    fun clear(context: Context, appWidgetIds: IntArray) {
        val editor = preferences(context).edit()
        appWidgetIds.forEach { appWidgetId ->
            editor.remove(TABLE_PREFIX + appWidgetId)
            editor.remove(WEEK_PREFIX + appWidgetId)
            editor.remove(DAY_PREFIX + appWidgetId)
            editor.remove(BACKGROUND_PREFIX + appWidgetId)
            editor.remove(COLOR_BLOCK_PREFIX + appWidgetId)
            editor.remove(TEXT_SIZE_PREFIX + appWidgetId)
            context.getSharedPreferences("widget_style_$appWidgetId", Context.MODE_PRIVATE)
                .edit().clear().apply()
            clearWidgetBackground(context, appWidgetId)
        }
        editor.apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isValidId(appWidgetId: Int): Boolean =
        appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
}

internal fun widgetKindForProvider(providerName: String): WidgetKind = when {
    providerName.contains("WeekSchedule", ignoreCase = true) -> WidgetKind.WEEK
    providerName.contains("TodayAndNext", ignoreCase = true) -> WidgetKind.TODAY_AND_NEXT_DAY
    providerName.contains("TodayModern", ignoreCase = true) -> WidgetKind.TODAY_MODERN
    providerName.contains("TodayCourse", ignoreCase = true) -> WidgetKind.TODAY
    else -> WidgetKind.NEXT
}

abstract class SleepDownWidgetReceiver : AppWidgetProvider() {
    abstract val kind: WidgetKind

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAsync(context, appWidgetManager, appWidgetIds.toList())
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        updateAsync(context, appWidgetManager, listOf(appWidgetId))
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAsync { withWidgetUpdate { WidgetPreferences.clear(context, appWidgetIds) } }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WEEK_PREVIOUS -> shiftWeekAsync(context, intent, -1)
            ACTION_WEEK_NEXT -> shiftWeekAsync(context, intent, 1)
            ACTION_DAY_PREVIOUS -> shiftDayAsync(context, intent, -1)
            ACTION_DAY_NEXT -> shiftDayAsync(context, intent, 1)
            ACTION_RESET_DATE -> {
                val id = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    runAsync {
                        refreshWidgetForConfiguration(context, id, kind) {
                            WidgetPreferences.setWeek(context, id, 0)
                            WidgetPreferences.setDayOffset(context, id, 0)
                        }
                    }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("ScheduleWidget", "Widget update failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateAsync(context: Context, manager: AppWidgetManager, ids: List<Int>) {
        if (ids.isEmpty()) return
        runAsync {
            ids.forEach { appWidgetId -> refreshWidget(context, manager, appWidgetId, kind) }
        }
    }

    private fun shiftWeekAsync(context: Context, intent: Intent, delta: Int) {
        val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || kind != WidgetKind.WEEK) return
        runAsync { shiftWeek(context, appWidgetId, delta) }
    }

    private fun shiftDayAsync(context: Context, intent: Intent, delta: Int) {
        val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || kind != WidgetKind.TODAY) return
        runAsync {
            refreshWidgetForConfiguration(context, appWidgetId, kind) {
                WidgetPreferences.setDayOffset(context, appWidgetId, if (delta > 0) 1 else 0)
            }
        }
    }
}

/** Existing provider names are kept so placed widgets continue to resolve. */
class ScheduleWidgetReceiver : SleepDownWidgetReceiver() {
    override val kind = WidgetKind.NEXT
}

class TodayCourseWidgetReceiver : SleepDownWidgetReceiver() {
    override val kind = WidgetKind.TODAY
}

class TodayModernWidgetReceiver : SleepDownWidgetReceiver() {
    override val kind = WidgetKind.TODAY_MODERN
}

class TodayAndNextDayWidgetReceiver : SleepDownWidgetReceiver() {
    override val kind = WidgetKind.TODAY_AND_NEXT_DAY
}

class WeekScheduleWidgetReceiver : SleepDownWidgetReceiver() {
    override val kind = WidgetKind.WEEK
}

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ScheduleWidgetFactory(applicationContext.withSleepDownAppLocales(), intent)
}

internal data class LoadedWidgetData(
    val timetable: Timetable,
    val snapshots: WidgetSnapshots,
    val nowEpochDay: Long,
    val nowMinuteOfDay: Int,
)

internal fun LoadedWidgetData.isRemainingCourse(epochDay: Long, endMinuteOfDay: Int): Boolean =
    epochDay > nowEpochDay || (epochDay == nowEpochDay && endMinuteOfDay >= nowMinuteOfDay)

private suspend fun loadWidgetData(context: Context, appWidgetId: Int, bindIfMissing: Boolean = true): LoadedWidgetData? {
    val appContext = context.applicationContext.withSleepDownAppLocales()
    val repository = AppContainer.repo(appContext)
    val configuredTableId = WidgetPreferences.tableId(appContext, appWidgetId)
    if (configuredTableId <= 0L && !bindIfMissing) return null
    val selectedTableId = configuredTableId.takeIf { it > 0L }
        ?: AppContainer.currentTableId(appContext).takeIf { it > 0L }
        ?: repository.ensureDefaultTable(appContext.getString(R.string.default_table_name))
    val timetable = repository.loadDomainTimetableOrNull(selectedTableId) ?: return null
    if (configuredTableId <= 0L) WidgetPreferences.setTableId(appContext, appWidgetId, selectedTableId)
    val now = LocalDateTime.now()
    val nowEpochDay = now.toLocalDate().toEpochDay()
    val nowMinuteOfDay = now.toLocalTime().toSecondOfDay() / 60
    return LoadedWidgetData(
        timetable = timetable,
        snapshots = WidgetSnapshotBuilder.buildAll(timetable, nowEpochDay, nowMinuteOfDay),
        nowEpochDay = nowEpochDay,
        nowMinuteOfDay = nowMinuteOfDay,
    )
}

private suspend fun shiftWeek(context: Context, appWidgetId: Int, delta: Int) {
    refreshWidgetForConfiguration(context, appWidgetId, WidgetKind.WEEK) {
        val data = loadWidgetData(context, appWidgetId) ?: return@refreshWidgetForConfiguration
        val maxWeek = data.timetable.maxWeek.coerceAtLeast(1)
        val current = academicWeek(data.timetable, LocalDate.ofEpochDay(data.nowEpochDay))
        val selected = WidgetPreferences.week(context, appWidgetId).takeIf { it > 0 } ?: current
        WidgetPreferences.setWeek(context, appWidgetId, (selected + delta).coerceIn(1, maxWeek))
    }
}

internal suspend fun refreshWidgetForConfiguration(
    context: Context,
    appWidgetId: Int,
    kind: WidgetKind,
    beforeRefresh: suspend () -> Unit = {},
): Boolean {
    val appContext = context.applicationContext.withSleepDownAppLocales()
    return refreshWidget(appContext, AppWidgetManager.getInstance(context.applicationContext), appWidgetId, kind, beforeRefresh)
}

private suspend fun refreshWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    kind: WidgetKind,
    beforeRefresh: suspend () -> Unit = {},
): Boolean = withWidgetUpdate {
    if (appWidgetManager.getAppWidgetInfo(appWidgetId) == null) return@withWidgetUpdate false
    beforeRefresh()
    val localizedContext = context.withSleepDownAppLocales()
    initializeWidgetStyle(localizedContext, appWidgetId, kind)
    val data = loadWidgetData(localizedContext, appWidgetId)
    val views = RemoteViews(localizedContext.packageName, layoutFor(kind))
    applyWidgetStyle(localizedContext, views, appWidgetId, kind)

    val tableId = data?.timetable?.id?.toLongOrNull()
        ?: WidgetPreferences.tableId(localizedContext, appWidgetId).takeIf { it > 0L }
        ?: AppContainer.currentTableId(localizedContext)
    val mainIntent = Intent(context.applicationContext, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(WIDGET_TABLE_ID_EXTRA, tableId)
    val mainPendingIntent = PendingIntent.getActivity(
        context.applicationContext,
        10000 + appWidgetId,
        mainIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    var contentPendingIntent = mainPendingIntent

    if (kind == WidgetKind.TODAY || kind == WidgetKind.WEEK) {
        val configIntent = Intent(context.applicationContext, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val configPendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            20000 + appWidgetId,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_settings, configPendingIntent)
        val style = WidgetStyle.read(localizedContext, appWidgetId)
        if (!style.showHeader || !style.showButtons) contentPendingIntent = configPendingIntent
    }
    views.setOnClickPendingIntent(R.id.widget_root, contentPendingIntent)

    when (kind) {
        WidgetKind.NEXT,
        WidgetKind.TODAY_MODERN -> configureModernWidget(localizedContext, views, appWidgetId, data, contentPendingIntent, kind)
        WidgetKind.TODAY -> configureTodayWidget(localizedContext, views, appWidgetId, data, contentPendingIntent)
        WidgetKind.TODAY_AND_NEXT_DAY -> configureTodayNextWidget(localizedContext, views, appWidgetId, data, contentPendingIntent)
        WidgetKind.WEEK -> configureWeekWidget(localizedContext, views, appWidgetId, data, contentPendingIntent)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
    collectionIdsFor(kind).forEach { viewId ->
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, viewId)
    }
    true
}

private fun configureModernWidget(
    context: Context,
    views: RemoteViews,
    appWidgetId: Int,
    data: LoadedWidgetData?,
    mainPendingIntent: PendingIntent,
    kind: WidgetKind,
) {
    val date = LocalDate.ofEpochDay(data?.nowEpochDay ?: LocalDate.now().toEpochDay())
    configureHeader(context, views, data?.timetable, date, data?.let { academicWeek(it.timetable, date) }, compact = true)
    views.setRemoteAdapter(R.id.widget_course_list, collectionIntent(context, appWidgetId, kind, 0))
    views.setEmptyView(R.id.widget_course_list, R.id.widget_empty)
    views.setPendingIntentTemplate(R.id.widget_course_list, mainPendingIntent)
    views.setTextViewText(
        R.id.widget_empty,
        if (data == null) context.getString(R.string.widget_no_timetable) else context.getString(R.string.widget_no_remaining_courses),
    )
}

private fun configureTodayWidget(
    context: Context,
    views: RemoteViews,
    appWidgetId: Int,
    data: LoadedWidgetData?,
    mainPendingIntent: PendingIntent,
) {
    val offset = WidgetPreferences.dayOffset(context, appWidgetId)
    val date = LocalDate.ofEpochDay(data?.nowEpochDay ?: LocalDate.now().toEpochDay()).plusDays(offset.toLong())
    configureHeader(context, views, data?.timetable, date, data?.let { academicWeek(it.timetable, date) }, appWidgetId = appWidgetId)
    if (offset == 1 && WidgetStyle.read(context, appWidgetId).showButtons) {
        views.setTextViewText(R.id.widget_date, context.getString(R.string.widget_tomorrow_short))
    }
    views.setRemoteAdapter(
        R.id.widget_course_list,
        collectionIntent(context, appWidgetId, WidgetKind.TODAY, offset),
    )
    views.setEmptyView(R.id.widget_course_list, R.id.widget_empty)
    views.setPendingIntentTemplate(R.id.widget_course_list, mainPendingIntent)
    views.setTextViewText(R.id.widget_empty, emptyMessage(context, data, date))
    val receiver = TodayCourseWidgetReceiver::class.java
    views.setOnClickPendingIntent(R.id.widget_date, actionPendingIntent(context, receiver, ACTION_RESET_DATE, appWidgetId))
    views.setContentDescription(R.id.widget_date, context.getString(R.string.widget_return_today))
    views.setBoolean(R.id.widget_previous, "setEnabled", offset == 1)
    views.setBoolean(R.id.widget_next, "setEnabled", offset == 0)
    views.setOnClickPendingIntent(
        R.id.widget_previous,
        actionPendingIntent(context, receiver, ACTION_DAY_PREVIOUS, appWidgetId),
    )
    views.setOnClickPendingIntent(
        R.id.widget_next,
        actionPendingIntent(context, receiver, ACTION_DAY_NEXT, appWidgetId),
    )
}

internal fun remainingTodayCourseItems(data: LoadedWidgetData, date: LocalDate): List<WidgetSnapshotItem> =
    todaySnapshot(data, date).items.filter { item ->
        data.isRemainingCourse(item.epochDay, item.endMinuteOfDay)
    }

private fun configureTodayNextWidget(
    context: Context,
    views: RemoteViews,
    appWidgetId: Int,
    data: LoadedWidgetData?,
    mainPendingIntent: PendingIntent,
) {
    val today = LocalDate.ofEpochDay(data?.nowEpochDay ?: LocalDate.now().toEpochDay())
    configureHeader(context, views, data?.timetable, today, data?.let { academicWeek(it.timetable, today) }, compact = true)
    views.setTextViewText(
        R.id.widget_today_label,
        context.getString(R.string.widget_today_short),
    )
    views.setTextViewText(
        R.id.widget_next_day_label,
        context.getString(R.string.widget_tomorrow_short),
    )
    views.setRemoteAdapter(
        R.id.widget_course_list,
        collectionIntent(context, appWidgetId, WidgetKind.TODAY_AND_NEXT_DAY, 0),
    )
    views.setRemoteAdapter(
        R.id.widget_course_list_next,
        collectionIntent(context, appWidgetId, WidgetKind.TODAY_AND_NEXT_DAY, 1),
    )
    views.setEmptyView(R.id.widget_course_list, R.id.widget_empty)
    views.setEmptyView(R.id.widget_course_list_next, R.id.widget_empty_next)
    views.setPendingIntentTemplate(R.id.widget_course_list, mainPendingIntent)
    views.setPendingIntentTemplate(R.id.widget_course_list_next, mainPendingIntent)
    views.setTextViewText(R.id.widget_empty, emptyMessage(context, data, today))
    views.setTextViewText(R.id.widget_empty_next, emptyMessage(context, data, today.plusDays(1)))
}

private fun configureWeekWidget(
    context: Context,
    views: RemoteViews,
    appWidgetId: Int,
    data: LoadedWidgetData?,
    mainPendingIntent: PendingIntent,
) {
    val maxWeek = data?.timetable?.maxWeek?.coerceAtLeast(1) ?: 1
    val current = data?.let { academicWeek(it.timetable, LocalDate.ofEpochDay(it.nowEpochDay)) } ?: 1
    val week = WidgetPreferences.week(context, appWidgetId).takeIf { it > 0 }?.coerceIn(1, maxWeek) ?: current
    val date = LocalDate.ofEpochDay(data?.nowEpochDay ?: LocalDate.now().toEpochDay())
    views.setTextViewText(R.id.widget_date, context.getString(R.string.widget_date_month_day, date.monthValue, date.dayOfMonth))
    views.setContentDescription(R.id.widget_previous, context.getString(R.string.widget_previous_week_description))
    views.setContentDescription(R.id.widget_next, context.getString(R.string.widget_next_week_description))
    views.setTextViewText(R.id.widget_table, tableName(context, data?.timetable, appWidgetId))
    val weekLabel = when {
        data == null -> context.getString(R.string.widget_no_table_header)
        week < 1 -> context.getString(R.string.widget_term_not_started)
        week > maxWeek -> context.getString(R.string.widget_term_ended)
        else -> context.getString(R.string.widget_week_number, week)
    }
    views.setTextViewText(R.id.widget_week, " | $weekLabel")
    views.setContentDescription(R.id.widget_date, context.getString(R.string.widget_return_current_week))
    views.setOnClickPendingIntent(
        R.id.widget_date,
        actionPendingIntent(context, WeekScheduleWidgetReceiver::class.java, ACTION_RESET_DATE, appWidgetId),
    )
    views.setBoolean(R.id.widget_previous, "setEnabled", data != null && week > 1)
    views.setBoolean(R.id.widget_next, "setEnabled", data != null && week < maxWeek)
    views.setRemoteAdapter(
        R.id.widget_course_list,
        collectionIntent(context, appWidgetId, WidgetKind.WEEK, 0),
    )
    views.setEmptyView(R.id.widget_course_list, R.id.widget_empty)
    views.setPendingIntentTemplate(R.id.widget_course_list, mainPendingIntent)
    views.setTextViewText(
        R.id.widget_empty,
        when {
            data == null -> context.getString(R.string.widget_no_timetable)
            week < 1 -> context.getString(R.string.widget_courses_not_started)
            week > maxWeek -> context.getString(R.string.widget_term_ended_message)
            else -> context.getString(R.string.widget_no_week_courses)
        },
    )
    views.setOnClickPendingIntent(
        R.id.widget_previous,
        actionPendingIntent(context, WeekScheduleWidgetReceiver::class.java, ACTION_WEEK_PREVIOUS, appWidgetId),
    )
    views.setOnClickPendingIntent(
        R.id.widget_next,
        actionPendingIntent(context, WeekScheduleWidgetReceiver::class.java, ACTION_WEEK_NEXT, appWidgetId),
    )
}

private fun configureHeader(
    context: Context,
    views: RemoteViews,
    timetable: Timetable?,
    date: LocalDate,
    week: Int?,
    compact: Boolean = false,
    appWidgetId: Int? = null,
) {
    views.setTextViewText(
        R.id.widget_date,
        if (compact) "${date.monthValue}.${date.dayOfMonth}" else context.getString(R.string.widget_date_month_day, date.monthValue, date.dayOfMonth),
    )
    views.setTextViewText(R.id.widget_table, tableName(context, timetable, appWidgetId))
    val weekText = when {
        timetable == null -> context.getString(R.string.widget_no_table_header)
        week == null || week < 1 -> context.getString(R.string.widget_term_not_started)
        week > timetable.maxWeek -> context.getString(R.string.widget_term_ended)
        else -> context.getString(R.string.widget_week_number, week)
    }
    if (compact) views.setTextViewText(R.id.widget_week_count, weekText)
    views.setTextViewText(
        R.id.widget_week,
        if (compact) weekdayName(context, date.dayOfWeek.value)
        else context.getString(R.string.widget_week_header, weekText, weekdayName(context, date.dayOfWeek.value)),
    )
}

internal fun collectionIntent(context: Context, appWidgetId: Int, kind: WidgetKind, dayOffset: Int): Intent =
    Intent(context, ScheduleWidgetService::class.java).apply {
        putExtra(EXTRA_WIDGET_ID, appWidgetId)
        putExtra(EXTRA_WIDGET_KIND, kind.name)
        putExtra(EXTRA_DAY_OFFSET, dayOffset)
        data = Uri.parse("sleepdown://widget/$appWidgetId/${kind.name}/$dayOffset")
    }

private fun actionPendingIntent(
    context: Context,
    receiver: Class<out AppWidgetProvider>,
    action: String,
    appWidgetId: Int,
): PendingIntent = PendingIntent.getBroadcast(
    context,
    appWidgetId * 10 + action.hashCode(),
    Intent(context, receiver).apply {
        this.action = action
        putExtra(EXTRA_WIDGET_ID, appWidgetId)
        data = Uri.parse("sleepdown://action/$appWidgetId/${action.hashCode()}")
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

internal fun layoutFor(kind: WidgetKind): Int = when (kind) {
    WidgetKind.NEXT -> R.layout.widget_today_compact
    WidgetKind.TODAY -> R.layout.widget_today
    WidgetKind.TODAY_MODERN -> R.layout.widget_today_modern
    WidgetKind.TODAY_AND_NEXT_DAY -> R.layout.widget_today_next
    WidgetKind.WEEK -> R.layout.widget_week
}

private fun collectionIdsFor(kind: WidgetKind): IntArray = when (kind) {
    WidgetKind.TODAY_AND_NEXT_DAY -> intArrayOf(R.id.widget_course_list, R.id.widget_course_list_next)
    else -> intArrayOf(R.id.widget_course_list)
}

internal sealed interface WidgetEntry {
    data class Course(val item: WidgetSnapshotItem, val snapshot: WidgetSnapshot) : WidgetEntry
    data class Day(
        val date: LocalDate,
        val courses: List<WidgetSnapshotItem>,
        val snapshot: WidgetSnapshot,
    ) : WidgetEntry
}

internal class ScheduleWidgetFactory(
    private val context: Context,
    private val sourceIntent: Intent,
) : RemoteViewsService.RemoteViewsFactory {
    private val appContext = context.withSleepDownAppLocales()
    private var entries: List<WidgetEntry> = emptyList()
    private var kind = WidgetKind.NEXT
    private var weekData: LoadedWidgetData? = null

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val appWidgetId = sourceIntent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        kind = sourceIntent.getStringExtra(EXTRA_WIDGET_KIND).toWidgetKind()
        val dayOffset = sourceIntent.getIntExtra(EXTRA_DAY_OFFSET, 0)
        // Android's legacy adapter conversion waits for this callback while updateAppWidget is running.
        val data = runBlocking(Dispatchers.IO) {
            loadWidgetData(appContext, appWidgetId, bindIfMissing = false)
        }
        weekData = data.takeIf { kind == WidgetKind.WEEK }
        entries = data?.let { buildEntries(appContext, it, appWidgetId, kind, dayOffset) }.orEmpty()
    }

    override fun getCount(): Int = if (kind == WidgetKind.WEEK) {
        if (weekData == null) 0 else 1
    } else entries.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (kind == WidgetKind.WEEK) {
            val data = weekData ?: return null
            if (position != 0) return null
            val snapshot = selectedWeekSnapshot(appContext, data, widgetId())
            return RemoteViews(appContext.packageName, R.layout.widget_week_grid_item).apply {
                setImageViewBitmap(R.id.widget_week_grid, renderWeekWidget(appContext, data, snapshot, widgetId()))
                setContentDescription(R.id.widget_week_grid, weekWidgetDescription(appContext, data, snapshot, widgetId()))
                setOnClickFillInIntent(R.id.widget_week_grid,
                    Intent().putExtra(WIDGET_TABLE_ID_EXTRA, snapshot.timetableId.toLongOrNull() ?: 0L))
            }
        }
        return when (val entry = entries.getOrNull(position)) {
            is WidgetEntry.Course -> courseViews(entry)
            is WidgetEntry.Day -> dayViews(entry)
            null -> null
        }
    }

    override fun getLoadingView(): RemoteViews =
        RemoteViews(appContext.packageName, R.layout.widget_loading).apply {
            setTextViewText(R.id.widget_loading_text, appContext.getString(R.string.widget_loading))
        }

    override fun getViewTypeCount(): Int = widgetViewTypeCount(
        sourceIntent.getStringExtra(EXTRA_WIDGET_KIND).toWidgetKind(),
    )

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    override fun onDestroy() {
        entries = emptyList()
        weekData = null
    }

    private fun courseViews(entry: WidgetEntry.Course): RemoteViews {
        val item = entry.item
        val classic = kind == WidgetKind.TODAY
        val widthDp = widgetWidthDp()
        val wideLayout = usesWideCourseLayout(kind, widthDp)
        val views = RemoteViews(appContext.packageName,
            when {
                classic -> R.layout.widget_course_classic_item
                wideLayout -> R.layout.widget_course_wide_item
                else -> R.layout.widget_course_item
            })
        views.setOnClickFillInIntent(
            R.id.widget_course_item,
            Intent().putExtra(WIDGET_TABLE_ID_EXTRA, entry.snapshot.timetableId.toLongOrNull() ?: 0L),
        )
        val palette = widgetPalette(appContext, widgetId(), kind)
        val style = WidgetStyle.read(appContext, widgetId())
        val colored = !classic || WidgetPreferences.showColorBlocks(appContext, widgetId())
        val ink = if (classic) {
            if (style.textColorCompose) androidx.core.graphics.ColorUtils.compositeColors(style.courseTextColor, courseColor(item)) else style.courseTextColor
        } else palette.primary
        if (classic) {
            views.setInt(R.id.widget_course_card_background, "setColorFilter",
                if (colored) courseColor(item) else palette.background)
            views.setInt(R.id.widget_course_card_background, "setImageAlpha", style.courseOpacity * 255 / 100)
        } else {
            views.setInt(R.id.widget_course_indicator, "setColorFilter", courseColor(item))
            views.setViewVisibility(R.id.widget_course_indicator, if (colored) View.VISIBLE else View.GONE)
        }
        views.setTextColor(R.id.widget_course_name, ink)
        listOf(R.id.widget_course_time, R.id.widget_course_location, R.id.widget_course_teacher).forEach {
            views.setTextColor(it, if (classic) ink else palette.secondary)
        }
        views.setTextViewText(R.id.widget_course_name, courseName(appContext, item))
        views.setTextViewText(R.id.widget_course_time, if (classic) {
            "${MinuteOfDay.format(item.startMinuteOfDay)}\n${MinuteOfDay.format(item.endMinuteOfDay)}"
        } else timeLabel(appContext, item))
        val room = item.room.trim().takeIf { !classic || style.showLocation }.orEmpty()
        val teacher = item.teacher.trim().takeIf { !classic || style.showTeacher }.orEmpty()
        val joinDetails = wideLayout || (kind != WidgetKind.TODAY_MODERN &&
            usesJoinedCourseDetails(kind, widthDp))
        val locationText = if (joinDetails) joinCourseDetails(room, teacher) else room
        views.setTextViewText(R.id.widget_course_location, locationText)
        views.setTextViewText(R.id.widget_course_teacher, if (joinDetails) "" else teacher)
        views.setViewVisibility(R.id.widget_course_location, if (locationText.isBlank()) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_course_teacher, if (!joinDetails && teacher.isNotBlank()) View.VISIBLE else View.GONE)
        views.setViewVisibility(
            R.id.widget_course_detail,
            if (locationText.isBlank() && (joinDetails || teacher.isBlank())) View.GONE else View.VISIBLE,
        )
        if (classic) {
            listOf(R.id.widget_course_name, R.id.widget_course_location, R.id.widget_course_teacher, R.id.widget_course_time).forEach {
                views.setTextViewTextSize(it, TypedValue.COMPLEX_UNIT_SP, style.courseTextSize.toFloat())
            }
        } else applyCourseTextSize(views, if (kind == WidgetKind.TODAY_MODERN) 1.15f else 1f)
        return views
    }

    private fun dayViews(entry: WidgetEntry.Day): RemoteViews {
        val views = RemoteViews(appContext.packageName, R.layout.widget_week_day_item)
        views.setOnClickFillInIntent(
            R.id.widget_week_day_item,
            Intent().putExtra(WIDGET_TABLE_ID_EXTRA, entry.snapshot.timetableId.toLongOrNull() ?: 0L),
        )
        views.setTextViewText(
            R.id.widget_day_title,
            appContext.getString(
                R.string.widget_day_title,
                weekdayName(appContext, entry.date.dayOfWeek.value),
                entry.date.dayOfMonth,
            ),
        )
        val firstItem = entry.courses.firstOrNull()
        if (firstItem != null) {
            views.setInt(R.id.widget_day_indicator, "setColorFilter", courseColor(firstItem))
        }
        views.setViewVisibility(
            R.id.widget_day_indicator,
            if (WidgetPreferences.showColorBlocks(appContext, widgetId()) && firstItem != null) View.VISIBLE else View.GONE,
        )
        val text = entry.courses.joinToString("\n") { item ->
            val room = item.room.trim()
            val suffix = if (room.isBlank()) "" else " · $room"
            "${courseName(appContext, item)}  ${timeLabel(appContext, item)}$suffix"
        }
        views.setTextViewText(R.id.widget_day_courses, text)
        views.setViewVisibility(R.id.widget_day_courses, if (text.isBlank()) View.GONE else View.VISIBLE)
        applyDayTextSize(views, WidgetPreferences.textSizeScale(appContext, widgetId()))
        return views
    }

    private fun widgetId(): Int =
        sourceIntent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    private fun widgetWidthDp(): Int =
        AppWidgetManager.getInstance(appContext).getAppWidgetOptions(widgetId())
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
}

internal fun widgetViewTypeCount(kind: WidgetKind): Int = when (kind) {
    WidgetKind.TODAY_MODERN,
    WidgetKind.TODAY_AND_NEXT_DAY -> 2
    else -> 1
}

internal fun courseWidthDp(widgetWidthDp: Int, kind: WidgetKind): Int =
    if (kind == WidgetKind.TODAY_AND_NEXT_DAY) {
        ((widgetWidthDp - WIDGET_HORIZONTAL_PADDING_DP - DOUBLE_DAY_GAP_DP) / 2).coerceAtLeast(0)
    } else {
        widgetWidthDp.coerceAtLeast(0)
    }

internal fun usesWideCourseLayout(kind: WidgetKind, widgetWidthDp: Int): Boolean = when (kind) {
    WidgetKind.TODAY_MODERN,
    WidgetKind.TODAY_AND_NEXT_DAY -> courseWidthDp(widgetWidthDp, kind) >=
        if (kind == WidgetKind.TODAY_AND_NEXT_DAY) WIDE_DOUBLE_COLUMN_MIN_WIDTH_DP else WIDE_COURSE_MIN_WIDTH_DP
    else -> false
}

internal fun usesJoinedCourseDetails(kind: WidgetKind, widgetWidthDp: Int): Boolean = when (kind) {
    WidgetKind.NEXT,
    WidgetKind.TODAY -> courseWidthDp(widgetWidthDp, kind) >= WIDE_COURSE_MIN_WIDTH_DP
    else -> false
}

internal fun joinCourseDetails(room: String, teacher: String): String =
    listOf(room.trim(), teacher.trim()).filter { it.isNotBlank() }.joinToString(" · ")

internal fun buildEntries(
    context: Context,
    data: LoadedWidgetData,
    appWidgetId: Int,
    kind: WidgetKind,
    dayOffset: Int,
): List<WidgetEntry> = when (kind.snapshotKind) {
    WidgetSnapshotKind.NEXT -> data.snapshots.next.items.map { WidgetEntry.Course(it, data.snapshots.next) }
    WidgetSnapshotKind.TODAY -> {
        val date = LocalDate.ofEpochDay(data.nowEpochDay).plusDays(dayOffset.toLong())
        val snapshot = todaySnapshot(data, date)
        remainingTodayCourseItems(data, date).map { WidgetEntry.Course(it, snapshot) }
    }
    WidgetSnapshotKind.WEEK -> {
        val snapshot = selectedWeekSnapshot(context, data, appWidgetId)
        val byDate = snapshot.items.filter {
            data.isRemainingCourse(it.epochDay, it.endMinuteOfDay)
        }.groupBy { it.epochDay }
        val weekStart = LocalDate.ofEpochDay(snapshot.anchorEpochDay)
        val days = if (snapshot.items.isEmpty()) emptyList() else displayedDays(data.timetable)
        days.map { day ->
            val date = weekStart.plusDays((day - 1).toLong())
            WidgetEntry.Day(date, byDate[date.toEpochDay()].orEmpty(), snapshot)
        }
    }
}

private fun todaySnapshot(data: LoadedWidgetData, date: LocalDate): WidgetSnapshot =
    if (date.toEpochDay() == data.nowEpochDay) {
        data.snapshots.today
    } else {
        WidgetSnapshotBuilder.build(
            WidgetSnapshotKind.TODAY,
            data.timetable,
            date.toEpochDay(),
            data.nowMinuteOfDay,
        )
    }

internal fun selectedWeekSnapshot(context: Context, data: LoadedWidgetData, appWidgetId: Int): WidgetSnapshot {
    val maxWeek = data.timetable.maxWeek.coerceAtLeast(1)
    val selected = WidgetPreferences.week(context, appWidgetId).takeIf { it > 0 } ?: return data.snapshots.week
    val week = selected.coerceIn(1, maxWeek)
    val weekStart = LocalDate.ofEpochDay(data.timetable.firstDayEpochDay)
        .plusWeeks((week - 1).toLong())
        .toEpochDay()
    return WidgetSnapshotBuilder.build(
        WidgetSnapshotKind.WEEK,
        data.timetable,
        weekStart,
        data.nowMinuteOfDay,
    )
}

internal fun displayedDays(timetable: Timetable): List<Int> {
    val ordered = if (timetable.sundayFirst) {
        listOf(7, 1, 2, 3, 4, 5, 6)
    } else {
        listOf(1, 2, 3, 4, 5, 6, 7)
    }
    return ordered.filter { day ->
        when (day) {
            6 -> timetable.showSaturday
            7 -> timetable.showSunday
            else -> true
        }
    }
}

private fun academicWeek(timetable: Timetable, date: LocalDate): Int =
    Math.floorDiv(date.toEpochDay() - timetable.firstDayEpochDay, 7L).toInt() + 1

private fun tableName(context: Context, timetable: Timetable?, appWidgetId: Int? = null): String {
    val width = appWidgetId?.let {
        AppWidgetManager.getInstance(context).getAppWidgetOptions(it).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
    }
    return timetable?.name?.trim().orEmpty().ifBlank { context.getString(R.string.widget_my_timetable) }
        .take(if (width != null && width <= 250) 6 else 12)
}

private fun courseName(context: Context, item: WidgetSnapshotItem): String {
    val name = item.courseName.trim().ifBlank { context.getString(R.string.widget_unnamed_course) }
    return if (item.conflictCount > 1) {
        if (item.isPreferred) {
            context.getString(R.string.widget_conflict_preferred, name, item.conflictCount)
        } else {
            context.getString(R.string.widget_conflict, name, item.conflictCount)
        }
    } else {
        name
    }
}

private fun timeLabel(context: Context, item: WidgetSnapshotItem): String {
    val start = runCatching { MinuteOfDay.format(item.startMinuteOfDay) }.getOrNull()
    val end = runCatching { MinuteOfDay.format(item.endMinuteOfDay) }.getOrNull()
    return if (start != null && end != null) "$start-$end" else context.getString(R.string.widget_time_undetermined)
}

internal fun emptyMessage(context: Context, data: LoadedWidgetData?, date: LocalDate): String {
    if (data == null) return context.getString(R.string.widget_no_timetable)
    val week = academicWeek(data.timetable, date)
    return when {
        week < 1 -> context.getString(R.string.widget_courses_not_started)
        week > data.timetable.maxWeek -> context.getString(R.string.widget_term_ended_message)
        date.toEpochDay() == data.nowEpochDay -> context.getString(R.string.widget_no_remaining_courses)
        else -> {
            val label = when (date.toEpochDay() - data.nowEpochDay) {
                0L -> context.getString(R.string.widget_today_short)
                1L -> context.getString(R.string.widget_tomorrow_short)
                else -> context.getString(R.string.widget_date_month_day, date.monthValue, date.dayOfMonth)
            }
            context.getString(R.string.widget_no_courses_on_day, label)
        }
    }
}

internal fun weekdayName(context: Context, day: Int): String =
    when (((day - 1) % 7 + 7) % 7) {
        0 -> context.getString(R.string.widget_weekday_monday)
        1 -> context.getString(R.string.widget_weekday_tuesday)
        2 -> context.getString(R.string.widget_weekday_wednesday)
        3 -> context.getString(R.string.widget_weekday_thursday)
        4 -> context.getString(R.string.widget_weekday_friday)
        5 -> context.getString(R.string.widget_weekday_saturday)
        else -> context.getString(R.string.widget_weekday_sunday)
    }

private fun courseColor(item: WidgetSnapshotItem): Int =
    item.color.takeUnless { it == 0 } ?: CourseColors.colorFor(item.courseName)

private fun String?.toWidgetKind(): WidgetKind = when (this) {
    "TODAY" -> WidgetKind.TODAY
    "TODAY_MODERN" -> WidgetKind.TODAY_MODERN
    "TODAY_AND_NEXT_DAY" -> WidgetKind.TODAY_AND_NEXT_DAY
    "WEEK" -> WidgetKind.WEEK
    "TODAY_COMPACT", "NEXT", null -> WidgetKind.NEXT
    else -> WidgetKind.NEXT
}

internal fun applyWidgetStyle(context: Context, views: RemoteViews, appWidgetId: Int, kind: WidgetKind) {
    val palette = widgetPalette(context, appWidgetId, kind)
    val style = WidgetStyle.read(context, appWidgetId)
    val modern = kind != WidgetKind.TODAY && kind != WidgetKind.WEEK
    views.setInt(R.id.widget_background, "setColorFilter", palette.background)
    views.setTextColor(R.id.widget_date, palette.primary)
    views.setTextColor(R.id.widget_table, palette.secondary)
    views.setTextColor(R.id.widget_week, palette.accent)
    views.setTextColor(R.id.widget_empty, palette.secondary)
    if (kind == WidgetKind.TODAY_AND_NEXT_DAY) {
        views.setTextColor(R.id.widget_today_label, palette.secondary)
        views.setTextColor(R.id.widget_next_day_label, palette.secondary)
        views.setTextColor(R.id.widget_empty_next, palette.secondary)
    }
    if (kind == WidgetKind.TODAY || kind == WidgetKind.WEEK) {
        views.setInt(R.id.widget_settings, "setColorFilter", palette.secondary)
        views.setInt(R.id.widget_previous, "setColorFilter", palette.secondary)
        views.setInt(R.id.widget_next, "setColorFilter", palette.secondary)
    } else {
        views.setTextColor(R.id.widget_week_count, palette.secondary)
        views.setViewVisibility(R.id.widget_week_count, if (kind == WidgetKind.NEXT) View.GONE else View.VISIBLE)
    }
    val showBackground = WidgetPreferences.showBackground(context, appWidgetId)
    val imageBackground = !modern && showBackground && style.backgroundImage.isNotBlank()
    if (!modern) {
        views.setViewVisibility(R.id.widget_background_image, if (imageBackground) View.VISIBLE else View.GONE)
        views.setImageViewBitmap(R.id.widget_background_image,
            if (imageBackground) loadWidgetBackground(context, Uri.parse(style.backgroundImage)) else null)
    }
    views.setViewVisibility(
        R.id.widget_background,
        if (modern || (showBackground && !imageBackground)) View.VISIBLE else View.GONE,
    )
    views.setInt(R.id.widget_background, "setImageAlpha", if (modern) 255 else style.backgroundOpacity * 255 / 100)
    views.setViewVisibility(R.id.widget_header, if (modern || style.showHeader) View.VISIBLE else View.GONE)
    if (!modern) {
        views.setViewVisibility(R.id.widget_date, if (style.showDate) View.VISIBLE else View.GONE)
        listOf(R.id.widget_previous, R.id.widget_settings, R.id.widget_next).forEach {
            views.setViewVisibility(it, if (style.showButtons) View.VISIBLE else View.GONE)
        }
        if (kind == WidgetKind.TODAY && style.showButtons) {
            val tomorrow = WidgetPreferences.dayOffset(context, appWidgetId) == 1
            views.setViewVisibility(R.id.widget_previous, if (tomorrow) View.VISIBLE else View.INVISIBLE)
            views.setViewVisibility(R.id.widget_next, if (tomorrow) View.INVISIBLE else View.VISIBLE)
        }
        val width = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
        val buttonsWidth = if (!style.showButtons) 0 else if (kind == WidgetKind.TODAY) 64 else 96
        views.setInt(R.id.widget_table, "setMaxWidth", ((width - 24 - buttonsWidth).coerceAtLeast(40) * 0.4f * context.resources.displayMetrics.density).toInt())
    }
    val headerSize = if (kind == WidgetKind.NEXT) 12f else 13f
    val sizeById = mutableMapOf(
        R.id.widget_date to if (modern) headerSize else style.headerTextSize + 2f,
        R.id.widget_table to if (modern) headerSize else style.headerTextSize.toFloat(),
        R.id.widget_week to if (modern) headerSize else style.headerTextSize.toFloat(),
        R.id.widget_empty to 13f,
    )
    if (modern) sizeById[R.id.widget_week_count] = headerSize
    if (kind == WidgetKind.TODAY_AND_NEXT_DAY) {
        sizeById[R.id.widget_today_label] = 12f
        sizeById[R.id.widget_next_day_label] = 12f
        sizeById[R.id.widget_empty_next] = 13f
    }
    sizeById.forEach { (viewId, size) ->
        views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, size)
    }
}

private fun applyCourseTextSize(views: RemoteViews, scale: Float) {
    views.setTextViewTextSize(R.id.widget_course_name, TypedValue.COMPLEX_UNIT_SP, 15f * scale)
    views.setTextViewTextSize(R.id.widget_course_location, TypedValue.COMPLEX_UNIT_SP, 13f * scale)
    views.setTextViewTextSize(R.id.widget_course_teacher, TypedValue.COMPLEX_UNIT_SP, 13f * scale)
    views.setTextViewTextSize(R.id.widget_course_time, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
}

private fun applyDayTextSize(views: RemoteViews, scale: Float) {
    views.setTextViewTextSize(R.id.widget_day_title, TypedValue.COMPLEX_UNIT_SP, 13f * scale)
    views.setTextViewTextSize(R.id.widget_day_courses, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
}

/** Refresh every placed SleepDown widget after a timetable change. */
public suspend fun refreshScheduleWidgets(context: Context) = withContext(Dispatchers.IO) {
    val manager = AppWidgetManager.getInstance(context.applicationContext)
    val providers = listOf(
        ScheduleWidgetReceiver::class.java to WidgetKind.NEXT,
        TodayCourseWidgetReceiver::class.java to WidgetKind.TODAY,
        TodayModernWidgetReceiver::class.java to WidgetKind.TODAY_MODERN,
        TodayAndNextDayWidgetReceiver::class.java to WidgetKind.TODAY_AND_NEXT_DAY,
        WeekScheduleWidgetReceiver::class.java to WidgetKind.WEEK,
    )
    providers.forEach { (receiver, kind) ->
        manager.getAppWidgetIds(ComponentName(context, receiver)).forEach { appWidgetId ->
            refreshWidget(context, manager, appWidgetId, kind)
        }
    }
}

/** Backward-compatible name for callers that only knew the original widget. */
public suspend fun refreshScheduleWidget(context: Context) {
    refreshScheduleWidgets(context)
}
