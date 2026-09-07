package com.letr.sleepdown.ui

import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimetableGridLayoutTest {
    private val times = ScheduleTimeTable(nodes = listOf(
        TimeTableNode(1, 480, 530),
        TimeTableNode(2, 540, 590),
        TimeTableNode(3, 600, 650),
    ))

    private fun occurrence(start: Int, count: Int = 1, custom: MinuteRange? = null) =
        TimetableEngine.recurringOccurrences(Timetable(
            id = "table", name = "Table", firstDayEpochDay = 0, maxWeek = 2, timeTable = times,
            courses = listOf(Course(id = "course", name = "Course", slots = listOf(
                LogicalCourseSlot(id = "slot", courseId = "course", dayOfWeek = 1,
                    startNode = start, nodeCount = count, customTime = custom),
            ))),
        )).first()

    @Test
    fun selectedPeriodCountControlsRowsIndependentlyOfTimeTableCapacity() {
        val lesson = occurrence(2)
        val twoRows = requireNotNull(visibleCoursePlacement(lesson, 2, times))
        assertEquals(0.5, twoRows.topFraction, 0.0001)
        assertEquals(0.5, twoRows.heightFraction, 0.0001)
        val fourRows = requireNotNull(visibleCoursePlacement(lesson, 4, times))
        assertEquals(0.25, fourRows.topFraction, 0.0001)
        assertEquals(0.25, fourRows.heightFraction, 0.0001)
    }

    @Test
    fun laterLessonsAreHiddenInsteadOfStackingAtTheLastRow() {
        assertNull(visibleCoursePlacement(occurrence(3), 2, times))
    }

    @Test
    fun crossingLessonsAreClippedWithoutMutatingTheirDuration() {
        val lesson = occurrence(2, 2)
        val placement = requireNotNull(visibleCoursePlacement(lesson, 2, times))
        assertEquals(0.5, placement.heightFraction, 0.0001)
        assertEquals(2, lesson.nodeCount)
    }

    @Test
    fun customTimeUsesActualPeriodPositionIncludingOtherWeeks() {
        val lesson = occurrence(1, custom = MinuteRange(565, 590)).copy(week = 2)
        val placement = requireNotNull(visibleCoursePlacement(lesson, 2, times))
        assertEquals(0.75, placement.topFraction, 0.0001)
        assertEquals(0.25, placement.heightFraction, 0.0001)
    }

    @Test
    fun customTimePastTheLastVisiblePeriodIsHidden() {
        assertNull(visibleCoursePlacement(occurrence(1, custom = MinuteRange(600, 650)), 2, times))
    }
}
