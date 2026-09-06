package com.letr.sleepdown.domain

enum class TimetableCommandScope {
    LOGICAL_SLOT,
    RECURRENCE_SEGMENT,
    OCCURRENCE,
    COURSE,
}

class TimetableCommandException(message: String) : IllegalArgumentException(message)

private data class CommandMutation(
    val timetable: Timetable,
    val segmentIdMapping: Map<String, String?> = emptyMap(),
)

/**
 * Commands make the user's intended mutation scope explicit. An occurrence
 * mutation is represented as a dated exception; source recurrence is unchanged.
 */
sealed interface TimetableCommand {
    val scope: TimetableCommandScope

    data class MoveLogicalSlot(
        val logicalSlotId: String,
        val dayDelta: Int = 0,
        val startNodeDelta: Int = 0,
        val minuteDelta: Int = 0,
        val targetDayOfWeek: Int? = null,
        val targetStartNode: Int? = null,
        val targetNodeCount: Int? = null,
        val targetCustomTime: MinuteRange? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.LOGICAL_SLOT
    }

    data class MoveRecurrenceSegment(
        val logicalSlotId: String,
        val recurrenceSegmentId: String,
        val dayDelta: Int = 0,
        val startNodeDelta: Int = 0,
        val minuteDelta: Int = 0,
        val targetDayOfWeek: Int? = null,
        val targetStartNode: Int? = null,
        val targetNodeCount: Int? = null,
        val targetCustomTime: MinuteRange? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.RECURRENCE_SEGMENT
    }

    data class MoveOccurrence(
        val logicalSlotId: String,
        val originalEpochDay: Long,
        val targetEpochDay: Long,
        val recurrenceSegmentId: String? = null,
        val targetDayOfWeek: Int? = null,
        val targetStartNode: Int? = null,
        val targetNodeCount: Int? = null,
        val targetCustomTime: MinuteRange? = null,
        val targetTeacher: String? = null,
        val targetRoom: String? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.OCCURRENCE
    }

    data class MoveLogicalSlotFromWeek(
        val logicalSlotId: String,
        val fromWeek: Int,
        val dayDelta: Int = 0,
        val startNodeDelta: Int = 0,
        val minuteDelta: Int = 0,
        val targetDayOfWeek: Int? = null,
        val targetStartNode: Int? = null,
        val targetNodeCount: Int? = null,
        val targetCustomTime: MinuteRange? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.LOGICAL_SLOT
    }

    data class MoveRecurrenceSegmentFromWeek(
        val logicalSlotId: String,
        val recurrenceSegmentId: String,
        val fromWeek: Int,
        val dayDelta: Int = 0,
        val startNodeDelta: Int = 0,
        val minuteDelta: Int = 0,
        val targetDayOfWeek: Int? = null,
        val targetStartNode: Int? = null,
        val targetNodeCount: Int? = null,
        val targetCustomTime: MinuteRange? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.RECURRENCE_SEGMENT
    }

    data class DeleteLogicalSlot(
        val logicalSlotId: String,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.LOGICAL_SLOT
    }

    data class DeleteRecurrenceSegment(
        val logicalSlotId: String,
        val recurrenceSegmentId: String,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.RECURRENCE_SEGMENT
    }

    data class DeleteLogicalSlotFromWeek(
        val logicalSlotId: String,
        val fromWeek: Int,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.LOGICAL_SLOT
    }

    data class DeleteRecurrenceSegmentFromWeek(
        val logicalSlotId: String,
        val recurrenceSegmentId: String,
        val fromWeek: Int,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.RECURRENCE_SEGMENT
    }

    data class DeleteOccurrence(
        val logicalSlotId: String,
        val originalEpochDay: Long,
        val recurrenceSegmentId: String? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.OCCURRENCE
    }

    data class RescheduleOccurrence(
        val logicalSlotId: String,
        val originalEpochDay: Long,
        val targetEpochDay: Long,
        val recurrenceSegmentId: String? = null,
        val targetDayOfWeek: Int? = null,
        val targetStartNode: Int? = null,
        val targetNodeCount: Int? = null,
        val targetCustomTime: MinuteRange? = null,
        val targetTeacher: String? = null,
        val targetRoom: String? = null,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.OCCURRENCE
    }

    data class DeleteCourse(
        val courseId: String,
    ) : TimetableCommand {
        override val scope: TimetableCommandScope = TimetableCommandScope.COURSE
    }
}

object TimetableCommands {
    @Throws(Exception::class)
    fun apply(timetable: Timetable, command: TimetableCommand): Timetable =
        applyChecked(timetable, command).getOrThrow()

    fun applyChecked(timetable: Timetable, command: TimetableCommand): Result<Timetable> =
        runCatching { applyInternal(timetable, command) }

    private fun applyInternal(timetable: Timetable, command: TimetableCommand): Timetable {
        TimeTableValidator.requireValid(timetable, "Command input")
        val mutation = when (command) {
            is TimetableCommand.MoveLogicalSlot -> CommandMutation(
                timetable = moveLogicalSlot(timetable, command),
                segmentIdMapping = segmentIdsForSlot(timetable, command.logicalSlotId),
            )
            is TimetableCommand.MoveRecurrenceSegment -> CommandMutation(
                timetable = moveRecurrenceSegment(timetable, command),
                segmentIdMapping = mapOf(command.recurrenceSegmentId to command.recurrenceSegmentId),
            )
            is TimetableCommand.MoveOccurrence -> CommandMutation(
                timetable = rescheduleOccurrence(
                    timetable,
                    TimetableCommand.RescheduleOccurrence(
                        logicalSlotId = command.logicalSlotId,
                        originalEpochDay = command.originalEpochDay,
                        targetEpochDay = command.targetEpochDay,
                        recurrenceSegmentId = command.recurrenceSegmentId,
                        targetDayOfWeek = command.targetDayOfWeek,
                        targetStartNode = command.targetStartNode,
                        targetNodeCount = command.targetNodeCount,
                        targetCustomTime = command.targetCustomTime,
                        targetTeacher = command.targetTeacher,
                        targetRoom = command.targetRoom,
                    ),
                ),
            )
            is TimetableCommand.MoveLogicalSlotFromWeek -> moveLogicalSlotFromWeek(timetable, command)
            is TimetableCommand.MoveRecurrenceSegmentFromWeek -> moveRecurrenceSegmentFromWeek(timetable, command)
            is TimetableCommand.DeleteLogicalSlot -> CommandMutation(
                timetable = deleteLogicalSlot(timetable, command),
                segmentIdMapping = segmentIdsForSlot(timetable, command.logicalSlotId, remove = true),
            )
            is TimetableCommand.DeleteRecurrenceSegment -> CommandMutation(
                timetable = deleteRecurrenceSegment(timetable, command),
                segmentIdMapping = mapOf(command.recurrenceSegmentId to null),
            )
            is TimetableCommand.DeleteLogicalSlotFromWeek -> deleteLogicalSlotFromWeek(timetable, command)
            is TimetableCommand.DeleteRecurrenceSegmentFromWeek -> deleteRecurrenceSegmentFromWeek(timetable, command)
            is TimetableCommand.DeleteOccurrence -> {
                requireOccurrenceTarget(timetable, command.logicalSlotId, command.originalEpochDay, command.recurrenceSegmentId)
                CommandMutation(
                    timetable = addDateException(
                        timetable,
                        occurrenceException(
                            logicalSlotId = command.logicalSlotId,
                            originalEpochDay = command.originalEpochDay,
                            recurrenceSegmentId = command.recurrenceSegmentId,
                            type = DateExceptionType.CANCEL,
                        ),
                    ),
                )
            }
            is TimetableCommand.RescheduleOccurrence -> CommandMutation(
                timetable = rescheduleOccurrence(timetable, command),
            )
            is TimetableCommand.DeleteCourse -> CommandMutation(
                timetable = deleteCourse(timetable, command),
                segmentIdMapping = segmentIdsForCourse(timetable, command.courseId),
            )
        }
        val rewritten = rewriteOccurrencePreferences(
            before = timetable,
            after = mutation.timetable,
            dropMissing = command.isDeletion(),
            segmentIdMapping = mutation.segmentIdMapping,
        )
        return TimeTableValidator.requireValid(rewritten, "Command output")
    }

    fun toDateException(command: TimetableCommand): DateException? = when (command) {
        is TimetableCommand.DeleteOccurrence -> occurrenceException(
            logicalSlotId = command.logicalSlotId,
            originalEpochDay = command.originalEpochDay,
            recurrenceSegmentId = command.recurrenceSegmentId,
            type = DateExceptionType.CANCEL,
        )
        is TimetableCommand.RescheduleOccurrence -> occurrenceException(
            logicalSlotId = command.logicalSlotId,
            originalEpochDay = command.originalEpochDay,
            recurrenceSegmentId = command.recurrenceSegmentId,
            type = DateExceptionType.RESCHEDULE,
            targetEpochDay = command.targetEpochDay,
            targetDayOfWeek = command.targetDayOfWeek,
            targetStartNode = command.targetStartNode,
            targetNodeCount = command.targetNodeCount,
            targetCustomTime = command.targetCustomTime,
            targetTeacher = command.targetTeacher,
            targetRoom = command.targetRoom,
        )
        is TimetableCommand.MoveOccurrence -> toDateException(
            TimetableCommand.RescheduleOccurrence(
                logicalSlotId = command.logicalSlotId,
                originalEpochDay = command.originalEpochDay,
                targetEpochDay = command.targetEpochDay,
                recurrenceSegmentId = command.recurrenceSegmentId,
                targetDayOfWeek = command.targetDayOfWeek,
                targetStartNode = command.targetStartNode,
                targetNodeCount = command.targetNodeCount,
                targetCustomTime = command.targetCustomTime,
                targetTeacher = command.targetTeacher,
                targetRoom = command.targetRoom,
            ),
        )
        else -> null
    }
}

private fun rescheduleOccurrence(
    timetable: Timetable,
    command: TimetableCommand.RescheduleOccurrence,
): Timetable {
    val source = requireOccurrenceTarget(
        timetable,
        command.logicalSlotId,
        command.originalEpochDay,
        command.recurrenceSegmentId,
    )
    if (command.targetEpochDay !in timetable.coverageRange) {
        throw TimetableCommandException("targetEpochDay must be inside the timetable coverage range")
    }
    val targetDayOfWeek = command.targetDayOfWeek
        ?: timetableDayOfWeek(timetable, command.targetEpochDay)
    val targetStartNode = command.targetStartNode ?: source.startNode
    val targetNodeCount = command.targetNodeCount ?: source.nodeCount
    requireDayOfWeek(targetDayOfWeek, "targetDayOfWeek")
    requireNodeSpan(timetable, targetStartNode, targetNodeCount, "reschedule occurrence")
    command.targetCustomTime?.let { requireMinuteRange(it, "targetCustomTime") }
    return addDateException(
        timetable,
        occurrenceException(
            logicalSlotId = command.logicalSlotId,
            originalEpochDay = command.originalEpochDay,
            recurrenceSegmentId = command.recurrenceSegmentId,
            type = DateExceptionType.RESCHEDULE,
            targetEpochDay = command.targetEpochDay,
            targetDayOfWeek = targetDayOfWeek,
            targetStartNode = targetStartNode,
            targetNodeCount = targetNodeCount,
            targetCustomTime = command.targetCustomTime,
            targetTeacher = command.targetTeacher,
            targetRoom = command.targetRoom,
        ),
    )
}

@Throws(Exception::class)
fun applyTimetableCommand(
    timetable: Timetable,
    command: TimetableCommand,
): Timetable = TimetableCommands.apply(timetable, command)

fun applyTimetableCommandChecked(
    timetable: Timetable,
    command: TimetableCommand,
): Result<Timetable> = TimetableCommands.applyChecked(timetable, command)

private fun moveLogicalSlot(
    timetable: Timetable,
    command: TimetableCommand.MoveLogicalSlot,
): Timetable {
    val target = findUniqueSlot(timetable, command.logicalSlotId)
    val newDay = command.targetDayOfWeek ?: normalizedDay(target.slot.dayOfWeek + command.dayDelta)
    val newStartNode = command.targetStartNode ?: target.slot.startNode + command.startNodeDelta
    val newNodeCount = command.targetNodeCount ?: target.slot.nodeCount
    requireDayOfWeek(newDay, "targetDayOfWeek")
    requireNodeSpan(timetable, newStartNode, newNodeCount, "move logical slot")
    command.targetCustomTime?.let { requireMinuteRange(it, "targetCustomTime") }
    val dayShift = signedDayDelta(target.slot.dayOfWeek, newDay)
    val nodeShift = newStartNode - target.slot.startNode
    val changedSlot = target.slot.copy(
        dayOfWeek = newDay,
        startNode = newStartNode,
        nodeCount = newNodeCount,
        customTime = command.targetCustomTime ?: shiftMinuteRange(target.slot.customTime, command.minuteDelta),
        recurrenceSegments = target.slot.recurrenceSegments.map { segment ->
            val changedSegment = segment.copy(
                dayOfWeek = segment.dayOfWeek?.let { normalizedDay(it + dayShift) },
                startNode = segment.startNode?.plus(nodeShift),
                nodeCount = segment.nodeCount?.let { it + (newNodeCount - target.slot.nodeCount) },
                customTime = shiftMinuteRange(segment.customTime, command.minuteDelta),
            )
            requireDayOfWeek(changedSegment.dayOfWeek ?: newDay, "recurrence segment dayOfWeek")
            requireNodeSpan(
                timetable,
                changedSegment.startNode ?: newStartNode,
                changedSegment.nodeCount ?: newNodeCount,
                "move logical slot recurrence segment",
            )
            changedSegment
        },
    )
    val moved = replaceSlot(timetable, target, changedSlot)
    return updateSegmentReferences(
        before = timetable,
        after = moved,
        slotId = target.slot.id,
        fromWeek = 1,
        futureIds = target.slot.recurrenceSegments.associate { it.id to it.id },
        affectedSegmentIds = target.slot.recurrenceSegments.mapTo(mutableSetOf()) { it.id },
        dropFutureExceptions = false,
    )
}

private fun moveRecurrenceSegment(
    timetable: Timetable,
    command: TimetableCommand.MoveRecurrenceSegment,
): Timetable {
    val target = findUniqueSegment(timetable, command.logicalSlotId, command.recurrenceSegmentId)
    val changed = target.segment.copy(
        dayOfWeek = moveDay(target.segment.dayOfWeek ?: target.slot.dayOfWeek, command.dayDelta, command.targetDayOfWeek),
        startNode = moveStart(target.segment.startNode ?: target.slot.startNode, command.startNodeDelta, command.targetStartNode),
        nodeCount = command.targetNodeCount ?: target.segment.nodeCount,
        customTime = movedCustomTime(target.slot, target.segment, command.minuteDelta, command.targetCustomTime),
    ).let { segment ->
        segment.copy(
            dayOfWeek = if (command.targetDayOfWeek != null || command.dayDelta != 0) segment.dayOfWeek else target.segment.dayOfWeek,
            startNode = if (command.targetStartNode != null || command.startNodeDelta != 0) segment.startNode else target.segment.startNode,
            nodeCount = if (command.targetNodeCount != null) segment.nodeCount else target.segment.nodeCount,
        )
    }
    requireDayOfWeek(changed.dayOfWeek ?: target.slot.dayOfWeek, "recurrence segment dayOfWeek")
    requireNodeSpan(
        timetable,
        changed.startNode ?: target.slot.startNode,
        changed.nodeCount ?: target.slot.nodeCount,
        "move recurrence segment",
    )
    changed.customTime?.let { requireMinuteRange(it, "recurrence segment customTime") }
    val moved = replaceSegment(timetable, target, changed)
    return updateSegmentReferences(
        before = timetable,
        after = moved,
        slotId = target.slot.id,
        fromWeek = 1,
        futureIds = mapOf(target.segment.id to target.segment.id),
        affectedSegmentIds = setOf(target.segment.id),
        dropFutureExceptions = false,
    )
}

private fun moveLogicalSlotFromWeek(
    timetable: Timetable,
    command: TimetableCommand.MoveLogicalSlotFromWeek,
): CommandMutation {
    requireFromWeek(timetable, command.fromWeek)
    val target = findUniqueSlot(timetable, command.logicalSlotId)
    val mutation = mutateSegmentsFromWeek(target.slot.recurrenceSegments, command.fromWeek) { segment ->
        moveSegmentValues(
            slot = target.slot,
            segment = segment,
            dayDelta = command.dayDelta,
            startNodeDelta = command.startNodeDelta,
            minuteDelta = command.minuteDelta,
            targetDayOfWeek = command.targetDayOfWeek,
            targetStartNode = command.targetStartNode,
            targetNodeCount = command.targetNodeCount,
            targetCustomTime = command.targetCustomTime,
            timeTable = timetable.timeTable,
        )
    }
    val moved = replaceSlot(timetable, target, target.slot.copy(recurrenceSegments = mutation.segments))
    return CommandMutation(
        timetable = updateSegmentReferences(
            before = timetable,
            after = moved,
            slotId = target.slot.id,
            fromWeek = command.fromWeek,
            futureIds = mutation.futureIds,
            affectedSegmentIds = target.slot.recurrenceSegments.mapTo(mutableSetOf()) { it.id },
            dropFutureExceptions = false,
        ),
        segmentIdMapping = mutation.futureIds,
    )
}

private fun moveRecurrenceSegmentFromWeek(
    timetable: Timetable,
    command: TimetableCommand.MoveRecurrenceSegmentFromWeek,
): CommandMutation {
    requireFromWeek(timetable, command.fromWeek)
    val target = findUniqueSegment(timetable, command.logicalSlotId, command.recurrenceSegmentId)
    val mutation = mutateSegmentsFromWeek(listOf(target.segment), command.fromWeek) { segment ->
        moveSegmentValues(
            slot = target.slot,
            segment = segment,
            dayDelta = command.dayDelta,
            startNodeDelta = command.startNodeDelta,
            minuteDelta = command.minuteDelta,
            targetDayOfWeek = command.targetDayOfWeek,
            targetStartNode = command.targetStartNode,
            targetNodeCount = command.targetNodeCount,
            targetCustomTime = command.targetCustomTime,
            timeTable = timetable.timeTable,
        )
    }
    val changedSlot = target.slot.copy(
        recurrenceSegments = target.slot.recurrenceSegments.flatMap { segment ->
            if (segment.id == target.segment.id) mutation.segments else listOf(segment)
        },
    )
    val moved = replaceSlot(timetable, target.slotRef, changedSlot)
    return CommandMutation(
        timetable = updateSegmentReferences(
            before = timetable,
            after = moved,
            slotId = target.slot.id,
            fromWeek = command.fromWeek,
            futureIds = mutation.futureIds,
            affectedSegmentIds = setOf(target.segment.id),
            dropFutureExceptions = false,
        ),
        segmentIdMapping = mutation.futureIds,
    )
}

private fun deleteLogicalSlot(
    timetable: Timetable,
    command: TimetableCommand.DeleteLogicalSlot,
): Timetable {
    val target = findUniqueSlot(timetable, command.logicalSlotId)
    val slotIds = setOf(target.slot.id)
    val occurrenceIds = occurrenceIdsForSlots(timetable, slotIds)
    return timetable.copy(
        courses = timetable.courses.map { course ->
            if (course.id == target.course.id) {
                course.copy(slots = course.slots.filterNot { it.id == target.slot.id })
            } else {
                course
            }
        },
        dateExceptions = timetable.dateExceptions.filterNot { it.logicalSlotId in slotIds },
        conflictPreferences = timetable.conflictPreferences.filterNot {
            it.logicalSlotId in slotIds || it.occurrenceId in occurrenceIds
        },
    )
}

private fun deleteRecurrenceSegment(
    timetable: Timetable,
    command: TimetableCommand.DeleteRecurrenceSegment,
): Timetable {
    val target = findUniqueSegment(timetable, command.logicalSlotId, command.recurrenceSegmentId)
    val recurringOccurrences = TimetableEngine.recurringOccurrences(timetable, timetable.coverageRange)
        .filter { it.logicalSlotId == target.slot.id && it.recurrenceSegmentId == target.segment.id }
    val occurrenceIds = TimetableEngine.expandOccurrences(timetable)
        .filter { it.logicalSlotId == target.slot.id && it.recurrenceSegmentId == target.segment.id }
        .mapTo(mutableSetOf()) { it.id }
    val sourceEpochDays = recurringOccurrences.mapTo(mutableSetOf()) { it.sourceEpochDay }
    return replaceSlot(
        timetable.copy(
            dateExceptions = timetable.dateExceptions.filterNot { exception ->
                exception.logicalSlotId == target.slot.id &&
                    (exception.recurrenceSegmentId == target.segment.id ||
                        (exception.recurrenceSegmentId == null && exception.originalEpochDay in sourceEpochDays))
            },
            conflictPreferences = timetable.conflictPreferences.filterNot {
                it.occurrenceId in occurrenceIds
            },
        ),
        target.slotRef,
        target.slot.copy(recurrenceSegments = target.slot.recurrenceSegments.filterNot { it.id == target.segment.id }),
    )
}

private fun deleteLogicalSlotFromWeek(
    timetable: Timetable,
    command: TimetableCommand.DeleteLogicalSlotFromWeek,
): CommandMutation {
    requireFromWeek(timetable, command.fromWeek)
    val target = findUniqueSlot(timetable, command.logicalSlotId)
    val mutation = mutateSegmentsFromWeek(target.slot.recurrenceSegments, command.fromWeek) { null }
    val changedSlot = target.slot.copy(recurrenceSegments = mutation.segments)
    val changed = replaceSlot(timetable, target, changedSlot)
    return CommandMutation(
        timetable = updateSegmentReferences(
            before = timetable,
            after = changed,
            slotId = target.slot.id,
            fromWeek = command.fromWeek,
            futureIds = mutation.futureIds,
            affectedSegmentIds = target.slot.recurrenceSegments.mapTo(mutableSetOf()) { it.id },
            dropFutureExceptions = true,
        ),
        segmentIdMapping = mutation.futureIds,
    )
}

private fun deleteRecurrenceSegmentFromWeek(
    timetable: Timetable,
    command: TimetableCommand.DeleteRecurrenceSegmentFromWeek,
): CommandMutation {
    requireFromWeek(timetable, command.fromWeek)
    val target = findUniqueSegment(timetable, command.logicalSlotId, command.recurrenceSegmentId)
    val mutation = mutateSegmentsFromWeek(listOf(target.segment), command.fromWeek) { null }
    val changedSlot = target.slot.copy(
        recurrenceSegments = target.slot.recurrenceSegments.flatMap { segment ->
            if (segment.id == target.segment.id) mutation.segments else listOf(segment)
        },
    )
    val changed = replaceSlot(timetable, target.slotRef, changedSlot)
    return CommandMutation(
        timetable = updateSegmentReferences(
            before = timetable,
            after = changed,
            slotId = target.slot.id,
            fromWeek = command.fromWeek,
            futureIds = mutation.futureIds,
            affectedSegmentIds = setOf(target.segment.id),
            dropFutureExceptions = true,
        ),
        segmentIdMapping = mutation.futureIds,
    )
}

private fun deleteCourse(
    timetable: Timetable,
    command: TimetableCommand.DeleteCourse,
): Timetable {
    val course = findUniqueCourse(timetable, command.courseId)
    val slotIds = course.slots.mapTo(mutableSetOf()) { it.id }
    val occurrenceIds = occurrenceIdsForSlots(timetable, slotIds)
    return timetable.copy(
        courses = timetable.courses.filterNot { it.id == course.id },
        dateExceptions = timetable.dateExceptions.filterNot { it.logicalSlotId in slotIds },
        conflictPreferences = timetable.conflictPreferences.filterNot {
            it.courseId == course.id || it.logicalSlotId in slotIds || it.occurrenceId in occurrenceIds
        },
    )
}

private fun addDateException(
    timetable: Timetable,
    exception: DateException,
): Timetable {
    val sameSource = timetable.dateExceptions.filterNot {
        it.logicalSlotId == exception.logicalSlotId &&
            it.originalEpochDay == exception.originalEpochDay &&
            it.recurrenceSegmentId == exception.recurrenceSegmentId
    }
    return timetable.copy(dateExceptions = sameSource + exception)
}

private fun occurrenceException(
    logicalSlotId: String,
    originalEpochDay: Long,
    recurrenceSegmentId: String?,
    type: DateExceptionType,
    targetEpochDay: Long? = null,
    targetDayOfWeek: Int? = null,
    targetStartNode: Int? = null,
    targetNodeCount: Int? = null,
    targetCustomTime: MinuteRange? = null,
    targetTeacher: String? = null,
    targetRoom: String? = null,
): DateException = DateException(
    id = lengthPrefixedId(
        "exception",
        type.name,
        logicalSlotId,
        originalEpochDay.toString(),
        recurrenceSegmentId ?: "",
        targetEpochDay?.toString() ?: "",
        targetDayOfWeek?.toString() ?: "",
        targetStartNode?.toString() ?: "",
        targetNodeCount?.toString() ?: "",
        targetCustomTime?.startMinuteOfDay?.toString() ?: "",
        targetCustomTime?.endMinuteOfDay?.toString() ?: "",
        targetTeacher ?: "",
        targetRoom ?: "",
    ),
    logicalSlotId = logicalSlotId,
    originalEpochDay = originalEpochDay,
    type = type,
    recurrenceSegmentId = recurrenceSegmentId,
    targetEpochDay = targetEpochDay,
    targetDayOfWeek = targetDayOfWeek,
    targetStartNode = targetStartNode,
    targetNodeCount = targetNodeCount,
    targetCustomTime = targetCustomTime,
    targetTeacher = targetTeacher,
    targetRoom = targetRoom,
)

private data class SlotRef(
    val course: Course,
    val slot: LogicalCourseSlot,
)

private data class SegmentRef(
    val slotRef: SlotRef,
    val slot: LogicalCourseSlot,
    val segment: RecurrenceSegment,
)

private fun findUniqueCourse(timetable: Timetable, courseId: String): Course {
    val matches = timetable.courses.filter { it.id == courseId }
    return when (matches.size) {
        1 -> matches.single()
        0 -> throw TimetableCommandException("courseId '$courseId' does not identify a course")
        else -> throw TimetableCommandException("courseId '$courseId' is ambiguous")
    }
}

private fun segmentIdsForSlot(
    timetable: Timetable,
    slotId: String,
    remove: Boolean = false,
): Map<String, String?> = findUniqueSlot(timetable, slotId).slot.recurrenceSegments.associate {
    it.id to if (remove) null else it.id
}

private fun segmentIdsForCourse(
    timetable: Timetable,
    courseId: String,
): Map<String, String?> = findUniqueCourse(timetable, courseId).slots
    .flatMap { it.recurrenceSegments }
    .associate { it.id to null }

private fun findUniqueSlot(timetable: Timetable, slotId: String): SlotRef {
    val matches = timetable.courses.flatMap { course ->
        course.slots.filter { it.id == slotId }.map { SlotRef(course, it) }
    }
    return when (matches.size) {
        1 -> matches.single()
        0 -> throw TimetableCommandException("logicalSlotId '$slotId' does not identify a slot")
        else -> throw TimetableCommandException("logicalSlotId '$slotId' is ambiguous")
    }
}

private fun findUniqueSegment(
    timetable: Timetable,
    slotId: String,
    segmentId: String,
): SegmentRef {
    val slotRef = findUniqueSlot(timetable, slotId)
    val matches = slotRef.slot.recurrenceSegments.filter { it.id == segmentId }
    return when (matches.size) {
        1 -> SegmentRef(slotRef, slotRef.slot, matches.single())
        0 -> throw TimetableCommandException("recurrenceSegmentId '$segmentId' does not identify a segment in slot '$slotId'")
        else -> throw TimetableCommandException("recurrenceSegmentId '$segmentId' is ambiguous in slot '$slotId'")
    }
}

private fun replaceSlot(
    timetable: Timetable,
    target: SlotRef,
    replacement: LogicalCourseSlot,
): Timetable = timetable.copy(
    courses = timetable.courses.map { course ->
        if (course.id == target.course.id) {
            course.copy(slots = course.slots.map { slot -> if (slot.id == target.slot.id) replacement else slot })
        } else {
            course
        }
    },
)

private fun replaceSegment(
    timetable: Timetable,
    target: SegmentRef,
    replacement: RecurrenceSegment,
): Timetable = replaceSlot(
    timetable,
    target.slotRef,
    target.slot.copy(
        recurrenceSegments = target.slot.recurrenceSegments.map { segment ->
            if (segment.id == target.segment.id) replacement else segment
        },
    ),
)

private fun requireOccurrenceTarget(
    timetable: Timetable,
    slotId: String,
    originalEpochDay: Long,
    segmentId: String?,
): CourseOccurrence {
    val matches = TimetableEngine.recurringOccurrences(
        timetable,
        EpochDayRange(originalEpochDay, originalEpochDay),
    ).filter {
        it.logicalSlotId == slotId &&
            it.sourceEpochDay == originalEpochDay &&
            (segmentId == null || it.recurrenceSegmentId == segmentId)
    }
    return when (matches.size) {
        1 -> matches.single()
        0 -> throw TimetableCommandException("no recurring occurrence matches slot '$slotId' on epochDay $originalEpochDay")
        else -> throw TimetableCommandException("occurrence target is ambiguous: ${matches.size} recurring occurrences match")
    }
}

private fun TimetableCommand.isDeletion(): Boolean = when (this) {
    is TimetableCommand.DeleteCourse,
    is TimetableCommand.DeleteLogicalSlot,
    is TimetableCommand.DeleteRecurrenceSegment,
    is TimetableCommand.DeleteLogicalSlotFromWeek,
    is TimetableCommand.DeleteRecurrenceSegmentFromWeek,
    is TimetableCommand.DeleteOccurrence,
    -> true
    else -> false
}

private data class SegmentMutationResult(
    val segments: List<RecurrenceSegment>,
    val futureIds: Map<String, String?>,
)

private fun RecurrenceSegment.hasMatchingWeek(): Boolean =
    (startWeek..endWeek).any { weekPattern.matches(it) }

private fun mutateSegmentsFromWeek(
    segments: List<RecurrenceSegment>,
    fromWeek: Int,
    transform: (RecurrenceSegment) -> RecurrenceSegment?,
): SegmentMutationResult {
    val output = mutableListOf<RecurrenceSegment>()
    val futureIds = linkedMapOf<String, String?>()
    segments.forEach { segment ->
        when {
            segment.endWeek < fromWeek -> {
                output += segment
                futureIds[segment.id] = segment.id
            }
            segment.startWeek >= fromWeek -> {
                val transformed = transform(segment)
                if (transformed != null) output += transformed
                futureIds[segment.id] = transformed?.id
            }
            else -> {
                val before = segment.copy(endWeek = fromWeek - 1)
                if (before.hasMatchingWeek()) output += before
                val afterId = lengthPrefixedId(segment.id, "from-week", fromWeek.toString())
                val transformed = transform(segment.copy(id = afterId, startWeek = fromWeek))
                if (transformed != null && transformed.hasMatchingWeek()) output += transformed
                futureIds[segment.id] = transformed?.takeIf { it.hasMatchingWeek() }?.id
            }
        }
    }
    return SegmentMutationResult(output, futureIds)
}

private data class SourceOccurrenceKey(
    val segmentId: String,
    val sourceEpochDay: Long,
)

private data class SourceOccurrenceReplacement(
    val segmentId: String,
    val sourceEpochDay: Long,
)

private fun updateSegmentReferences(
    before: Timetable,
    after: Timetable,
    slotId: String,
    fromWeek: Int,
    futureIds: Map<String, String?>,
    affectedSegmentIds: Set<String>,
    dropFutureExceptions: Boolean,
): Timetable {
    val replacements = buildSourceOccurrenceReplacements(
        before = before,
        after = after,
        slotId = slotId,
        fromWeek = fromWeek,
        futureIds = futureIds,
        affectedSegmentIds = affectedSegmentIds,
    )
    if (replacements.isEmpty()) return after

    val replacementsByDate = replacements.entries.groupBy { it.key.sourceEpochDay }
    val rewrittenExceptions = after.dateExceptions.mapNotNull { exception ->
        if (exception.logicalSlotId != slotId) {
            return@mapNotNull exception
        }

        val matching: List<Pair<SourceOccurrenceKey, SourceOccurrenceReplacement?>> = if (exception.recurrenceSegmentId == null) {
            replacementsByDate[exception.originalEpochDay].orEmpty().map { entry -> entry.key to entry.value }
        } else {
            val key = SourceOccurrenceKey(exception.recurrenceSegmentId, exception.originalEpochDay)
            if (replacements.containsKey(key)) listOf(key to replacements.getValue(key)) else emptyList()
        }
        if (matching.isEmpty()) {
            return@mapNotNull exception
        }
        if (matching.size > 1) {
            throw TimetableCommandException(
                "date exception source is ambiguous after changing slot '$slotId'",
            )
        }

        val replacement = matching.single().second
        if (replacement == null) {
            if (dropFutureExceptions) {
                null
            } else {
                throw TimetableCommandException(
                    "command moved an occurrence outside timetable coverage",
                )
            }
        } else {
            exception.copy(
                recurrenceSegmentId = if (exception.recurrenceSegmentId == null) {
                    null
                } else {
                    replacement.segmentId
                },
                originalEpochDay = replacement.sourceEpochDay,
            )
        }
    }
    return after.copy(dateExceptions = rewrittenExceptions)
}

private fun buildSourceOccurrenceReplacements(
    before: Timetable,
    after: Timetable,
    slotId: String,
    fromWeek: Int,
    futureIds: Map<String, String?>,
    affectedSegmentIds: Set<String>,
): Map<SourceOccurrenceKey, SourceOccurrenceReplacement?> {
    val oldOccurrences = TimetableEngine.recurringOccurrences(before)
        .filter {
            it.logicalSlotId == slotId &&
                it.recurrenceSegmentId in affectedSegmentIds &&
                it.week >= fromWeek
        }
    val newOccurrences = TimetableEngine.recurringOccurrences(after)
        .filter { it.logicalSlotId == slotId }

    return oldOccurrences.associate { old ->
        val newSegmentId = futureIds[old.recurrenceSegmentId]
        val candidates = if (newSegmentId == null) {
            emptyList()
        } else {
            newOccurrences.filter {
                it.recurrenceSegmentId == newSegmentId && it.week == old.week
            }
        }
        val replacement = when (candidates.size) {
            0 -> null
            1 -> SourceOccurrenceReplacement(
                segmentId = candidates.single().recurrenceSegmentId,
                sourceEpochDay = candidates.single().sourceEpochDay,
            )
            else -> throw TimetableCommandException(
                "command made slot '$slotId' occurrence source ambiguous in week ${old.week}",
            )
        }
        SourceOccurrenceKey(old.recurrenceSegmentId, old.sourceEpochDay) to replacement
    }
}

private fun moveSegmentValues(
    slot: LogicalCourseSlot,
    segment: RecurrenceSegment,
    dayDelta: Int,
    startNodeDelta: Int,
    minuteDelta: Int,
    targetDayOfWeek: Int?,
    targetStartNode: Int?,
    targetNodeCount: Int?,
    targetCustomTime: MinuteRange?,
    timeTable: ScheduleTimeTable,
): RecurrenceSegment {
    val effectiveDay = segment.dayOfWeek ?: slot.dayOfWeek
    val effectiveStart = segment.startNode ?: slot.startNode
    val effectiveNodeCount = segment.nodeCount ?: slot.nodeCount
    val moveDay = targetDayOfWeek != null || dayDelta != 0
    val moveStart = targetStartNode != null || startNodeDelta != 0
    val moveCount = targetNodeCount != null
    val resultDay = if (moveDay) targetDayOfWeek ?: normalizedDay(effectiveDay + dayDelta) else effectiveDay
    val resultStart = if (moveStart) targetStartNode ?: effectiveStart + startNodeDelta else effectiveStart
    val resultCount = if (moveCount) targetNodeCount ?: effectiveNodeCount else effectiveNodeCount
    requireDayOfWeek(resultDay, "recurrence segment dayOfWeek")
    requireNodeSpan(timeTable, resultStart, resultCount, "move recurrence segment from week")
    targetCustomTime?.let { requireMinuteRange(it, "recurrence segment customTime") }
    return segment.copy(
        dayOfWeek = if (moveDay) resultDay else segment.dayOfWeek,
        startNode = if (moveStart) resultStart else segment.startNode,
        nodeCount = if (moveCount) resultCount else segment.nodeCount,
        customTime = when {
            targetCustomTime != null -> targetCustomTime
            minuteDelta != 0 -> shiftMinuteRange(segment.customTime ?: slot.customTime, minuteDelta)
            else -> segment.customTime
        },
    )
}

private fun movedCustomTime(
    slot: LogicalCourseSlot,
    segment: RecurrenceSegment,
    minuteDelta: Int,
    targetCustomTime: MinuteRange?,
): MinuteRange? = when {
    targetCustomTime != null -> targetCustomTime
    minuteDelta != 0 -> shiftMinuteRange(segment.customTime ?: slot.customTime, minuteDelta)
    else -> segment.customTime
}

private fun moveDay(current: Int, delta: Int, target: Int?): Int =
    target ?: normalizedDay(current + delta)

private fun moveStart(current: Int, delta: Int, target: Int?): Int =
    target ?: current + delta

private fun shiftMinuteRange(range: MinuteRange?, deltaMinutes: Int): MinuteRange? =
    range?.let {
        MinuteRange(
            startMinuteOfDay = it.startMinuteOfDay + deltaMinutes,
            endMinuteOfDay = it.endMinuteOfDay + deltaMinutes,
        )
    }

private fun requireNodeSpan(
    timetable: Timetable,
    startNode: Int,
    nodeCount: Int,
    context: String,
) = requireNodeSpan(timetable.timeTable, startNode, nodeCount, context)

private fun requireNodeSpan(
    timeTable: ScheduleTimeTable,
    startNode: Int,
    nodeCount: Int,
    context: String,
) {
    if (startNode < 1 || nodeCount < 1 || timeTable.rangeForNodes(startNode, nodeCount) == null) {
        throw TimetableCommandException(
            "$context node span startNode=$startNode nodeCount=$nodeCount is outside the time table",
        )
    }
}

private fun requireDayOfWeek(dayOfWeek: Int, context: String) {
    if (dayOfWeek !in 1..7) {
        throw TimetableCommandException("$context must be between 1 and 7")
    }
}

private fun requireMinuteRange(range: MinuteRange, context: String) {
    if (!range.isValid) {
        throw TimetableCommandException("$context must be a valid positive minute range")
    }
}

private fun requireFromWeek(timetable: Timetable, fromWeek: Int) {
    if (fromWeek !in 1..timetable.maxWeek) {
        throw TimetableCommandException("fromWeek must be within 1..${timetable.maxWeek}")
    }
}

private fun normalizedDay(dayOfWeek: Int): Int {
    val zeroBased = (dayOfWeek - 1) % 7
    return if (zeroBased < 0) zeroBased + 8 else zeroBased + 1
}

private fun signedDayDelta(from: Int, to: Int): Int {
    val raw = to - from
    return when {
        raw > 3 -> raw - 7
        raw < -3 -> raw + 7
        else -> raw
    }
}

private fun weekOf(timetable: Timetable, epochDay: Long): Int {
    val relativeDay = epochDay - timetable.firstDayEpochDay
    return if (relativeDay < 0) 0 else floorDiv(relativeDay, 7L).toInt() + 1
}

private fun occurrenceIdsForSlots(
    timetable: Timetable,
    slotIds: Set<String>,
): Set<String> = TimetableEngine.expandOccurrences(timetable)
    .filter { it.logicalSlotId in slotIds }
    .mapTo(mutableSetOf()) { it.id }

private fun rewriteOccurrencePreferences(
    before: Timetable,
    after: Timetable,
    dropMissing: Boolean,
    segmentIdMapping: Map<String, String?>,
): Timetable {
    if (before.conflictPreferences.none { it.occurrenceId != null }) return after
    val oldOccurrences = TimetableEngine.expandOccurrences(before)
        .associateBy { it.id }
    val newOccurrences = TimetableEngine.expandOccurrences(after)
    val rewritten = after.conflictPreferences.mapNotNull { preference ->
        val occurrenceId = preference.occurrenceId ?: return@mapNotNull preference
        val old = oldOccurrences[occurrenceId]
            ?: throw TimetableCommandException("conflict preference references missing occurrence '$occurrenceId'")
        val unchanged = newOccurrences.firstOrNull { it.id == occurrenceId }
        if (unchanged != null) return@mapNotNull preference.copy(epochDay = unchanged.epochDay)
        val mappedSegmentId = if (old.recurrenceSegmentId in segmentIdMapping) {
            segmentIdMapping[old.recurrenceSegmentId]
        } else {
            old.recurrenceSegmentId
        }
        if (mappedSegmentId == null) {
            return@mapNotNull if (dropMissing) null else throw TimetableCommandException(
                "command removed occurrence '$occurrenceId' referenced by a conflict preference",
            )
        }
        val candidates = newOccurrences.filter { candidate ->
            candidate.courseId == old.courseId &&
                candidate.logicalSlotId == old.logicalSlotId &&
                candidate.recurrenceSegmentId == mappedSegmentId &&
                (candidate.week == old.week || candidate.sourceEpochDay == old.sourceEpochDay)
        }
        when (candidates.size) {
            1 -> candidates.single().let { candidate ->
                preference.copy(
                    occurrenceId = candidate.id,
                    epochDay = candidate.epochDay,
                )
            }
            0 -> if (dropMissing) null else throw TimetableCommandException(
                "command removed occurrence '$occurrenceId' referenced by a conflict preference",
            )
            else -> throw TimetableCommandException(
                "command made conflict preference occurrence '$occurrenceId' ambiguous",
            )
        }
    }
    return after.copy(conflictPreferences = rewritten)
}
