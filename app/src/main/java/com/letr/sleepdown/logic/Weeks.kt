package com.letr.sleepdown.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 周次与日期换算 */
object Weeks {

    /** date 处于从 startDate（第一周周一）开始的第几周，未开学返回 0，超过范围返回 maxWeek+1 */
    fun weekOf(startDate: Long, date: LocalDate, maxWeek: Int): Int {
        val start = LocalDate.ofEpochDay(startDate)
        val startMonday = start.with(java.time.DayOfWeek.MONDAY)
        val days = java.time.temporal.ChronoUnit.DAYS.between(startMonday, date)
        if (days < 0) return 0
        return (days / 7).toInt() + 1
    }

    fun clampWeek(week: Int, maxWeek: Int): Int = week.coerceIn(1, maxWeek)

    /** 该时间段在 week 周是否有课 */
    fun inWeek(startWeek: Int, endWeek: Int, weekType: Int, week: Int): Boolean {
        if (week < startWeek || week > endWeek) return false
        return when (weekType) {
            1 -> week % 2 == 1
            2 -> week % 2 == 0
            else -> true
        }
    }

    fun weekTypeLabel(weekType: Int): String = when (weekType) {
        1 -> "单周"
        2 -> "双周"
        else -> "每周"
    }

    /** 某周某天的日期，day: 1=周一 */
    fun dateOf(startDate: Long, week: Int, day: Int): LocalDate =
        LocalDate.ofEpochDay(startDate)
            .with(java.time.DayOfWeek.MONDAY)
            .plusWeeks((week - 1).toLong())
            .plusDays((day - 1).toLong())

    fun parseDate(text: String): LocalDate? = runCatching {
        text.trim().let {
            when {
                it.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}")) ->
                    LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-M-d"))
                it.matches(Regex("\\d{4}/\\d{1,2}/\\d{1,2}")) ->
                    LocalDate.parse(it.replace('/', '-'), DateTimeFormatter.ofPattern("yyyy-M-d"))
                else -> null
            }
        }
    }.getOrNull()
}
