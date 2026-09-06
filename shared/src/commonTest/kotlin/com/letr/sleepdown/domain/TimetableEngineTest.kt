package com.letr.sleepdown.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TimetableEngineTest {
    private val timeTable = TimeTable(
        id = "school-day",
        name = "School day",
        nodes = listOf(
            TimeTableNode(node = 1, startMinuteOfDay = 480, endMinuteOfDay = 530),
            TimeTableNode(node = 2, startMinuteOfDay = 540, endMinuteOfDay = 590),
            TimeTableNode(node = 3, startMinuteOfDay = 600, endMinuteOfDay = 650),
            TimeTableNode(node = 4, startMinuteOfDay = 660, endMinuteOfDay = 710),
        ),
    )

    private fun timetable(
        courses: List<Course>,
        exceptions: List<DateException> = emptyList(),
        preferences: List<ConflictPreference> = emptyList(),
        maxWeek: Int = 4,
    ): Timetable = Timetable(
        id = "table",
        name = "Test table",
        firstDayEpochDay = 1_005L,
        maxWeek = maxWeek,
        timeTable = timeTable,
        courses = courses,
        dateExceptions = exceptions,
        conflictPreferences = preferences,
    )

    private fun course(
        id: String,
        dayOfWeek: Int = 1,
        startNode: Int = 1,
        nodeCount: Int = 1,
        segment: RecurrenceSegment = RecurrenceSegment("$id-segment", endWeek = 4),
        customTime: MinuteRange? = null,
        teacher: String = "Teacher",
        room: String = "Room",
        note: String = "Note",
    ): Course = Course(
        id = id,
        name = id.uppercase(),
        note = note,
        slots = listOf(
            LogicalCourseSlot(
                id = "$id-slot",
                courseId = id,
                dayOfWeek = dayOfWeek,
                startNode = startNode,
                nodeCount = nodeCount,
                teacher = teacher,
                room = room,
                customTime = customTime,
                recurrenceSegments = listOf(segment),
            ),
        ),
    )

    @Test
    fun recurrenceExpandsAllOddAndEvenWeeksFromTheFirstEpochDay() {
        val odd = timetable(
            listOf(
                course(
                    id = "odd",
                    segment = RecurrenceSegment("odd-segment", 1, 4, WeekPattern.ODD),
                ),
            ),
        )
        val even = timetable(
            listOf(
                course(
                    id = "even",
                    segment = RecurrenceSegment("even-segment", 1, 4, WeekPattern.EVEN),
                ),
            )
        )

        assertEquals(
            listOf(1_005L, 1_019L),
            TimetableEngine.recurringOccurrences(odd).map { it.epochDay },
        )
        assertEquals(
            listOf(1_012L, 1_026L),
            TimetableEngine.recurringOccurrences(even).map { it.epochDay },
        )
    }

    @Test
    fun dateExceptionsCancelOrMoveOneOccurrenceWithoutChangingRecurrence() {
        val source = timetable(listOf(course("math")))
        val cancelled = TimetableCommands.apply(
            source,
            TimetableCommand.DeleteOccurrence(
                logicalSlotId = "math-slot",
                originalEpochDay = 1_005L,
            ),
        )
        val moved = TimetableCommands.apply(
            source,
            TimetableCommand.RescheduleOccurrence(
                logicalSlotId = "math-slot",
                originalEpochDay = 1_012L,
                targetEpochDay = 1_014L,
                targetStartNode = 2,
            ),
        )

        assertEquals(4, TimetableEngine.recurringOccurrences(cancelled).size)
        assertEquals(
            listOf(1_012L, 1_019L, 1_026L),
            TimetableEngine.expandOccurrences(cancelled).map { it.epochDay },
        )
        assertEquals(
            listOf(1_005L, 1_014L, 1_019L, 1_026L),
            TimetableEngine.expandOccurrences(moved).map { it.epochDay },
        )
        val movedOccurrence = TimetableEngine.expandOccurrences(moved).first { it.epochDay == 1_014L }
        assertTrue(movedOccurrence.isRescheduled)
        assertEquals(1_012L, movedOccurrence.sourceEpochDay)
        assertEquals(2, movedOccurrence.startNode)
    }

    @Test
    fun customTimeUsesMinuteBoundariesForFractionalPlacement() {
        val table = timetable(
            listOf(
                course(
                    id = "custom",
                    segment = RecurrenceSegment("custom-segment", endWeek = 1),
                    customTime = MinuteRange(505, 555),
                ),
            ),
            maxWeek = 1,
        )
        val occurrence = TimetableEngine.recurringOccurrences(table).single()
        val placement = TimetableEngine.gridPlacement(occurrence, timeTable)

        assertEquals(25.0 / 230.0, placement.topFraction, 0.000001)
        assertEquals(50.0 / 230.0, placement.heightFraction, 0.000001)
    }

    @Test
    fun overlappingOccurrencesGetDeterministicColumnsAndPreferredCourse() {
        val table = timetable(
            courses = listOf(
                course("b", customTime = MinuteRange(500, 560), segment = RecurrenceSegment("b-segment", endWeek = 1)),
                course("a", customTime = MinuteRange(505, 550), segment = RecurrenceSegment("a-segment", endWeek = 1)),
            ),
            preferences = listOf(ConflictPreference(courseId = "b")),
            maxWeek = 1,
        )
        val occurrences = TimetableEngine.recurringOccurrences(table)
        val groups = TimetableEngine.conflictGroups(table, EpochDayRange(1_005L, 1_005L))
        val group = groups.single()
        val placements = TimetableEngine.gridPlacements(occurrences, timeTable)

        assertEquals(2, group.occurrences.size)
        assertEquals("b", group.preferredOccurrence.courseId)
        assertEquals(1, placements.getValue(occurrences.first { it.courseId == "a" }.id).column)
        assertEquals(0, placements.getValue(occurrences.first { it.courseId == "b" }.id).column)
        assertEquals(0.5, placements.values.first().widthFraction, 0.000001)
        assertEquals(2, placements.values.first().columnCount)

        val noPreferenceGroup = TimetableEngine.conflictGroups(occurrences).single()
        assertEquals("a", noPreferenceGroup.preferredOccurrence.courseId)
    }

    @Test
    fun commandsKeepLogicalAndSegmentScopesSeparate() {
        val source = timetable(
            listOf(
                course(
                    id = "physics",
                    segment = RecurrenceSegment(
                        id = "physics-segment",
                        startWeek = 1,
                        endWeek = 2,
                        weekPattern = WeekPattern.ALL,
                        dayOfWeek = 2,
                        startNode = 2,
                        customTime = MinuteRange(550, 600),
                    ),
                ),
            ),
        )
        val moved = TimetableCommands.apply(
            source,
            TimetableCommand.MoveLogicalSlot(
                logicalSlotId = "physics-slot",
                dayDelta = 1,
                startNodeDelta = 1,
                minuteDelta = 5,
            ),
        )
        val movedSlot = moved.courses.single().slots.single()
        val movedSegment = movedSlot.recurrenceSegments.single()

        assertEquals(2, movedSlot.dayOfWeek)
        assertEquals(2, movedSlot.startNode)
        assertEquals(3, movedSegment.dayOfWeek)
        assertEquals(3, movedSegment.startNode)
        assertEquals(MinuteRange(555, 605), movedSegment.customTime)
        assertEquals(1, source.courses.single().slots.single().dayOfWeek)
        assertEquals(2, source.courses.single().slots.single().recurrenceSegments.single().dayOfWeek)

        val segmentDeleted = TimetableCommands.apply(
            source,
            TimetableCommand.DeleteRecurrenceSegment("physics-slot", "physics-segment"),
        )
        assertTrue(segmentDeleted.courses.single().slots.single().recurrenceSegments.isEmpty())
        assertEquals(1, source.courses.single().slots.single().recurrenceSegments.size)
    }

    @Test
    fun reminderPlannerEmitsStartAndEndWithContentAndLeadMinutes() {
        val occurrence = TimetableEngine.recurringOccurrences(
            timetable(
                listOf(
                    course(
                        id = "english",
                        segment = RecurrenceSegment("english-segment", endWeek = 1),
                        customTime = MinuteRange(505, 555),
                    ),
                ),
                maxWeek = 1,
            ),
        ).single()
        val plans = ReminderPlanner.plan(
            listOf(occurrence),
            ReminderSettings(
                startEnabled = true,
                endEnabled = true,
                startLeadMinutes = 10,
                endLeadMinutes = 5,
                content = ReminderContentSettings(includeNote = true),
                vibrate = true,
                silent = false,
            ),
        )

        assertEquals(listOf(ReminderKind.LESSON_START, ReminderKind.LESSON_END), plans.map { it.kind })
        assertEquals(495, plans.first().triggerMinuteOfDay)
        assertEquals(550, plans.last().triggerMinuteOfDay)
        assertEquals(10, plans.first().leadMinutes)
        assertTrue(plans.first().body.contains("ENGLISH"))
        assertTrue(plans.first().body.contains("Teacher"))
        assertTrue(plans.first().body.contains("Room"))
        assertTrue(plans.first().body.contains("Note"))
        assertTrue(plans.first().vibrate)
        assertFalse(plans.first().silent)
    }

    @Test
    fun widgetSnapshotsExposeNextTodayAndWeekDataWithoutUiTypes() {
        val table = timetable(
            listOf(
                course("math", dayOfWeek = 1),
                course("later", dayOfWeek = 2),
            ),
        )
        val snapshots = WidgetSnapshotBuilder.buildAll(table, nowEpochDay = 1_005L, nowMinuteOfDay = 500)

        assertEquals(WidgetSnapshotKind.NEXT, snapshots.next.kind)
        assertEquals("math", snapshots.next.items.single().courseId)
        assertTrue(snapshots.next.items.single().isCurrent)
        assertEquals(WidgetSnapshotKind.TODAY, snapshots.today.kind)
        assertEquals(listOf("math"), snapshots.today.items.map { it.courseId })
        assertEquals(WidgetSnapshotKind.WEEK, snapshots.week.kind)
        assertEquals(listOf("math", "later"), snapshots.week.items.map { it.courseId })
        assertEquals(table.id, snapshots.week.timetableId)
    }

    @Test
    fun timeTableValidationRejectsInvalidAndOverlappingNodeTimes() {
        val result = TimeTableValidator.validate(
            listOf(
                TimeTableNode(node = 1, startMinuteOfDay = 480, endMinuteOfDay = 540),
                TimeTableNode(node = 2, startMinuteOfDay = 530, endMinuteOfDay = 600),
                TimeTableNode(node = 3, startMinuteOfDay = 700, endMinuteOfDay = 700),
                TimeTableNode(node = 4, startMinuteOfDay = -1, endMinuteOfDay = 1_500),
            ),
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.code == TimeTableValidationCode.OVERLAPPING_NODES })
        assertTrue(result.issues.any { it.code == TimeTableValidationCode.END_NOT_AFTER_START })
        assertTrue(result.issues.any { it.code == TimeTableValidationCode.INVALID_START_MINUTE })
        assertTrue(result.issues.any { it.code == TimeTableValidationCode.INVALID_END_MINUTE })
        assertTrue(TimeTableValidator.validate(timeTable).isValid)
        assertNotNull(TimeTableNode.fromStrings(1, "08:00", "08:50").minuteRange)
    }
}
