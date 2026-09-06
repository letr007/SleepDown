package com.letr.sleepdown.domain

/** Builds UI-independent data for the three shared widget contracts. */
object WidgetSnapshotBuilder {
    fun buildAll(
        timetable: Timetable,
        nowEpochDay: Long,
        nowMinuteOfDay: Int,
    ): WidgetSnapshots = WidgetSnapshots(
        next = build(WidgetSnapshotKind.NEXT, timetable, nowEpochDay, nowMinuteOfDay),
        today = build(WidgetSnapshotKind.TODAY, timetable, nowEpochDay, nowMinuteOfDay),
        week = build(WidgetSnapshotKind.WEEK, timetable, nowEpochDay, nowMinuteOfDay),
    )

    fun build(
        kind: WidgetSnapshotKind,
        timetable: Timetable,
        nowEpochDay: Long,
        nowMinuteOfDay: Int,
    ): WidgetSnapshot {
        val safeNowMinute = nowMinuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        val range = when (kind) {
            WidgetSnapshotKind.NEXT -> timetable.coverageRange
            WidgetSnapshotKind.TODAY -> EpochDayRange(nowEpochDay, nowEpochDay)
            WidgetSnapshotKind.WEEK -> {
                val weekStart = startOfIsoWeek(nowEpochDay)
                EpochDayRange(weekStart, weekStart + 6L)
            }
        }
        val occurrences = TimetableEngine.expandOccurrences(timetable, range)
        val groups = TimetableEngine.conflictGroups(occurrences, timetable.conflictPreferences)
        val groupByOccurrence = groups
            .flatMap { group -> group.occurrences.map { it.id to group } }
            .toMap()
        val items = when (kind) {
            WidgetSnapshotKind.NEXT -> listOfNotNull(
                chooseNextOccurrence(
                    occurrences = occurrences,
                    groups = groups,
                    nowEpochDay = nowEpochDay,
                    nowMinuteOfDay = safeNowMinute,
                ),
            )
            WidgetSnapshotKind.TODAY -> occurrences.sortedWith(occurrenceDisplayComparator)
            WidgetSnapshotKind.WEEK -> occurrences.sortedWith(occurrenceDisplayComparator)
        }.map { occurrence ->
            val group = groupByOccurrence[occurrence.id]
            WidgetSnapshotItem(
                occurrenceId = occurrence.id,
                courseId = occurrence.courseId,
                courseName = occurrence.courseName,
                epochDay = occurrence.epochDay,
                startMinuteOfDay = occurrence.startMinuteOfDay,
                endMinuteOfDay = occurrence.endMinuteOfDay,
                teacher = occurrence.teacher,
                room = occurrence.room,
                note = occurrence.note,
                color = occurrence.color,
                isCurrent = occurrence.epochDay == nowEpochDay &&
                    occurrence.startMinuteOfDay <= safeNowMinute &&
                    safeNowMinute < occurrence.endMinuteOfDay,
                conflictCount = group?.occurrences?.size ?: 1,
                isPreferred = group?.preferredOccurrenceId == occurrence.id,
            )
        }
        return WidgetSnapshot(
            kind = kind,
            timetableId = timetable.id,
            anchorEpochDay = when (kind) {
                WidgetSnapshotKind.NEXT,
                WidgetSnapshotKind.TODAY,
                -> nowEpochDay
                WidgetSnapshotKind.WEEK -> range.startEpochDay
            },
            generatedAtMinuteOfDay = safeNowMinute,
            items = items,
        )
    }

    fun buildNext(timetable: Timetable, nowEpochDay: Long, nowMinuteOfDay: Int): WidgetSnapshot =
        build(WidgetSnapshotKind.NEXT, timetable, nowEpochDay, nowMinuteOfDay)

    fun buildToday(timetable: Timetable, nowEpochDay: Long, nowMinuteOfDay: Int): WidgetSnapshot =
        build(WidgetSnapshotKind.TODAY, timetable, nowEpochDay, nowMinuteOfDay)

    fun buildWeek(timetable: Timetable, nowEpochDay: Long, nowMinuteOfDay: Int): WidgetSnapshot =
        build(WidgetSnapshotKind.WEEK, timetable, nowEpochDay, nowMinuteOfDay)
}

fun buildWidgetSnapshots(
    timetable: Timetable,
    nowEpochDay: Long,
    nowMinuteOfDay: Int,
): WidgetSnapshots = WidgetSnapshotBuilder.buildAll(timetable, nowEpochDay, nowMinuteOfDay)

private fun isCurrentOrFuture(
    occurrence: CourseOccurrence,
    nowEpochDay: Long,
    nowMinuteOfDay: Int,
): Boolean = occurrence.epochDay > nowEpochDay ||
    (occurrence.epochDay == nowEpochDay && occurrence.endMinuteOfDay > nowMinuteOfDay)

private fun chooseNextOccurrence(
    occurrences: List<CourseOccurrence>,
    groups: List<ConflictGroup>,
    nowEpochDay: Long,
    nowMinuteOfDay: Int,
): CourseOccurrence? {
    val candidates = occurrences
        .filter { isCurrentOrFuture(it, nowEpochDay, nowMinuteOfDay) }
        .sortedWith(nextOccurrenceComparator(nowEpochDay, nowMinuteOfDay))
    val first = candidates.firstOrNull() ?: return null
    val group = groups.firstOrNull { it.occurrences.any { occurrence -> occurrence.id == first.id } }
    return group?.preferredOccurrence?.takeIf { preferred ->
        candidates.any { it.id == preferred.id }
    } ?: first
}

private fun nextOccurrenceComparator(
    nowEpochDay: Long,
    nowMinuteOfDay: Int,
): Comparator<CourseOccurrence> = compareBy<CourseOccurrence> {
    if (it.epochDay == nowEpochDay &&
        it.startMinuteOfDay <= nowMinuteOfDay &&
        nowMinuteOfDay < it.endMinuteOfDay
    ) {
        0
    } else {
        1
    }
}.then(occurrenceDisplayComparator)

private val occurrenceDisplayComparator = compareBy<CourseOccurrence> {
    it.epochDay
}.thenBy { it.startMinuteOfDay }
    .thenBy { it.endMinuteOfDay }
    .thenBy { it.courseId }
    .thenBy { it.logicalSlotId }
    .thenBy { it.recurrenceSegmentId }
    .thenBy { it.id }

private fun startOfIsoWeek(epochDay: Long): Long =
    epochDay - (isoDayOfWeek(epochDay) - 1L)
