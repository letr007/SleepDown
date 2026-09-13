package com.letr.sleepdown.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class WidgetRefreshScheduleTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")

    private fun millis(zoneId: ZoneId, dateTime: LocalDateTime): Long =
        ZonedDateTime.of(dateTime, zoneId).toInstant().toEpochMilli()

    @Test
    fun refreshTargetsTheNextLocalMidnight() {
        assertEquals(
            millis(shanghai, LocalDateTime.of(2026, 9, 13, 0, 0)),
            nextWidgetRefreshMillis(millis(shanghai, LocalDateTime.of(2026, 9, 12, 23, 59, 30)), shanghai),
        )
    }

    @Test
    fun midnightItselfSchedulesTheFollowingDay() {
        assertEquals(
            millis(shanghai, LocalDateTime.of(2026, 9, 13, 0, 0)),
            nextWidgetRefreshMillis(millis(shanghai, LocalDateTime.of(2026, 9, 12, 0, 0)), shanghai),
        )
    }

    @Test
    fun refreshUsesTheProvidedZone() {
        val utc = ZoneId.of("UTC")
        assertEquals(
            millis(utc, LocalDateTime.of(2026, 9, 13, 0, 0)),
            nextWidgetRefreshMillis(millis(shanghai, LocalDateTime.of(2026, 9, 12, 20, 0)), utc),
        )
    }
}
