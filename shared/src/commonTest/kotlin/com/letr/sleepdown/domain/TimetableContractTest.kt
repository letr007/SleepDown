package com.letr.sleepdown.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TimetableContractTest {
    private val timeTable = TimeTable(
        id = "school-day",
        name = "School day",
        nodes = listOf(
            TimeTableNode(1, 480, 530),
            TimeTableNode(2, 540, 590),
            TimeTableNode(3, 600, 650),
            TimeTableNode(4, 660, 710),
        ),
    )

    private fun timetable(
        courses: List<Course>? = null,
        maxWeek: Int = 4,
        exceptions: List<DateException> = emptyList(),
        preferences: List<ConflictPreference> = emptyList(),
        firstDayEpochDay: Long = 1_005L,
    ): Timetable = Timetable(
        id = "table",
        name = "Test table",
        firstDayEpochDay = firstDayEpochDay,
        maxWeek = maxWeek,
        timeTable = timeTable,
        courses = courses ?: listOf(course("math", endWeek = maxWeek)),
        dateExceptions = exceptions,
        conflictPreferences = preferences,
    )

    private fun course(
        courseId: String,
        slotId: String = "$courseId-slot",
        segmentId: String = "$courseId-segment",
        dayOfWeek: Int = 1,
        startNode: Int = 1,
        nodeCount: Int = 1,
        endWeek: Int = 4,
        customTime: MinuteRange? = null,
    ): Course = Course(
        id = courseId,
        name = courseId,
        slots = listOf(
            LogicalCourseSlot(
                id = slotId,
                courseId = courseId,
                dayOfWeek = dayOfWeek,
                startNode = startNode,
                nodeCount = nodeCount,
                customTime = customTime,
                recurrenceSegments = listOf(
                    RecurrenceSegment(
                        id = segmentId,
                        endWeek = endWeek,
                    ),
                ),
            ),
        ),
    )

    @Test
    fun aggregateValidationRejectsInvalidIdsReferencesRangesAndAnchor() {
        val invalid = timetable(
            courses = listOf(
                Course(
                    id = "",
                    name = "invalid",
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "",
                            courseId = "missing-course",
                            dayOfWeek = 0,
                            startNode = 9,
                            nodeCount = 2,
                            recurrenceSegments = listOf(
                                RecurrenceSegment(
                                    id = "",
                                    startWeek = 0,
                                    endWeek = 5,
                                    dayOfWeek = 8,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            maxWeek = 0,
            firstDayEpochDay = 1_006L,
            exceptions = listOf(
                DateException(
                    id = "",
                    logicalSlotId = "missing-slot",
                    originalEpochDay = 1_005L,
                    type = DateExceptionType.CANCEL,
                ),
            ),
            preferences = listOf(
                ConflictPreference(
                    courseId = "missing-course",
                    logicalSlotId = "missing-slot",
                    occurrenceId = "missing-occurrence",
                    epochDay = 1_005L,
                ),
            ),
        )

        val codes = TimeTableValidator.validate(invalid).issues.map { it.code }.toSet()
        assertTrue(TimeTableValidationCode.INVALID_ID in codes)
        assertTrue(TimeTableValidationCode.SLOT_COURSE_REFERENCE in codes)
        assertTrue(TimeTableValidationCode.INVALID_DAY_OF_WEEK in codes)
        assertTrue(TimeTableValidationCode.INVALID_RECURRENCE_RANGE in codes)
        assertTrue(TimeTableValidationCode.INVALID_COURSE_NODE_RANGE in codes)
        assertTrue(TimeTableValidationCode.INVALID_SEMESTER_ANCHOR in codes)
        assertTrue(TimeTableValidationCode.INVALID_MAX_WEEK in codes)
        assertTrue(TimeTableValidationCode.MISSING_EXCEPTION_SLOT in codes)
        assertTrue(TimeTableValidationCode.INVALID_CONFLICT_REFERENCE in codes)
    }

    @Test
    fun aggregateValidationRejectsDuplicateEntityIdsAndExceptionSources() {
        val duplicateCourses = timetable(
            courses = listOf(course("same"), course("same")),
        )
        assertTrue(
            TimeTableValidator.validate(duplicateCourses).issues.any {
                it.code == TimeTableValidationCode.DUPLICATE_ID
            },
        )

        val base = timetable()
        val sourceDay = TimetableEngine.recurringOccurrences(base).first().sourceEpochDay
        val duplicateExceptions = base.copy(
            dateExceptions = listOf(
                DateException("one", "math-slot", sourceDay, DateExceptionType.CANCEL),
                DateException("two", "math-slot", sourceDay, DateExceptionType.CANCEL),
            ),
        )
        val result = TimeTableValidator.validate(duplicateExceptions)
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.code == TimeTableValidationCode.DUPLICATE_EXCEPTION_SOURCE })
    }

    @Test
    fun timeTableValidationRequiresBoundedContiguousChronologicalNodes() {
        val invalid = TimeTableValidator.validate(
            listOf(
                TimeTableNode(1, 480, 540),
                TimeTableNode(3, 530, 600),
                TimeTableNode(3, 610, 620),
            ),
        )
        assertFalse(invalid.isValid)
        assertTrue(invalid.issues.any { it.code == TimeTableValidationCode.NONCONTIGUOUS_NODES })
        assertTrue(invalid.issues.any { it.code == TimeTableValidationCode.DUPLICATE_NODE_NUMBER })
        assertTrue(invalid.issues.any { it.code == TimeTableValidationCode.OVERLAPPING_NODES })

        val tooMany = TimeTableValidator.validate(
            (1..61).map { TimeTableNode(it, 480, 530) },
        )
        assertTrue(tooMany.issues.any { it.code == TimeTableValidationCode.INVALID_NODE_COUNT })
    }

    @Test
    fun customTimeDoesNotRequireAReusableNodeSpan() {
        val custom = timetable(
            courses = listOf(
                course(
                    courseId = "custom",
                    startNode = 99,
                    nodeCount = 1,
                    endWeek = 1,
                    customTime = MinuteRange(505, 555),
                ),
            ),
            maxWeek = 1,
        )

        assertTrue(TimeTableValidator.validate(custom).isValid)
        assertEquals(1, TimetableEngine.recurringOccurrences(custom).size)
        assertEquals(MinuteRange(505, 555), TimetableEngine.recurringOccurrences(custom).single().minuteRange)
    }

    @Test
    fun generatedOccurrenceAndExceptionIdsAreCollisionSafeAndDeterministic() {
        val occurrenceA = timetable(
            courses = listOf(course("a|b", slotId = "c", segmentId = "segment")),
            maxWeek = 1,
        )
        val occurrenceB = timetable(
            courses = listOf(course("a", slotId = "b|c", segmentId = "segment")),
            maxWeek = 1,
        )
        val occurrenceIdA = TimetableEngine.recurringOccurrences(occurrenceA).single().id
        val occurrenceIdB = TimetableEngine.recurringOccurrences(occurrenceB).single().id
        assertNotEquals(occurrenceIdA, occurrenceIdB)
        assertEquals(occurrenceIdA, TimetableEngine.recurringOccurrences(occurrenceA).single().id)

        val exceptionA = TimetableCommands.toDateException(
            TimetableCommand.DeleteOccurrence("a|b", 1_005L, "c"),
        )!!.id
        val exceptionB = TimetableCommands.toDateException(
            TimetableCommand.DeleteOccurrence("a", 1_005L, "b|c"),
        )!!.id
        assertNotEquals(exceptionA, exceptionB)
    }

    @Test
    fun checkedCommandApiReportsErrorsAndApplyThrowsTheSameFailure() {
        val command = TimetableCommand.DeleteOccurrence("missing-slot", 1_005L)
        val checked = TimetableCommands.applyChecked(timetable(), command)

        assertTrue(checked.isFailure)
        assertTrue(checked.exceptionOrNull() is TimetableCommandException)
        assertFailsWith<TimetableCommandException> {
            TimetableCommands.apply(timetable(), command)
        }
    }

    @Test
    fun commandsRejectAmbiguousTargetsAndInvalidOutput() {
        val ambiguous = timetable(
            courses = listOf(
                Course(
                    id = "math",
                    name = "math",
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "math-slot",
                            courseId = "math",
                            dayOfWeek = 1,
                            startNode = 1,
                            recurrenceSegments = listOf(
                                RecurrenceSegment("first-segment", endWeek = 4),
                                RecurrenceSegment("second-segment", endWeek = 4),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val ambiguousResult = TimetableCommands.applyChecked(
            ambiguous,
            TimetableCommand.DeleteOccurrence("math-slot", 1_005L),
        )
        assertTrue(ambiguousResult.isFailure)
        assertTrue(ambiguousResult.exceptionOrNull() is TimetableCommandException)

        val missingSegment = TimetableCommands.applyChecked(
            timetable(),
            TimetableCommand.MoveRecurrenceSegment("math-slot", "missing-segment"),
        )
        assertTrue(missingSegment.isFailure)
        assertTrue(missingSegment.exceptionOrNull() is TimetableCommandException)

        val outsideCoverage = TimetableCommands.applyChecked(
            timetable(maxWeek = 1),
            TimetableCommand.RescheduleOccurrence(
                logicalSlotId = "math-slot",
                originalEpochDay = 1_005L,
                targetEpochDay = 1_012L,
            ),
        )
        assertTrue(outsideCoverage.isFailure)

        val invalidOutput = TimetableCommands.applyChecked(
            timetable(maxWeek = 1),
            TimetableCommand.RescheduleOccurrence(
                logicalSlotId = "math-slot",
                originalEpochDay = 1_005L,
                targetEpochDay = 1_005L,
                targetCustomTime = MinuteRange(600, 599),
            ),
        )
        assertTrue(invalidOutput.isFailure)
    }

    @Test
    fun movingARecurrenceSegmentShiftsInheritedCustomTime() {
        val source = timetable(
            courses = listOf(
                course(
                    courseId = "custom",
                    endWeek = 1,
                    customTime = MinuteRange(500, 550),
                ),
            ),
            maxWeek = 1,
        )
        val moved = TimetableCommands.apply(
            source,
            TimetableCommand.MoveRecurrenceSegment(
                logicalSlotId = "custom-slot",
                recurrenceSegmentId = "custom-segment",
                minuteDelta = 5,
            ),
        )

        assertEquals(MinuteRange(505, 555), moved.courses.single().slots.single().recurrenceSegments.single().customTime)
        assertEquals(MinuteRange(505, 555), TimetableEngine.recurringOccurrences(moved).single().minuteRange)
    }

    @Test
    fun deletingCourseSlotOrSegmentRemovesMatchingExceptionsAndPreferences() {
        val base = timetable(maxWeek = 1)
        val source = TimetableEngine.recurringOccurrences(base).single()
        val rescheduled = TimetableCommands.apply(
            base,
            TimetableCommand.RescheduleOccurrence(
                logicalSlotId = source.logicalSlotId,
                originalEpochDay = source.sourceEpochDay,
                targetEpochDay = source.sourceEpochDay + 1,
            ),
        )
        val effective = TimetableEngine.expandOccurrences(rescheduled).single()
        val withOccurrencePreference = rescheduled.copy(
            conflictPreferences = listOf(
                ConflictPreference(
                    courseId = source.courseId,
                    logicalSlotId = source.logicalSlotId,
                    occurrenceId = effective.id,
                ),
            ),
        )
        assertTrue(TimeTableValidator.validate(withOccurrencePreference).isValid)

        val segmentDeleted = TimetableCommands.apply(
            withOccurrencePreference,
            TimetableCommand.DeleteRecurrenceSegment(source.logicalSlotId, source.recurrenceSegmentId),
        )
        assertTrue(segmentDeleted.dateExceptions.isEmpty())
        assertTrue(segmentDeleted.conflictPreferences.isEmpty())

        val withBroadPreferences = base.copy(
            dateExceptions = listOf(
                DateException("cancel", source.logicalSlotId, source.sourceEpochDay, DateExceptionType.CANCEL),
            ),
            conflictPreferences = listOf(
                ConflictPreference(courseId = source.courseId),
                ConflictPreference(courseId = source.courseId, logicalSlotId = source.logicalSlotId),
            ),
        )
        assertTrue(TimeTableValidator.validate(withBroadPreferences).isValid)
        val slotDeleted = TimetableCommands.apply(
            withBroadPreferences,
            TimetableCommand.DeleteLogicalSlot(source.logicalSlotId),
        )
        assertTrue(slotDeleted.dateExceptions.isEmpty())
        assertEquals(1, slotDeleted.conflictPreferences.size)
        assertTrue(slotDeleted.conflictPreferences.single().logicalSlotId == null)

        val courseDeleted = TimetableCommands.apply(
            withBroadPreferences,
            TimetableCommand.DeleteCourse(source.courseId),
        )
        assertTrue(courseDeleted.dateExceptions.isEmpty())
        assertTrue(courseDeleted.conflictPreferences.isEmpty())
    }

    @Test
    fun occurrenceScopedPreferencesFollowRescheduledOccurrenceIdAndEpochDay() {
        val source = timetable(maxWeek = 3)
        val original = TimetableEngine.recurringOccurrences(source).first()
        val withPreference = source.copy(
            conflictPreferences = listOf(
                ConflictPreference(
                    courseId = original.courseId,
                    logicalSlotId = original.logicalSlotId,
                    occurrenceId = original.id,
                    epochDay = original.epochDay,
                    priority = 7,
                ),
            ),
        )

        val moved = TimetableCommands.apply(
            withPreference,
            TimetableCommand.RescheduleOccurrence(
                logicalSlotId = original.logicalSlotId,
                originalEpochDay = original.sourceEpochDay,
                targetEpochDay = original.sourceEpochDay + 8L,
            ),
        )
        val movedOccurrence = TimetableEngine.expandOccurrences(moved).single { it.isRescheduled }
        val updatedPreference = moved.conflictPreferences.single()

        assertNotEquals(original.id, movedOccurrence.id)
        assertEquals(movedOccurrence.id, updatedPreference.occurrenceId)
        assertEquals(movedOccurrence.epochDay, updatedPreference.epochDay)
        assertTrue(TimeTableValidator.validate(moved).isValid)
    }

    @Test
    fun fromWeekParitySplitsOmitEmptyHalvesAtOddAndEvenBoundaries() {
        fun parityTimetable(pattern: WeekPattern): Timetable = timetable(
            maxWeek = 4,
            courses = listOf(
                Course(
                    id = "parity",
                    name = "parity",
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "parity-slot",
                            courseId = "parity",
                            dayOfWeek = 1,
                            startNode = 1,
                            recurrenceSegments = listOf(
                                RecurrenceSegment(
                                    id = "parity-segment",
                                    startWeek = 1,
                                    endWeek = 4,
                                    weekPattern = pattern,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val movedOdd = TimetableCommands.apply(
            parityTimetable(WeekPattern.ODD),
            TimetableCommand.MoveLogicalSlotFromWeek("parity-slot", fromWeek = 2, dayDelta = 1),
        )
        assertEquals(listOf(1, 3), TimetableEngine.recurringOccurrences(movedOdd).map { it.week })
        assertEquals(listOf(1, 2), TimetableEngine.recurringOccurrences(movedOdd).map { it.dayOfWeek })
        assertTrue(TimeTableValidator.validate(movedOdd).isValid)

        val movedEven = TimetableCommands.apply(
            parityTimetable(WeekPattern.EVEN),
            TimetableCommand.MoveRecurrenceSegmentFromWeek(
                logicalSlotId = "parity-slot",
                recurrenceSegmentId = "parity-segment",
                fromWeek = 2,
                dayDelta = 1,
            ),
        )
        assertEquals(listOf(2, 4), TimetableEngine.recurringOccurrences(movedEven).map { it.week })
        assertEquals(listOf(2, 2), TimetableEngine.recurringOccurrences(movedEven).map { it.dayOfWeek })
        assertTrue(TimeTableValidator.validate(movedEven).isValid)

        val deletedEven = TimetableCommands.apply(
            parityTimetable(WeekPattern.EVEN),
            TimetableCommand.DeleteLogicalSlotFromWeek("parity-slot", fromWeek = 2),
        )
        assertTrue(TimetableEngine.recurringOccurrences(deletedEven).isEmpty())
        assertTrue(TimeTableValidator.validate(deletedEven).isValid)

        val deletedOddAtBoundary = TimetableCommands.apply(
            parityTimetable(WeekPattern.ODD),
            TimetableCommand.DeleteRecurrenceSegmentFromWeek(
                logicalSlotId = "parity-slot",
                recurrenceSegmentId = "parity-segment",
                fromWeek = 4,
            ),
        )
        assertEquals(listOf(1, 3), TimetableEngine.recurringOccurrences(deletedOddAtBoundary).map { it.week })
        assertTrue(TimeTableValidator.validate(deletedOddAtBoundary).isValid)
    }

    @Test
    fun fromWeekCommandsPreservePastWeeksAndUpdateFutureReferences() {
        val base = timetable(maxWeek = 4)
        val slotId = "math-slot"
        val firstDay = 1_005L
        val futureException = DateException(
            id = "future-cancel",
            logicalSlotId = slotId,
            originalEpochDay = firstDay + 14L,
            type = DateExceptionType.CANCEL,
        )
        val withException = base.copy(dateExceptions = listOf(futureException))
        assertTrue(TimeTableValidator.validate(withException).isValid)

        val deleted = TimetableCommands.apply(
            withException,
            TimetableCommand.DeleteLogicalSlotFromWeek(slotId, fromWeek = 3),
        )
        assertEquals(listOf(firstDay, firstDay + 7L), TimetableEngine.recurringOccurrences(deleted).map { it.sourceEpochDay })
        assertTrue(deleted.dateExceptions.isEmpty())
        assertEquals(2, deleted.courses.single().slots.single().recurrenceSegments.single().endWeek)

        val moved = TimetableCommands.apply(
            withException,
            TimetableCommand.MoveLogicalSlotFromWeek(slotId, fromWeek = 3, dayDelta = 1),
        )
        val movedOccurrences = TimetableEngine.recurringOccurrences(moved)
        assertEquals(listOf(1, 1, 2, 2), movedOccurrences.map { it.dayOfWeek })
        assertEquals(listOf(firstDay, firstDay + 7L, firstDay + 15L, firstDay + 22L), movedOccurrences.map { it.sourceEpochDay })
        assertEquals(firstDay + 15L, moved.dateExceptions.single().originalEpochDay)
        assertEquals(listOf(firstDay, firstDay + 7L, firstDay + 22L), TimetableEngine.expandOccurrences(moved).map { it.sourceEpochDay })
    }

    @Test
    fun timetableDisplayDefaultsRemainCompatible() {
        val table = timetable()
        assertEquals(0, table.sortOrder)
        assertTrue(table.showSaturday)
        assertTrue(table.showSunday)
        assertFalse(table.sundayFirst)
        assertEquals(table.showSaturday, table.showSat)
        assertEquals(table.showSunday, table.showSun)
    }
}
