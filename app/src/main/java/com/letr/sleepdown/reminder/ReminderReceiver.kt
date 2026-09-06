package com.letr.sleepdown.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.letr.sleepdown.R
import com.letr.sleepdown.withSleepDownAppLocales

/** Turns one alarm broadcast into a user-visible course reminder notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_REMINDER) return
        val localizedContext = context.withSleepDownAppLocales()

        val planId = intent.getStringExtra(EXTRA_PLAN_ID).orEmpty()
        if (planId.isBlank()) {
            Log.w(TAG, "Ignoring reminder broadcast without a plan id")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission is not granted; reminder=$planId was not posted")
            return
        }

        val notificationManager = NotificationManagerCompat.from(localizedContext)
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications are disabled; reminder=$planId was not posted")
            return
        }

        val silent = intent.getBooleanExtra(EXTRA_SILENT, false)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val channelId = channelIdFor(silent = silent, vibrate = vibrate)
        ensureNotificationChannels(localizedContext)

        val title = intent.getStringExtra(EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle(localizedContext, intent.getStringExtra(EXTRA_KIND))
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val notification = NotificationCompat.Builder(localizedContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setSilent(silent)
            .setVibrate(if (vibrate && !silent) VIBRATION_PATTERN else longArrayOf(0L))
            .build()

        try {
            notificationManager.notify(ReminderScheduler.notificationIdFor(planId), notification)
        } catch (error: SecurityException) {
            Log.w(TAG, "Notification permission changed before posting reminder=$planId", error)
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = ReminderScheduler.ACTION_SHOW_REMINDER
        const val EXTRA_PLAN_ID = ReminderScheduler.EXTRA_PLAN_ID
        const val EXTRA_OCCURRENCE_ID = ReminderScheduler.EXTRA_OCCURRENCE_ID
        const val EXTRA_KIND = ReminderScheduler.EXTRA_KIND
        const val EXTRA_TITLE = ReminderScheduler.EXTRA_TITLE
        const val EXTRA_BODY = ReminderScheduler.EXTRA_BODY
        const val EXTRA_VIBRATE = ReminderScheduler.EXTRA_VIBRATE
        const val EXTRA_SILENT = ReminderScheduler.EXTRA_SILENT

        const val CHANNEL_ID = "course_reminders"
        const val CHANNEL_NO_VIBRATION_ID = "course_reminders_no_vibration"
        const val CHANNEL_SILENT_ID = "course_reminders_silent"

        private const val TAG = "ReminderReceiver"
        private val VIBRATION_PATTERN = longArrayOf(0L, 250L, 150L, 250L)

        fun channelIdFor(silent: Boolean, vibrate: Boolean): String = when {
            silent -> CHANNEL_SILENT_ID
            vibrate -> CHANNEL_ID
            else -> CHANNEL_NO_VIBRATION_ID
        }

        fun ensureNotificationChannels(context: Context) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val normalChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setSound(sound, audioAttributes)
            }
            val noVibrationChannel = NotificationChannel(
                CHANNEL_NO_VIBRATION_ID,
                context.getString(R.string.reminder_channel_no_vibration_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.reminder_channel_no_vibration_description)
                enableVibration(false)
                setSound(sound, audioAttributes)
            }
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT_ID,
                context.getString(R.string.reminder_channel_silent_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.reminder_channel_silent_description)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannels(
                listOf(normalChannel, noVibrationChannel, silentChannel),
            )
        }

        private fun fallbackTitle(context: Context, kind: String?): String =
            if (kind == "LESSON_END") {
                context.getString(R.string.reminder_lesson_end)
            } else {
                context.getString(R.string.reminder_upcoming_lesson)
            }
    }
}
