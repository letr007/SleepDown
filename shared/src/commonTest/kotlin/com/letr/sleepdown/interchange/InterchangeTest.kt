package com.letr.sleepdown.interchange

import com.letr.sleepdown.domain.ConflictPreference
import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.DateException
import com.letr.sleepdown.domain.DateExceptionType
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.ReminderContentSettings
import com.letr.sleepdown.domain.ReminderSettings
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.TimeTableValidationCode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.WeekPattern
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterchangeTest {
    private val timeTable = ScheduleTimeTable(
        id = "school",
        name = "School hours",
        nodes = listOf(
            TimeTableNode(1, 480, 530),
            TimeTableNode(2, 540, 590),
            TimeTableNode(3, 600, 650),
        ),
    )

    private val icsExportOptions = IcsExportOptions(
        dtStampEpochDay = 20_000L,
        dtStampMinuteOfDay = 123,
    )

    private fun richTimetable(
        id: String = "rich",
        courseId: String = "course-1",
    ): Timetable {
        val slot = LogicalCourseSlot(
            id = "slot-1",
            courseId = courseId,
            dayOfWeek = 1,
            startNode = 1,
            nodeCount = 2,
            teacher = "Teacher",
            room = "Room, A;1",
            customTime = MinuteRange(485, 585),
            recurrenceSegments = listOf(
                RecurrenceSegment(
                    id = "segment-1",
                    startWeek = 1,
                    endWeek = 2,
                    weekPattern = WeekPattern.ALL,
                    teacher = "Teacher",
                    room = "Room, A;1",
                    customTime = MinuteRange(485, 585),
                ),
                RecurrenceSegment(
                    id = "segment-2",
                    startWeek = 3,
                    endWeek = 4,
                    weekPattern = WeekPattern.EVEN,
                    dayOfWeek = 2,
                    startNode = 2,
                    nodeCount = 1,
                    teacher = "Substitute",
                    room = "Room B",
                    customTime = MinuteRange(545, 575),
                ),
            ),
        )
        return Timetable(
            id = id,
            name = "Rich timetable",
            firstDayEpochDay = 19_996L,
            maxWeek = 4,
            timeTable = timeTable,
            courses = listOf(
                Course(
                    id = courseId,
                    name = "Algorithms",
                    color = 0xFF112233.toInt(),
                    note = "Line 1\nLine 2, with; punctuation",
                    credit = 3.5f,
                    slots = listOf(slot),
                ),
            ),
            dateExceptions = listOf(
                DateException(
                    id = "exception-1",
                    logicalSlotId = "slot-1",
                    originalEpochDay = 19_996L,
                    type = DateExceptionType.RESCHEDULE,
                    recurrenceSegmentId = "segment-1",
                    targetEpochDay = 19_998L,
                    targetDayOfWeek = 3,
                    targetStartNode = 3,
                    targetNodeCount = 1,
                    targetCustomTime = MinuteRange(605, 640),
                    targetTeacher = "Replacement",
                    targetRoom = "Room C",
                ),
            ),
            conflictPreferences = listOf(
                ConflictPreference(
                    courseId = courseId,
                    priority = 7,
                    logicalSlotId = "slot-1",
                    epochDay = 20_000L,
                ),
            ),
            reminderSettings = ReminderSettings(
                startEnabled = true,
                endEnabled = true,
                startLeadMinutes = 15,
                endLeadMinutes = 5,
                content = ReminderContentSettings(
                    includeCourseName = true,
                    includeTeacher = false,
                    includeRoom = true,
                    includeNote = true,
                ),
                vibrate = false,
                silent = true,
            ),
        )
    }

    @Test
    fun versionedBackupRoundTripsAllSharedDomainFields() {
        val source = richTimetable().copy(
            sortOrder = 9,
            showSaturday = false,
            showSunday = false,
            sundayFirst = true,
        )
        val encoded = BackupFormat.encode(source)
        val restored = BackupFormat.decode(encoded)
        val document = BackupFormat.decodeDocument(encoded)

        assertEquals(source, restored)
        assertEquals(SLEEP_DOWN_BACKUP_FORMAT_VERSION, document.formatVersion)
        assertContains(encoded, "\"formatVersion\":1")
        assertContains(encoded, "\"dateExceptions\"")
        assertContains(encoded, "\"reminderSettings\"")
    }

    @Test
    fun backupImportModesHaveExplicitDeterministicCollisionHandling() {
        val current = richTimetable(id = "current", courseId = "course-1")
        val imported = richTimetable(id = "incoming", courseId = "course-1")
        val first = BackupImportPolicy.apply(current, imported, BackupImportMode.MERGE)
        val second = BackupImportPolicy.apply(current, imported, BackupImportMode.MERGE)

        assertEquals(first, second)
        assertEquals(2, first.timetable.courses.size)
        assertEquals("course-1", first.timetable.courses[0].id)
        assertEquals("course-1~import-2", first.timetable.courses[1].id)
        assertEquals("course-1~import-2", first.idRemapping["course:course-1"])
        assertEquals(imported, BackupImportPolicy.apply(current, imported, BackupImportMode.REPLACE).timetable)
    }

    @Test
    fun mergeRebasesAbsoluteDatesTimesExpandsUnionAndRemapsReferencesExactly() {
        val current = simpleMergeTimetable(
            courseId = "a",
            slotId = "current-slot",
            segmentId = "current-segment",
            firstDayEpochDay = 19_996L,
            maxWeek = 1,
            nodeRange = MinuteRange(480, 530),
        )
        val importedBase = simpleMergeTimetable(
            courseId = "az",
            slotId = "a",
            segmentId = "az-segment",
            firstDayEpochDay = 20_010L,
            maxWeek = 2,
            nodeRange = MinuteRange(700, 760),
        )
        val importedOccurrenceId = com.letr.sleepdown.domain.TimetableEngine.expandOccurrences(importedBase).single().id
        val imported = importedBase.copy(
            conflictPreferences = listOf(
                ConflictPreference(
                    courseId = "az",
                    logicalSlotId = "a",
                    occurrenceId = importedOccurrenceId,
                    epochDay = 20_010L,
                ),
            ),
        )

        val result = BackupImportPolicy.merge(current, imported)
        val importedOccurrence = com.letr.sleepdown.domain.TimetableEngine.expandOccurrences(result.timetable)
            .single { it.courseId == "az" }
        val importedPreference = result.timetable.conflictPreferences.single { it.courseId == "az" }

        assertEquals(19_996L, result.timetable.firstDayEpochDay)
        assertEquals(4, result.timetable.maxWeek)
        assertEquals(20_010L, importedOccurrence.epochDay)
        assertEquals(MinuteRange(700, 760), importedOccurrence.minuteRange)
        assertEquals("az", importedOccurrence.courseId)
        assertEquals("a~import-2", importedOccurrence.logicalSlotId)
        assertEquals(importedOccurrence.id, importedPreference.occurrenceId)
        assertEquals("a~import-2", importedPreference.logicalSlotId)
        assertTrue(result.remappedIds.contains(BackupIdRemap("slot", "a", "a~import-2")))
        assertTrue(com.letr.sleepdown.domain.TimeTableValidator.validate(result.timetable).isValid)
    }

    @Test
    fun mergeRebasingSwapsOddAndEvenPatternsWithoutChangingAbsoluteDates() {
        val current = simpleMergeTimetable(
            courseId = "current",
            slotId = "current-slot",
            segmentId = "current-segment",
            firstDayEpochDay = 19_996L,
            maxWeek = 1,
            nodeRange = MinuteRange(480, 530),
        )

        listOf(WeekPattern.ODD, WeekPattern.EVEN).forEach { pattern ->
            val courseId = "incoming-${pattern.name}"
            val source = simpleMergeTimetable(
                courseId = courseId,
                slotId = "$courseId-slot",
                segmentId = "$courseId-segment",
                firstDayEpochDay = 20_003L,
                maxWeek = 4,
                nodeRange = MinuteRange(700, 760),
                weekPattern = pattern,
                segmentEndWeek = 4,
            )
            val sourceDates = TimetableEngine.expandOccurrences(source).map { it.epochDay }
            val merged = BackupImportPolicy.merge(current, source).timetable
            val mergedDates = TimetableEngine.expandOccurrences(merged)
                .filter { it.courseId == courseId }
                .map { it.epochDay }
            val mergedPattern = merged.courses.single { it.id == courseId }
                .slots.single().recurrenceSegments.single().weekPattern

            assertEquals(sourceDates, mergedDates)
            assertEquals(
                when (pattern) {
                    WeekPattern.ODD -> WeekPattern.EVEN
                    WeekPattern.EVEN -> WeekPattern.ODD
                    WeekPattern.ALL -> WeekPattern.ALL
                },
                mergedPattern,
            )
        }
    }

    @Test
    fun mergeRebasesDateExceptionAndOccurrenceReferencesWithoutLosingEffectiveTime() {
        val current = simpleMergeTimetable(
            courseId = "current",
            slotId = "current-slot",
            segmentId = "current-segment",
            firstDayEpochDay = 19_996L,
            maxWeek = 1,
            nodeRange = MinuteRange(480, 530),
        )
        val importedBase = simpleMergeTimetable(
            courseId = "incoming",
            slotId = "incoming-slot",
            segmentId = "incoming-segment",
            firstDayEpochDay = 20_010L,
            maxWeek = 2,
            nodeRange = MinuteRange(700, 760),
        )
        val importedWithException = importedBase.copy(
            dateExceptions = listOf(
                DateException(
                    id = "incoming-exception",
                    logicalSlotId = "incoming-slot",
                    originalEpochDay = 20_010L,
                    type = DateExceptionType.RESCHEDULE,
                    recurrenceSegmentId = "incoming-segment",
                    targetEpochDay = 20_011L,
                    targetDayOfWeek = 2,
                    targetStartNode = 1,
                    targetNodeCount = 1,
                    targetCustomTime = MinuteRange(710, 770),
                ),
            ),
        )
        val effectiveId = com.letr.sleepdown.domain.TimetableEngine.expandOccurrences(importedWithException).single().id
        val imported = importedWithException.copy(
            conflictPreferences = listOf(
                ConflictPreference(
                    courseId = "incoming",
                    logicalSlotId = "incoming-slot",
                    occurrenceId = effectiveId,
                    epochDay = 20_011L,
                ),
            ),
        )

        val result = BackupImportPolicy.merge(current, imported)
        val occurrence = com.letr.sleepdown.domain.TimetableEngine.expandOccurrences(result.timetable)
            .single { it.courseId == "incoming" }
        val exception = result.timetable.dateExceptions.single { it.id == "incoming-exception" }
        val preference = result.timetable.conflictPreferences.single { it.courseId == "incoming" }

        assertEquals(20_010L, occurrence.sourceEpochDay)
        assertEquals(20_011L, occurrence.epochDay)
        assertEquals(MinuteRange(710, 770), occurrence.minuteRange)
        assertEquals("incoming-slot", exception.logicalSlotId)
        assertEquals("incoming-segment", exception.recurrenceSegmentId)
        assertEquals(occurrence.id, preference.occurrenceId)
        assertEquals(20_011L, preference.epochDay)
        assertTrue(com.letr.sleepdown.domain.TimeTableValidator.validate(result.timetable).isValid)
    }

    @Test
    fun icsExportUsesFloatingTimesEscapingAndCrLfAndParsesBack() {
        val source = richTimetable().copy(dateExceptions = emptyList())
        val ics = IcsCodec.export(
            source,
            EpochDayRange(source.firstDayEpochDay, source.firstDayEpochDay),
            icsExportOptions,
        )
        val events = IcsCodec.parse(ics)
        val event = events.single()

        assertTrue(ics.endsWith("\r\n"))
        assertFalse(ics.replace("\r\n", "").contains('\n'))
        assertContains(ics, "DTSTART:202")
        assertContains(ics, "SUMMARY:Algorithms")
        assertContains(ics, "Room\\, A\\;1")
        assertContains(ics, "DESCRIPTION:Line 1\\nLine 2\\, with\\; punctuation")
        assertEquals("Algorithms", event.summary)
        assertEquals("Room, A;1", event.location)
        assertEquals("Line 1\nLine 2, with; punctuation", event.description)
        assertEquals(485, event.startMinuteOfDay)
        assertEquals(585, event.endMinuteOfDay)
        assertFalse(event.isDateEvent)
    }

    @Test
    fun icsExportIncludesCallerSuppliedDeterministicDtStamp() {
        val source = richTimetable().copy(dateExceptions = emptyList())
        val ics = IcsCodec.export(
            source,
            EpochDayRange(source.firstDayEpochDay, source.firstDayEpochDay),
            icsExportOptions,
        )

        assertContains(ics, "DTSTAMP:20241004T020300Z")
        assertEquals(1, IcsCodec.parse(ics).size)
    }

    @Test
    fun allPublicIcsExportHelpersEmitTheRequiredDtStamp() {
        val source = richTimetable().copy(dateExceptions = emptyList())
        val range = EpochDayRange(source.firstDayEpochDay, source.firstDayEpochDay)
        val outputs = listOf(
            IcsCodec.export(source, range, icsExportOptions),
            IcsCodec.encode(source, range, icsExportOptions),
            exportIcs(source, range, icsExportOptions),
        )

        outputs.forEach { output ->
            assertContains(output, "BEGIN:VEVENT\r\nDTSTAMP:20241004T020300Z\r\n")
        }
    }

    @Test
    fun icsExportUsesEffectiveRescheduledOccurrence() {
        val source = richTimetable()
        val events = IcsCodec.parse(
            IcsCodec.export(
                source,
                EpochDayRange(source.firstDayEpochDay, source.firstDayEpochDay + 2L),
                icsExportOptions,
            ),
        )

        assertEquals(1, events.size)
        assertEquals(source.firstDayEpochDay + 2L, events.single().startEpochDay)
        assertEquals(605, events.single().startMinuteOfDay)
        assertEquals("Room C", events.single().location)
    }

    @Test
    fun icsSupportsDateEventsAndUnfoldsContinuationLines() {
        val text = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:all-day-1",
            "DTSTART;VALUE=DATE:20260302",
            "DTEND;VALUE=DATE:20260303",
            "SUMMARY:All day\\, event",
            "DESCRIPTION:long first part",
            " continuation",
            "END:VEVENT",
            "END:VCALENDAR",
            "",
        ).joinToString("\r\n")
        val event = IcsCodec.parse(text).single()

        assertTrue(event.isDateEvent)
        assertEquals(null, event.startMinuteOfDay)
        assertEquals(1, event.endEpochDay - event.startEpochDay)
        assertEquals("All day, event", event.summary)
        assertEquals("long first partcontinuation", event.description)
        assertEquals(0, IcsCodec.toTimetable(text, firstDayEpochDay = event.startEpochDay).courses.single().slots.single().customTime?.startMinuteOfDay)
    }

    @Test
    fun backupDecodeReportsAggregateIssueDetails() {
        val invalid = BackupFormat.encode(richTimetable()).replace("\"maxWeek\":4", "\"maxWeek\":0")
        val error = assertFailsWith<BackupFormatException> {
            BackupFormat.decode(invalid)
        }

        assertContains(error.message ?: "", "maxWeek")
        assertTrue(error.issues.any { it.code == TimeTableValidationCode.INVALID_MAX_WEEK })
    }

    @Test
    fun icsImportDefaultsToEarliestEventMondayAndExpandsCoverage() {
        val text = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:tuesday",
            "DTSTART:20260303T080000",
            "DTEND:20260303T090000",
            "SUMMARY:Tuesday",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:next-monday",
            "DTSTART:20260309T080000",
            "DTEND:20260309T090000",
            "SUMMARY:Next Monday",
            "END:VEVENT",
            "END:VCALENDAR",
            "",
        ).joinToString("\r\n")
        val imported = IcsCodec.toTimetable(text)
        val events = IcsCodec.parse(text)

        assertEquals(events.minOf { it.startEpochDay } - 1L, imported.firstDayEpochDay)
        assertEquals(2, imported.maxWeek)
        assertEquals(2, imported.courses.size)
    }

    @Test
    fun unsupportedIcsSemanticsFailWithExplicitReasons() {
        fun event(property: String): String = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:unsupported",
            if (property.startsWith("DTSTART")) "" else "DTSTART:20260302T080000",
            "DTEND:20260302T090000",
            property,
            "END:VEVENT",
            "END:VCALENDAR",
            "",
        ).filter { it.isNotEmpty() }.joinToString("\r\n")

        val unsupported = listOf(
            "RRULE:FREQ=WEEKLY" to "RRULE",
            "RDATE:20260303T080000" to "RDATE",
            "EXDATE:20260303T080000" to "EXDATE",
            "RECURRENCE-ID:20260302T080000" to "RECURRENCE-ID",
            "STATUS:CANCELLED" to "STATUS:CANCELLED",
            "DTSTART;TZID=Asia/Shanghai:20260302T080000" to "TZID",
            "DTSTART:20260302T080000Z" to "UTC",
            "DTSTART:20260302T080001" to "nonzero seconds",
        )
        unsupported.forEach { (property, reason) ->
            val error = assertFailsWith<IcsFormatException> {
                IcsCodec.parse(event(property))
            }
            assertContains(error.message ?: "", reason)
        }
    }

    @Test
    fun duplicateIcsSingleValuePropertiesFailExplicitly() {
        val duplicate = listOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "BEGIN:VEVENT",
            "UID:duplicate",
            "DTSTART:20260302T080000",
            "DTSTART:20260302T081000",
            "DTEND:20260302T090000",
            "END:VEVENT",
            "END:VCALENDAR",
            "",
        ).joinToString("\r\n")
        val error = assertFailsWith<IcsFormatException> {
            IcsCodec.parse(duplicate)
        }
        assertContains(error.message ?: "", "duplicate single-value property DTSTART")
    }

    @Test
    fun malformedBackupAndIcsInputsProduceActionableErrors() {
        assertFailsWith<BackupFormatException> {
            BackupFormat.decode("{\"timetable\":{}}")
        }
        assertFailsWith<IcsFormatException> {
            IcsCodec.parse("BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:x\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n")
        }
        assertFailsWith<IcsFormatException> {
            IcsCodec.parse("BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\nUID:x\nDTSTART:20260302T080000\nDTEND:20260302T090000\nSUMMARY:x\nEND:VEVENT\nBEGIN:VEVENT\nUID:x\nDTSTART:20260303T080000\nDTEND:20260303T090000\nSUMMARY:y\nEND:VEVENT\nEND:VCALENDAR\n")
        }
        assertFailsWith<IcsFormatException> {
            IcsCodec.parse(
                "BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\nUID:x\nDTSTART:20260302T080000Z\nDTEND:20260302T090000Z\nSUMMARY:x\nEND:VEVENT\nEND:VCALENDAR\n",
            )
        }
    }

    private fun simpleMergeTimetable(
        courseId: String,
        slotId: String,
        segmentId: String,
        firstDayEpochDay: Long,
        maxWeek: Int,
        nodeRange: MinuteRange,
        weekPattern: WeekPattern = WeekPattern.ALL,
        segmentEndWeek: Int = 1,
    ): Timetable {
        val table = ScheduleTimeTable(
            id = "$courseId-table",
            name = "Source table",
            nodes = listOf(TimeTableNode(1, nodeRange.startMinuteOfDay, nodeRange.endMinuteOfDay)),
        )
        return Timetable(
            id = "$courseId-timetable",
            name = "Source timetable",
            firstDayEpochDay = firstDayEpochDay,
            maxWeek = maxWeek,
            timeTable = table,
            courses = listOf(
                Course(
                    id = courseId,
                    name = courseId,
                    slots = listOf(
                        LogicalCourseSlot(
                            id = slotId,
                            courseId = courseId,
                            dayOfWeek = 1,
                            startNode = 1,
                            nodeCount = 1,
                            recurrenceSegments = listOf(
                                RecurrenceSegment(
                                    id = segmentId,
                                    startWeek = 1,
                                    endWeek = segmentEndWeek,
                                    weekPattern = weekPattern,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
