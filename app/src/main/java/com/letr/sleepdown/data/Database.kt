package com.letr.sleepdown.data

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
    @Query("SELECT * FROM tables ORDER BY id")
    fun observeTables(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    fun observeTable(id: Long): Flow<TableEntity?>

    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getTable(id: Long): TableEntity?

    @Query("SELECT * FROM tables ORDER BY id LIMIT 1")
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

    @Upsert
    suspend fun upsertCourse(course: CourseEntity): Long

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("SELECT * FROM course_times WHERE courseId = :courseId")
    suspend fun getTimes(courseId: Long): List<CourseTimeEntity>

    @Query("SELECT * FROM course_times WHERE courseId IN (SELECT id FROM courses WHERE tableId = :tableId)")
    fun observeAllTimes(tableId: Long): Flow<List<CourseTimeEntity>>

    @Upsert
    suspend fun upsertTime(time: CourseTimeEntity): Long

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
}

@Database(
    entities = [TableEntity::class, CourseEntity::class, CourseTimeEntity::class, NodeTimeEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TimetableDao

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

        @Volatile
        var instance: AppDatabase? = null
    }
}
