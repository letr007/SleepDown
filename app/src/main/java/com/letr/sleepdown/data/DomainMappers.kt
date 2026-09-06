package com.letr.sleepdown.data

import com.letr.sleepdown.domain.ConflictPreference
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.DateException
import com.letr.sleepdown.domain.DateExceptionType
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.ReminderContentSettings
import com.letr.sleepdown.domain.ReminderSettings
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.WeekPattern

/**
 * The complete Room-side representation of one shared timetable aggregate.
 *
 * The mapper deliberately contains no database or Android dependencies. Repository
 * code can therefore load all rows in one transaction and map them as one value.
 */
data class TimetableEntityRows(
    val table: TableEntity,
    val courses: List<CourseEntity>,
    val courseTimes: List<CourseTimeEntity>,
    val nodeTimes: List<NodeTimeEntity> = emptyList(),
    val reusableTimeTable: ReusableTimeTableEntity? = null,
    val timeTableNodes: List<TimeTableNodeEntity> = emptyList(),
    val dateExceptions: List<DateExceptionEntity> = emptyList(),
    val conflictPreferences: List<ConflictPreferenceEntity> = emptyList(),
    val reminderSettings: ReminderSettingsEntity? = null,
) {
    val times: List<CourseTimeEntity>
        get() = courseTimes

    val timeTable: ReusableTimeTableEntity?
        get() = reusableTimeTable
}

typealias DomainTimetableRows = TimetableEntityRows
typealias TimetableRows = TimetableEntityRows

/** Pure conversions between the Room rows and the shared timetable aggregate. */
object DomainMappers {
    fun toDomain(rows: TimetableEntityRows): Timetable = toDomain(
        table = rows.table,
        courses = rows.courses,
        courseTimes = rows.courseTimes,
        nodeTimes = rows.nodeTimes,
        reusableTimeTable = rows.reusableTimeTable,
        timeTableNodes = rows.timeTableNodes,
        dateExceptions = rows.dateExceptions,
        conflictPreferences = rows.conflictPreferences,
        reminderSettings = rows.reminderSettings,
    )

    fun fromRows(rows: TimetableEntityRows): Timetable = toDomain(rows)

    fun toTimetable(rows: TimetableEntityRows): Timetable = toDomain(rows)

    fun fromEntities(
        table: TableEntity,
        courses: List<CourseEntity>,
        times: List<CourseTimeEntity>,
        nodeTimes: List<NodeTimeEntity> = emptyList(),
        timeTable: ReusableTimeTableEntity? = null,
        timeTableNodes: List<TimeTableNodeEntity> = emptyList(),
        exceptions: List<DateExceptionEntity> = emptyList(),
        preferences: List<ConflictPreferenceEntity> = emptyList(),
        reminders: ReminderSettingsEntity? = null,
    ): Timetable = toDomain(
        table = table,
        courses = courses,
        courseTimes = times,
        nodeTimes = nodeTimes,
        reusableTimeTable = timeTable,
        timeTableNodes = timeTableNodes,
        dateExceptions = exceptions,
        conflictPreferences = preferences,
        reminderSettings = reminders,
    )

    fun toDomain(
        table: TableEntity,
        courses: List<CourseEntity>,
        courseTimes: List<CourseTimeEntity>,
        nodeTimes: List<NodeTimeEntity> = emptyList(),
        reusableTimeTable: ReusableTimeTableEntity? = null,
        timeTableNodes: List<TimeTableNodeEntity> = emptyList(),
        dateExceptions: List<DateExceptionEntity> = emptyList(),
        conflictPreferences: List<ConflictPreferenceEntity> = emptyList(),
        reminderSettings: ReminderSettingsEntity? = null,
    ): Timetable {
        val courseTimesByCourse = courseTimes.groupBy { it.courseId }
        val domainCourses = courses
            .sortedBy { it.id }
            .map { course ->
                val rowsForCourse = courseTimesByCourse[course.id].orEmpty()
                Course(
                    id = course.id.toString(),
                    name = course.name,
                    color = course.color,
                    note = course.note,
                    credit = course.credit,
                    slots = mapSlots(course, rowsForCourse),
                )
            }

        val storedNodes = timeTableNodes
            .filter { it.timeTableId == (reusableTimeTable?.id ?: it.timeTableId) }
            .sortedWith(compareBy<TimeTableNodeEntity> { it.node }.thenBy { it.id })
        val fallbackNodes = nodeTimes
            .filter { it.tableId == table.id }
            .sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id })
        val nodes = when {
            storedNodes.isNotEmpty() -> storedNodes.map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            }
            fallbackNodes.isNotEmpty() -> fallbackNodes.map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            }
            else -> emptyList()
        }
        val timeTableId = reusableTimeTable?.id?.toString()
            ?: table.timeTableId.takeIf { it > 0L }?.toString()
            ?: stableId("legacy-time-table", table.id.toString())
        val timeTable = ScheduleTimeTable(
            id = timeTableId,
            name = reusableTimeTable?.name ?: table.name,
            nodes = nodes,
        )

        return Timetable(
            id = table.id.toString(),
            name = table.name,
            firstDayEpochDay = table.startDate,
            maxWeek = table.maxWeek,
            timeTable = timeTable,
            courses = domainCourses,
            dateExceptions = dateExceptions
                .filter { it.tableId == table.id }
                .sortedWith(compareBy<DateExceptionEntity> { it.originalEpochDay }.thenBy { it.id })
                .map { exception -> exception.toDomain() },
            conflictPreferences = conflictPreferences
                .filter { it.tableId == table.id }
                .sortedWith(compareByDescending<ConflictPreferenceEntity> { it.priority }.thenBy { it.id })
                .map { preference ->
                    ConflictPreference(
                        courseId = preference.courseId.toString(),
                        priority = preference.priority,
                        logicalSlotId = preference.logicalSlotId,
                        occurrenceId = preference.occurrenceId,
                        epochDay = preference.epochDay,
                    )
                },
            reminderSettings = reminderSettings
                ?.takeIf { it.tableId == table.id }
                ?.let { settings -> settings.toDomain() }
                ?: ReminderSettings(),
            sortOrder = table.sortOrder,
            showSaturday = table.showSat,
            showSunday = table.showSun,
            sundayFirst = table.sundayFirst,
        )
    }

    fun fromTimetable(
        timetable: Timetable,
        tableId: Long = timetable.id.toLongOrNull() ?: 0L,
        courseIdByDomainId: Map<String, Long> = emptyMap(),
        timeTableId: Long = timetable.timeTable.id.toLongOrNull() ?: 0L,
        existingTable: TableEntity? = null,
        existingTimeTable: ReusableTimeTableEntity? = null,
        existingCourseTimes: List<CourseTimeEntity> = emptyList(),
        existingNodeTimes: List<NodeTimeEntity> = emptyList(),
        existingTimeTableNodes: List<TimeTableNodeEntity> = emptyList(),
    ): TimetableEntityRows = toEntities(
        timetable = timetable,
        tableId = tableId,
        courseIdByDomainId = courseIdByDomainId,
        timeTableId = timeTableId,
        existingTable = existingTable,
        existingTimeTable = existingTimeTable,
        existingCourseTimes = existingCourseTimes,
        existingNodeTimes = existingNodeTimes,
        existingTimeTableNodes = existingTimeTableNodes,
    )

    fun fromDomain(
        timetable: Timetable,
        tableId: Long = timetable.id.toLongOrNull() ?: 0L,
        courseIdByDomainId: Map<String, Long> = emptyMap(),
        timeTableId: Long = timetable.timeTable.id.toLongOrNull() ?: 0L,
        existingTable: TableEntity? = null,
        existingTimeTable: ReusableTimeTableEntity? = null,
        existingCourseTimes: List<CourseTimeEntity> = emptyList(),
        existingNodeTimes: List<NodeTimeEntity> = emptyList(),
        existingTimeTableNodes: List<TimeTableNodeEntity> = emptyList(),
    ): TimetableEntityRows = toEntities(
        timetable = timetable,
        tableId = tableId,
        courseIdByDomainId = courseIdByDomainId,
        timeTableId = timeTableId,
        existingTable = existingTable,
        existingTimeTable = existingTimeTable,
        existingCourseTimes = existingCourseTimes,
        existingNodeTimes = existingNodeTimes,
        existingTimeTableNodes = existingTimeTableNodes,
    )

    fun toEntities(
        timetable: Timetable,
        tableId: Long = timetable.id.toLongOrNull() ?: 0L,
        courseIdByDomainId: Map<String, Long> = emptyMap(),
        timeTableId: Long = timetable.timeTable.id.toLongOrNull() ?: 0L,
        existingTable: TableEntity? = null,
        existingTimeTable: ReusableTimeTableEntity? = null,
        existingCourseTimes: List<CourseTimeEntity> = emptyList(),
        existingNodeTimes: List<NodeTimeEntity> = emptyList(),
        existingTimeTableNodes: List<TimeTableNodeEntity> = emptyList(),
    ): TimetableEntityRows {
        val resolvedTimeTableId = if (timeTableId > 0L) {
            timeTableId
        } else {
            existingTable?.timeTableId?.takeIf { it > 0L } ?: 0L
        }
        val courseIds = timetable.courses.mapIndexed { index, course ->
            course.id to (courseIdByDomainId[course.id] ?: course.id.toLongOrNull()
                ?: throw IllegalArgumentException("No Room course id mapping for '${course.id}' at index $index"))
        }.toMap()
        val table = (existingTable ?: TableEntity(
            id = tableId,
            name = timetable.name,
            startDate = timetable.firstDayEpochDay,
            maxWeek = timetable.maxWeek,
            nodeCount = timetable.timeTable.nodes.size,
            showWeekend = timetable.showSaturday || timetable.showSunday,
            showSat = timetable.showSaturday,
            showSun = timetable.showSunday,
            sundayFirst = timetable.sundayFirst,
            sortOrder = timetable.sortOrder,
            timeTableId = resolvedTimeTableId,
        )).copy(
            id = tableId,
            name = timetable.name,
            startDate = timetable.firstDayEpochDay,
            maxWeek = timetable.maxWeek,
            nodeCount = timetable.timeTable.nodes.size,
            showWeekend = timetable.showSaturday || timetable.showSunday,
            showSat = timetable.showSaturday,
            showSun = timetable.showSunday,
            sundayFirst = timetable.sundayFirst,
            sortOrder = timetable.sortOrder,
            timeTableId = resolvedTimeTableId,
        )

        val occurrenceIdMap = occurrenceIdMapForCourseIds(timetable, courseIds)
        val rows = mutableListOf<CourseTimeEntity>()
        val courses = timetable.courses.map { course ->
            val courseId = courseIds.getValue(course.id)
            val oldCourseTimes = existingCourseTimes.filter { it.courseId == courseId }
            course.toEntity(courseId, tableId, oldCourseTimes).also {
                course.slots.forEach { slot ->
                    slot.recurrenceSegments.forEach { segment ->
                        val existing = existingCourseTimes.firstOrNull {
                            it.courseId == courseId && it.recurrenceSegmentId == segment.id
                        }
                        rows += segment.toEntity(
                            courseId = courseId,
                            slot = slot,
                            segment = segment,
                            existing = existing,
                        )
                    }
                }
            }
        }

        val canonical = ReusableTimeTableEntity(
            id = resolvedTimeTableId,
            name = timetable.timeTable.name,
            sortOrder = existingTimeTable?.sortOrder ?: timetable.sortOrder,
        )
        val canonicalNodes = timetable.timeTable.nodes.map { node ->
            val existing = existingTimeTableNodes.firstOrNull { it.timeTableId == resolvedTimeTableId && it.node == node.node }
            TimeTableNodeEntity(
                id = existing?.id ?: 0L,
                timeTableId = resolvedTimeTableId,
                node = node.node,
                start = MinuteOfDay.format(node.startMinuteOfDay),
                end = MinuteOfDay.format(node.endMinuteOfDay),
            )
        }
        val projectedNodes = timetable.timeTable.nodes.map { node ->
            val existing = existingNodeTimes.firstOrNull { it.tableId == tableId && it.node == node.node }
            NodeTimeEntity(
                id = existing?.id ?: 0L,
                tableId = tableId,
                node = node.node,
                start = MinuteOfDay.format(node.startMinuteOfDay),
                end = MinuteOfDay.format(node.endMinuteOfDay),
            )
        }

        return TimetableEntityRows(
            table = table,
            courses = courses,
            courseTimes = rows,
            nodeTimes = projectedNodes,
            reusableTimeTable = canonical,
            timeTableNodes = canonicalNodes,
            dateExceptions = timetable.dateExceptions.map { exception ->
                exception.toEntity(tableId)
            },
            conflictPreferences = timetable.conflictPreferences.map { preference ->
                preference.toEntity(tableId, courseIds, occurrenceIdMap)
            },
            reminderSettings = timetable.reminderSettings.toEntity(tableId),
        )
    }

    private fun mapSlots(
        course: CourseEntity,
        rows: List<CourseTimeEntity>,
    ): List<LogicalCourseSlot> {
        if (rows.isEmpty()) return emptyList()
        val sortedRows = rows.sortedWith(
            compareBy<CourseTimeEntity> { it.startWeek }
                .thenBy { it.endWeek }
                .thenBy { it.weekType }
                .thenBy { it.id },
        )
        val grouped = sortedRows.groupBy { row ->
            SlotKey(
                courseId = course.id,
                logicalSlotId = logicalSlotId(course.id, row),
            )
        }
        return grouped.entries
            .sortedWith(compareBy<Map.Entry<SlotKey, List<CourseTimeEntity>>> { it.key.logicalSlotId })
            .map { (key, slotRows) -> mapSlot(course, key.logicalSlotId, slotRows) }
    }

    private fun mapSlot(
        course: CourseEntity,
        logicalSlotId: String,
        rows: List<CourseTimeEntity>,
    ): LogicalCourseSlot {
        val sortedRows = rows.sortedWith(
            compareBy<CourseTimeEntity> { it.startWeek }
                .thenBy { it.endWeek }
                .thenBy { it.weekType }
                .thenBy { it.id },
        )
        val first = sortedRows.first()
        val effectiveTeachers = sortedRows.map { row ->
            if (usableId(row.logicalSlotId) == null && usableId(row.recurrenceSegmentId) == null) {
                row.teacher.ifBlank { course.teacher }
            } else {
                row.teacher
            }
        }
        val customTimes = sortedRows.map(::customTime)
        val slotCustomTime = customTimes.distinct().singleOrNull()
        val slotTeacher = effectiveTeachers.first()
        val slotRoom = first.room
        val slot = LogicalCourseSlot(
            id = logicalSlotId,
            courseId = course.id.toString(),
            dayOfWeek = first.day,
            startNode = first.startNode,
            nodeCount = first.step,
            teacher = slotTeacher,
            room = slotRoom,
            customTime = slotCustomTime,
            recurrenceSegments = sortedRows.mapIndexed { index, row ->
                val teacher = effectiveTeachers[index]
                val rowCustomTime = customTimes[index]
                RecurrenceSegment(
                    id = normalizedSegmentId(course.id, logicalSlotId, row, index),
                    startWeek = row.startWeek,
                    endWeek = row.endWeek,
                    weekPattern = row.weekType.toWeekPattern(),
                    dayOfWeek = row.day.takeIf { it != first.day },
                    startNode = row.startNode.takeIf { it != first.startNode },
                    nodeCount = row.step.takeIf { it != first.step },
                    teacher = teacher.takeIf { it != slotTeacher },
                    room = row.room.takeIf { it != slotRoom },
                    customTime = rowCustomTime.takeIf { it != slotCustomTime },
                )
            },
        )
        return slot
    }

    private fun Course.toEntity(
        courseId: Long,
        tableId: Long,
        oldCourseTime: List<CourseTimeEntity>?,
    ): CourseEntity = CourseEntity(
        id = courseId,
        tableId = tableId,
        name = name,
        color = color,
        // Domain teachers live on slots. Keep a useful legacy projection for old UI callers.
        teacher = slots.firstOrNull()?.teacher ?: oldCourseTime?.firstOrNull()?.teacher.orEmpty(),
        note = note,
        credit = credit,
    )

    private fun RecurrenceSegment.toEntity(
        courseId: Long,
        slot: LogicalCourseSlot,
        segment: RecurrenceSegment,
        existing: CourseTimeEntity?,
    ): CourseTimeEntity {
        val custom = segment.customTime ?: slot.customTime
        val day = segment.dayOfWeek ?: slot.dayOfWeek
        val startNode = segment.startNode ?: slot.startNode
        val nodeCount = segment.nodeCount ?: slot.nodeCount
        val teacher = segment.teacher ?: slot.teacher
        val room = segment.room ?: slot.room
        return CourseTimeEntity(
            id = existing?.id ?: 0L,
            courseId = courseId,
            day = day,
            startNode = startNode,
            step = nodeCount,
            startWeek = segment.startWeek,
            endWeek = segment.endWeek,
            weekType = segment.weekPattern.toWeekType(),
            room = room,
            ownTime = custom != null,
            startTime = custom?.let { MinuteOfDay.format(it.startMinuteOfDay) }.orEmpty(),
            endTime = custom?.let { MinuteOfDay.format(it.endMinuteOfDay) }.orEmpty(),
            logicalSlotId = slot.id,
            teacher = teacher,
            recurrenceSegmentId = segment.id,
        )
    }

    private fun DateException.toEntity(tableId: Long): DateExceptionEntity {
        val custom = targetCustomTime
        return DateExceptionEntity(
            id = dateExceptionStorageId(tableId, id),
            tableId = tableId,
            logicalSlotId = logicalSlotId,
            originalEpochDay = originalEpochDay,
            type = type.name,
            recurrenceSegmentId = recurrenceSegmentId,
            targetEpochDay = targetEpochDay,
            targetDayOfWeek = targetDayOfWeek,
            targetStartNode = targetStartNode,
            targetNodeCount = targetNodeCount,
            targetStartTime = custom?.let { MinuteOfDay.format(it.startMinuteOfDay) },
            targetEndTime = custom?.let { MinuteOfDay.format(it.endMinuteOfDay) },
            targetTeacher = targetTeacher,
            targetRoom = targetRoom,
        )
    }

    private fun ConflictPreference.toEntity(
        tableId: Long,
        courseIds: Map<String, Long>,
        occurrenceIdMap: Map<String, String>,
    ): ConflictPreferenceEntity = ConflictPreferenceEntity(
        tableId = tableId,
        courseId = courseIds[courseId]
            ?: throw IllegalArgumentException("No Room course id mapping for conflict preference '$courseId'"),
        priority = priority,
        logicalSlotId = logicalSlotId,
        occurrenceId = occurrenceId?.let { occurrenceIdMap[it] ?:
            throw IllegalArgumentException("No Room occurrence id mapping for conflict preference '$it'") },
        epochDay = epochDay,
    )

    private fun ReminderSettings.toEntity(tableId: Long): ReminderSettingsEntity = ReminderSettingsEntity(
        tableId = tableId,
        startEnabled = startEnabled,
        endEnabled = endEnabled,
        startLeadMinutes = startLeadMinutes,
        endLeadMinutes = endLeadMinutes,
        includeCourseName = content.includeCourseName,
        includeTeacher = content.includeTeacher,
        includeRoom = content.includeRoom,
        includeNote = content.includeNote,
        vibrate = vibrate,
        silent = silent,
    )

    private fun DateExceptionEntity.toDomain(): DateException {
        val custom = if (targetStartTime != null && targetEndTime != null) {
            minuteRangeOrNull(targetStartTime, targetEndTime)
        } else {
            null
        }
        return DateException(
            id = dateExceptionDomainId(tableId, id),
            logicalSlotId = logicalSlotId,
            originalEpochDay = originalEpochDay,
            type = type.toDateExceptionType(),
            recurrenceSegmentId = recurrenceSegmentId,
            targetEpochDay = targetEpochDay,
            targetDayOfWeek = targetDayOfWeek,
            targetStartNode = targetStartNode,
            targetNodeCount = targetNodeCount,
            targetCustomTime = custom,
            targetTeacher = targetTeacher,
            targetRoom = targetRoom,
        )
    }

    private fun ReminderSettingsEntity.toDomain(): ReminderSettings = ReminderSettings(
        startEnabled = startEnabled,
        endEnabled = endEnabled,
        startLeadMinutes = startLeadMinutes,
        endLeadMinutes = endLeadMinutes,
        content = ReminderContentSettings(
            includeCourseName = includeCourseName,
            includeTeacher = includeTeacher,
            includeRoom = includeRoom,
            includeNote = includeNote,
        ),
        vibrate = vibrate,
        silent = silent,
    )

    private fun customTime(row: CourseTimeEntity): MinuteRange? =
        if (row.ownTime) minuteRangeOrNull(row.startTime, row.endTime) else null

    private fun minuteRangeOrNull(start: String, end: String): MinuteRange? {
        val startMinute = MinuteOfDay.parse(start) ?: return null
        val endMinute = MinuteOfDay.parse(end) ?: return null
        return MinuteRange(startMinute, endMinute).takeIf { it.isValid }
    }

    private fun normalizedSegmentId(
        courseId: Long,
        logicalSlotId: String,
        row: CourseTimeEntity,
        index: Int,
    ): String {
        val stored = usableId(row.recurrenceSegmentId)
        return stored ?: stableId(
            "legacy-segment",
            courseId.toString(),
            logicalSlotId,
            row.id.takeIf { it > 0L }?.toString() ?: index.toString(),
            row.startWeek.toString(),
            row.endWeek.toString(),
            row.weekType.toString(),
        )
    }

    private fun Int.toWeekPattern(): WeekPattern = when (this) {
        CourseTimeEntity.TYPE_ALL -> WeekPattern.ALL
        CourseTimeEntity.TYPE_ODD -> WeekPattern.ODD
        CourseTimeEntity.TYPE_EVEN -> WeekPattern.EVEN
        else -> throw IllegalArgumentException("Unknown course time weekType: $this")
    }

    private fun WeekPattern.toWeekType(): Int = when (this) {
        WeekPattern.ALL -> CourseTimeEntity.TYPE_ALL
        WeekPattern.ODD -> CourseTimeEntity.TYPE_ODD
        WeekPattern.EVEN -> CourseTimeEntity.TYPE_EVEN
    }

    private fun String.toDateExceptionType(): DateExceptionType = when (trim().uppercase()) {
        DateExceptionType.CANCEL.name -> DateExceptionType.CANCEL
        DateExceptionType.RESCHEDULE.name -> DateExceptionType.RESCHEDULE
        else -> throw IllegalArgumentException("Unknown date exception type: $this")
    }

    private fun logicalSlotId(courseId: Long, row: CourseTimeEntity): String {
        val stored = usableId(row.logicalSlotId)
        val legacyParts = stored?.takeIf { it.startsWith("legacy|") }?.split('|')
        if (legacyParts != null && legacyParts.size >= 5 &&
            legacyParts[1].toLongOrNull() != null &&
            legacyParts[2].toIntOrNull() == row.day &&
            legacyParts[3].toIntOrNull() == row.startNode &&
            legacyParts[4].toIntOrNull() == row.step
        ) {
            return stableId(
                "legacy-slot",
                legacyParts[1],
                legacyParts[2],
                legacyParts[3],
                legacyParts[4],
            )
        }
        return stored ?: stableId(
            "legacy-slot",
            courseId.toString(),
            row.day.toString(),
            row.startNode.toString(),
            row.step.toString(),
        )
    }

    private fun occurrenceIdMapForCourseIds(
        timetable: Timetable,
        courseIds: Map<String, Long>,
    ): Map<String, String> {
        if (timetable.conflictPreferences.none { it.occurrenceId != null }) return emptyMap()
        val remapped = timetable.copy(
            courses = timetable.courses.map { course ->
                val courseId = courseIds.getValue(course.id).toString()
                course.copy(
                    id = courseId,
                    slots = course.slots.map { slot -> slot.copy(courseId = courseId) },
                )
            },
        )
        val remappedByKey = TimetableEngine.expandOccurrences(remapped).groupBy(::occurrenceKey)
        return TimetableEngine.expandOccurrences(timetable).associate { occurrence ->
            val candidates = remappedByKey[occurrenceKey(occurrence)].orEmpty()
            require(candidates.size == 1) {
                "No unique Room occurrence mapping for '${occurrence.id}'"
            }
            occurrence.id to candidates.single().id
        }
    }

    private fun occurrenceKey(occurrence: com.letr.sleepdown.domain.CourseOccurrence): String =
        stableId(
            "domain-occurrence",
            occurrence.logicalSlotId,
            occurrence.recurrenceSegmentId,
            occurrence.sourceEpochDay.toString(),
            occurrence.epochDay.toString(),
            occurrence.startMinuteOfDay.toString(),
            occurrence.endMinuteOfDay.toString(),
            occurrence.exceptionId ?: "",
        )

    private fun usableId(value: String): String? = value.trim()
        .takeIf { it.isNotEmpty() && it != "legacy" }

    private fun stableId(prefix: String, vararg parts: String): String = buildString {
        append(prefix)
        parts.forEach { part ->
            append('|')
            append(part.length)
            append(':')
            append(part)
        }
    }

    private data class SlotKey(
        val courseId: Long,
        val logicalSlotId: String,
    )
}

fun TimetableEntityRows.toDomainTimetable(): Timetable = DomainMappers.toDomain(this)

fun TimetableEntityRows.toDomain(): Timetable = DomainMappers.toDomain(this)

fun Timetable.toEntityRows(
    tableId: Long = id.toLongOrNull() ?: 0L,
    courseIdByDomainId: Map<String, Long> = emptyMap(),
    timeTableId: Long = timeTable.id.toLongOrNull() ?: 0L,
    existingTable: TableEntity? = null,
    existingTimeTable: ReusableTimeTableEntity? = null,
    existingCourseTimes: List<CourseTimeEntity> = emptyList(),
    existingNodeTimes: List<NodeTimeEntity> = emptyList(),
    existingTimeTableNodes: List<TimeTableNodeEntity> = emptyList(),
): TimetableEntityRows = DomainMappers.toEntities(
    timetable = this,
    tableId = tableId,
    courseIdByDomainId = courseIdByDomainId,
    timeTableId = timeTableId,
    existingTable = existingTable,
    existingTimeTable = existingTimeTable,
    existingCourseTimes = existingCourseTimes,
    existingNodeTimes = existingNodeTimes,
    existingTimeTableNodes = existingTimeTableNodes,
)
