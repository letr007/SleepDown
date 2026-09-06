package com.letr.sleepdown.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.letr.sleepdown.AppContainer
import com.letr.sleepdown.R
import com.letr.sleepdown.withSleepDownAppLocales
import com.letr.sleepdown.domain.ReminderPlan
import com.letr.sleepdown.domain.ReminderPlanner
import com.letr.sleepdown.domain.Timetable
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The platform state that can prevent a reminder from being delivered. */
data class ReminderPermissionStatus(
    val notificationPermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val exactAlarmAllowed: Boolean,
) {
    val notificationReady: Boolean
        get() = notificationPermissionGranted && notificationsEnabled

    val canSchedule: Boolean
        get() = notificationReady
}

enum class ReminderScheduleBlockReason {
    NOTIFICATION_PERMISSION,
    NOTIFICATIONS_DISABLED,
    EXACT_ALARM_PERMISSION,
    EXACT_ALARM_SECURITY_EXCEPTION,
}

data class ReminderScheduleReport(
    val permissionStatus: ReminderPermissionStatus,
    val scheduledPlanIds: List<String> = emptyList(),
    val skippedPastPlanIds: List<String> = emptyList(),
    val cancelledPlanIds: List<String> = emptyList(),
    val blockedReason: ReminderScheduleBlockReason? = null,
    val usedInexactFallback: Boolean = false,
) {
    val isSuccess: Boolean
        get() = blockedReason == null
}

/**
 * Schedules each reminder as its own exact alarm. Alarm identity is derived from
 * the stable domain plan id, so an edited timetable can cancel old alarms without
 * depending on generated Room ids or list positions.
 */
class ReminderScheduler(
    context: Context,
    private val alarmManager: AlarmManager = requireNotNull(
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager,
    ),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val appContext = context.applicationContext.withSleepDownAppLocales()
    private val preferences: SharedPreferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    /** Returns permission state without attempting to schedule or requesting access. */
    fun permissionStatus(): ReminderPermissionStatus {
        val notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notificationsEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        val exactAlarmAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        return ReminderPermissionStatus(
            notificationPermissionGranted = notificationPermissionGranted,
            notificationsEnabled = notificationsEnabled,
            exactAlarmAllowed = exactAlarmAllowed,
        )
    }

    /** Schedules or replaces one plan without touching other scheduled plans. */
    fun schedule(plan: ReminderPlan): ReminderScheduleReport {
        require(plan.id.isNotBlank())
        val cancelled = cancelAlarmOnly(plan.id)
        forgetPlan(plan.id)
        val permissionStatus = permissionStatus()
        val triggerAtMillis = triggerAtMillis(plan)
        if (triggerAtMillis <= clockMillis()) {
            return ReminderScheduleReport(
                permissionStatus = permissionStatus,
                skippedPastPlanIds = listOf(plan.id),
                cancelledPlanIds = if (cancelled) listOf(plan.id) else emptyList(),
            )
        }

        permissionStatus.blockReason()?.let { reason ->
            return ReminderScheduleReport(
                permissionStatus = permissionStatus,
                cancelledPlanIds = if (cancelled) listOf(plan.id) else emptyList(),
                blockedReason = reason,
            )
        }

        return try {
            val usedInexactFallback = scheduleAlarm(plan, triggerAtMillis, permissionStatus.exactAlarmAllowed)
            rememberPlans(scheduledPlanIds() + plan.id)
            ReminderScheduleReport(
                permissionStatus = permissionStatus,
                scheduledPlanIds = listOf(plan.id),
                cancelledPlanIds = if (cancelled) listOf(plan.id) else emptyList(),
                usedInexactFallback = usedInexactFallback,
            )
        } catch (_: SecurityException) {
            cancelAlarmOnly(plan.id)
            ReminderScheduleReport(
                permissionStatus = permissionStatus.copy(exactAlarmAllowed = false),
                cancelledPlanIds = if (cancelled) listOf(plan.id) else emptyList(),
                blockedReason = ReminderScheduleBlockReason.EXACT_ALARM_SECURITY_EXCEPTION,
            )
        }
    }

    /** Schedules a complete replacement set and removes every previously tracked alarm. */
    fun reschedule(plans: Collection<ReminderPlan>): ReminderScheduleReport {
        plans.forEach { plan ->
            require(plan.id.isNotBlank())
        }
        val previouslyScheduled = scheduledPlanIds()
        val cancelledPlanIds = previouslyScheduled.filter { cancelAlarmOnly(it) }
        forgetAllPlans()

        val permissionStatus = permissionStatus()
        val now = clockMillis()
        val futurePlans = filterFuturePlans(plans)
        val skippedPastPlanIds = plans
            .asSequence()
            .distinctBy { it.id }
            .filter { triggerAtMillis(it) <= now }
            .map { it.id }
            .toList()
        if (futurePlans.isEmpty()) {
            return ReminderScheduleReport(
                permissionStatus = permissionStatus,
                skippedPastPlanIds = skippedPastPlanIds,
                cancelledPlanIds = cancelledPlanIds,
            )
        }

        permissionStatus.blockReason()?.let { reason ->
            return ReminderScheduleReport(
                permissionStatus = permissionStatus,
                skippedPastPlanIds = skippedPastPlanIds,
                cancelledPlanIds = cancelledPlanIds,
                blockedReason = reason,
            )
        }

        val scheduled = mutableListOf<String>()
        var usedInexactFallback = false
        return try {
            futurePlans.forEach { plan ->
                usedInexactFallback = scheduleAlarm(
                    plan,
                    triggerAtMillis(plan),
                    permissionStatus.exactAlarmAllowed,
                ) || usedInexactFallback
                scheduled += plan.id
            }
            rememberPlans(scheduled.toSet())
            ReminderScheduleReport(
                permissionStatus = permissionStatus,
                scheduledPlanIds = scheduled,
                skippedPastPlanIds = skippedPastPlanIds,
                cancelledPlanIds = cancelledPlanIds,
                usedInexactFallback = usedInexactFallback,
            )
        } catch (_: SecurityException) {
            futurePlans.forEach { plan -> cancelAlarmOnly(plan.id) }
            forgetAllPlans()
            ReminderScheduleReport(
                permissionStatus = permissionStatus.copy(exactAlarmAllowed = false),
                skippedPastPlanIds = skippedPastPlanIds,
                cancelledPlanIds = cancelledPlanIds,
                blockedReason = ReminderScheduleBlockReason.EXACT_ALARM_SECURITY_EXCEPTION,
            )
        }
    }

    fun schedule(plans: List<ReminderPlan>): ReminderScheduleReport = reschedule(plans)

    fun schedulePlan(plan: ReminderPlan): ReminderScheduleReport = schedule(plan)

    fun schedulePlans(plans: Collection<ReminderPlan>): ReminderScheduleReport = reschedule(plans)

    /** Rebuilds alarms for one already-loaded timetable. */
    fun rebuild(timetable: Timetable): ReminderScheduleReport =
        reschedule(ReminderPlanner.plan(timetable))

    /** Loads the selected local timetable and rebuilds its future reminder alarms. */
    suspend fun rebuildCurrentTable(): ReminderScheduleReport {
        val repository = AppContainer.repo(appContext)
        val selectedId = AppContainer.currentTableId(appContext)
        val defaultName = appContext.getString(R.string.default_table_name)
        val tableId = selectedId.takeIf { it > 0L } ?: repository.ensureDefaultTable(defaultName).also {
            AppContainer.setCurrentTableId(appContext, it)
        }
        val timetable = repository.loadDomainTimetableOrNull(tableId)
            ?: repository.ensureDefaultTable(defaultName).let { fallbackId ->
                AppContainer.setCurrentTableId(appContext, fallbackId)
                repository.loadDomainTimetable(fallbackId)
            }
        return rebuild(timetable)
    }

    /** Cancels one tracked or untracked alarm by its stable plan id. */
    fun cancel(planId: String): Boolean {
        require(planId.isNotBlank())
        val cancelled = cancelAlarmOnly(planId)
        forgetPlan(planId)
        return cancelled
    }

    fun cancel(plan: ReminderPlan): Boolean = cancel(plan.id)

    /** Cancels all alarms recorded by this scheduler. */
    fun cancelAll(): List<String> {
        val ids = scheduledPlanIds()
        val cancelled = ids.filter { cancelAlarmOnly(it) }
        forgetAllPlans()
        return cancelled
    }

    /** Filters duplicate and already-expired plans using the scheduler clock. */
    fun filterFuturePlans(plans: Collection<ReminderPlan>): List<ReminderPlan> {
        val now = clockMillis()
        return plans.asSequence()
            .distinctBy { it.id }
            .filter { triggerAtMillis(it) > now }
            .toList()
    }

    fun triggerAtMillis(plan: ReminderPlan): Long =
        ZonedDateTime.of(
            LocalDate.ofEpochDay(plan.triggerEpochDay),
            LocalTime.MIDNIGHT,
            zoneId(),
        ).plusMinutes(plan.triggerMinuteOfDay.toLong())
            .toInstant()
            .toEpochMilli()

    fun scheduledPlanIds(): Set<String> =
        preferences.getStringSet(SCHEDULED_PLAN_IDS_KEY, emptySet()).orEmpty().toSet()

    private fun cancelAlarmOnly(planId: String): Boolean {
        val pendingIntent = pendingIntentForExisting(appContext, planId) ?: return false
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        return true
    }

    private fun rememberPlans(planIds: Set<String>) {
        preferences.edit().putStringSet(SCHEDULED_PLAN_IDS_KEY, planIds).apply()
    }

    private fun forgetPlan(planId: String) {
        val updated = scheduledPlanIds() - planId
        preferences.edit().putStringSet(SCHEDULED_PLAN_IDS_KEY, updated).apply()
    }

    private fun forgetAllPlans() {
        preferences.edit().remove(SCHEDULED_PLAN_IDS_KEY).apply()
    }

    private fun ReminderPermissionStatus.blockReason(): ReminderScheduleBlockReason? = when {
        !notificationPermissionGranted -> ReminderScheduleBlockReason.NOTIFICATION_PERMISSION
        !notificationsEnabled -> ReminderScheduleBlockReason.NOTIFICATIONS_DISABLED
        else -> null
    }

    private fun scheduleAlarm(
        plan: ReminderPlan,
        triggerAtMillis: Long,
        exactAlarmAllowed: Boolean,
    ): Boolean {
        val pendingIntent = pendingIntentFor(appContext, plan)
        if (exactAlarmAllowed) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return false
            } catch (_: SecurityException) {
                // Special access may be revoked between the permission check and this call.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        return true
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.letr.sleepdown.reminder.SHOW_REMINDER"
        const val EXTRA_PLAN_ID = "com.letr.sleepdown.reminder.PLAN_ID"
        const val EXTRA_OCCURRENCE_ID = "com.letr.sleepdown.reminder.OCCURRENCE_ID"
        const val EXTRA_KIND = "com.letr.sleepdown.reminder.KIND"
        const val EXTRA_TITLE = "com.letr.sleepdown.reminder.TITLE"
        const val EXTRA_BODY = "com.letr.sleepdown.reminder.BODY"
        const val EXTRA_VIBRATE = "com.letr.sleepdown.reminder.VIBRATE"
        const val EXTRA_SILENT = "com.letr.sleepdown.reminder.SILENT"

        private const val PREFERENCES_NAME = "reminder_scheduler"
        private const val SCHEDULED_PLAN_IDS_KEY = "scheduled_plan_ids"
        private const val REMINDER_URI_SCHEME = "sleepdown"
        private const val REMINDER_URI_HOST = "reminder"

        fun pendingIntentFor(context: Context, planId: String): PendingIntent {
            require(planId.isNotBlank())
            return PendingIntent.getBroadcast(
                context.applicationContext,
                requestCodeFor(planId),
                identityIntent(context, planId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun pendingIntentFor(context: Context, plan: ReminderPlan): PendingIntent {
            require(plan.id.isNotBlank())
            return PendingIntent.getBroadcast(
                context.applicationContext,
                requestCodeFor(plan.id),
                intentFor(context, plan),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun intentFor(context: Context, plan: ReminderPlan): Intent =
            identityIntent(context, plan.id).apply {
                putExtra(EXTRA_PLAN_ID, plan.id)
                putExtra(EXTRA_OCCURRENCE_ID, plan.occurrenceId)
                putExtra(EXTRA_KIND, plan.kind.name)
                putExtra(
                    EXTRA_TITLE,
                    context.getString(
                        if (plan.kind.name == "LESSON_END") {
                            R.string.reminder_lesson_end
                        } else {
                            R.string.reminder_upcoming_lesson
                        },
                    ),
                )
                putExtra(EXTRA_BODY, plan.body)
                putExtra(EXTRA_VIBRATE, plan.vibrate)
                putExtra(EXTRA_SILENT, plan.silent)
            }

        fun requestCodeFor(planId: String): Int = planId.hashCode()

        fun notificationIdFor(planId: String): Int = planId.hashCode()

        fun exactAlarmSettingsIntent(context: Context): Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(
                    Uri.parse("package:${context.applicationContext.packageName}"),
                )
            } else {
                null
            }

        fun permissionStatus(context: Context): ReminderPermissionStatus =
            ReminderScheduler(context).permissionStatus()

        private fun identityIntent(context: Context, planId: String): Intent =
            Intent(context.applicationContext, ReminderReceiver::class.java)
                .setAction(ACTION_SHOW_REMINDER)
                .setData(Uri.parse("$REMINDER_URI_SCHEME://$REMINDER_URI_HOST/${Uri.encode(planId)}"))

        private fun pendingIntentForExisting(context: Context, planId: String): PendingIntent? =
            PendingIntent.getBroadcast(
                context.applicationContext,
                requestCodeFor(planId),
                identityIntent(context, planId),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
