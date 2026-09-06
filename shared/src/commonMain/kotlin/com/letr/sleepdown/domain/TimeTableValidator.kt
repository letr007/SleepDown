package com.letr.sleepdown.domain

enum class TimeTableValidationCode {
    INVALID_NODE_NUMBER,
    DUPLICATE_NODE_NUMBER,
    INVALID_START_MINUTE,
    INVALID_END_MINUTE,
    END_NOT_AFTER_START,
    OVERLAPPING_NODES,
    INVALID_NODE_COUNT,
    NONCONTIGUOUS_NODES,
    NONCHRONOLOGICAL_NODES,
    INVALID_ID,
    DUPLICATE_ID,
    SLOT_COURSE_REFERENCE,
    INVALID_DAY_OF_WEEK,
    INVALID_RECURRENCE_RANGE,
    INVALID_RECURRENCE_PATTERN,
    INVALID_CUSTOM_TIME,
    INVALID_SEMESTER_ANCHOR,
    INVALID_MAX_WEEK,
    INVALID_COURSE_NODE_RANGE,
    MISSING_EXCEPTION_SLOT,
    AMBIGUOUS_EXCEPTION_SLOT,
    MISSING_EXCEPTION_SEGMENT,
    AMBIGUOUS_EXCEPTION_SEGMENT,
    INVALID_EXCEPTION_SOURCE,
    DUPLICATE_EXCEPTION_SOURCE,
    MISSING_EXCEPTION_TARGET,
    INVALID_EXCEPTION_TARGET,
    INVALID_CONFLICT_REFERENCE,
}

data class TimeTableValidationIssue(
    val code: TimeTableValidationCode,
    val node: Int,
    val otherNode: Int? = null,
    val message: String,
    val path: String = "",
)

data class TimeTableValidationResult(
    val issues: List<TimeTableValidationIssue>,
) {
    val isValid: Boolean
        get() = issues.isEmpty()

    val valid: Boolean
        get() = isValid

    fun requireValid(context: String = "Timetable"): TimeTableValidationResult {
        if (!isValid) throw TimetableValidationException(context, issues)
        return this
    }
}

class TimetableValidationException(
    val context: String,
    val issues: List<TimeTableValidationIssue>,
) : IllegalArgumentException(
    buildString {
        append(context)
        append(" is invalid: ")
        append(
            issues.joinToString("; ") { issue ->
                buildString {
                    if (issue.path.isNotBlank()) {
                        append(issue.path)
                        append(" ")
                    }
                    append(issue.message)
                }
            },
        )
    },
)

/** Validates both reusable node times and the complete timetable aggregate. */
object TimeTableValidator {
    fun validate(timeTable: TimeTable): TimeTableValidationResult =
        validateNodes(timeTable.nodes)

    fun validate(nodes: List<TimeTableNode>): TimeTableValidationResult =
        validateNodes(nodes)

    fun validate(timetable: Timetable): TimeTableValidationResult {
        val issues = mutableListOf<TimeTableValidationIssue>()
        if (timetable.maxWeek < 1) {
            issues += issue(
                TimeTableValidationCode.INVALID_MAX_WEEK,
                "maxWeek",
                "maxWeek must be at least 1",
            )
        }
        if (isoDayOfWeek(timetable.firstDayEpochDay) != 1) {
            issues += issue(
                TimeTableValidationCode.INVALID_SEMESTER_ANCHOR,
                "firstDayEpochDay",
                "firstDayEpochDay must represent a Monday",
            )
        }

        issues += validateNodes(timetable.timeTable.nodes).issues.map {
            it.copy(path = if (it.path.isBlank()) "timeTable.nodes" else "timeTable.nodes.${it.path}")
        }

        if (timetable.id.isBlank()) {
            issues += issue(TimeTableValidationCode.INVALID_ID, "timetable.id", "timetable id must not be blank")
        }
        if (timetable.timeTable.id.isBlank()) {
            issues += issue(TimeTableValidationCode.INVALID_ID, "timeTable.id", "time table id must not be blank")
        }
        val allEntityIds = mutableMapOf<String, String>()
        val courseIds = mutableMapOf<String, String>()
        val slotIds = mutableMapOf<String, String>()
        val segmentIds = mutableMapOf<String, String>()
        timetable.courses.forEachIndexed { courseIndex, course ->
            val coursePath = "courses[$courseIndex]"
            registerId(
                id = course.id,
                path = "$coursePath.id",
                ids = courseIds,
                issues = issues,
                globalIds = allEntityIds,
            )
            course.slots.forEachIndexed { slotIndex, slot ->
                val slotPath = "$coursePath.slots[$slotIndex]"
                registerId(slot.id, "$slotPath.id", slotIds, issues, globalIds = allEntityIds)
                if (slot.courseId.isBlank() || slot.courseId != course.id) {
                    issues += issue(
                        TimeTableValidationCode.SLOT_COURSE_REFERENCE,
                        slotPath,
                        "slot.courseId must reference its containing course",
                    )
                }
                validateDay(slot.dayOfWeek, "$slotPath.dayOfWeek", issues)
                validateCustomTime(slot.customTime, "$slotPath.customTime", issues)
                validateNodeRange(
                    startNode = slot.startNode,
                    nodeCount = slot.nodeCount,
                    nodes = timetable.timeTable.nodes,
                    requireExistingSpan = slot.customTime == null,
                    path = slotPath,
                    issues = issues,
                )

                slot.recurrenceSegments.forEachIndexed { segmentIndex, segment ->
                    val segmentPath = "$slotPath.recurrenceSegments[$segmentIndex]"
                    registerId(segment.id, "$segmentPath.id", segmentIds, issues, globalIds = allEntityIds)
                    validateRecurrence(segment, timetable.maxWeek, segmentPath, issues)
                    val day = segment.dayOfWeek ?: slot.dayOfWeek
                    val startNode = segment.startNode ?: slot.startNode
                    val nodeCount = segment.nodeCount ?: slot.nodeCount
                    val customTime = segment.customTime ?: slot.customTime
                    validateDay(day, "$segmentPath.dayOfWeek", issues)
                    validateCustomTime(customTime, "$segmentPath.customTime", issues)
                    validateNodeRange(
                        startNode = startNode,
                        nodeCount = nodeCount,
                        nodes = timetable.timeTable.nodes,
                        requireExistingSpan = customTime == null,
                        path = segmentPath,
                        issues = issues,
                    )
                }
            }
        }

        val slotOccurrences = if (timetable.maxWeek >= 1 && timetable.timeTable.nodes.isNotEmpty()) {
            TimetableEngine.recurringOccurrences(timetable, timetable.coverageRange)
        } else {
            emptyList()
        }
        val slotsById = timetable.courses
            .flatMap { course -> course.slots.map { course.id to it } }
            .groupBy { it.second.id }
        val segmentsBySlot = timetable.courses
            .flatMap { course -> course.slots.flatMap { slot -> slot.recurrenceSegments.map { slot.id to it } } }
            .groupBy { it.first }

        val exceptionIds = mutableMapOf<String, String>()
        val exceptionSources = mutableMapOf<String, String>()
        timetable.dateExceptions.forEachIndexed { exceptionIndex, exception ->
            val exceptionPath = "dateExceptions[$exceptionIndex]"
            registerId(exception.id, "$exceptionPath.id", exceptionIds, issues, globalIds = allEntityIds)
            if (exception.logicalSlotId.isBlank()) {
                issues += issue(TimeTableValidationCode.INVALID_ID, "$exceptionPath.logicalSlotId", "logicalSlotId must not be blank")
            }
            val slotCandidates = slotsById[exception.logicalSlotId].orEmpty()
            when {
                slotCandidates.isEmpty() -> issues += issue(
                    TimeTableValidationCode.MISSING_EXCEPTION_SLOT,
                    "$exceptionPath.logicalSlotId",
                    "date exception references a missing logical slot",
                )
                slotCandidates.size > 1 -> issues += issue(
                    TimeTableValidationCode.AMBIGUOUS_EXCEPTION_SLOT,
                    "$exceptionPath.logicalSlotId",
                    "date exception references more than one logical slot",
                )
            }
            if (exception.originalEpochDay !in timetable.coverageRange) {
                issues += issue(
                    TimeTableValidationCode.INVALID_EXCEPTION_SOURCE,
                    "$exceptionPath.originalEpochDay",
                    "originalEpochDay must be inside the timetable coverage range",
                )
            }
            val slot = slotCandidates.singleOrNull()?.second
            val segmentCandidates = if (slot == null || exception.recurrenceSegmentId == null) {
                emptyList()
            } else {
                segmentsBySlot[slot.id].orEmpty().filter { it.second.id == exception.recurrenceSegmentId }
            }
            if (exception.recurrenceSegmentId != null) {
                when {
                    slot == null || segmentCandidates.isEmpty() -> issues += issue(
                        TimeTableValidationCode.MISSING_EXCEPTION_SEGMENT,
                        "$exceptionPath.recurrenceSegmentId",
                        "date exception references a missing recurrence segment",
                    )
                    segmentCandidates.size > 1 -> issues += issue(
                        TimeTableValidationCode.AMBIGUOUS_EXCEPTION_SEGMENT,
                        "$exceptionPath.recurrenceSegmentId",
                        "date exception references more than one recurrence segment",
                    )
                }
            }
            val sourceMatches = slotOccurrences.filter { occurrence ->
                occurrence.logicalSlotId == exception.logicalSlotId &&
                    occurrence.sourceEpochDay == exception.originalEpochDay &&
                    (exception.recurrenceSegmentId == null || occurrence.recurrenceSegmentId == exception.recurrenceSegmentId)
            }
            if (sourceMatches.size != 1) {
                issues += issue(
                    TimeTableValidationCode.INVALID_EXCEPTION_SOURCE,
                    exceptionPath,
                    when {
                        sourceMatches.isEmpty() -> "date exception source does not match a recurring occurrence"
                        else -> "date exception source is ambiguous (${sourceMatches.size} recurring occurrences match)"
                    },
                )
            } else {
                val sourceOccurrenceId = sourceMatches.single().id
                val previousPath = exceptionSources.put(sourceOccurrenceId, exceptionPath)
                if (previousPath != null) {
                    issues += issue(
                        TimeTableValidationCode.DUPLICATE_EXCEPTION_SOURCE,
                        exceptionPath,
                        "date exception duplicates source $previousPath",
                    )
                }
            }

            when (exception.type) {
                DateExceptionType.CANCEL -> {
                    if (exception.hasTargetValues()) {
                        issues += issue(
                            TimeTableValidationCode.INVALID_EXCEPTION_TARGET,
                            exceptionPath,
                            "cancel exceptions must not contain reschedule target values",
                        )
                    }
                }
                DateExceptionType.RESCHEDULE -> {
                    val targetEpochDay = exception.targetEpochDay
                    if (targetEpochDay == null) {
                        issues += issue(
                            TimeTableValidationCode.MISSING_EXCEPTION_TARGET,
                            "$exceptionPath.targetEpochDay",
                            "reschedule exception requires targetEpochDay",
                        )
                    } else if (targetEpochDay !in timetable.coverageRange) {
                        issues += issue(
                            TimeTableValidationCode.INVALID_EXCEPTION_TARGET,
                            "$exceptionPath.targetEpochDay",
                            "targetEpochDay must be inside the timetable coverage range",
                        )
                    }
                    val targetDay = exception.targetDayOfWeek
                    if (targetDay != null) {
                        validateDay(targetDay, "$exceptionPath.targetDayOfWeek", issues)
                        if (targetEpochDay != null && targetEpochDay in timetable.coverageRange &&
                            timetableDayOfWeek(timetable, targetEpochDay) != targetDay
                        ) {
                            issues += issue(
                                TimeTableValidationCode.INVALID_EXCEPTION_TARGET,
                                "$exceptionPath.targetDayOfWeek",
                                "targetDayOfWeek does not match targetEpochDay",
                            )
                        }
                    }
                    val source = sourceMatches.singleOrNull()
                    val targetStartNode = exception.targetStartNode ?: source?.startNode
                    val targetNodeCount = exception.targetNodeCount ?: source?.nodeCount
                    val hasNodeOverride = exception.targetStartNode != null || exception.targetNodeCount != null
                    validateCustomTime(exception.targetCustomTime, "$exceptionPath.targetCustomTime", issues)
                    val usesInheritedCustomTime =
                        exception.targetCustomTime == null && !hasNodeOverride && source?.usesCustomTime == true
                    if (targetStartNode != null && targetNodeCount != null) {
                        validateNodeRange(
                            startNode = targetStartNode,
                            nodeCount = targetNodeCount,
                            nodes = timetable.timeTable.nodes,
                            requireExistingSpan = !usesInheritedCustomTime && exception.targetCustomTime == null,
                            path = exceptionPath,
                            issues = issues,
                        )
                    }
                }
            }
        }

        val courseById = timetable.courses.groupBy { it.id }
        val effectiveOccurrences = if (slotOccurrences.isNotEmpty()) {
            TimetableEngine.applyDateExceptions(timetable, slotOccurrences, timetable.coverageRange)
        } else {
            emptyList()
        }
        timetable.conflictPreferences.forEachIndexed { preferenceIndex, preference ->
            val preferencePath = "conflictPreferences[$preferenceIndex]"
            val courses = courseById[preference.courseId].orEmpty()
            if (preference.courseId.isBlank() || courses.size != 1) {
                issues += issue(
                    TimeTableValidationCode.INVALID_CONFLICT_REFERENCE,
                    "$preferencePath.courseId",
                    "conflict preference must reference exactly one existing course",
                )
            }
            if (preference.logicalSlotId != null) {
                val slots = slotsById[preference.logicalSlotId].orEmpty()
                if (slots.size != 1 || slots.single().first != preference.courseId) {
                    issues += issue(
                        TimeTableValidationCode.INVALID_CONFLICT_REFERENCE,
                        "$preferencePath.logicalSlotId",
                        "conflict preference logicalSlotId must reference one slot of courseId",
                    )
                }
            }
            if (preference.epochDay != null && preference.epochDay !in timetable.coverageRange) {
                issues += issue(
                    TimeTableValidationCode.INVALID_CONFLICT_REFERENCE,
                    "$preferencePath.epochDay",
                    "conflict preference epochDay must be inside the timetable coverage range",
                )
            }
            if (preference.occurrenceId != null) {
                val occurrence = effectiveOccurrences.firstOrNull { it.id == preference.occurrenceId }
                if (occurrence == null) {
                    issues += issue(
                        TimeTableValidationCode.INVALID_CONFLICT_REFERENCE,
                        "$preferencePath.occurrenceId",
                        "conflict preference occurrenceId does not reference an effective occurrence",
                    )
                } else {
                    if (occurrence.courseId != preference.courseId ||
                        (preference.logicalSlotId != null && occurrence.logicalSlotId != preference.logicalSlotId) ||
                        (preference.epochDay != null && occurrence.epochDay != preference.epochDay)
                    ) {
                        issues += issue(
                            TimeTableValidationCode.INVALID_CONFLICT_REFERENCE,
                            preferencePath,
                            "conflict preference occurrenceId does not belong to its course, slot, or epochDay reference",
                        )
                    }
                }
            }
        }

        return TimeTableValidationResult(sortIssues(issues))
    }

    fun isValid(timeTable: TimeTable): Boolean = validate(timeTable).isValid

    fun isValid(timetable: Timetable): Boolean = validate(timetable).isValid

    fun requireValid(timetable: Timetable, context: String = "Timetable"): Timetable {
        validate(timetable).requireValid(context)
        return timetable
    }
}

fun validateTimeTable(timeTable: TimeTable): TimeTableValidationResult =
    TimeTableValidator.validate(timeTable)

fun validateTimetable(timetable: Timetable): TimeTableValidationResult =
    TimeTableValidator.validate(timetable)

fun requireValidTimetable(timetable: Timetable, context: String = "Timetable"): Timetable =
    TimeTableValidator.requireValid(timetable, context)

private fun validateNodes(nodes: List<TimeTableNode>): TimeTableValidationResult {
    val issues = mutableListOf<TimeTableValidationIssue>()
    if (nodes.size !in 1..60) {
        issues += issue(
            TimeTableValidationCode.INVALID_NODE_COUNT,
            "nodes",
            "time table must contain between 1 and 60 nodes",
        )
    }
    nodes.forEachIndexed { index, item ->
        val expected = index + 1
        if (item.node != expected) {
            issues += TimeTableValidationIssue(
                code = if (item.node < 1) TimeTableValidationCode.INVALID_NODE_NUMBER else TimeTableValidationCode.NONCONTIGUOUS_NODES,
                node = item.node,
                message = if (item.node < 1) "node number must be positive" else "node number must be contiguous at $expected",
                path = "[$index]",
            )
        }
        if (nodes.take(index).any { it.node == item.node }) {
            issues += TimeTableValidationIssue(
                code = TimeTableValidationCode.DUPLICATE_NODE_NUMBER,
                node = item.node,
                otherNode = item.node,
                message = "node number ${item.node} appears more than once",
                path = "nodes[$index]",
            )
        }
        if (item.startMinuteOfDay !in 0 until MINUTES_PER_DAY) {
            issues += issue(
                TimeTableValidationCode.INVALID_START_MINUTE,
                "nodes[$index]",
                "start minute must be between 0 and ${MINUTES_PER_DAY - 1}",
                node = item.node,
            )
        }
        if (item.endMinuteOfDay !in 1..MINUTES_PER_DAY) {
            issues += issue(
                TimeTableValidationCode.INVALID_END_MINUTE,
                "nodes[$index]",
                "end minute must be between 1 and $MINUTES_PER_DAY",
                node = item.node,
            )
        }
        if (item.startMinuteOfDay in 0 until MINUTES_PER_DAY &&
            item.endMinuteOfDay in 1..MINUTES_PER_DAY &&
            item.startMinuteOfDay >= item.endMinuteOfDay
        ) {
            issues += issue(
                TimeTableValidationCode.END_NOT_AFTER_START,
                "nodes[$index]",
                "end minute must be after start minute",
                node = item.node,
            )
        }
        if (index > 0) {
            val previous = nodes[index - 1]
            if (item.startMinuteOfDay < previous.startMinuteOfDay) {
                issues += TimeTableValidationIssue(
                    code = TimeTableValidationCode.NONCHRONOLOGICAL_NODES,
                    node = item.node,
                    otherNode = previous.node,
                    message = "node ${item.node} starts before node ${previous.node}",
                    path = "nodes[$index]",
                )
            }
            if (previous.minuteRange.isValid && item.minuteRange.isValid &&
                item.startMinuteOfDay < previous.endMinuteOfDay
            ) {
                issues += TimeTableValidationIssue(
                    code = TimeTableValidationCode.OVERLAPPING_NODES,
                    node = previous.node,
                    otherNode = item.node,
                    message = "node ${previous.node} overlaps node ${item.node}",
                    path = "nodes[$index]",
                )
            }
        }
    }
    return TimeTableValidationResult(sortIssues(issues))
}

private fun validateRecurrence(
    segment: RecurrenceSegment,
    maxWeek: Int,
    path: String,
    issues: MutableList<TimeTableValidationIssue>,
) {
    if (segment.startWeek < 1 || segment.endWeek < segment.startWeek || segment.endWeek > maxWeek) {
        issues += issue(
            TimeTableValidationCode.INVALID_RECURRENCE_RANGE,
            path,
            "recurrence weeks must satisfy 1 <= startWeek <= endWeek <= maxWeek",
        )
    } else if ((segment.startWeek..segment.endWeek).none { segment.weekPattern.matches(it) }) {
        issues += issue(
            TimeTableValidationCode.INVALID_RECURRENCE_PATTERN,
            path,
            "recurrence pattern does not select any week in its range",
        )
    }
    // WeekPattern is an enum, so an in-memory value cannot be outside the supported set.
    if (segment.weekPattern !in WeekPattern.entries) {
        issues += issue(
            TimeTableValidationCode.INVALID_RECURRENCE_PATTERN,
            path,
            "recurrence pattern is unsupported",
        )
    }
}

private fun validateDay(
    dayOfWeek: Int,
    path: String,
    issues: MutableList<TimeTableValidationIssue>,
) {
    if (dayOfWeek !in 1..7) {
        issues += issue(
            TimeTableValidationCode.INVALID_DAY_OF_WEEK,
            path,
            "dayOfWeek must be between 1 and 7",
        )
    }
}

private fun validateNodeRange(
    startNode: Int,
    nodeCount: Int,
    nodes: List<TimeTableNode>,
    requireExistingSpan: Boolean,
    path: String,
    issues: MutableList<TimeTableValidationIssue>,
) {
    val endNode = startNode.toLong() + nodeCount.toLong() - 1L
    val hasPositiveBounds = startNode >= 1 && nodeCount >= 1
    val hasValidBounds = hasPositiveBounds && endNode <= nodes.size.toLong()
    val spanExists = hasPositiveBounds && (!requireExistingSpan || (
        hasValidBounds &&
            (startNode.toLong()..endNode).all { number ->
                nodes.any { it.node.toLong() == number }
            }
        ))
    if (!spanExists) {
        issues += issue(
            TimeTableValidationCode.INVALID_COURSE_NODE_RANGE,
            path,
            "course node range must fit within nodes 1..${nodes.size}",
            node = startNode,
        )
    }
}

private fun validateCustomTime(
    range: MinuteRange?,
    path: String,
    issues: MutableList<TimeTableValidationIssue>,
) {
    if (range != null && !range.isValid) {
        issues += issue(
            TimeTableValidationCode.INVALID_CUSTOM_TIME,
            path,
            "custom time must be a positive interval between 00:00 and 24:00",
        )
    }
}

private fun registerId(
    id: String,
    path: String,
    ids: MutableMap<String, String>,
    issues: MutableList<TimeTableValidationIssue>,
    globalIds: MutableMap<String, String>? = null,
) {
    if (id.isBlank()) {
        issues += issue(TimeTableValidationCode.INVALID_ID, path, "id must not be blank")
        return
    }
    val previousPath = ids[id] ?: globalIds?.get(id)
    if (previousPath != null) {
        issues += issue(
            TimeTableValidationCode.DUPLICATE_ID,
            path,
            "id duplicates $previousPath",
        )
    } else {
        ids[id] = path
        globalIds?.put(id, path)
    }
}

private fun issue(
    code: TimeTableValidationCode,
    path: String,
    message: String,
    node: Int = 0,
    otherNode: Int? = null,
): TimeTableValidationIssue = TimeTableValidationIssue(
    code = code,
    node = node,
    otherNode = otherNode,
    message = message,
    path = path,
)

private fun sortIssues(issues: List<TimeTableValidationIssue>): List<TimeTableValidationIssue> =
    issues.sortedWith(
        compareBy<TimeTableValidationIssue> { it.path }
            .thenBy { it.node }
            .thenBy { it.otherNode ?: Int.MIN_VALUE }
            .thenBy { it.code },
    )

private fun DateException.hasTargetValues(): Boolean =
    targetEpochDay != null ||
        targetDayOfWeek != null ||
        targetStartNode != null ||
        targetNodeCount != null ||
        targetCustomTime != null ||
        targetTeacher != null ||
        targetRoom != null
