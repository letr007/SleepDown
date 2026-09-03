package com.letr.sleepdown.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.letr.sleepdown.logic.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

class TimetableRepository(context: Context) {

    private val db = AppDatabase.instance ?: synchronized(AppDatabase) {
        Room.databaseBuilder(context, AppDatabase::class.java, "timetable.db")
.addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
            .also { AppDatabase.instance = it }
    }
    private val dao = db.dao()

    fun observeTables(): Flow<List<TableEntity>> = dao.observeTables()

    fun observeTable(id: Long): Flow<TableEntity?> = dao.observeTable(id)

    suspend fun getTable(id: Long): TableEntity? = withContext(Dispatchers.IO) {
        dao.getTable(id)
    }

    fun observeCourses(tableId: Long): Flow<List<CourseEntity>> = dao.observeCourses(tableId)

    suspend fun getCourses(tableId: Long): List<CourseEntity> = withContext(Dispatchers.IO) {
        dao.getCourses(tableId)
    }

    suspend fun getAllCourses(tableId: Long): List<CourseEntity> = getCourses(tableId)

    fun observeTableWithMeta(id: Long): Flow<TableWithMeta?> =
        combine(dao.observeTable(id), dao.observeCourses(id), dao.observeAllTimes(id), dao.observeNodeTimes(id)) { table, courses, times, nodes ->
            if (table == null) return@combine null
            val courseById = courses.associateBy { it.id }
            TableWithMeta(
                table = table,
                nodeTimes = nodes,
                items = times.mapNotNull { t ->
                    courseById[t.courseId]?.let { CourseItem(it, t) }
                },
                courses = courses,
            )
        }

    suspend fun ensureDefaultTable(): Long = withContext(Dispatchers.IO) {
        if (dao.tableCount() == 0) {
            db.withTransaction {
                val monday = LocalDate.now().with(DayOfWeek.MONDAY)
                val id = dao.upsertTable(
                    TableEntity(
                        name = "我的课表",
                        startDate = monday.toEpochDay(),
                        maxWeek = 20,
                        nodeCount = 20,
                    ),
                )
                dao.upsertNodeTimes(defaultNodeTimes(id))
                id
            }
        } else {
            dao.getFirstTable()?.id ?: 0L
        }
    }

    suspend fun saveTable(table: TableEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertTable(table)
    }

    suspend fun deleteTable(table: TableEntity) = withContext(Dispatchers.IO) {
        dao.deleteTable(table)
    }

    suspend fun deleteTable(tableId: Long) = withContext(Dispatchers.IO) {
        val table = dao.getTable(tableId)
        if (table != null) dao.deleteTable(table)
    }

    suspend fun getNodeTimes(tableId: Long): List<NodeTimeEntity> = withContext(Dispatchers.IO) {
        dao.getNodeTimes(tableId)
    }

    /** 用一组节次替换课表原有的节次时间，避免删除后的旧行残留。 */
    suspend fun saveNodeTimes(times: List<NodeTimeEntity>) {
        if (times.isEmpty()) return
        replaceNodeTimes(times.first().tableId, times)
    }

    suspend fun saveNodeTimes(tableId: Long, times: List<NodeTimeEntity>) {
        replaceNodeTimes(tableId, times)
    }

    private suspend fun replaceNodeTimes(tableId: Long, times: List<NodeTimeEntity>) =
        withContext(Dispatchers.IO) {
            require(times.all { it.tableId == tableId }) { "所有节次必须属于同一张课表" }
            db.withTransaction {
                dao.deleteNodeTimes(tableId)
                dao.upsertNodeTimes(times)
            }
        }

    /** 在课表末尾添加一节，并同步增加课表的节次数。 */
    suspend fun addNode(
        tableId: Long,
        start: String = "00:00",
        end: String = "00:00",
    ): NodeTimeEntity = withContext(Dispatchers.IO) {
        db.withTransaction {
            val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            val nextNode = table.nodeCount + 1
            require(nextNode <= MAX_NODE_COUNT) { "节次不能超过 $MAX_NODE_COUNT" }
            val existingNode = dao.getNodeTimes(tableId).firstOrNull { it.node == nextNode }
            val nodeTime = existingNode ?: NodeTimeEntity(
                tableId = tableId,
                node = nextNode,
                start = start,
                end = end,
            )
            if (existingNode == null) dao.upsertNodeTimes(listOf(nodeTime))
            if (table.nodeCount < nextNode) {
                dao.upsertTable(table.copy(nodeCount = nextNode))
            }
            nodeTime
        }
    }

    suspend fun addNodeTime(
        tableId: Long,
        start: String = "00:00",
        end: String = "00:00",
    ): NodeTimeEntity = addNode(tableId, start, end)

    /** 删除一节并整理后续节次编号，同时保持课程时间段与节次对齐。 */
    suspend fun deleteNode(tableId: Long, node: Int) = withContext(Dispatchers.IO) {
        require(node >= 1) { "节次必须从 1 开始" }
        db.withTransaction {
            val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            require(node <= table.nodeCount) { "节次超出课表范围: $node" }

            val remainingNodes = dao.getNodeTimes(tableId)
                .filterNot { it.node == node }
                .sortedBy { it.node }
                .mapIndexed { index, item -> item.copy(node = index + 1) }
            dao.deleteNodeTimes(tableId)
            dao.upsertNodeTimes(remainingNodes)

            for (course in dao.getCourses(tableId)) {
                for (time in dao.getTimes(course.id)) {
                    val adjusted = adjustAfterNodeDeletion(time, node)
                    if (adjusted == null) dao.deleteTime(time) else dao.upsertTime(adjusted)
                }
            }
            val newNodeCount = maxOf(
                1,
                table.nodeCount - 1,
                remainingNodes.maxOfOrNull { it.node } ?: 0,
            )
            dao.upsertTable(table.copy(nodeCount = newNodeCount))
        }
    }

    suspend fun deleteNodeTime(tableId: Long, node: Int) = deleteNode(tableId, node)

    /** 保存一门课（含全部时间段）：先删旧时间段再整体写入 */
    suspend fun saveCourse(tableId: Long, course: CourseEntity, times: List<CourseTimeEntity>): Long =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val savedCourse = course.copy(tableId = tableId)
                val courseId = if (savedCourse.id == 0L) {
                    dao.upsertCourse(savedCourse)
                } else {
                    dao.upsertCourse(savedCourse)
                    savedCourse.id
                }
                for (time in dao.getTimes(courseId)) dao.deleteTime(time)
                for (time in times) dao.upsertTime(time.copy(courseId = courseId))
                courseId
            }
        }

    suspend fun getCourseTimes(courseId: Long): List<CourseTimeEntity> = withContext(Dispatchers.IO) {
        dao.getTimes(courseId)
    }

    suspend fun deleteCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        dao.deleteCourse(course)
    }

    suspend fun getCourse(courseId: Long): CourseEntity? = withContext(Dispatchers.IO) {
        dao.getCourseById(courseId)
    }

    /** 从 WakeUp 兼容 CSV 导入为新课表 */
    suspend fun importCsv(name: String, startDate: Long, csvText: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val parsed = CsvParser.parse(csvText, startDate).getOrThrow()
                val count = db.withTransaction {
                    val tableId = dao.upsertTable(
                        TableEntity(name = name, startDate = startDate, maxWeek = 20, nodeCount = 20),
                    )
                    dao.upsertNodeTimes(defaultNodeTimes(tableId))
                    var importedCount = 0
                    for ((courseName, rows) in parsed.groupBy { it.name }) {
                        val courseId = dao.upsertCourse(
                            CourseEntity(
                                tableId = tableId,
                                name = courseName,
                                color = 0,
                                teacher = rows.firstNotNullOfOrNull { r -> r.teacher.takeIf { it.isNotBlank() } } ?: "",
                            ),
                        )
                        for (row in rows) {
                            for (seg in row.weekSegments) {
                                dao.upsertTime(
                                    CourseTimeEntity(
                                        courseId = courseId,
                                        day = row.day,
                                        startNode = row.startNode,
                                        step = row.endNode - row.startNode + 1,
                                        startWeek = seg.start,
                                        endWeek = seg.end,
                                        weekType = seg.weekType,
                                        room = row.room,
                                    ),
                                )
                            }
                        }
                        importedCount += rows.size
                    }
                    importedCount
                }
                Result.success(count)
            } catch (error: Throwable) {
                Result.failure<Int>(error)
            }
        }

    /** 复制课表及其节次、课程和课程时间段，返回新课表 id。 */
    suspend fun copyTable(sourceTableId: Long, name: String? = null): Long =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val source = requireNotNull(dao.getTable(sourceTableId)) { "课表不存在: $sourceTableId" }
                val copyName = name?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "${source.name} 副本"
                val newTableId = dao.upsertTable(source.copy(id = 0L, name = copyName))
                dao.upsertNodeTimes(
                    dao.getNodeTimes(sourceTableId).map {
                        it.copy(id = 0L, tableId = newTableId)
                    },
                )
                for (course in dao.getCourses(sourceTableId)) {
                    val newCourseId = dao.upsertCourse(
                        course.copy(id = 0L, tableId = newTableId),
                    )
                    for (time in dao.getTimes(course.id)) {
                        dao.upsertTime(time.copy(id = 0L, courseId = newCourseId))
                    }
                }
                newTableId
            }
        }

    suspend fun copyTable(source: TableEntity, name: String? = null): Long =
        copyTable(source.id, name)

    suspend fun duplicateTable(sourceTableId: Long, name: String? = null): Long =
        copyTable(sourceTableId, name)

    suspend fun removeNode(tableId: Long, node: Int) = deleteNode(tableId, node)

    /** 获取课表的完整快照，包含没有时间段的课程。 */
    suspend fun tableSnapshot(tableId: Long): TableWithMeta? = withContext(Dispatchers.IO) {
        db.withTransaction {
            val table = dao.getTable(tableId) ?: return@withTransaction null
            val courses = dao.getCourses(tableId)
            val courseById = courses.associateBy { it.id }
            val items = mutableListOf<CourseItem>()
            for (course in courses) {
                for (time in dao.getTimes(course.id)) {
                    courseById[time.courseId]?.let { items += CourseItem(it, time) }
                }
            }
            TableWithMeta(
                table = table,
                nodeTimes = dao.getNodeTimes(tableId),
                items = items,
                courses = courses,
            )
        }
    }

    /** 导出 JSON 备份（本应用自有格式）。 */
    suspend fun exportBackup(tableId: Long): String = withContext(Dispatchers.IO) {
        db.withTransaction {
            val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            val courses = dao.getCourses(tableId)
            val times = mutableListOf<CourseTimeEntity>()
            for (course in courses) times += dao.getTimes(course.id)
            BackupCodec.encode(
                BackupData(
                    table = table,
                    nodeTimes = dao.getNodeTimes(tableId),
                    courses = courses,
                    times = times,
                ),
            )
        }
    }

    suspend fun exportBackupResult(tableId: Long): Result<String> = try {
        Result.success(exportBackup(tableId))
    } catch (error: Throwable) {
        Result.failure<String>(error)
    }

    /** 导入 JSON 备份（本应用导出的格式），返回新课表 id */
    suspend fun importBackup(json: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val backup = BackupCodec.decode(json)
            val tableId = db.withTransaction {
                val newTableId = dao.upsertTable(backup.table.copy(id = 0L))
                dao.upsertNodeTimes(
                    backup.nodeTimes.map { it.copy(id = 0L, tableId = newTableId) },
                )
                val timesByCourse = backup.times.groupBy { it.courseId }
                for (course in backup.courses) {
                    val courseId = dao.upsertCourse(
                        course.copy(id = 0L, tableId = newTableId),
                    )
                    for (time in timesByCourse[course.id].orEmpty()) {
                        dao.upsertTime(time.copy(id = 0L, courseId = courseId))
                    }
                }
                newTableId
            }
            Result.success(tableId)
        } catch (error: Throwable) {
            Result.failure<Long>(error)
        }
    }

    companion object {
        const val MAX_NODE_COUNT = 60

        private fun adjustAfterNodeDeletion(
            time: CourseTimeEntity,
            deletedNode: Int,
        ): CourseTimeEntity? {
            val start = time.startNode
            val end = start + time.step - 1
            return when {
                end < deletedNode -> time
                start > deletedNode -> time.copy(startNode = start - 1)
                end == deletedNode && start == deletedNode -> null
                else -> time.copy(step = time.step - 1)
            }
        }

        /** 与 WakeUp 一致的默认作息：每节 45 分钟 + 10 分钟休息，午休 2 小时 */
        fun defaultNodeTimes(tableId: Long): List<NodeTimeEntity> {
            val slots = listOf(
                "08:00" to "08:50", "09:00" to "09:50", "10:10" to "11:00", "11:10" to "12:00",
                "13:30" to "14:20", "14:30" to "15:20", "15:40" to "16:30", "16:40" to "17:30",
                "18:30" to "19:20", "19:30" to "20:20", "20:30" to "21:20", "21:25" to "21:30",
                "21:35" to "21:40", "21:45" to "21:50", "21:55" to "22:00", "22:05" to "22:10",
                "22:15" to "22:20", "22:25" to "22:30", "22:35" to "22:40", "22:45" to "22:50",
                "22:55" to "23:00", "23:05" to "23:10", "23:15" to "23:20", "23:25" to "23:30",
                "23:35" to "23:40", "23:45" to "23:50", "23:51" to "23:55", "23:56" to "23:59",
                "00:00" to "00:00", "00:00" to "00:00",
            )
            return slots.mapIndexed { i, (s, e) ->
                NodeTimeEntity(tableId = tableId, node = i + 1, start = s, end = e)
            }
        }
    }
}
