package com.letr.sleepdown.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.letr.sleepdown.widget.refreshScheduleWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Rebuilds date-based alarms after Android changes the wall clock or timezone. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in REBUILD_ACTIONS) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val report = ReminderScheduler(appContext).rebuildCurrentTable()
                if (report.blockedReason != null) {
                    Log.w(
                        TAG,
                        "Reminder rebuild was blocked: reason=${report.blockedReason}, " +
                            "permissions=${report.permissionStatus}",
                    )
                } else {
                    Log.i(TAG, "Reminder rebuild scheduled ${report.scheduledPlanIds.size} plan(s)")
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to rebuild course reminders", error)
            } finally {
                try {
                    refreshScheduleWidgets(appContext)
                } catch (error: Exception) {
                    Log.e(TAG, "Failed to refresh widgets after clock change", error)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReschedule"
        private const val ACTION_TIME_SET = "android.intent.action.TIME_SET"
        private const val ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"
        private val REBUILD_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_TIME_SET,
            ACTION_TIMEZONE_CHANGED,
        )
    }
}
