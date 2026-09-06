package com.letr.sleepdown.reminder

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.R
import com.letr.sleepdown.domain.ReminderKind
import com.letr.sleepdown.domain.ReminderPlan
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {
    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test
    fun pendingIntentIdentityIsStableForTheSamePlanId() {
        val first = ReminderScheduler.pendingIntentFor(context, "stable-plan")
        val samePlan = ReminderScheduler.pendingIntentFor(context, "stable-plan")
        val otherPlan = ReminderScheduler.pendingIntentFor(context, "other-plan")

        try {
            assertEquals(first, samePlan)
            assertNotEquals(first, otherPlan)
            assertEquals(
                ReminderScheduler.requestCodeFor("stable-plan"),
                ReminderScheduler.requestCodeFor("stable-plan"),
            )
            assertNotEquals(
                ReminderScheduler.requestCodeFor("stable-plan"),
                ReminderScheduler.requestCodeFor("other-plan"),
            )
        } finally {
            first.cancel()
            otherPlan.cancel()
        }
    }

    @Test
    fun filterFuturePlansRemovesExpiredAndDuplicatePlanIds() {
        val now = LocalDate.of(2026, 1, 1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val scheduler = ReminderScheduler(
            context = context,
            clockMillis = { now },
            zoneId = { ZoneOffset.UTC },
        )
        val future = reminderPlan("future", 1, 5)
        val futureDuplicate = reminderPlan("future", 1, 6)
        val past = reminderPlan("past", -1, 5)

        val result = scheduler.filterFuturePlans(listOf(past, future, futureDuplicate))

        assertEquals(listOf("future"), result.map { it.id })
    }

    @Test
    fun reminderIntentCarriesContentAndReceiverChannelPolicy() {
        val plan = reminderPlan("content", 1, 8).copy(
            title = "即将上课",
            body = "数学 · 教学楼 A",
            vibrate = true,
            silent = false,
        )

        val intent = ReminderScheduler.intentFor(context, plan)

        assertEquals(ReminderScheduler.ACTION_SHOW_REMINDER, intent.action)
        assertEquals(plan.id, intent.getStringExtra(ReminderScheduler.EXTRA_PLAN_ID))
        assertEquals(
            context.getString(R.string.reminder_upcoming_lesson),
            intent.getStringExtra(ReminderScheduler.EXTRA_TITLE),
        )
        assertEquals(plan.body, intent.getStringExtra(ReminderScheduler.EXTRA_BODY))
        assertTrue(intent.getBooleanExtra(ReminderScheduler.EXTRA_VIBRATE, false))
        assertEquals(ReminderReceiver.CHANNEL_ID, ReminderReceiver.channelIdFor(false, true))
        assertEquals(ReminderReceiver.CHANNEL_NO_VIBRATION_ID, ReminderReceiver.channelIdFor(false, false))
        assertEquals(ReminderReceiver.CHANNEL_SILENT_ID, ReminderReceiver.channelIdFor(true, true))
    }

    @Test
    fun exactAlarmAccessIsAnExplicitDegradationNotACompleteBlock() {
        val status = ReminderPermissionStatus(
            notificationPermissionGranted = true,
            notificationsEnabled = true,
            exactAlarmAllowed = false,
        )
        assertTrue(status.canSchedule)
    }

    @Test
    fun notificationChannelsHaveExpectedContractWhenSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        ReminderReceiver.ensureNotificationChannels(context)
        val manager = requireNotNull(
            context.getSystemService(android.app.NotificationManager::class.java),
        )

        assertNotNull(manager.getNotificationChannel(ReminderReceiver.CHANNEL_ID))
        assertNotNull(manager.getNotificationChannel(ReminderReceiver.CHANNEL_NO_VIBRATION_ID))
        assertNotNull(manager.getNotificationChannel(ReminderReceiver.CHANNEL_SILENT_ID))
    }

    private fun reminderPlan(id: String, epochDayOffset: Long, minuteOfDay: Int): ReminderPlan {
        val epochDay = LocalDate.of(2026, 1, 1).toEpochDay() + epochDayOffset
        return ReminderPlan(
            id = id,
            occurrenceId = "occurrence-$id",
            kind = ReminderKind.LESSON_START,
            eventEpochDay = epochDay,
            eventMinuteOfDay = minuteOfDay,
            triggerEpochDay = epochDay,
            triggerMinuteOfDay = minuteOfDay,
            leadMinutes = 0,
            title = "title-$id",
            body = "body-$id",
            vibrate = true,
            silent = false,
        )
    }
}
