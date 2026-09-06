package com.letr.sleepdown.domain

private data class EffectiveSlotValues(
    val dayOfWeek: Int,
    val startNode: Int,
    val nodeCount: Int,
    val teacher: String,
    val room: String,
    val customTime: MinuteRange?,
)

/** Pure recurrence, exception, conflict, and layout algorithms for a timetable. */
object TimetableEngine {
    fun recurringOccurrences(
        timetable: Timetable,
        range: EpochDayRange,
    ): List<CourseOccurrence> {
        if (timetable.maxWeek < 1) return emptyList()

        return timetable.courses
            .sortedBy { it.id }
            .flatMap { course ->
                course.slots
                    .sortedBy { it.id }
                    .flatMap { slot ->
                        slot.recurrenceSegments
                            .sortedBy { it.id }
                            .flatMap { segment ->
                                expandSegment(timetable, course, slot, segment, range)
                            }
                    }
            }
            .sortedWith(occurrenceComparator)
    }

    fun recurringOccurrences(timetable: Timetable): List<CourseOccurrence> =
        recurringOccurrences(timetable, timetable.coverageRange)

    fun expandOccurrences(
        timetable: Timetable,
        range: EpochDayRange,
    ): List<CourseOccurrence> =
        applyDateExceptions(timetable, recurringOccurrences(timetable, range), range)

    fun expandOccurrences(timetable: Timetable): List<CourseOccurrence> =
        expandOccurrences(timetable, timetable.coverageRange)

    /** Apply immutable date overrides to a recurring result. */
    fun applyDateExceptions(
        timetable: Timetable,
        recurring: List<CourseOccurrence>,
        range: EpochDayRange,
    ): List<CourseOccurrence> {
        val result = recurring.toMutableList()
        val exceptions = timetable.dateExceptions.sortedWith(
            compareBy<DateException> {
                it.originalEpochDay
            }.thenBy { it.logicalSlotId }
                .thenBy { it.recurrenceSegmentId ?: "" }
                .thenBy { it.id },
        )

        exceptions.forEach { exception ->
            val matching = result.filter { occurrence -> exception.matches(occurrence) }
            val sourceCandidates = if (matching.isNotEmpty()) {
                matching
            } else {
                sourceOccurrencesForException(timetable, exception)
            }

            when (exception.type) {
                DateExceptionType.CANCEL -> result.removeAll { occurrence -> exception.matches(occurrence) }
                DateExceptionType.RESCHEDULE -> {
                    val targetEpochDay = exception.targetEpochDay ?: return@forEach
                    val moved = if (targetEpochDay in range) {
                        sourceCandidates
                            .distinctBy { it.id }
                            .mapNotNull { source -> rescheduledOccurrence(timetable, source, exception) }
                    } else {
                        emptyList()
                    }
                    if (targetEpochDay in range && moved.isEmpty()) return@forEach
                    result.removeAll { occurrence -> exception.matches(occurrence) }
                    moved.forEach { result += it }
                }
            }
        }

        return result
            .distinctBy { it.id }
            .sortedWith(occurrenceComparator)
    }

    fun applyDateExceptions(
        timetable: Timetable,
        recurring: List<CourseOccurrence>,
    ): List<CourseOccurrence> =
        applyDateExceptions(timetable, recurring, timetable.coverageRange)

    /** Group transitive interval overlaps by date and choose a stable preferred item. */
    fun conflictGroups(
        occurrences: List<CourseOccurrence>,
        preferences: List<ConflictPreference> = emptyList(),
    ): List<ConflictGroup> =
        buildConflictGroups(occurrences, preferences, timeTable = null)

    fun conflictGroups(
        timetable: Timetable,
        range: EpochDayRange,
    ): List<ConflictGroup> =
        buildConflictGroups(
            occurrences = expandOccurrences(timetable, range),
            preferences = timetable.conflictPreferences,
            timeTable = timetable.timeTable,
        )

    /** Calculate vertical minute-boundary placement and optional overlap columns. */
    fun gridPlacement(
        occurrence: CourseOccurrence,
        timeTable: TimeTable,
        overlappingOccurrences: List<CourseOccurrence> = emptyList(),
    ): GridPlacement {
        val candidates = (overlappingOccurrences + occurrence)
            .distinctBy { it.id }
            .filter { it.epochDay == occurrence.epochDay && it.hasValidInterval() }
        val group = buildConflictGroups(candidates, emptyList(), timeTable)
            .firstOrNull { group -> group.occurrences.any { it.id == occurrence.id } }
        val placement = group?.placements?.get(occurrence.id)
        return placement ?: verticalPlacement(occurrence, timeTable).copy()
    }

    fun gridPlacements(
        occurrences: List<CourseOccurrence>,
        timeTable: TimeTable,
        preferences: List<ConflictPreference> = emptyList(),
    ): Map<String, GridPlacement> =
        buildConflictGroups(occurrences, preferences, timeTable)
            .flatMap { it.placements.entries }
            .associate { it.key to it.value }
}

fun recurringOccurrences(
    timetable: Timetable,
    range: EpochDayRange,
): List<CourseOccurrence> = TimetableEngine.recurringOccurrences(timetable, range)

fun recurringOccurrences(timetable: Timetable): List<CourseOccurrence> =
    TimetableEngine.recurringOccurrences(timetable)

fun expandOccurrences(
    timetable: Timetable,
    range: EpochDayRange,
): List<CourseOccurrence> = TimetableEngine.expandOccurrences(timetable, range)

fun expandOccurrences(timetable: Timetable): List<CourseOccurrence> =
    TimetableEngine.expandOccurrences(timetable)

fun applyDateExceptions(
    timetable: Timetable,
    recurring: List<CourseOccurrence>,
    range: EpochDayRange,
): List<CourseOccurrence> = TimetableEngine.applyDateExceptions(timetable, recurring, range)

fun conflictGroups(
    occurrences: List<CourseOccurrence>,
    preferences: List<ConflictPreference> = emptyList(),
): List<ConflictGroup> = TimetableEngine.conflictGroups(occurrences, preferences)

fun conflictGroups(
    timetable: Timetable,
    range: EpochDayRange,
): List<ConflictGroup> = TimetableEngine.conflictGroups(timetable, range)

fun gridPlacement(
    occurrence: CourseOccurrence,
    timeTable: TimeTable,
    overlappingOccurrences: List<CourseOccurrence> = emptyList(),
): GridPlacement = TimetableEngine.gridPlacement(occurrence, timeTable, overlappingOccurrences)

fun gridPlacements(
    occurrences: List<CourseOccurrence>,
    timeTable: TimeTable,
    preferences: List<ConflictPreference> = emptyList(),
): Map<String, GridPlacement> = TimetableEngine.gridPlacements(occurrences, timeTable, preferences)

private fun expandSegment(
    timetable: Timetable,
    course: Course,
    slot: LogicalCourseSlot,
    segment: RecurrenceSegment,
    range: EpochDayRange,
): List<CourseOccurrence> {
    val startWeek = maxOf(1, segment.startWeek)
    val endWeek = minOf(timetable.maxWeek, segment.endWeek)
    if (startWeek > endWeek) return emptyList()

    val values = effectiveValues(slot, segment)
    if (values.dayOfWeek !in 1..7 || values.nodeCount < 1 || values.startNode < 1) return emptyList()
    val minuteRange = values.customTime ?: timetable.timeTable.rangeForNodes(values.startNode, values.nodeCount)
    if (minuteRange == null || !minuteRange.isValid) return emptyList()

    return (startWeek..endWeek)
        .asSequence()
        .filter { segment.weekPattern.matches(it) }
        .map { week ->
            val epochDay = timetable.firstDayEpochDay + (week - 1L) * 7L + values.dayOfWeek - 1L
            week to epochDay
        }
        .filter { (_, epochDay) -> epochDay in range }
        .map { (week, epochDay) ->
            createOccurrence(
                timetable = timetable,
                course = course,
                slot = slot,
                segment = segment,
                sourceEpochDay = epochDay,
                epochDay = epochDay,
                week = week,
                values = values,
                minuteRange = minuteRange,
                isRescheduled = false,
                exceptionId = null,
            )
        }
        .toList()
}

private fun effectiveValues(
    slot: LogicalCourseSlot,
    segment: RecurrenceSegment,
): EffectiveSlotValues = EffectiveSlotValues(
    dayOfWeek = segment.dayOfWeek ?: slot.dayOfWeek,
    startNode = segment.startNode ?: slot.startNode,
    nodeCount = segment.nodeCount ?: slot.nodeCount,
    teacher = segment.teacher ?: slot.teacher,
    room = segment.room ?: slot.room,
    customTime = segment.customTime ?: slot.customTime,
)

private fun createOccurrence(
    timetable: Timetable,
    course: Course,
    slot: LogicalCourseSlot,
    segment: RecurrenceSegment,
    sourceEpochDay: Long,
    epochDay: Long,
    week: Int,
    values: EffectiveSlotValues,
    minuteRange: MinuteRange,
    isRescheduled: Boolean,
    exceptionId: String?,
): CourseOccurrence = CourseOccurrence(
    id = occurrenceId(course.id, slot.id, segment.id, sourceEpochDay, exceptionId),
    courseId = course.id,
    courseName = course.name,
    logicalSlotId = slot.id,
    recurrenceSegmentId = segment.id,
    sourceEpochDay = sourceEpochDay,
    epochDay = epochDay,
    dayOfWeek = values.dayOfWeek,
    week = week,
    startNode = values.startNode,
    nodeCount = values.nodeCount,
    startMinuteOfDay = minuteRange.startMinuteOfDay,
    endMinuteOfDay = minuteRange.endMinuteOfDay,
    teacher = values.teacher,
    room = values.room,
    note = course.note,
    color = course.color,
    usesCustomTime = values.customTime != null,
    isRescheduled = isRescheduled,
    exceptionId = exceptionId,
)

private fun sourceOccurrencesForException(
    timetable: Timetable,
    exception: DateException,
): List<CourseOccurrence> = timetable.courses
    .sortedBy { it.id }
    .flatMap { course ->
        course.slots
            .filter { it.id == exception.logicalSlotId }
            .sortedBy { it.id }
            .flatMap { slot ->
                slot.recurrenceSegments
                    .filter { exception.recurrenceSegmentId == null || it.id == exception.recurrenceSegmentId }
                    .sortedBy { it.id }
                    .mapNotNull { segment ->
                        sourceOccurrenceOnDate(timetable, course, slot, segment, exception.originalEpochDay)
                    }
            }
    }

private fun sourceOccurrenceOnDate(
    timetable: Timetable,
    course: Course,
    slot: LogicalCourseSlot,
    segment: RecurrenceSegment,
    epochDay: Long,
): CourseOccurrence? {
    val relativeDay = epochDay - timetable.firstDayEpochDay
    if (relativeDay < 0) return null
    val week = floorDiv(relativeDay, 7L).toInt() + 1
    val dayOfWeek = (floorMod(relativeDay, 7L) + 1L).toInt()
    if (week !in 1..timetable.maxWeek || dayOfWeek != (segment.dayOfWeek ?: slot.dayOfWeek)) return null
    if (week !in segment.startWeek..segment.endWeek || !segment.weekPattern.matches(week)) return null

    val values = effectiveValues(slot, segment)
    if (values.startNode < 1 || values.nodeCount < 1) return null
    val minuteRange = values.customTime ?: timetable.timeTable.rangeForNodes(values.startNode, values.nodeCount)
    if (values.dayOfWeek !in 1..7 || minuteRange == null || !minuteRange.isValid) {
        return null
    }
    return createOccurrence(
        timetable = timetable,
        course = course,
        slot = slot,
        segment = segment,
        sourceEpochDay = epochDay,
        epochDay = epochDay,
        week = week,
        values = values,
        minuteRange = minuteRange,
        isRescheduled = false,
        exceptionId = null,
    )
}

private fun rescheduledOccurrence(
    timetable: Timetable,
    source: CourseOccurrence,
    exception: DateException,
): CourseOccurrence? {
    val targetEpochDay = exception.targetEpochDay ?: return null
    val targetStartNode = exception.targetStartNode ?: source.startNode
    val targetNodeCount = exception.targetNodeCount ?: source.nodeCount
    val hasNodeOverride = exception.targetStartNode != null || exception.targetNodeCount != null
    val targetRange = when {
        exception.targetCustomTime != null -> exception.targetCustomTime
        hasNodeOverride -> timetable.timeTable.rangeForNodes(targetStartNode, targetNodeCount)
        else -> source.minuteRange
    } ?: return null
    if (!targetRange.isValid) return null

    val targetDayOfWeek = exception.targetDayOfWeek
        ?: timetableDayOfWeek(timetable, targetEpochDay)
    if (targetDayOfWeek !in 1..7 || targetStartNode < 1 || targetNodeCount < 1) return null

    return source.copy(
        id = occurrenceId(
            courseId = source.courseId,
            logicalSlotId = source.logicalSlotId,
            recurrenceSegmentId = source.recurrenceSegmentId,
            sourceEpochDay = source.sourceEpochDay,
            exceptionId = exception.id,
        ),
        epochDay = targetEpochDay,
        dayOfWeek = targetDayOfWeek,
        week = weekOf(timetable, targetEpochDay),
        startNode = targetStartNode,
        nodeCount = targetNodeCount,
        startMinuteOfDay = targetRange.startMinuteOfDay,
        endMinuteOfDay = targetRange.endMinuteOfDay,
        teacher = exception.targetTeacher ?: source.teacher,
        room = exception.targetRoom ?: source.room,
        usesCustomTime = exception.targetCustomTime != null || (!hasNodeOverride && source.usesCustomTime),
        isRescheduled = true,
        exceptionId = exception.id,
    )
}

private fun occurrenceId(
    courseId: String,
    logicalSlotId: String,
    recurrenceSegmentId: String,
    sourceEpochDay: Long,
    exceptionId: String?,
): String = lengthPrefixedId(
    "occurrence",
    courseId,
    logicalSlotId,
    recurrenceSegmentId,
    sourceEpochDay.toString(),
    exceptionId ?: "",
)

private fun DateException.matches(occurrence: CourseOccurrence): Boolean =
    occurrence.logicalSlotId == logicalSlotId &&
        occurrence.sourceEpochDay == originalEpochDay &&
        (recurrenceSegmentId == null || occurrence.recurrenceSegmentId == recurrenceSegmentId)

private fun weekOf(timetable: Timetable, epochDay: Long): Int {
    val relativeDay = epochDay - timetable.firstDayEpochDay
    return if (relativeDay < 0) 0 else floorDiv(relativeDay, 7L).toInt() + 1
}

private val occurrenceComparator = compareBy<CourseOccurrence> {
    it.epochDay
}.thenBy { it.startMinuteOfDay }
    .thenBy { it.endMinuteOfDay }
    .thenBy { it.courseId }
    .thenBy { it.logicalSlotId }
    .thenBy { it.recurrenceSegmentId }
    .thenBy { it.id }

private fun CourseOccurrence.hasValidInterval(): Boolean =
    startMinuteOfDay < endMinuteOfDay

private fun buildConflictGroups(
    occurrences: List<CourseOccurrence>,
    preferences: List<ConflictPreference>,
    timeTable: TimeTable?,
): List<ConflictGroup> {
    val groups = mutableListOf<ConflictGroup>()
    occurrences
        .filter { it.hasValidInterval() }
        .groupBy { it.epochDay }
        .entries
        .sortedBy { it.key }
        .forEach { entry ->
            val epochDay = entry.key
            val dayOccurrences = entry.value
            val sorted = dayOccurrences.sortedWith(occurrenceComparator)
            var index = 0
            while (index < sorted.size) {
                val component = mutableListOf(sorted[index])
                var runningEnd = sorted[index].endMinuteOfDay
                index++
                while (index < sorted.size && sorted[index].startMinuteOfDay < runningEnd) {
                    val occurrence = sorted[index]
                    component += occurrence
                    runningEnd = maxOf(runningEnd, occurrence.endMinuteOfDay)
                    index++
                }
                val preferred = choosePreferred(component, preferences)
                val id = lengthPrefixedId(
                    "conflict",
                    epochDay.toString(),
                    *component.map { it.id }.sorted().toTypedArray(),
                )
                val placements = timeTable?.let { layout(component, it) } ?: emptyMap()
                groups += ConflictGroup(
                    id = id,
                    epochDay = epochDay,
                    occurrences = component,
                    preferredOccurrenceId = preferred.id,
                    placements = placements,
                )
            }
        }
    return groups
}

private fun choosePreferred(
    occurrences: List<CourseOccurrence>,
    preferences: List<ConflictPreference>,
): CourseOccurrence = occurrences.sortedWith(
    compareByDescending<CourseOccurrence> { occurrence ->
        preferences.any { it.matches(occurrence) }
    }.thenByDescending { occurrence ->
        preferences.filter { it.matches(occurrence) }.maxOfOrNull { it.priority } ?: Int.MIN_VALUE
    }.thenByDescending { occurrence ->
        preferences.filter { it.matches(occurrence) }.maxOfOrNull { it.specificity } ?: 0
    }.thenBy { it.courseId }
        .thenBy { it.logicalSlotId }
        .thenBy { it.recurrenceSegmentId }
        .thenBy { it.id },
).first()

private val ConflictPreference.specificity: Int
    get() = when {
        occurrenceId != null -> 4
        logicalSlotId != null -> 3
        epochDay != null -> 2
        else -> 1
    }

private fun ConflictPreference.matches(occurrence: CourseOccurrence): Boolean =
    courseId == occurrence.courseId &&
        (logicalSlotId == null || logicalSlotId == occurrence.logicalSlotId) &&
        (occurrenceId == null || occurrenceId == occurrence.id) &&
        (epochDay == null || epochDay == occurrence.epochDay)

private fun layout(
    occurrences: List<CourseOccurrence>,
    timeTable: TimeTable,
): Map<String, GridPlacement> {
    val sorted = occurrences.sortedWith(occurrenceComparator)
    val columnEnd = mutableListOf<Int>()
    val columns = mutableMapOf<String, Int>()
    sorted.forEach { occurrence ->
        val column = columnEnd.indexOfFirst { it <= occurrence.startMinuteOfDay }
            .takeUnless { it == -1 }
            ?: columnEnd.size.also { columnEnd += Int.MIN_VALUE }
        columns[occurrence.id] = column
        columnEnd[column] = occurrence.endMinuteOfDay
    }
    val columnCount = maxOf(1, columnEnd.size)
    return sorted.associate { occurrence ->
        val column = columns.getValue(occurrence.id)
        occurrence.id to verticalPlacement(occurrence, timeTable).copy(
            leftFraction = column.toDouble() / columnCount,
            widthFraction = 1.0 / columnCount,
            column = column,
            columnCount = columnCount,
        )
    }
}

private fun verticalPlacement(
    occurrence: CourseOccurrence,
    timeTable: TimeTable,
): GridPlacement {
    val gridStart = timeTable.gridStartMinuteOfDay
    val gridEnd = timeTable.gridEndMinuteOfDay
    val span = (gridEnd - gridStart).coerceAtLeast(1)
    val clippedStart = occurrence.startMinuteOfDay.coerceIn(gridStart, gridEnd)
    val clippedEnd = occurrence.endMinuteOfDay.coerceIn(gridStart, gridEnd)
    val top = ((clippedStart - gridStart).toDouble() / span).coerceIn(0.0, 1.0)
    val bottom = ((clippedEnd - gridStart).toDouble() / span).coerceIn(0.0, 1.0)
    return GridPlacement(
        topFraction = top,
        heightFraction = (bottom - top).coerceAtLeast(0.0),
    )
}
