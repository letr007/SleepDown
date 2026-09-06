package com.letr.sleepdown.interchange

import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.TimeTable
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.TimeTableValidator
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.TimetableValidationException
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.floorDiv
import com.letr.sleepdown.domain.floorMod

/** Export options for values that have a defined UTC meaning. */
data class IcsExportOptions(
    /** Already-UTC DTSTAMP date; no timezone conversion is performed. */
    val dtStampEpochDay: Long,
    val dtStampMinuteOfDay: Int,
) {
    init {
        require(dtStampMinuteOfDay in 0 until 24 * 60) {
            "dtStampMinuteOfDay must be between 0 and ${24 * 60 - 1}"
        }
    }
}

/** One VEVENT with either floating local times or DATE-valued boundaries. */
data class IcsEvent(
    val uid: String,
    val startEpochDay: Long,
    val startMinuteOfDay: Int?,
    val endEpochDay: Long,
    val endMinuteOfDay: Int?,
    val summary: String,
    val location: String = "",
    val description: String = "",
    val isDateEvent: Boolean = startMinuteOfDay == null && endMinuteOfDay == null,
)

data class IcsCalendar(
    val events: List<IcsEvent>,
)

class IcsFormatException(message: String) : IllegalArgumentException(message)

/** RFC 5545-oriented, timezone-neutral calendar interchange. */
object IcsCodec {
    const val PRODUCT_ID: String = "-//SleepDown//Timetable//EN"

    @Throws(Exception::class)
    fun export(
        timetable: Timetable,
        range: EpochDayRange = timetable.coverageRange,
        options: IcsExportOptions,
    ): String {
        TimeTableValidator.requireValid(timetable, "ICS export")
        val occurrences = TimetableEngine.expandOccurrences(timetable, range)
        val lines = mutableListOf<String>()
        lines += "BEGIN:VCALENDAR"
        lines += "VERSION:2.0"
        lines += "PRODID:${escapeText(PRODUCT_ID)}"
        lines += "CALSCALE:GREGORIAN"
        occurrences.forEach { occurrence ->
            lines += "BEGIN:VEVENT"
            lines += "DTSTAMP:${formatBasicDate(options.dtStampEpochDay)}T${formatIcsTime(options.dtStampMinuteOfDay)}Z"
            if (occurrence.startMinuteOfDay == 0 && occurrence.endMinuteOfDay == 24 * 60) {
                lines += "DTSTART;VALUE=DATE:${formatBasicDate(occurrence.epochDay)}"
                lines += "DTEND;VALUE=DATE:${formatBasicDate(occurrence.epochDay + 1L)}"
            } else {
                lines += "DTSTART:${formatBasicDate(occurrence.epochDay)}T${formatIcsTime(occurrence.startMinuteOfDay)}"
                val endEpochDay = if (occurrence.endMinuteOfDay == 24 * 60) occurrence.epochDay + 1L else occurrence.epochDay
                val endMinute = if (occurrence.endMinuteOfDay == 24 * 60) 0 else occurrence.endMinuteOfDay
                lines += "DTEND:${formatBasicDate(endEpochDay)}T${formatIcsTime(endMinute)}"
            }
            lines += "SUMMARY:${escapeText(occurrence.courseName)}"
            lines += "LOCATION:${escapeText(occurrence.room)}"
            lines += "DESCRIPTION:${escapeText(occurrence.note)}"
            lines += "UID:${escapeText(occurrence.id)}"
            lines += "END:VEVENT"
        }
        lines += "END:VCALENDAR"
        return lines
            .flatMap(::foldContentLine)
            .joinToString("\r\n", postfix = "\r\n")
    }

    @Throws(Exception::class)
    fun encode(
        timetable: Timetable,
        range: EpochDayRange = timetable.coverageRange,
        options: IcsExportOptions,
    ): String = export(timetable, range, options)

    @Throws(Exception::class)
    fun parse(text: String): List<IcsEvent> = parseCalendar(text).events

    @Throws(Exception::class)
    fun decode(text: String): List<IcsEvent> = parse(text)

    @Throws(Exception::class)
    fun parseCalendar(text: String): IcsCalendar {
        val lines = unfoldLines(text)
        if (lines.isEmpty()) throw IcsFormatException("ICS input is empty")

        var calendarStarted = false
        var calendarEnded = false
        var currentEvent: MutableMap<String, IcsProperty>? = null
        val calendarProperties = mutableSetOf<String>()
        val events = mutableListOf<IcsEvent>()

        lines.forEachIndexed { index, line ->
            val marker = line.substringBefore(':', missingDelimiterValue = "").uppercase()
            when {
                line.equals("BEGIN:VCALENDAR", ignoreCase = true) -> {
                    if (calendarStarted || calendarEnded) errorAt(index, "duplicate or misplaced BEGIN:VCALENDAR")
                    calendarStarted = true
                }
                line.equals("END:VCALENDAR", ignoreCase = true) -> {
                    if (!calendarStarted || currentEvent != null || calendarEnded) {
                        errorAt(index, "misplaced END:VCALENDAR")
                    }
                    calendarEnded = true
                }
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    if (!calendarStarted || calendarEnded || currentEvent != null) {
                        errorAt(index, "misplaced BEGIN:VEVENT")
                    }
                    currentEvent = linkedMapOf()
                }
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    val properties = currentEvent ?: errorAt(index, "misplaced END:VEVENT")
                    events += parseEvent(properties, index + 1)
                    currentEvent = null
                }
                currentEvent != null -> {
                    if (line.startsWith("BEGIN:", ignoreCase = true) || line.startsWith("END:", ignoreCase = true)) {
                        errorAt(index, "nested components such as VALARM are unsupported")
                    }
                    val property = parseProperty(line, index + 1)
                    if (property.parameters.containsKey("TZID")) {
                        throw IcsFormatException("line ${index + 1}: ${property.name} TZID is unsupported")
                    }
                    if (property.name in unsupportedEventProperties) {
                        throw IcsFormatException(
                            "line ${index + 1}: ${property.name} recurrence or cancellation semantics are unsupported",
                        )
                    }
                    if (currentEvent.containsKey(property.name)) {
                        errorAt(index, "duplicate single-value property ${property.name} in VEVENT")
                    }
                    currentEvent[property.name] = property
                }
                marker.isNotEmpty() && calendarStarted && !calendarEnded -> {
                    val property = parseProperty(line, index + 1)
                    if (property.parameters.containsKey("TZID")) {
                        throw IcsFormatException("line ${index + 1}: ${property.name} TZID is unsupported")
                    }
                    if (property.name == "METHOD" && property.value.equals("CANCEL", ignoreCase = true)) {
                        throw IcsFormatException("line ${index + 1}: METHOD:CANCEL is unsupported")
                    }
                    if (property.name in singleValueCalendarProperties && !calendarProperties.add(property.name)) {
                        errorAt(index, "duplicate single-value property ${property.name} in VCALENDAR")
                    }
                }
                line.isNotBlank() -> errorAt(index, "content outside VCALENDAR")
            }
        }

        if (!calendarStarted || !calendarEnded) {
            throw IcsFormatException("ICS input must contain one complete VCALENDAR")
        }
        if (currentEvent != null) {
            throw IcsFormatException("ICS input ended before END:VEVENT")
        }
        val duplicateUid = events.groupBy { it.uid }.entries.firstOrNull { it.value.size > 1 }?.key
        if (duplicateUid != null) {
            throw IcsFormatException("ICS input contains duplicate UID $duplicateUid")
        }
        return IcsCalendar(events)
    }

    @Throws(Exception::class)
    fun toTimetable(
        text: String,
        timetableId: String = "ics-import",
        name: String = "Imported calendar",
        firstDayEpochDay: Long? = null,
        timeTable: TimeTable = TimeTable(),
    ): Timetable {
        val events = parse(text)
        val earliestEventDay = events.minOfOrNull { it.startEpochDay }
        val firstDay = firstDayEpochDay ?: earliestEventDay?.let(::startOfIsoWeek) ?: startOfIsoWeek(0L)
        if (com.letr.sleepdown.domain.isoDayOfWeek(firstDay) != 1) {
            throw IcsFormatException("firstDayEpochDay must represent a Monday")
        }
        val effectiveTimeTable = timeTable.takeIf { it.nodes.isNotEmpty() }
            ?: TimeTable(
                id = "ics-default",
                name = "ICS",
                nodes = listOf(TimeTableNode(node = 1, startMinuteOfDay = 0, endMinuteOfDay = 24 * 60)),
            )
        val maxEventWeek = events.maxOfOrNull { event ->
            val relativeDay = event.startEpochDay - firstDay
            if (relativeDay < 0L) {
                throw IcsFormatException("VEVENT ${event.uid} occurs before the timetable first day")
            }
            floorDiv(relativeDay, 7L).toInt() + 1
        } ?: 1
        val courses = events
            .sortedWith(compareBy<IcsEvent> { it.startEpochDay }.thenBy { it.startMinuteOfDay ?: -1 }.thenBy { it.uid })
            .map { event ->
                val range = eventRange(event)
                val relativeDay = event.startEpochDay - firstDay
                val week = floorDiv(relativeDay, 7L).toInt() + 1
                val dayOfWeek = (floorMod(relativeDay, 7L) + 1L).toInt()
                val courseId = "ics-course:${event.uid}"
                val slotId = "$courseId:slot"
                Course(
                    id = courseId,
                    name = event.summary.ifBlank { "Untitled event" },
                    note = event.description,
                    slots = listOf(
                        LogicalCourseSlot(
                            id = slotId,
                            courseId = courseId,
                            dayOfWeek = dayOfWeek,
                            startNode = 1,
                            nodeCount = 1,
                            room = event.location,
                            customTime = range,
                            recurrenceSegments = listOf(
                                RecurrenceSegment(
                                    id = "$slotId:segment",
                                    startWeek = week,
                                    endWeek = week,
                                    dayOfWeek = dayOfWeek,
                                    startNode = 1,
                                    nodeCount = 1,
                                    room = event.location,
                                    customTime = range,
                                ),
                            ),
                        ),
                    ),
                )
            }
        val timetable = Timetable(
            id = timetableId,
            name = name,
            firstDayEpochDay = firstDay,
            maxWeek = maxEventWeek,
            timeTable = effectiveTimeTable,
            courses = courses,
        )
        try {
            return TimeTableValidator.requireValid(timetable, "ICS import")
        } catch (error: TimetableValidationException) {
            throw IcsFormatException("ICS import produced an invalid timetable: ${error.message}")
        }
    }

    @Throws(Exception::class)
    fun fromIcs(
        text: String,
        timetableId: String = "ics-import",
        name: String = "Imported calendar",
        firstDayEpochDay: Long? = null,
        timeTable: TimeTable = TimeTable(),
    ): Timetable = toTimetable(text, timetableId, name, firstDayEpochDay, timeTable)

    @Throws(Exception::class)
    fun importInto(
        text: String,
        current: Timetable?,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): BackupImportResult = BackupImportPolicy.apply(current, toTimetable(text), mode)
}

@Throws(Exception::class)
fun exportIcs(
    timetable: Timetable,
    range: EpochDayRange = timetable.coverageRange,
    options: IcsExportOptions,
): String = IcsCodec.export(timetable, range, options)

@Throws(Exception::class)
fun importIcs(
    text: String,
    timetableId: String = "ics-import",
    name: String = "Imported calendar",
): Timetable = IcsCodec.toTimetable(text, timetableId, name)

private data class IcsProperty(
    val name: String,
    val parameters: Map<String, String>,
    val value: String,
)

private data class IcsTemporal(
    val epochDay: Long,
    val minuteOfDay: Int?,
    val isDate: Boolean,
)

private val unsupportedEventProperties = setOf(
    "RRULE",
    "RDATE",
    "EXDATE",
    "RECURRENCE-ID",
)

private val singleValueCalendarProperties = setOf(
    "VERSION",
    "PRODID",
    "CALSCALE",
    "METHOD",
)

private fun parseEvent(properties: Map<String, IcsProperty>, lineNumber: Int): IcsEvent {
    properties.keys.firstOrNull { it in unsupportedEventProperties }?.let { property ->
        throw IcsFormatException("line $lineNumber: $property recurrence or cancellation semantics are unsupported")
    }
    properties["STATUS"]?.let { property ->
        if (property.value.equals("CANCELLED", ignoreCase = true)) {
            throw IcsFormatException("line $lineNumber: STATUS:CANCELLED is unsupported")
        }
        throw IcsFormatException("line $lineNumber: STATUS semantics are unsupported")
    }
    properties["DTSTAMP"]?.let { property -> parseTimestamp(property, lineNumber) }
    val uid = properties.requiredText("UID", lineNumber)
    val start = properties["DTSTART"]?.let { parseTemporal(it, "DTSTART", lineNumber) }
        ?: throw IcsFormatException("line $lineNumber: VEVENT is missing DTSTART")
    val end = properties["DTEND"]?.let { parseTemporal(it, "DTEND", lineNumber) }
        ?: throw IcsFormatException("line $lineNumber: VEVENT is missing DTEND")
    if (start.isDate != end.isDate) {
        throw IcsFormatException("line $lineNumber: DTSTART and DTEND must use the same DATE or floating DATE-TIME form")
    }
    if (start.isDate) {
        if (end.epochDay <= start.epochDay) {
            throw IcsFormatException("line $lineNumber: DATE DTEND must be after DTSTART")
        }
    } else {
        val sameDayInterval = start.epochDay == end.epochDay && end.minuteOfDay!! > start.minuteOfDay!!
        val endsAtMidnight = end.epochDay == start.epochDay + 1L && end.minuteOfDay == 0
        if (!sameDayInterval && !endsAtMidnight) {
            throw IcsFormatException("line $lineNumber: floating DTSTART/DTEND must describe a positive same-day interval or end at next-day midnight")
        }
    }
    return IcsEvent(
        uid = uid,
        startEpochDay = start.epochDay,
        startMinuteOfDay = start.minuteOfDay,
        endEpochDay = end.epochDay,
        endMinuteOfDay = end.minuteOfDay,
        summary = properties["SUMMARY"]?.let { unescapeText(it.value) } ?: "",
        location = properties["LOCATION"]?.let { unescapeText(it.value) } ?: "",
        description = properties["DESCRIPTION"]?.let { unescapeText(it.value) } ?: "",
        isDateEvent = start.isDate,
    )
}

private fun eventRange(event: IcsEvent): MinuteRange {
    if (event.isDateEvent) {
        if (event.endEpochDay != event.startEpochDay + 1L) {
            throw IcsFormatException("VEVENT ${event.uid} spans multiple DATE days and cannot map to one timetable occurrence")
        }
        return MinuteRange(0, 24 * 60)
    }
    val startMinute = event.startMinuteOfDay
        ?: throw IcsFormatException("VEVENT ${event.uid} is missing floating DTSTART time")
    val endMinute = event.endMinuteOfDay
        ?: throw IcsFormatException("VEVENT ${event.uid} is missing floating DTEND time")
    return if (event.endEpochDay == event.startEpochDay + 1L && endMinute == 0) {
        MinuteRange(startMinute, 24 * 60)
    } else {
        MinuteRange(startMinute, endMinute)
    }
}

private fun parseTemporal(
    property: IcsProperty,
    field: String,
    lineNumber: Int,
): IcsTemporal {
    if (property.parameters.containsKey("TZID")) {
        throw IcsFormatException("line $lineNumber: $field TZID is unsupported; use floating local time instead")
    }
    val declaredDate = property.parameters["VALUE"]?.equals("DATE", ignoreCase = true) == true
    if (declaredDate || (property.parameters["VALUE"] == null && property.value.length == 8)) {
        val epochDay = parseBasicDate(property.value)
            ?: throw IcsFormatException("line $lineNumber: $field has invalid DATE value")
        return IcsTemporal(epochDay, null, isDate = true)
    }
    if (property.parameters["VALUE"]?.equals("DATE-TIME", ignoreCase = true) == false) {
        throw IcsFormatException("line $lineNumber: $field has unsupported VALUE parameter")
    }
    if (property.value.endsWith("Z", ignoreCase = true)) {
        throw IcsFormatException("line $lineNumber: $field UTC values are unsupported; use floating local time instead")
    }
    val separator = property.value.indexOf('T')
    if (separator != 8) {
        throw IcsFormatException("line $lineNumber: $field must use YYYYMMDDTHHMM or YYYYMMDDTHHMMSS")
    }
    val epochDay = parseBasicDate(property.value.substring(0, separator))
        ?: throw IcsFormatException("line $lineNumber: $field has invalid calendar date")
    val time = property.value.substring(separator + 1)
    if (time.length != 4 && time.length != 6 || time.any { !it.isDigit() }) {
        throw IcsFormatException("line $lineNumber: $field has invalid local time")
    }
    val hour = time.substring(0, 2).toIntOrNull() ?: -1
    val minute = time.substring(2, 4).toIntOrNull() ?: -1
    val second = if (time.length == 6) time.substring(4, 6).toIntOrNull() ?: 0 else 0
    if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) {
        throw IcsFormatException("line $lineNumber: $field has out-of-range local time")
    }
    if (time.length == 6 && second != 0) {
        throw IcsFormatException("line $lineNumber: $field must not contain nonzero seconds")
    }
    return IcsTemporal(epochDay, hour * 60 + minute, isDate = false)
}

private fun parseTimestamp(property: IcsProperty, lineNumber: Int) {
    if (property.parameters.containsKey("TZID") ||
        property.parameters["VALUE"]?.equals("DATE-TIME", ignoreCase = true) == false
    ) {
        throw IcsFormatException("line $lineNumber: DTSTAMP must be an explicit UTC DATE-TIME")
    }
    val value = property.value
    if (!value.endsWith("Z", ignoreCase = true) || value.length != 16) {
        throw IcsFormatException("line $lineNumber: DTSTAMP must use YYYYMMDDTHHMMSSZ")
    }
    val date = parseBasicDate(value.substring(0, 8))
        ?: throw IcsFormatException("line $lineNumber: DTSTAMP has an invalid date")
    val time = value.substring(9, 15)
    if (value[8] != 'T' || time.any { !it.isDigit() }) {
        throw IcsFormatException("line $lineNumber: DTSTAMP must use YYYYMMDDTHHMMSSZ")
    }
    val hour = time.substring(0, 2).toIntOrNull() ?: -1
    val minute = time.substring(2, 4).toIntOrNull() ?: -1
    val second = time.substring(4, 6).toIntOrNull() ?: -1
    if (hour !in 0..23 || minute !in 0..59 || second != 0) {
        throw IcsFormatException("line $lineNumber: DTSTAMP must have zero seconds")
    }
    date.hashCode()
}

private fun parseProperty(line: String, lineNumber: Int): IcsProperty {
    val colon = line.indexOf(':')
    if (colon <= 0) throw IcsFormatException("line $lineNumber: property must contain a name and colon")
    val nameAndParameters = line.substring(0, colon).split(';')
    val name = nameAndParameters.first().uppercase()
    if (name.isBlank()) throw IcsFormatException("line $lineNumber: property name is empty")
    val parameters = nameAndParameters.drop(1).associate { parameter ->
        val equals = parameter.indexOf('=')
        if (equals <= 0) throw IcsFormatException("line $lineNumber: malformed $name parameter")
        parameter.substring(0, equals).uppercase() to parameter.substring(equals + 1).trim('"')
    }
    return IcsProperty(name, parameters, line.substring(colon + 1))
}

private fun Map<String, IcsProperty>.requiredText(name: String, lineNumber: Int): String {
    val property = this[name] ?: throw IcsFormatException("line $lineNumber: VEVENT is missing $name")
    val value = unescapeText(property.value)
    if (value.isBlank()) throw IcsFormatException("line $lineNumber: $name must not be blank")
    return value
}

private fun unfoldLines(text: String): List<String> {
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    val unfolded = mutableListOf<String>()
    normalized.split('\n').forEachIndexed { index, physical ->
        if (physical.startsWith(" ") || physical.startsWith("\t")) {
            if (unfolded.isEmpty()) {
                throw IcsFormatException("line ${index + 1}: folded line has no preceding content line")
            }
            unfolded[unfolded.lastIndex] += physical.substring(1)
        } else if (physical.isNotEmpty()) {
            unfolded += physical
        }
    }
    return unfolded
}

private fun errorAt(index: Int, message: String): Nothing =
    throw IcsFormatException("line ${index + 1}: $message")

private fun startOfIsoWeek(epochDay: Long): Long =
    epochDay - (com.letr.sleepdown.domain.isoDayOfWeek(epochDay) - 1L)

private fun formatIcsTime(minuteOfDay: Int): String {
    require(minuteOfDay in 0 until 24 * 60) { "ICS time must be within one day" }
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "${hour.toString().padStart(2, '0')}${minute.toString().padStart(2, '0')}00"
}

private fun escapeText(value: String): String = value
    .replace("\\", "\\\\")
    .replace(";", "\\;")
    .replace(",", "\\,")
    .replace("\r\n", "\n")
    .replace('\r', '\n')
    .replace("\n", "\\n")

private fun unescapeText(value: String): String {
    val result = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '\\' && index + 1 < value.length) {
            when (val escaped = value[index + 1]) {
                'n', 'N' -> result.append('\n')
                '\\', ';', ',' -> result.append(escaped)
                else -> result.append(escaped)
            }
            index += 2
        } else {
            result.append(character)
            index++
        }
    }
    return result.toString()
}

private fun foldContentLine(line: String): List<String> {
    if (line.encodeToByteArray().size <= 75) return listOf(line)
    val result = mutableListOf<String>()
    var offset = 0
    var first = true
    while (offset < line.length) {
        val limit = if (first) 75 else 74
        var end = offset
        var bytes = 0
        while (end < line.length) {
            val nextEnd = if (line[end].isHighSurrogate() && end + 1 < line.length && line[end + 1].isLowSurrogate()) {
                end + 2
            } else {
                end + 1
            }
            val nextBytes = line.substring(end, nextEnd).encodeToByteArray().size
            if (bytes + nextBytes > limit) break
            bytes += nextBytes
            end = nextEnd
        }
        if (end == offset) end++
        val segment = line.substring(offset, end)
        result += if (first) segment else " $segment"
        first = false
        offset = end
    }
    return result
}
