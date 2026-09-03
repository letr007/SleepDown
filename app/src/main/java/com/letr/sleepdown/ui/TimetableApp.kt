package com.letr.sleepdown.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.letr.sleepdown.R
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssistantPhoto
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.letr.sleepdown.AppContainer
import com.letr.sleepdown.data.CourseEntity
import com.letr.sleepdown.data.CourseItem
import com.letr.sleepdown.data.CourseTimeEntity
import com.letr.sleepdown.data.NodeTimeEntity
import com.letr.sleepdown.data.TableEntity
import com.letr.sleepdown.data.TableWithMeta
import com.letr.sleepdown.data.TimetableRepository
import com.letr.sleepdown.logic.CourseColors
import com.letr.sleepdown.logic.Weeks
import com.letr.sleepdown.widget.refreshScheduleWidgets
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val WakeUpEditorBackground = Color(0xFFFAF8FF)
private val WakeUpEditorText = Color(0xFF4A4A4A)
private val WakeUpEditorHint = Color(0xFFA3A3A3)
private val WakeUpEditorDivider = Color(0xFFE0DFE8)
private val WakeUpTeal = Color(0xFF16AEA0)
private val WakeUpOrange = Color(0xFFFFA622)
private val WakeUpBlue = Color(0xFF1E88E5)
private val WakeUpRed = Color(0xFFF63D3E)
private val WakeUpYellow = Color(0xFFFDD835)

@Composable
fun TimetableTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFFFF2D55),
            onPrimary = Color.White,
            background = Color(0xFFF7F7F7),
            surface = Color.White,
            onSurface = Color(0xFF141414),
            onSurfaceVariant = Color(0xFF626466),
        ),
        content = content,
    )
}

private enum class AppScreen {
    WEEK,
    TABLE_MANAGE,
    TABLE_SETTINGS,
    APPEARANCE,
    COURSE_MANAGE,
    COURSE_EDITOR,
    TIME_TABLE,
    IMPORT,
    WIDGET_HELP,
}

private data class TimeDraft(
    val id: Long = 0L,
    val day: Int = 1,
    val startNode: Int = 1,
    val step: Int = 1,
    val selectedWeeks: Set<Int> = emptySet(),
    val weekType: Int = CourseTimeEntity.TYPE_ALL,
    val room: String = "",
    val ownTime: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
)

private data class NodeDraft(
    val id: Long,
    val node: Int,
    val start: String,
    val end: String,
)

private data class CellSelection(
    val day: Int,
    val startNode: Int,
    val endNode: Int,
)

private enum class CellGestureResult {
    TAP,
    SCROLL,
    LONG_PRESS,
}

private enum class PickerTarget {
    DAY,
    START_NODE,
    STEP,
}

private data class TimePickerTarget(
    val index: Int,
    val isStart: Boolean,
)

private data class ImportNotice(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
)

@Composable
fun TimetableApp(
    repository: TimetableRepository,
    initialTableId: Long? = null,
    initialImportUri: Uri? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var externalImportUri by remember { mutableStateOf(initialImportUri) }
    val tables by repository.observeTables().collectAsStateWithLifecycle(initialValue = emptyList())
    var currentTableId by remember { mutableStateOf(AppContainer.currentTableId(context)) }
    var screen by remember { mutableStateOf(AppScreen.WEEK) }
    var editingTableId by remember { mutableStateOf<Long?>(null) }
    var tableDraft by remember { mutableStateOf<TableEntity?>(null) }
    var editingCourseId by remember { mutableStateOf<Long?>(null) }
    var editorDay by remember { mutableIntStateOf(1) }
    var editorStartNode by remember { mutableIntStateOf(1) }
    var editorStep by remember { mutableIntStateOf(2) }
    var selectLatestAfterImport by remember { mutableStateOf(false) }
    var importTableCountBefore by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        val id = repository.ensureDefaultTable()
        if (currentTableId <= 0L) {
            currentTableId = id
            AppContainer.setCurrentTableId(context, id)
        }
    }
    LaunchedEffect(initialTableId, tables) {
        val id = initialTableId
        if (id != null && tables.any { it.id == id } && currentTableId != id) {
            currentTableId = id
            AppContainer.setCurrentTableId(context, id)
            screen = AppScreen.WEEK
        }
    }
    LaunchedEffect(tables, currentTableId, selectLatestAfterImport) {
        when {
            selectLatestAfterImport && tables.size > importTableCountBefore -> {
                val id = tables.maxByOrNull { it.id }!!.id
                currentTableId = id
                AppContainer.setCurrentTableId(context, id)
                selectLatestAfterImport = false
            }
            tables.isNotEmpty() && tables.none { it.id == currentTableId } -> {
                val id = tables.first().id
                currentTableId = id
                AppContainer.setCurrentTableId(context, id)
            }
        }
    }

    val tableId = currentTableId.takeIf { it > 0L } ?: tables.firstOrNull()?.id ?: 0L
    val table = tables.firstOrNull { it.id == tableId }
    val tableMeta by remember(repository, tableId) {
        repository.observeTableWithMeta(tableId)
    }.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(initialImportUri) {
        if (initialImportUri != null) {
            externalImportUri = initialImportUri
            importTableCountBefore = tables.size
            screen = AppScreen.IMPORT
        }
    }
    LaunchedEffect(tables, tableMeta) {
        if (tables.isNotEmpty() && tableMeta != null) {
            refreshScheduleWidgets(context)
        }
    }

    BackHandler(enabled = screen != AppScreen.WEEK) {
        screen = when (screen) {
            AppScreen.TABLE_MANAGE -> AppScreen.WEEK
            AppScreen.TABLE_SETTINGS,
            AppScreen.APPEARANCE,
            AppScreen.COURSE_MANAGE,
            AppScreen.COURSE_EDITOR,
            AppScreen.TIME_TABLE,
            AppScreen.IMPORT,
            AppScreen.WIDGET_HELP -> AppScreen.WEEK
            AppScreen.WEEK -> AppScreen.WEEK
        }
    }

    fun selectTable(id: Long) {
        currentTableId = id
        AppContainer.setCurrentTableId(context, id)
    }

    fun openAddCourse(day: Int = 1, startNode: Int = 1, step: Int = 2) {
        editingCourseId = null
        editorDay = day
        editorStartNode = startNode
        editorStep = step
        screen = AppScreen.COURSE_EDITOR
    }

    fun saveTable(tableToSave: TableEntity) {
        tableDraft = null
        scope.launch {
            val id = repository.saveTable(tableToSave)
            if (tableToSave.id == 0L) {
                repository.saveNodeTimes(TimetableRepository.defaultNodeTimes(id))
            }
            selectTable(id)
            showToast(context, "保存成功")
            screen = AppScreen.WEEK
        }
    }

    val updateTable: (TableEntity) -> Unit = { updated ->
        tableDraft = updated
        scope.launch { repository.saveTable(updated) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F7F7)) {
        when (screen) {
            AppScreen.WEEK -> {
                if (tableMeta == null || table == null) {
                    LoadingView()
                } else {
                    androidx.compose.runtime.key(tableId) {
                        ScheduleScreen(
                            meta = tableMeta!!,
                            tables = tables,
                            currentTableId = tableId,
                            onSelectTable = ::selectTable,
                            onAddCourse = ::openAddCourse,
                            onOpenManage = { screen = AppScreen.TABLE_MANAGE },
                            onOpenSettings = { tableDraft = null; editingTableId = tableId; screen = AppScreen.TABLE_SETTINGS },
                            onOpenAppearance = { editingTableId = tableId; tableDraft = null; screen = AppScreen.APPEARANCE },
                            onOpenCourseManage = { screen = AppScreen.COURSE_MANAGE },
                            onOpenTimeTable = { screen = AppScreen.TIME_TABLE },
                            onOpenImport = { importTableCountBefore = tables.size; screen = AppScreen.IMPORT },
                            onOpenWidgetHelp = { screen = AppScreen.WIDGET_HELP },
                            onShare = {
                                scope.launch {
                                    repository.exportBackupResult(tableId).onSuccess { backup ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_TEXT, backup)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "分享课表备份"))
                                    }
                                    .onFailure { error -> showToast(context, error.message ?: "导出失败") }
                                }
                            },
                            onEditCourse = { course ->
                                editingCourseId = course.id
                                editorDay = 1
                                editorStartNode = 1
                                editorStep = 1
                                screen = AppScreen.COURSE_EDITOR
                            },
                            onDeleteCourse = { course ->
                                scope.launch {
                                    repository.deleteCourse(course)
                                    showToast(context, "删除成功")
                                }
                            },
                            onCopyCourse = { course ->
                                scope.launch {
                                    val copy = course.copy(id = 0L, name = "${course.name}（副本）")
                                    val times = repository.getCourseTimes(course.id).map { it.copy(id = 0L, courseId = 0L) }
                                    repository.saveCourse(table.id, copy, times)
                                    showToast(context, "课程已复制")
                                }
                            },
                            onMoveCourse = { item, day, startNode ->
                                scope.launch {
                                    val times = repository.getCourseTimes(item.course.id)
                                    val maxStart = (table.nodeCount - item.time.step + 1).coerceAtLeast(1)
                                    val updated = times.map { time ->
                                        if (time.id == item.time.id) {
                                            time.copy(
                                                day = day,
                                                startNode = startNode.coerceIn(1, maxStart),
                                            )
                                        } else {
                                            time
                                        }
                                    }
                                    repository.saveCourse(table.id, item.course, updated)
                                    showToast(context, "课程已移动")
                                }
                            },
                        )
                    }
                }
            }
            AppScreen.TABLE_MANAGE -> TableManagementScreen(
                tables = tables,
                currentTableId = tableId,
                onBack = { tableDraft = null; screen = AppScreen.WEEK },
                onSelect = { tableDraft = null; selectTable(it); screen = AppScreen.WEEK },
                onEditSettings = { tableDraft = null; editingTableId = it; screen = AppScreen.TABLE_SETTINGS },
                onAppearance = { tableDraft = null; editingTableId = it; selectTable(it); screen = AppScreen.APPEARANCE },
                onCourses = { tableDraft = null; selectTable(it); screen = AppScreen.COURSE_MANAGE },
                onCopy = { source ->
                    scope.launch {
                        selectTable(repository.copyTable(source.id))
                        showToast(context, "课表已复制")
                    }
                },
                onDelete = { tableToDelete ->
                    scope.launch {
                        val deletingCurrent = tableToDelete.id == currentTableId
                        repository.deleteTable(tableToDelete)
                        if (deletingCurrent) {
                            val replacement = tables.firstOrNull { it.id != tableToDelete.id }?.id ?: 0L
                            currentTableId = replacement
                            AppContainer.setCurrentTableId(context, replacement)
                        }
                        showToast(context, "删除成功")
                    }
                },
                onNew = { tableDraft = null; editingTableId = null; screen = AppScreen.TABLE_SETTINGS },
            )
            AppScreen.TABLE_SETTINGS -> {
                val editingTable = editingTableId?.let { id -> tables.firstOrNull { it.id == id } }
                TableSettingsScreen(
                    table = tableDraft ?: editingTable,
                    onBack = { screen = AppScreen.WEEK },
                    onSave = ::saveTable,
                    onUpdate = updateTable,
                    currentWeek = (tableDraft ?: table)?.let { currentWeek(it) } ?: 1,
                    onAppearance = { if (editingTable != null) { selectTable(editingTable.id); screen = AppScreen.APPEARANCE } },
                    onCourses = { if (editingTable != null) { selectTable(editingTable.id); screen = AppScreen.COURSE_MANAGE } },
                    onTimeTable = { if (editingTable != null) { selectTable(editingTable.id); screen = AppScreen.TIME_TABLE } },
                    onWidgetHelp = { screen = AppScreen.WIDGET_HELP },
                )
            }
            AppScreen.APPEARANCE -> {
                if (table == null || tableMeta == null) {
                    LoadingView()
                } else {
                    AppearanceScreen(
                        table = tableDraft ?: table,
                        meta = tableMeta!!,
                        onBack = { screen = AppScreen.WEEK },
                        onSave = updateTable,
                    )
                }
            }
            AppScreen.COURSE_MANAGE -> {
                if (table == null || tableMeta == null) {
                    LoadingView()
                } else {
                    CourseManagementScreen(
                        table = tableDraft ?: table,
                        courses = tableMeta!!.courses,
                        onBack = { screen = AppScreen.WEEK },
                        onAdd = { openAddCourse() },
                        onEdit = { course ->
                            editingCourseId = course.id
                            editorDay = 1
                            editorStartNode = 1
                            editorStep = 1
                            screen = AppScreen.COURSE_EDITOR
                        },
                        onDelete = { course ->
                            scope.launch {
                                repository.deleteCourse(course)
                                showToast(context, "删除成功")
                            }
                        },
                        onClear = { courses ->
                            scope.launch {
                                courses.forEach { repository.deleteCourse(it) }
                                showToast(context, "课程已清空")
                            }
                        },
                    )
                }
            }
            AppScreen.COURSE_EDITOR -> {
                if (table == null) LoadingView() else CourseEditorScreen(
                    repository = repository,
                    table = tableDraft ?: table,
                    courseId = editingCourseId,
                    initialDay = editorDay,
                    initialStartNode = editorStartNode,
                    initialStep = editorStep,
                    existingCourses = tableMeta?.courses.orEmpty(),
                    nodeTimes = tableMeta?.nodeTimes.orEmpty(),
                    onBack = { screen = AppScreen.WEEK },
                    onSaved = { screen = AppScreen.WEEK },
                )
            }
            AppScreen.TIME_TABLE -> {
                if (table == null) LoadingView() else TimeSettingsScreen(
                    repository = repository,
                    table = tableDraft ?: table,
                    onBack = { screen = AppScreen.WEEK },
                    onCopy = {
                        scope.launch {
                            val newId = repository.copyTable(table.id)
                            selectTable(newId)
                            screen = AppScreen.WEEK
                        }
                    },
                )
            }
            AppScreen.IMPORT -> ImportScreen(
                repository = repository,
                initialUri = externalImportUri,
                onInitialUriConsumed = { externalImportUri = null },
                onBack = { screen = AppScreen.WEEK },
                onJsonImported = { id -> selectTable(id); screen = AppScreen.WEEK },
                onCsvImported = { selectLatestAfterImport = true; screen = AppScreen.WEEK },
            )
            AppScreen.WIDGET_HELP -> WidgetHelpScreen(onBack = { screen = AppScreen.WEEK })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleScreen(
    meta: TableWithMeta,
    tables: List<TableEntity>,
    currentTableId: Long,
    onSelectTable: (Long) -> Unit,
    onAddCourse: (Int, Int, Int) -> Unit,
    onOpenManage: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenCourseManage: () -> Unit,
    onOpenTimeTable: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenWidgetHelp: () -> Unit,
    onShare: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onDeleteCourse: (CourseEntity) -> Unit,
    onCopyCourse: (CourseEntity) -> Unit,
    onMoveCourse: (CourseItem, Int, Int) -> Unit,
) {
    val table = meta.table
    val current = currentWeek(table)
    val initialPage = (current - 1).coerceIn(0, (table.maxWeek - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { table.maxWeek.coerceAtLeast(1) })
    val pagerScope = rememberCoroutineScope()
    var weekDialog by remember { mutableStateOf(false) }
    var tableDialog by remember { mutableStateOf(false) }
    var shareMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var selectedCourse by remember(table.id) { mutableStateOf<CourseEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScheduleBackground(table)
        Column(modifier = Modifier.fillMaxSize()) {
            MainToolbar(
                table = table,
                week = pagerState.currentPage + 1,
                currentWeek = current,
                onWeekClick = { weekDialog = true },
                onAdd = { onAddCourse(1, 1, 2) },
                onImport = onOpenImport,
                onShare = { shareMenu = true },
                onMore = { moreMenu = true },
                shareMenuExpanded = shareMenu,
                moreMenuExpanded = moreMenu,
                onDismissShareMenu = { shareMenu = false },
                onDismissMoreMenu = { moreMenu = false },
                onShareBackup = { shareMenu = false; onShare() },
                onSwitchTable = { moreMenu = false; tableDialog = true },
                onManage = { moreMenu = false; onOpenManage() },
                onSettings = { moreMenu = false; onOpenSettings() },
                onAppearance = { moreMenu = false; onOpenAppearance() },
                onCourseManage = { moreMenu = false; onOpenCourseManage() },
                onTimeTable = { moreMenu = false; onOpenTimeTable() },
                onWidgetHelp = { moreMenu = false; onOpenWidgetHelp() },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                SchedulePage(
                    table = table,
                    meta = meta,
                    week = page + 1,
                    currentWeek = current,
                    onAddCourse = onAddCourse,
                    onMoveCourse = onMoveCourse,
                    onOpenCourse = { selectedCourse = it },
                )
            }
        }
    }

    if (weekDialog) {
        WeekPickerDialog(
            maxWeek = table.maxWeek,
            selectedWeek = pagerState.currentPage + 1,
            onSelect = { page ->
                weekDialog = false
                pagerScope.launch { pagerState.animateScrollToPage(page - 1) }
            },
            onCurrent = {
                weekDialog = false
                pagerScope.launch { pagerState.animateScrollToPage(initialPage) }
            },
            onDismiss = { weekDialog = false },
        )
    }
    if (tableDialog) {
        TablePickerDialog(
            tables = tables,
            selectedId = table.id,
            onSelect = { onSelectTable(it); tableDialog = false },
            onDismiss = { tableDialog = false },
        )
    }
    selectedCourse?.let { course ->
        CourseActionSheet(
            course = course,
            items = meta.items.filter { it.course.id == course.id },
            nodeTimes = meta.nodeTimes,
            table = table,
            onEdit = { selectedCourse = null; onEditCourse(course) },
            onDelete = { selectedCourse = null; onDeleteCourse(course) },
            onCopy = { selectedCourse = null; onCopyCourse(course) },
            onDismiss = { selectedCourse = null },
        )
    }
}

@Composable
private fun ScheduleBackground(table: TableEntity) {
    val backgroundValue = table.bgImageUri.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (backgroundValue.startsWith("#")) {
                    Brush.verticalGradient(listOf(parseColor(backgroundValue, Color(0xFFE8E8F4)), parseColor(backgroundValue, Color(0xFFBECEE5))))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFE8E8F4), Color(0xFFBECEE5)))
                },
            ),
    ) {
        if (backgroundValue.isNotBlank() && !backgroundValue.startsWith("#")) {
            AsyncImage(
                model = backgroundValue,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun MainToolbar(
    table: TableEntity,
    week: Int,
    currentWeek: Int,
    onWeekClick: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    shareMenuExpanded: Boolean,
    moreMenuExpanded: Boolean,
    onDismissShareMenu: () -> Unit,
    onDismissMoreMenu: () -> Unit,
    onShareBackup: () -> Unit,
    onSwitchTable: () -> Unit,
    onManage: () -> Unit,
    onSettings: () -> Unit,
    onAppearance: () -> Unit,
    onCourseManage: () -> Unit,
    onTimeTable: () -> Unit,
    onWidgetHelp: () -> Unit,
) {
    val date = LocalDate.now()
    val dateText = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    val status = when {
        week < 1 -> "未开学"
        week > table.maxWeek -> "学期已结束"
        week == currentWeek -> "周${weekdayName(LocalDate.now().dayOfWeek.value)}"
        else -> "非本周"
    }
    val textColor = Color(table.textColor.toInt())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .heightIn(min = 46.dp)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onWeekClick)
                .padding(vertical = 3.dp),
        ) {
            Text(dateText, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("第${week}周", color = textColor.copy(alpha = 0.8f), fontSize = 13.sp)
                Text(status, color = textColor.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "添加课程", tint = textColor)
        }
        IconButton(onClick = onImport, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.FileDownload, contentDescription = "导入课表", tint = textColor)
        }
        Box {
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = "备份导出", tint = textColor)
            }
            DropdownMenu(expanded = shareMenuExpanded, onDismissRequest = onDismissShareMenu) {
                DropdownMenuItem(text = { Text("导出 JSON 备份") }, onClick = onShareBackup)
            }
        }
        Box {
            IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = textColor)
            }
            DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = onDismissMoreMenu) {
                DropdownMenuItem(text = { Text("切换课表") }, onClick = onSwitchTable)
                DropdownMenuItem(text = { Text("课表管理") }, onClick = onManage)
                DropdownMenuItem(text = { Text("课表设置") }, onClick = onSettings)
                DropdownMenuItem(text = { Text("课表外观") }, onClick = onAppearance)
                DropdownMenuItem(text = { Text("课程管理") }, onClick = onCourseManage)
                DropdownMenuItem(text = { Text("时间表") }, onClick = onTimeTable)
                DropdownMenuItem(text = { Text("桌面小部件") }, onClick = onWidgetHelp)
            }
        }
    }
}

@Composable
private fun SchedulePage(
    table: TableEntity,
    meta: TableWithMeta,
    week: Int,
    currentWeek: Int,
    onAddCourse: (Int, Int, Int) -> Unit,
    onMoveCourse: (CourseItem, Int, Int) -> Unit,
    onOpenCourse: (CourseEntity) -> Unit,
) {
    val visibleDays = visibleDays(table)
    val nodeTimes = nodeTimesFor(table, meta.nodeTimes)
    val bodyScroll = rememberScrollState()
    val headerTextColor = Color(table.textColor.toInt())
    val month = dateFor(table, week, visibleDays.firstOrNull() ?: 1).monthValue

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(end = if (visibleDays.size == 7) 4.dp else 8.dp)) {
            Box(modifier = Modifier.weight(0.64f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text("$month\n月", color = headerTextColor, fontSize = table.headerTextSize.coerceIn(8, 32).sp, fontWeight = FontWeight.Bold, lineHeight = (table.headerTextSize + 1).sp)
            }
            visibleDays.forEach { day ->
                val dayDate = dateFor(table, week, day)
                val today = week == currentWeek && dayDate == LocalDate.now()
                Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "${weekdayName(day)}\n${dayDate.dayOfMonth}",
                        color = headerTextColor.copy(alpha = if (today) 1f else 0.32f),
                        fontSize = table.headerTextSize.coerceIn(8, 32).sp,
                        fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = (table.headerTextSize + 1).sp,
                    )
                }
            }
        }
        ScheduleGrid(
            table = table,
            nodeTimes = nodeTimes,
            days = visibleDays,
            week = week,
            currentWeek = currentWeek,
            items = meta.items,
            scrollState = bodyScroll,
            onAddCourse = onAddCourse,
            onMoveCourse = onMoveCourse,
            onOpenCourse = onOpenCourse,
        )
    }
}

@Composable
private fun ScheduleGrid(
    table: TableEntity,
    nodeTimes: List<NodeTimeEntity>,
    days: List<Int>,
    week: Int,
    currentWeek: Int,
    items: List<CourseItem>,
    scrollState: androidx.compose.foundation.ScrollState,
    onAddCourse: (Int, Int, Int) -> Unit,
    onMoveCourse: (CourseItem, Int, Int) -> Unit,
    onOpenCourse: (CourseEntity) -> Unit,
) {
    val rowHeight = table.itemHeightDp.coerceIn(32, 128).dp + 2.dp
    val nodeCount = table.nodeCount.coerceIn(1, 60)
    val contentHeight = rowHeight * nodeCount.toFloat()
    val dayToColumn = days.withIndex().associate { it.value to it.index }
    val currentItems = items.filter {
        it.time.day in dayToColumn &&
            it.time.startNode in 1..nodeCount &&
            Weeks.inWeek(it.time.startWeek, it.time.endWeek, it.time.weekType, week)
    }
    val otherItems = if (table.showOtherWeekCourse) {
        items.filter {
            it.time.day in dayToColumn &&
                it.time.startNode in 1..nodeCount &&
                !Weeks.inWeek(it.time.startWeek, it.time.endWeek, it.time.weekType, week) &&
                it.time.startWeek <= table.maxWeek && it.time.endWeek >= 1
        }
    } else {
        emptyList()
    }
    val occupied = currentItems.flatMap { item ->
        (item.time.startNode until (item.time.startNode + item.time.step)).map { item.time.day to it }
    }.toSet()
    val textColor = Color(table.textColor.toInt())
    var selection by remember(week) { mutableStateOf<CellSelection?>(null) }
    var selectionAnchorNode by remember(week) { mutableStateOf<Int?>(null) }
    var selectionCursorNode by remember(week) { mutableStateOf<Int?>(null) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportHeightPx = it.height }
            .verticalScroll(scrollState),
    ) {
        Column(modifier = Modifier.weight(0.64f).height(contentHeight)) {
            nodeTimes.take(nodeCount).forEach { node ->
                Column(
                    modifier = Modifier.height(rowHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("${node.node}", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (table.showTimeBar && node.start.isNotBlank() && node.end.isNotBlank()) {
                        Text(node.start, color = textColor.copy(alpha = 0.56f), fontSize = 9.sp)
                        Text(node.end, color = textColor.copy(alpha = 0.56f), fontSize = 9.sp)
                    }
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(days.size.toFloat()).height(contentHeight).padding(end = if (days.size == 7) 4.dp else 8.dp)) {
            val columnWidth = if (days.isEmpty()) 0.dp else maxWidth / days.size
            Column(modifier = Modifier.height(contentHeight)) {
                repeat(nodeCount) { row ->
                    Row(modifier = Modifier.height(rowHeight)) {
                        days.forEach { day ->
                            val cellCourse = currentItems.firstOrNull {
                                it.time.day == day && row + 1 in it.time.startNode until (it.time.startNode + it.time.step)
                            }
                            val cellColor = cellCourse?.let { CourseColors.asColor(courseColor(it.course)) }
                            val node = row + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .then(gridModifier(table, cellColor))
                                    .pointerInput(day, node, rowHeightPx, cellCourse?.course?.id) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var totalDragX = 0f
                                            var totalDragY = 0f
                                            val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id }
                                                        ?: return@withTimeoutOrNull CellGestureResult.SCROLL
                                                    if (!change.pressed) {
                                                        return@withTimeoutOrNull CellGestureResult.TAP
                                                    }
                                                    totalDragX += change.position.x - change.previousPosition.x
                                                    totalDragY += change.position.y - change.previousPosition.y
                                                    if (abs(totalDragX) > viewConfiguration.touchSlop || abs(totalDragY) > viewConfiguration.touchSlop) {
                                                        return@withTimeoutOrNull CellGestureResult.SCROLL
                                                    }
                                                }
                                                CellGestureResult.LONG_PRESS
                                            }

                                            if (result == null) {
                                                if (cellCourse == null) {
                                                    selectionAnchorNode = node
                                                    selectionCursorNode = node
                                                    selection = CellSelection(day, node, node)
                                                }
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                    if (!change.pressed) break
                                                    totalDragX += change.position.x - change.previousPosition.x
                                                    totalDragY += change.position.y - change.previousPosition.y
                                                    if (cellCourse == null) {
                                                        change.consume()
                                                        val endNode = (node + (totalDragY / rowHeightPx).roundToInt())
                                                            .coerceIn(1, nodeCount)
                                                        selectionCursorNode = endNode
                                                        selection = CellSelection(
                                                            day = day,
                                                            startNode = minOf(node, endNode),
                                                            endNode = maxOf(node, endNode),
                                                        )
                                                    }
                                                }
                                            } else if (result == CellGestureResult.TAP && cellCourse == null) {
                                                val existing = selection
                                                if (existing != null && existing.day == day && node in existing.startNode..existing.endNode) {
                                                    selection = null
                                                    selectionAnchorNode = null
                                                    selectionCursorNode = null
                                                    onAddCourse(existing.day, existing.startNode, existing.endNode - existing.startNode + 1)
                                                } else {
                                                    selectionAnchorNode = node
                                                    selectionCursorNode = node
                                                    selection = CellSelection(day, node, node)
                                                }
                                            }
                                        }
                                    },
                            )
                        }
                    }
                }
            }
            selection?.let { selected ->
                val selectedColumn = dayToColumn[selected.day]
                if (selectedColumn != null) {
                    val selectionHeight = rowHeight * (selected.endNode - selected.startNode + 1).toFloat() - 2.dp
                    val selectionTop = rowHeight * (selected.startNode - 1).toFloat() + 1.dp
                    val handleOffsetY = selectionTop + ((selectionHeight - 72.dp).value / 2f).dp
                    val handleOffsetX = if (selectedColumn == days.lastIndex) {
                        columnWidth * selectedColumn.toFloat() - 32.dp
                    } else {
                        columnWidth * (selectedColumn + 1).toFloat() - 4.dp
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = columnWidth * selectedColumn.toFloat() + 1.dp, y = selectionTop)
                            .width((columnWidth - 2.dp).coerceAtLeast(1.dp))
                            .height(selectionHeight)
                            .background(Color(0xFFFF6272).copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                selection = null
                                selectionAnchorNode = null
                                selectionCursorNode = null
                                onAddCourse(selected.day, selected.startNode, selected.endNode - selected.startNode + 1)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加课程", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    SelectionResizeHandle(
                        modifier = Modifier.offset(x = handleOffsetX, y = handleOffsetY),
                        rowHeightPx = rowHeightPx,
                        onDragRows = { delta ->
                            val anchor = selectionAnchorNode ?: selected.startNode
                            val current = selectionCursorNode ?: selected.endNode
                            val next = (current + delta).coerceIn(1, nodeCount)
                            if (next != current) {
                                selectionAnchorNode = anchor
                                selectionCursorNode = next
                                selection = CellSelection(
                                    day = selected.day,
                                    startNode = minOf(anchor, next),
                                    endNode = maxOf(anchor, next),
                                )
                            }
                        },
                    )
                }
            }
            currentItems.forEach { item ->
                val column = dayToColumn[item.time.day] ?: return@forEach
                CourseCard(
                    item = item,
                    table = table,
                    columnWidth = columnWidth,
                    column = column,
                    currentWeek = week,
                    nodeTimes = nodeTimes,
                    rowHeight = rowHeight,
                    visibleDays = days,
                    scrollState = scrollState,
                    viewportHeightPx = viewportHeightPx,
                    onClick = { onOpenCourse(item.course) },
                    onMove = { day, startNode -> onMoveCourse(item, day, startNode) },
                )
            }
            otherItems.filter { item ->
                (item.time.startNode until (item.time.startNode + item.time.step))
                    .none { item.time.day to it in occupied }
            }.forEach { item ->
                val column = dayToColumn[item.time.day] ?: return@forEach
                CourseCard(
                    item = item,
                    table = table,
                    columnWidth = columnWidth,
                    column = column,
                    currentWeek = week,
                    nodeTimes = nodeTimes,
                    rowHeight = rowHeight,
                    visibleDays = days,
                    scrollState = scrollState,
                    viewportHeightPx = viewportHeightPx,
                    onClick = { onOpenCourse(item.course) },
                    onMove = null,
                )
            }
        }
    }
}

@Composable
private fun SelectionResizeHandle(
    modifier: Modifier,
    rowHeightPx: Float,
    onDragRows: (Int) -> Unit,
) {
    Image(
        painter = painterResource(R.drawable.sd_add_course_guid_icon),
        contentDescription = "调整课程节数",
        modifier = modifier
            .size(width = 36.dp, height = 72.dp)
            .zIndex(10f)
            .pointerInput(rowHeightPx) {
                var dragPixels = 0f
                detectDragGestures(
                    onDragStart = { dragPixels = 0f },
                    onDragEnd = { dragPixels = 0f },
                    onDragCancel = { dragPixels = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragPixels += amount.y
                        val delta = (dragPixels / rowHeightPx).roundToInt()
                        if (delta != 0) {
                            onDragRows(delta)
                            dragPixels -= delta * rowHeightPx
                        }
                    },
                )
            },
    )
}

@Composable
private fun CourseCard(
    item: CourseItem,
    table: TableEntity,
    columnWidth: Dp,
    column: Int,
    currentWeek: Int,
    nodeTimes: List<NodeTimeEntity>,
    rowHeight: Dp,
    visibleDays: List<Int>,
    scrollState: androidx.compose.foundation.ScrollState,
    viewportHeightPx: Int,
    onClick: () -> Unit,
    onMove: ((Int, Int) -> Unit)?,
) {
    val time = item.time
    val maxStep = (table.nodeCount - time.startNode + 1).coerceAtLeast(1)
    val step = time.step.coerceIn(1, maxStep)
    val current = Weeks.inWeek(time.startWeek, time.endWeek, time.weekType, currentWeek)
    val baseColor = CourseColors.asColor(courseColor(item.course))
    val configuredTextColor = Color(table.courseTextColor)
    val textColor = if (table.textColorCompose) blend(configuredTextColor, baseColor) else configuredTextColor
    val stroke = if (table.strokeColorCompose) blend(Color(table.strokeColor), baseColor) else Color(table.strokeColor)
    val displayAlpha = if (current) 1f else table.otherWeekAlpha.coerceIn(0f, 1f)
    val title = buildString {
        if (!current) append("[非本周]\n")
        append(item.course.name)
        if (table.showLocation && time.room.isNotBlank()) {
            append("\n")
            if (table.showRoomPrefix) append("@")
            append(time.room)
        }
        if (time.weekType == CourseTimeEntity.TYPE_ODD) append("\n单周")
        if (time.weekType == CourseTimeEntity.TYPE_EVEN) append("\n双周")
    }
    val scheduleTime = if (time.ownTime && time.startTime.isNotBlank() && time.endTime.isNotBlank()) {
        "${time.startTime}-${time.endTime}"
    } else {
        val start = nodeTimes.firstOrNull { it.node == time.startNode }?.start.orEmpty()
        val end = nodeTimes.firstOrNull { it.node == time.startNode + step - 1 }?.end.orEmpty()
        if (start.isNotBlank() && end.isNotBlank()) "$start-$end" else ""
    }
    val horizontal = if (table.itemCenterHorizontal) Alignment.CenterHorizontally else Alignment.Start
    val vertical = if (table.itemCenterVertical) Arrangement.Center else Arrangement.Top
    var dragging by remember(time.id) { mutableStateOf(false) }
    var dragX by remember(time.id) { mutableFloatStateOf(0f) }
    var dragY by remember(time.id) { mutableFloatStateOf(0f) }
    var dragStartScroll by remember(time.id) { mutableFloatStateOf(0f) }
    var dragChanged by remember(time.id) { mutableStateOf(false) }
    val dragScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val columnWidthPx = with(density) { columnWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val cardHeightPx = rowHeightPx * step.toFloat()

    Box(
        modifier = Modifier
            .offset(x = columnWidth * column.toFloat() + 1.dp, y = rowHeight * (time.startNode - 1).toFloat() + 1.dp)
            .graphicsLayer {
                scaleX = if (dragging) 1.1f else 1f
                scaleY = if (dragging) 1.1f else 1f
                translationX = dragX
                translationY = dragY + if (dragging) scrollState.value - dragStartScroll else 0f
            }
            .zIndex(if (dragging) 1f else 0f)
            .then(if (onMove == null) Modifier else Modifier.pointerInput(time.id, columnWidthPx, rowHeightPx, table.nodeCount, visibleDays.size, viewportHeightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val holdResult = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull false
                            if (!change.pressed) return@withTimeoutOrNull false
                            val deltaX = change.position.x - down.position.x
                            val deltaY = change.position.y - down.position.y
                            if (abs(deltaX) > viewConfiguration.touchSlop || abs(deltaY) > viewConfiguration.touchSlop) {
                                return@withTimeoutOrNull false
                            }
                        }
                    }
                    if (holdResult != null) return@awaitEachGesture

                    down.consume()
                    dragX = 0f
                    dragY = 0f
                    dragStartScroll = scrollState.value.toFloat()
                    dragChanged = false
                    dragging = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: break
                        if (!change.pressed) break
                        change.consume()
                        val delta = change.position - change.previousPosition
                        dragX += delta.x
                        dragY += delta.y
                        if (delta.x != 0f || delta.y != 0f) dragChanged = true
                        if (viewportHeightPx > 0 && scrollState.maxValue > 0) {
                            val scrollDelta = scrollState.value - dragStartScroll
                            val contentTop = rowHeightPx * (time.startNode - 1) + dragY + scrollDelta
                            val contentBottom = contentTop + cardHeightPx
                            val edge = with(density) { 48.dp.toPx() }
                            val viewportTop = scrollState.value.toFloat()
                            val viewportBottom = viewportTop + viewportHeightPx
                            val targetScroll = when {
                                contentTop < viewportTop + edge -> (scrollState.value - 24).coerceAtLeast(0)
                                contentBottom > viewportBottom - edge -> (scrollState.value + 24).coerceAtMost(scrollState.maxValue)
                                else -> null
                            }
                            if (targetScroll != null && targetScroll != scrollState.value) {
                                dragScope.launch { scrollState.animateScrollTo(targetScroll) }
                            }
                        }
                    }

                    val targetColumn = (column + (dragX / columnWidthPx).roundToInt())
                        .coerceIn(0, visibleDays.lastIndex)
                    val maxStart = (table.nodeCount - step + 1).coerceAtLeast(1)
                    val targetNode = (time.startNode + ((dragY + (scrollState.value - dragStartScroll)) / rowHeightPx).roundToInt())
                        .coerceIn(1, maxStart)
                    if (dragChanged && (targetColumn != column || targetNode != time.startNode)) {
                        onMove(visibleDays[targetColumn], targetNode)
                    }
                    dragX = 0f
                    dragY = 0f
                    dragStartScroll = 0f
                    dragChanged = false
                    dragging = false
                }
            })
            .width((columnWidth - 2.dp).coerceAtLeast(1.dp))
            .height((table.itemHeightDp.coerceIn(32, 128).dp * step.toFloat()) + (2.dp * (step - 1).toFloat()) - 2.dp)
            .alpha(displayAlpha)
            .background(baseColor.copy(alpha = table.itemAlpha.coerceIn(0f, 1f)), RoundedCornerShape(table.radius.coerceIn(0, 32).dp))
            .border(2.dp, stroke, RoundedCornerShape(table.radius.coerceIn(0, 32).dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = horizontal, verticalArrangement = vertical) {
            Text(title, color = textColor, fontSize = table.itemTextSize.coerceIn(8f, 32f).sp, fontWeight = FontWeight.Bold, lineHeight = (table.itemTextSize + 2f).sp)
            if (table.showTime && scheduleTime.isNotBlank()) Text(scheduleTime, color = textColor, fontSize = 11.sp, lineHeight = 13.sp)
            if (table.showTeacher && item.course.teacher.isNotBlank()) Text(item.course.teacher, color = textColor, fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun gridModifier(table: TableEntity, courseColor: Color?): Modifier {
    if (!table.showGrid) return Modifier
    val base = Color(table.strokeColor)
    val color = if (table.strokeColorCompose && courseColor != null) courseColor.copy(alpha = base.alpha) else base
    return if (table.useDottedLine) {
        Modifier.drawGridBorder(color)
    } else {
        Modifier.border(1.dp, color)
    }
}

private fun Modifier.drawGridBorder(color: Color): Modifier = drawBehind {
    val dash = 4.dp.toPx()
    drawRect(
        color = color,
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

@Composable
private fun WeekPickerDialog(
    maxWeek: Int,
    selectedWeek: Int,
    onSelect: (Int) -> Unit,
    onCurrent: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择周次") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items((1..maxWeek.coerceAtLeast(1)).toList()) { week ->
                    TextButton(onClick = { onSelect(week) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (week == selectedWeek) "✓ 第${week}周" else "第${week}周")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCurrent) { Text("回到当前周") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TablePickerDialog(
    tables: List<TableEntity>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换课表") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(tables, key = { it.id }) { table ->
                    TextButton(onClick = { onSelect(table.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(table.name, modifier = Modifier.weight(1f))
                            if (table.id == selectedId) Icon(Icons.Default.Check, contentDescription = "当前课表")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseActionSheet(
    course: CourseEntity,
    items: List<CourseItem>,
    nodeTimes: List<NodeTimeEntity>,
    table: TableEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteDialog by remember(course.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(table.textColor.toInt()))
            if (course.teacher.isNotBlank()) Text("授课老师：${course.teacher}")
            if (course.credit > 0f) Text("学分：${course.credit}")
            val rooms = items.map { it.time.room }.filter { it.isNotBlank() }.distinct().joinToString("、")
            if (rooms.isNotBlank()) Text("上课地点：$rooms")
            items.forEach { item ->
                val time = item.time
                val clock = courseTimeLabel(time, nodeTimes)
                Text("周${weekdayName(time.day)} · $clock · 第${time.startWeek}-${time.endWeek}周${Weeks.weekTypeLabel(time.weekType)}")
            }
            if (course.note.isNotBlank()) Text("备注：${course.note}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("复制")
                }
                OutlinedButton(onClick = { deleteDialog = true }) { Text("删除") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = onEdit) { Text("编辑") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (deleteDialog) {
        ConfirmDialog(
            title = "提示",
            text = "确定要删除该课程吗？它的所有时间段都将会被删除。",
            onConfirm = { deleteDialog = false; onDelete() },
            onDismiss = { deleteDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = Color(0xFFF7F7F7),
    titleSize: Int = 20,
    titleWeight: FontWeight = FontWeight.SemiBold,
    titleStartPadding: Dp = 0.dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                title = { Text(title, modifier = Modifier.padding(start = titleStartPadding), color = Color(0xFF141414), fontSize = titleSize.sp, fontWeight = titleWeight) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color(0xFF141414)) } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
            )
        },
        content = content,
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(title, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp), color = Color(0xFF626466), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String = "",
    tint: Color = Color(0xFF626466),
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFF141414), fontSize = 15.sp)
            if (summary.isNotBlank()) Text(summary, color = Color(0xFF626466), fontSize = 12.sp, lineHeight = 16.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun TableManagementScreen(
    tables: List<TableEntity>,
    currentTableId: Long,
    onBack: () -> Unit,
    onSelect: (Long) -> Unit,
    onEditSettings: (Long) -> Unit,
    onAppearance: (Long) -> Unit,
    onCourses: (Long) -> Unit,
    onCopy: (TableEntity) -> Unit,
    onDelete: (TableEntity) -> Unit,
    onNew: () -> Unit,
) {
    var sortByName by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TableEntity?>(null) }
    val displayTables = if (sortByName) tables.sortedBy { it.name } else tables

    ScreenScaffold(
        title = "多课表管理",
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = "排序") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("按添加顺序") }, onClick = { sortByName = false; sortMenu = false })
                    DropdownMenuItem(text = { Text("按名称排序") }, onClick = { sortByName = true; sortMenu = false })
                }
            }
            IconButton(onClick = onNew) { Icon(Icons.Default.Add, contentDescription = "新建课表") }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayTables.isEmpty()) {
                EmptyState("还没有课表", "创建一张课表开始记录课程", onNew, padding)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text("点击卡片切换当前课表\n长按拖动排序，使用下方按钮管理", modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFF626466), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    items(displayTables, key = { it.id }) { table ->
                        TableCard(
                            table = table,
                            selected = table.id == currentTableId,
                            onClick = { onSelect(table.id) },
                            onCourses = { onCourses(table.id) },
                            onSettings = { onEditSettings(table.id) },
                            onAppearance = { onAppearance(table.id) },
                            onCopy = { onCopy(table) },
                            onDelete = { deleting = table },
                        )
                    }
                }
            }
            FloatingActionButton(onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = Color(0xFFFF2D55), contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "新建课表")
            }
        }
    }
    deleting?.let { table ->
        if (tables.size <= 1) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("无法删除") },
                text = { Text("至少保留一张课表。请先新建另一张课表，再删除当前课表。") },
                confirmButton = { TextButton(onClick = { deleting = null }) { Text("知道了") } },
            )
        } else {
            ConfirmDialog(
                title = "提示",
                text = "确定要删除课表「${table.name}」吗？此操作不可撤销。",
                onConfirm = { deleting = null; onDelete(table) },
                onDismiss = { deleting = null },
            )
        }
    }
}

@Composable
private fun TableCard(
    table: TableEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onCourses: () -> Unit,
    onSettings: () -> Unit,
    onAppearance: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(96.dp).clickable(onClick = onClick)) {
                TablePreviewBackground(table)
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(table.name.ifBlank { "默认" }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Default.Check, contentDescription = "当前课表", tint = Color.White)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCourses) { Icon(Icons.Default.List, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("课程") }
                TextButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("设置") }
                TextButton(onClick = onAppearance) { Icon(Icons.Default.Palette, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("外观") }
                TextButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("复制") }
                TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("删除") }
            }
        }
    }
}

@Composable
private fun TablePreviewBackground(table: TableEntity) {
    val value = table.bgImageUri.orEmpty()
    if (value.startsWith("#")) {
        Box(modifier = Modifier.fillMaxSize().background(parseColor(value, Color(0xFF9AA8D8))))
    } else if (value.isNotBlank()) {
        AsyncImage(model = value, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF7B8CCB), Color(0xFFB4C9DE)))))
    }
}

@Composable
private fun TableSettingsScreen(
    table: TableEntity?,
    onBack: () -> Unit,
    onSave: (TableEntity) -> Unit,
    onUpdate: (TableEntity) -> Unit,
    currentWeek: Int,
    onAppearance: () -> Unit,
    onCourses: () -> Unit,
    onTimeTable: () -> Unit,
    onWidgetHelp: () -> Unit,
) {
    val defaultTable = remember {
        TableEntity(name = "我的课表", startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay())
    }
    var working by remember(table) { mutableStateOf(table ?: defaultTable) }
    val context = LocalContext.current
    var nameDialog by remember { mutableStateOf(false) }
    var numberDialog by remember { mutableStateOf<String?>(null) }
    var dateDialog by remember { mutableStateOf(false) }

    fun persistWorking() {
        if (table != null && working.name.isNotBlank()) onUpdate(working)
    }

    ScreenScaffold(
        title = "课表设置",
        onBack = {
            persistWorking()
            onBack()
        },
        actions = {
            IconButton(onClick = {
                if (working.name.isBlank()) {
                    Toast.makeText(context, "名称不能为空哦>_<", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(working)
                }
            }) { Icon(Icons.Default.Save, contentDescription = "保存") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                SettingsGroup("课表名称") {
                    SettingRow(Icons.Default.TableView, "课表名称", working.name, tint = Color(0xFF4E7BD9), onClick = { nameDialog = true })
                }
            }
            item {
                SettingsGroup("课表数据") {
                    SettingRow(Icons.Default.AccessTime, "上课时间", "点击此处更改", tint = Color(0xFF2AA69B), onClick = { persistWorking(); onTimeTable() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.CalendarMonth, "第一周的第一天", LocalDate.ofEpochDay(working.startDate).toString(), tint = Color(0xFF4E7BD9), onClick = { dateDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.CalendarMonth, "当前周", "第${currentWeek.coerceIn(1, working.maxWeek)}周", tint = Color(0xFF4E7BD9))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.List, "一天课程节数", "${working.nodeCount} 节", tint = Color(0xFF4E7BD9), onClick = { numberDialog = "nodeCount" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.CalendarMonth, "学期周数", "${working.maxWeek} 周", tint = Color(0xFF4E7BD9), onClick = { numberDialog = "maxWeek" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.CalendarMonth, "每周从周日开始", trailing = { Switch(checked = working.sundayFirst, onCheckedChange = { working = working.copy(sundayFirst = it) }) }, onClick = { working = working.copy(sundayFirst = !working.sundayFirst) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.List, "管理已添加课程", "集中编辑或清空课程", tint = Color(0xFF4E7BD9), onClick = { persistWorking(); onCourses() })
                }
            }
            item {
                SettingsGroup("课表外观") {
                    SettingRow(Icons.Default.Visibility, "显示周六", trailing = { Switch(checked = working.showSat, onCheckedChange = { working = working.copy(showSat = it, showWeekend = it || working.showSun) }) }, onClick = { working = working.copy(showSat = !working.showSat, showWeekend = !working.showSat || working.showSun) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Visibility, "显示周日", trailing = { Switch(checked = working.showSun, onCheckedChange = { working = working.copy(showSun = it, showWeekend = working.showSat || it) }) }, onClick = { working = working.copy(showSun = !working.showSun, showWeekend = working.showSat || !working.showSun) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Visibility, "显示非本周课程", trailing = { Switch(checked = working.showOtherWeekCourse, onCheckedChange = { working = working.copy(showOtherWeekCourse = it) }) }, onClick = { working = working.copy(showOtherWeekCourse = !working.showOtherWeekCourse) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Tune, "更多外观设置", "课程格子、文字与颜色", tint = Color(0xFF8D62C8), onClick = { persistWorking(); onAppearance() })
                }
            }
            item {
                SettingsGroup("默认配置") {
                    SettingRow(Icons.Default.Widgets, "桌面小部件", "添加、调整和排查课表小部件", tint = Color(0xFF4E7BD9), onClick = onWidgetHelp)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Refresh, "恢复默认外观", "恢复渐变背景和基础显示设置", tint = Color(0xFFFF8A00), onClick = {
                        working = working.copy(
                            bgImageUri = null,
                            showWeekend = true,
                            showSat = true,
                            showSun = true,
                            sundayFirst = false,
                            textColor = 0xFF000000,
                            itemTextSize = 12f,
                            itemAlpha = 0.5f,
                            itemHeightDp = 64,
                            showTime = false,
                            showLocation = true,
                            showRoomPrefix = true,
                            showTeacher = true,
                            showOtherWeekCourse = true,
                            otherWeekAlpha = 0.5f,
                            showGrid = false,
                            showTimeBar = true,
                            headerTextSize = 11,
                            courseTextColor = 0xFFFFFFFF.toInt(),
                            strokeColor = 0x80FFFFFF.toInt(),
                            useDottedLine = false,
                            itemCenterHorizontal = false,
                            itemCenterVertical = false,
                            textColorCompose = false,
                            strokeColorCompose = false,
                            radius = 4,
                        )
                    })
                }
            }
        }
    }

    if (nameDialog) {
        TextInputDialog(
            title = "课表名称",
            initial = working.name,
            validation = { value -> if (value.isBlank()) "名称不能为空哦>_<" else null },
            onConfirm = { value -> working = working.copy(name = value.trim()); nameDialog = false },
            onDismiss = { nameDialog = false },
        )
    }
    numberDialog?.let { field ->
        val initial = if (field == "nodeCount") working.nodeCount.toString() else working.maxWeek.toString()
        TextInputDialog(
            title = if (field == "nodeCount") "一天课程节数" else "学期周数",
            initial = initial,
            number = true,
            validation = { value -> if (value.toIntOrNull() == null) "请输入有效数字" else null },
            onConfirm = { value ->
            val number = value.toIntOrNull()
            if (number != null) {
                working = if (field == "nodeCount") working.copy(nodeCount = number.coerceIn(1, 60)) else working.copy(maxWeek = number.coerceIn(1, 60))
            }
                numberDialog = null
            },
            onDismiss = { numberDialog = null },
        )
    }
    if (dateDialog) {
        DatePickerDialogFor(working.startDate, onSelected = { date -> working = working.copy(startDate = date.toEpochDay()); dateDialog = false }, onDismiss = { dateDialog = false })
    }
}

@Composable
private fun AppearanceScreen(
    table: TableEntity,
    meta: TableWithMeta,
    onBack: () -> Unit,
    onSave: (TableEntity) -> Unit,
) {
    var working by remember(table) { mutableStateOf(table) }
    var colorTarget by remember { mutableStateOf<String?>(null) }
    var inputTarget by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            working = working.copy(bgImageUri = uri.toString())
        }
    }

    ScreenScaffold(
        title = "课表外观",
        onBack = {
            onSave(working)
            showToast(context, "保存成功")
            onBack()
        },
        actions = {
            IconButton(onClick = {
                onSave(working)
                showToast(context, "保存成功")
            }) { Icon(Icons.Default.Save, contentDescription = "保存") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                AppearancePreview(table = working, meta = meta)
            }
            item {
                SettingsGroup("整体") {
                    SettingRow(
                        Icons.Default.Palette,
                        "课程表背景",
                        if (working.bgImageUri.isNullOrBlank()) "默认渐变背景" else "已设置图片背景",
                        tint = Color(0xFF4E7BD9),
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        trailing = {
                            if (!working.bgImageUri.isNullOrBlank()) {
                                TextButton(onClick = { working = working.copy(bgImageUri = null) }) { Text("清除") }
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.GridOn, "显示网格辅助线", trailing = { Switch(checked = working.showGrid, onCheckedChange = { working = working.copy(showGrid = it) }) }, onClick = { working = working.copy(showGrid = !working.showGrid) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.TextFields, "界面文字颜色", colorName(working.textColor.toInt()), tint = Color(0xFF2AA69B), onClick = { colorTarget = "text" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.TextFields, "表头文字大小", "${working.headerTextSize} sp", tint = Color(0xFF2AA69B), onClick = { inputTarget = "headerTextSize" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.AccessTime, "节数栏显示时间", trailing = { Switch(checked = working.showTimeBar, onCheckedChange = { working = working.copy(showTimeBar = it) }) }, onClick = { working = working.copy(showTimeBar = !working.showTimeBar) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Visibility, "显示周六", trailing = { Switch(checked = working.showSat, onCheckedChange = { working = working.copy(showSat = it, showWeekend = it || working.showSun) }) }, onClick = { working = working.copy(showSat = !working.showSat, showWeekend = !working.showSat || working.showSun) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Visibility, "显示周日", trailing = { Switch(checked = working.showSun, onCheckedChange = { working = working.copy(showSun = it, showWeekend = working.showSat || it) }) }, onClick = { working = working.copy(showSun = !working.showSun, showWeekend = working.showSat || !working.showSun) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Visibility, "显示非本周课程", trailing = { Switch(checked = working.showOtherWeekCourse, onCheckedChange = { working = working.copy(showOtherWeekCourse = it) }) }, onClick = { working = working.copy(showOtherWeekCourse = !working.showOtherWeekCourse) })
                }
            }
            item {
                SettingsGroup("课程格子") {
                    SettingRow(Icons.Default.TextFields, "课程文字颜色", colorName(working.courseTextColor), tint = Color(0xFF4E7BD9), onClick = { colorTarget = "course" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "课程文字颜色叠加格子颜色", trailing = { Switch(checked = working.textColorCompose, onCheckedChange = { working = working.copy(textColorCompose = it) }) }, onClick = { working = working.copy(textColorCompose = !working.textColorCompose) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "格子边框颜色", colorName(working.strokeColor), tint = Color(0xFFFF8A00), onClick = { colorTarget = "stroke" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "边框使用格子颜色", trailing = { Switch(checked = working.strokeColorCompose, onCheckedChange = { working = working.copy(strokeColorCompose = it) }) }, onClick = { working = working.copy(strokeColorCompose = !working.strokeColorCompose) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.GridOn, "课表格子使用虚线边框", trailing = { Switch(checked = working.useDottedLine, onCheckedChange = { working = working.copy(useDottedLine = it) }) }, onClick = { working = working.copy(useDottedLine = !working.useDottedLine) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Tune, "课程格子高度", "${working.itemHeightDp} dp", tint = Color(0xFF8D62C8), onClick = { inputTarget = "height" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Tune, "格子圆角半径", "${working.radius} dp", tint = Color(0xFF8D62C8), onClick = { inputTarget = "radius" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "课程格子不透明度", "${(working.itemAlpha * 100).toInt()}%", tint = Color(0xFF8D62C8), onClick = { inputTarget = "alpha" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "非本周课程不透明比", "${(working.otherWeekAlpha * 100).toInt()}%", tint = Color(0xFF8D62C8), onClick = { inputTarget = "otherAlpha" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.TextFields, "课程显示文字大小", "${working.itemTextSize} sp", tint = Color(0xFF4E7BD9), onClick = { inputTarget = "itemTextSize" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.TextFields, "格子文字水平居中", trailing = { Switch(checked = working.itemCenterHorizontal, onCheckedChange = { working = working.copy(itemCenterHorizontal = it) }) }, onClick = { working = working.copy(itemCenterHorizontal = !working.itemCenterHorizontal) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.TextFields, "格子文字竖直居中", trailing = { Switch(checked = working.itemCenterVertical, onCheckedChange = { working = working.copy(itemCenterVertical = it) }) }, onClick = { working = working.copy(itemCenterVertical = !working.itemCenterVertical) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.AccessTime, "在格子内显示上课时间", trailing = { Switch(checked = working.showTime, onCheckedChange = { working = working.copy(showTime = it) }) }, onClick = { working = working.copy(showTime = !working.showTime) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Place, "在格子内显示上课地点", trailing = { Switch(checked = working.showLocation, onCheckedChange = { working = working.copy(showLocation = it) }) }, onClick = { working = working.copy(showLocation = !working.showLocation) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Place, "上课地点前显示@", trailing = { Switch(checked = working.showRoomPrefix, onCheckedChange = { working = working.copy(showRoomPrefix = it) }) }, onClick = { working = working.copy(showRoomPrefix = !working.showRoomPrefix) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Person, "在格子内显示授课老师", trailing = { Switch(checked = working.showTeacher, onCheckedChange = { working = working.copy(showTeacher = it) }) }, onClick = { working = working.copy(showTeacher = !working.showTeacher) })
                }
            }
        }
    }

    colorTarget?.let { target ->
        ColorChoiceDialog(
            title = when (target) { "text" -> "界面文字颜色"; "course" -> "课程文字颜色"; else -> "格子边框颜色" },
            initial = when (target) { "text" -> working.textColor.toInt(); "course" -> working.courseTextColor; else -> working.strokeColor },
            onSelect = { color ->
                working = when (target) {
                    "text" -> working.copy(textColor = color.toLong())
                    "course" -> working.copy(courseTextColor = color)
                    else -> working.copy(strokeColor = color)
                }
                colorTarget = null
            },
            onDismiss = { colorTarget = null },
        )
    }
    inputTarget?.let { target ->
        val initial = when (target) {
            "headerTextSize" -> working.headerTextSize.toString()
            "height" -> working.itemHeightDp.toString()
            "radius" -> working.radius.toString()
            "alpha" -> (working.itemAlpha * 100).toInt().toString()
            "otherAlpha" -> (working.otherWeekAlpha * 100).toInt().toString()
            else -> working.itemTextSize.toString()
        }
        TextInputDialog(
            title = when (target) {
                "headerTextSize" -> "表头文字大小"
                "height" -> "课程格子高度"
                "radius" -> "格子圆角半径"
                "alpha" -> "课程格子不透明度（0-100）"
                "otherAlpha" -> "非本周课程不透明比（0-100）"
                else -> "课程显示文字大小"
            },
            initial = initial,
            number = true,
            clearable = false,
            onConfirm = { value ->
                val number = value.toFloatOrNull()
                working = when (target) {
                    "headerTextSize" -> working.copy(headerTextSize = number?.toInt()?.coerceIn(8, 32) ?: working.headerTextSize)
                    "height" -> working.copy(itemHeightDp = number?.toInt()?.coerceIn(32, 128) ?: working.itemHeightDp)
                    "radius" -> working.copy(radius = number?.toInt()?.coerceIn(0, 32) ?: working.radius)
                    "alpha" -> working.copy(itemAlpha = (number ?: (working.itemAlpha * 100)).coerceIn(0f, 100f) / 100f)
                    "otherAlpha" -> working.copy(otherWeekAlpha = (number ?: (working.otherWeekAlpha * 100)).coerceIn(0f, 100f) / 100f)
                    else -> working.copy(itemTextSize = number?.coerceIn(8f, 32f) ?: working.itemTextSize)
                }
                inputTarget = null
            },
            onDismiss = { inputTarget = null },
        )
    }
}

@Composable
private fun AppearancePreview(table: TableEntity, meta: TableWithMeta) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
            ScheduleBackground(table)
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("预览", color = Color(table.textColor.toInt()), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (1..5).forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("${weekdayName(day)}\n${day + 1}", color = Color(table.textColor.toInt()).copy(alpha = 0.7f), fontSize = 10.sp) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(5) { index ->
                        val color = CourseColors.asColor(CourseColors.all()[index % CourseColors.all().size])
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = (index * 8).dp).background(color.copy(alpha = table.itemAlpha), RoundedCornerShape(table.radius.dp)).border(1.dp, Color(table.strokeColor), RoundedCornerShape(table.radius.dp)), contentAlignment = Alignment.Center) {
                            Text(meta.courses.getOrNull(index)?.name ?: "课程", color = Color(table.courseTextColor), fontSize = table.itemTextSize.coerceIn(8f, 20f).sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseManagementScreen(
    table: TableEntity,
    courses: List<CourseEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (CourseEntity) -> Unit,
    onDelete: (CourseEntity) -> Unit,
    onClear: (List<CourseEntity>) -> Unit,
) {
    var sortMenu by remember { mutableStateOf(false) }
    var sortByName by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var deleteCourse by remember { mutableStateOf<CourseEntity?>(null) }
    val displayCourses = if (sortByName) courses.sortedBy { it.name } else courses

    ScreenScaffold(
        title = "课程管理",
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = "排序") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("按添加顺序") }, onClick = { sortByName = false; sortMenu = false })
                    DropdownMenuItem(text = { Text("按课程名称") }, onClick = { sortByName = true; sortMenu = false })
                    DropdownMenuItem(text = { Text("清空") }, onClick = { sortMenu = false; clearDialog = true })
                }
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "添加课程") }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayCourses.isEmpty()) {
                EmptyState("还没有添加任何课程哦", "轻触右上角加号开始添加", onAdd, padding)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("轻触编辑，长按删除", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFF626466), fontSize = 12.sp) }
                    items(displayCourses, key = { it.id }) { course ->
                        val color = CourseColors.asColor(courseColor(course))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 16.dp)
                                .combinedClickable(onClick = { onEdit(course) }, onLongClick = { deleteCourse = course }),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.32f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(course.name, color = Color(0xFF141414), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = Color(0xFFFF2D55), contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        }
    }
    if (clearDialog) {
        ConfirmDialog(title = "提示", text = "真的要清空课表吗？这将无法恢复。", onConfirm = { clearDialog = false; onClear(courses) }, onDismiss = { clearDialog = false })
    }
    deleteCourse?.let { course ->
        ConfirmDialog(title = "提示", text = "确定要删除该课程吗？它的所有时间段都将会被删除。", onConfirm = { deleteCourse = null; onDelete(course) }, onDismiss = { deleteCourse = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditorScreen(
    repository: TimetableRepository,
    table: TableEntity,
    courseId: Long?,
    initialDay: Int,
    initialStartNode: Int,
    initialStep: Int,
    existingCourses: List<CourseEntity>,
    nodeTimes: List<NodeTimeEntity>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember(courseId, table.id) { mutableStateOf("") }
    var teacher by remember(courseId, table.id) { mutableStateOf("") }
    var note by remember(courseId, table.id) { mutableStateOf("") }
    var credit by remember(courseId, table.id) { mutableStateOf("") }
    var color by remember(courseId, table.id) { mutableIntStateOf(0) }
    val drafts = remember(courseId, table.id, initialDay, initialStartNode, initialStep) { mutableStateListOf<TimeDraft>() }
    var loaded by remember(courseId, table.id, initialDay, initialStartNode, initialStep) { mutableStateOf(false) }
    var error by remember(courseId, table.id, initialDay, initialStartNode, initialStep) { mutableStateOf<String?>(null) }
    var inputTarget by remember { mutableStateOf<String?>(null) }
    var colorDialog by remember { mutableStateOf(false) }
    var customTimeInfo by remember { mutableStateOf(false) }
    var roomEditorIndex by remember { mutableStateOf<Int?>(null) }
    val availableNodeTimes = nodeTimesFor(table, nodeTimes)

    LaunchedEffect(courseId, table.id, initialDay, initialStartNode, initialStep) {
        drafts.clear()
        if (courseId == null) {
            drafts.add(defaultTimeDraft(table.maxWeek, initialDay, initialStartNode, initialStep))
        } else {
            val course = repository.getCourse(courseId)
            if (course != null) {
                name = course.name
                teacher = course.teacher
                note = course.note
                credit = if (course.credit == 0f) "" else course.credit.toString()
                color = course.color
                repository.getCourseTimes(courseId).forEach { time ->
                    drafts += TimeDraft(
                        id = time.id,
                        day = time.day,
                        startNode = time.startNode,
                        step = time.step,
                        selectedWeeks = selectedWeeks(time, table.maxWeek),
                        weekType = time.weekType,
                        room = time.room,
                        ownTime = time.ownTime,
                        startTime = time.startTime,
                        endTime = time.endTime,
                    )
                }
                if (drafts.isEmpty()) drafts.add(defaultTimeDraft(table.maxWeek, initialDay, initialStartNode, initialStep))
            }
        }
        loaded = true
    }

    if (!loaded) {
        LoadingView()
        return
    }

    fun save() {
        when {
            name.isBlank() -> {
                error = "请填写课程名称"
                showToast(context, "请填写课程名称")
            }
            drafts.isEmpty() -> {
                error = "请至少添加一个时间段"
                showToast(context, "请至少添加一个时间段")
            }
            drafts.any { it.selectedWeeks.isEmpty() } -> {
                error = "请至少选择一周"
                showToast(context, "请至少选择一周")
            }
            else -> scope.launch {
                val entity = CourseEntity(
                    id = courseId ?: 0L,
                    tableId = table.id,
                    name = name.trim(),
                    color = if (color == 0) CourseColors.colorFor(name) else color,
                    teacher = teacher.trim(),
                    note = note.trim(),
                    credit = credit.toFloatOrNull() ?: 0f,
                )
                repository.saveCourse(table.id, entity, drafts.flatMap { it.toEntities(entity.id) })
                showToast(context, "保存成功")
                onSaved()
            }
        }
    }

    ScreenScaffold(
        title = if (courseId == null) "添加课程" else "编辑课程",
        onBack = onBack,
        containerColor = WakeUpEditorBackground,
        titleSize = 22,
        titleWeight = FontWeight.Normal,
        titleStartPadding = 3.dp,
        actions = {
            TextButton(onClick = ::save, modifier = Modifier.offset(x = 7.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("保存", color = WakeUpEditorText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    CourseBaseEditorSection(
                        name = name,
                        onNameChange = { name = it },
                        candidates = existingCourses,
                        color = color,
                        credit = credit,
                        note = note,
                        onColorClick = { colorDialog = true },
                        onCreditClick = { inputTarget = "credit" },
                        onNoteClick = { inputTarget = "note" },
                    )
                }
                items(drafts.indices.toList(), key = { drafts[it].id * 1000L + it }) { index ->
                    TimeDraftEditor(
                        index = index,
                        draft = drafts[index],
                        table = table,
                        nodeTimes = availableNodeTimes,
                        onChange = { drafts[index] = it },
                        onRemove = { if (drafts.size > 1) drafts.removeAt(index) },
                        onTeacher = { inputTarget = "teacher" },
                        onRoom = { roomEditorIndex = index },
                        teacher = teacher,
                        onCustomTimeInfo = { customTimeInfo = true },
                    )
                }
                item { error?.let { Text(it, modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp), color = Color(0xFFD32F2F), fontSize = 13.sp) } }
            }
            FloatingActionButton(
                onClick = { drafts += defaultTimeDraft(table.maxWeek, 1, 1) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 40.dp),
                containerColor = Color(0xFFDCE1FF),
                contentColor = Color(0xFF03174B),
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加时间段")
            }
        }
    }

    inputTarget?.let { target ->
        val initial = when (target) { "credit" -> credit; "note" -> note; else -> teacher }
        TextInputDialog(
            title = when (target) { "credit" -> "学分"; "note" -> "备注"; else -> "授课老师" },
            initial = initial,
            number = target == "credit",
            multiline = target == "note",
            clearable = true,
            onConfirm = { value ->
                when (target) { "credit" -> credit = value; "note" -> note = value; else -> teacher = value }
                inputTarget = null
            },
            onClear = {
                when (target) { "credit" -> credit = ""; "note" -> note = ""; else -> teacher = "" }
                inputTarget = null
            },
            onDismiss = { inputTarget = null },
        )
    }
    roomEditorIndex?.let { index ->
        TextInputDialog(
            title = "上课地点",
            initial = drafts.getOrNull(index)?.room.orEmpty(),
            clearable = true,
            onConfirm = { value -> drafts[index] = drafts[index].copy(room = value); roomEditorIndex = null },
            onClear = { drafts[index] = drafts[index].copy(room = ""); roomEditorIndex = null },
            onDismiss = { roomEditorIndex = null },
        )
    }
    if (colorDialog) {
        ColorChoiceDialog(title = "课程颜色", initial = color, includeAuto = true, onSelect = { color = it; colorDialog = false }, onDismiss = { colorDialog = false })
    }
    if (customTimeInfo) {
        AlertDialog(
            onDismissRequest = { customTimeInfo = false },
            title = { Text("自定义时间") },
            text = { Text("自定义时间会按照具体时间决定显示位置。") },
            confirmButton = { TextButton(onClick = { customTimeInfo = false }) { Text("知道了") } },
        )
    }
}

@Composable
private fun CourseBaseEditorSection(
    name: String,
    onNameChange: (String) -> Unit,
    candidates: List<CourseEntity>,
    color: Int,
    credit: String,
    note: String,
    onColorClick: () -> Unit,
    onCreditClick: () -> Unit,
    onNoteClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WakeUpEditorRow(R.drawable.sd_ic_twotone_class_24, WakeUpTeal) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                singleLine = true,
                textStyle = TextStyle(color = WakeUpEditorText, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (name.isBlank()) Text("课程名称", color = WakeUpEditorHint, fontSize = 14.sp)
                        innerTextField()
                    }
                },
            )
        }
        if (name.isBlank() && candidates.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 56.dp, end = 24.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                candidates.distinctBy { it.name }.take(8).forEach { candidate ->
                    OutlinedButton(onClick = { onNameChange(candidate.name) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                        Text(candidate.name, maxLines = 1, fontSize = 12.sp)
                    }
                }
            }
        }
        val selectedColor = if (color == 0) Color(0xFF3480FF) else CourseColors.asColor(color)
        WakeUpEditorRow(R.drawable.sd_ic_twotone_colorize_24, selectedColor, onClick = onColorClick) {
            Text("点此更改颜色", color = selectedColor, fontSize = 14.sp)
        }
        WakeUpEditorRow(R.drawable.sd_ic_twotone_assistant_photo_24, WakeUpBlue, onClick = onCreditClick) {
            Text(
                if (credit.isBlank()) "学分（可不填）" else "$credit 学分",
                color = if (credit.isBlank()) WakeUpEditorHint else WakeUpEditorText,
                fontSize = 14.sp,
            )
        }
        WakeUpEditorRow(R.drawable.sd_ic_twotone_sticky_note_2_24, WakeUpYellow, onClick = onNoteClick) {
            Text(
                if (note.isBlank()) "备注（可不填）" else note,
                color = if (note.isBlank()) WakeUpEditorHint else WakeUpEditorText,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = WakeUpEditorDivider)
    }
}

@Composable
private fun WakeUpEditorRow(
    iconRes: Int,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 0.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = iconTint, modifier = Modifier.size(56.dp).padding(16.dp))
        content()
    }
}

@Composable
private fun TimeDraftEditor(
    index: Int,
    draft: TimeDraft,
    table: TableEntity,
    nodeTimes: List<NodeTimeEntity>,
    onChange: (TimeDraft) -> Unit,
    onRemove: () -> Unit,
    onTeacher: () -> Unit,
    onRoom: () -> Unit,
    teacher: String,
    onCustomTimeInfo: () -> Unit,
) {
    var weekDialog by remember(draft.id, index) { mutableStateOf(false) }
    var timeDialog by remember(draft.id, index) { mutableStateOf(false) }
    var customTimeField by remember(draft.id, index) { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(43.dp)) {
            Text("时间段", modifier = Modifier.padding(start = 16.dp, top = 24.dp), color = WakeUpEditorText, fontSize = 12.sp)
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(32.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "删除时间段", tint = WakeUpEditorText, modifier = Modifier.size(20.dp))
            }
        }
        WakeUpEditorRow(R.drawable.sd_ic_twotone_today_24, WakeUpTeal, onClick = { weekDialog = true }) {
            Text(selectedWeekLabel(draft.selectedWeeks, table.maxWeek), color = WakeUpEditorText, fontSize = 14.sp)
        }
        WakeUpEditorRow(R.drawable.sd_ic_twotone_access_time_24, WakeUpOrange, onClick = { timeDialog = true }) {
            Text(timeRowLabel(draft), color = WakeUpEditorText, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("自定义时间", color = if (draft.ownTime) WakeUpEditorText else WakeUpEditorHint, fontSize = 14.sp)
                Checkbox(
                    checked = draft.ownTime,
                    onCheckedChange = {
                        if (it) onCustomTimeInfo()
                        onChange(draft.copy(ownTime = it))
                    },
                    modifier = Modifier.size(36.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = WakeUpTeal,
                        uncheckedColor = Color(0xFF5C5962),
                        checkmarkColor = Color.White,
                    ),
                )
            }
        }
        if (draft.ownTime) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 104.dp, end = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { customTimeField = "start" }, modifier = Modifier.weight(1f)) { Text(draft.startTime.ifBlank { "上课时间" }) }
                OutlinedButton(onClick = { customTimeField = "end" }, modifier = Modifier.weight(1f)) { Text(draft.endTime.ifBlank { "下课时间" }) }
            }
        }
        WakeUpEditorRow(R.drawable.sd_ic_twotone_person_24, WakeUpBlue, onClick = onTeacher) {
            Text(
                if (teacher.isBlank()) "授课老师（可不填）" else teacher,
                color = if (teacher.isBlank()) WakeUpEditorHint else WakeUpEditorText,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
        WakeUpEditorRow(R.drawable.sd_ic_twotone_meeting_room_24, WakeUpRed, onClick = onRoom) {
            Text(
                if (draft.room.isBlank()) "上课地点（可不填）" else draft.room,
                color = if (draft.room.isBlank()) WakeUpEditorHint else WakeUpEditorText,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
    }

    if (weekDialog) {
        AlertDialog(
            onDismissRequest = { weekDialog = false },
            title = { Text("选择周数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekNumberGrid(maxWeek = table.maxWeek, selected = draft.selectedWeeks, onChange = { onChange(draft.copy(selectedWeeks = it)) })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeekTypeButton("每周", draft.weekType == CourseTimeEntity.TYPE_ALL, { onChange(draft.copy(weekType = CourseTimeEntity.TYPE_ALL, selectedWeeks = (1..table.maxWeek).toSet())) }, Modifier.weight(1f))
                        WeekTypeButton("单周", draft.weekType == CourseTimeEntity.TYPE_ODD, { onChange(draft.copy(weekType = CourseTimeEntity.TYPE_ODD, selectedWeeks = (1..table.maxWeek).filter { it % 2 == 1 }.toSet())) }, Modifier.weight(1f))
                        WeekTypeButton("双周", draft.weekType == CourseTimeEntity.TYPE_EVEN, { onChange(draft.copy(weekType = CourseTimeEntity.TYPE_EVEN, selectedWeeks = (1..table.maxWeek).filter { it % 2 == 0 }.toSet())) }, Modifier.weight(1f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { weekDialog = false }) { Text("完成") } },
        )
    }
    if (timeDialog) {
        TimeSelectionDialog(
            draft = draft,
            maxNode = table.nodeCount.coerceIn(1, 60),
            onChange = onChange,
            onDismiss = { timeDialog = false },
        )
    }
    customTimeField?.let { field ->
        TimePickerDialogFor(
            value = if (field == "start") draft.startTime else draft.endTime,
            onSelected = { value ->
                onChange(if (field == "start") draft.copy(startTime = value) else draft.copy(endTime = value))
                customTimeField = null
            },
            onDismiss = { customTimeField = null },
        )
    }
}

private fun timeRowLabel(draft: TimeDraft): String =
    "周${weekdayName(draft.day)}    第${draft.startNode} - ${draft.startNode + draft.step - 1}节"

@Composable
private fun TimeSelectionDialog(
    draft: TimeDraft,
    maxNode: Int,
    onChange: (TimeDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择上课时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeChoiceMenuRow("星期", "周${weekdayName(draft.day)}", (1..7).map { it to "周${weekdayName(it)}" }) { onChange(draft.copy(day = it)) }
                TimeChoiceMenuRow("开始节", "第${draft.startNode}节", (1..maxNode).map { it to "第${it}节" }) { value -> onChange(draft.copy(startNode = value, step = draft.step.coerceAtMost((maxNode - value + 1).coerceAtLeast(1)))) }
                TimeChoiceMenuRow("连续节数", "${draft.step}节", (1..(maxNode - draft.startNode + 1).coerceAtLeast(1)).map { it to "${it}节" }) { onChange(draft.copy(step = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun TimeChoiceMenuRow(
    label: String,
    value: String,
    options: List<Pair<Int, String>>,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember(label, value) { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = WakeUpEditorText, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) { Text(value, color = WakeUpTeal, fontSize = 14.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (optionValue, optionLabel) ->
                    DropdownMenuItem(text = { Text(optionLabel) }, onClick = { onSelected(optionValue); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun WeekNumberGrid(maxWeek: Int, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp).padding(horizontal = 16.dp), userScrollEnabled = false) {
        gridItems((1..maxWeek.coerceIn(1, 60)).toList()) { week ->
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (week in selected) Color(0xFFFF2D55) else Color(0xFFF0F0F0))
                    .clickable { onChange(if (week in selected) selected - week else selected + week) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$week", color = if (week in selected) Color.White else Color(0xFF626466), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WeekTypeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (selected) Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 0.dp)) { Text(label) }
    else OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 0.dp)) { Text(label) }
}

@Composable
private fun ChoicePickerDialog(
    title: String,
    options: List<Pair<Int, String>>,
    selectedValue: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(options, key = { it.first }) { (value, label) ->
                    TextButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (value == selectedValue) {
                                Icon(Icons.Default.Check, contentDescription = "已选择")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TimeSettingsScreen(
    repository: TimetableRepository,
    table: TableEntity,
    onBack: () -> Unit,
    onCopy: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rows = remember(table.id) { mutableStateListOf<NodeDraft>() }
    var loaded by remember(table.id) { mutableStateOf(false) }
    var uniform by remember(table.id) { mutableStateOf(false) }
    var duration by remember(table.id) { mutableStateOf("50") }
    var addDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NodeDraft?>(null) }
    var uniformConfirm by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var timePickerTarget by remember(table.id) { mutableStateOf<TimePickerTarget?>(null) }

    LaunchedEffect(table.id) {
        rows.clear()
        val source = nodeTimesFor(table, repository.getNodeTimes(table.id))
        rows.addAll(source.map { NodeDraft(it.id, it.node, it.start, it.end) })
        loaded = true
    }
    if (!loaded) {
        LoadingView()
        return
    }
    ScreenScaffold(
        title = "时间表",
        onBack = onBack,
        actions = {
            IconButton(onClick = { addDialog = true }) { Icon(Icons.Default.Add, contentDescription = "添加节次") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("复制") }, onClick = { menu = false; onCopy() })
                    DropdownMenuItem(text = { Text("保存") }, onClick = {
                        menu = false
                        scope.launch {
                            repository.saveNodeTimes(table.id, rows.map { NodeTimeEntity(it.id, table.id, it.node, it.start, it.end) })
                            showToast(context, "保存成功")
                        }
                    })
                }
            }
            IconButton(onClick = {
                scope.launch {
                    repository.saveNodeTimes(table.id, rows.map { NodeTimeEntity(it.id, table.id, it.node, it.start, effectiveEnd(it.start, it.end, uniform, duration)) })
                    showToast(context, "保存成功")
                }
            }) { Icon(Icons.Default.Save, contentDescription = "保存") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("要用多少节就调整多少节的时间，多余的节数忽略即可。\n如果需要单独设置某节课或某地点的时间，请直接编辑该课程，勾选「自定义时间」", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = Color(0xFF626466), fontSize = 12.sp, lineHeight = 18.sp)
            }
            item {
                SettingsGroup("当前课表关联的时间表") {
                    SettingRow(Icons.Default.AccessTime, "默认时间表", "点击即可编辑 · 24 小时制", tint = Color(0xFF2AA69B))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.ContentCopy, "复制时间表", "复制整张课表时会一并复制时间表", tint = Color(0xFF4E7BD9), onClick = onCopy)
                }
            }
            item {
                SettingsGroup("编辑时间表") {
                    SettingRow(Icons.Default.Tune, "每节课时长相同", trailing = { Switch(checked = uniform, onCheckedChange = { if (it) uniformConfirm = true else uniform = false }) }, onClick = { if (!uniform) uniformConfirm = true else uniform = false })
                    if (uniform) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                        SettingRow(Icons.Default.AccessTime, "一节课时长", "$duration 分钟", tint = Color(0xFF4E7BD9), onClick = { addDialog = true })
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    Text("使用 24 小时制。点击每节课的开始或结束时间进行调整。", modifier = Modifier.padding(16.dp), color = Color(0xFF626466), fontSize = 12.sp)
                }
            }
            items(rows.indices.toList(), key = { rows[it].node }) { index ->
                val row = rows[index]
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${row.node}", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = Color(0xFF141414))
                        OutlinedButton(onClick = { timePickerTarget = TimePickerTarget(index, isStart = true) }, modifier = Modifier.weight(1f)) { Text(row.start) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { timePickerTarget = TimePickerTarget(index, isStart = false) }, modifier = Modifier.weight(1f)) { Text(row.end) }
                        IconButton(onClick = { if (rows.size > 1) deleting = row }) { Icon(Icons.Default.Delete, contentDescription = "删除第${row.node}节", tint = if (rows.size > 1) Color(0xFFE53935) else Color.LightGray) }
                    }
                }
            }
        }
    }
    if (addDialog) {
        TextInputDialog(
            title = "添加节次 / 统一课时长度",
            initial = if (uniform) duration else "1",
            number = true,
            validation = { value -> if (value.toIntOrNull() == null) "请输入有效数字" else null },
            onConfirm = { value ->
            if (uniform) duration = value.toIntOrNull()?.coerceIn(10, 180)?.toString() ?: duration
            else {
                scope.launch {
                    if (rows.size < TimetableRepository.MAX_NODE_COUNT) {
                        val node = repository.addNode(table.id)
                        rows += NodeDraft(node.id, node.node, node.start, node.end)
                        showToast(context, "已添加第${node.node}节")
                    } else {
                        showToast(context, "最多支持 ${TimetableRepository.MAX_NODE_COUNT} 节")
                    }
                }
            }
            addDialog = false
        }, onDismiss = { addDialog = false })
    }
    if (uniformConfirm) {
        ConfirmDialog(title = "统一课时长度", text = "开启后，原来设置的下课时间会被覆盖。", onConfirm = { uniformConfirm = false; uniform = true }, onDismiss = { uniformConfirm = false })
    }
    deleting?.let { row ->
        ConfirmDialog(title = "删除第${row.node}节", text = "删除后后续节次会自动前移，课程安排也会同步调整。", onConfirm = {
            deleting = null
            scope.launch {
                repository.deleteNode(table.id, row.node)
                val refreshed = nodeTimesFor(table, repository.getNodeTimes(table.id))
                rows.clear()
                rows.addAll(refreshed.map { NodeDraft(it.id, it.node, it.start, it.end) })
                showToast(context, "删除成功")
            }
        }, onDismiss = { deleting = null })
    }
    timePickerTarget?.let { target ->
        val row = rows.getOrNull(target.index)
        if (row != null) {
            TimePickerDialogFor(
                value = if (target.isStart) row.start else row.end,
                onSelected = { value ->
                    rows[target.index] = if (target.isStart) {
                        row.copy(start = value, end = if (uniform) addMinutes(value, duration.toIntOrNull() ?: 50) else row.end)
                    } else {
                        row.copy(end = value)
                    }
                    timePickerTarget = null
                },
                onDismiss = { timePickerTarget = null },
            )
        } else {
            timePickerTarget = null
        }
    }
}

@Composable
private fun ImportScreen(
    repository: TimetableRepository,
    initialUri: Uri? = null,
    onInitialUriConsumed: () -> Unit = {},
    onBack: () -> Unit,
    onJsonImported: (Long) -> Unit,
    onCsvImported: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("CSV") }
    var tableName by remember { mutableStateOf("导入课表") }
    var startDate by remember { mutableStateOf(LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString()) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var notice by remember { mutableStateOf<ImportNotice?>(null) }
    var loading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingUri = it }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            pendingUri = initialUri
            onInitialUriConsumed()
        }
    }

    LaunchedEffect(pendingUri, type) {
        val uri = pendingUri ?: return@LaunchedEffect
        pendingUri = null
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("无法读取文件")
        }.getOrElse {
            notice = ImportNotice("导入失败", it.message ?: "读取文件失败") {}
            return@LaunchedEffect
        }
        loading = true
        scope.launch {
            if (type == "CSV") {
                val date = Weeks.parseDate(startDate)
                if (date == null) {
                    notice = ImportNotice("导入失败", "日期格式应为 yyyy-MM-dd") {}
                } else {
                    repository.importCsv(tableName.ifBlank { "导入课表" }, date.toEpochDay(), text)
                        .onSuccess { count ->
                            notice = ImportNotice("导入成功", "已导入 $count 条课程记录") { onCsvImported() }
                        }
                        .onFailure { error ->
                            notice = ImportNotice("导入失败", error.message ?: "CSV 导入失败") {}
                        }
                }
            } else {
                repository.importBackup(text)
                    .onSuccess { id ->
                        notice = ImportNotice("导入成功", "JSON 课表已导入") { onJsonImported(id) }
                    }
                    .onFailure { error ->
                        notice = ImportNotice("导入失败", error.message ?: "JSON 导入失败") {}
                    }
            }
            loading = false
        }
    }

    ScreenScaffold(title = "导入课表", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekTypeButton("CSV", type == "CSV", { type = "CSV" }, Modifier.weight(1f))
                    WeekTypeButton("JSON 备份", type == "JSON", { type = "JSON" }, Modifier.weight(1f))
                }
            }
            if (type == "CSV") {
                item {
                    SettingsGroup("WakeUp CSV") {
                        Text("列顺序：课程名称、星期、开始节数、结束节数、老师、地点、周数", modifier = Modifier.padding(16.dp), color = Color(0xFF626466), fontSize = 12.sp)
                        OutlinedTextField(value = tableName, onValueChange = { tableName = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), label = { Text("新课表名称") }, singleLine = true)
                        OutlinedTextField(value = startDate, onValueChange = { startDate = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), label = { Text("第一周的第一天") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                item {
                    SettingsGroup("JSON 备份") {
                        SettingRow(Icons.Default.Description, "导入本应用导出的单张课表备份文件", "导入后会创建一张新的课表", tint = Color(0xFF4E7BD9))
                    }
                }
            }
            item {
                Button(onClick = { launcher.launch(arrayOf("text/*", "application/json", "application/octet-stream")) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (loading) "正在导入…" else "选择文件")
                }
            }
        }
    }
    notice?.let { current ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text(current.title) },
            text = { Text(current.message) },
            confirmButton = {
                TextButton(onClick = {
                    notice = null
                    current.onConfirm()
                }) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun WidgetHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var pinMessage by remember { mutableStateOf<String?>(null) }
    val requestPin = { receiver: Class<*> ->
        val manager = AppWidgetManager.getInstance(context)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
            pinMessage = "请在桌面确认添加；系统会在添加时打开配置页。"
        } else {
            pinMessage = "当前桌面不支持从应用内添加，请从桌面小工具列表中选择 SleepDown。"
        }
    }

    ScreenScaffold(title = "桌面小部件", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item {
                    SettingsGroup("添加小部件") {
                        SettingRow(Icons.Default.Today, "今日课程（紧凑）", "按时间顺序显示今天的课程，适合较小的桌面区域。", tint = Color(0xFF4E7BD9), onClick = { requestPin(com.letr.sleepdown.widget.ScheduleWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                        SettingRow(Icons.Default.List, "今日课程", "按时间顺序显示今天的课程，并显示时间、地点和老师。", tint = Color(0xFF2AA69B), onClick = { requestPin(com.letr.sleepdown.widget.TodayCourseWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                        SettingRow(Icons.Default.Widgets, "今日课程（宽屏）", "在较宽的小部件中显示更多今日课程。", tint = Color(0xFF8D62C8), onClick = { requestPin(com.letr.sleepdown.widget.TodayModernWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                        SettingRow(Icons.Default.DateRange, "今日和明日", "同时显示今天与明天的课程。", tint = Color(0xFFFF8A00), onClick = { requestPin(com.letr.sleepdown.widget.TodayAndNextDayWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                        SettingRow(Icons.Default.TableView, "周课表", "按星期分栏显示当前周课程。", tint = Color(0xFFE53935), onClick = { requestPin(com.letr.sleepdown.widget.WeekScheduleWidgetReceiver::class.java) })
                    }
                }
                pinMessage?.let { message ->
                    item { Text(message, modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFF2AA69B), fontSize = 12.sp) }
                }
            }
            item {
                SettingsGroup("使用说明") {
                    SettingRow(Icons.Default.HelpOutline, "如何添加小部件？", "长按桌面空白处，选择小部件，找到 SleepDown 后添加。添加过程中会选择要显示的课表。", tint = Color(0xFF2AA69B))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Tune, "如何调整小部件大小？", "长按桌面上的小部件后拖动边缘调整大小，具体操作取决于桌面应用。", tint = Color(0xFF8D62C8))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "如何更换显示的课表？", "删除后重新添加小部件，并在配置页面选择另一张课表。", tint = Color(0xFFFF8A00))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Refresh, "小部件什么时候更新？", "修改课程、课表或时间表后会刷新已添加的小部件；系统也会定期检查，桌面应用可能有额外的刷新限制。", tint = Color(0xFF4E7BD9))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, onAction: () -> Unit, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color(0xFFFF2D55))
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF141414))
        Text(message, color = Color(0xFF626466), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("新建") }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("正在加载…", color = Color(0xFF626466)) }
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    number: Boolean = false,
    multiline: Boolean = false,
    clearable: Boolean = false,
    validation: (String) -> String? = { null },
    onConfirm: (String) -> Unit,
    onClear: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var value by remember(title, initial) { mutableStateOf(initial) }
    var validationError by remember(title, initial) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    validationError = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                isError = validationError != null,
                supportingText = validationError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                validation(value)?.let { validationError = it } ?: onConfirm(value)
            }) { Text("确定") }
        },
        dismissButton = {
            Row {
                if (clearable && onClear != null) TextButton(onClick = onClear) { Text("清除") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

@Composable
private fun ColorChoiceDialog(
    title: String,
    initial: Int,
    includeAuto: Boolean = false,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (includeAuto) {
                    Surface(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onSelect(0) }, color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD))) {
                        Box(contentAlignment = Alignment.Center) { Text("自", fontSize = 12.sp, color = Color(0xFF626466)) }
                    }
                }
                (listOf(Color.Black.toArgbCompat(), Color.White.toArgbCompat()) + CourseColors.all()).distinct().forEach { color ->
                    Surface(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onSelect(color) }, color = CourseColors.asColor(color), border = androidx.compose.foundation.BorderStroke(if (color == initial) 3.dp else 1.dp, if (color == initial) Color(0xFF141414) else Color(0xFFDDDDDD))) {}
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = onConfirm) { Text("确定") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(value: Long, onSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val initialMillis = LocalDate.ofEpochDay(value)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = { DatePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogFor(value: String, onSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val parsed = parseTime(value)
    val state = rememberTimePickerState(
        initialHour = parsed.hour,
        initialMinute = parsed.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onSelected("%02d:%02d".format(Locale.US, state.hour, state.minute))
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier, decimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(if (decimal) Regex("\\d{0,3}(\\.\\d{0,2})?") else Regex("\\d{0,3}"))) onValueChange(input)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun defaultTimeDraft(maxWeek: Int, day: Int, startNode: Int, step: Int = 2) = TimeDraft(day = day.coerceIn(1, 7), startNode = startNode.coerceAtLeast(1), step = step.coerceAtLeast(1), selectedWeeks = (1..maxWeek.coerceAtLeast(1)).toSet())

private fun selectedWeeks(time: CourseTimeEntity, maxWeek: Int): Set<Int> = (time.startWeek..time.endWeek).filter { week -> Weeks.inWeek(time.startWeek, time.endWeek, time.weekType, week) && week in 1..maxWeek }.toSet()

private fun TimeDraft.toEntities(courseId: Long): List<CourseTimeEntity> {
    val weeks = selectedWeeks.filter { it > 0 }.sorted()
    if (weeks.isEmpty()) return emptyList()
    val groups = mutableListOf<Pair<Int, Int>>()
    var start = weeks.first()
    var previous = start
    val stride = if (weekType == CourseTimeEntity.TYPE_ALL) 1 else 2
    for (week in weeks.drop(1)) {
        if (week != previous + stride) {
            groups += start to previous
            start = week
        }
        previous = week
    }
    groups += start to previous
    return groups.mapIndexed { index, (from, to) ->
        CourseTimeEntity(id = if (index == 0) id else 0L, courseId = courseId, day = day, startNode = startNode, step = step, startWeek = from, endWeek = to, weekType = weekType, room = room, ownTime = ownTime, startTime = startTime, endTime = endTime)
    }
}

private fun timeSummary(draft: TimeDraft, nodeTimes: List<NodeTimeEntity>): String {
    val day = "周${weekdayName(draft.day)}"
    val clock = if (draft.ownTime && draft.startTime.isNotBlank() && draft.endTime.isNotBlank()) "${draft.startTime}-${draft.endTime}" else {
        val start = nodeTimes.firstOrNull { it.node == draft.startNode }?.start.orEmpty()
        val end = nodeTimes.firstOrNull { it.node == draft.startNode + draft.step - 1 }?.end.orEmpty()
        if (start.isNotBlank() && end.isNotBlank()) "$start-$end" else ""
    }
    return "$day    第${draft.startNode} - ${draft.startNode + draft.step - 1}节${if (clock.isBlank()) "" else " · $clock"}"
}

private fun selectedWeekLabel(selected: Set<Int>, maxWeek: Int): String {
    if (selected.isEmpty()) return "未选择"
    val values = selected.filter { it in 1..maxWeek }.sorted()
    if (values.isEmpty()) return "未选择"
    if (values.size == maxWeek && values.first() == 1 && values.last() == maxWeek) return "第1 - ${maxWeek}周"
    val contiguous = values.zipWithNext().all { (left, right) -> right == left + 1 }
    return if (contiguous) {
        "第${values.first()} - ${values.last()}周"
    } else if (values.size <= 4) {
        values.joinToString("、") { "第${it}周" }
    } else {
        "已选 ${values.size} 周"
    }
}

private fun courseTimeLabel(time: CourseTimeEntity, nodeTimes: List<NodeTimeEntity>): String {
    val start = nodeTimes.firstOrNull { it.node == time.startNode }?.start.orEmpty()
    val end = nodeTimes.firstOrNull { it.node == time.startNode + time.step - 1 }?.end.orEmpty()
    return "第${time.startNode}-${time.startNode + time.step - 1}节" + if (start.isNotBlank() && end.isNotBlank()) " $start-$end" else ""
}

private fun courseColor(course: CourseEntity): Int = if (course.color == 0) CourseColors.colorFor(course.name) else course.color

private fun currentWeek(table: TableEntity): Int {
    val days = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(table.startDate), LocalDate.now())
    return Math.floorDiv(days, 7L).toInt() + 1
}

private fun dateFor(table: TableEntity, week: Int, day: Int): LocalDate {
    val base = LocalDate.ofEpochDay(table.startDate)
    val offset = if (table.sundayFirst) {
        if (day == 7) 0 else day
    } else {
        day - 1
    }
    return base.plusWeeks((week - 1).toLong()).plusDays(offset.toLong())
}

private fun nodeTimesFor(table: TableEntity, source: List<NodeTimeEntity>): List<NodeTimeEntity> {
    val byNode = source.associateBy { it.node }
    return (1..table.nodeCount.coerceIn(1, 60)).map { node ->
        byNode[node] ?: NodeTimeEntity(tableId = table.id, node = node, start = "", end = "")
    }
}

private fun visibleDays(table: TableEntity): List<Int> {
    val ordered = if (table.sundayFirst) listOf(7, 1, 2, 3, 4, 5, 6) else listOf(1, 2, 3, 4, 5, 6, 7)
    return ordered.filter { day -> when (day) { 6 -> table.showSat; 7 -> table.showSun; else -> true } }
}

private fun weekdayName(day: Int): String = listOf("一", "二", "三", "四", "五", "六", "日")[((day - 1) % 7 + 7) % 7]

private fun blend(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha + background.alpha * (1f - foreground.alpha)
    if (alpha <= 0f) return Color.Transparent
    return Color(
        red = (foreground.red * foreground.alpha + background.red * background.alpha * (1f - foreground.alpha)) / alpha,
        green = (foreground.green * foreground.alpha + background.green * background.alpha * (1f - foreground.alpha)) / alpha,
        blue = (foreground.blue * foreground.alpha + background.blue * background.alpha * (1f - foreground.alpha)) / alpha,
        alpha = alpha,
    )
}

private fun parseColor(value: String, fallback: Color): Color {
    val raw = value.removePrefix("#")
    val normalized = when (raw.length) { 6 -> "FF$raw"; 8 -> raw; else -> return fallback }
    return runCatching { Color(normalized.toLong(16).toInt()) }.getOrElse { fallback }
}

private fun colorName(argb: Int): String = "#${argb.toUInt().toString(16).padStart(8, '0').uppercase(Locale.US)}"

private fun parseTime(value: String): LocalTime = runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm")) }.getOrDefault(LocalTime.of(0, 0))

private fun addMinutes(value: String, minutes: Int): String = parseTime(value).plusMinutes(minutes.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))

private fun effectiveEnd(start: String, end: String, uniform: Boolean, duration: String): String = if (uniform && start.isNotBlank()) addMinutes(start, duration.toIntOrNull() ?: 50) else end

private fun Color.toArgbCompat(): Int {
    val red = (red * 255f).toInt().coerceIn(0, 255)
    val green = (green * 255f).toInt().coerceIn(0, 255)
    val blue = (blue * 255f).toInt().coerceIn(0, 255)
    val alpha = (alpha * 255f).toInt().coerceIn(0, 255)
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
