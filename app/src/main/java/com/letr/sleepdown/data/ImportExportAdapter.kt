package com.letr.sleepdown.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimeTableValidator
import com.letr.sleepdown.domain.WeekPattern
import com.letr.sleepdown.interchange.BackupFormat
import com.letr.sleepdown.interchange.BackupImportMode
import com.letr.sleepdown.interchange.IcsCodec
import com.letr.sleepdown.interchange.IcsExportOptions
import com.letr.sleepdown.interchange.WakeUpBackupImporter
import com.letr.sleepdown.logic.CsvParser
import java.io.IOException
import java.time.Clock
import java.time.ZoneOffset

/** The supported file representations for timetable interchange. */
enum class ImportFormat {
    CSV,
    JSON,
    WAKE_UP,
    ICS;

    companion object {
        /** Alias for callers that use WakeUp's compact spelling. */
        val WAKEUP: ImportFormat
            get() = WAKE_UP

        /** Alias for the application's versioned JSON backup. */
        val SLEEP_DOWN_JSON: ImportFormat
            get() = JSON
    }
}

typealias ExportFormat = ImportFormat

enum class ImportTarget {
    CREATE_NEW,
    MERGE_CURRENT,
    REPLACE_CURRENT;

    companion object {
        val CREATE: ImportTarget
            get() = CREATE_NEW
        val MERGE: ImportTarget
            get() = MERGE_CURRENT
        val REPLACE: ImportTarget
            get() = REPLACE_CURRENT
    }
}

/** Options shared by the text importers. */
data class ImportOptions(
    val name: String? = null,
    val firstDayEpochDay: Long? = null,
    val timeTable: ScheduleTimeTable? = null,
    /** Alias for firstDayEpochDay used by the legacy CSV API. */
    val startDate: Long? = null,
) {
    val resolvedFirstDayEpochDay: Long?
        get() = firstDayEpochDay ?: startDate
}

data class ImportResult(
    val tableId: Long,
    val timetable: Timetable,
) {
    val importedTableId: Long
        get() = tableId
}

/**
 * Android-side file adapter. Parsing and import policy remain in the shared
 * interchange layer; this class only bridges ContentResolver and Room.
 */
class ImportExportAdapter(
    private val repository: TimetableRepository,
    private val contentResolver: ContentResolver,
    private val clock: Clock = Clock.systemUTC(),
) {
    constructor(
        context: Context,
        repository: TimetableRepository = TimetableRepository(context),
        clock: Clock = Clock.systemUTC(),
    ) : this(repository, context.contentResolver, clock)

    constructor(
        contentResolver: ContentResolver,
        repository: TimetableRepository,
        clock: Clock = Clock.systemUTC(),
    ) : this(repository, contentResolver, clock)

    suspend fun detectFormat(uri: Uri): ImportFormat? = withContext(Dispatchers.IO) {
        val name = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
        } else uri.lastPathSegment
        val extension = name?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        when (extension) {
            "csv" -> ImportFormat.CSV
            "json" -> ImportFormat.JSON
            "ics" -> ImportFormat.ICS
            "wakeup_schedule", "wakeup", "wt" -> ImportFormat.WAKE_UP
            else -> when (contentResolver.getType(uri)?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)) {
                "text/csv", "text/comma-separated-values", "application/vnd.ms-excel" -> ImportFormat.CSV
                "application/json" -> ImportFormat.JSON
                "text/calendar", "application/ics" -> ImportFormat.ICS
                else -> null
            }
        }
    }

    /** Imports a resolver-backed file into the requested timetable target. */
    suspend fun importFile(
        uri: Uri,
        format: ImportFormat,
        target: ImportTarget,
        currentTableId: Long? = null,
        options: ImportOptions = ImportOptions(),
    ): ImportResult {
        when (target) {
            ImportTarget.CREATE_NEW -> Unit
            ImportTarget.MERGE_CURRENT,
            ImportTarget.REPLACE_CURRENT -> repository.loadDomainTimetable(requireCurrentTableId(currentTableId))
        }
        val imported = parse(format, readText(uri, format), options)
        val tableId = when (target) {
            ImportTarget.CREATE_NEW -> repository.importDomainTimetable(imported)
            ImportTarget.MERGE_CURRENT -> {
                val id = requireCurrentTableId(currentTableId)
                repository.importDomainTimetable(
                    tableId = id,
                    imported = imported,
                    mode = BackupImportMode.MERGE,
                )
                id
            }
            ImportTarget.REPLACE_CURRENT -> {
                val id = requireCurrentTableId(currentTableId)
                repository.importDomainTimetable(
                    tableId = id,
                    imported = imported,
                    mode = BackupImportMode.REPLACE,
                )
                id
            }
        }
        return ImportResult(
            tableId = tableId,
            timetable = repository.loadDomainTimetable(tableId),
        )
    }

    suspend fun importFromUri(
        uri: Uri,
        format: ImportFormat,
        target: ImportTarget,
        currentTableId: Long? = null,
        options: ImportOptions = ImportOptions(),
    ): ImportResult = importFile(uri, format, target, currentTableId, options)

    suspend fun importCsv(
        uri: Uri,
        target: ImportTarget,
        startDate: Long,
        currentTableId: Long? = null,
        name: String? = null,
        timeTable: ScheduleTimeTable? = null,
    ): ImportResult = importFile(
        uri = uri,
        format = ImportFormat.CSV,
        target = target,
        currentTableId = currentTableId,
        options = ImportOptions(name = name, firstDayEpochDay = startDate, timeTable = timeTable),
    )

    suspend fun importJson(
        uri: Uri,
        target: ImportTarget,
        currentTableId: Long? = null,
    ): ImportResult = importFile(uri, ImportFormat.JSON, target, currentTableId)

    suspend fun importWakeUp(
        uri: Uri,
        target: ImportTarget,
        currentTableId: Long? = null,
        name: String? = null,
    ): ImportResult = importFile(
        uri = uri,
        format = ImportFormat.WAKE_UP,
        target = target,
        currentTableId = currentTableId,
        options = ImportOptions(name = name),
    )

    suspend fun importIcs(
        uri: Uri,
        target: ImportTarget,
        currentTableId: Long? = null,
        name: String? = null,
        firstDayEpochDay: Long? = null,
        timeTable: ScheduleTimeTable? = null,
    ): ImportResult = importFile(
        uri = uri,
        format = ImportFormat.ICS,
        target = target,
        currentTableId = currentTableId,
        options = ImportOptions(
            name = name,
            firstDayEpochDay = firstDayEpochDay,
            timeTable = timeTable,
        ),
    )

    /** Writes an editable UTF-8 CSV with examples accepted by the course importer. */
    suspend fun exportCsvTemplate(uri: Uri) = writeText(
        uri,
        "\uFEFF课程名称,星期,开始节数,结束节数,老师,地点,周数\r\n" +
            "高等数学,1,1,2,张老师,教学楼101,1-16\r\n" +
            "大学英语,3,3,4,李老师,教学楼203,1-16单\r\n" +
            "程序设计,5,5,6,王老师,实验楼301,\"2-8双,10,12\"\r\n",
    )

    /** Exports JSON or ICS to a resolver-backed destination. */
    suspend fun exportFile(
        uri: Uri,
        format: ImportFormat,
        tableId: Long,
        icsOptions: IcsExportOptions? = null,
        range: EpochDayRange? = null,
    ) {
        val text = when (format) {
            ImportFormat.JSON -> repository.exportBackup(tableId)
            ImportFormat.ICS -> {
                val timetable = repository.loadDomainTimetable(tableId)
                IcsCodec.export(
                    timetable = timetable,
                    range = range ?: timetable.coverageRange,
                    options = icsOptions ?: defaultIcsExportOptions(),
                )
            }
            ImportFormat.CSV,
            ImportFormat.WAKE_UP -> throw IllegalArgumentException("Export is not supported for $format")
        }
        writeText(uri, text)
    }

    suspend fun exportToUri(
        uri: Uri,
        format: ImportFormat,
        tableId: Long,
        icsOptions: IcsExportOptions? = null,
        range: EpochDayRange? = null,
    ) = exportFile(uri, format, tableId, icsOptions, range)

    suspend fun exportJson(uri: Uri, tableId: Long) =
        exportFile(uri, ImportFormat.JSON, tableId)

    suspend fun exportIcs(
        uri: Uri,
        tableId: Long,
        icsOptions: IcsExportOptions? = null,
        range: EpochDayRange? = null,
    ) = exportFile(uri, ImportFormat.ICS, tableId, icsOptions, range)

    private fun parse(format: ImportFormat, text: String, options: ImportOptions): Timetable {
        val normalizedText = text.removePrefix("\uFEFF")
        return when (format) {
            ImportFormat.JSON -> parseJson(normalizedText)
            ImportFormat.CSV -> parseCsv(normalizedText, options)
            ImportFormat.WAKE_UP -> {
                val imported = WakeUpBackupImporter.importToTimetable(normalizedText)
                options.name?.trim()?.takeIf { it.isNotEmpty() }
                    ?.let { imported.copy(name = it) }
                    ?: imported
            }
            ImportFormat.ICS -> IcsCodec.toTimetable(
                text = normalizedText,
                timetableId = "ics-import",
                name = options.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Imported calendar",
                firstDayEpochDay = options.resolvedFirstDayEpochDay,
                timeTable = options.timeTable ?: ScheduleTimeTable(),
            )
        }
    }

    private fun parseJson(text: String): Timetable {
        val sharedError = runCatching { BackupFormat.decode(text) }.exceptionOrNull()
        if (sharedError == null) return BackupFormat.decode(text)

        return try {
            val legacy = BackupCodec.decode(text)
            val mapped = DomainMappers.toDomain(
                TimetableEntityRows(
                    table = legacy.table,
                    courses = legacy.courses,
                    courseTimes = legacy.times,
                    nodeTimes = legacy.nodeTimes,
                ),
            )
            val nodeLimit = legacy.table.nodeCount.coerceIn(1, TimetableRepository.MAX_NODE_COUNT)
            val validNodes = mapped.timeTable.nodes
                .take(nodeLimit)
                .takeWhile { it.minuteRange.isValid }
            require(validNodes.isNotEmpty()) { "Legacy JSON backup has no valid timetable nodes" }
            val timetable = mapped.copy(timeTable = mapped.timeTable.copy(nodes = validNodes))
            TimeTableValidator.requireValid(timetable, "Decoded JSON backup")
        } catch (legacyError: Throwable) {
            legacyError.addSuppressed(sharedError)
            throw legacyError
        }
    }

    private fun parseCsv(text: String, options: ImportOptions): Timetable {
        val firstDayEpochDay = options.resolvedFirstDayEpochDay
            ?: throw IllegalArgumentException("CSV import requires firstDayEpochDay")
        val rows = CsvParser.parse(text, firstDayEpochDay).getOrThrow()
        val timeTable = options.timeTable ?: defaultCsvTimeTable()
        val maxWeek = maxOf(
            20,
            rows.flatMap { row -> row.weekSegments }.maxOfOrNull { it.end } ?: 1,
        )
        val courses = rows.groupBy { it.name }.entries.mapIndexed { courseIndex, (courseName, courseRows) ->
            val courseId = "csv-course:${courseIndex + 1}"
            val slots = courseRows
                .groupBy { row ->
                    CsvSlotKey(
                        day = row.day,
                        startNode = row.startNode,
                        endNode = row.endNode,
                        teacher = row.teacher,
                        room = row.room,
                    )
                }
                .entries
                .sortedWith(compareBy({ it.key.day }, { it.key.startNode }, { it.key.endNode }, { it.key.teacher }, { it.key.room }))
                .mapIndexed { slotIndex, (_, slotRows) ->
                    val first = slotRows.first()
                    val slotId = "$courseId:slot:${slotIndex + 1}"
                    val segments = slotRows
                        .flatMap { row ->
                            row.weekSegments.map { segment -> row to segment }
                        }
                        .sortedWith(compareBy({ it.second.start }, { it.second.end }, { it.second.weekType }))
                        .mapIndexed { segmentIndex, (row, segment) ->
                            RecurrenceSegment(
                                id = "$slotId:segment:${segmentIndex + 1}",
                                startWeek = segment.start,
                                endWeek = segment.end,
                                weekPattern = when (segment.weekType) {
                                    1 -> WeekPattern.ODD
                                    2 -> WeekPattern.EVEN
                                    else -> WeekPattern.ALL
                                },
                                dayOfWeek = row.day,
                                startNode = row.startNode,
                                nodeCount = row.endNode - row.startNode + 1,
                                teacher = row.teacher,
                                room = row.room,
                            )
                        }
                    LogicalCourseSlot(
                        id = slotId,
                        courseId = courseId,
                        dayOfWeek = first.day,
                        startNode = first.startNode,
                        nodeCount = first.endNode - first.startNode + 1,
                        teacher = first.teacher,
                        room = first.room,
                        recurrenceSegments = segments,
                    )
                }
            Course(
                id = courseId,
                name = courseName,
                slots = slots,
            )
        }
        val timetable = Timetable(
            id = "csv-import",
            name = options.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Imported CSV",
            firstDayEpochDay = firstDayEpochDay,
            maxWeek = maxWeek,
            timeTable = timeTable,
            courses = courses,
        )
        return TimeTableValidator.requireValid(timetable, "CSV import")
    }

    private fun defaultCsvTimeTable(): ScheduleTimeTable {
        val nodes = TimetableRepository.defaultNodeTimes(0L).mapNotNull { row ->
            val start = MinuteOfDay.parse(row.start) ?: return@mapNotNull null
            val end = MinuteOfDay.parse(row.end) ?: return@mapNotNull null
            if (start >= end) return@mapNotNull null
            TimeTableNode(row.node, start, end)
        }
        return ScheduleTimeTable(id = "csv-default", name = "CSV", nodes = nodes)
    }

    private fun defaultIcsExportOptions(): IcsExportOptions {
        val utc = clock.instant().atZone(ZoneOffset.UTC)
        return IcsExportOptions(
            dtStampEpochDay = utc.toLocalDate().toEpochDay(),
            dtStampMinuteOfDay = utc.hour * 60 + utc.minute,
        )
    }

    private fun requireCurrentTableId(currentTableId: Long?): Long =
        currentTableId?.takeIf { it > 0L }
            ?: throw IllegalArgumentException("${ImportTarget.MERGE_CURRENT} and ${ImportTarget.REPLACE_CURRENT} require currentTableId")

    private fun readText(uri: Uri, format: ImportFormat): String = try {
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("ContentResolver returned no input stream for $uri")
        val bytes = input.use { it.readBytes() }
        if (format == ImportFormat.CSV && !looksLikeUtf8(bytes)) {
            String(bytes, charset("GBK"))
        } else {
            String(bytes, Charsets.UTF_8)
        }
    } catch (error: IOException) {
        throw IOException("Failed to read import file $uri", error)
    } catch (error: SecurityException) {
        throw IOException("Permission denied while reading import file $uri", error)
    }

    private fun looksLikeUtf8(bytes: ByteArray): Boolean = runCatching {
        Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes))
        true
    }.getOrDefault(false)

    private fun writeText(uri: Uri, text: String) {
        try {
            val output = contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("ContentResolver returned no output stream for $uri")
            output.use {
                it.write(text.toByteArray(Charsets.UTF_8))
                it.flush()
            }
        } catch (error: IOException) {
            throw IOException("Failed to write export file $uri", error)
        } catch (error: SecurityException) {
            throw IOException("Permission denied while writing export file $uri", error)
        }
    }

    private data class CsvSlotKey(
        val day: Int,
        val startNode: Int,
        val endNode: Int,
        val teacher: String,
        val room: String,
    )
}
