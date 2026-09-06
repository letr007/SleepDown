package com.letr.sleepdown.data

import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.domain.ConflictPreference
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
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableCommand
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.interchange.BackupFormat
import com.letr.sleepdown.interchange.BackupImportMode
import com.letr.sleepdown.ui.aggregateMoveCommand
import com.letr.sleepdown.ui.aggregateSourceWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TimetableRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: TimetableRepository

    @Before
    fun setUp() {
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase(DATABASE_NAME)
        repository = TimetableRepository(context)
    }

    @After
    fun tearDown() {
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun domainMapperRoundTripsSplitRowsAndPerSegmentOverrides() {
        val table = tableEntity(1L, timeTableId = 7L, nodeCount = 2)
        val timeTable = ReusableTimeTableEntity(7L, "School", 3)
        val nodes = listOf(
            TimeTableNodeEntity(70L, 7L, 1, "08:00", "08:50"),
            TimeTableNodeEntity(71L, 7L, 2, "09:00", "09:50"),
        )
        val course = CourseEntity(10L, 1L, "Math", 0x123456, "Teacher", "note", 2f)
        val rows = listOf(
            CourseTimeEntity(
                id = 101L,
                courseId = 10L,
                day = 1,
                startNode = 1,
                step = 1,
                startWeek = 1,
                endWeek = 2,
                weekType = CourseTimeEntity.TYPE_ALL,
                room = "A",
                ownTime = false,
                logicalSlotId = "slot",
                teacher = "Teacher",
                recurrenceSegmentId = "segment-a",
            ),
            CourseTimeEntity(
                id = 102L,
                courseId = 10L,
                day = 2,
                startNode = 2,
                step = 1,
                startWeek = 3,
                endWeek = 4,
                weekType = CourseTimeEntity.TYPE_ODD,
                room = "B",
                ownTime = true,
                startTime = "10:05",
                endTime = "10:45",
                logicalSlotId = "slot",
                teacher = "Substitute",
                recurrenceSegmentId = "segment-b",
            ),
        )

        val domain = DomainMappers.toDomain(
            TimetableEntityRows(
                table = table,
                courses = listOf(course),
                courseTimes = rows,
                nodeTimes = emptyList(),
                reusableTimeTable = timeTable,
                timeTableNodes = nodes,
            ),
        )
        val slot = domain.courses.single().slots.single()
        assertEquals("slot", slot.id)
        assertEquals(2, slot.recurrenceSegments.size)
        assertEquals("Teacher", slot.teacher)
        assertEquals("A", slot.room)
        assertEquals(2, slot.recurrenceSegments[1].dayOfWeek)
        assertEquals("Substitute", slot.recurrenceSegments[1].teacher)
        assertEquals(MinuteRange(605, 645), slot.recurrenceSegments[1].customTime)

        val mapped = DomainMappers.toEntities(
            timetable = domain,
            tableId = 1L,
            courseIdByDomainId = mapOf("10" to 10L),
            timeTableId = 7L,
            existingTable = table,
            existingCourseTimes = rows,
            existingTimeTableNodes = nodes,
        )
        assertEquals(rows, mapped.courseTimes)
        assertEquals(nodes, mapped.timeTableNodes)
        assertEquals(table.sortOrder, mapped.table.sortOrder)
        assertEquals(table.sundayFirst, mapped.table.sundayFirst)
    }

    @Test
    fun legacyTemporalIdentityGroupsRoomTeacherAndCustomTimeOverrides() {
        val table = tableEntity(1L, timeTableId = 7L, nodeCount = 1)
        val course = CourseEntity(10L, 1L, "Math", 1, "Default teacher")
        val domain = DomainMappers.toDomain(
            TimetableEntityRows(
                table = table,
                courses = listOf(course),
                courseTimes = listOf(
                    CourseTimeEntity(
                        id = 1L,
                        courseId = 10L,
                        day = 1,
                        startNode = 1,
                        step = 1,
                        startWeek = 1,
                        endWeek = 1,
                        room = "Room A",
                        teacher = "Teacher A",
                        logicalSlotId = "legacy",
                        recurrenceSegmentId = "segment-a",
                    ),
                    CourseTimeEntity(
                        id = 2L,
                        courseId = 10L,
                        day = 1,
                        startNode = 1,
                        step = 1,
                        startWeek = 2,
                        endWeek = 2,
                        room = "Room B",
                        teacher = "Teacher B",
                        ownTime = true,
                        startTime = "09:05",
                        endTime = "09:45",
                        logicalSlotId = "legacy",
                        recurrenceSegmentId = "segment-b",
                    ),
                ),
                reusableTimeTable = ReusableTimeTableEntity(7L, "School", 0),
                timeTableNodes = listOf(TimeTableNodeEntity(70L, 7L, 1, "08:00", "08:50")),
            ),
        )

        val slot = domain.courses.single().slots.single()
        assertEquals(1, domain.courses.single().slots.size)
        assertEquals("Room A", slot.room)
        assertEquals("Teacher A", slot.teacher)
        assertEquals("Room B", slot.recurrenceSegments[1].room)
        assertEquals("Teacher B", slot.recurrenceSegments[1].teacher)
        assertEquals(MinuteRange(545, 585), slot.recurrenceSegments[1].customTime)
    }

    @Test
    fun legacyDefaultNodeRowsRemainAvailableWhileCanonicalNodesStayValid() = runBlocking {
        val tableId = repository.saveTable(tableEntity(id = 0L, nodeCount = 20))
        repository.saveNodeTimes(TimetableRepository.defaultNodeTimes(tableId))

        assertEquals(30, repository.getNodeTimes(tableId).size)
        assertEquals(20, repository.loadDomainTimetable(tableId).timeTable.nodes.size)
    }

    @Test
    fun savingDefaultCourseRowsAssignsLogicalAndSegmentIdsAndCommandUpdatesEverySplitRow() = runBlocking {
        val tableId = repository.saveTable(tableEntity(id = 0L, nodeCount = 3))
        val courseId = repository.saveCourse(
            tableId = tableId,
            course = CourseEntity(tableId = tableId, name = "Math", color = 1, teacher = "Teacher"),
            times = listOf(
                CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 2),
                CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 3, endWeek = 4),
            ),
        )
        val savedRows = repository.getCourseTimes(courseId)
        assertEquals(2, savedRows.size)
        assertTrue(savedRows.all { it.logicalSlotId.isNotBlank() && it.logicalSlotId != "legacy" })
        assertTrue(savedRows.all { it.recurrenceSegmentId.isNotBlank() && it.recurrenceSegmentId != "legacy" })
        assertEquals(savedRows[0].logicalSlotId, savedRows[1].logicalSlotId)
        assertNotEquals(savedRows[0].recurrenceSegmentId, savedRows[1].recurrenceSegmentId)

        val slotId = repository.loadDomainTimetable(tableId).courses.single().slots.single().id
        repository.applyTimetableCommand(
            tableId,
            TimetableCommand.MoveLogicalSlot(logicalSlotId = slotId, dayDelta = 1, startNodeDelta = 1),
        )
        val movedRows = repository.getCourseTimes(courseId)
        assertEquals(listOf(2, 2), movedRows.map { it.day })
        assertEquals(listOf(2, 2), movedRows.map { it.startNode })
    }

    @Test
    fun importedOccurrencePreferenceIsRemappedToPersistedCourseIdentity() = runBlocking {
        val tableId = repository.saveTable(tableEntity(0L, nodeCount = 2))
        val base = repository.loadDomainTimetable(tableId)
        val importedBase = Timetable(
            id = "incoming-table",
            name = "Incoming",
            firstDayEpochDay = base.firstDayEpochDay,
            maxWeek = 1,
            timeTable = base.timeTable,
            courses = listOf(
                com.letr.sleepdown.domain.Course(
                    id = "incoming-course",
                    name = "Incoming course",
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "incoming-slot",
                            courseId = "incoming-course",
                            dayOfWeek = 1,
                            startNode = 1,
                            recurrenceSegments = listOf(RecurrenceSegment("incoming-segment", endWeek = 1)),
                        ),
                    ),
                ),
            ),
        )
        val importedOccurrence = TimetableEngine.expandOccurrences(importedBase).single()
        val imported = importedBase.copy(
            conflictPreferences = listOf(
                ConflictPreference(
                    courseId = "incoming-course",
                    logicalSlotId = "incoming-slot",
                    occurrenceId = importedOccurrence.id,
                    epochDay = importedOccurrence.epochDay,
                    priority = 9,
                ),
            ),
        )

        val restored = repository.importDomainTimetable(tableId, imported, BackupImportMode.REPLACE)
        val persistedOccurrence = TimetableEngine.expandOccurrences(restored).single()
        val preference = restored.conflictPreferences.single()
        assertEquals(persistedOccurrence.courseId, preference.courseId)
        assertEquals(persistedOccurrence.logicalSlotId, preference.logicalSlotId)
        assertEquals(persistedOccurrence.id, preference.occurrenceId)
        assertEquals(9, preference.priority)
    }

    @Test
    fun tableSortOrderPersistsAndReusableTimeTableUpdatesAllLegacyProjections() = runBlocking {
        val timeTableId = repository.saveTimeTable(
            ScheduleTimeTable(
                id = "new",
                name = "Shared",
                nodes = listOf(
                    TimeTableNode(1, 480, 530),
                    TimeTableNode(2, 540, 590),
                ),
            ),
        )
        val first = repository.saveTable(tableEntity(0L, timeTableId, 2))
        val second = repository.saveTable(tableEntity(0L, timeTableId, 2).copy(name = "Second"))
        repository.reorderTables(listOf(second, first))
        assertEquals(listOf(second, first), repository.observeTables().first().map { it.id })

        repository.saveTimeTable(
            ScheduleTimeTable(
                id = timeTableId.toString(),
                name = "Shared edited",
                nodes = listOf(
                    TimeTableNode(1, 500, 545),
                    TimeTableNode(2, 555, 600),
                ),
            ),
        )
        assertEquals(listOf("08:20", "09:15"), repository.getNodeTimes(first).map { it.start })
        assertEquals(listOf("08:20", "09:15"), repository.getNodeTimes(second).map { it.start })
        assertEquals(1, repository.getReusableTimeTables().count { it.id == timeTableId })
    }

    @Test
    fun updatingCourseReconcilesRemovedSlotsWhilePreservingUnrelatedReferences() = runBlocking {
        val tableId = repository.saveTable(tableEntity(0L, nodeCount = 2))
        val courseId = repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Math", color = 1),
            listOf(
                CourseTimeEntity(
                    courseId = 0L,
                    day = 1,
                    startNode = 1,
                    step = 1,
                    startWeek = 1,
                    endWeek = 4,
                    logicalSlotId = "slot-a",
                    recurrenceSegmentId = "segment-a",
                ),
                CourseTimeEntity(
                    courseId = 0L,
                    day = 2,
                    startNode = 2,
                    step = 1,
                    startWeek = 1,
                    endWeek = 4,
                    logicalSlotId = "slot-b",
                    recurrenceSegmentId = "segment-b",
                ),
            ),
        )
        val otherCourseId = repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Physics", color = 2),
            listOf(
                CourseTimeEntity(
                    courseId = 0L,
                    day = 3,
                    startNode = 1,
                    step = 1,
                    startWeek = 1,
                    endWeek = 4,
                    logicalSlotId = "slot-c",
                    recurrenceSegmentId = "segment-c",
                ),
            ),
        )
        val before = repository.loadDomainTimetable(tableId)
        val math = before.courses.single { it.id == courseId.toString() }
        val slotA = math.slots.single { it.id == "slot-a" }
        val slotB = math.slots.single { it.id == "slot-b" }
        val occurrenceA = TimetableEngine.expandOccurrences(before)
            .first { it.logicalSlotId == slotA.id }
        val occurrenceB = TimetableEngine.expandOccurrences(before)
            .first { it.logicalSlotId == slotB.id }
        repository.saveDateException(
            tableId,
            DateException("removed-exception", slotB.id, occurrenceB.sourceEpochDay, DateExceptionType.CANCEL),
        )
        repository.saveConflictPreferences(
            tableId,
            listOf(
                ConflictPreference(
                    courseId = courseId.toString(),
                    logicalSlotId = slotA.id,
                    occurrenceId = occurrenceA.id,
                    epochDay = occurrenceA.epochDay,
                    priority = 4,
                ),
                ConflictPreference(
                    courseId = courseId.toString(),
                    logicalSlotId = slotB.id,
                    epochDay = occurrenceB.epochDay,
                    priority = 3,
                ),
                ConflictPreference(courseId = otherCourseId.toString(), priority = 1),
            ),
        )

        val updatedCourse = requireNotNull(repository.getCourse(courseId))
        val existingA = repository.getCourseTimes(courseId).single { it.logicalSlotId == "slot-a" }
        repository.saveCourse(
            tableId,
            updatedCourse,
            listOf(existingA.copy(ownTime = true, startTime = "08:05", endTime = "08:45", room = "New room")),
        )

        val after = repository.loadDomainTimetable(tableId)
        assertTrue(after.dateExceptions.isEmpty())
        assertEquals(2, after.conflictPreferences.size)
        assertTrue(after.conflictPreferences.any { it.courseId == otherCourseId.toString() })
        val preserved = after.conflictPreferences.single { it.courseId == courseId.toString() }
        val persistedA = TimetableEngine.expandOccurrences(after)
            .first { it.logicalSlotId == slotA.id }
        assertEquals(persistedA.id, preserved.occurrenceId)
        assertEquals(4, preserved.priority)
    }

    @Test
    fun exceptionsPreferencesAndReminderSettingsSurviveDomainPersistence() = runBlocking {
        val tableId = repository.saveTable(tableEntity(0L, nodeCount = 2))
        val courseId = repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Math", color = 1, teacher = "Teacher"),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 4)),
        )
        val timetable = repository.loadDomainTimetable(tableId)
        val slot = timetable.courses.single().slots.single()
        val segment = slot.recurrenceSegments.single()
        val sourceDay = timetable.firstDayEpochDay
        repository.saveDateException(
            tableId,
            DateException(
                id = "exception-1",
                logicalSlotId = slot.id,
                originalEpochDay = sourceDay,
                type = DateExceptionType.RESCHEDULE,
                recurrenceSegmentId = segment.id,
                targetEpochDay = sourceDay + 2,
                targetDayOfWeek = 3,
                targetStartNode = 2,
                targetCustomTime = MinuteRange(600, 640),
                targetTeacher = "Replacement",
                targetRoom = "Room C",
            ),
        )
        repository.saveConflictPreferences(
            tableId,
            listOf(
                ConflictPreference(
                    courseId = courseId.toString(),
                    priority = 8,
                    logicalSlotId = slot.id,
                    epochDay = sourceDay,
                ),
            ),
        )
        repository.saveReminderSettings(
            tableId,
            ReminderSettings(
                startEnabled = false,
                endEnabled = true,
                startLeadMinutes = 12,
                endLeadMinutes = 4,
                content = ReminderContentSettings(includeTeacher = false, includeNote = true),
                vibrate = false,
                silent = true,
            ),
        )

        val restored = repository.loadDomainTimetable(tableId)
        assertEquals("exception-1", restored.dateExceptions.single().id)
        assertEquals(8, restored.conflictPreferences.single().priority)
        assertEquals(4, restored.reminderSettings.endLeadMinutes)
        assertTrue(restored.reminderSettings.content.includeNote)
        assertTrue(restored.reminderSettings.silent)
    }

    @Test
    fun legacyJsonBackupRemainsImportableThroughTheSharedRepositoryPath() = runBlocking {
        val legacyTable = tableEntity(42L, nodeCount = 2)
        val legacyCourse = CourseEntity(9L, 42L, "Legacy", 7, "Teacher", "note", 1f)
        val result = repository.importBackup(
            BackupCodec.encode(
                BackupData(
                    table = legacyTable,
                    nodeTimes = listOf(
                        NodeTimeEntity(1L, 42L, 1, "08:00", "08:50"),
                        NodeTimeEntity(2L, 42L, 2, "09:00", "09:50"),
                    ),
                    courses = listOf(legacyCourse),
                    times = listOf(
                        CourseTimeEntity(
                            id = 3L,
                            courseId = 9L,
                            day = 1,
                            startNode = 1,
                            step = 1,
                            startWeek = 1,
                            endWeek = 1,
                        ),
                    ),
                ),
            ),
        )

        assertTrue(result.isSuccess)
        val imported = repository.loadDomainTimetable(result.getOrThrow())
        assertEquals("Legacy", imported.courses.single().name)
        assertEquals(2, imported.timeTable.nodes.size)
        assertTrue(imported.courses.single().slots.single().id.isNotBlank())
    }

    @Test
    fun copyingTableKeepsSharedTimeTableAndRemapsPerTableReferences() = runBlocking {
        val tableId = repository.saveTable(tableEntity(0L, nodeCount = 2))
        val courseId = repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Math", color = 1),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 1)),
        )
        val source = repository.loadDomainTimetable(tableId)
        val slot = source.courses.single().slots.single()
        repository.saveDateException(
            tableId,
            DateException("source-exception", slot.id, source.firstDayEpochDay, DateExceptionType.CANCEL),
        )
        repository.saveConflictPreferences(
            tableId,
            listOf(
                ConflictPreference(
                    courseId = courseId.toString(),
                    logicalSlotId = slot.id,
                    epochDay = source.firstDayEpochDay,
                    priority = 2,
                ),
            ),
        )

        val copiedTableId = repository.copyTable(tableId)
        val copied = repository.loadDomainTimetable(copiedTableId)
        assertEquals(source.timeTable.id, copied.timeTable.id)
        assertEquals(1, copied.dateExceptions.size)
        assertNotEquals("source-exception", copied.dateExceptions.single().id)
        assertEquals(1, copied.conflictPreferences.size)
        assertEquals(2, copied.conflictPreferences.single().priority)
        assertEquals(copiedTableId.toString(), copied.id)
    }

    @Test
    fun dateExceptionIdsAreIsolatedPerTableAndDeleteUsesTheDomainId() = runBlocking {
        val firstTableId = repository.saveTable(tableEntity(0L, nodeCount = 1))
        val secondTableId = repository.saveTable(tableEntity(0L, nodeCount = 1))
        val firstCourseId = repository.saveCourse(
            firstTableId,
            CourseEntity(tableId = firstTableId, name = "First", color = 1),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 1)),
        )
        val secondCourseId = repository.saveCourse(
            secondTableId,
            CourseEntity(tableId = secondTableId, name = "Second", color = 2),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 1)),
        )
        val first = repository.loadDomainTimetable(firstTableId)
        val second = repository.loadDomainTimetable(secondTableId)
        repository.saveDateException(
            firstTableId,
            DateException("shared-id", first.courses.single().slots.single().id, first.firstDayEpochDay, DateExceptionType.CANCEL),
        )
        repository.saveDateException(
            secondTableId,
            DateException("shared-id", second.courses.single().slots.single().id, second.firstDayEpochDay, DateExceptionType.CANCEL),
        )

        assertEquals("shared-id", repository.getDateExceptions(firstTableId).single().id)
        assertEquals("shared-id", repository.getDateExceptions(secondTableId).single().id)
        repository.deleteDateException(secondTableId, "shared-id")
        assertEquals(1, repository.getDateExceptions(firstTableId).size)
        assertTrue(repository.getDateExceptions(secondTableId).isEmpty())
        assertTrue(firstCourseId > 0L && secondCourseId > 0L)
    }

    @Test
    fun reusableTimeTableUpdateRejectsInvalidLinkedAggregateAtomically() = runBlocking {
        val timeTableId = repository.saveTimeTable(
            ScheduleTimeTable(
                id = "atomic",
                name = "Shared",
                nodes = listOf(
                    TimeTableNode(1, 480, 530),
                    TimeTableNode(2, 540, 590),
                ),
            ),
        )
        val firstTableId = repository.saveTable(tableEntity(0L, timeTableId, 2))
        val secondTableId = repository.saveTable(tableEntity(0L, timeTableId, 2).copy(name = "Second"))
        val courseId = repository.saveCourse(
            firstTableId,
            CourseEntity(tableId = firstTableId, name = "Node two", color = 1),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 2, step = 1, startWeek = 1, endWeek = 1)),
        )
        val oldTimeTable = requireNotNull(repository.loadTimeTable(timeTableId))
        val oldFirstNodes = repository.getNodeTimes(firstTableId)
        val oldSecondNodes = repository.getNodeTimes(secondTableId)
        var rejected = false
        try {
            repository.saveTimeTable(
                oldTimeTable.copy(nodes = listOf(TimeTableNode(1, 480, 530))),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(oldTimeTable, repository.loadTimeTable(timeTableId))
        assertEquals(oldFirstNodes, repository.getNodeTimes(firstTableId))
        assertEquals(oldSecondNodes, repository.getNodeTimes(secondTableId))
        assertEquals(2, requireNotNull(repository.getTable(firstTableId)).nodeCount)
        assertEquals(1, repository.getCourseTimes(courseId).size)
    }

    @Test
    fun referencedTimeTablesAndLastNodesCannotBeDeleted() = runBlocking {
        val sharedId = repository.saveTimeTable(
            ScheduleTimeTable(
                name = "Shared",
                nodes = listOf(TimeTableNode(1, 480, 530)),
            ),
        )
        val tableId = repository.saveTable(tableEntity(0L, sharedId, 1))
        var referenceRejected = false
        try {
            repository.deleteTimeTable(sharedId)
        } catch (_: IllegalArgumentException) {
            referenceRejected = true
        }
        assertTrue(referenceRejected)
        assertEquals(1, repository.loadTimeTable(sharedId)?.nodes?.size)

        var emptyRejected = false
        try {
            repository.saveTimeTable(ScheduleTimeTable(id = sharedId.toString(), name = "Shared", nodes = emptyList()))
        } catch (_: IllegalArgumentException) {
            emptyRejected = true
        }
        assertTrue(emptyRejected)
        assertEquals(tableId, repository.loadDomainTimetable(tableId).id.toLong())

        val editableTableId = repository.saveTable(tableEntity(0L, nodeCount = 2))
        repository.deleteNode(editableTableId, 1)
        var lastNodeRejected = false
        try {
            repository.deleteNode(editableTableId, 1)
        } catch (_: IllegalArgumentException) {
            lastNodeRejected = true
        }
        assertTrue(lastNodeRejected)
    }

    @Test
    fun sharedBackupImportSupportsMergeAndReplace() = runBlocking {
        val tableId = repository.saveTable(tableEntity(0L, nodeCount = 2))
        repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Current", color = 1),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 1)),
        )
        val base = repository.loadDomainTimetable(tableId)
        val imported = Timetable(
            id = "incoming",
            name = "Incoming",
            firstDayEpochDay = base.firstDayEpochDay,
            maxWeek = 1,
            timeTable = base.timeTable,
            courses = listOf(
                com.letr.sleepdown.domain.Course(
                    id = "incoming-course",
                    name = "Incoming course",
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "incoming-slot",
                            courseId = "incoming-course",
                            dayOfWeek = 1,
                            startNode = 1,
                            recurrenceSegments = listOf(RecurrenceSegment("incoming-segment", endWeek = 1)),
                        ),
                    ),
                ),
            ),
        )
        val backup = BackupFormat.encode(imported)
        assertTrue(repository.importBackup(tableId, backup, BackupImportMode.MERGE).isSuccess)
        assertEquals(2, repository.loadDomainTimetable(tableId).courses.size)
        assertTrue(repository.importBackup(tableId, backup, BackupImportMode.REPLACE).isSuccess)
        assertEquals(1, repository.getReusableTimeTables().size)
        val replaced = repository.loadDomainTimetable(tableId)
        assertEquals(1, replaced.courses.size)
        assertEquals("Incoming course", replaced.courses.single().name)
    }

    @Test
    fun bindingReusableTimeTableValidatesAggregateAndUpdatesProjectionAtomically() = runBlocking {
        val sourceId = repository.saveTimeTable(
            ScheduleTimeTable(
                name = "Source",
                nodes = listOf(TimeTableNode(1, 480, 530), TimeTableNode(2, 540, 590)),
            ),
        )
        val tableId = repository.saveTable(tableEntity(0L, sourceId, 2))
        repository.saveCourse(
            tableId,
            CourseEntity(tableId = tableId, name = "Node two", color = 1),
            listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 2, step = 1, startWeek = 1, endWeek = 1)),
        )
        val tooShortId = repository.saveTimeTable(
            ScheduleTimeTable(name = "Too short", nodes = listOf(TimeTableNode(1, 600, 650))),
        )
        val before = requireNotNull(repository.getTable(tableId))
        val beforeProjection = repository.getNodeTimes(tableId)
        var rejected = false
        try {
            repository.bindTimeTable(tableId, tooShortId)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(before, repository.getTable(tableId))
        assertEquals(beforeProjection, repository.getNodeTimes(tableId))

        val targetId = repository.saveTimeTable(
            ScheduleTimeTable(
                name = "Target",
                nodes = listOf(TimeTableNode(1, 500, 545), TimeTableNode(2, 555, 600)),
            ),
        )
        val updated = repository.bindTimeTable(tableId, targetId)
        assertEquals(targetId, updated.timeTableId)
        assertEquals(2, updated.nodeCount)
        assertEquals(listOf("08:20", "09:15"), repository.getNodeTimes(tableId).map { it.start })
        assertEquals(targetId.toString(), repository.loadDomainTimetable(tableId).timeTable.id)
    }

    @Test
    fun savingConflictPreferenceReplacesPreviousChoiceForTheSameConflictGroup() = runBlocking {
        val tableId = repository.saveTable(tableEntity(0L, nodeCount = 1))
        repeat(2) { index ->
            repository.saveCourse(
                tableId,
                CourseEntity(tableId = tableId, name = "Course ${index + 1}", color = index + 1),
                listOf(CourseTimeEntity(courseId = 0L, day = 1, startNode = 1, step = 1, startWeek = 1, endWeek = 1)),
            )
        }
        val timetable = repository.loadDomainTimetable(tableId)
        val group = TimetableEngine.conflictGroups(TimetableEngine.expandOccurrences(timetable), timetable.conflictPreferences).single()
        val first = group.occurrences.first()
        val second = group.occurrences.last()
        val groupIds = group.occurrences.mapTo(mutableSetOf()) { it.id }
        repository.saveConflictPreference(
            tableId,
            ConflictPreference(first.courseId, 1, first.logicalSlotId, first.id, first.epochDay),
            groupIds,
        )
        repository.saveConflictPreference(
            tableId,
            ConflictPreference(second.courseId, 1, second.logicalSlotId, second.id, second.epochDay),
            groupIds,
        )

        val restored = repository.loadDomainTimetable(tableId)
        assertEquals(listOf(second.id), restored.conflictPreferences.mapNotNull { it.occurrenceId })
        val restoredGroup = TimetableEngine.conflictGroups(
            TimetableEngine.expandOccurrences(restored),
            restored.conflictPreferences,
        ).single()
        assertEquals(second.id, restoredGroup.preferredOccurrenceId)
    }

    @Test
    fun aggregateMoveCommandUsesOccurrenceScopeAndPreservesCustomTimeOffset() {
        val firstDay = LocalDate.of(2026, 3, 2).toEpochDay()
        val timeTable = ScheduleTimeTable(
            id = "1",
            name = "School",
            nodes = listOf(TimeTableNode(1, 480, 530), TimeTableNode(2, 540, 590)),
        )
        val ordinary = Timetable(
            id = "table",
            name = "Table",
            firstDayEpochDay = firstDay,
            maxWeek = 4,
            timeTable = timeTable,
            courses = listOf(
                Course(
                    id = "course",
                    name = "Course",
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "slot",
                            courseId = "course",
                            dayOfWeek = 1,
                            startNode = 1,
                            recurrenceSegments = listOf(
                                RecurrenceSegment("segment-a", startWeek = 1, endWeek = 2),
                                RecurrenceSegment("segment-b", startWeek = 3, endWeek = 4),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val ordinaryOccurrence = TimetableEngine.expandOccurrences(ordinary).first()
        assertTrue(aggregateMoveCommand(ordinary, ordinaryOccurrence, 2, 2) is TimetableCommand.MoveLogicalSlot)

        val custom = ordinary.copy(
            courses = listOf(
                ordinary.courses.single().copy(
                    slots = listOf(
                        ordinary.courses.single().slots.single().copy(
                            customTime = MinuteRange(505, 565),
                            recurrenceSegments = listOf(RecurrenceSegment("segment-a", startWeek = 1, endWeek = 4)),
                        ),
                    ),
                ),
            ),
        )
        val customOccurrence = TimetableEngine.expandOccurrences(custom).first()
        val customMove = aggregateMoveCommand(custom, customOccurrence, 1, 2) as TimetableCommand.MoveLogicalSlot
        assertEquals(MinuteRange(565, 625), customMove.targetCustomTime)

        val overridden = ordinary.copy(
            courses = listOf(
                ordinary.courses.single().copy(
                    slots = listOf(
                        ordinary.courses.single().slots.single().copy(
                            recurrenceSegments = listOf(
                                RecurrenceSegment("segment-a", startWeek = 1, endWeek = 4, teacher = "Substitute"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val overriddenOccurrence = TimetableEngine.expandOccurrences(overridden).first()
        assertTrue(aggregateMoveCommand(overridden, overriddenOccurrence, 2, 2) is TimetableCommand.MoveRecurrenceSegment)

        val rescheduled = ordinary.copy(
            dateExceptions = listOf(
                DateException(
                    id = "move",
                    logicalSlotId = "slot",
                    originalEpochDay = firstDay,
                    type = DateExceptionType.RESCHEDULE,
                    recurrenceSegmentId = "segment-a",
                    targetEpochDay = firstDay + 16,
                    targetDayOfWeek = 3,
                    targetStartNode = 1,
                ),
            ),
        )
        val rescheduledOccurrence = TimetableEngine.expandOccurrences(rescheduled).single { it.isRescheduled }
        val rescheduledMove = aggregateMoveCommand(rescheduled, rescheduledOccurrence, 4, 2) as TimetableCommand.RescheduleOccurrence
        assertEquals(firstDay, rescheduledMove.originalEpochDay)
        assertEquals(firstDay + 17, rescheduledMove.targetEpochDay)
        assertEquals(1, aggregateSourceWeek(rescheduled, rescheduledOccurrence))
    }

    private fun tableEntity(id: Long, timeTableId: Long = 0L, nodeCount: Int = 2): TableEntity =
        TableEntity(
            id = id,
            name = if (id == 0L) "Table" else "Table $id",
            startDate = LocalDate.of(2026, 3, 2).toEpochDay(),
            maxWeek = 4,
            nodeCount = nodeCount,
            sortOrder = 0,
            timeTableId = timeTableId,
        )

    private companion object {
        const val DATABASE_NAME = "timetable.db"
    }
}
