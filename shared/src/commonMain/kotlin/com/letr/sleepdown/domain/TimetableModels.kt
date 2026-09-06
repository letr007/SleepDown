package com.letr.sleepdown.domain

/**
 * Shared time primitives. Dates are represented as a proleptic epoch-day number
 * and clock values as minutes since midnight so the domain stays deterministic on
 * every platform.
 */
const val MINUTES_PER_DAY: Int = 24 * 60

/** A closed, inclusive range of epoch days. */
data class EpochDayRange(
    val startEpochDay: Long,
    val endEpochDay: Long,
) {
    init {
        require(startEpochDay <= endEpochDay) { "startEpochDay must not be after endEpochDay" }
    }

    operator fun contains(epochDay: Long): Boolean =
        epochDay in startEpochDay..endEpochDay

    val days: LongRange
        get() = startEpochDay..endEpochDay
}

/** A clock interval in minutes since midnight. End points are half-open. */
data class MinuteRange(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
) {
    val durationMinutes: Int
        get() = endMinuteOfDay - startMinuteOfDay

    val isValid: Boolean
        get() = startMinuteOfDay in 0 until MINUTES_PER_DAY &&
            endMinuteOfDay in 1..MINUTES_PER_DAY &&
            startMinuteOfDay < endMinuteOfDay
}

/** Strict conversion helpers for the HH:mm representation used by platform UIs. */
object MinuteOfDay {
    fun parse(value: String): Int? {
        val parts = value.trim().split(":")
        if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (minute !in 0..59 || hour !in 0..24 || (hour == 24 && minute != 0)) return null
        return hour * 60 + minute
    }

    fun format(minuteOfDay: Int): String {
        require(minuteOfDay in 0..MINUTES_PER_DAY) {
            "minuteOfDay must be between 0 and $MINUTES_PER_DAY"
        }
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
}

/** 1=Monday ... 7=Sunday. */
enum class WeekPattern {
    ALL,
    ODD,
    EVEN;

    fun matches(week: Int): Boolean = when (this) {
        ALL -> true
        ODD -> week % 2 == 1
        EVEN -> week % 2 == 0
    }
}

/** One node in a reusable timetable. Values intentionally remain raw for validation. */
data class TimeTableNode(
    val node: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
) {
    val minuteRange: MinuteRange
        get() = MinuteRange(startMinuteOfDay, endMinuteOfDay)

    companion object {
        fun fromStrings(node: Int, start: String, end: String): TimeTableNode =
            TimeTableNode(
                node = node,
                startMinuteOfDay = MinuteOfDay.parse(start) ?: -1,
                endMinuteOfDay = MinuteOfDay.parse(end) ?: -1,
            )
    }
}

/** A named, reusable collection of timetable nodes. */
data class ScheduleTimeTable(
    val id: String = "default",
    val name: String = "Default",
    val nodes: List<TimeTableNode> = emptyList(),
) {
    fun node(number: Int): TimeTableNode? = nodes.firstOrNull { it.node == number }

    fun rangeForNodes(startNode: Int, nodeCount: Int): MinuteRange? {
        if (nodeCount < 1) return null
        val selected = (startNode until startNode + nodeCount).map { node(it) ?: return null }
        return MinuteRange(
            startMinuteOfDay = selected.first().startMinuteOfDay,
            endMinuteOfDay = selected.last().endMinuteOfDay,
        )
    }

    val gridStartMinuteOfDay: Int
        get() = nodes.minOfOrNull { it.startMinuteOfDay } ?: 0

    val gridEndMinuteOfDay: Int
        get() = nodes.maxOfOrNull { it.endMinuteOfDay } ?: MINUTES_PER_DAY
}

/** Short source alias for callers that use the two-word timetable spelling. */
typealias TimeTable = ScheduleTimeTable

/** A logical slot is the user-visible course slot; recurrence segments may split its weeks. */
data class LogicalCourseSlot(
    val id: String,
    val courseId: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val nodeCount: Int = 1,
    val teacher: String = "",
    val room: String = "",
    val customTime: MinuteRange? = null,
    val recurrenceSegments: List<RecurrenceSegment> =
        listOf(RecurrenceSegment(id = lengthPrefixedId("segment", id, "1"))),
) {
    val endNode: Int
        get() = startNode + nodeCount - 1
}

/**
 * A split recurrence segment keeps its own identity and optional overrides while
 * retaining the logical slot as the source of common course semantics.
 */
data class RecurrenceSegment(
    val id: String,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekPattern: WeekPattern = WeekPattern.ALL,
    val dayOfWeek: Int? = null,
    val startNode: Int? = null,
    val nodeCount: Int? = null,
    val teacher: String? = null,
    val room: String? = null,
    val customTime: MinuteRange? = null,
)

data class Course(
    val id: String,
    val name: String,
    val color: Int = 0,
    val note: String = "",
    val credit: Float = 0f,
    val slots: List<LogicalCourseSlot> = emptyList(),
) {
    val logicalSlots: List<LogicalCourseSlot>
        get() = slots
}

enum class DateExceptionType {
    CANCEL,
    RESCHEDULE,
}

/** A one-date override. It never changes the source recurrence segment. */
data class DateException(
    val id: String,
    val logicalSlotId: String,
    val originalEpochDay: Long,
    val type: DateExceptionType,
    val recurrenceSegmentId: String? = null,
    val targetEpochDay: Long? = null,
    val targetDayOfWeek: Int? = null,
    val targetStartNode: Int? = null,
    val targetNodeCount: Int? = null,
    val targetCustomTime: MinuteRange? = null,
    val targetTeacher: String? = null,
    val targetRoom: String? = null,
) {
    val isCancellation: Boolean
        get() = type == DateExceptionType.CANCEL

    val isReschedule: Boolean
        get() = type == DateExceptionType.RESCHEDULE
}

/** A persisted preference used when more than one occurrence occupies a time range. */
data class ConflictPreference(
    val courseId: String,
    val priority: Int = 0,
    val logicalSlotId: String? = null,
    val occurrenceId: String? = null,
    val epochDay: Long? = null,
) {
    val preferredCourseId: String
        get() = courseId

    val slotId: String?
        get() = logicalSlotId
}

typealias ConflictPriority = ConflictPreference
typealias DateExceptionKind = DateExceptionType
typealias CourseSlot = LogicalCourseSlot
typealias CourseTimeSlot = LogicalCourseSlot
typealias CourseRecurrenceSegment = RecurrenceSegment

/** The complete platform-neutral timetable aggregate. */
data class Timetable(
    val id: String,
    val name: String,
    val firstDayEpochDay: Long,
    val maxWeek: Int = 20,
    val timeTable: TimeTable = TimeTable(),
    val courses: List<Course> = emptyList(),
    val dateExceptions: List<DateException> = emptyList(),
    val conflictPreferences: List<ConflictPreference> = emptyList(),
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val sortOrder: Int = 0,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true,
    val sundayFirst: Boolean = false,
) {
    val showSat: Boolean
        get() = showSaturday

    val showSun: Boolean
        get() = showSunday

    val order: Int
        get() = sortOrder

    val coverageRange: EpochDayRange
        get() = EpochDayRange(
            startEpochDay = firstDayEpochDay,
            endEpochDay = firstDayEpochDay + maxOf(1, maxWeek) * 7L - 1L,
        )
}

/** An occurrence after recurrence expansion and date-exception application. */
data class CourseOccurrence(
    val id: String,
    val courseId: String,
    val courseName: String,
    val logicalSlotId: String,
    val recurrenceSegmentId: String,
    val sourceEpochDay: Long,
    val epochDay: Long,
    val dayOfWeek: Int,
    val week: Int,
    val startNode: Int,
    val nodeCount: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val teacher: String,
    val room: String,
    val note: String,
    val color: Int,
    val usesCustomTime: Boolean,
    val isRescheduled: Boolean = false,
    val exceptionId: String? = null,
) {
    val durationMinutes: Int
        get() = endMinuteOfDay - startMinuteOfDay

    val minuteRange: MinuteRange
        get() = MinuteRange(startMinuteOfDay, endMinuteOfDay)

    val originalEpochDay: Long
        get() = sourceEpochDay
}

data class GridPlacement(
    val topFraction: Double,
    val heightFraction: Double,
    val leftFraction: Double = 0.0,
    val widthFraction: Double = 1.0,
    val column: Int = 0,
    val columnCount: Int = 1,
) {
    val positionFraction: Double
        get() = topFraction

    val sizeFraction: Double
        get() = heightFraction
}

data class ConflictGroup(
    val id: String,
    val epochDay: Long,
    val occurrences: List<CourseOccurrence>,
    val preferredOccurrenceId: String,
    val placements: Map<String, GridPlacement> = emptyMap(),
) {
    val preferredOccurrence: CourseOccurrence
        get() = occurrences.first { it.id == preferredOccurrenceId }

    val hasConflict: Boolean
        get() = occurrences.size > 1
}

enum class ReminderKind {
    LESSON_START,
    LESSON_END,
}

data class ReminderContentSettings(
    val includeCourseName: Boolean = true,
    val includeTeacher: Boolean = true,
    val includeRoom: Boolean = true,
    val includeNote: Boolean = false,
)

data class ReminderSettings(
    val startEnabled: Boolean = true,
    val endEnabled: Boolean = false,
    val startLeadMinutes: Int = 10,
    val endLeadMinutes: Int = 0,
    val content: ReminderContentSettings = ReminderContentSettings(),
    val vibrate: Boolean = true,
    val silent: Boolean = false,
)

data class ReminderPlan(
    val id: String,
    val occurrenceId: String,
    val kind: ReminderKind,
    val eventEpochDay: Long,
    val eventMinuteOfDay: Int,
    val triggerEpochDay: Long,
    val triggerMinuteOfDay: Int,
    val leadMinutes: Int,
    val title: String,
    val body: String,
    val vibrate: Boolean,
    val silent: Boolean,
)

enum class WidgetSnapshotKind {
    NEXT,
    TODAY,
    WEEK,
}

data class WidgetSnapshotItem(
    val occurrenceId: String,
    val courseId: String,
    val courseName: String,
    val epochDay: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val teacher: String,
    val room: String,
    val note: String,
    val color: Int,
    val isCurrent: Boolean,
    val conflictCount: Int,
    val isPreferred: Boolean,
) {
    val durationMinutes: Int
        get() = endMinuteOfDay - startMinuteOfDay
}

data class WidgetSnapshot(
    val kind: WidgetSnapshotKind,
    val timetableId: String,
    val anchorEpochDay: Long,
    val generatedAtMinuteOfDay: Int,
    val items: List<WidgetSnapshotItem>,
)

data class WidgetSnapshots(
    val next: WidgetSnapshot,
    val today: WidgetSnapshot,
    val week: WidgetSnapshot,
)

internal fun floorDiv(value: Long, divisor: Long): Long {
    require(divisor > 0) { "divisor must be positive" }
    val quotient = value / divisor
    val remainder = value % divisor
    return if (remainder < 0) quotient - 1 else quotient
}

internal fun floorMod(value: Long, divisor: Long): Long {
    require(divisor > 0) { "divisor must be positive" }
    val remainder = value % divisor
    return if (remainder < 0) remainder + divisor else remainder
}

/** ISO weekday derived from the epoch-day primitive (1970-01-01 was Thursday). */
internal fun isoDayOfWeek(epochDay: Long): Int =
    (floorMod(epochDay + 3L, 7L) + 1L).toInt()

internal fun timetableDayOfWeek(timetable: Timetable, epochDay: Long): Int =
    (floorMod(epochDay - timetable.firstDayEpochDay, 7L) + 1L).toInt()
