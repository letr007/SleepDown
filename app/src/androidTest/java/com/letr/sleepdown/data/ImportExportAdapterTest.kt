package com.letr.sleepdown.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.domain.Course
import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.LogicalCourseSlot
import com.letr.sleepdown.domain.RecurrenceSegment
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.interchange.BackupFormat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class ImportExportAdapterTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: TimetableRepository
    private lateinit var adapter: ImportExportAdapter
    private val files = mutableListOf<File>()

    @Before
    fun setUp() {
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase(DATABASE_NAME)
        repository = TimetableRepository(context)
        adapter = ImportExportAdapter(
            context = context,
            repository = repository,
            clock = Clock.fixed(Instant.parse("2026-09-04T10:15:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() {
        files.forEach { it.delete() }
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    fun detectsSharedDocumentsFromDisplayNameAndMimeRatherThanUriPath() = runBlocking {
        var name: String? = "Semester.JSON"
        var mime = "text/plain"
        val provider = object : android.content.ContentProvider() {
            override fun onCreate() = true
            override fun getType(uri: Uri) = mime
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) =
                android.database.MatrixCursor(arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf(name)) }
            override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = error("Read only")
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = error("Read only")
            override fun update(uri: Uri, values: android.content.ContentValues?, selection: String?, selectionArgs: Array<out String>?) = error("Read only")
        }
        val detector = ImportExportAdapter(repository, android.content.ContentResolver.wrap(provider))
        val opaque = Uri.parse("content://documents/document/123.csv")
        assertEquals(ImportFormat.JSON, detector.detectFormat(opaque))
        name = "semester.wakeup_schedule"
        assertEquals(ImportFormat.WAKE_UP, detector.detectFormat(opaque))
        name = "calendar.ics"
        assertEquals(ImportFormat.ICS, detector.detectFormat(opaque))
        name = null
        mime = "application/json"
        assertEquals(ImportFormat.JSON, detector.detectFormat(opaque))
        mime = "text/calendar"
        assertEquals(ImportFormat.ICS, detector.detectFormat(opaque))
        mime = "text/comma-separated-values"
        assertEquals(ImportFormat.CSV, detector.detectFormat(opaque))
        mime = "application/octet-stream"
        assertEquals(null, detector.detectFormat(opaque))
        mime = "text/plain"
        assertEquals(null, detector.detectFormat(opaque))
        assertEquals(ImportFormat.ICS, adapter.detectFormat(Uri.fromFile(newFile("schedule.ics"))))
    }

    @Test
    fun importsCsvJsonWakeUpAndIcsAsNewTables() = runBlocking {
        val csv = adapter.importFile(
            uri = writeFile(
                """
                课程名称,星期,开始节数,结束节数,老师,地点,周数
                数学,1,1,1,王老师,101,1-2
                """.trimIndent(),
            ),
            format = ImportFormat.CSV,
            target = ImportTarget.CREATE_NEW,
            options = ImportOptions(
                name = "CSV table",
                firstDayEpochDay = FIRST_DAY,
            ),
        )
        assertEquals("CSV table", csv.timetable.name)
        assertEquals("数学", csv.timetable.courses.single().name)
        assertEquals(1, csv.timetable.courses.single().slots.single().dayOfWeek)

        val sourceJson = sampleTimetable("json-table", "json-course", "JSON course")
        val json = adapter.importFile(
            uri = writeFile(BackupFormat.encode(sourceJson)),
            format = ImportFormat.JSON,
            target = ImportTarget.CREATE_NEW,
        )
        assertEquals("JSON course", json.timetable.courses.single().name)

        val wakeUp = adapter.importFile(
            uri = writeFile(wakeUpBackup()),
            format = ImportFormat.WAKE_UP,
            target = ImportTarget.CREATE_NEW,
        )
        assertEquals("WakeUp table", wakeUp.timetable.name)
        assertEquals("WakeUp course", wakeUp.timetable.courses.single().name)
        assertEquals("WakeUp teacher", wakeUp.timetable.courses.single().slots.single().teacher)

        val ics = adapter.importFile(
            uri = writeFile(icsCalendar()),
            format = ImportFormat.ICS,
            target = ImportTarget.CREATE_NEW,
        )
        assertEquals("Calendar course", ics.timetable.courses.single().name)
        assertEquals("Room 1", ics.timetable.courses.single().slots.single().room)
    }

    @Test
    fun appliesCreateMergeAndReplaceTargetsToTheSameCurrentTable() = runBlocking {
        val currentId = repository.importDomainTimetable(
            sampleTimetable("current-table", "current-course", "Current course"),
        )
        val incoming = sampleTimetable("incoming-table", "incoming-course", "Incoming course")
        val incomingUri = writeFile(BackupFormat.encode(incoming))

        val created = adapter.importFile(
            uri = incomingUri,
            format = ImportFormat.JSON,
            target = ImportTarget.CREATE_NEW,
        )
        assertTrue(created.tableId != currentId)
        assertEquals(1, created.timetable.courses.size)

        val merged = adapter.importFile(
            uri = incomingUri,
            format = ImportFormat.JSON,
            target = ImportTarget.MERGE_CURRENT,
            currentTableId = currentId,
        )
        assertEquals(currentId, merged.tableId)
        assertEquals(2, merged.timetable.courses.size)
        assertTrue(merged.timetable.courses.any { it.name == "Current course" })
        assertTrue(merged.timetable.courses.any { it.name == "Incoming course" })

        val replaced = adapter.importFile(
            uri = incomingUri,
            format = ImportFormat.JSON,
            target = ImportTarget.REPLACE_CURRENT,
            currentTableId = currentId,
        )
        assertEquals(currentId, replaced.tableId)
        assertEquals(listOf("Incoming course"), replaced.timetable.courses.map { it.name })
    }

    @Test
    fun exportsJsonAndIcsWithAnExplicitUtcDtStamp() = runBlocking {
        val tableId = repository.importDomainTimetable(
            sampleTimetable("export-table", "export-course", "Export course"),
        )
        val jsonFile = newFile("export.json")
        adapter.exportFile(Uri.fromFile(jsonFile), ImportFormat.JSON, tableId)
        assertEquals("Export course", BackupFormat.decode(jsonFile.readText()).courses.single().name)

        val icsFile = newFile("export.ics")
        adapter.exportFile(
            uri = Uri.fromFile(icsFile),
            format = ImportFormat.ICS,
            tableId = tableId,
            range = EpochDayRange(FIRST_DAY, FIRST_DAY),
        )
        assertTrue(icsFile.readText().contains("DTSTAMP:20260904T101500Z"))
    }

    @Test
    fun contentResolverReadAndWriteFailuresAreExplicit() = runBlocking {
        val missingInput = File(context.cacheDir, "missing-import.json")
        missingInput.delete()
        var readFailure: IOException? = null
        try {
            adapter.importFile(
                uri = Uri.fromFile(missingInput),
                format = ImportFormat.JSON,
                target = ImportTarget.CREATE_NEW,
            )
        } catch (error: IOException) {
            readFailure = error
        }
        assertTrue(readFailure != null)

        val tableId = repository.importDomainTimetable(
            sampleTimetable("write-table", "write-course", "Write course"),
        )
        val missingParent = File(context.cacheDir, "missing-export-parent/export.json")
        missingParent.delete()
        missingParent.parentFile?.delete()
        var writeFailure: IOException? = null
        try {
            adapter.exportFile(Uri.fromFile(missingParent), ImportFormat.JSON, tableId)
        } catch (error: IOException) {
            writeFailure = error
        }
        assertTrue(writeFailure != null)
    }

    private fun writeFile(content: String): Uri {
        val file = newFile("import-${files.size}.txt")
        file.writeText(content)
        return Uri.fromFile(file)
    }

    private fun newFile(name: String): File {
        val file = File(context.cacheDir, "import-export-test-$name")
        file.delete()
        files += file
        return file
    }

    private fun sampleTimetable(id: String, courseId: String, courseName: String): Timetable =
        Timetable(
            id = id,
            name = id,
            firstDayEpochDay = FIRST_DAY,
            maxWeek = 1,
            timeTable = ScheduleTimeTable(
                id = "sample-time-table",
                name = "Sample time table",
                nodes = listOf(TimeTableNode(node = 1, startMinuteOfDay = 480, endMinuteOfDay = 530)),
            ),
            courses = listOf(
                Course(
                    id = courseId,
                    name = courseName,
                    slots = listOf(
                        LogicalCourseSlot(
                            id = "$courseId-slot",
                            courseId = courseId,
                            dayOfWeek = 1,
                            startNode = 1,
                            recurrenceSegments = listOf(
                                RecurrenceSegment(
                                    id = "$courseId-segment",
                                    startWeek = 1,
                                    endWeek = 1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun wakeUpBackup(): String = listOf(
        "{\"id\":1,\"name\":\"WakeUp time table\"}",
        "[{\"endTime\":\"08:50\",\"node\":1,\"startTime\":\"08:00\",\"timeTable\":1}]",
        "{\"id\":2,\"maxWeek\":1,\"nodes\":1,\"showSat\":true,\"showSun\":true,\"startDate\":\"2026-03-02\",\"tableName\":\"WakeUp table\",\"timeTable\":1}",
        "[{\"color\":\"#112233\",\"courseName\":\"WakeUp course\",\"id\":10,\"tableId\":2}]",
        "[{\"day\":1,\"endWeek\":1,\"id\":10,\"level\":0,\"ownTime\":false,\"room\":\"Room W\",\"startNode\":1,\"startWeek\":1,\"step\":1,\"tableId\":2,\"teacher\":\"WakeUp teacher\",\"type\":0}]",
    ).joinToString("\n")

    private fun icsCalendar(): String = listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//SleepDown//Test//EN",
        "BEGIN:VEVENT",
        "DTSTAMP:20260904T101500Z",
        "DTSTART:20260302T080000",
        "DTEND:20260302T085000",
        "UID:calendar-event",
        "SUMMARY:Calendar course",
        "LOCATION:Room 1",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString("\r\n", postfix = "\r\n")

    private companion object {
        const val DATABASE_NAME = "timetable.db"
        val FIRST_DAY: Long = LocalDate.of(2026, 3, 2).toEpochDay()
    }
}
