package com.letr.sleepdown.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.domain.DateException
import com.letr.sleepdown.domain.DateExceptionType
import com.letr.sleepdown.domain.TimetableEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseEditingPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: TimetableRepository

    @Before
    fun setUp() {
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase("timetable.db")
        repository = TimetableRepository(context)
    }

    @After
    fun tearDown() {
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase("timetable.db")
    }

    @Test
    fun aTimeSlotCanKeepItsTeacherEmptyAcrossSaveReloadAndCommands() = runBlocking {
        val tableId = repository.ensureDefaultTable("Semester")
        val courseId = repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Math", color = 1, teacher = "Teacher A"),
            listOf(
                time(day = 1, slot = "monday").copy(teacher = "Teacher A"),
                time(day = 3, slot = "wednesday").copy(teacher = "Teacher B"),
            ),
        )
        val course = requireNotNull(repository.getCourse(courseId))
        repository.saveCourse(tableId, course, repository.getCourseTimes(courseId).map {
            if (it.day == 3) it.copy(teacher = "") else it
        })
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        repository = TimetableRepository(context)
        var restored = repository.loadDomainTimetable(tableId)
        assertEquals("Teacher A", restored.courses.single().slots.single { it.dayOfWeek == 1 }.teacher)
        assertEquals("", restored.courses.single().slots.single { it.dayOfWeek == 3 }.teacher)
        assertEquals("", repository.getCourseTimes(courseId).single { it.day == 3 }.teacher)
        val target = TimetableEngine.expandOccurrences(restored).first { it.dayOfWeek == 3 }
        repository.saveDateException(tableId, DateException(
            id = "cancel-wednesday",
            logicalSlotId = target.logicalSlotId,
            recurrenceSegmentId = target.recurrenceSegmentId,
            originalEpochDay = target.epochDay,
            type = DateExceptionType.CANCEL,
        ))
        restored = repository.loadDomainTimetable(tableId)
        assertEquals("", restored.courses.single().slots.single { it.dayOfWeek == 3 }.teacher)
        assertEquals(1, restored.dateExceptions.size)
    }

    @Test
    fun cancellationFollowsItsDateWhenSelectedWeeksSplitTheRecurrence() = runBlocking {
        val tableId = repository.ensureDefaultTable("Semester")
        val courseId = repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Math", color = 1),
            listOf(time(day = 1, slot = "monday")),
        )
        val current = repository.loadDomainTimetable(tableId)
        val source = TimetableEngine.expandOccurrences(current).single { it.week == 4 }
        repository.saveDateException(tableId, DateException(
            id = "cancel-week-four",
            logicalSlotId = source.logicalSlotId,
            recurrenceSegmentId = source.recurrenceSegmentId,
            originalEpochDay = source.epochDay,
            type = DateExceptionType.CANCEL,
        ))
        val row = repository.getCourseTimes(courseId).single()
        repository.saveCourse(tableId, requireNotNull(repository.getCourse(courseId)), listOf(
            row.copy(endWeek = 1),
            row.copy(id = 0, startWeek = 3, recurrenceSegmentId = "later-weeks"),
        ))
        val restored = repository.loadDomainTimetable(tableId)
        assertEquals("cancel-week-four", restored.dateExceptions.single().id)
        assertEquals("later-weeks", restored.dateExceptions.single().recurrenceSegmentId)
        assertTrue(TimetableEngine.expandOccurrences(restored).none { it.epochDay == source.epochDay })
    }

    @Test
    fun courseMovePreviewMatchesTheCommittedPlacementInBothDirections() = runBlocking {
        val tableId = repository.ensureDefaultTable("Semester")
        repository.saveCourse(tableId, CourseEntity(tableId = tableId, name = "Math", color = 1),
            listOf(time(day = 1, slot = "monday")))
        for ((day, node) in listOf(3 to 5, 1 to 1)) {
            val before = repository.loadDomainTimetable(tableId)
            val occurrence = TimetableEngine.expandOccurrences(before).first()
            val command = com.letr.sleepdown.ui.aggregateMoveCommand(before, occurrence, day, node)
            val preview = com.letr.sleepdown.domain.TimetableCommands.apply(before, command)
            val committed = repository.applyTimetableCommand(tableId, command)
            assertEquals(preview, committed)
            val reopened = repository.loadDomainTimetable(tableId)
            val positions = TimetableEngine.expandOccurrences(reopened)
            assertTrue(positions.isNotEmpty())
            assertTrue(positions.all { it.dayOfWeek == day && it.startNode == node })
        }
    }

    @Test
    fun moveScopeKeepsOtherWeeksAndIndependentSlotsIntact() = runBlocking {
        val tableId = repository.ensureDefaultTable("Semester")
        repository.saveCourse(tableId, CourseEntity(tableId = tableId, name = "Math", color = 1),
            listOf(time(1, "monday").copy(endWeek = 1),
                time(1, "monday").copy(startWeek = 3, recurrenceSegmentId = "later"),
                time(5, "friday")))
        var current = repository.loadDomainTimetable(tableId)
        val source = TimetableEngine.expandOccurrences(current).first { it.logicalSlotId == "monday" }
        val original = current
        suspend fun move(occurrence: com.letr.sleepdown.domain.CourseOccurrence, day: Int, node: Int,
                         scope: com.letr.sleepdown.ui.CourseMoveScope) {
            val commands = com.letr.sleepdown.ui.aggregateMoveCommands(current, occurrence, day, node, scope)
            val preview = commands.fold(current, com.letr.sleepdown.domain.TimetableCommands::apply)
            assertEquals(current, repository.loadDomainTimetable(tableId))
            assertEquals(preview, repository.applyTimetableCommands(tableId, commands))
            current = repository.loadDomainTimetable(tableId)
        }
        move(source, 2, 3, com.letr.sleepdown.ui.CourseMoveScope.THIS_WEEK)
        assertEquals(original.courses, current.courses)
        var slots = TimetableEngine.expandOccurrences(current).filter { it.logicalSlotId == "monday" }
        assertTrue(slots.single { it.week == 1 }.isRescheduled)
        assertTrue(slots.filter { it.week > 1 }.all { it.dayOfWeek == 1 && it.startNode == 1 })
        move(slots.single { it.week == 1 }, 3, 4, com.letr.sleepdown.ui.CourseMoveScope.THIS_WEEK)
        assertEquals(1, current.dateExceptions.size)
        slots = TimetableEngine.expandOccurrences(current).filter { it.logicalSlotId == "monday" }
        move(slots.single { it.week == 1 }, 4, 5, com.letr.sleepdown.ui.CourseMoveScope.ALL_WEEKS)
        slots = TimetableEngine.expandOccurrences(current).filter { it.logicalSlotId == "monday" }
        assertEquals(setOf(1, 3, 4), slots.map { it.week }.toSet())
        assertTrue(slots.all { it.dayOfWeek == 4 && it.startNode == 5 })
        assertTrue(TimetableEngine.expandOccurrences(current).filter { it.logicalSlotId == "friday" }
            .all { it.dayOfWeek == 5 && it.startNode == 1 })
    }

    @Test
    fun multipleMoveCommandsRollBackTogetherOnFailure() = runBlocking {
        val tableId = repository.ensureDefaultTable("Semester")
        repository.saveCourse(tableId, CourseEntity(tableId = tableId, name = "Math", color = 1), listOf(time(1, "monday")))
        val before = repository.loadDomainTimetable(tableId)
        val result = runCatching {
            repository.applyTimetableCommands(tableId, listOf(
                com.letr.sleepdown.domain.TimetableCommand.MoveLogicalSlot("monday", targetDayOfWeek = 2),
                com.letr.sleepdown.domain.TimetableCommand.MoveLogicalSlot("missing", targetDayOfWeek = 3)))
        }
        assertTrue(result.isFailure)
        assertEquals(before, repository.loadDomainTimetable(tableId))
    }

    @Test
    fun legacyRowsStillReadTheirCourseTeacher() {
        val course = CourseEntity(id = 1, tableId = 1, name = "Math", color = 1, teacher = "Legacy teacher")
        val table = TableEntity(id = 1, name = "Semester", startDate = 0, nodeCount = 1)
        val legacy = time(day = 1, slot = "legacy").copy(courseId = 1, recurrenceSegmentId = "legacy")
        val mapped = DomainMappers.toDomain(TimetableEntityRows(
            table = table,
            courses = listOf(course),
            courseTimes = listOf(legacy),
            nodeTimes = listOf(NodeTimeEntity(tableId = 1, node = 1, start = "08:00", end = "08:50")),
        ))
        assertEquals("Legacy teacher", mapped.courses.single().slots.single().teacher)
    }

    private fun time(day: Int, slot: String) = CourseTimeEntity(
        courseId = 0,
        day = day,
        startNode = 1,
        step = 1,
        startWeek = 1,
        endWeek = 4,
        logicalSlotId = slot,
        recurrenceSegmentId = "$slot-weeks",
    )
}
