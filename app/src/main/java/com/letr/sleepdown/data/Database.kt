package com.letr.sleepdown.data

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.letr.sleepdown.domain.MinuteOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** 一张课表（如某个学期），带外观与作息配置 */
@Serializable
@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 第一周的第一天，epochDay；未启用 sundayFirst 时按周一解释 */
    val startDate: Long,
    val maxWeek: Int = 20,
    val nodeCount: Int = 20,
    val showWeekend: Boolean = true,
    val bgImageUri: String? = null,
    /** 课程卡片主文字大小，dp */
    val itemTextSize: Float = 12f,
    /** 课表文字颜色（表头/节次列），ARGB */
    val textColor: Long = 0xFF000000,
    /** 课程卡片填充不透明度 0..1 */
    val itemAlpha: Float = 0.5f,
    /** 单节高度 dp */
    val itemHeightDp: Int = 64,
    /** 卡片内显示上课时间 */
    val showTime: Boolean = false,
    /** 卡片内显示教室 */
    val showLocation: Boolean = true,
    /** 教室用 @前缀换行格式 */
    val showRoomPrefix: Boolean = true,
    /** 显示老师 */
    val showTeacher: Boolean = true,
    /** 显示非本周课程 */
    val showOtherWeekCourse: Boolean = true,
    /** 非本周课程淡化系数 0..1 */
    val otherWeekAlpha: Float = 0.5f,
    /** 显示网格线 */
    val showGrid: Boolean = false,
    /** 节次列显示起止时间 */
    val showTimeBar: Boolean = true,
    /** 是否显示周六；默认跟随旧版的周末开关 */
    @ColumnInfo(defaultValue = "1")
    val showSat: Boolean = showWeekend,
    /** 是否显示周日；默认跟随旧版的周末开关 */
    @ColumnInfo(defaultValue = "1")
    val showSun: Boolean = showWeekend,
    /** 表头文字大小，sp */
    @ColumnInfo(defaultValue = "11")
    val headerTextSize: Int = 11,
    /** 课程文字颜色，ARGB */
    @ColumnInfo(defaultValue = "-1")
    val courseTextColor: Int = 0xFFFFFFFF.toInt(),
    /** 课程格子边框颜色，ARGB */
    @ColumnInfo(defaultValue = "-2130706433")
    val strokeColor: Int = 0x80FFFFFF.toInt(),
    /** 使用虚线边框 */
    @ColumnInfo(defaultValue = "0")
    val useDottedLine: Boolean = false,
    /** 课程文字水平居中 */
    @ColumnInfo(defaultValue = "0")
    val itemCenterHorizontal: Boolean = false,
    /** 课程文字竖直居中 */
    @ColumnInfo(defaultValue = "0")
    val itemCenterVertical: Boolean = false,
    /** 课程格子圆角半径，dp */
    @ColumnInfo(defaultValue = "4")
    val radius: Int = 4,
    /** 界面文字颜色是否叠加背景色 */
    @ColumnInfo(defaultValue = "0")
    val textColorCompose: Boolean = false,
    /** 格子边框颜色是否叠加课程颜色 */
    @ColumnInfo(defaultValue = "0")
    val strokeColorCompose: Boolean = false,
    /** 是否以周日作为每周第一天 */
    @ColumnInfo(defaultValue = "0")
    val sundayFirst: Boolean = false,
    /** 课表在列表中的持久化顺序 */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
    /** 引用的可复用时间表 */
    @ColumnInfo(defaultValue = "0")
    val timeTableId: Long = 0L,
) {
    /** 与原版命名兼容的周六开关 */
    val showSaturday: Boolean
        get() = showSat

    /** 与原版命名兼容的周日开关 */
    val showSunday: Boolean
        get() = showSun

    /** 与原版命名兼容的课程格子高度 */
    val itemHeight: Int
        get() = itemHeightDp

    /** 与原版命名兼容的非本周课程透明度 */
    val otherWeekCourseAlpha: Float
        get() = otherWeekAlpha

    /** 与原版命名兼容的圆角半径 */
    val radiusDp: Int
        get() = radius
}

@Entity(
    tableName = "courses",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tableId")],
)
@Serializable
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val name: String,
    /** ARGB */
    val color: Int,
    val teacher: String = "",
    val note: String = "",
    /** 学分（可选） */
    @ColumnInfo(defaultValue = "0")
    val credit: Float = 0f,
)

/** 某门课的一个时间段（星期+节次+周次） */
@Entity(
    tableName = "course_times",
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("courseId")],
)
@Serializable
data class CourseTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    /** 1=周一 ... 7=周日 */
    val day: Int,
    val startNode: Int,
    /** 连续节数，>=1 */
    val step: Int,
    val startWeek: Int,
    val endWeek: Int,
    /** 0=每周 1=单周 2=双周 */
    val weekType: Int = TYPE_ALL,
    val room: String = "",
    /** 使用自定义起止时间而非节次时间 */
    val ownTime: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    /** 用户视角下的逻辑时间段标识；拆分重复周时保持不变 */
    @ColumnInfo(defaultValue = "'legacy'")
    val logicalSlotId: String = "legacy",
    /** 该时间段覆盖的老师 */
    @ColumnInfo(defaultValue = "''")
    val teacher: String = "",
    /** 该逻辑时间段中的重复规则分段标识 */
    @ColumnInfo(defaultValue = "'legacy'")
    val recurrenceSegmentId: String = "legacy",
) {
    companion object {
        const val TYPE_ALL = 0
        const val TYPE_ODD = 1
        const val TYPE_EVEN = 2
    }
}

/** 某张课表各节次的起止时间 */
@Entity(
    tableName = "node_times",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tableId")],
)
@Serializable
data class NodeTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    /** 从 1 开始 */
    val node: Int,
    /** "08:00" */
    val start: String,
    val end: String,
)

/** 可被多张课表引用的命名时间表。 */
@Entity(tableName = "reusable_time_tables")
data class ReusableTimeTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
)

/** 可复用时间表中的一节。 */
@Entity(
    tableName = "time_table_nodes",
    foreignKeys = [ForeignKey(
        entity = ReusableTimeTableEntity::class,
        parentColumns = ["id"],
        childColumns = ["timeTableId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("timeTableId")],
)
data class TimeTableNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeTableId: Long,
    /** 从 1 开始 */
    val node: Int,
    /** "08:00" */
    val start: String,
    val end: String,
)

private const val DATE_EXCEPTION_STORAGE_PREFIX = "table-exception|"

/** Stores a domain exception id in the database namespace of its timetable. */
internal fun dateExceptionStorageId(tableId: Long, domainId: String): String =
    "$DATE_EXCEPTION_STORAGE_PREFIX$tableId|${domainId.length}:$domainId"

/** Reads both namespaced ids and ids written by the pre-namespacing schema. */
internal fun dateExceptionDomainId(tableId: Long, storedId: String): String {
    val prefix = "$DATE_EXCEPTION_STORAGE_PREFIX$tableId|"
    if (!storedId.startsWith(prefix)) return storedId
    val encoded = storedId.removePrefix(prefix)
    val separator = encoded.indexOf(':')
    if (separator <= 0) return storedId
    val length = encoded.substring(0, separator).toIntOrNull() ?: return storedId
    val domainId = encoded.substring(separator + 1)
    return domainId.takeIf { it.length == length } ?: storedId
}

/** 某个逻辑时间段在指定日期上的取消或调课覆盖。 */
@Entity(
    tableName = "course_date_exceptions",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tableId")],
)
/**
 * Room stores a table-scoped key in id; DomainMappers exposes the original domain id.
 */
data class DateExceptionEntity(
    @PrimaryKey val id: String,
    val tableId: Long,
    val logicalSlotId: String,
    val originalEpochDay: Long,
    @ColumnInfo(defaultValue = "'CANCEL'")
    val type: String = "CANCEL",
    val recurrenceSegmentId: String? = null,
    val targetEpochDay: Long? = null,
    val targetDayOfWeek: Int? = null,
    val targetStartNode: Int? = null,
    val targetNodeCount: Int? = null,
    val targetStartTime: String? = null,
    val targetEndTime: String? = null,
    val targetTeacher: String? = null,
    val targetRoom: String? = null,
)

/** 某张课表的冲突优先级选择。 */
@Entity(
    tableName = "conflict_preferences",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tableId")],
)
data class ConflictPreferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val courseId: Long,
    @ColumnInfo(defaultValue = "0")
    val priority: Int = 0,
    val logicalSlotId: String? = null,
    val occurrenceId: String? = null,
    val epochDay: Long? = null,
)

/** 某张课表的提醒开关和内容偏好。 */
@Entity(
    tableName = "reminder_settings",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ReminderSettingsEntity(
    @PrimaryKey val tableId: Long,
    @ColumnInfo(defaultValue = "1")
    val startEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val endEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "10")
    val startLeadMinutes: Int = 10,
    @ColumnInfo(defaultValue = "0")
    val endLeadMinutes: Int = 0,
    @ColumnInfo(defaultValue = "1")
    val includeCourseName: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val includeTeacher: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val includeRoom: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val includeNote: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val vibrate: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val silent: Boolean = false,
)

/** 周视图展示用的聚合模型 */
data class CourseItem(
    val course: CourseEntity,
    val time: CourseTimeEntity,
)

data class TableWithMeta(
    val table: TableEntity,
    val nodeTimes: List<NodeTimeEntity>,
    val items: List<CourseItem>,
    val courses: List<CourseEntity> = emptyList(),
)

@Dao
interface TimetableDao {
    @Query("SELECT * FROM tables ORDER BY sortOrder, id")
    fun observeTables(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    fun observeTable(id: Long): Flow<TableEntity?>

    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getTable(id: Long): TableEntity?

    @Query("SELECT * FROM tables ORDER BY sortOrder, id LIMIT 1")
    suspend fun getFirstTable(): TableEntity?

    @Upsert
    suspend fun upsertTable(table: TableEntity): Long

    @Delete
    suspend fun deleteTable(table: TableEntity)

    @Query("SELECT * FROM courses WHERE tableId = :tableId ORDER BY id")
    fun observeCourses(tableId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE tableId = :tableId ORDER BY id")
    suspend fun getCourses(tableId: Long): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT * FROM tables ORDER BY sortOrder, id")
    suspend fun getAllTables(): List<TableEntity>

    @Query("SELECT * FROM tables WHERE timeTableId = :timeTableId ORDER BY sortOrder, id")
    suspend fun getTablesUsingTimeTable(timeTableId: Long): List<TableEntity>

    @Query("UPDATE tables SET sortOrder = :sortOrder WHERE id = :tableId")
    suspend fun updateTableSortOrder(tableId: Long, sortOrder: Int)

    @Upsert
    suspend fun upsertCourse(course: CourseEntity): Long

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE tableId = :tableId")
    suspend fun deleteCourses(tableId: Long)

    @Query("SELECT * FROM course_times WHERE courseId = :courseId")
    suspend fun getTimes(courseId: Long): List<CourseTimeEntity>

    @Query("DELETE FROM course_times WHERE courseId = :courseId")
    suspend fun deleteTimes(courseId: Long)

    @Query("SELECT * FROM course_times WHERE courseId IN (SELECT id FROM courses WHERE tableId = :tableId)")
    fun observeAllTimes(tableId: Long): Flow<List<CourseTimeEntity>>

    @Upsert
    suspend fun upsertTime(time: CourseTimeEntity): Long

    @Upsert
    suspend fun upsertTimes(times: List<CourseTimeEntity>)

    @Delete
    suspend fun deleteTime(time: CourseTimeEntity)

    @Query("SELECT * FROM node_times WHERE tableId = :tableId ORDER BY node")
    fun observeNodeTimes(tableId: Long): Flow<List<NodeTimeEntity>>

    @Query("SELECT * FROM node_times WHERE tableId = :tableId ORDER BY node")
    suspend fun getNodeTimes(tableId: Long): List<NodeTimeEntity>

    @Upsert
    suspend fun upsertNodeTimes(times: List<NodeTimeEntity>)

    @Query("DELETE FROM node_times WHERE tableId = :tableId")
    suspend fun deleteNodeTimes(tableId: Long)

    @Query("SELECT COUNT(*) FROM tables")
    suspend fun tableCount(): Int

    @Query("DELETE FROM course_date_exceptions WHERE tableId = :tableId")
    suspend fun deleteDateExceptions(tableId: Long)

    @Query("DELETE FROM conflict_preferences WHERE tableId = :tableId")
    suspend fun deleteConflictPreferences(tableId: Long)

    @Query("DELETE FROM reminder_settings WHERE tableId = :tableId")
    suspend fun deleteReminderSettings(tableId: Long)
}

@Dao
interface ReusableTimeTableDao {
    @Query("SELECT * FROM reusable_time_tables ORDER BY sortOrder, id")
    fun observeTimeTables(): Flow<List<ReusableTimeTableEntity>>

    @Query("SELECT * FROM reusable_time_tables ORDER BY sortOrder, id")
    suspend fun getAllTimeTables(): List<ReusableTimeTableEntity>

    @Query("SELECT * FROM reusable_time_tables WHERE id = :id")
    suspend fun getTimeTable(id: Long): ReusableTimeTableEntity?

    @Upsert
    suspend fun upsertTimeTable(timeTable: ReusableTimeTableEntity): Long

    @Delete
    suspend fun deleteTimeTable(timeTable: ReusableTimeTableEntity)

    @Query("SELECT * FROM time_table_nodes WHERE timeTableId = :timeTableId ORDER BY node")
    fun observeNodes(timeTableId: Long): Flow<List<TimeTableNodeEntity>>

    @Query("SELECT * FROM time_table_nodes WHERE timeTableId = :timeTableId ORDER BY node")
    suspend fun getNodes(timeTableId: Long): List<TimeTableNodeEntity>

    @Upsert
    suspend fun upsertNodes(nodes: List<TimeTableNodeEntity>)

    @Delete
    suspend fun deleteNode(node: TimeTableNodeEntity)

    @Query("DELETE FROM time_table_nodes WHERE timeTableId = :timeTableId")
    suspend fun deleteNodes(timeTableId: Long)
}

typealias TimeTableDao = ReusableTimeTableDao

@Dao
interface DateExceptionDao {
    @Query("SELECT * FROM course_date_exceptions WHERE tableId = :tableId ORDER BY originalEpochDay, id")
    fun observeForTable(tableId: Long): Flow<List<DateExceptionEntity>>

    @Query("SELECT * FROM course_date_exceptions WHERE tableId = :tableId ORDER BY originalEpochDay, id")
    suspend fun getForTable(tableId: Long): List<DateExceptionEntity>

    @Upsert
    suspend fun upsert(exception: DateExceptionEntity): Long

    @Delete
    suspend fun delete(exception: DateExceptionEntity)
}

@Dao
interface ConflictPreferenceDao {
    @Query("SELECT * FROM conflict_preferences WHERE tableId = :tableId ORDER BY priority DESC, id")
    fun observeForTable(tableId: Long): Flow<List<ConflictPreferenceEntity>>

    @Query("SELECT * FROM conflict_preferences WHERE tableId = :tableId ORDER BY priority DESC, id")
    suspend fun getForTable(tableId: Long): List<ConflictPreferenceEntity>

    @Upsert
    suspend fun upsert(preference: ConflictPreferenceEntity): Long

    @Delete
    suspend fun delete(preference: ConflictPreferenceEntity)
}

@Dao
interface ReminderSettingsDao {
    @Query("SELECT * FROM reminder_settings WHERE tableId = :tableId")
    fun observe(tableId: Long): Flow<ReminderSettingsEntity?>

    @Query("SELECT * FROM reminder_settings WHERE tableId = :tableId")
    suspend fun get(tableId: Long): ReminderSettingsEntity?

    @Upsert
    suspend fun upsert(settings: ReminderSettingsEntity)

    @Delete
    suspend fun delete(settings: ReminderSettingsEntity)
}

@Database(
    entities = [
        TableEntity::class,
        CourseEntity::class,
        CourseTimeEntity::class,
        NodeTimeEntity::class,
        ReusableTimeTableEntity::class,
        TimeTableNodeEntity::class,
        DateExceptionEntity::class,
        ConflictPreferenceEntity::class,
        ReminderSettingsEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TimetableDao
    abstract fun timeTableDao(): ReusableTimeTableDao
    abstract fun dateExceptionDao(): DateExceptionDao
    abstract fun conflictPreferenceDao(): ConflictPreferenceDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tables ADD COLUMN showSat INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE tables ADD COLUMN showSun INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE tables ADD COLUMN headerTextSize INTEGER NOT NULL DEFAULT 11")
                database.execSQL("ALTER TABLE tables ADD COLUMN courseTextColor INTEGER NOT NULL DEFAULT -1")
                database.execSQL("ALTER TABLE tables ADD COLUMN strokeColor INTEGER NOT NULL DEFAULT -2130706433")
                database.execSQL("ALTER TABLE tables ADD COLUMN useDottedLine INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tables ADD COLUMN itemCenterHorizontal INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tables ADD COLUMN itemCenterVertical INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tables ADD COLUMN radius INTEGER NOT NULL DEFAULT 4")
                database.execSQL("ALTER TABLE tables ADD COLUMN textColorCompose INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tables ADD COLUMN strokeColorCompose INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tables ADD COLUMN sundayFirst INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE tables SET showSat = showWeekend, showSun = showWeekend")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE courses ADD COLUMN credit REAL NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tables ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tables ADD COLUMN timeTableId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE course_times ADD COLUMN logicalSlotId TEXT NOT NULL DEFAULT 'legacy'")
                database.execSQL("ALTER TABLE course_times ADD COLUMN teacher TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE course_times ADD COLUMN recurrenceSegmentId TEXT NOT NULL DEFAULT 'legacy'")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reusable_time_tables (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS time_table_nodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timeTableId INTEGER NOT NULL,
                        node INTEGER NOT NULL,
                        start TEXT NOT NULL,
                        end TEXT NOT NULL,
                        FOREIGN KEY(timeTableId) REFERENCES reusable_time_tables(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_time_table_nodes_timeTableId ON time_table_nodes(timeTableId)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS course_date_exceptions (
                        id TEXT NOT NULL PRIMARY KEY,
                        tableId INTEGER NOT NULL,
                        logicalSlotId TEXT NOT NULL,
                        originalEpochDay INTEGER NOT NULL,
                        type TEXT NOT NULL DEFAULT 'CANCEL',
                        recurrenceSegmentId TEXT,
                        targetEpochDay INTEGER,
                        targetDayOfWeek INTEGER,
                        targetStartNode INTEGER,
                        targetNodeCount INTEGER,
                        targetStartTime TEXT,
                        targetEndTime TEXT,
                        targetTeacher TEXT,
                        targetRoom TEXT,
                        FOREIGN KEY(tableId) REFERENCES tables(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_course_date_exceptions_tableId ON course_date_exceptions(tableId)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conflict_preferences (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tableId INTEGER NOT NULL,
                        courseId INTEGER NOT NULL,
                        priority INTEGER NOT NULL DEFAULT 0,
                        logicalSlotId TEXT,
                        occurrenceId TEXT,
                        epochDay INTEGER,
                        FOREIGN KEY(tableId) REFERENCES tables(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conflict_preferences_tableId ON conflict_preferences(tableId)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminder_settings (
                        tableId INTEGER NOT NULL,
                        startEnabled INTEGER NOT NULL DEFAULT 1,
                        endEnabled INTEGER NOT NULL DEFAULT 0,
                        startLeadMinutes INTEGER NOT NULL DEFAULT 10,
                        endLeadMinutes INTEGER NOT NULL DEFAULT 0,
                        includeCourseName INTEGER NOT NULL DEFAULT 1,
                        includeTeacher INTEGER NOT NULL DEFAULT 1,
                        includeRoom INTEGER NOT NULL DEFAULT 1,
                        includeNote INTEGER NOT NULL DEFAULT 0,
                        vibrate INTEGER NOT NULL DEFAULT 1,
                        silent INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(tableId),
                        FOREIGN KEY(tableId) REFERENCES tables(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                // Reuse the old table ids so the v4 -> v5 relationship is deterministic.
                database.execSQL(
                    "INSERT INTO reusable_time_tables (id, name, sortOrder) SELECT id, name, sortOrder FROM tables ORDER BY id",
                )
                migrateLegacyTimeTableNodes(database)
                database.execSQL("UPDATE tables SET timeTableId = id")
                database.execSQL(
                    "UPDATE tables SET sortOrder = " +
                        "(SELECT COUNT(*) - 1 FROM tables AS previous WHERE previous.id <= tables.id)",
                )
                database.execSQL(
                    "UPDATE reusable_time_tables SET sortOrder = " +
                        "(SELECT sortOrder FROM tables WHERE tables.id = reusable_time_tables.id)",
                )
                database.execSQL(
                    "UPDATE course_times SET teacher = COALESCE(" +
                        "(SELECT teacher FROM courses WHERE courses.id = course_times.courseId), '')",
                )
                database.execSQL(
                    "UPDATE course_times SET logicalSlotId = " +
                        "'legacy|' || courseId || '|' || day || '|' || startNode || '|' || step",
                )
                database.execSQL(
                    "UPDATE course_times SET recurrenceSegmentId = " +
                        "'legacy-segment|' || courseId || '|' || id",
                )
                database.execSQL(
                    """
                    INSERT INTO reminder_settings (
                        tableId,
                        startEnabled,
                        endEnabled,
                        startLeadMinutes,
                        endLeadMinutes,
                        includeCourseName,
                        includeTeacher,
                        includeRoom,
                        includeNote,
                        vibrate,
                        silent
                    )
                    SELECT id, 1, 0, 10, 0, 1, 1, 1, 0, 1, 0
                    FROM tables
                    ORDER BY id
                    """.trimIndent(),
                )
            }
        }

        private const val DATABASE_MIGRATION_TAG = "AppDatabase"

        private data class MigrationNode(
            val node: Int,
            val start: String,
            val end: String,
            val sourceNode: Int?,
        )

        private data class LegacyNodeRow(
            val tableId: Long,
            val node: Int,
            val start: String,
            val end: String,
            val id: Long,
        )

        private fun migrateLegacyTimeTableNodes(database: SupportSQLiteDatabase) {
            val tableCounts = linkedMapOf<Long, Int>()
            val tables = database.query("SELECT id, nodeCount FROM tables ORDER BY id")
            try {
                val idIndex = tables.getColumnIndexOrThrow("id")
                val countIndex = tables.getColumnIndexOrThrow("nodeCount")
                while (tables.moveToNext()) {
                    tableCounts[tables.getLong(idIndex)] = tables.getInt(countIndex)
                }
            } finally {
                tables.close()
            }

            val rowsByTable = linkedMapOf<Long, MutableList<LegacyNodeRow>>()
            val nodes = database.query(
                "SELECT tableId, node, start, end, id FROM node_times ORDER BY tableId, node, id",
            )
            try {
                val tableIdIndex = nodes.getColumnIndexOrThrow("tableId")
                val nodeIndex = nodes.getColumnIndexOrThrow("node")
                val startIndex = nodes.getColumnIndexOrThrow("start")
                val endIndex = nodes.getColumnIndexOrThrow("end")
                val idIndex = nodes.getColumnIndexOrThrow("id")
                while (nodes.moveToNext()) {
                    val row = LegacyNodeRow(
                        tableId = nodes.getLong(tableIdIndex),
                        node = nodes.getInt(nodeIndex),
                        start = nodes.getString(startIndex),
                        end = nodes.getString(endIndex),
                        id = nodes.getLong(idIndex),
                    )
                    rowsByTable.getOrPut(row.tableId) { mutableListOf() } += row
                }
            } finally {
                nodes.close()
            }

            tableCounts.forEach { (tableId, requestedCount) ->
                val rows = rowsByTable[tableId].orEmpty()
                val canonical = canonicalMigrationNodes(
                    rows = rows,
                    requestedCount = requestedCount,
                )
                val nodeLimit = requestedCount.coerceIn(1, 60)
                val consideredRows = rows.filter { it.node in 1..nodeLimit }
                val sourceNodes = canonical.mapNotNull { it.sourceNode }
                val sourceNodesAreContiguous = sourceNodes == (1..sourceNodes.size).toList()
                if (consideredRows.isEmpty() ||
                    consideredRows.size != sourceNodes.size ||
                    !sourceNodesAreContiguous ||
                    canonical.any { it.sourceNode == null }
                ) {
                    Log.w(
                        DATABASE_MIGRATION_TAG,
                        "Repaired v4 node_times for table $tableId: " +
                            "${rows.size} legacy rows -> ${canonical.size} canonical nodes",
                    )
                }
                canonical.forEach { node ->
                    database.execSQL(
                        "INSERT INTO time_table_nodes (timeTableId, node, start, end) VALUES (?, ?, ?, ?)",
                        arrayOf<Any?>(tableId, node.node, node.start, node.end),
                    )
                }
                remapLegacyCourseTimeNodes(database, tableId, requestedCount, canonical)
            }
        }

        /**
         * Keeps valid, non-overlapping legacy intervals, renumbers sparse rows, and
         * synthesizes defaults only when a table has no usable interval.
         */
        private fun remapLegacyCourseTimeNodes(
            database: SupportSQLiteDatabase,
            tableId: Long,
            requestedCount: Int,
            canonical: List<MigrationNode>,
        ) {
            if (canonical.isEmpty()) return
            val sourceToTarget = canonical.mapNotNull { node ->
                node.sourceNode?.let { it to node.node }
            }.toMap()
            val nodeLimit = requestedCount.coerceIn(1, 60)
            val courseTimes = database.query(
                """
                SELECT course_times.id, course_times.startNode, course_times.step, course_times.ownTime
                FROM course_times
                INNER JOIN courses ON courses.id = course_times.courseId
                WHERE courses.tableId = ?
                ORDER BY course_times.id
                """.trimIndent(),
                arrayOf<Any?>(tableId),
            )
            try {
                val idIndex = courseTimes.getColumnIndexOrThrow("id")
                val startIndex = courseTimes.getColumnIndexOrThrow("startNode")
                val stepIndex = courseTimes.getColumnIndexOrThrow("step")
                val ownTimeIndex = courseTimes.getColumnIndexOrThrow("ownTime")
                while (courseTimes.moveToNext()) {
                    if (courseTimes.getInt(ownTimeIndex) != 0) continue
                    val startNode = courseTimes.getInt(startIndex)
                    val step = courseTimes.getInt(stepIndex)
                    if (step < 1) continue
                    val oldEnd = (startNode.toLong() + step.toLong() - 1L)
                        .coerceIn(1L, nodeLimit.toLong())
                        .toInt()
                    val mappedStart = mapLegacyNode(startNode, nodeLimit, canonical.size, sourceToTarget)
                    val mappedEnd = mapLegacyNode(oldEnd, nodeLimit, canonical.size, sourceToTarget)
                    val newStart = minOf(mappedStart, mappedEnd)
                    val newStep = maxOf(1, maxOf(mappedStart, mappedEnd) - newStart + 1)
                    if (newStart != startNode || newStep != step) {
                        database.execSQL(
                            "UPDATE course_times SET startNode = ?, step = ? WHERE id = ?",
                            arrayOf<Any?>(newStart, newStep, courseTimes.getLong(idIndex)),
                        )
                    }
                }
            } finally {
                courseTimes.close()
            }
        }

        private fun mapLegacyNode(
            node: Int,
            nodeLimit: Int,
            canonicalSize: Int,
            sourceToTarget: Map<Int, Int>,
        ): Int {
            sourceToTarget[node]?.let { return it }
            if (sourceToTarget.isEmpty()) return node.coerceIn(1, canonicalSize)
            return sourceToTarget.entries
                .minWith(
                    compareBy<Map.Entry<Int, Int>> { kotlin.math.abs(it.key - node.coerceIn(1, nodeLimit)) }
                        .thenBy { it.key },
                )
                .value
        }

        private fun canonicalMigrationNodes(
            rows: List<LegacyNodeRow>,
            requestedCount: Int,
        ): List<MigrationNode> {
            val nodeLimit = requestedCount.coerceIn(1, 60)
            val deduplicated = rows
                .filter { row ->
                    row.node in 1..nodeLimit &&
                        validMigrationRange(row.start, row.end) != null
                }
                .groupBy { it.node }
                .toSortedMap()
                .values
                .map { candidates -> candidates.minWith(compareBy<LegacyNodeRow> { it.id }) }
                .sortedWith(
                    compareBy<LegacyNodeRow> { validMigrationRange(it.start, it.end)!!.first }
                        .thenBy { validMigrationRange(it.start, it.end)!!.second }
                        .thenBy { it.node }
                        .thenBy { it.id },
                )

            val selected = mutableListOf<MigrationNode>()
            var lastEnd = -1
            deduplicated.forEach { row ->
                val range = validMigrationRange(row.start, row.end) ?: return@forEach
                if (range.first < lastEnd) return@forEach
                selected += MigrationNode(
                    node = selected.size + 1,
                    start = MinuteOfDay.format(range.first),
                    end = MinuteOfDay.format(range.second),
                    sourceNode = row.node,
                )
                lastEnd = range.second
            }
            if (selected.isNotEmpty()) return selected.take(60)

            return (1..nodeLimit).map { node ->
                val startMinute = 480 + (node - 1) * 10
                MigrationNode(
                    node = node,
                    start = MinuteOfDay.format(startMinute),
                    end = MinuteOfDay.format(startMinute + 9),
                    sourceNode = null,
                )
            }
        }

        private fun validMigrationRange(start: String, end: String): Pair<Int, Int>? {
            val startMinute = MinuteOfDay.parse(start) ?: return null
            val endMinute = MinuteOfDay.parse(end) ?: return null
            return if (startMinute < endMinute) startMinute to endMinute else null
        }

        @Volatile
        var instance: AppDatabase? = null
    }
}
