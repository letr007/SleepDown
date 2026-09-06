package com.letr.sleepdown.interchange

import com.letr.sleepdown.domain.ConflictPreference
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.TimeTableValidator
import com.letr.sleepdown.domain.TimeTableValidationIssue
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.TimetableValidationException
import com.letr.sleepdown.domain.WeekPattern
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Exact Gson field names observed in the WakeUp 6.1.22 export path. */
@Serializable
data class WakeUpTimeTableCompatDto(
    val courseLen: Int? = null,
    val id: Int? = null,
    val name: String? = null,
    val sameBreakLen: Boolean? = null,
    val sameLen: Boolean? = null,
    val theBreakLen: Int? = null,
    val sortOrder: Int? = null,
    val order: Int? = null,
)

@Serializable
data class WakeUpTimeDetailBeanDto(
    val endTime: String? = null,
    val node: Int? = null,
    val startTime: String? = null,
    val timeTable: Int? = null,
)

@Serializable
data class WakeUpTableCompatDto(
    val background: String? = null,
    val courseTextColor: Int? = null,
    val id: Int? = null,
    val itemAlpha: Int? = null,
    val itemHeight: Int? = null,
    val itemTextSize: Int? = null,
    val maxWeek: Int? = null,
    val nodes: Int? = null,
    val school: String? = null,
    val showOtherWeekCourse: Boolean? = null,
    val showSat: Boolean? = null,
    val showSun: Boolean? = null,
    val showTime: Boolean? = null,
    val startDate: String? = null,
    val strokeColor: Int? = null,
    val sundayFirst: Boolean? = null,
    val tableName: String? = null,
    val textColor: Int? = null,
    val tid: String? = null,
    val timeTable: Int? = null,
    val type: Int? = null,
    val updateTime: Long? = null,
    val widgetCourseTextColor: Int? = null,
    val widgetItemAlpha: Int? = null,
    val widgetItemHeight: Int? = null,
    val widgetItemTextSize: Int? = null,
    val widgetStrokeColor: Int? = null,
    val widgetTextColor: Int? = null,
    val sortOrder: Int? = null,
    val order: Int? = null,
)

@Serializable
data class WakeUpCourseBaseBeanDto(
    val color: String? = null,
    val courseName: String? = null,
    val credit: Float? = null,
    val id: Int? = null,
    val note: String? = null,
    val tableId: Int? = null,
)

@Serializable
data class WakeUpCourseDetailBeanDto(
    val day: Int? = null,
    val endTime: String? = null,
    val endWeek: Int? = null,
    val id: Int? = null,
    val level: Int? = null,
    val ownTime: Boolean? = null,
    val room: String? = null,
    val startNode: Int? = null,
    val startTime: String? = null,
    val startWeek: Int? = null,
    val step: Int? = null,
    val tableId: Int? = null,
    val teacher: String? = null,
    val type: Int? = null,
)

/** The five values emitted by WakeUp's ScheduleViewModel export method. */
data class WakeUpBackupDocument(
    val timeTable: WakeUpTimeTableCompatDto,
    val timeDetails: List<WakeUpTimeDetailBeanDto>,
    val table: WakeUpTableCompatDto,
    val courseBases: List<WakeUpCourseBaseBeanDto>,
    val courseDetails: List<WakeUpCourseDetailBeanDto>,
)

class WakeUpBackupParseException(
    val lineNumber: Int? = null,
    val field: String? = null,
    message: String,
    cause: Throwable? = null,
    val issues: List<TimeTableValidationIssue> = emptyList(),
) : IllegalArgumentException(
    buildString {
        if (lineNumber != null) append("line ").append(lineNumber).append(": ")
        if (field != null) append(field).append(": ")
        append(message)
    },
    cause,
)

private val wakeUpJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

/** Read-only compatibility importer for the observed WakeUp 6.1.22 five-line format. */
object WakeUpBackupImporter {
    @Throws(Exception::class)
    fun parse(text: String): WakeUpBackupDocument {
        val values = text.split('\n')
            .mapIndexed { index, value -> index + 1 to value }
            .filter { (_, value) -> value.trim().isNotEmpty() }
        if (values.size != 5) {
            throw WakeUpBackupParseException(
                message = "expected exactly five nonblank LF-delimited JSON values in WakeUp export order, found ${values.size}",
            )
        }
        return WakeUpBackupDocument(
            timeTable = decode(values[0], WakeUpTimeTableCompatDto.serializer()),
            timeDetails = decode(values[1], ListSerializer(WakeUpTimeDetailBeanDto.serializer())),
            table = decode(values[2], WakeUpTableCompatDto.serializer()),
            courseBases = decode(values[3], ListSerializer(WakeUpCourseBaseBeanDto.serializer())),
            courseDetails = decode(values[4], ListSerializer(WakeUpCourseDetailBeanDto.serializer())),
        )
    }

    @Throws(Exception::class)
    fun parse(bytes: ByteArray): WakeUpBackupDocument = parse(decodeUtf8Strict(bytes))

    @Throws(Exception::class)
    fun parseFiveLines(text: String): WakeUpBackupDocument = parse(text)

    @Throws(Exception::class)
    fun importToTimetable(
        text: String,
        timetableId: String = "wakeup-import",
    ): Timetable = importDocument(parse(text), timetableId)

    @Throws(Exception::class)
    fun importToTimetable(
        bytes: ByteArray,
        timetableId: String = "wakeup-import",
    ): Timetable = importDocument(parse(bytes), timetableId)

    @Throws(Exception::class)
    fun importDocument(
        document: WakeUpBackupDocument,
        timetableId: String = "wakeup-import",
    ): Timetable {
        if (document.timeDetails.isEmpty()) {
            fail(2, "timeDetails", "must contain at least one node")
        }
        val timeTableId = required(document.timeTable.id, 1, "id")
        val timeTableName = requiredText(document.timeTable.name, 1, "name")
        val timeNodes = document.timeDetails
            .mapIndexed { index, detail ->
                val node = required(detail.node, 2, "[$index].node")
                val start = requiredText(detail.startTime, 2, "[$index].startTime")
                val end = requiredText(detail.endTime, 2, "[$index].endTime")
                val referencedTimeTable = required(detail.timeTable, 2, "[$index].timeTable")
                if (referencedTimeTable != timeTableId) {
                    fail(2, "[$index].timeTable", "references time table $referencedTimeTable instead of $timeTableId")
                }
                if (node < 1) fail(2, "[$index].node", "must be positive")
                val startMinute = MinuteOfDay.parse(start)
                    ?: fail(2, "[$index].startTime", "must use a valid HH:mm value")
                val endMinute = MinuteOfDay.parse(end)
                    ?: fail(2, "[$index].endTime", "must use a valid HH:mm value")
                if (!MinuteRange(startMinute, endMinute).isValid) {
                    fail(2, "[$index]", "endTime must be after startTime")
                }
                TimeTableNode(node, startMinute, endMinute)
            }
        if (timeNodes.map { it.node }.distinct().size != timeNodes.size) {
            fail(2, "timeDetails", "contains duplicate node values")
        }

        val tableId = required(document.table.id, 3, "id")
        val tableName = requiredText(document.table.tableName, 3, "tableName")
        val referencedTimeTable = required(document.table.timeTable, 3, "timeTable")
        if (referencedTimeTable != timeTableId) {
            fail(3, "timeTable", "references time table $referencedTimeTable instead of $timeTableId")
        }
        val maxWeek = required(document.table.maxWeek, 3, "maxWeek")
        if (maxWeek < 1) fail(3, "maxWeek", "must be positive")
        val nodeCount = required(document.table.nodes, 3, "nodes")
        if (nodeCount < 1) fail(3, "nodes", "must be positive")
        if (timeNodes.size != nodeCount) {
            fail(2, "timeDetails", "contains ${timeNodes.size} nodes but TableCompat.nodes=$nodeCount")
        }
        timeNodes.firstOrNull { it.node > nodeCount }?.let {
            fail(2, "timeDetails", "node ${it.node} exceeds TableCompat.nodes=$nodeCount")
        }
        val nodeValidation = TimeTableValidator.validate(
            ScheduleTimeTable(id = timeTableId.toString(), name = timeTableName, nodes = timeNodes),
        )
        if (!nodeValidation.isValid) {
            throw WakeUpBackupParseException(
                lineNumber = 2,
                field = "timeDetails",
                message = nodeValidation.issues.joinToString("; ") { issue ->
                    val path = issue.path.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
                    "$path${issue.code}: ${issue.message}"
                },
                issues = nodeValidation.issues,
            )
        }
        val startDateText = requiredText(document.table.startDate, 3, "startDate")
        val firstDayEpochDay = parseIsoDate(startDateText)
            ?: fail(3, "startDate", "must use yyyy-M-d or yyyy-MM-dd")

        val baseById = linkedMapOf<Int, WakeUpCourseBaseBeanDto>()
        document.courseBases
            .sortedWith(compareBy<WakeUpCourseBaseBeanDto> { it.id ?: Int.MIN_VALUE }.thenBy { it.courseName ?: "" })
            .forEachIndexed { index, base ->
                val id = required(base.id, 4, "[$index].id")
                if (baseById.put(id, base) != null) fail(4, "[$index].id", "is duplicated")
                requiredText(base.courseName, 4, "[$index].courseName")
                val referencedTable = required(base.tableId, 4, "[$index].tableId")
                if (referencedTable != tableId) {
                    fail(4, "[$index].tableId", "references table $referencedTable instead of $tableId")
                }
                parseColor(base.color, 4, "[$index].color")
            }

        val details = document.courseDetails
            .sortedWith(courseDetailComparator)
            .mapIndexed { index, detail ->
                val id = required(detail.id, 5, "[$index].id")
                if (!baseById.containsKey(id)) fail(5, "[$index].id", "does not reference a CourseBaseBean")
                val referencedTable = required(detail.tableId, 5, "[$index].tableId")
                if (referencedTable != tableId) {
                    fail(5, "[$index].tableId", "references table $referencedTable instead of $tableId")
                }
                val day = required(detail.day, 5, "[$index].day")
                if (day !in 1..7) fail(5, "[$index].day", "must be between 1 and 7")
                val startNode = required(detail.startNode, 5, "[$index].startNode")
                if (startNode < 1) fail(5, "[$index].startNode", "must be positive")
                val step = required(detail.step, 5, "[$index].step")
                if (step < 1) fail(5, "[$index].step", "must be positive")
                if (startNode + step - 1 > nodeCount) {
                    fail(5, "[$index]", "startNode+step exceeds TableCompat.nodes=$nodeCount")
                }
                val startWeek = required(detail.startWeek, 5, "[$index].startWeek")
                val endWeek = required(detail.endWeek, 5, "[$index].endWeek")
                if (startWeek !in 1..maxWeek || endWeek !in 1..maxWeek || startWeek > endWeek) {
                    fail(5, "[$index]", "startWeek/endWeek must be an ordered range within 1..$maxWeek")
                }
                val type = required(detail.type, 5, "[$index].type")
                if (type !in 0..2) fail(5, "[$index].type", "must be 0 (all), 1 (odd), or 2 (even)")
                val ownTime = required(detail.ownTime, 5, "[$index].ownTime")
                val customTime = if (ownTime) {
                    val startText = requiredText(detail.startTime, 5, "[$index].startTime")
                    val endText = requiredText(detail.endTime, 5, "[$index].endTime")
                    val startMinute = MinuteOfDay.parse(startText)
                        ?: fail(5, "[$index].startTime", "must use a valid HH:mm value when ownTime=true")
                    val endMinute = MinuteOfDay.parse(endText)
                        ?: fail(5, "[$index].endTime", "must use a valid HH:mm value when ownTime=true")
                    MinuteRange(startMinute, endMinute).takeIf { it.isValid }
                        ?: fail(5, "[$index]", "endTime must be after startTime when ownTime=true")
                } else {
                    null
                }
                detail.copy(
                    id = id,
                    day = day,
                    startNode = startNode,
                    step = step,
                    startWeek = startWeek,
                    endWeek = endWeek,
                    type = type,
                    tableId = referencedTable,
                    ownTime = ownTime,
                    startTime = detail.startTime ?: "",
                    endTime = detail.endTime ?: "",
                    room = detail.room ?: "",
                    teacher = detail.teacher ?: "",
                ) to customTime
            }

        val preferenceSpecs = mutableListOf<WakeUpConflictPreferenceSpec>()
        val courses = baseById.entries.sortedBy { it.key }.map { (baseId, base) ->
            val baseDetails = details
                .filter { (detail, _) -> detail.id == baseId }
                .map { it }
            if (baseDetails.isEmpty()) {
                fail(4, "courseBases[$baseId]", "has no matching CourseDetailBean")
            }
            val grouped = baseDetails
                .groupBy { (detail, _) -> WakeUpSlotKey.from(detail) }
                .entries
                .sortedWith(compareBy({ it.key.day }, { it.key.startNode }, { it.key.step }, { it.key.ownTime }, { it.key.startTime }, { it.key.endTime }))
            val courseId = "wakeup:$tableId:course:$baseId"
            val slots = grouped.mapIndexed { slotIndex, (_, groupedDetails) ->
                val first = groupedDetails
                    .sortedWith(compareBy({ it.first.startWeek ?: 0 }, { it.first.endWeek ?: 0 }, { it.first.type ?: 0 }, { it.first.teacher ?: "" }, { it.first.room ?: "" }))
                    .first()
                val firstDetail = first.first
                val firstCustomTime = first.second
                val slotId = "$courseId:slot:${slotIndex + 1}"
                val segments = groupedDetails
                    .sortedWith(compareBy({ it.first.startWeek ?: 0 }, { it.first.endWeek ?: 0 }, { it.first.type ?: 0 }, { it.first.teacher ?: "" }, { it.first.room ?: "" }))
                    .mapIndexed { segmentIndex, (detail, customTime) ->
                        val segmentId = "$slotId:segment:${segmentIndex + 1}"
                        preferenceSpecs += WakeUpConflictPreferenceSpec(
                            courseId = courseId,
                            logicalSlotId = slotId,
                            recurrenceSegmentId = segmentId,
                            priority = detail.level ?: 0,
                        )
                        RecurrenceSegment(
                            id = segmentId,
                            startWeek = detail.startWeek ?: error("validated startWeek missing"),
                            endWeek = detail.endWeek ?: error("validated endWeek missing"),
                            weekPattern = weekPattern(detail.type ?: error("validated type missing")),
                            dayOfWeek = detail.day,
                            startNode = detail.startNode,
                            nodeCount = detail.step,
                            teacher = detail.teacher,
                            room = detail.room,
                            customTime = customTime,
                        )
                    }
                LogicalCourseSlot(
                    id = slotId,
                    courseId = courseId,
                    dayOfWeek = firstDetail.day ?: error("validated day missing"),
                    startNode = firstDetail.startNode ?: error("validated startNode missing"),
                    nodeCount = firstDetail.step ?: error("validated step missing"),
                    teacher = firstDetail.teacher ?: "",
                    room = firstDetail.room ?: "",
                    customTime = firstCustomTime,
                    recurrenceSegments = segments,
                )
            }
            Course(
                id = courseId,
                name = base.courseName ?: error("validated courseName missing"),
                color = parseColor(base.color, 4, "course $baseId color"),
                note = base.note ?: "",
                credit = base.credit ?: 0f,
                slots = slots,
            )
        }

        val baseTimetable = Timetable(
            id = timetableId,
            name = tableName,
            firstDayEpochDay = firstDayEpochDay,
            maxWeek = maxWeek,
            timeTable = ScheduleTimeTable(
                id = timeTableId.toString(),
                name = timeTableName,
                nodes = timeNodes,
            ),
            courses = courses,
            sortOrder = document.table.sortOrder
                ?: document.table.order
                ?: document.timeTable.sortOrder
                ?: document.timeTable.order
                ?: 0,
            showSaturday = document.table.showSat ?: true,
            showSunday = document.table.showSun ?: true,
            sundayFirst = document.table.sundayFirst ?: false,
        )
        val recurringOccurrences = TimetableEngine.recurringOccurrences(baseTimetable)
        val timetable = baseTimetable.copy(
            conflictPreferences = preferenceSpecs.flatMap { spec ->
                recurringOccurrences
                    .filter { occurrence ->
                        occurrence.courseId == spec.courseId &&
                            occurrence.logicalSlotId == spec.logicalSlotId &&
                            occurrence.recurrenceSegmentId == spec.recurrenceSegmentId
                    }
                    .map { occurrence ->
                        ConflictPreference(
                            courseId = occurrence.courseId,
                            priority = spec.priority,
                            logicalSlotId = occurrence.logicalSlotId,
                            occurrenceId = occurrence.id,
                            epochDay = occurrence.epochDay,
                        )
                    }
            },
        )
        try {
            return TimeTableValidator.requireValid(timetable, "WakeUp import")
        } catch (error: TimetableValidationException) {
            throw WakeUpBackupParseException(
                message = "WakeUp import produced an invalid timetable: ${error.issues.joinToString("; ") { issue ->
                    val path = issue.path.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
                    "$path${issue.code}: ${issue.message}"
                }}",
                cause = error,
                issues = error.issues,
            )
        }
    }

    private fun <T> decode(
        value: Pair<Int, String>,
        deserializer: DeserializationStrategy<T>,
    ): T = try {
        wakeUpJson.decodeFromString(deserializer, value.second.trim())
    } catch (error: SerializationException) {
        throw WakeUpBackupParseException(
            lineNumber = value.first,
            message = "invalid JSON value: ${error.message}",
            cause = error,
        )
    } catch (error: IllegalArgumentException) {
        throw WakeUpBackupParseException(
            lineNumber = value.first,
            message = "invalid JSON value: ${error.message}",
            cause = error,
        )
    }
}

@Throws(Exception::class)
fun parseWakeUpBackup(text: String): WakeUpBackupDocument = WakeUpBackupImporter.parse(text)

@Throws(Exception::class)
fun importWakeUpBackup(
    text: String,
    timetableId: String = "wakeup-import",
): Timetable = WakeUpBackupImporter.importToTimetable(text, timetableId)

private data class WakeUpConflictPreferenceSpec(
    val courseId: String,
    val logicalSlotId: String,
    val recurrenceSegmentId: String,
    val priority: Int,
)

private data class WakeUpSlotKey(
    val day: Int,
    val startNode: Int,
    val step: Int,
    val ownTime: Boolean,
    val startTime: String,
    val endTime: String,
) {
    companion object {
        fun from(detail: WakeUpCourseDetailBeanDto): WakeUpSlotKey = WakeUpSlotKey(
            day = detail.day ?: 0,
            startNode = detail.startNode ?: 0,
            step = detail.step ?: 0,
            ownTime = detail.ownTime == true,
            startTime = if (detail.ownTime == true) detail.startTime ?: "" else "",
            endTime = if (detail.ownTime == true) detail.endTime ?: "" else "",
        )
    }
}

private val courseDetailComparator = compareBy<WakeUpCourseDetailBeanDto> { it.id ?: Int.MIN_VALUE }
    .thenBy { it.day ?: Int.MIN_VALUE }
    .thenBy { it.startNode ?: Int.MIN_VALUE }
    .thenBy { it.step ?: Int.MIN_VALUE }
    .thenBy { it.startWeek ?: Int.MIN_VALUE }
    .thenBy { it.endWeek ?: Int.MIN_VALUE }
    .thenBy { it.type ?: Int.MIN_VALUE }
    .thenBy { it.ownTime ?: false }
    .thenBy { it.startTime ?: "" }
    .thenBy { it.endTime ?: "" }
    .thenBy { it.teacher ?: "" }
    .thenBy { it.room ?: "" }
    .thenBy { it.level ?: 0 }

private fun weekPattern(type: Int): WeekPattern = when (type) {
    0 -> WeekPattern.ALL
    1 -> WeekPattern.ODD
    2 -> WeekPattern.EVEN
    else -> error("validated type out of range")
}

private fun parseColor(value: String?, line: Int, field: String): Int {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return 0
    val digits = text.removePrefix("#")
    val normalized = when (digits.length) {
        6 -> "FF$digits"
        8 -> digits
        else -> fail(line, field, "must be #RRGGBB or #AARRGGBB")
    }
    if (normalized.any { it.digitToIntOrNull(16) == null }) {
        fail(line, field, "contains non-hexadecimal color digits")
    }
    return normalized.toLong(16).toInt()
}

private fun <T> required(value: T?, line: Int, field: String): T =
    value ?: fail(line, field, "is required")

private fun requiredText(value: String?, line: Int, field: String): String {
    val text = required(value, line, field)
    if (text.isBlank()) fail(line, field, "must not be blank")
    return text
}

private fun fail(line: Int, field: String, message: String): Nothing =
    throw WakeUpBackupParseException(lineNumber = line, field = field, message = message)

private fun decodeUtf8Strict(bytes: ByteArray): String {
    val result = StringBuilder(bytes.size)
    var index = 0
    while (index < bytes.size) {
        val first = bytes[index].toInt() and 0xFF
        when {
            first <= 0x7F -> {
                result.append(first.toChar())
                index++
            }
            first in 0xC2..0xDF -> {
                val second = continuationByte(bytes, index + 1)
                val codePoint = ((first and 0x1F) shl 6) or (second and 0x3F)
                result.append(codePoint.toChar())
                index += 2
            }
            first in 0xE0..0xEF -> {
                val second = continuationByte(bytes, index + 1)
                val third = continuationByte(bytes, index + 2)
                if (first == 0xE0 && second < 0xA0 || first == 0xED && second >= 0xA0) {
                    invalidUtf8(index)
                }
                val codePoint = ((first and 0x0F) shl 12) or
                    ((second and 0x3F) shl 6) or
                    (third and 0x3F)
                result.append(codePoint.toChar())
                index += 3
            }
            first in 0xF0..0xF4 -> {
                val second = continuationByte(bytes, index + 1)
                val third = continuationByte(bytes, index + 2)
                val fourth = continuationByte(bytes, index + 3)
                if (first == 0xF0 && second < 0x90 || first == 0xF4 && second > 0x8F) {
                    invalidUtf8(index)
                }
                val codePoint = ((first and 0x07) shl 18) or
                    ((second and 0x3F) shl 12) or
                    ((third and 0x3F) shl 6) or
                    (fourth and 0x3F)
                val adjusted = codePoint - 0x10000
                result.append((0xD800 + (adjusted shr 10)).toChar())
                result.append((0xDC00 + (adjusted and 0x3FF)).toChar())
                index += 4
            }
            else -> invalidUtf8(index)
        }
    }
    return result.toString()
}

private fun continuationByte(bytes: ByteArray, index: Int): Int {
    if (index >= bytes.size) invalidUtf8(index)
    val value = bytes[index].toInt() and 0xFF
    if (value !in 0x80..0xBF) invalidUtf8(index)
    return value
}

private fun invalidUtf8(index: Int): Nothing =
    throw WakeUpBackupParseException(message = "input is not valid UTF-8 at byte $index")
