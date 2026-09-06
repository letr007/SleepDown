package com.letr.sleepdown.interchange

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
import com.letr.sleepdown.domain.TimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimeTableValidator
import com.letr.sleepdown.domain.TimeTableValidationIssue
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.TimetableValidationException
import com.letr.sleepdown.domain.floorDiv
import com.letr.sleepdown.domain.WeekPattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val SLEEP_DOWN_BACKUP_FORMAT_VERSION: Int = 1

@Serializable
data class BackupDocumentDto(
    val formatVersion: Int,
    val timetable: TimetableBackupDto,
)

@Serializable
data class TimetableBackupDto(
    val id: String,
    val name: String,
    val firstDayEpochDay: Long,
    val maxWeek: Int,
    val timeTable: ScheduleTimeTableBackupDto,
    val courses: List<CourseBackupDto>,
    val dateExceptions: List<DateExceptionBackupDto>,
    val conflictPreferences: List<ConflictPreferenceBackupDto>,
    val reminderSettings: ReminderSettingsBackupDto,
    val sortOrder: Int = 0,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true,
    val sundayFirst: Boolean = false,
)

@Serializable
data class ScheduleTimeTableBackupDto(
    val id: String,
    val name: String,
    val nodes: List<TimeTableNodeBackupDto>,
)

typealias TimeTableBackupDto = ScheduleTimeTableBackupDto
typealias SleepDownBackupDto = BackupDocumentDto

@Serializable
data class TimeTableNodeBackupDto(
    val node: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
)

@Serializable
data class CourseBackupDto(
    val id: String,
    val name: String,
    val color: Int,
    val note: String,
    val credit: Float,
    val slots: List<LogicalCourseSlotBackupDto>,
)

@Serializable
data class LogicalCourseSlotBackupDto(
    val id: String,
    val courseId: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val nodeCount: Int,
    val teacher: String,
    val room: String,
    val customTime: MinuteRangeBackupDto?,
    val recurrenceSegments: List<RecurrenceSegmentBackupDto>,
)

@Serializable
data class RecurrenceSegmentBackupDto(
    val id: String,
    val startWeek: Int,
    val endWeek: Int,
    val weekPattern: WeekPattern,
    val dayOfWeek: Int?,
    val startNode: Int?,
    val nodeCount: Int?,
    val teacher: String?,
    val room: String?,
    val customTime: MinuteRangeBackupDto?,
)

@Serializable
data class MinuteRangeBackupDto(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
)

@Serializable
data class DateExceptionBackupDto(
    val id: String,
    val logicalSlotId: String,
    val originalEpochDay: Long,
    val type: DateExceptionType,
    val recurrenceSegmentId: String?,
    val targetEpochDay: Long?,
    val targetDayOfWeek: Int?,
    val targetStartNode: Int?,
    val targetNodeCount: Int?,
    val targetCustomTime: MinuteRangeBackupDto?,
    val targetTeacher: String?,
    val targetRoom: String?,
)

@Serializable
data class ConflictPreferenceBackupDto(
    val courseId: String,
    val priority: Int,
    val logicalSlotId: String?,
    val occurrenceId: String?,
    val epochDay: Long?,
)

@Serializable
data class ReminderContentSettingsBackupDto(
    val includeCourseName: Boolean,
    val includeTeacher: Boolean,
    val includeRoom: Boolean,
    val includeNote: Boolean,
)

@Serializable
data class ReminderSettingsBackupDto(
    val startEnabled: Boolean,
    val endEnabled: Boolean,
    val startLeadMinutes: Int,
    val endLeadMinutes: Int,
    val content: ReminderContentSettingsBackupDto,
    val vibrate: Boolean,
    val silent: Boolean,
)

class BackupFormatException(
    message: String,
    cause: Throwable? = null,
    val issues: List<TimeTableValidationIssue> = emptyList(),
) : IllegalArgumentException(message, cause)

private fun TimetableValidationException.asBackupFormatException(context: String): BackupFormatException {
    val details = issues.joinToString("; ") { issue ->
        val path = issue.path.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
        "$path${issue.code}: ${issue.message}"
    }
    return BackupFormatException(
        message = "$context is invalid: $details",
        cause = this,
        issues = issues,
    )
}

enum class BackupImportMode {
    MERGE,
    REPLACE,
}

data class BackupIdRemap(
    val namespace: String,
    val oldId: String,
    val newId: String,
)

data class BackupImportResult(
    val timetable: Timetable,
    val remappedIds: List<BackupIdRemap> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    /** Namespaced keys avoid ambiguity when independent entity types share an ID. */
    val idRemapping: Map<String, String>
        get() = remappedIds.associate { "${it.namespace}:${it.oldId}" to it.newId }
}

internal val interchangeJson: Json = Json {
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = true
}

object BackupFormat {
    @Throws(Exception::class)
    fun encode(timetable: Timetable): String {
        TimeTableValidator.requireValid(timetable, "SleepDown backup")
        return interchangeJson.encodeToString(toDto(timetable))
    }

    @Throws(Exception::class)
    fun decode(json: String): Timetable {
        val timetable = try {
            fromDto(decodeDocument(json).timetable)
        } catch (error: TimetableValidationException) {
            throw error.asBackupFormatException("Decoded SleepDown backup")
        }
        return try {
            TimeTableValidator.requireValid(timetable, "Decoded SleepDown backup")
        } catch (error: TimetableValidationException) {
            throw error.asBackupFormatException("Decoded SleepDown backup")
        }
    }

    fun encodeDocument(timetable: Timetable): BackupDocumentDto = toDto(timetable)

    @Throws(Exception::class)
    fun decodeDocument(json: String): BackupDocumentDto {
        val root = try {
            interchangeJson.parseToJsonElement(json).jsonObject
        } catch (error: SerializationException) {
            throw BackupFormatException("Invalid SleepDown backup JSON: ${error.message}", error)
        } catch (error: IllegalArgumentException) {
            throw BackupFormatException("Invalid SleepDown backup JSON: ${error.message}", error)
        }
        val version = try {
            root["formatVersion"]?.jsonPrimitive?.content?.toIntOrNull()
        } catch (error: IllegalArgumentException) {
            throw BackupFormatException("SleepDown backup formatVersion must be an integer", error)
        } ?: throw BackupFormatException("SleepDown backup is missing integer formatVersion")
        if (version != SLEEP_DOWN_BACKUP_FORMAT_VERSION) {
            throw BackupFormatException(
                "Unsupported SleepDown backup formatVersion=$version; supported version is $SLEEP_DOWN_BACKUP_FORMAT_VERSION",
            )
        }
        if (root["timetable"] == null) {
            throw BackupFormatException("SleepDown backup is missing timetable")
        }
        return try {
            interchangeJson.decodeFromString<BackupDocumentDto>(json)
        } catch (error: SerializationException) {
            throw BackupFormatException("Invalid SleepDown backup fields: ${error.message}", error)
        }
    }

    @Throws(Exception::class)
    fun importInto(
        current: Timetable?,
        json: String,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): BackupImportResult = BackupImportPolicy.apply(current, decode(json), mode)

    @Throws(Exception::class)
    fun importDocumentInto(
        current: Timetable?,
        document: BackupDocumentDto,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): BackupImportResult = BackupImportPolicy.apply(current, fromDto(document.timetable), mode)

    fun toDto(timetable: Timetable): BackupDocumentDto =
        BackupDocumentDto(
            formatVersion = SLEEP_DOWN_BACKUP_FORMAT_VERSION,
            timetable = timetable.toBackupDto(),
        )
}

fun Timetable.toBackupDto(): TimetableBackupDto = TimetableBackupDto(
    id = id,
    name = name,
    firstDayEpochDay = firstDayEpochDay,
    maxWeek = maxWeek,
    timeTable = timeTable.toBackupDto(),
    courses = courses.map { it.toBackupDto() },
    dateExceptions = dateExceptions.map { it.toBackupDto() },
    conflictPreferences = conflictPreferences.map { it.toBackupDto() },
    reminderSettings = reminderSettings.toBackupDto(),
    sortOrder = sortOrder,
    showSaturday = showSaturday,
    showSunday = showSunday,
    sundayFirst = sundayFirst,
)

fun TimetableBackupDto.toDomain(): Timetable = fromDto(this)

private fun ScheduleTimeTable.toBackupDto(): ScheduleTimeTableBackupDto = ScheduleTimeTableBackupDto(
    id = id,
    name = name,
    nodes = nodes.map { it.toBackupDto() },
)

private fun TimeTableNode.toBackupDto(): TimeTableNodeBackupDto = TimeTableNodeBackupDto(
    node = node,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
)

private fun Course.toBackupDto(): CourseBackupDto = CourseBackupDto(
    id = id,
    name = name,
    color = color,
    note = note,
    credit = credit,
    slots = slots.map { it.toBackupDto() },
)

private fun LogicalCourseSlot.toBackupDto(): LogicalCourseSlotBackupDto = LogicalCourseSlotBackupDto(
    id = id,
    courseId = courseId,
    dayOfWeek = dayOfWeek,
    startNode = startNode,
    nodeCount = nodeCount,
    teacher = teacher,
    room = room,
    customTime = customTime?.toBackupDto(),
    recurrenceSegments = recurrenceSegments.map { it.toBackupDto() },
)

private fun RecurrenceSegment.toBackupDto(): RecurrenceSegmentBackupDto = RecurrenceSegmentBackupDto(
    id = id,
    startWeek = startWeek,
    endWeek = endWeek,
    weekPattern = weekPattern,
    dayOfWeek = dayOfWeek,
    startNode = startNode,
    nodeCount = nodeCount,
    teacher = teacher,
    room = room,
    customTime = customTime?.toBackupDto(),
)

private fun MinuteRange.toBackupDto(): MinuteRangeBackupDto = MinuteRangeBackupDto(
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
)

private fun DateException.toBackupDto(): DateExceptionBackupDto = DateExceptionBackupDto(
    id = id,
    logicalSlotId = logicalSlotId,
    originalEpochDay = originalEpochDay,
    type = type,
    recurrenceSegmentId = recurrenceSegmentId,
    targetEpochDay = targetEpochDay,
    targetDayOfWeek = targetDayOfWeek,
    targetStartNode = targetStartNode,
    targetNodeCount = targetNodeCount,
    targetCustomTime = targetCustomTime?.toBackupDto(),
    targetTeacher = targetTeacher,
    targetRoom = targetRoom,
)

private fun ConflictPreference.toBackupDto(): ConflictPreferenceBackupDto = ConflictPreferenceBackupDto(
    courseId = courseId,
    priority = priority,
    logicalSlotId = logicalSlotId,
    occurrenceId = occurrenceId,
    epochDay = epochDay,
)

private fun ReminderSettings.toBackupDto(): ReminderSettingsBackupDto = ReminderSettingsBackupDto(
    startEnabled = startEnabled,
    endEnabled = endEnabled,
    startLeadMinutes = startLeadMinutes,
    endLeadMinutes = endLeadMinutes,
    content = content.toBackupDto(),
    vibrate = vibrate,
    silent = silent,
)

private fun ReminderContentSettings.toBackupDto(): ReminderContentSettingsBackupDto = ReminderContentSettingsBackupDto(
    includeCourseName = includeCourseName,
    includeTeacher = includeTeacher,
    includeRoom = includeRoom,
    includeNote = includeNote,
)

private fun fromDto(dto: TimetableBackupDto): Timetable = Timetable(
    id = dto.id,
    name = dto.name,
    firstDayEpochDay = dto.firstDayEpochDay,
    maxWeek = dto.maxWeek,
    timeTable = ScheduleTimeTable(
        id = dto.timeTable.id,
        name = dto.timeTable.name,
        nodes = dto.timeTable.nodes.map { node ->
            TimeTableNode(
                node = node.node,
                startMinuteOfDay = node.startMinuteOfDay,
                endMinuteOfDay = node.endMinuteOfDay,
            )
        },
    ),
    courses = dto.courses.map { course ->
        Course(
            id = course.id,
            name = course.name,
            color = course.color,
            note = course.note,
            credit = course.credit,
            slots = course.slots.map { slot ->
                LogicalCourseSlot(
                    id = slot.id,
                    courseId = slot.courseId,
                    dayOfWeek = slot.dayOfWeek,
                    startNode = slot.startNode,
                    nodeCount = slot.nodeCount,
                    teacher = slot.teacher,
                    room = slot.room,
                    customTime = slot.customTime?.toDomain(),
                    recurrenceSegments = slot.recurrenceSegments.map { segment ->
                        RecurrenceSegment(
                            id = segment.id,
                            startWeek = segment.startWeek,
                            endWeek = segment.endWeek,
                            weekPattern = segment.weekPattern,
                            dayOfWeek = segment.dayOfWeek,
                            startNode = segment.startNode,
                            nodeCount = segment.nodeCount,
                            teacher = segment.teacher,
                            room = segment.room,
                            customTime = segment.customTime?.toDomain(),
                        )
                    },
                )
            },
        )
    },
    dateExceptions = dto.dateExceptions.map { exception ->
        DateException(
            id = exception.id,
            logicalSlotId = exception.logicalSlotId,
            originalEpochDay = exception.originalEpochDay,
            type = exception.type,
            recurrenceSegmentId = exception.recurrenceSegmentId,
            targetEpochDay = exception.targetEpochDay,
            targetDayOfWeek = exception.targetDayOfWeek,
            targetStartNode = exception.targetStartNode,
            targetNodeCount = exception.targetNodeCount,
            targetCustomTime = exception.targetCustomTime?.toDomain(),
            targetTeacher = exception.targetTeacher,
            targetRoom = exception.targetRoom,
        )
    },
    conflictPreferences = dto.conflictPreferences.map { preference ->
        ConflictPreference(
            courseId = preference.courseId,
            priority = preference.priority,
            logicalSlotId = preference.logicalSlotId,
            occurrenceId = preference.occurrenceId,
            epochDay = preference.epochDay,
        )
    },
    reminderSettings = ReminderSettings(
        startEnabled = dto.reminderSettings.startEnabled,
        endEnabled = dto.reminderSettings.endEnabled,
        startLeadMinutes = dto.reminderSettings.startLeadMinutes,
        endLeadMinutes = dto.reminderSettings.endLeadMinutes,
        content = ReminderContentSettings(
            includeCourseName = dto.reminderSettings.content.includeCourseName,
            includeTeacher = dto.reminderSettings.content.includeTeacher,
            includeRoom = dto.reminderSettings.content.includeRoom,
            includeNote = dto.reminderSettings.content.includeNote,
        ),
        vibrate = dto.reminderSettings.vibrate,
        silent = dto.reminderSettings.silent,
    ),
    sortOrder = dto.sortOrder,
    showSaturday = dto.showSaturday,
    showSunday = dto.showSunday,
    sundayFirst = dto.sundayFirst,
)

private fun MinuteRangeBackupDto.toDomain(): MinuteRange = MinuteRange(
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
)

object BackupImportPolicy {
    @Throws(Exception::class)
    fun apply(
        current: Timetable?,
        imported: Timetable,
        mode: BackupImportMode,
    ): BackupImportResult {
        current?.let { TimeTableValidator.requireValid(it, "Current timetable") }
        TimeTableValidator.requireValid(imported, "Imported timetable")
        return when (mode) {
            BackupImportMode.REPLACE -> BackupImportResult(timetable = imported)
            BackupImportMode.MERGE -> current?.let { merge(it, imported) }
                ?: BackupImportResult(timetable = imported)
        }
    }

    @Throws(Exception::class)
    fun replace(imported: Timetable): BackupImportResult {
        TimeTableValidator.requireValid(imported, "Imported timetable")
        return BackupImportResult(timetable = imported)
    }

    @Throws(Exception::class)
    fun merge(current: Timetable, imported: Timetable): BackupImportResult {
        TimeTableValidator.requireValid(current, "Current timetable")
        TimeTableValidator.requireValid(imported, "Imported timetable")

        val mergedAnchor = minOf(current.firstDayEpochDay, imported.firstDayEpochDay)
        val rebasedCurrent = rebaseForMerge(current, mergedAnchor)
        val normalizedImported = try {
            val normalized = normalizeForTimeTable(imported, current.timeTable)
            TimeTableValidator.requireValid(normalized, "Normalized imported timetable")
        } catch (error: TimetableValidationException) {
            throw error.asBackupFormatException("Imported timetable cannot be represented by the current time table")
        }
        val rebasedImported = rebaseForMerge(normalizedImported, mergedAnchor)
        val unionEndEpochDay = maxOf(
            current.coverageRange.endEpochDay,
            imported.coverageRange.endEpochDay,
        )
        val mergedMaxWeek = maxOf(
            1,
            (floorDiv(unionEndEpochDay - mergedAnchor, 7L) + 1L).toInt(),
        )
        val remapper = IdRemapper(rebasedCurrent, rebasedImported)
        val remapped = remapper.remapTimetable()
        val occurrenceMap = remapper.occurrenceIdMap(rebasedImported, remapped)
        val importedPreferences = remapped.conflictPreferences.map { preference ->
            if (preference.occurrenceId == null) {
                preference
            } else {
                val newOccurrenceId = occurrenceMap[preference.occurrenceId]
                    ?: throw BackupFormatException(
                        "Merged backup conflict preference references an occurrence that cannot be remapped: ${preference.occurrenceId}",
                    )
                preference.copy(occurrenceId = newOccurrenceId)
            }
        }
        val merged = rebasedCurrent.copy(
            firstDayEpochDay = mergedAnchor,
            maxWeek = mergedMaxWeek,
            courses = rebasedCurrent.courses + remapped.courses,
            dateExceptions = rebasedCurrent.dateExceptions + remapped.dateExceptions,
            conflictPreferences = rebasedCurrent.conflictPreferences + importedPreferences,
            timeTable = current.timeTable,
        )
        try {
            TimeTableValidator.requireValid(merged, "Merged timetable")
        } catch (error: TimetableValidationException) {
            throw error.asBackupFormatException("Merged timetable")
        }
        return BackupImportResult(
            timetable = merged,
            remappedIds = remapper.remaps,
        )
    }
}

private class IdRemapper(
    private val current: Timetable,
    private val imported: Timetable,
) {
    val remaps = mutableListOf<BackupIdRemap>()

    private val usedIds = entityIds(current).toMutableSet()
    private val reservedImportedIds = entityIds(imported)

    private val courseIdMap = linkedMapOf<String, String>()
    private val slotIdMap = linkedMapOf<String, String>()
    private val segmentIdMap = linkedMapOf<String, String>()
    private val exceptionIdMap = linkedMapOf<String, String>()

    fun remapTimetable(): Timetable {
        val courses = imported.courses
            .map { course ->
                val courseId = remap("course", course.id, courseIdMap)
                course.copy(
                    id = courseId,
                    slots = course.slots.map { slot ->
                        val slotId = remap("slot", slot.id, slotIdMap)
                        slot.copy(
                            id = slotId,
                            courseId = courseId,
                            recurrenceSegments = slot.recurrenceSegments.map { segment ->
                                segment.copy(id = remap("segment", segment.id, segmentIdMap))
                            },
                        )
                    },
                )
            }
        val exceptions = imported.dateExceptions
            .map { exception ->
                exception.copy(
                    id = remap("exception", exception.id, exceptionIdMap),
                    logicalSlotId = slotIdMap[exception.logicalSlotId] ?: exception.logicalSlotId,
                    recurrenceSegmentId = exception.recurrenceSegmentId?.let { segmentIdMap[it] ?: it },
                )
            }
        val preferences = imported.conflictPreferences.map { preference ->
            preference.copy(
                courseId = courseIdMap[preference.courseId] ?: preference.courseId,
                logicalSlotId = preference.logicalSlotId?.let { slotIdMap[it] ?: it },
            )
        }
        return imported.copy(
            courses = courses,
            dateExceptions = exceptions,
            conflictPreferences = preferences,
        )
    }

    fun occurrenceIdMap(
        original: Timetable,
        remapped: Timetable,
    ): Map<String, String> {
        val oldOccurrences = TimetableEngine.expandOccurrences(original)
        val newOccurrences = TimetableEngine.expandOccurrences(remapped)
        val newByKey = newOccurrences.groupBy { occurrence ->
            OccurrenceKey(
                courseId = occurrence.courseId,
                logicalSlotId = occurrence.logicalSlotId,
                recurrenceSegmentId = occurrence.recurrenceSegmentId,
                sourceEpochDay = occurrence.sourceEpochDay,
                epochDay = occurrence.epochDay,
                startMinuteOfDay = occurrence.startMinuteOfDay,
                endMinuteOfDay = occurrence.endMinuteOfDay,
                exceptionId = occurrence.exceptionId,
            )
        }
        val result = linkedMapOf<String, String>()
        oldOccurrences.forEach { occurrence ->
            val key = OccurrenceKey(
                courseId = courseIdMap[occurrence.courseId] ?: occurrence.courseId,
                logicalSlotId = slotIdMap[occurrence.logicalSlotId] ?: occurrence.logicalSlotId,
                recurrenceSegmentId = segmentIdMap[occurrence.recurrenceSegmentId] ?: occurrence.recurrenceSegmentId,
                sourceEpochDay = occurrence.sourceEpochDay,
                epochDay = occurrence.epochDay,
                startMinuteOfDay = occurrence.startMinuteOfDay,
                endMinuteOfDay = occurrence.endMinuteOfDay,
                exceptionId = occurrence.exceptionId?.let { exceptionIdMap[it] ?: it },
            )
            val candidates = newByKey[key].orEmpty()
            when (candidates.size) {
                1 -> result[occurrence.id] = candidates.single().id
                0 -> throw BackupFormatException(
                    "Merged backup could not remap occurrence '${occurrence.id}' without changing its absolute date or time",
                )
                else -> throw BackupFormatException(
                    "Merged backup occurrence '${occurrence.id}' maps to multiple occurrences",
                )
            }
        }
        return result
    }

    private fun remap(
        namespace: String,
        oldId: String,
        mapping: MutableMap<String, String>,
    ): String {
        val existing = mapping[oldId]
        if (existing != null) return existing

        val candidate = if (oldId !in usedIds) {
            oldId
        } else {
            var suffix = 2
            var generated: String
            do {
                generated = "$oldId~import-$suffix"
                suffix++
            } while (generated in usedIds || generated in reservedImportedIds)
            generated
        }
        usedIds += candidate
        if (candidate != oldId) {
            remaps += BackupIdRemap(namespace = namespace, oldId = oldId, newId = candidate)
        }
        mapping[oldId] = candidate
        return candidate
    }
}

private fun entityIds(timetable: Timetable): Set<String> {
    val ids = mutableSetOf<String>()
    timetable.courses.forEach { course ->
        ids += course.id
        course.slots.forEach { slot ->
            ids += slot.id
            slot.recurrenceSegments.forEach { segment -> ids += segment.id }
        }
    }
    timetable.dateExceptions.forEach { exception -> ids += exception.id }
    return ids
}

private data class OccurrenceKey(
    val courseId: String,
    val logicalSlotId: String,
    val recurrenceSegmentId: String,
    val sourceEpochDay: Long,
    val epochDay: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val exceptionId: String?,
)

private fun rebaseForMerge(timetable: Timetable, newAnchor: Long): Timetable {
    val dayDelta = timetable.firstDayEpochDay - newAnchor
    if (dayDelta % 7L != 0L) {
        throw BackupFormatException("Cannot merge timetables whose semester anchors are not aligned Mondays")
    }
    val weekDelta = (dayDelta / 7L).toInt()
    val oldEnd = timetable.coverageRange.endEpochDay
    val newMaxWeek = (floorDiv(oldEnd - newAnchor, 7L) + 1L).toInt()
    return timetable.copy(
        firstDayEpochDay = newAnchor,
        maxWeek = maxOf(1, newMaxWeek),
        courses = timetable.courses.map { course ->
            course.copy(
                slots = course.slots.map { slot ->
                    slot.copy(
                        recurrenceSegments = slot.recurrenceSegments.map { segment ->
                            segment.copy(
                                startWeek = segment.startWeek + weekDelta,
                                endWeek = segment.endWeek + weekDelta,
                                weekPattern = segment.weekPattern.rebasedForWeekDelta(weekDelta),
                            )
                        },
                    )
                },
            )
        },
    )
}

private fun WeekPattern.rebasedForWeekDelta(weekDelta: Int): WeekPattern =
    if (weekDelta % 2 == 0) {
        this
    } else {
        when (this) {
            WeekPattern.ODD -> WeekPattern.EVEN
            WeekPattern.EVEN -> WeekPattern.ODD
            WeekPattern.ALL -> WeekPattern.ALL
        }
    }

private fun normalizeForTimeTable(
    timetable: Timetable,
    targetTimeTable: TimeTable,
): Timetable {
    val sourceOccurrences = TimetableEngine.recurringOccurrences(timetable)
    val normalizedCourses = timetable.courses.map { course ->
        course.copy(
            slots = course.slots.map { slot ->
                normalizeSlot(timetable, targetTimeTable, slot)
            },
        )
    }
    val normalizedExceptions = timetable.dateExceptions.map { exception ->
        normalizeException(timetable, targetTimeTable, sourceOccurrences, exception)
    }
    return timetable.copy(
        courses = normalizedCourses,
        dateExceptions = normalizedExceptions,
    )
}

private fun normalizeSlot(
    sourceTimetable: Timetable,
    targetTimeTable: TimeTable,
    slot: LogicalCourseSlot,
): LogicalCourseSlot {
    val sourceSlotRange = effectiveRange(
        customTime = slot.customTime,
        timeTable = sourceTimetable.timeTable,
        startNode = slot.startNode,
        nodeCount = slot.nodeCount,
    ) ?: throw BackupFormatException(
        "Imported slot '${slot.id}' has no resolvable source time range",
    )
    val targetSlotRange = targetTimeTable.rangeForNodes(slot.startNode, slot.nodeCount)
    val normalizedSlotStart = if (targetSlotRange == null) 1 else slot.startNode
    val normalizedSlotCount = if (targetSlotRange == null) 1 else slot.nodeCount
    val normalizedSlotCustomTime = when {
        slot.customTime != null -> slot.customTime
        targetSlotRange == null || targetSlotRange != sourceSlotRange -> sourceSlotRange
        else -> null
    }
    val normalizedSlot = slot.copy(
        startNode = normalizedSlotStart,
        nodeCount = normalizedSlotCount,
        customTime = normalizedSlotCustomTime,
    )
    return normalizedSlot.copy(
        recurrenceSegments = slot.recurrenceSegments.map { segment ->
            normalizeSegment(
                sourceTimetable = sourceTimetable,
                targetTimeTable = targetTimeTable,
                sourceSlot = slot,
                targetSlot = normalizedSlot,
                segment = segment,
            )
        },
    )
}

private fun normalizeSegment(
    sourceTimetable: Timetable,
    targetTimeTable: TimeTable,
    sourceSlot: LogicalCourseSlot,
    targetSlot: LogicalCourseSlot,
    segment: RecurrenceSegment,
): RecurrenceSegment {
    val sourceStartNode = segment.startNode ?: sourceSlot.startNode
    val sourceNodeCount = segment.nodeCount ?: sourceSlot.nodeCount
    val sourceRange = effectiveRange(
        customTime = segment.customTime ?: sourceSlot.customTime,
        timeTable = sourceTimetable.timeTable,
        startNode = sourceStartNode,
        nodeCount = sourceNodeCount,
    ) ?: throw BackupFormatException(
        "Imported recurrence segment '${segment.id}' has no resolvable source time range",
    )
    val targetStartNode = segment.startNode ?: targetSlot.startNode
    val targetNodeCount = segment.nodeCount ?: targetSlot.nodeCount
    val targetRange = targetTimeTable.rangeForNodes(targetStartNode, targetNodeCount)
    val targetSpanValid = targetRange != null
    val normalizedStartNode = if (targetSpanValid) targetStartNode else 1
    val normalizedNodeCount = if (targetSpanValid) targetNodeCount else 1
    val inheritedTargetTime = targetSlot.customTime
    val normalizedCustomTime = when {
        segment.customTime != null -> segment.customTime
        !targetSpanValid -> sourceRange
        inheritedTargetTime != null && inheritedTargetTime != sourceRange -> sourceRange
        targetRange != sourceRange -> sourceRange
        else -> null
    }
    return segment.copy(
        startNode = if (segment.startNode != null || !targetSpanValid) normalizedStartNode else null,
        nodeCount = if (segment.nodeCount != null || !targetSpanValid) normalizedNodeCount else null,
        customTime = normalizedCustomTime,
    )
}

private fun normalizeException(
    sourceTimetable: Timetable,
    targetTimeTable: TimeTable,
    sourceOccurrences: List<com.letr.sleepdown.domain.CourseOccurrence>,
    exception: DateException,
): DateException {
    if (exception.type != DateExceptionType.RESCHEDULE) return exception
    val sourceOccurrence = sourceOccurrences.singleOrNull { occurrence ->
        occurrence.logicalSlotId == exception.logicalSlotId &&
            occurrence.sourceEpochDay == exception.originalEpochDay &&
            (exception.recurrenceSegmentId == null || occurrence.recurrenceSegmentId == exception.recurrenceSegmentId)
    } ?: return exception

    val hasNodeOverride = exception.targetStartNode != null || exception.targetNodeCount != null
    if (!hasNodeOverride) return exception

    val sourceTargetStartNode = exception.targetStartNode ?: sourceOccurrence.startNode
    val sourceTargetNodeCount = exception.targetNodeCount ?: sourceOccurrence.nodeCount
    val sourceTargetRange = exception.targetCustomTime
        ?: sourceTimetable.timeTable.rangeForNodes(sourceTargetStartNode, sourceTargetNodeCount)
        ?: sourceOccurrence.minuteRange
    val targetRange = targetTimeTable.rangeForNodes(sourceTargetStartNode, sourceTargetNodeCount)
    val normalizedStartNode = if (targetRange == null) 1 else sourceTargetStartNode
    val normalizedNodeCount = if (targetRange == null) 1 else sourceTargetNodeCount
    val normalizedCustomTime = when {
        exception.targetCustomTime != null -> exception.targetCustomTime
        targetRange == null || targetRange != sourceTargetRange -> sourceTargetRange
        else -> null
    }
    return exception.copy(
        targetStartNode = normalizedStartNode,
        targetNodeCount = normalizedNodeCount,
        targetCustomTime = normalizedCustomTime,
    )
}

private fun effectiveRange(
    customTime: MinuteRange?,
    timeTable: TimeTable,
    startNode: Int,
    nodeCount: Int,
): MinuteRange? = customTime ?: timeTable.rangeForNodes(startNode, nodeCount)
