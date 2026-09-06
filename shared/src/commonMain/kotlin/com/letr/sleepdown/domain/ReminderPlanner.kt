package com.letr.sleepdown.domain

/** Builds platform-neutral notification intents; platform code owns actual scheduling. */
object ReminderPlanner {
    fun plan(
        occurrences: List<CourseOccurrence>,
        settings: ReminderSettings,
    ): List<ReminderPlan> = occurrences
        .asSequence()
        .filter { it.startMinuteOfDay < it.endMinuteOfDay }
        .flatMap { occurrence ->
            buildList {
                if (settings.startEnabled) {
                    add(
                        reminderPlan(
                            occurrence = occurrence,
                            kind = ReminderKind.LESSON_START,
                            eventMinuteOfDay = occurrence.startMinuteOfDay,
                            leadMinutes = settings.startLeadMinutes,
                            settings = settings,
                        ),
                    )
                }
                if (settings.endEnabled) {
                    add(
                        reminderPlan(
                            occurrence = occurrence,
                            kind = ReminderKind.LESSON_END,
                            eventMinuteOfDay = occurrence.endMinuteOfDay,
                            leadMinutes = settings.endLeadMinutes,
                            settings = settings,
                        ),
                    )
                }
            }
        }
        .sortedWith(
            compareBy<ReminderPlan> { it.triggerEpochDay }
                .thenBy { it.triggerMinuteOfDay }
                .thenBy { it.eventEpochDay }
                .thenBy { it.eventMinuteOfDay }
                .thenBy { it.kind }
                .thenBy { it.id },
        )
        .toList()

    fun plan(
        timetable: Timetable,
        range: EpochDayRange,
        settings: ReminderSettings,
    ): List<ReminderPlan> = plan(
        occurrences = TimetableEngine.expandOccurrences(timetable, range),
        settings = settings,
    )

    fun plan(
        timetable: Timetable,
        range: EpochDayRange,
    ): List<ReminderPlan> = plan(timetable, range, timetable.reminderSettings)

    fun plan(timetable: Timetable): List<ReminderPlan> =
        plan(timetable, timetable.coverageRange, timetable.reminderSettings)
}

fun planReminders(
    occurrences: List<CourseOccurrence>,
    settings: ReminderSettings,
): List<ReminderPlan> = ReminderPlanner.plan(occurrences, settings)

private fun reminderPlan(
    occurrence: CourseOccurrence,
    kind: ReminderKind,
    eventMinuteOfDay: Int,
    leadMinutes: Int,
    settings: ReminderSettings,
): ReminderPlan {
    val safeLeadMinutes = leadMinutes.coerceAtLeast(0)
    val eventAbsoluteMinute = occurrence.epochDay * MINUTES_PER_DAY.toLong() + eventMinuteOfDay
    val triggerAbsoluteMinute = eventAbsoluteMinute - safeLeadMinutes
    val triggerEpochDay = floorDiv(triggerAbsoluteMinute, MINUTES_PER_DAY.toLong())
    val triggerMinuteOfDay = floorMod(triggerAbsoluteMinute, MINUTES_PER_DAY.toLong()).toInt()
    val title = when (kind) {
        ReminderKind.LESSON_START -> "即将上课"
        ReminderKind.LESSON_END -> "课程结束"
    }
    val body = buildList {
        if (settings.content.includeCourseName) add(occurrence.courseName)
        if (settings.content.includeTeacher && occurrence.teacher.isNotBlank()) add(occurrence.teacher)
        if (settings.content.includeRoom && occurrence.room.isNotBlank()) add(occurrence.room)
        if (settings.content.includeNote && occurrence.note.isNotBlank()) add(occurrence.note)
    }.joinToString(" · ")

    return ReminderPlan(
        id = lengthPrefixedId("reminder", occurrence.id, kind.name.lowercase()),
        occurrenceId = occurrence.id,
        kind = kind,
        eventEpochDay = occurrence.epochDay,
        eventMinuteOfDay = eventMinuteOfDay,
        triggerEpochDay = triggerEpochDay,
        triggerMinuteOfDay = triggerMinuteOfDay,
        leadMinutes = safeLeadMinutes,
        title = title,
        body = body,
        vibrate = settings.vibrate && !settings.silent,
        silent = settings.silent,
    )
}
