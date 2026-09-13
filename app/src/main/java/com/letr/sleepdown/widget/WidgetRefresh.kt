package com.letr.sleepdown.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private const val REFRESH_REQUEST_CODE = 920000

private const val TAG = "ScheduleWidget"

/** Wall clock of the local midnight after [nowMillis], when date-dependent content changes. */
internal fun nextWidgetRefreshMillis(nowMillis: Long, zoneId: ZoneId): Long =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

/**
 * Arms the alarm that refreshes placed widgets at the next local midnight.
 *
 * Providers only receive the system's periodic update broadcast when the system decides to
 * deliver it, so a widget that shows the current day can otherwise stay on the previous day
 * indefinitely. Every widget refresh re-arms this alarm, and an app without placed widgets
 * stops being woken because nothing schedules the next one.
 *
 * Best effort: a failure here is logged and swallowed so that it cannot fail the widget update
 * that requested it.
 */
internal fun scheduleNextWidgetRefresh(
    context: Context,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    try {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REFRESH_REQUEST_CODE,
            Intent(appContext, WidgetRefreshReceiver::class.java)
                .setAction(WidgetRefreshReceiver.ACTION_REFRESH_WIDGETS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAtMillis = nextWidgetRefreshMillis(nowMillis, zoneId)
        val exactAlarmAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAlarmAllowed) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Special access can be revoked between the permission check and this call.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } catch (error: Exception) {
        Log.e(TAG, "Failed to schedule the next widget refresh", error)
    }
}

/** Refreshes placed widgets when [scheduleNextWidgetRefresh] fires at the date rollover. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH_WIDGETS) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                refreshScheduleWidgets(appContext)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Scheduled widget refresh failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGETS = "com.letr.sleepdown.widget.REFRESH_WIDGETS"
    }
}
