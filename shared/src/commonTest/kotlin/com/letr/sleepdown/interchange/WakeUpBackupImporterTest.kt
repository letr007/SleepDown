package com.letr.sleepdown.interchange

import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.WeekPattern
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeUpBackupImporterTest {
    @Test
    fun syntheticWakeUp6122FiveLineFixtureMapsDetailsIntoLogicalSlots() {
        val parsed = WakeUpBackupImporter.parse(syntheticWakeUp6122FiveLineFixture)
        val timetable = WakeUpBackupImporter.importDocument(parsed, timetableId = "synthetic-wakeup")
        val course = timetable.courses.single()
        val regularSlot = course.slots.first()
        val customSlot = course.slots.last()

        assertEquals(3, parsed.timeTable.id)
        assertEquals("Synthetic timetable", parsed.timeTable.name)
        assertEquals(4, timetable.maxWeek)
        assertEquals(5, timetable.sortOrder)
        assertTrue(timetable.showSaturday)
        assertTrue(timetable.showSunday)
        assertFalse(timetable.sundayFirst)
        assertEquals("3", timetable.timeTable.id)
        assertEquals(3, timetable.timeTable.nodes.size)
        assertEquals("Algorithms", course.name)
        assertEquals(0xFF112233.toInt(), course.color)
        assertEquals("lab", course.note)
        assertEquals(3.5f, course.credit)
        assertEquals(2, course.slots.size)
        assertEquals(2, regularSlot.recurrenceSegments.size)
        assertEquals(1, regularSlot.dayOfWeek)
        assertEquals(1, regularSlot.startNode)
        assertEquals(2, regularSlot.nodeCount)
        assertEquals("Alice", regularSlot.recurrenceSegments[0].teacher)
        assertEquals("A-1", regularSlot.recurrenceSegments[0].room)
        assertEquals(WeekPattern.ALL, regularSlot.recurrenceSegments[0].weekPattern)
        assertEquals(1, regularSlot.recurrenceSegments[0].startWeek)
        assertEquals(2, regularSlot.recurrenceSegments[0].endWeek)
        assertEquals("Bob", regularSlot.recurrenceSegments[1].teacher)
        assertEquals("B-2", regularSlot.recurrenceSegments[1].room)
        assertEquals(WeekPattern.ODD, regularSlot.recurrenceSegments[1].weekPattern)
        assertEquals(MinuteRange(605, 655), customSlot.customTime)
        assertEquals("Carol", customSlot.recurrenceSegments.single().teacher)
        assertEquals("Lab", customSlot.recurrenceSegments.single().room)
        assertTrue(timetable.firstDayEpochDay != 0L)
    }

    @Test
    fun importerMapsWakeUpLevelsToDeterministicOccurrencePriorities() {
        val parsed = WakeUpBackupImporter.parse(syntheticWakeUp6122FiveLineFixture)
        val withLevels = parsed.copy(
            courseBases = parsed.courseBases + WakeUpCourseBaseBeanDto(
                color = "#445566",
                courseName = "Physics",
                id = 9,
                tableId = 8,
            ),
            courseDetails = parsed.courseDetails
                .map { detail ->
                    if (detail.id == 7 && detail.startWeek == 1) detail.copy(level = 4) else detail
                } + WakeUpCourseDetailBeanDto(
                    day = 1,
                    endWeek = 1,
                    id = 9,
                    level = 2,
                    ownTime = false,
                    room = "A-1",
                    startNode = 1,
                    startWeek = 1,
                    step = 2,
                    tableId = 8,
                    teacher = "Other",
                    type = 0,
                ),
        )

        val timetable = WakeUpBackupImporter.importDocument(withLevels, timetableId = "levelled-wakeup")
        val repeated = WakeUpBackupImporter.importDocument(withLevels, timetableId = "levelled-wakeup")
        val firstDay = timetable.firstDayEpochDay
        val algorithms = TimetableEngine.expandOccurrences(timetable)
            .single { it.courseId == "wakeup:8:course:7" && it.epochDay == firstDay }
        val physics = TimetableEngine.expandOccurrences(timetable)
            .single { it.courseId == "wakeup:8:course:9" && it.epochDay == firstDay }
        val algorithmPreference = timetable.conflictPreferences.single {
            it.occurrenceId == algorithms.id
        }
        val physicsPreference = timetable.conflictPreferences.single {
            it.occurrenceId == physics.id
        }
        val group = TimetableEngine.conflictGroups(
            timetable,
            EpochDayRange(firstDay, firstDay),
        ).single()

        assertEquals(4, algorithmPreference.priority)
        assertEquals(firstDay, algorithmPreference.epochDay)
        assertEquals(2, physicsPreference.priority)
        assertEquals(firstDay, physicsPreference.epochDay)
        assertEquals(algorithms.id, group.preferredOccurrenceId)
        assertEquals(timetable.conflictPreferences, repeated.conflictPreferences)
    }

    @Test
    fun parserAcceptsUnknownKeysButRequiresExactlyFiveNonblankValues() {
        val withBlankLines = "\n$syntheticWakeUp6122FiveLineFixture\n\n"
        val parsed = WakeUpBackupImporter.parse(withBlankLines.encodeToByteArray())
        assertEquals(1, parsed.courseBases.size)

        val tooFew = syntheticWakeUp6122FiveLineFixture.lineSequence().take(4).joinToString("\n")
        val error = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.parse(tooFew)
        }
        assertContains(error.message ?: "", "exactly five")

        val utf8Error = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.parse(byteArrayOf(0xC3.toByte(), 0x28))
        }
        assertContains(utf8Error.message ?: "", "valid UTF-8")
    }

    @Test
    fun parserReportsBrokenWakeUpReferencesAndInvalidDetailFields() {
        val brokenReference = syntheticWakeUp6122FiveLineFixture.replace(
            "\"timeTable\":3,\"unknownTimeField\":true",
            "\"timeTable\":99,\"unknownTimeField\":true",
        )
        val referenceError = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.importToTimetable(brokenReference)
        }
        assertContains(referenceError.message ?: "", "timeTable")
        assertContains(referenceError.message ?: "", "instead of 3")

        val invalidType = syntheticWakeUp6122FiveLineFixture.replace(
            "\"teacher\":\"Bob\",\"type\":1",
            "\"teacher\":\"Bob\",\"type\":7",
        )
        val typeError = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.importToTimetable(invalidType)
        }
        assertContains(typeError.message ?: "", "type")
        assertContains(typeError.message ?: "", "0 (all)")
    }

    @Test
    fun importerRejectsEmptyGappedNonchronologicalNodesAndUnresolvableSpans() {
        val parsed = WakeUpBackupImporter.parse(syntheticWakeUp6122FiveLineFixture)

        val emptyError = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.importDocument(parsed.copy(timeDetails = emptyList()))
        }
        assertContains(emptyError.message ?: "", "timeDetails")

        val gapped = parsed.copy(
            timeDetails = parsed.timeDetails.mapIndexed { index, detail ->
                when (index) {
                    1 -> detail.copy(node = 3)
                    2 -> detail.copy(node = 2)
                    else -> detail
                }
            },
        )
        val gapError = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.importDocument(gapped)
        }
        assertContains(gapError.message ?: "", "contiguous")

        val nonchronological = parsed.copy(
            timeDetails = parsed.timeDetails.mapIndexed { index, detail ->
                if (index == 1) detail.copy(startTime = "07:00", endTime = "07:50") else detail
            },
        )
        val orderError = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.importDocument(nonchronological)
        }
        assertContains(orderError.message ?: "", "starts before")

        val unresolvableSpan = parsed.copy(
            courseDetails = parsed.courseDetails.map { detail ->
                detail.copy(startNode = 3, step = 2)
            },
        )
        val spanError = assertFailsWith<WakeUpBackupParseException> {
            WakeUpBackupImporter.importDocument(unresolvableSpan)
        }
        assertContains(spanError.message ?: "", "startNode+step")
    }

    private companion object {
        /** Synthetic because no real WakeUp export sample is available in the repository. */
        val syntheticWakeUp6122FiveLineFixture = """
            {"courseLen":50,"id":3,"name":"Synthetic timetable","sameBreakLen":false,"sameLen":true,"theBreakLen":10,"unknownKey":"ignored"}
            [{"endTime":"08:50","node":1,"startTime":"08:00","timeTable":3},{"endTime":"09:50","node":2,"startTime":"09:00","timeTable":3},{"endTime":"10:50","node":3,"startTime":"10:00","timeTable":3,"unknownTimeField":true}]
            {"background":"","courseTextColor":-16777216,"id":8,"itemAlpha":50,"itemHeight":64,"itemTextSize":12,"maxWeek":4,"nodes":3,"school":"Synthetic University","showOtherWeekCourse":true,"showSat":true,"showSun":true,"showTime":true,"sortOrder":5,"startDate":"2026-3-2","strokeColor":-2130706433,"sundayFirst":false,"tableName":"Synthetic timetable","textColor":-16777216,"tid":"synthetic-tid","timeTable":3,"type":0,"updateTime":1700000000000,"widgetCourseTextColor":-16777216,"widgetItemAlpha":50,"widgetItemHeight":64,"widgetItemTextSize":12,"widgetStrokeColor":-2130706433,"widgetTextColor":-16777216}
            [{"color":"#112233","courseName":"Algorithms","credit":3.5,"id":7,"note":"lab","tableId":8}]
            [{"day":1,"endTime":"","endWeek":2,"id":7,"level":0,"ownTime":false,"room":"A-1","startNode":1,"startTime":"","startWeek":1,"step":2,"tableId":8,"teacher":"Alice","type":0},{"day":1,"endTime":"","endWeek":4,"id":7,"level":0,"ownTime":false,"room":"B-2","startNode":1,"startTime":"","startWeek":3,"step":2,"tableId":8,"teacher":"Bob","type":1},{"day":2,"endTime":"10:55","endWeek":2,"id":7,"level":0,"ownTime":true,"room":"Lab","startNode":3,"startTime":"10:05","startWeek":2,"step":1,"tableId":8,"teacher":"Carol","type":2}]
        """.trimIndent()
    }
}
