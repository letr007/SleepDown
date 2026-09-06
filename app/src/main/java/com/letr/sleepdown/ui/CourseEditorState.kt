package com.letr.sleepdown.ui

import com.letr.sleepdown.data.CourseEntity
import com.letr.sleepdown.data.CourseTimeEntity
import com.letr.sleepdown.logic.Weeks
import java.util.UUID

internal data class TimeDraft(
    val id: Long = 0L,
    val day: Int = 1,
    val startNode: Int = 1,
    val step: Int = 2,
    val selectedWeeks: Set<Int> = emptySet(),
    val room: String = "",
    val teacher: String = "",
    val logicalSlotId: String = UUID.randomUUID().toString(),
    val recurrenceSegmentId: String = UUID.randomUUID().toString(),
    val ownTime: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val sourceRows: List<CourseTimeEntity> = emptyList(),
)

internal fun defaultTimeDraft(maxWeek: Int, day: Int, startNode: Int, step: Int = 2, maxNode: Int = 60): TimeDraft {
    val start = startNode.coerceIn(1, maxNode.coerceAtLeast(1))
    return TimeDraft(
        day = day.coerceIn(1, 7),
        startNode = start,
        step = step.coerceIn(1, maxNode.coerceAtLeast(1) - start + 1),
        selectedWeeks = (1..maxWeek.coerceAtLeast(1)).toSet(),
    )
}

private fun CourseTimeEntity.weeks(): Set<Int> = (startWeek..endWeek)
    .filter { Weeks.inWeek(startWeek, endWeek, weekType, it) }.toSet()

internal fun courseTimeDrafts(course: CourseEntity, rows: List<CourseTimeEntity>): List<TimeDraft> {
    // Recurrence rows share an editor block only when their effective time and location agree.
    return rows.groupBy { row ->
        val legacy = row.logicalSlotId.isBlank() || row.logicalSlotId == "legacy"
        row.copy(
            id = 0L,
            startWeek = 0,
            endWeek = 0,
            weekType = 0,
            teacher = if (legacy && row.recurrenceSegmentId == "legacy") row.teacher.ifBlank { course.teacher } else row.teacher,
            recurrenceSegmentId = "",
        )
    }.values.map { source ->
        val first = source.first()
        val legacy = first.logicalSlotId.isBlank() || first.logicalSlotId == "legacy"
        val selected = source.flatMap { it.weeks() }.toSet()
        TimeDraft(
            id = first.id,
            day = first.day,
            startNode = first.startNode,
            step = first.step,
            selectedWeeks = selected,
            room = first.room,
            teacher = if (legacy && first.recurrenceSegmentId == "legacy") first.teacher.ifBlank { course.teacher } else first.teacher,
            logicalSlotId = if (legacy) UUID.randomUUID().toString() else first.logicalSlotId,
            recurrenceSegmentId = first.recurrenceSegmentId.takeUnless { it.isBlank() || it == "legacy" } ?: UUID.randomUUID().toString(),
            ownTime = first.ownTime,
            startTime = first.startTime,
            endTime = first.endTime,
            sourceRows = source,
        )
    }
}

private fun weekPatternFor(weeks: Set<Int>): Int = when {
    weeks.isNotEmpty() && weeks.all { it % 2 == 1 } -> CourseTimeEntity.TYPE_ODD
    weeks.isNotEmpty() && weeks.all { it % 2 == 0 } -> CourseTimeEntity.TYPE_EVEN
    else -> CourseTimeEntity.TYPE_ALL
}

internal fun TimeDraft.toEntities(courseId: Long): List<CourseTimeEntity> {
    val weeks = selectedWeeks.filter { it > 0 }.sorted()
    if (weeks.isEmpty()) return emptyList()
    fun update(row: CourseTimeEntity) = row.copy(
        courseId = courseId,
        day = day,
        startNode = startNode,
        step = step,
        room = room.trim(),
        teacher = teacher.trim(),
        ownTime = ownTime,
        startTime = if (ownTime) startTime else "",
        endTime = if (ownTime) endTime else "",
        logicalSlotId = logicalSlotId,
    )
    if (sourceRows.isNotEmpty() && weeks.toSet() == sourceRows.flatMap { it.weeks() }.toSet()) {
        return sourceRows.map(::update)
    }
    // The selected weeks are authoritative; a preset must never filter subsequent manual selections.
    val pattern = weekPatternFor(weeks.toSet())
    val stride = if (pattern == CourseTimeEntity.TYPE_ALL) 1 else 2
    val ranges = mutableListOf<IntRange>()
    var first = weeks.first()
    var last = first
    for (week in weeks.drop(1)) {
        if (week != last + stride) {
            ranges += first..last
            first = week
        }
        last = week
    }
    ranges += first..last
    val usedSegments = mutableSetOf<String>()
    return ranges.mapIndexed { index, range ->
        val previous = sourceRows.firstOrNull {
            it.startWeek == range.first && it.endWeek == range.last && it.weekType == pattern && it.recurrenceSegmentId !in usedSegments
        } ?: sourceRows.firstOrNull { range.first in it.weeks() && it.recurrenceSegmentId !in usedSegments }
        val segmentId = previous?.recurrenceSegmentId
            ?: if (index == 0 && recurrenceSegmentId !in usedSegments) recurrenceSegmentId else UUID.randomUUID().toString()
        usedSegments += segmentId
        update(CourseTimeEntity(
            id = previous?.id ?: 0L,
            courseId = courseId,
            day = day,
            startNode = startNode,
            step = step,
            startWeek = range.first,
            endWeek = range.last,
            weekType = pattern,
            recurrenceSegmentId = segmentId,
        ))
    }
}

internal fun validCourseCredit(value: String): Boolean = value.isBlank() ||
    value.toFloatOrNull()?.let { it.isFinite() && it >= 0f } == true

internal data class CourseEditorSnapshot(
    val name: String,
    val color: Int,
    val credit: String,
    val note: String,
    val times: List<TimeDraft>,
)
