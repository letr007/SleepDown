package com.letr.sleepdown.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letr.sleepdown.data.CourseEntity
import com.letr.sleepdown.data.CourseTimeEntity
import com.letr.sleepdown.logic.Weeks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseEditorStateTest {
    private val course = CourseEntity(id = 10, tableId = 1, name = "Math", color = 1)

    @Test
    fun selectedWeeksArePreservedExactly() {
        val selected = setOf(1, 2, 3, 5, 7)
        val draft = defaultTimeDraft(8, 1, 1).copy(selectedWeeks = selected)
        val rows = draft.toEntities(course.id)
        assertEquals(selected, actualWeeks(rows))
        assertEquals(1, rows.map { it.logicalSlotId }.distinct().size)
        assertEquals(rows.size, rows.map { it.recurrenceSegmentId }.distinct().size)
    }

    @Test
    fun splitWeeksReopenAsOneTimeSlotAndKeepUnchangedIdentities() {
        val rows = listOf(row(11, 1, 3, "first"), row(12, 7, 9, "second"))
        val draft = courseTimeDrafts(course, rows).single()
        assertEquals(setOf(1, 2, 3, 7, 8, 9), draft.selectedWeeks)
        assertEquals(rows, draft.toEntities(course.id))
        val renamedRoom = draft.copy(room = "Room B").toEntities(course.id)
        assertEquals(listOf(11L, 12L), renamedRoom.map { it.id })
        assertEquals(listOf("first", "second"), renamedRoom.map { it.recurrenceSegmentId })
        assertTrue(renamedRoom.all { it.room == "Room B" })
    }

    @Test
    fun differentTeachersOrTimesStaySeparatelyEditable() {
        val rows = listOf(row(11, 1, 3, "first"), row(12, 7, 9, "second").copy(teacher = "Substitute"))
        assertEquals(2, courseTimeDrafts(course, rows).size)
        val moved = rows[1].copy(teacher = "", day = 3)
        assertEquals(2, courseTimeDrafts(course, listOf(rows[0], moved)).size)
    }

    @Test
    fun selectedWeeksSurviveSaveAndLoad() {
        for (selection in listOf(setOf(2), setOf(1, 3, 9), setOf(2, 4, 8), setOf(1, 2, 4, 5, 7))) {
            val draft = defaultTimeDraft(10, 3, 2).copy(selectedWeeks = selection)
            val rows = draft.toEntities(course.id)
            val restored = courseTimeDrafts(course, rows).single()
            assertEquals(selection, restored.selectedWeeks)
            assertEquals(selection, actualWeeks(restored.toEntities(course.id)))
        }
    }

    @Test
    fun unchangedOddAndEvenRowsKeepTheirRecurrenceIdentities() {
        val rows = listOf(
            row(11, 1, 9, "odd").copy(weekType = CourseTimeEntity.TYPE_ODD),
            row(12, 2, 8, "even").copy(weekType = CourseTimeEntity.TYPE_EVEN),
        )
        val draft = courseTimeDrafts(course, rows).single()
        assertEquals((1..9).toSet(), draft.selectedWeeks)
        assertEquals(rows, draft.toEntities(course.id))
    }

    @Test
    fun defaultTimeFitsTheAvailablePeriods() {
        assertEquals(2, defaultTimeDraft(20, 1, 1, maxNode = 12).step)
        assertEquals(1, defaultTimeDraft(20, 1, 12, maxNode = 12).step)
        assertEquals(12, defaultTimeDraft(20, 1, 15, maxNode = 12).startNode)
    }

    @Test
    fun gridSelectionResizesAcrossItsAnchorInEitherDirection() {
        val selected = CourseGridSelection(day = 3, anchorNode = 4, cursorNode = 4)
        val downward = selected.resizeBy(3, 12)
        assertEquals(4, downward.startNode)
        assertEquals(7, downward.endNode)
        val upward = downward.resizeBy(-5, 12)
        assertEquals(2, upward.startNode)
        assertEquals(4, upward.endNode)
        assertEquals(3, upward.day)
        assertEquals(4, upward.anchorNode)
        assertEquals(selected, upward.resizeBy(2, 12))
    }

    @Test
    fun gridSelectionStopsAtTheFirstAndLastPeriod() {
        val selected = CourseGridSelection(day = 7, anchorNode = 6, cursorNode = 6)
        assertEquals(1, selected.resizeBy(-20, 12).startNode)
        assertEquals(12, selected.resizeBy(20, 12).endNode)
        assertEquals(11, selected.resizeBy(20, 12).resizeBy(-1, 12).endNode)
    }

    @Test
    fun optionalCreditsMustBeFiniteAndNonnegative() {
        listOf("", "0", "2.5").forEach { assertTrue(validCourseCredit(it)) }
        listOf("-1", "NaN", "Infinity", "1e999", "abc").forEach { assertFalse(validCourseCredit(it)) }
    }

    private fun row(id: Long, start: Int, end: Int, segment: String) = CourseTimeEntity(
        id = id, courseId = course.id, day = 1, startNode = 1, step = 2,
        startWeek = start, endWeek = end, logicalSlotId = "monday", recurrenceSegmentId = segment,
    )

    private fun actualWeeks(rows: List<CourseTimeEntity>): Set<Int> = rows.flatMap { row ->
        (row.startWeek..row.endWeek).filter { Weeks.inWeek(row.startWeek, row.endWeek, row.weekType, it) }
    }.toSet()
}
