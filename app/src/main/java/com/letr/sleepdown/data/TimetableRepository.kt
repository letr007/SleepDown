package com.letr.sleepdown.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.letr.sleepdown.domain.ConflictPreference
import com.letr.sleepdown.domain.DateException
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.ReminderSettings
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableCommand
import com.letr.sleepdown.domain.TimetableCommands
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.domain.TimetableValidationException
import com.letr.sleepdown.domain.TimeTableValidator
import com.letr.sleepdown.interchange.BackupFormat
import com.letr.sleepdown.interchange.BackupImportMode
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
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()
            .also { AppDatabase.instance = it }
    }
    private val dao = db.dao()
    private val timeTableDao = db.timeTableDao()
    private val dateExceptionDao = db.dateExceptionDao()
    private val conflictPreferenceDao = db.conflictPreferenceDao()
    private val reminderSettingsDao = db.reminderSettingsDao()

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
                items = times.mapNotNull { time ->
                    courseById[time.courseId]?.let { course -> CourseItem(course, time) }
                },
                courses = courses,
            )
        }

    /** Loads the first table, creating a valid default table and time table when needed. */
    suspend fun ensureDefaultTable(defaultName: String): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.getFirstTable()?.let { existing ->
                val timeTableId = ensureTimeTableForTable(existing)
                if (reminderSettingsDao.get(existing.id) == null) {
                    reminderSettingsDao.upsert(ReminderSettingsEntity(tableId = existing.id))
                }
                if (dao.getNodeTimes(existing.id).isEmpty()) {
                    replaceLegacyProjection(
                        existing.id,
                        timeTableDao.getNodes(timeTableId).map { node ->
                            NodeTimeEntity(
                                tableId = existing.id,
                                node = node.node,
                                start = node.start,
                                end = node.end,
                            )
                        },
                    )
                }
                return@withTransaction existing.id
            }

            val table = TableEntity(
                name = defaultName.trim().also { require(it.isNotEmpty()) { "defaultName must not be blank" } },
                startDate = LocalDate.now().with(DayOfWeek.MONDAY).toEpochDay(),
                maxWeek = 20,
                nodeCount = 20,
            )
            val tableId = insertTable(table)
            val nodes = defaultCanonicalNodeTimes(tableId, table.nodeCount)
            val timeTableId = persistCanonicalTimeTable(
                requestedId = 0L,
                name = table.name,
                sortOrder = table.sortOrder,
                nodes = nodes,
            )
            dao.upsertTable(table.copy(id = tableId, timeTableId = timeTableId))
            reminderSettingsDao.upsert(ReminderSettingsEntity(tableId = tableId))
            replaceLegacyProjection(tableId, defaultNodeTimes(tableId))
            tableId
        }
    }

    /**
     * Keeps the old UI-facing save method. A table always gets a valid reusable
     * time table before the method returns, even when callers use timeTableId=0.
     */
    suspend fun saveTable(table: TableEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = table.id.takeIf { it > 0L }?.let { dao.getTable(it) }
            val tableToSave = if (existing != null && existing.timeTableId > 0L && table.timeTableId == 0L) {
                table.copy(timeTableId = existing.timeTableId)
            } else {
                table
            }
            val tableId = insertTable(tableToSave)
            val saved = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            val timeTableId = if (saved.timeTableId > 0L) {
                ensureTimeTableForTable(saved)
            } else {
                val legacyNodes = dao.getNodeTimes(tableId)
                val sourceNodes = if (legacyNodes.isNotEmpty()) {
                    selectCanonicalNodes(saved, legacyNodes)
                } else {
                    defaultCanonicalNodeTimes(tableId, saved.nodeCount)
                }
                persistCanonicalTimeTable(
                    requestedId = 0L,
                    name = saved.name,
                    sortOrder = saved.sortOrder,
                    nodes = sourceNodes,
                )
            }
            if (saved.timeTableId != timeTableId) {
                dao.upsertTable(saved.copy(timeTableId = timeTableId))
            }
            if (reminderSettingsDao.get(tableId) == null) {
                reminderSettingsDao.upsert(ReminderSettingsEntity(tableId = tableId))
            }
            if (existing == null || existing.timeTableId != timeTableId || dao.getNodeTimes(tableId).isEmpty()) {
                replaceLegacyProjection(
                    tableId,
                    timeTableDao.getNodes(timeTableId).map { node ->
                        NodeTimeEntity(
                            tableId = tableId,
                            node = node.node,
                            start = node.start,
                            end = node.end,
                        )
                    },
                )
            }
            tableId
        }
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

    /**
     * Replaces the legacy projection and the canonical time table together. The
     * list overload remains a no-op for an empty list for source compatibility.
     */
    suspend fun saveNodeTimes(times: List<NodeTimeEntity>) {
        if (times.isEmpty()) return
        saveNodeTimes(times.first().tableId, times)
    }

    suspend fun saveNodeTimes(tableId: Long, times: List<NodeTimeEntity>) {
        require(times.isNotEmpty()) { "时间表至少需要一节有效节次" }
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
                require(times.all { it.tableId == tableId }) { "所有节次必须属于同一张课表" }
                val sorted = times.sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id })
                val canonicalNodes = selectCanonicalNodes(table, sorted)
                val canonicalId = ensureTimeTableForTable(table)
                persistCanonicalTimeTable(
                    requestedId = canonicalId,
                    name = requireNotNull(timeTableDao.getTimeTable(canonicalId)).name,
                    sortOrder = requireNotNull(timeTableDao.getTimeTable(canonicalId)).sortOrder,
                    nodes = canonicalNodes,
                )
                // Preserve the complete legacy projection for the existing UI.
                replaceLegacyProjection(tableId, sorted)
            }
        }
    }

    /** Adds one node to the table's shared time table and updates its projections. */
    suspend fun addNode(
        tableId: Long,
        start: String = "00:00",
        end: String = "00:00",
    ): NodeTimeEntity = withContext(Dispatchers.IO) {
        db.withTransaction {
            val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            val canonicalId = ensureTimeTableForTable(table)
            val canonicalNodes = timeTableDao.getNodes(canonicalId).sortedBy { it.node }
            val nextNode = table.nodeCount + 1
            require(nextNode <= MAX_NODE_COUNT) { "节次不能超过 $MAX_NODE_COUNT" }
            val existing = canonicalNodes.firstOrNull { it.node == nextNode }
            if (existing != null) {
                dao.upsertTable(table.copy(nodeCount = nextNode))
                return@withTransaction NodeTimeEntity(
                    tableId = tableId,
                    node = existing.node,
                    start = existing.start,
                    end = existing.end,
                )
            }
            val fallbackStart = canonicalNodes.lastOrNull()?.end ?: "08:00"
            val requestedRange = parseValidRange(start, end)
            val nodeStart = requestedRange?.first ?: fallbackStart
            val nodeEnd = requestedRange?.second ?: addMinutes(nodeStart, DEFAULT_NODE_DURATION_MINUTES)
            val node = NodeTimeEntity(tableId = tableId, node = nextNode, start = nodeStart, end = nodeEnd)
            val updatedNodes = canonicalNodes + TimeTableNodeEntity(
                timeTableId = canonicalId,
                node = nextNode,
                start = nodeStart,
                end = nodeEnd,
            )
            persistCanonicalTimeTable(
                requestedId = canonicalId,
                name = requireNotNull(timeTableDao.getTimeTable(canonicalId)).name,
                sortOrder = requireNotNull(timeTableDao.getTimeTable(canonicalId)).sortOrder,
                nodes = updatedNodes.map { row ->
                    NodeTimeEntity(
                        tableId = tableId,
                        node = row.node,
                        start = row.start,
                        end = row.end,
                    )
                },
            )
            dao.upsertTable(table.copy(nodeCount = nextNode))
            node
        }
    }

    suspend fun addNodeTime(
        tableId: Long,
        start: String = "00:00",
        end: String = "00:00",
    ): NodeTimeEntity = addNode(tableId, start, end)

    /** Deletes and renumbers one node, keeping course times aligned with it. */
    suspend fun deleteNode(tableId: Long, node: Int) = withContext(Dispatchers.IO) {
        require(node >= 1) { "节次必须从 1 开始" }
        db.withTransaction {
            val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            require(node <= table.nodeCount) { "节次超出课表范围: $node" }
            val canonicalId = ensureTimeTableForTable(table)
            val remainingNodes = timeTableDao.getNodes(canonicalId)
                .filterNot { it.node == node }
                .sortedBy { it.node }
                .mapIndexed { index, item -> item.copy(node = index + 1) }
            require(remainingNodes.isNotEmpty()) { "时间表至少需要一节有效节次" }
            persistCanonicalTimeTable(
                requestedId = canonicalId,
                name = requireNotNull(timeTableDao.getTimeTable(canonicalId)).name,
                sortOrder = requireNotNull(timeTableDao.getTimeTable(canonicalId)).sortOrder,
                nodes = remainingNodes.map { item ->
                    NodeTimeEntity(tableId = tableId, node = item.node, start = item.start, end = item.end)
                },
            )

            for (course in dao.getCourses(tableId)) {
                for (time in dao.getTimes(course.id)) {
                    val adjusted = adjustAfterNodeDeletion(time, node)
                    if (adjusted == null) dao.deleteTime(time) else dao.upsertTime(adjusted)
                }
            }
            val newNodeCount = minOf(
                maxOf(1, table.nodeCount - 1),
                remainingNodes.size,
            )
            dao.upsertTable(table.copy(nodeCount = newNodeCount))
        }
    }

    suspend fun deleteNodeTime(tableId: Long, node: Int) = deleteNode(tableId, node)

    /** Saves one course and all of its recurrence rows in one transaction. */
    suspend fun saveCourse(tableId: Long, course: CourseEntity, times: List<CourseTimeEntity>): Long =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val initialTable = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
                val canonicalId = ensureTimeTableForTable(initialTable)
                val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
                val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                    "课表不存在: $tableId"
                }
                val existingCourses = dao.getCourses(tableId)
                if (course.id > 0L) {
                    val existing = requireNotNull(dao.getCourseById(course.id)) {
                        "课程不存在: ${course.id}"
                    }
                    require(existing.tableId == tableId) { "课程 ${course.id} 不属于课表 $tableId" }
                }
                val existingTimes = existingCourses.flatMap { existingCourse ->
                    dao.getTimes(existingCourse.id)
                }
                val savedCourseInput = course.copy(tableId = tableId)
                val courseId = if (savedCourseInput.id == 0L) {
                    // The enclosing transaction rolls this insert back if validation fails.
                    dao.upsertCourse(savedCourseInput)
                } else {
                    savedCourseInput.id
                }
                val savedCourse = savedCourseInput.copy(id = courseId)
                val usedExistingTimeIds = mutableSetOf<Long>()
                val safeTimes = times.map { time ->
                    val keepId = time.id > 0L &&
                        existingTimes.any { it.id == time.id && it.courseId == courseId } &&
                        usedExistingTimeIds.add(time.id)
                    time.copy(id = if (keepId) time.id else 0L, courseId = courseId)
                }
                val normalized = normalizeCourseTimes(courseId, savedCourse, safeTimes)
                val canonicalNodes = timeTableDao.getNodes(canonicalId)
                val reusableTimeTable = timeTableDao.getTimeTable(canonicalId)
                val legacyNodes = dao.getNodeTimes(tableId)
                val courseDomain = DomainMappers.toDomain(
                    TimetableEntityRows(
                        table = table,
                        courses = listOf(savedCourse),
                        courseTimes = normalized,
                        nodeTimes = legacyNodes,
                        reusableTimeTable = reusableTimeTable,
                        timeTableNodes = canonicalNodes,
                    ),
                ).courses.single()
                val candidateBase = current.copy(
                    courses = current.courses
                        .filterNot { it.id == courseId.toString() } + courseDomain,
                    dateExceptions = emptyList(),
                    conflictPreferences = emptyList(),
                )
                TimeTableValidator.requireValid(candidateBase, "Course")
                val candidateWithExceptions = candidateBase.copy(
                    dateExceptions = reconcileDateExceptions(candidateBase, current.dateExceptions),
                )
                val candidate = candidateWithExceptions.copy(
                    conflictPreferences = reconcileConflictPreferences(current, candidateWithExceptions),
                )
                TimeTableValidator.requireValid(candidate, "Course")

                val candidateTimes = existingTimes
                    .filterNot { it.courseId == courseId } + normalized
                val referenceRows = DomainMappers.toEntities(
                    timetable = candidate,
                    tableId = tableId,
                    courseIdByDomainId = candidate.courses.associate { it.id to it.id.toLong() },
                    timeTableId = canonicalId,
                    existingTable = table,
                    existingTimeTable = reusableTimeTable,
                    existingCourseTimes = candidateTimes,
                    existingNodeTimes = legacyNodes,
                    existingTimeTableNodes = canonicalNodes,
                )

                dao.upsertCourse(savedCourse)
                dao.deleteTimes(courseId)
                if (normalized.isNotEmpty()) dao.upsertTimes(normalized)
                dao.deleteDateExceptions(tableId)
                referenceRows.dateExceptions.forEach { exception ->
                    dateExceptionDao.upsert(exception)
                }
                dao.deleteConflictPreferences(tableId)
                referenceRows.conflictPreferences.forEach { preference ->
                    conflictPreferenceDao.upsert(preference)
                }
                courseId
            }
        }

    suspend fun getCourseTimes(courseId: Long): List<CourseTimeEntity> = withContext(Dispatchers.IO) {
        dao.getTimes(courseId)
    }

    suspend fun deleteCourse(course: CourseEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = dao.getCourseById(course.id) ?: return@withTransaction
            val slotIds = dao.getTimes(course.id).mapTo(mutableSetOf()) { time ->
                usableLogicalSlotId(course.id, time)
            }
            dateExceptionDao.getForTable(existing.tableId)
                .filter { it.logicalSlotId in slotIds }
                .forEach { dateExceptionDao.delete(it) }
            conflictPreferenceDao.getForTable(existing.tableId)
                .filter { it.courseId == course.id || it.logicalSlotId in slotIds }
                .forEach { conflictPreferenceDao.delete(it) }
            dao.deleteCourse(existing)
        }
    }

    suspend fun getCourse(courseId: Long): CourseEntity? = withContext(Dispatchers.IO) {
        dao.getCourseById(courseId)
    }

    /** Imports the old WakeUp-compatible CSV as a new table. */
    suspend fun importCsv(name: String, startDate: Long, csvText: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val parsed = CsvParser.parse(csvText, startDate).getOrThrow()
                val count = db.withTransaction {
                    val baseTable = TableEntity(name = name, startDate = startDate, maxWeek = 20, nodeCount = 20)
                    val tableId = insertTable(baseTable)
                    val canonicalId = persistCanonicalTimeTable(
                        requestedId = 0L,
                        name = baseTable.name,
                        sortOrder = baseTable.sortOrder,
                        nodes = defaultCanonicalNodeTimes(tableId, baseTable.nodeCount),
                    )
                    dao.upsertTable(baseTable.copy(id = tableId, timeTableId = canonicalId))
                    replaceLegacyProjection(tableId, defaultNodeTimes(tableId))

                    var importedCount = 0
                    for ((courseName, rows) in parsed.groupBy { it.name }) {
                        val courseId = dao.upsertCourse(
                            CourseEntity(
                                tableId = tableId,
                                name = courseName,
                                color = 0,
                                teacher = rows.firstNotNullOfOrNull { row ->
                                    row.teacher.takeIf { it.isNotBlank() }
                                } ?: "",
                            ),
                        )
                        val importedTimes = rows.flatMap { row ->
                            row.weekSegments.map { segment ->
                                CourseTimeEntity(
                                    courseId = courseId,
                                    day = row.day,
                                    startNode = row.startNode,
                                    step = row.endNode - row.startNode + 1,
                                    startWeek = segment.start,
                                    endWeek = segment.end,
                                    weekType = segment.weekType,
                                    room = row.room,
                                )
                            }
                        }
                        val course = requireNotNull(dao.getCourseById(courseId))
                        val normalized = normalizeCourseTimes(courseId, course, importedTimes)
                        if (normalized.isNotEmpty()) dao.upsertTimes(normalized)
                        importedCount += rows.size
                    }
                    importedCount
                }
                Result.success(count)
            } catch (error: Throwable) {
                Result.failure<Int>(error)
            }
        }

    /** Copies a table while preserving its reusable time-table reference. */
    suspend fun copyTable(sourceTableId: Long, name: String? = null): Long =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val sourceBeforeEnsure = requireNotNull(dao.getTable(sourceTableId)) { "课表不存在: $sourceTableId" }
                ensureTimeTableForTable(sourceBeforeEnsure)
                val source = requireNotNull(dao.getTable(sourceTableId))
                val sourceDomain = loadDomainTimetableOrNullInternal(sourceTableId)
                val copyName = name?.trim()?.takeIf { it.isNotEmpty() } ?: "${source.name} 副本"
                val newTableId = dao.upsertTable(source.copy(id = 0L, name = copyName))
                val sourceNodes = dao.getNodeTimes(sourceTableId).ifEmpty {
                    timeTableDao.getNodes(source.timeTableId).map { node ->
                        NodeTimeEntity(
                            tableId = sourceTableId,
                            node = node.node,
                            start = node.start,
                            end = node.end,
                        )
                    }
                }
                replaceLegacyProjection(newTableId, sourceNodes.map {
                    it.copy(id = 0L, tableId = newTableId)
                })
                val courseIdMap = mutableMapOf<Long, Long>()
                for (course in dao.getCourses(sourceTableId)) {
                    val newCourseId = dao.upsertCourse(course.copy(id = 0L, tableId = newTableId))
                    courseIdMap[course.id] = newCourseId
                    val copiedTimes = dao.getTimes(course.id).map {
                        it.copy(id = 0L, courseId = newCourseId)
                    }
                    val normalized = normalizeCourseTimes(
                        newCourseId,
                        requireNotNull(dao.getCourseById(newCourseId)),
                        copiedTimes,
                    )
                    if (normalized.isNotEmpty()) dao.upsertTimes(normalized)
                }
                sourceDomain?.dateExceptions?.forEach { exception ->
                    dateExceptionDao.upsert(
                        toDateExceptionEntity(
                            exception = exception.copy(
                                id = stableId(
                                    "copied-exception",
                                    sourceTableId.toString(),
                                    newTableId.toString(),
                                    exception.id,
                                ),
                            ),
                            tableId = newTableId,
                        ),
                    )
                } ?: dateExceptionDao.getForTable(sourceTableId).forEach { exception ->
                    val domainId = stableId(
                        "copied-exception",
                        sourceTableId.toString(),
                        newTableId.toString(),
                        dateExceptionDomainId(sourceTableId, exception.id),
                    )
                    dateExceptionDao.upsert(
                        exception.copy(
                            id = dateExceptionStorageId(newTableId, domainId),
                            tableId = newTableId,
                        ),
                    )
                }
                val copiedDomain = loadDomainTimetableOrNullInternal(newTableId)
                val occurrenceIds = occurrenceIdMap(sourceDomain, copiedDomain)
                sourceDomain?.conflictPreferences?.forEach { preference ->
                    val newCourseId = courseIdMap[preference.courseId.toLongOrNull()]
                    if (newCourseId != null) {
                        conflictPreferenceDao.upsert(
                            ConflictPreferenceEntity(
                                tableId = newTableId,
                                courseId = newCourseId,
                                priority = preference.priority,
                                logicalSlotId = preference.logicalSlotId,
                                occurrenceId = preference.occurrenceId?.let {
                                    requireNotNull(occurrenceIds[it]) { "无法复制冲突优先级对应的课程实例" }
                                },
                                epochDay = preference.epochDay,
                            ),
                        )
                    }
                } ?: conflictPreferenceDao.getForTable(sourceTableId).forEach { preference ->
                    courseIdMap[preference.courseId]?.let { newCourseId ->
                        conflictPreferenceDao.upsert(
                            preference.copy(id = 0L, tableId = newTableId, courseId = newCourseId),
                        )
                    }
                }
                reminderSettingsDao.get(sourceTableId)?.let { settings ->
                    reminderSettingsDao.upsert(settings.copy(tableId = newTableId))
                }
                newTableId
            }
        }

    suspend fun copyTable(source: TableEntity, name: String? = null): Long = copyTable(source.id, name)

    suspend fun duplicateTable(sourceTableId: Long, name: String? = null): Long = copyTable(sourceTableId, name)

    suspend fun removeNode(tableId: Long, node: Int) = deleteNode(tableId, node)

    /** Loads the complete shared aggregate for a Room table. */
    suspend fun loadDomainTimetable(tableId: Long): Timetable = withContext(Dispatchers.IO) {
        db.withTransaction {
            requireNotNull(loadDomainTimetableOrNullInternal(tableId)) { "课表不存在: $tableId" }
        }
    }

    suspend fun loadDomainTimetableOrNull(tableId: Long): Timetable? = withContext(Dispatchers.IO) {
        db.withTransaction { loadDomainTimetableOrNullInternal(tableId) }
    }

    /** Applies a shared command atomically and returns the committed aggregate. */
    suspend fun applyTimetableCommand(tableId: Long, command: TimetableCommand): Timetable =
        applyTimetableCommands(tableId, listOf(command))

    suspend fun applyTimetableCommands(tableId: Long, commands: List<TimetableCommand>): Timetable =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val before = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                    "课表不存在: $tableId"
                }
                val after = commands.fold(before, TimetableCommands::apply)
                persistLoadedDomainTimetable(tableId, after)
                after
            }
        }

    suspend fun applyCommand(tableId: Long, command: TimetableCommand): Timetable =
        applyTimetableCommand(tableId, command)

    suspend fun apply(tableId: Long, command: TimetableCommand): Timetable =
        applyTimetableCommand(tableId, command)

    suspend fun applyTimetableCommand(command: TimetableCommand, tableId: Long): Timetable =
        applyTimetableCommand(tableId, command)

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

    /** Exports the shared, versioned backup format. */
    suspend fun exportBackup(tableId: Long): String = withContext(Dispatchers.IO) {
        BackupFormat.encode(loadDomainTimetable(tableId))
    }

    suspend fun exportBackupResult(tableId: Long): Result<String> = try {
        Result.success(exportBackup(tableId))
    } catch (error: Throwable) {
        Result.failure<String>(error)
    }

    /** Imports a backup as a new table, preserving the original one-argument API. */
    suspend fun importBackup(json: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val imported = decodeBackup(json)
            val tableId = db.withTransaction {
                val baseTable = TableEntity(
                    name = imported.name,
                    startDate = imported.firstDayEpochDay,
                    maxWeek = imported.maxWeek,
                    nodeCount = imported.timeTable.nodes.size.coerceIn(1, MAX_NODE_COUNT),
                    showWeekend = imported.showSaturday || imported.showSunday,
                    showSat = imported.showSaturday,
                    showSun = imported.showSunday,
                    sundayFirst = imported.sundayFirst,
                    sortOrder = imported.sortOrder,
                )
                val newTableId = insertTable(baseTable)
                persistImportedDomainTimetable(
                    tableId = newTableId,
                    imported = imported,
                    mode = BackupImportMode.REPLACE,
                )
                newTableId
            }
            Result.success(tableId)
        } catch (error: Throwable) {
            Result.failure<Long>(error)
        }
    }

    /** Applies MERGE or REPLACE to a selected existing table. */
    suspend fun importBackup(
        tableId: Long,
        json: String,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val imported = decodeBackup(json)
            db.withTransaction {
                requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
                val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId))
                val result = com.letr.sleepdown.interchange.BackupImportPolicy.apply(current, imported, mode)
                persistImportedDomainTimetable(tableId, result.timetable, mode)
            }
            Result.success(tableId)
        } catch (error: Throwable) {
            Result.failure<Long>(error)
        }
    }

    suspend fun importBackup(
        json: String,
        tableId: Long,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): Result<Long> = importBackup(tableId, json, mode)

    suspend fun importBackup(
        json: String,
        mode: BackupImportMode,
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val tableId = db.withTransaction { dao.getFirstTable()?.id }
            if (tableId == null) importBackup(json) else importBackup(tableId, json, mode)
        } catch (error: Throwable) {
            Result.failure<Long>(error)
        }
    }

    suspend fun importBackupInto(
        tableId: Long,
        json: String,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): Result<Long> = importBackup(tableId, json, mode)

    suspend fun importDomainTimetable(
        tableId: Long,
        imported: Timetable,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): Timetable = withContext(Dispatchers.IO) {
        db.withTransaction {
            val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                "课表不存在: $tableId"
            }
            val result = com.letr.sleepdown.interchange.BackupImportPolicy.apply(current, imported, mode)
            persistImportedDomainTimetable(tableId, result.timetable, mode)
            requireNotNull(loadDomainTimetableOrNullInternal(tableId))
        }
    }

    /** Persists a parsed aggregate as a new Room table. */
    suspend fun importDomainTimetable(imported: Timetable): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val validated = com.letr.sleepdown.interchange.BackupImportPolicy
                .apply(null, imported, BackupImportMode.REPLACE)
                .timetable
            val baseTable = TableEntity(
                name = validated.name,
                startDate = validated.firstDayEpochDay,
                maxWeek = validated.maxWeek,
                nodeCount = validated.timeTable.nodes.size.coerceIn(1, MAX_NODE_COUNT),
                showWeekend = validated.showSaturday || validated.showSunday,
                showSat = validated.showSaturday,
                showSun = validated.showSunday,
                sundayFirst = validated.sundayFirst,
                sortOrder = validated.sortOrder,
            )
            val tableId = insertTable(baseTable)
            persistImportedDomainTimetable(tableId, validated, BackupImportMode.REPLACE)
            tableId
        }
    }

    // ---- Persistent table ordering -------------------------------------------------

    suspend fun reorderTables(orderedTableIds: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = dao.getAllTables()
            require(existing.map { it.id }.toSet() == orderedTableIds.toSet() &&
                existing.size == orderedTableIds.size) {
                "排序列表必须包含每一张课表且不能重复"
            }
            orderedTableIds.forEachIndexed { index, tableId ->
                dao.updateTableSortOrder(tableId, index)
            }
        }
    }

    suspend fun saveTableOrder(orderedTableIds: List<Long>) = reorderTables(orderedTableIds)

    suspend fun setTableSortOrder(tableId: Long, sortOrder: Int) = withContext(Dispatchers.IO) {
        db.withTransaction {
            requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            dao.updateTableSortOrder(tableId, sortOrder)
        }
    }

    // ---- Reusable time tables ------------------------------------------------------

    fun observeReusableTimeTables(): Flow<List<ReusableTimeTableEntity>> =
        timeTableDao.observeTimeTables()

    fun observeTimeTables(): Flow<List<ReusableTimeTableEntity>> = observeReusableTimeTables()

    suspend fun getReusableTimeTables(): List<ReusableTimeTableEntity> = withContext(Dispatchers.IO) {
        timeTableDao.getAllTimeTables()
    }

    suspend fun getReusableTimeTable(timeTableId: Long): ReusableTimeTableEntity? =
        getTimeTableEntity(timeTableId)

    suspend fun getTimeTables(): List<ScheduleTimeTable> = withContext(Dispatchers.IO) {
        timeTableDao.getAllTimeTables().mapNotNull { entity ->
            loadScheduleTimeTable(entity.id)
        }
    }

    suspend fun getTimeTableEntity(timeTableId: Long): ReusableTimeTableEntity? =
        withContext(Dispatchers.IO) { timeTableDao.getTimeTable(timeTableId) }

    suspend fun getTimeTableNodes(timeTableId: Long): List<TimeTableNode> = withContext(Dispatchers.IO) {
        timeTableDao.getNodes(timeTableId)
            .sortedWith(compareBy<TimeTableNodeEntity> { it.node }.thenBy { it.id })
            .map { node -> TimeTableNode.fromStrings(node.node, node.start, node.end) }
    }

    suspend fun loadTimeTable(timeTableId: Long): ScheduleTimeTable? = withContext(Dispatchers.IO) {
        loadScheduleTimeTable(timeTableId)
    }

    suspend fun getTimeTable(timeTableId: Long): ScheduleTimeTable? = loadTimeTable(timeTableId)

    suspend fun saveTimeTable(timeTable: ScheduleTimeTable): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val nodes = timeTable.nodes.map { node ->
                NodeTimeEntity(
                    tableId = 0L,
                    node = node.node,
                    start = MinuteOfDay.format(node.startMinuteOfDay),
                    end = MinuteOfDay.format(node.endMinuteOfDay),
                )
            }
            val requestedId = timeTable.id.toLongOrNull()?.takeIf { it > 0L } ?: 0L
            if (requestedId > 0L) {
                validateTimeTableReferences(requestedId, timeTable.copy(id = requestedId.toString()))
            }
            persistCanonicalTimeTable(
                requestedId = requestedId,
                name = timeTable.name,
                sortOrder = requestedId.takeIf { it > 0L }?.let {
                    timeTableDao.getTimeTable(it)?.sortOrder
                } ?: 0,
                nodes = nodes,
            )
        }
    }

    suspend fun saveReusableTimeTable(timeTable: ScheduleTimeTable): Long = saveTimeTable(timeTable)

    suspend fun updateReusableTimeTable(timeTable: ScheduleTimeTable): Long = saveTimeTable(timeTable)

    suspend fun saveTimeTable(
        timeTable: ReusableTimeTableEntity,
        nodes: List<TimeTableNodeEntity>,
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            persistCanonicalTimeTable(
                requestedId = timeTable.id,
                name = timeTable.name,
                sortOrder = timeTable.sortOrder,
                nodes = nodes.map { node ->
                    NodeTimeEntity(tableId = 0L, node = node.node, start = node.start, end = node.end)
                },
            )
        }
    }

    suspend fun saveTimeTableNodes(timeTableId: Long, nodes: List<TimeTableNode>): Unit =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val entity = requireNotNull(timeTableDao.getTimeTable(timeTableId)) {
                    "时间表不存在: $timeTableId"
                }
                val candidate = ScheduleTimeTable(
                    id = timeTableId.toString(),
                    name = entity.name,
                    nodes = nodes,
                )
                validateTimeTableReferences(timeTableId, candidate)
                persistCanonicalTimeTable(
                    requestedId = timeTableId,
                    name = entity.name,
                    sortOrder = entity.sortOrder,
                    nodes = nodes.map { node ->
                        NodeTimeEntity(tableId = 0L, node = node.node, start = MinuteOfDay.format(node.startMinuteOfDay), end = MinuteOfDay.format(node.endMinuteOfDay))
                    },
                )
            }
        }

    private suspend fun validateTimeTableReferences(
        timeTableId: Long,
        candidate: ScheduleTimeTable,
    ) {
        val validation = TimeTableValidator.validate(candidate)
        require(validation.isValid) {
            validation.issues.joinToString(separator = "; ") { it.message }
        }
        dao.getTablesUsingTimeTable(timeTableId).forEach { table ->
            val current = requireNotNull(loadDomainTimetableOrNullInternal(table.id)) {
                "课表不存在: ${table.id}"
            }
            TimeTableValidator.requireValid(
                current.copy(timeTable = candidate),
                "课表 ${table.id} 引用时间表 $timeTableId",
            )
        }
    }

    suspend fun bindTimeTable(tableId: Long, timeTableId: Long): TableEntity = withContext(Dispatchers.IO) {
        db.withTransaction {
            val table = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
            val candidateTimeTable = requireNotNull(loadScheduleTimeTable(timeTableId)) {
                "时间表不存在: $timeTableId"
            }
            val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                "课表不存在: $tableId"
            }
            TimeTableValidator.requireValid(
                current.copy(timeTable = candidateTimeTable),
                "课表 $tableId 切换时间表",
            )
            val updated = table.copy(
                timeTableId = timeTableId,
                nodeCount = candidateTimeTable.nodes.size,
            )
            dao.upsertTable(updated)
            replaceLegacyProjection(
                tableId,
                candidateTimeTable.nodes.map { node ->
                    NodeTimeEntity(
                        tableId = tableId,
                        node = node.node,
                        start = MinuteOfDay.format(node.startMinuteOfDay),
                        end = MinuteOfDay.format(node.endMinuteOfDay),
                    )
                },
            )
            updated
        }
    }

    suspend fun reorderTimeTables(orderedTimeTableIds: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = timeTableDao.getAllTimeTables()
            require(existing.map { it.id }.toSet() == orderedTimeTableIds.toSet() &&
                existing.size == orderedTimeTableIds.size) {
                "排序列表必须包含每一张时间表且不能重复"
            }
            orderedTimeTableIds.forEachIndexed { index, id ->
                val entity = requireNotNull(timeTableDao.getTimeTable(id)) { "时间表不存在: $id" }
                timeTableDao.upsertTimeTable(entity.copy(sortOrder = index))
            }
        }
    }

    suspend fun deleteTimeTable(timeTableId: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val timeTable = requireNotNull(timeTableDao.getTimeTable(timeTableId)) {
                "时间表不存在: $timeTableId"
            }
            require(dao.getTablesUsingTimeTable(timeTable.id).isEmpty()) {
                "时间表 ${timeTable.id} 仍被课表引用，不能删除"
            }
            val nodes = timeTableDao.getNodes(timeTable.id)
            requireValidTimeTableNodes(nodes, "时间表 ${timeTable.id}")
            timeTableDao.deleteNodes(timeTable.id)
            timeTableDao.deleteTimeTable(timeTable)
        }
    }

    suspend fun deleteTimeTable(timeTable: ReusableTimeTableEntity) = deleteTimeTable(timeTable.id)

    suspend fun deleteTimeTable(timeTable: ScheduleTimeTable) {
        val id = timeTable.id.toLongOrNull()
            ?: throw IllegalArgumentException("时间表 id 必须是 Room 数字 id")
        deleteTimeTable(id)
    }

    suspend fun deleteReusableTimeTable(timeTableId: Long) = deleteTimeTable(timeTableId)

    // ---- Date exceptions, conflict priorities, and reminders ----------------------

    suspend fun getDateExceptions(tableId: Long): List<DateException> =
        withContext(Dispatchers.IO) { loadDomainTimetable(tableId).dateExceptions }

    suspend fun saveDateException(exception: DateException, tableId: Long) =
        saveDateException(tableId, exception)

    suspend fun saveDateException(tableId: Long, exception: DateException) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                "课表不存在: $tableId"
            }
            persistLoadedDomainTimetable(
                tableId,
                current.copy(
                    dateExceptions = current.dateExceptions.filterNot { it.id == exception.id } + exception,
                ),
            )
        }
    }

    suspend fun deleteDateException(tableId: Long, exception: DateException) =
        deleteDateException(tableId, exception.id)

    suspend fun deleteDateException(tableId: Long, exceptionId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            dateExceptionDao.getForTable(tableId)
                .firstOrNull { dateExceptionDomainId(tableId, it.id) == exceptionId }
                ?.let { dateExceptionDao.delete(it) }
        }
    }

    suspend fun getConflictPreferences(tableId: Long): List<ConflictPreference> =
        withContext(Dispatchers.IO) { loadDomainTimetable(tableId).conflictPreferences }

    suspend fun saveDateExceptions(tableId: Long, exceptions: List<DateException>) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                    "课表不存在: $tableId"
                }
                persistLoadedDomainTimetable(tableId, current.copy(dateExceptions = exceptions))
            }
        }

    suspend fun saveConflictPreferences(tableId: Long, preferences: List<ConflictPreference>) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                    "课表不存在: $tableId"
                }
                persistLoadedDomainTimetable(
                    tableId,
                    current.copy(conflictPreferences = preferences),
                )
            }
        }

    suspend fun deleteConflictPreference(tableId: Long, preference: ConflictPreference) =
        saveConflictPreferences(
            tableId,
            getConflictPreferences(tableId).filterNot { it.sameTarget(preference) },
        )

    suspend fun saveConflictPreference(
        tableId: Long,
        preference: ConflictPreference,
        conflictingOccurrenceIds: Set<String> = emptySet(),
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val current = requireNotNull(loadDomainTimetableOrNullInternal(tableId)) {
                "课表不存在: $tableId"
            }
            val updated = current.conflictPreferences.filterNot { existing ->
                existing.sameTarget(preference) ||
                    (existing.occurrenceId != null && existing.occurrenceId in conflictingOccurrenceIds)
            } + preference
            persistLoadedDomainTimetable(
                tableId,
                current.copy(conflictPreferences = updated),
            )
        }
    }

    suspend fun saveConflictPriority(tableId: Long, preference: ConflictPreference) =
        saveConflictPreference(tableId, preference)

    suspend fun getReminderSettings(tableId: Long): ReminderSettings =
        withContext(Dispatchers.IO) { loadDomainTimetable(tableId).reminderSettings }

    suspend fun saveReminderSettings(tableId: Long, settings: ReminderSettings) =
        withContext(Dispatchers.IO) {
            require(settings.startLeadMinutes >= 0 && settings.endLeadMinutes >= 0) {
                "提醒提前分钟数不能为负数"
            }
            db.withTransaction {
                requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
                reminderSettingsDao.upsert(
                    ReminderSettingsEntity(
                        tableId = tableId,
                        startEnabled = settings.startEnabled,
                        endEnabled = settings.endEnabled,
                        startLeadMinutes = settings.startLeadMinutes,
                        endLeadMinutes = settings.endLeadMinutes,
                        includeCourseName = settings.content.includeCourseName,
                        includeTeacher = settings.content.includeTeacher,
                        includeRoom = settings.content.includeRoom,
                        includeNote = settings.content.includeNote,
                        vibrate = settings.vibrate,
                        silent = settings.silent,
                    ),
                )
            }
        }

    // ---- Internal aggregate persistence -------------------------------------------

    private suspend fun loadDomainTimetableOrNullInternal(tableId: Long): Timetable? {
        val table = dao.getTable(tableId) ?: return null
        val courses = dao.getCourses(tableId)
        val times = courses.flatMap { course -> dao.getTimes(course.id) }
        val reusable = table.timeTableId.takeIf { it > 0L }?.let { timeTableDao.getTimeTable(it) }
        val canonicalNodes = reusable?.let { timeTableDao.getNodes(it.id) }.orEmpty()
        val legacyNodes = dao.getNodeTimes(tableId)
        val exceptions = dateExceptionDao.getForTable(tableId)
        val preferences = conflictPreferenceDao.getForTable(tableId)
        val reminder = reminderSettingsDao.get(tableId)
        return DomainMappers.toDomain(
            TimetableEntityRows(
                table = table,
                courses = courses,
                courseTimes = times,
                nodeTimes = legacyNodes,
                reusableTimeTable = reusable,
                timeTableNodes = canonicalNodes,
                dateExceptions = exceptions,
                conflictPreferences = preferences,
                reminderSettings = reminder,
            ),
        )
    }

    private suspend fun persistLoadedDomainTimetable(tableId: Long, timetable: Timetable) {
        TimeTableValidator.requireValid(timetable, "Timetable")
        val existingTable = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
        val existingCourses = dao.getCourses(tableId)
        val courseIds = timetable.courses.associate { course ->
            val id = course.id.toLongOrNull()
                ?: throw IllegalArgumentException("命令结果包含不能映射到 Room 的课程 id '${course.id}'")
            require(existingCourses.any { it.id == id }) {
                "命令结果引用了不属于课表 $tableId 的课程 id '${course.id}'"
            }
            course.id to id
        }
        val existingTimes = existingCourses.flatMap { course -> dao.getTimes(course.id) }
        val canonicalId = resolveExistingTimeTableId(existingTable, timetable.timeTable.id)
        val existingTimeTable = canonicalId.takeIf { it > 0L }?.let { timeTableDao.getTimeTable(it) }
        val canonicalRows = timetable.timeTable.nodes.map { node ->
            TimeTableNodeEntity(
                timeTableId = canonicalId,
                node = node.node,
                start = MinuteOfDay.format(node.startMinuteOfDay),
                end = MinuteOfDay.format(node.endMinuteOfDay),
            )
        }
        val actualCanonicalId = persistCanonicalTimeTable(
            requestedId = canonicalId,
            name = timetable.timeTable.name,
            sortOrder = existingTimeTable?.sortOrder ?: timetable.sortOrder,
            nodes = canonicalRows.map { row ->
                NodeTimeEntity(tableId = tableId, node = row.node, start = row.start, end = row.end)
            },
        )
        val rows = DomainMappers.toEntities(
            timetable = timetable,
            tableId = tableId,
            courseIdByDomainId = courseIds,
            timeTableId = actualCanonicalId,
            existingTable = existingTable,
            existingTimeTable = existingTimeTable,
            existingCourseTimes = existingTimes,
            existingNodeTimes = dao.getNodeTimes(tableId),
            existingTimeTableNodes = timeTableDao.getNodes(actualCanonicalId),
        )
        persistEntityRows(rows, tableId, existingCourses)
    }

    private suspend fun persistImportedDomainTimetable(
        tableId: Long,
        imported: Timetable,
        mode: BackupImportMode,
    ) {
        val existingTable = requireNotNull(dao.getTable(tableId)) { "课表不存在: $tableId" }
        val existingCourses = dao.getCourses(tableId)
        val replace = mode == BackupImportMode.REPLACE
        if (replace) {
            dao.deleteCourses(tableId)
            dao.deleteDateExceptions(tableId)
            dao.deleteConflictPreferences(tableId)
            dao.deleteReminderSettings(tableId)
        }

        val availableCourses = if (replace) emptyList() else existingCourses
        val courseIds = linkedMapOf<String, Long>()
        for (course in imported.courses) {
            val existing = availableCourses.firstOrNull { it.id.toString() == course.id }
            val courseId = existing?.id ?: dao.upsertCourse(
                CourseEntity(
                    tableId = tableId,
                    name = course.name,
                    color = course.color,
                    teacher = course.slots.firstOrNull()?.teacher.orEmpty(),
                    note = course.note,
                    credit = course.credit,
                ),
            )
            courseIds[course.id] = courseId
        }

        val currentCanonicalId = existingTable.timeTableId
        val currentCanonicalUsers = currentCanonicalId.takeIf { it > 0L }
            ?.let { dao.getTablesUsingTimeTable(it) }
            .orEmpty()
        val canReuseCurrentCanonical = currentCanonicalId > 0L &&
            (!replace || currentCanonicalUsers.all { it.id == tableId })
        val canonicalId = currentCanonicalId.takeIf { canReuseCurrentCanonical } ?: 0L
        val canonicalRows = imported.timeTable.nodes.map { node ->
            NodeTimeEntity(
                tableId = tableId,
                node = node.node,
                start = MinuteOfDay.format(node.startMinuteOfDay),
                end = MinuteOfDay.format(node.endMinuteOfDay),
            )
        }
        val canonicalSortOrder = canonicalId.takeIf { it > 0L }
            ?.let { timeTableDao.getTimeTable(it)?.sortOrder }
            ?: imported.sortOrder
        val actualCanonicalId = persistCanonicalTimeTable(
            requestedId = canonicalId,
            name = imported.timeTable.name,
            sortOrder = canonicalSortOrder,
            nodes = canonicalRows,
        )
        val existingTimes = if (replace) emptyList() else existingCourses.flatMap { course -> dao.getTimes(course.id) }
        val rows = DomainMappers.toEntities(
            timetable = imported,
            tableId = tableId,
            courseIdByDomainId = courseIds,
            timeTableId = actualCanonicalId,
            existingTable = existingTable,
            existingTimeTable = timeTableDao.getTimeTable(actualCanonicalId),
            existingCourseTimes = existingTimes,
            existingNodeTimes = dao.getNodeTimes(tableId),
            existingTimeTableNodes = timeTableDao.getNodes(actualCanonicalId),
        )
        persistEntityRows(rows, tableId, if (replace) emptyList() else existingCourses)
    }

    private suspend fun persistEntityRows(
        rows: TimetableEntityRows,
        tableId: Long,
        existingCourses: List<CourseEntity>,
    ) {
        val newCourseIds = rows.courses.mapTo(mutableSetOf()) { it.id }
        existingCourses.filterNot { it.id in newCourseIds }.forEach { dao.deleteCourse(it) }
        dao.upsertTable(rows.table.copy(id = tableId, timeTableId = rows.reusableTimeTable?.id ?: rows.table.timeTableId))
        rows.courses.forEach { course ->
            dao.upsertCourse(course)
            dao.deleteTimes(course.id)
            rows.courseTimes.filter { it.courseId == course.id }
                .takeIf { it.isNotEmpty() }
                ?.let { dao.upsertTimes(it) }
        }
        dao.deleteDateExceptions(tableId)
        rows.dateExceptions.takeIf { it.isNotEmpty() }?.let { values ->
            values.forEach { dateExceptionDao.upsert(it.copy(tableId = tableId)) }
        }
        dao.deleteConflictPreferences(tableId)
        rows.conflictPreferences.takeIf { it.isNotEmpty() }?.let { values ->
            values.forEach { conflictPreferenceDao.upsert(it.copy(tableId = tableId)) }
        }
        reminderSettingsDao.upsert(rows.reminderSettings ?: ReminderSettingsEntity(tableId = tableId))
        if (rows.nodeTimes.isNotEmpty()) replaceLegacyProjection(tableId, rows.nodeTimes)
    }

    private fun reconcileDateExceptions(
        candidate: Timetable,
        previous: List<DateException>,
    ): List<DateException> {
        val recurring = TimetableEngine.recurringOccurrences(candidate)
        return previous.mapNotNull { exception ->
            val sourcesOnDate = recurring.filter { occurrence ->
                occurrence.logicalSlotId == exception.logicalSlotId &&
                    occurrence.sourceEpochDay == exception.originalEpochDay
            }
            val source = sourcesOnDate.filter {
                exception.recurrenceSegmentId == null || it.recurrenceSegmentId == exception.recurrenceSegmentId
            }.singleOrNull() ?: sourcesOnDate.singleOrNull() ?: return@mapNotNull null
            val updated = exception.copy(
                recurrenceSegmentId = exception.recurrenceSegmentId?.let { source.recurrenceSegmentId },
            )
            updated.takeIf {
                TimeTableValidator.validate(candidate.copy(dateExceptions = listOf(updated))).isValid
            }
        }
    }

    private fun reconcileConflictPreferences(
        previous: Timetable,
        candidate: Timetable,
    ): List<ConflictPreference> {
        val previousOccurrences = TimetableEngine.expandOccurrences(previous)
        val candidateByOccurrenceKey = TimetableEngine.expandOccurrences(candidate)
            .groupBy(::occurrenceIdentityKey)
        return previous.conflictPreferences.mapNotNull { preference ->
            val mappedOccurrenceId = preference.occurrenceId?.let { occurrenceId ->
                val previousOccurrence = previousOccurrences.firstOrNull { it.id == occurrenceId }
                    ?: return@mapNotNull null
                candidateByOccurrenceKey[occurrenceIdentityKey(previousOccurrence)]
                    ?.singleOrNull()
                    ?.id
            }
            if (preference.occurrenceId != null && mappedOccurrenceId == null) {
                return@mapNotNull null
            }
            val updated = preference.copy(occurrenceId = mappedOccurrenceId)
            updated.takeIf {
                TimeTableValidator.validate(candidate.copy(conflictPreferences = listOf(updated))).isValid
            }
        }
    }

    private suspend fun resolveExistingTimeTableId(table: TableEntity, domainTimeTableId: String): Long {
        val requested = domainTimeTableId.toLongOrNull()?.takeIf { it > 0L }
        return when {
            requested != null && requested == table.timeTableId -> requested
            table.timeTableId > 0L -> {
                requireNotNull(timeTableDao.getTimeTable(table.timeTableId)) {
                    "课表 ${table.id} 的时间表不存在: ${table.timeTableId}"
                }
                table.timeTableId
            }
            else -> 0L
        }.also { id ->
            if (id > 0L) requireValidTimeTableNodes(timeTableDao.getNodes(id), "时间表 $id")
        }
    }

    private suspend fun insertTable(table: TableEntity): Long {
        val id = dao.upsertTable(table)
        return if (table.id == 0L) {
            require(id > 0L) { "无法生成课表 id" }
            id
        } else {
            table.id
        }
    }

    private suspend fun ensureTimeTableForTable(table: TableEntity): Long {
        if (table.timeTableId > 0L) {
            val existing = requireNotNull(timeTableDao.getTimeTable(table.timeTableId)) {
                "课表 ${table.id} 的时间表不存在: ${table.timeTableId}"
            }
            val canonicalNodes = timeTableDao.getNodes(table.timeTableId)
            if (canonicalNodes.isNotEmpty()) {
                requireValidTimeTableNodes(canonicalNodes, "时间表 ${table.timeTableId}")
                return table.timeTableId
            }
            val legacyNodes = dao.getNodeTimes(table.id)
            val repairNodes = if (legacyNodes.isNotEmpty()) {
                selectCanonicalNodes(table, legacyNodes)
            } else {
                defaultCanonicalNodeTimes(table.id, table.nodeCount)
            }
            persistCanonicalTimeTable(
                requestedId = table.timeTableId,
                name = existing.name,
                sortOrder = existing.sortOrder,
                nodes = repairNodes,
            )
            return table.timeTableId
        }
        val legacyNodes = dao.getNodeTimes(table.id)
        val nodes = if (legacyNodes.isNotEmpty()) {
            selectCanonicalNodes(table, legacyNodes)
        } else {
            defaultCanonicalNodeTimes(table.id, table.nodeCount)
        }
        val timeTableId = persistCanonicalTimeTable(
            requestedId = 0L,
            name = table.name,
            sortOrder = table.sortOrder,
            nodes = nodes,
        )
        dao.upsertTable(table.copy(timeTableId = timeTableId))
        return timeTableId
    }

    private suspend fun persistCanonicalTimeTable(
        requestedId: Long,
        name: String,
        sortOrder: Int,
        nodes: List<NodeTimeEntity>,
    ): Long {
        require(nodes.isNotEmpty()) { "时间表至少需要一节有效节次" }
        val normalized = nodes.sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id })
        requireValidLegacyTimeTableNodes(normalized, "时间表 $name")
        val linkedTables = if (requestedId > 0L) {
            dao.getTablesUsingTimeTable(requestedId)
        } else {
            emptyList()
        }
        val candidateTimeTable = ScheduleTimeTable(
            id = requestedId.takeIf { it > 0L }?.toString() ?: "pending",
            name = name,
            nodes = normalized.map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            },
        )
        linkedTables.forEach { linked ->
            val linkedTimetable = requireNotNull(loadDomainTimetableOrNullInternal(linked.id)) {
                "课表不存在: ${linked.id}"
            }
            TimeTableValidator.requireValid(
                linkedTimetable.copy(timeTable = candidateTimeTable),
                "时间表 $requestedId 对课表 ${linked.id} 的更新",
            )
        }

        val entity = ReusableTimeTableEntity(
            id = requestedId,
            name = name,
            sortOrder = sortOrder,
        )
        val generatedId = timeTableDao.upsertTimeTable(entity)
        val actualId = if (requestedId > 0L) requestedId else generatedId
        require(actualId > 0L) { "无法生成时间表 id" }
        val existingNodes = timeTableDao.getNodes(actualId)
        timeTableDao.deleteNodes(actualId)
        timeTableDao.upsertNodes(
            normalized.map { node ->
                val existing = existingNodes.firstOrNull { it.node == node.node }
                TimeTableNodeEntity(
                    id = existing?.id ?: 0L,
                    timeTableId = actualId,
                    node = node.node,
                    start = node.start,
                    end = node.end,
                )
            },
        )
        val actualLinkedTables = if (requestedId > 0L) {
            linkedTables
        } else {
            dao.getTablesUsingTimeTable(actualId)
        }
        for (linked in actualLinkedTables) {
            replaceLegacyProjection(
                linked.id,
                normalized.map { node ->
                    NodeTimeEntity(
                        tableId = linked.id,
                        node = node.node,
                        start = node.start,
                        end = node.end,
                    )
                },
            )
            if (linked.nodeCount > normalized.size) {
                dao.upsertTable(linked.copy(nodeCount = normalized.size))
            }
        }
        return actualId
    }

    private suspend fun replaceLegacyProjection(tableId: Long, nodes: List<NodeTimeEntity>) {
        require(nodes.isNotEmpty()) { "时间表至少需要一节有效节次" }
        val existing = dao.getNodeTimes(tableId)
        dao.deleteNodeTimes(tableId)
        dao.upsertNodeTimes(
            nodes.sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id }).map { node ->
                node.copy(
                    id = existing.firstOrNull { it.node == node.node }?.id ?: node.id,
                    tableId = tableId,
                )
            },
        )
    }

    private fun selectCanonicalNodes(
        table: TableEntity,
        nodes: List<NodeTimeEntity>,
    ): List<NodeTimeEntity> {
        val sorted = nodes.sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id })
        val limit = table.nodeCount.coerceIn(1, MAX_NODE_COUNT)
        val considered = sorted.take(limit)
        if (isValidLegacyTimeTableNodes(considered)) return considered

        val validPrefix = considered.takeWhile(::isValidLegacyNode)
        require(validPrefix.isNotEmpty()) { "课表 ${table.id} 的时间表没有有效节次" }
        if (!considered.drop(validPrefix.size).all(::isLegacyPlaceholder)) {
            requireValidLegacyTimeTableNodes(considered, "课表 ${table.id} 的时间表")
            throw IllegalArgumentException("课表 ${table.id} 的时间表包含无效节次")
        }
        requireValidLegacyTimeTableNodes(validPrefix, "课表 ${table.id} 的时间表")
        return validPrefix
    }

    private fun isValidLegacyNode(node: NodeTimeEntity): Boolean {
        val start = MinuteOfDay.parse(node.start) ?: return false
        val end = MinuteOfDay.parse(node.end) ?: return false
        return start < end
    }

    private fun isLegacyPlaceholder(node: NodeTimeEntity): Boolean =
        (node.start.isBlank() && node.end.isBlank()) ||
            (node.start.trim() == "00:00" && node.end.trim() == "00:00")

    private fun requireValidTimeTableNodes(nodes: List<TimeTableNodeEntity>, context: String) {
        val result = TimeTableValidator.validate(
            nodes.sortedWith(compareBy<TimeTableNodeEntity> { it.node }.thenBy { it.id }).map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            },
        )
        if (!result.isValid) throw TimetableValidationException(context, result.issues)
    }

    private fun requireValidLegacyTimeTableNodes(nodes: List<NodeTimeEntity>, context: String) {
        val result = TimeTableValidator.validate(
            nodes.sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id }).map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            },
        )
        if (!result.isValid) throw TimetableValidationException(context, result.issues)
    }

    private fun isValidLegacyTimeTableNodes(nodes: List<NodeTimeEntity>): Boolean =
        nodes.isNotEmpty() && TimeTableValidator.validate(
            nodes.sortedWith(compareBy<NodeTimeEntity> { it.node }.thenBy { it.id }).map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            },
        ).isValid

    private fun parseValidRange(start: String, end: String): Pair<String, String>? {
        val startMinute = MinuteOfDay.parse(start) ?: return null
        val endMinute = MinuteOfDay.parse(end) ?: return null
        if (startMinute >= endMinute) return null
        return start to end
    }

    private fun addMinutes(start: String, minutes: Int): String {
        val startMinute = MinuteOfDay.parse(start) ?: return "00:00"
        val endMinute = (startMinute + minutes).coerceAtMost(24 * 60)
        require(startMinute < endMinute) { "新增节次没有可用的时间范围" }
        return MinuteOfDay.format(endMinute)
    }

    private fun defaultCanonicalNodeTimes(tableId: Long, requestedCount: Int): List<NodeTimeEntity> {
        val count = requestedCount.coerceIn(1, MAX_NODE_COUNT)
        val validDefaults = defaultNodeTimes(tableId).filter { node ->
            val start = MinuteOfDay.parse(node.start)
            val end = MinuteOfDay.parse(node.end)
            start != null && end != null && start < end
        }
        val selected = validDefaults.take(count)
        require(selected.isNotEmpty()) { "默认时间表没有有效节次" }
        return selected
    }

    private fun normalizeCourseTimes(
        courseId: Long,
        course: CourseEntity,
        times: List<CourseTimeEntity>,
    ): List<CourseTimeEntity> {
        val slotIds = linkedMapOf<String, String>()
        val usedSegmentIds = mutableSetOf<String>()
        return times.map { time ->
            val slotCandidate = usableLogicalSlotId(courseId, time)
            val logicalSlotId = slotIds.getOrPut(slotCandidate) { slotCandidate }
            val rawSegmentId = usableId(time.recurrenceSegmentId)
                ?: stableId(
                    "recurrence-segment",
                    courseId.toString(),
                    logicalSlotId,
                    time.day.toString(),
                    time.startNode.toString(),
                    time.step.toString(),
                    time.startWeek.toString(),
                    time.endWeek.toString(),
                    time.weekType.toString(),
                    time.teacher,
                    time.room,
                    time.ownTime.toString(),
                    time.startTime,
                    time.endTime,
                )
            val segmentId = uniqueId(rawSegmentId, usedSegmentIds)
            usedSegmentIds += segmentId
            time.copy(
                courseId = courseId,
                logicalSlotId = logicalSlotId,
                recurrenceSegmentId = segmentId,
                teacher = if (usableId(time.logicalSlotId) == null && usableId(time.recurrenceSegmentId) == null) {
                    time.teacher.ifBlank { course.teacher }
                } else {
                    time.teacher
                },
            )
        }
    }

    private fun usableLogicalSlotId(courseId: Long, time: CourseTimeEntity): String {
        val stored = usableId(time.logicalSlotId)
        val legacyParts = stored?.takeIf { it.startsWith("legacy|") }?.split('|')
        if (legacyParts != null && legacyParts.size >= 5 &&
            legacyParts[1].toLongOrNull() != null &&
            legacyParts[2].toIntOrNull() == time.day &&
            legacyParts[3].toIntOrNull() == time.startNode &&
            legacyParts[4].toIntOrNull() == time.step
        ) {
            return stableId(
                "legacy-slot",
                legacyParts[1],
                legacyParts[2],
                legacyParts[3],
                legacyParts[4],
            )
        }
        return stored ?: stableId(
            "logical-slot",
            courseId.toString(),
            time.day.toString(),
            time.startNode.toString(),
            time.step.toString(),
        )
    }

    private fun uniqueId(candidate: String, used: Set<String>): String {
        if (candidate !in used) return candidate
        var suffix = 2
        var generated: String
        do {
            generated = "$candidate~$suffix"
            suffix++
        } while (generated in used)
        return generated
    }

    private fun usableId(value: String): String? = value.trim()
        .takeIf { it.isNotEmpty() && it != "legacy" }

    private fun stableId(prefix: String, vararg parts: String): String = buildString {
        append(prefix)
        parts.forEach { part ->
            append('|')
            append(part.length)
            append(':')
            append(part)
        }
    }

    private suspend fun loadScheduleTimeTable(timeTableId: Long): ScheduleTimeTable? {
        val entity = timeTableDao.getTimeTable(timeTableId) ?: return null
        val nodes = timeTableDao.getNodes(timeTableId)
            .sortedWith(compareBy<TimeTableNodeEntity> { it.node }.thenBy { it.id })
            .map { node ->
                TimeTableNode.fromStrings(node.node, node.start, node.end)
            }
        return ScheduleTimeTable(
            id = entity.id.toString(),
            name = entity.name,
            nodes = nodes,
        )
    }

    private fun decodeBackup(text: String): Timetable {
        val sharedError = runCatching { BackupFormat.decode(text) }.exceptionOrNull()
        if (sharedError == null) return BackupFormat.decode(text)
        return runCatching {
            val legacy = BackupCodec.decode(text)
            val mapped = DomainMappers.toDomain(
                TimetableEntityRows(
                    table = legacy.table,
                    courses = legacy.courses,
                    courseTimes = legacy.times,
                    nodeTimes = legacy.nodeTimes,
                ),
            )
            val nodeLimit = legacy.table.nodeCount.coerceIn(1, MAX_NODE_COUNT)
            val validNodes = mapped.timeTable.nodes
                .take(nodeLimit)
                .takeWhile { node ->
                    val range = node.minuteRange
                    range.isValid
                }
            require(validNodes.isNotEmpty()) { "旧版备份不包含有效的时间表节次" }
            mapped.copy(timeTable = mapped.timeTable.copy(nodes = validNodes))
        }.getOrElse { legacyError ->
            legacyError.addSuppressed(sharedError)
            throw legacyError
        }
    }

    private fun toDateExceptionEntity(
        exception: DateException,
        tableId: Long,
    ): DateExceptionEntity {
        val custom = exception.targetCustomTime
        return DateExceptionEntity(
            id = dateExceptionStorageId(tableId, exception.id),
            tableId = tableId,
            logicalSlotId = exception.logicalSlotId,
            originalEpochDay = exception.originalEpochDay,
            type = exception.type.name,
            recurrenceSegmentId = exception.recurrenceSegmentId,
            targetEpochDay = exception.targetEpochDay,
            targetDayOfWeek = exception.targetDayOfWeek,
            targetStartNode = exception.targetStartNode,
            targetNodeCount = exception.targetNodeCount,
            targetStartTime = custom?.let { MinuteOfDay.format(it.startMinuteOfDay) },
            targetEndTime = custom?.let { MinuteOfDay.format(it.endMinuteOfDay) },
            targetTeacher = exception.targetTeacher,
            targetRoom = exception.targetRoom,
        )
    }

    private fun occurrenceIdMap(
        source: Timetable?,
        copied: Timetable?,
    ): Map<String, String> {
        if (source == null || copied == null) return emptyMap()
        val copiedByKey = TimetableEngine.expandOccurrences(copied).groupBy(::occurrenceKey)
        return TimetableEngine.expandOccurrences(source).associate { occurrence ->
            val candidates = copiedByKey[occurrenceKey(occurrence)].orEmpty()
            require(candidates.size == 1) { "无法为复制的课程实例建立唯一映射" }
            occurrence.id to candidates.single().id
        }
    }

    private fun occurrenceIdentityKey(occurrence: com.letr.sleepdown.domain.CourseOccurrence): String = stableId(
        "course-occurrence",
        occurrence.courseId,
        occurrence.logicalSlotId,
        occurrence.recurrenceSegmentId,
        occurrence.sourceEpochDay.toString(),
        occurrence.exceptionId ?: "",
    )

    private fun occurrenceKey(occurrence: com.letr.sleepdown.domain.CourseOccurrence): String = stableId(
        "copied-occurrence",
        occurrence.logicalSlotId,
        occurrence.recurrenceSegmentId,
        occurrence.sourceEpochDay.toString(),
        occurrence.epochDay.toString(),
        occurrence.startMinuteOfDay.toString(),
        occurrence.endMinuteOfDay.toString(),
        occurrence.isRescheduled.toString(),
    )

    private fun ConflictPreference.sameTarget(other: ConflictPreference): Boolean =
        courseId == other.courseId &&
            logicalSlotId == other.logicalSlotId &&
            occurrenceId == other.occurrenceId &&
            epochDay == other.epochDay

    companion object {
        const val MAX_NODE_COUNT = 60
        private const val DEFAULT_NODE_DURATION_MINUTES = 50

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

        /** WakeUp-compatible default rows; the UI may keep unused trailing rows. */
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
            return slots.mapIndexed { index, (start, end) ->
                NodeTimeEntity(tableId = tableId, node = index + 1, start = start, end = end)
            }
        }
    }
}
