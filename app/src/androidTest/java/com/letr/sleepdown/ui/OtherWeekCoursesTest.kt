package com.letr.sleepdown.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.DateException
import com.letr.sleepdown.domain.DateExceptionType
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OtherWeekCoursesTest {
    private val timetable = Timetable(
        id = "table",
        name = "Term",
        firstDayEpochDay = 0L,
        maxWeek = 4,
        timeTable = ScheduleTimeTable(
            nodes = listOf(
                TimeTableNode(1, 480, 530),
                TimeTableNode(2, 540, 590),
                TimeTableNode(3, 600, 650),
            ),
        ),
        courses = listOf(
            course("blocked", day = 1, node = 1),
            course("past", day = 4, node = 2, startWeek = 1, endWeek = 1),
            course("future", day = 5, node = 3, startWeek = 4, endWeek = 4),
            course("duplicate-a", day = 6, node = 1, startWeek = 1, endWeek = 1),
            course("duplicate-b", day = 6, node = 1, startWeek = 4, endWeek = 4),
        ),
        dateExceptions = listOf(
            DateException(
                id = "cancel-current",
                logicalSlotId = "blocked-slot",
                originalEpochDay = 7L,
                type = DateExceptionType.CANCEL,
            ),
        ),
    )

    @Test
    fun includesPastAndFutureWeeksInTheSelectedDayAndPeriodLayout() {
        val result = aggregateOtherWeekOccurrences(timetable, week = 2)
        val ids = result.map { it.courseId }.toSet()

        assertTrue(ids.contains("past"))
        assertTrue(ids.contains("future"))
        assertFalse(ids.contains("blocked"))
    }

    @Test
    fun deduplicatesOtherWeeksByCoursePeriod() {
        val result = aggregateOtherWeekOccurrences(timetable, week = 2)
        val duplicates = result.filter { it.dayOfWeek == 6 && it.startNode == 1 }

        assertEquals(1, duplicates.size)
        assertEquals("duplicate-a", duplicates.single().courseId)
    }

    @Test
    fun partiallyOverlappingCurrentLessonsTakePriority() {
        val current = course("current", day = 2, node = 1).let { it.copy(slots = it.slots.map { slot -> slot.copy(nodeCount = 2) }) }
        val other = course("other", day = 2, node = 2, startWeek = 4, endWeek = 4)
        assertTrue(aggregateOtherWeekOccurrences(timetable.copy(courses = listOf(current, other)), 2).isEmpty())
    }

    @Test
    fun oddWeekLessonsAppearOnEvenWeeksOnlyAsOtherWeekLessons() {
        val odd = course("odd", day = 2, node = 1).let { course ->
            course.copy(slots = course.slots.map { slot ->
                slot.copy(recurrenceSegments = slot.recurrenceSegments.map { it.copy(weekPattern = com.letr.sleepdown.domain.WeekPattern.ODD) })
            })
        }
        val source = timetable.copy(courses = listOf(odd), dateExceptions = emptyList())
        assertEquals(listOf("odd"), aggregateOtherWeekOccurrences(source, 2).map { it.courseId })
        assertTrue(aggregateOtherWeekOccurrences(source, 1).isEmpty())
        assertTrue(aggregateOtherWeekOccurrences(source, 0).isEmpty())
    }

    @Test
    fun aCancelledCurrentPeriodStillBlocksOtherWeekFallback() {
        val result = aggregateOtherWeekOccurrences(timetable, week = 2)

        assertTrue(result.none { it.logicalSlotId == "blocked-slot" })
    }

    private fun course(
        id: String,
        day: Int,
        node: Int,
        startWeek: Int = 1,
        endWeek: Int = 4,
    ) = Course(
        id = id,
        name = id,
        slots = listOf(
            LogicalCourseSlot(
                id = "$id-slot",
                courseId = id,
                dayOfWeek = day,
                startNode = node,
                recurrenceSegments = listOf(
                    RecurrenceSegment(
                        id = "$id-segment",
                        startWeek = startWeek,
                        endWeek = endWeek,
                    ),
                ),
            ),
        ),
    )
}
