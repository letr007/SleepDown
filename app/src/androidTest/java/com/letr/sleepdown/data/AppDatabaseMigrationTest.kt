package com.letr.sleepdown.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrateV4ToV5CreatesValidTimeTableForLegacyTableWithoutNodes() {
        val oldDatabase = helper.createDatabase(EMPTY_DATABASE_NAME, 4)
        oldDatabase.execSQL(
            """
            INSERT INTO tables (
                id, name, startDate, maxWeek, nodeCount, showWeekend, bgImageUri,
                itemTextSize, textColor, itemAlpha, itemHeightDp, showTime,
                showLocation, showRoomPrefix, showTeacher, showOtherWeekCourse,
                otherWeekAlpha, showGrid, showTimeBar
            ) VALUES (1, 'Empty', 100, 4, 2, 1, NULL, 12, -16777216, 0.5, 64, 0, 1, 1, 1, 1, 0.5, 0, 1)
            """.trimIndent(),
        )
        oldDatabase.close()

        val database = helper.runMigrationsAndValidate(
            EMPTY_DATABASE_NAME,
            5,
            true,
            AppDatabase.MIGRATION_4_5,
        )
        try {
            assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM time_table_nodes"))
            assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM time_table_nodes WHERE timeTableId = 1"))
            assertEquals(1L, scalar(database, "SELECT COUNT(*) FROM reminder_settings"))
            val cursor = database.query("SELECT start, end FROM time_table_nodes WHERE timeTableId = 1 ORDER BY node")
            try {
                assertTrue(cursor.moveToFirst())
                assertEquals("08:00", cursor.getString(0))
                assertEquals("08:09", cursor.getString(1))
                assertTrue(cursor.moveToNext())
                assertEquals("08:10", cursor.getString(0))
                assertEquals("08:19", cursor.getString(1))
                assertFalse(cursor.moveToNext())
            } finally {
                cursor.close()
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrateV4ToV5RepairsSparseDuplicatePlaceholderMalformedAndOverlappingNodes() {
        val oldDatabase = helper.createDatabase(REPAIRED_DATABASE_NAME, 4)
        oldDatabase.execSQL(
            """
            INSERT INTO tables (
                id, name, startDate, maxWeek, nodeCount, showWeekend, bgImageUri,
                itemTextSize, textColor, itemAlpha, itemHeightDp, showTime,
                showLocation, showRoomPrefix, showTeacher, showOtherWeekCourse,
                otherWeekAlpha, showGrid, showTimeBar
            ) VALUES (3, 'Repaired', 100, 6, 6, 1, NULL, 12, -16777216, 0.5, 64, 0, 1, 1, 1, 1, 0.5, 0, 1)
            """.trimIndent(),
        )
        oldDatabase.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(1L, 3L, 4, "10:00", "10:50"),
        )
        oldDatabase.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(2L, 3L, 2, "08:00", "08:50"),
        )
        oldDatabase.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(3L, 3L, 2, "08:10", "08:40"),
        )
        oldDatabase.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(4L, 3L, 3, "malformed", "09:00"),
        )
        oldDatabase.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(5L, 3L, 5, "00:00", "00:00"),
        )
        oldDatabase.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(6L, 3L, 6, "08:30", "09:00"),
        )
        oldDatabase.execSQL(
            "INSERT INTO courses (id, tableId, name, color, teacher, note, credit) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(30L, 3L, "Sparse course", 1, "Teacher", "", 0.0),
        )
        oldDatabase.execSQL(
            """
            INSERT INTO course_times (
                id, courseId, day, startNode, step, startWeek, endWeek,
                weekType, room, ownTime, startTime, endTime
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(31L, 30L, 1, 4, 1, 1, 1, 0, "", 0, "", ""),
        )
        oldDatabase.close()

        val database = helper.runMigrationsAndValidate(
            REPAIRED_DATABASE_NAME,
            5,
            true,
            AppDatabase.MIGRATION_4_5,
        )
        try {
            assertEquals(6L, scalar(database, "SELECT COUNT(*) FROM node_times"))
            assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM time_table_nodes WHERE timeTableId = 3"))
            val courseCursor = database.query("SELECT startNode, step FROM course_times WHERE id = 31")
            try {
                assertTrue(courseCursor.moveToFirst())
                assertEquals(2, courseCursor.getInt(0))
                assertEquals(1, courseCursor.getInt(1))
            } finally {
                courseCursor.close()
            }
            val cursor = database.query(
                "SELECT node, start, end FROM time_table_nodes WHERE timeTableId = 3 ORDER BY node",
            )
            try {
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals("08:00", cursor.getString(1))
                assertEquals("08:50", cursor.getString(2))
                assertTrue(cursor.moveToNext())
                assertEquals(2, cursor.getInt(0))
                assertEquals("10:00", cursor.getString(1))
                assertEquals("10:50", cursor.getString(2))
                assertFalse(cursor.moveToNext())
            } finally {
                cursor.close()
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrateV4ToV5PreservesDataAndBackfillsNewRelationships() {
        val oldDatabase = helper.createDatabase(DATABASE_NAME, 4)
        insertV4Data(oldDatabase)
        oldDatabase.close()

        val database = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            5,
            true,
            AppDatabase.MIGRATION_4_5,
        )
        try {
            assertEquals(5L, scalar(database, "PRAGMA user_version"))
            assertLegacyTables(database)
            assertLegacyCourses(database)
            assertLegacyCourseTimesAndBackfill(database)
            assertLegacyNodeTimes(database)
            assertReusableTimeTables(database)
            assertDefaultReminderSettings(database)
            assertEquals(0L, scalar(database, "SELECT COUNT(*) FROM course_date_exceptions"))
            assertEquals(0L, scalar(database, "SELECT COUNT(*) FROM conflict_preferences"))
            assertEquals("TEXT", columnType(database, "course_date_exceptions", "id"))
        } finally {
            database.close()
        }
    }

    private fun insertV4Data(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO tables (
                id, name, startDate, maxWeek, nodeCount, showWeekend, bgImageUri,
                itemTextSize, textColor, itemAlpha, itemHeightDp, showTime,
                showLocation, showRoomPrefix, showTeacher, showOtherWeekCourse,
                otherWeekAlpha, showGrid, showTimeBar, showSat, showSun,
                headerTextSize, courseTextColor, strokeColor, useDottedLine,
                itemCenterHorizontal, itemCenterVertical, radius, textColorCompose,
                strokeColorCompose, sundayFirst
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                7L, "Later", 200L, 16, 2, 0, "content://later",
                14.5, 0x11223344L, 0.75, 72, 1, 0, 1, 0, 1,
                0.25, 1, 0, 0, 1, 12, -101, -202, 1, 1, 0, 9, 1, 0, 1,
            ),
        )
        database.execSQL(
            """
            INSERT INTO tables (
                id, name, startDate, maxWeek, nodeCount, showWeekend, bgImageUri,
                itemTextSize, textColor, itemAlpha, itemHeightDp, showTime,
                showLocation, showRoomPrefix, showTeacher, showOtherWeekCourse,
                otherWeekAlpha, showGrid, showTimeBar, showSat, showSun,
                headerTextSize, courseTextColor, strokeColor, useDottedLine,
                itemCenterHorizontal, itemCenterVertical, radius, textColorCompose,
                strokeColorCompose, sundayFirst
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                2L, "Earlier", 100L, 20, 3, 1, null,
                11.0, 0x55667788L, 0.5, 64, 0, 1, 1, 1, 0,
                0.5, 0, 1, 1, 1, 11, -1, -3, 0, 0, 1, 4, 0, 1, 0,
            ),
        )

        database.execSQL(
            "INSERT INTO courses (id, tableId, name, color, teacher, note, credit) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(20L, 7L, "Physics", 0x123456, "Dr. Beta", "lab", 3.5),
        )
        database.execSQL(
            "INSERT INTO courses (id, tableId, name, color, teacher, note, credit) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(10L, 2L, "Math", 0x654321, "Dr. Alpha", "proof", 2.0),
        )

        database.execSQL(
            """
            INSERT INTO course_times (
                id, courseId, day, startNode, step, startWeek, endWeek,
                weekType, room, ownTime, startTime, endTime
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(101L, 20L, 1, 2, 2, 1, 4, 1, "Room A", 1, "08:15", "09:45"),
        )
        database.execSQL(
            """
            INSERT INTO course_times (
                id, courseId, day, startNode, step, startWeek, endWeek,
                weekType, room, ownTime, startTime, endTime
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(102L, 20L, 1, 2, 2, 5, 8, 2, "Room A", 1, "08:15", "09:45"),
        )
        database.execSQL(
            """
            INSERT INTO course_times (
                id, courseId, day, startNode, step, startWeek, endWeek,
                weekType, room, ownTime, startTime, endTime
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(103L, 20L, 1, 2, 2, 9, 12, 0, "Room B", 1, "08:15", "09:45"),
        )
        database.execSQL(
            """
            INSERT INTO course_times (
                id, courseId, day, startNode, step, startWeek, endWeek,
                weekType, room, ownTime, startTime, endTime
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(201L, 10L, 3, 1, 1, 1, 20, 0, "Room C", 0, "", ""),
        )

        database.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(701L, 7L, 1, "08:00", "08:50"),
        )
        database.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(702L, 7L, 2, "09:00", "09:50"),
        )
        database.execSQL(
            "INSERT INTO node_times (id, tableId, node, start, end) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(201L, 2L, 1, "10:00", "10:50"),
        )
    }

    private fun assertLegacyTables(database: SupportSQLiteDatabase) {
        assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM tables"))
        assertTable(
            database = database,
            id = 7L,
            name = "Later",
            startDate = 200L,
            maxWeek = 16,
            nodeCount = 2,
            showWeekend = 0,
            bgImageUri = "content://later",
            itemTextSize = 14.5,
            textColor = 0x11223344L,
            itemAlpha = 0.75,
            itemHeightDp = 72,
            showTime = 1,
            showLocation = 0,
            showRoomPrefix = 1,
            showTeacher = 0,
            showOtherWeekCourse = 1,
            otherWeekAlpha = 0.25,
            showGrid = 1,
            showTimeBar = 0,
            showSat = 0,
            showSun = 1,
            headerTextSize = 12,
            courseTextColor = -101,
            strokeColor = -202,
            useDottedLine = 1,
            itemCenterHorizontal = 1,
            itemCenterVertical = 0,
            radius = 9,
            textColorCompose = 1,
            strokeColorCompose = 0,
            sundayFirst = 1,
            sortOrder = 1,
            timeTableId = 7L,
        )
        assertTable(
            database = database,
            id = 2L,
            name = "Earlier",
            startDate = 100L,
            maxWeek = 20,
            nodeCount = 3,
            showWeekend = 1,
            bgImageUri = null,
            itemTextSize = 11.0,
            textColor = 0x55667788L,
            itemAlpha = 0.5,
            itemHeightDp = 64,
            showTime = 0,
            showLocation = 1,
            showRoomPrefix = 1,
            showTeacher = 1,
            showOtherWeekCourse = 0,
            otherWeekAlpha = 0.5,
            showGrid = 0,
            showTimeBar = 1,
            showSat = 1,
            showSun = 1,
            headerTextSize = 11,
            courseTextColor = -1,
            strokeColor = -3,
            useDottedLine = 0,
            itemCenterHorizontal = 0,
            itemCenterVertical = 1,
            radius = 4,
            textColorCompose = 0,
            strokeColorCompose = 1,
            sundayFirst = 0,
            sortOrder = 0,
            timeTableId = 2L,
        )

        val ordered = database.query("SELECT id FROM tables ORDER BY sortOrder, id")
        try {
            assertTrue(ordered.moveToFirst())
            assertEquals(2L, ordered.getLong(0))
            assertTrue(ordered.moveToNext())
            assertEquals(7L, ordered.getLong(0))
            assertFalse(ordered.moveToNext())
        } finally {
            ordered.close()
        }
    }

    private fun assertTable(
        database: SupportSQLiteDatabase,
        id: Long,
        name: String,
        startDate: Long,
        maxWeek: Int,
        nodeCount: Int,
        showWeekend: Int,
        bgImageUri: String?,
        itemTextSize: Double,
        textColor: Long,
        itemAlpha: Double,
        itemHeightDp: Int,
        showTime: Int,
        showLocation: Int,
        showRoomPrefix: Int,
        showTeacher: Int,
        showOtherWeekCourse: Int,
        otherWeekAlpha: Double,
        showGrid: Int,
        showTimeBar: Int,
        showSat: Int,
        showSun: Int,
        headerTextSize: Int,
        courseTextColor: Int,
        strokeColor: Int,
        useDottedLine: Int,
        itemCenterHorizontal: Int,
        itemCenterVertical: Int,
        radius: Int,
        textColorCompose: Int,
        strokeColorCompose: Int,
        sundayFirst: Int,
        sortOrder: Int,
        timeTableId: Long,
    ) {
        val cursor = database.query("SELECT * FROM tables WHERE id = $id")
        try {
            assertTrue(cursor.moveToFirst())
            fun index(name: String): Int = cursor.getColumnIndexOrThrow(name)
            assertEquals(id, cursor.getLong(index("id")))
            assertEquals(name, cursor.getString(index("name")))
            assertEquals(startDate, cursor.getLong(index("startDate")))
            assertEquals(maxWeek, cursor.getInt(index("maxWeek")))
            assertEquals(nodeCount, cursor.getInt(index("nodeCount")))
            assertEquals(showWeekend, cursor.getInt(index("showWeekend")))
            if (bgImageUri == null) {
                assertTrue(cursor.isNull(index("bgImageUri")))
            } else {
                assertEquals(bgImageUri, cursor.getString(index("bgImageUri")))
            }
            assertEquals(itemTextSize, cursor.getDouble(index("itemTextSize")), 0.0)
            assertEquals(textColor, cursor.getLong(index("textColor")))
            assertEquals(itemAlpha, cursor.getDouble(index("itemAlpha")), 0.0)
            assertEquals(itemHeightDp, cursor.getInt(index("itemHeightDp")))
            assertEquals(showTime, cursor.getInt(index("showTime")))
            assertEquals(showLocation, cursor.getInt(index("showLocation")))
            assertEquals(showRoomPrefix, cursor.getInt(index("showRoomPrefix")))
            assertEquals(showTeacher, cursor.getInt(index("showTeacher")))
            assertEquals(showOtherWeekCourse, cursor.getInt(index("showOtherWeekCourse")))
            assertEquals(otherWeekAlpha, cursor.getDouble(index("otherWeekAlpha")), 0.0)
            assertEquals(showGrid, cursor.getInt(index("showGrid")))
            assertEquals(showTimeBar, cursor.getInt(index("showTimeBar")))
            assertEquals(showSat, cursor.getInt(index("showSat")))
            assertEquals(showSun, cursor.getInt(index("showSun")))
            assertEquals(headerTextSize, cursor.getInt(index("headerTextSize")))
            assertEquals(courseTextColor, cursor.getInt(index("courseTextColor")))
            assertEquals(strokeColor, cursor.getInt(index("strokeColor")))
            assertEquals(useDottedLine, cursor.getInt(index("useDottedLine")))
            assertEquals(itemCenterHorizontal, cursor.getInt(index("itemCenterHorizontal")))
            assertEquals(itemCenterVertical, cursor.getInt(index("itemCenterVertical")))
            assertEquals(radius, cursor.getInt(index("radius")))
            assertEquals(textColorCompose, cursor.getInt(index("textColorCompose")))
            assertEquals(strokeColorCompose, cursor.getInt(index("strokeColorCompose")))
            assertEquals(sundayFirst, cursor.getInt(index("sundayFirst")))
            assertEquals(sortOrder, cursor.getInt(index("sortOrder")))
            assertEquals(timeTableId, cursor.getLong(index("timeTableId")))
        } finally {
            cursor.close()
        }
    }

    private fun assertLegacyCourses(database: SupportSQLiteDatabase) {
        assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM courses"))
        val cursor = database.query("SELECT * FROM courses ORDER BY id")
        try {
            assertTrue(cursor.moveToFirst())
            assertEquals(10L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("tableId")))
            assertEquals("Math", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(0x654321, cursor.getInt(cursor.getColumnIndexOrThrow("color")))
            assertEquals("Dr. Alpha", cursor.getString(cursor.getColumnIndexOrThrow("teacher")))
            assertEquals("proof", cursor.getString(cursor.getColumnIndexOrThrow("note")))
            assertEquals(2.0, cursor.getDouble(cursor.getColumnIndexOrThrow("credit")), 0.0)

            assertTrue(cursor.moveToNext())
            assertEquals(20L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            assertEquals(7L, cursor.getLong(cursor.getColumnIndexOrThrow("tableId")))
            assertEquals("Physics", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(0x123456, cursor.getInt(cursor.getColumnIndexOrThrow("color")))
            assertEquals("Dr. Beta", cursor.getString(cursor.getColumnIndexOrThrow("teacher")))
            assertEquals("lab", cursor.getString(cursor.getColumnIndexOrThrow("note")))
            assertEquals(3.5, cursor.getDouble(cursor.getColumnIndexOrThrow("credit")), 0.0)
            assertFalse(cursor.moveToNext())
        } finally {
            cursor.close()
        }
    }

    private fun assertLegacyCourseTimesAndBackfill(database: SupportSQLiteDatabase) {
        assertEquals(4L, scalar(database, "SELECT COUNT(*) FROM course_times"))
        val cursor = database.query("SELECT * FROM course_times ORDER BY id")
        val logicalSlotIds = mutableMapOf<Long, String>()
        val recurrenceSegmentIds = mutableMapOf<Long, String>()
        try {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                logicalSlotIds[id] = cursor.getString(cursor.getColumnIndexOrThrow("logicalSlotId"))
                recurrenceSegmentIds[id] = cursor.getString(cursor.getColumnIndexOrThrow("recurrenceSegmentId"))
                assertTrue(logicalSlotIds.getValue(id).isNotBlank())
                assertTrue(recurrenceSegmentIds.getValue(id).isNotBlank())
                assertNotEquals("legacy", recurrenceSegmentIds.getValue(id))
                when (id) {
                    101L -> {
                        assertCourseTime(cursor, 20L, 1, 2, 2, 1, 4, 1, "Room A", 1, "08:15", "09:45", "Dr. Beta")
                    }
                    102L -> {
                        assertCourseTime(cursor, 20L, 1, 2, 2, 5, 8, 2, "Room A", 1, "08:15", "09:45", "Dr. Beta")
                    }
                    103L -> {
                        assertCourseTime(cursor, 20L, 1, 2, 2, 9, 12, 0, "Room B", 1, "08:15", "09:45", "Dr. Beta")
                    }
                    201L -> {
                        assertCourseTime(cursor, 10L, 3, 1, 1, 1, 20, 0, "Room C", 0, "", "", "Dr. Alpha")
                    }
                    else -> throw AssertionError("unexpected course time $id")
                }
            }
        } finally {
            cursor.close()
        }
        assertEquals(logicalSlotIds[101L], logicalSlotIds[102L])
        assertEquals(logicalSlotIds[101L], logicalSlotIds[103L])
        assertNotNull(logicalSlotIds[201L])
        assertEquals("legacy-segment|20|101", recurrenceSegmentIds[101L])
        assertEquals("legacy-segment|20|102", recurrenceSegmentIds[102L])
        assertEquals("legacy-segment|20|103", recurrenceSegmentIds[103L])
        assertEquals("legacy-segment|10|201", recurrenceSegmentIds[201L])
        assertEquals(4, recurrenceSegmentIds.values.toSet().size)
    }

    private fun assertCourseTime(
        cursor: android.database.Cursor,
        courseId: Long,
        day: Int,
        startNode: Int,
        step: Int,
        startWeek: Int,
        endWeek: Int,
        weekType: Int,
        room: String,
        ownTime: Int,
        startTime: String,
        endTime: String,
        teacher: String,
    ) {
        assertEquals(courseId, cursor.getLong(cursor.getColumnIndexOrThrow("courseId")))
        assertEquals(day, cursor.getInt(cursor.getColumnIndexOrThrow("day")))
        assertEquals(startNode, cursor.getInt(cursor.getColumnIndexOrThrow("startNode")))
        assertEquals(step, cursor.getInt(cursor.getColumnIndexOrThrow("step")))
        assertEquals(startWeek, cursor.getInt(cursor.getColumnIndexOrThrow("startWeek")))
        assertEquals(endWeek, cursor.getInt(cursor.getColumnIndexOrThrow("endWeek")))
        assertEquals(weekType, cursor.getInt(cursor.getColumnIndexOrThrow("weekType")))
        assertEquals(room, cursor.getString(cursor.getColumnIndexOrThrow("room")))
        assertEquals(ownTime, cursor.getInt(cursor.getColumnIndexOrThrow("ownTime")))
        assertEquals(startTime, cursor.getString(cursor.getColumnIndexOrThrow("startTime")))
        assertEquals(endTime, cursor.getString(cursor.getColumnIndexOrThrow("endTime")))
        assertEquals(teacher, cursor.getString(cursor.getColumnIndexOrThrow("teacher")))
    }

    private fun assertLegacyNodeTimes(database: SupportSQLiteDatabase) {
        assertEquals(3L, scalar(database, "SELECT COUNT(*) FROM node_times"))
        val cursor = database.query("SELECT * FROM node_times ORDER BY id")
        try {
            assertTrue(cursor.moveToFirst())
            assertEquals(201L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("tableId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("node")))
            assertEquals("10:00", cursor.getString(cursor.getColumnIndexOrThrow("start")))
            assertEquals("10:50", cursor.getString(cursor.getColumnIndexOrThrow("end")))
            assertTrue(cursor.moveToNext())
            assertEquals(701L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            assertEquals(7L, cursor.getLong(cursor.getColumnIndexOrThrow("tableId")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("node")))
            assertEquals("08:00", cursor.getString(cursor.getColumnIndexOrThrow("start")))
            assertEquals("08:50", cursor.getString(cursor.getColumnIndexOrThrow("end")))
            assertTrue(cursor.moveToNext())
            assertEquals(702L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
            assertEquals(7L, cursor.getLong(cursor.getColumnIndexOrThrow("tableId")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("node")))
            assertEquals("09:00", cursor.getString(cursor.getColumnIndexOrThrow("start")))
            assertEquals("09:50", cursor.getString(cursor.getColumnIndexOrThrow("end")))
            assertFalse(cursor.moveToNext())
        } finally {
            cursor.close()
        }

        assertEquals(3L, scalar(database, "SELECT COUNT(*) FROM time_table_nodes"))
        assertEquals(1L, scalar(database, "SELECT COUNT(*) FROM time_table_nodes WHERE timeTableId = 2"))
        assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM time_table_nodes WHERE timeTableId = 7"))
    }

    private fun assertReusableTimeTables(database: SupportSQLiteDatabase) {
        assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM reusable_time_tables"))
        val cursor = database.query("SELECT id, name, sortOrder FROM reusable_time_tables ORDER BY id")
        try {
            assertTrue(cursor.moveToFirst())
            assertEquals(2L, cursor.getLong(0))
            assertEquals("Earlier", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertTrue(cursor.moveToNext())
            assertEquals(7L, cursor.getLong(0))
            assertEquals("Later", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
            assertFalse(cursor.moveToNext())
        } finally {
            cursor.close()
        }
    }

    private fun assertDefaultReminderSettings(database: SupportSQLiteDatabase) {
        assertEquals(2L, scalar(database, "SELECT COUNT(*) FROM reminder_settings"))
        val cursor = database.query("SELECT * FROM reminder_settings ORDER BY tableId")
        try {
            while (cursor.moveToNext()) {
                assertTrue(cursor.getLong(cursor.getColumnIndexOrThrow("tableId")) in setOf(2L, 7L))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("startEnabled")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("endEnabled")))
                assertEquals(10, cursor.getInt(cursor.getColumnIndexOrThrow("startLeadMinutes")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("endLeadMinutes")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("includeCourseName")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("includeTeacher")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("includeRoom")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("includeNote")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("vibrate")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("silent")))
            }
        } finally {
            cursor.close()
        }
    }

    private fun scalar(database: SupportSQLiteDatabase, sql: String): Long {
        val cursor = database.query(sql)
        try {
            assertTrue(cursor.moveToFirst())
            return cursor.getLong(0)
        } finally {
            cursor.close()
        }
    }

    private fun columnType(database: SupportSQLiteDatabase, table: String, column: String): String {
        val cursor = database.query("PRAGMA table_info($table)")
        try {
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return cursor.getString(typeIndex)
            }
        } finally {
            cursor.close()
        }
        throw AssertionError("missing column $table.$column")
    }

    private companion object {
        const val DATABASE_NAME = "app-database-migration-test"
        const val EMPTY_DATABASE_NAME = "app-database-empty-migration-test"
        const val REPAIRED_DATABASE_NAME = "app-database-repaired-migration-test"
    }
}
