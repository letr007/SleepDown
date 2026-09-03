package com.letr.sleepdown.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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

private enum class PickerTarget {
    DAY,
    START_NODE,
    STEP,
}

@Composable
fun TimetableApp(repository: TimetableRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tables by repository.observeTables().collectAsStateWithLifecycle(initialValue = emptyList())
    var currentTableId by remember { mutableStateOf(AppContainer.currentTableId(context)) }
    var screen by remember { mutableStateOf(AppScreen.WEEK) }
    var editingTableId by remember { mutableStateOf<Long?>(null) }
    var tableDraft by remember { mutableStateOf<TableEntity?>(null) }
    var editingCourseId by remember { mutableStateOf<Long?>(null) }
    var editorDay by remember { mutableIntStateOf(1) }
    var editorStartNode by remember { mutableIntStateOf(1) }
    var editorStep by remember { mutableIntStateOf(1) }
    var selectLatestAfterImport by remember { mutableStateOf(false) }
    var importTableCountBefore by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        val id = repository.ensureDefaultTable()
        if (currentTableId <= 0L) {
            currentTableId = id
            AppContainer.setCurrentTableId(context, id)
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

    BackHandler(enabled = screen != AppScreen.WEEK) {
        screen = when (screen) {
            AppScreen.TABLE_MANAGE -> AppScreen.WEEK
            AppScreen.TABLE_SETTINGS -> AppScreen.TABLE_MANAGE
            AppScreen.APPEARANCE -> AppScreen.TABLE_SETTINGS
            AppScreen.COURSE_MANAGE -> AppScreen.TABLE_MANAGE
            AppScreen.COURSE_EDITOR -> AppScreen.COURSE_MANAGE
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

    fun openAddCourse(day: Int = 1, startNode: Int = 1, step: Int = 1) {
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
                                scope.launch { repository.deleteCourse(course) }
                            },
                            onCopyCourse = { course ->
                                scope.launch {
                                    val copy = course.copy(id = 0L, name = "${course.name}（副本）")
                                    val times = repository.getCourseTimes(course.id).map { it.copy(id = 0L, courseId = 0L) }
                                    repository.saveCourse(table.id, copy, times)
                                }
                            },
                            onMoveCourse = { course, day, startNode ->
                                scope.launch {
                                    val times = repository.getCourseTimes(course.id).sortedBy { it.id }
                                    val first = times.firstOrNull() ?: return@launch
                                    val maxStart = (table.nodeCount - first.step + 1).coerceAtLeast(1)
                                    val updated = times.toMutableList()
                                    updated[0] = first.copy(
                                        day = day,
                                        startNode = startNode.coerceIn(1, maxStart),
                                    )
                                    repository.saveCourse(table.id, course, updated)
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
                    scope.launch { selectTable(repository.copyTable(source.id)) }
                },
                onDelete = { tableToDelete ->
                    scope.launch {
                        repository.deleteTable(tableToDelete)
                        if (tableToDelete.id == currentTableId) currentTableId = 0L
                    }
                },
                onNew = { tableDraft = null; editingTableId = null; screen = AppScreen.TABLE_SETTINGS },
            )
            AppScreen.TABLE_SETTINGS -> {
                val editingTable = editingTableId?.let { id -> tables.firstOrNull { it.id == id } }
                TableSettingsScreen(
                    table = tableDraft ?: editingTable,
                    onBack = { screen = AppScreen.TABLE_MANAGE },
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
                        onBack = { screen = AppScreen.TABLE_SETTINGS },
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
                        onBack = { screen = AppScreen.TABLE_MANAGE },
                        onAdd = { openAddCourse() },
                        onEdit = { course ->
                            editingCourseId = course.id
                            editorDay = 1
                            editorStartNode = 1
                            editorStep = 1
                            screen = AppScreen.COURSE_EDITOR
                        },
                        onDelete = { course -> scope.launch { repository.deleteCourse(course) } },
                        onClear = { courses -> scope.launch { courses.forEach { repository.deleteCourse(it) } } },
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
                    onBack = { screen = AppScreen.COURSE_MANAGE },
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
    onMoveCourse: (CourseEntity, Int, Int) -> Unit,
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
                onAdd = { onAddCourse(1, 1, 1) },
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
    onMoveCourse: (CourseEntity, Int, Int) -> Unit,
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
    onMoveCourse: (CourseEntity, Int, Int) -> Unit,
    onOpenCourse: (CourseEntity) -> Unit,
) {
    val rowHeight = table.itemHeightDp.coerceIn(32, 128).dp + 2.dp
    val nodeCount = table.nodeCount.coerceIn(1, 60)
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
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(0.64f).verticalScroll(scrollState)) {
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
        BoxWithConstraints(modifier = Modifier.weight(days.size.toFloat()).padding(end = if (days.size == 7) 4.dp else 8.dp).verticalScroll(scrollState)) {
            val columnWidth = if (days.isEmpty()) 0.dp else maxWidth / days.size
            Column(modifier = Modifier.height(rowHeight * nodeCount.toFloat())) {
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
                                            var moved = false
                                            var dragging = false
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) break
                                                totalDragX += change.position.x - change.previousPosition.x
                                                totalDragY += change.position.y - change.previousPosition.y
                                                if (!dragging && (abs(totalDragX) > viewConfiguration.touchSlop || abs(totalDragY) > viewConfiguration.touchSlop)) {
                                                    moved = true
                                                    if (abs(totalDragY) >= abs(totalDragX)) {
                                                        dragging = true
                                                        selection = CellSelection(day, node, node)
                                                    }
                                                }
                                                if (dragging && cellCourse == null) {
                                                    change.consume()
                                                    val endNode = (node + (totalDragY / rowHeightPx).roundToInt())
                                                        .coerceIn(1, nodeCount)
                                                    selection = CellSelection(
                                                        day = day,
                                                        startNode = minOf(node, endNode),
                                                        endNode = maxOf(node, endNode),
                                                    )
                                                }
                                            }
                                            if (!dragging && !moved && cellCourse == null) {
                                                val existing = selection
                                                if (existing != null && existing.day == day && node in existing.startNode..existing.endNode) {
                                                    selection = null
                                                    onAddCourse(existing.day, existing.startNode, existing.endNode - existing.startNode + 1)
                                                } else {
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
                    Box(
                        modifier = Modifier
                            .offset(
                                x = columnWidth * selectedColumn.toFloat() + 1.dp,
                                y = rowHeight * (selected.startNode - 1).toFloat() + 1.dp,
                            )
                            .width((columnWidth - 2.dp).coerceAtLeast(1.dp))
                            .height(rowHeight * (selected.endNode - selected.startNode + 1).toFloat() - 2.dp)
                            .background(Color(0xFFFF6272).copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .pointerInput(selected, rowHeightPx) {
                                var dragRows = 0f
                                detectDragGestures(
                                    onDragStart = { dragRows = 0f },
                                    onDragEnd = { dragRows = 0f },
                                    onDragCancel = { dragRows = 0f },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragRows += amount.y
                                        val delta = (dragRows / rowHeightPx).roundToInt()
                                        if (delta != 0) {
                                            val end = (selected.endNode + delta).coerceIn(selected.startNode, nodeCount)
                                            selection = selected.copy(endNode = end)
                                            dragRows -= delta * rowHeightPx
                                        }
                                    },
                                )
                            }
                            .clickable {
                                selection = null
                                onAddCourse(selected.day, selected.startNode, selected.endNode - selected.startNode + 1)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加课程", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
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
                    onClick = { onOpenCourse(item.course) },
                    onMove = { day, startNode -> onMoveCourse(item.course, day, startNode) },
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
                    onClick = { onOpenCourse(item.course) },
                    onMove = null,
                )
            }
        }
    }
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
    val density = LocalDensity.current
    val columnWidthPx = with(density) { columnWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }

    Box(
        modifier = Modifier
            .offset(x = columnWidth * column.toFloat() + 1.dp, y = rowHeight * (time.startNode - 1).toFloat() + 1.dp)
            .graphicsLayer {
                scaleX = if (dragging) 1.1f else 1f
                scaleY = if (dragging) 1.1f else 1f
                translationX = dragX
                translationY = dragY
            }
            .zIndex(if (dragging) 1f else 0f)
            .then(if (onMove == null) Modifier else Modifier.pointerInput(time.id, columnWidthPx, rowHeightPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                        dragging = true
                    },
                    onDragEnd = {
                        val targetColumn = (column + (dragX / columnWidthPx).roundToInt()).coerceIn(0, visibleDays.lastIndex)
                        val targetNode = (time.startNode + (dragY / rowHeightPx).roundToInt()).coerceIn(1, table.nodeCount)
                        onMove(visibleDays[targetColumn], targetNode)
                        dragX = 0f
                        dragY = 0f
                        dragging = false
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                        dragging = false
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                        dragY += amount.y
                    },
                )
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
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFFF7F7F7),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color(0xFF141414), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color(0xFF141414)) } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(content = content)
        }
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
    var message by remember { mutableStateOf<String?>(null) }
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
                        Text("点击卡片查看该课表的课程\n长按拖动排序", modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFF626466), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    items(displayTables, key = { it.id }) { table ->
                        TableCard(
                            table = table,
                            selected = table.id == currentTableId,
                            onClick = { onCourses(table.id) },
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
            message?.let { text ->
                Text(text, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), color = Color(0xFFFF2D55), fontSize = 13.sp)
            }
        }
    }
    deleting?.let { table ->
        if (table.id == currentTableId) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("无法删除") },
                text = { Text("当前显示的课表无法删除，请回主界面切换到其他课表后再删除") },
                confirmButton = { TextButton(onClick = { deleting = null }) { Text("知道了") } },
            )
        } else {
            ConfirmDialog(
                title = "删除课表",
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
    var nameDialog by remember { mutableStateOf(false) }
    var numberDialog by remember { mutableStateOf<String?>(null) }
    var dateDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                if (working.name.isBlank()) error = "名称不能为空哦>_<" else onSave(working)
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
            item { error?.let { Text(it, modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFFD32F2F), fontSize = 13.sp) } }
        }
    }

    if (nameDialog) {
        TextInputDialog("课表名称", working.name, onConfirm = { value -> working = working.copy(name = value); nameDialog = false }, onDismiss = { nameDialog = false })
    }
    numberDialog?.let { field ->
        val initial = if (field == "nodeCount") working.nodeCount.toString() else working.maxWeek.toString()
        TextInputDialog(if (field == "nodeCount") "一天课程节数" else "学期周数", initial, number = true, onConfirm = { value ->
            val number = value.toIntOrNull()
            if (number != null) {
                working = if (field == "nodeCount") working.copy(nodeCount = number.coerceIn(1, 60)) else working.copy(maxWeek = number.coerceIn(1, 60))
            }
            numberDialog = null
        }, onDismiss = { numberDialog = null })
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
            onBack()
        },
        actions = { IconButton(onClick = { onSave(working) }) { Icon(Icons.Default.Save, contentDescription = "保存") } },
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
        ConfirmDialog(title = "清空课程", text = "真的要清空课表吗？这将无法恢复。", onConfirm = { clearDialog = false; onClear(courses) }, onDismiss = { clearDialog = false })
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
    ScreenScaffold(
        title = if (courseId == null) "添加课程" else "编辑课程",
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                when {
                    name.isBlank() -> error = "请填写课程名称"
                    drafts.isEmpty() -> error = "请至少添加一个时间段"
                    drafts.any { it.selectedWeeks.isEmpty() } -> error = "请至少选择一周"
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
                        val times = drafts.flatMap { it.toEntities(entity.id) }
                        repository.saveCourse(table.id, entity, times)
                        onSaved()
                    }
                }
            }) { Icon(Icons.Default.Save, contentDescription = "保存") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SettingsGroup("课程信息") {
                    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), label = { Text("课程名称") }, singleLine = true)
                    if (existingCourses.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            existingCourses.distinctBy { it.name }.take(8).forEach { candidate ->
                                OutlinedButton(onClick = { name = candidate.name }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(candidate.name, maxLines = 1) }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "颜色", if (color == 0) "自动分配" else colorName(color), tint = Color(0xFF2AA69B), onClick = { colorDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Star, "学分", credit.ifBlank { "可不填" }, tint = Color(0xFFFF8A00), onClick = { inputTarget = "credit" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.StickyNote2, "备注", note.ifBlank { "可不填" }, tint = Color(0xFFFFC107), onClick = { inputTarget = "note" })
                }
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
                    teacher = teacher,
                    onCustomTimeInfo = { customTimeInfo = true },
                )
            }
            item {
                OutlinedButton(onClick = { drafts += defaultTimeDraft(table.maxWeek, 1, 1) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("添加时间段")
                }
            }
            item { error?.let { Text(it, modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFFD32F2F), fontSize = 13.sp) } }
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
    if (colorDialog) {
        ColorChoiceDialog(title = "课程颜色", initial = color, includeAuto = true, onSelect = { color = it; colorDialog = false }, onDismiss = { colorDialog = false })
    }
    if (customTimeInfo) {
        AlertDialog(
            onDismissRequest = { customTimeInfo = false },
            title = { Text("自定义时间") },
            text = { Text("统一调整课程时间应回到主界面右上角菜单中的上课时间设置。自定义时间会按具体时间决定显示位置。") },
            confirmButton = { TextButton(onClick = { customTimeInfo = false }) { Text("知道了") } },
        )
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
    teacher: String,
    onCustomTimeInfo: () -> Unit,
) {
    val context = LocalContext.current
    var pickerTarget by remember(draft.id, index) { mutableStateOf<PickerTarget?>(null) }
    SettingsGroup("时间段 ${index + 1}") {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("上课安排", modifier = Modifier.weight(1f), color = Color(0xFF141414), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onRemove, enabled = true) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(3.dp)); Text("删除") }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
        SettingRow(Icons.Default.CalendarMonth, "周数", selectedWeekLabel(draft.selectedWeeks, table.maxWeek), tint = Color(0xFF2AA69B))
        WeekNumberGrid(maxWeek = table.maxWeek, selected = draft.selectedWeeks, onChange = { onChange(draft.copy(selectedWeeks = it)) })
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekTypeButton("每周", draft.weekType == CourseTimeEntity.TYPE_ALL, { onChange(draft.copy(weekType = CourseTimeEntity.TYPE_ALL, selectedWeeks = (1..table.maxWeek).toSet())) }, Modifier.weight(1f))
            WeekTypeButton("单周", draft.weekType == CourseTimeEntity.TYPE_ODD, { onChange(draft.copy(weekType = CourseTimeEntity.TYPE_ODD, selectedWeeks = (1..table.maxWeek).filter { it % 2 == 1 }.toSet())) }, Modifier.weight(1f))
            WeekTypeButton("双周", draft.weekType == CourseTimeEntity.TYPE_EVEN, { onChange(draft.copy(weekType = CourseTimeEntity.TYPE_EVEN, selectedWeeks = (1..table.maxWeek).filter { it % 2 == 0 }.toSet())) }, Modifier.weight(1f))
        }
        SettingRow(
            Icons.Default.CalendarMonth,
            "星期",
            "周${weekdayName(draft.day)}",
            tint = Color(0xFF4E7BD9),
            onClick = { pickerTarget = PickerTarget.DAY },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
        SettingRow(
            Icons.Default.List,
            "开始节",
            "第${draft.startNode}节",
            tint = Color(0xFF4E7BD9),
            onClick = { pickerTarget = PickerTarget.START_NODE },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
        SettingRow(
            Icons.Default.Tune,
            "连续节数",
            "${draft.step}节",
            tint = Color(0xFF4E7BD9),
            onClick = { pickerTarget = PickerTarget.STEP },
        )
        SettingRow(Icons.Default.AccessTime, "时间", timeSummary(draft, nodeTimes), tint = Color(0xFF4E7BD9))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = draft.ownTime, onCheckedChange = {
                if (it) onCustomTimeInfo()
                onChange(draft.copy(ownTime = it))
            })
            Spacer(Modifier.width(8.dp))
            Text("自定义时间", color = Color(0xFF141414), modifier = Modifier.weight(1f))
        }
        if (draft.ownTime) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 24.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showTimePicker(context, draft.startTime, { onChange(draft.copy(startTime = it)) }) }, modifier = Modifier.weight(1f)) { Text(draft.startTime.ifBlank { "上课时间" }) }
                OutlinedButton(onClick = { showTimePicker(context, draft.endTime, { onChange(draft.copy(endTime = it)) }) }, modifier = Modifier.weight(1f)) { Text(draft.endTime.ifBlank { "下课时间" }) }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
        SettingRow(Icons.Default.Person, "授课老师", teacher.ifBlank { "可不填" }, tint = Color(0xFF4E7BD9), onClick = onTeacher)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
        SettingRow(Icons.Default.Place, "上课地点", draft.room.ifBlank { "可不填" }, tint = Color(0xFFE53935), onClick = { /* room is edited below */ })
        OutlinedTextField(value = draft.room, onValueChange = { onChange(draft.copy(room = it)) }, modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 24.dp, bottom = 12.dp), label = { Text("上课地点（可不填）") }, singleLine = true)
    }

    pickerTarget?.let { target ->
        val maxNode = table.nodeCount.coerceIn(1, 60)
        val options = when (target) {
            PickerTarget.DAY -> (1..7).map { it to "周${weekdayName(it)}" }
            PickerTarget.START_NODE -> (1..maxNode).map { it to "第${it}节" }
            PickerTarget.STEP -> (1..(maxNode - draft.startNode + 1).coerceAtLeast(1)).map { it to "${it}节" }
        }
        val selected = when (target) {
            PickerTarget.DAY -> draft.day
            PickerTarget.START_NODE -> draft.startNode
            PickerTarget.STEP -> draft.step.coerceIn(1, options.size)
        }
        ChoicePickerDialog(
            title = when (target) {
                PickerTarget.DAY -> "选择星期"
                PickerTarget.START_NODE -> "选择开始节"
                PickerTarget.STEP -> "选择连续节数"
            },
            options = options,
            selectedValue = selected,
            onSelect = { value ->
                onChange(
                    when (target) {
                        PickerTarget.DAY -> draft.copy(day = value)
                        PickerTarget.START_NODE -> draft.copy(
                            startNode = value,
                            step = draft.step.coerceAtMost((maxNode - value + 1).coerceAtLeast(1)),
                        )
                        PickerTarget.STEP -> draft.copy(step = value)
                    },
                )
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows = remember(table.id) { mutableStateListOf<NodeDraft>() }
    var loaded by remember(table.id) { mutableStateOf(false) }
    var uniform by remember(table.id) { mutableStateOf(false) }
    var duration by remember(table.id) { mutableStateOf("50") }
    var addDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NodeDraft?>(null) }
    var uniformConfirm by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

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
                            saved = true
                        }
                    })
                }
            }
            IconButton(onClick = {
                scope.launch {
                    repository.saveNodeTimes(table.id, rows.map { NodeTimeEntity(it.id, table.id, it.node, it.start, effectiveEnd(it.start, it.end, uniform, duration)) })
                    saved = true
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
                        OutlinedButton(onClick = { showTimePicker(context, row.start) { value -> rows[index] = row.copy(start = value, end = if (uniform) addMinutes(value, duration.toIntOrNull() ?: 50) else row.end); saved = false } }, modifier = Modifier.weight(1f)) { Text(row.start) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showTimePicker(context, row.end) { value -> rows[index] = row.copy(end = value); saved = false } }, modifier = Modifier.weight(1f)) { Text(row.end) }
                        IconButton(onClick = { if (rows.size > 1) deleting = row }) { Icon(Icons.Default.Delete, contentDescription = "删除第${row.node}节", tint = if (rows.size > 1) Color(0xFFE53935) else Color.LightGray) }
                    }
                }
            }
            item { if (saved) Text("已保存", modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFF2AA69B), fontSize = 13.sp) }
        }
    }
    if (addDialog) {
        TextInputDialog("添加节次 / 统一课时长度", if (uniform) duration else "1", number = true, onConfirm = { value ->
            if (uniform) duration = value.toIntOrNull()?.coerceIn(10, 180)?.toString() ?: duration
            else {
                scope.launch {
                    if (rows.size < TimetableRepository.MAX_NODE_COUNT) {
                        val node = repository.addNode(table.id)
                        rows += NodeDraft(node.id, node.node, node.start, node.end)
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
            }
        }, onDismiss = { deleting = null })
    }
}

@Composable
private fun ImportScreen(
    repository: TimetableRepository,
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
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingUri = it }

    LaunchedEffect(pendingUri, type) {
        val uri = pendingUri ?: return@LaunchedEffect
        pendingUri = null
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("无法读取文件")
        }.getOrElse {
            message = it.message ?: "读取文件失败"
            return@LaunchedEffect
        }
        loading = true
        scope.launch {
            if (type == "CSV") {
                val date = Weeks.parseDate(startDate)
                if (date == null) {
                    message = "日期格式应为 yyyy-MM-dd"
                } else {
                    repository.importCsv(tableName.ifBlank { "导入课表" }, date.toEpochDay(), text)
                        .onSuccess { count -> message = "已导入 $count 条课程记录"; onCsvImported() }
                        .onFailure { message = it.message ?: "CSV 导入失败" }
                }
            } else {
                repository.importBackup(text)
                    .onSuccess { id -> message = "JSON 导入成功"; onJsonImported(id) }
                    .onFailure { message = it.message ?: "JSON 导入失败" }
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
            item { message?.let { Text(it, color = if (it.contains("失败") || it.contains("格式")) Color(0xFFD32F2F) else Color(0xFF2AA69B), fontSize = 13.sp) } }
        }
    }
}

@Composable
private fun WidgetHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenScaffold(title = "桌面小部件", onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item {
                    SettingsGroup("添加小部件") {
                        SettingRow(Icons.Default.Widgets, "将课表添加到桌面", "有日视图和周视图可选哦，能否添加成功取决于系统，如果添加不了可以看下方的教程。添加成功后，可以左右滑动桌面看看系统把课表放到哪一页了。", tint = Color(0xFF4E7BD9), onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            val provider = ComponentName(context, com.letr.sleepdown.widget.ScheduleWidgetReceiver::class.java)
                            if (manager.isRequestPinAppWidgetSupported) {
                                manager.requestPinAppWidget(provider, null, null)
                            }
                        })
                    }
                }
            }
            item {
                SettingsGroup("使用说明") {
                    SettingRow(Icons.Default.HelpOutline, "如何添加小部件？", "长按桌面空白处，或者在桌面做双指捏合手势，选择桌面小工具，肯定是有的，仔细找找，实在找不到就重启手机再找。请允许应用后台自启和后台运行。", tint = Color(0xFF2AA69B))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Tune, "如何调整小部件大小？", "桌面长按小部件调整。MIUI 长按后晃动小部件可以调整小部件大小；华为/荣耀设备可能因第三方主题无法调整。", tint = Color(0xFF8D62C8))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Palette, "如何调整小部件样式？", "小部件右上角有个「调整」的按钮，点它就可以了。", tint = Color(0xFFFF8A00))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.Refresh, "小部件刷新不及时/显示正在加载", "请允许后台自启和后台运行。华为/荣耀路径：手机管家 -> 应用启动管理 -> WakeUp课程表 -> 手动管理。小部件右上角小箭头点击两次可强制刷新。", tint = Color(0xFF4E7BD9))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8E8E8))
                    SettingRow(Icons.Default.HelpOutline, "更多问题", "根据反馈不定时更新", tint = Color(0xFF626466), onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wakeup.fun/doc/faqs.html"))) })
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
    onConfirm: (String) -> Unit,
    onClear: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var value by remember(title, initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("确定") } },
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

@Composable
private fun DatePickerDialogFor(value: Long, onSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(value) {
        val date = LocalDate.ofEpochDay(value)
        DatePickerDialog(context, { _, year, month, day -> onSelected(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth).apply { setOnCancelListener { onDismiss() }; show() }
    }
}

private fun showTimePicker(context: Context, value: String, onSelected: (String) -> Unit) {
    val parsed = parseTime(value)
    TimePickerDialog(context, { _, hour, minute -> onSelected("%02d:%02d".format(Locale.US, hour, minute)) }, parsed.hour, parsed.minute, true).show()
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

private fun defaultTimeDraft(maxWeek: Int, day: Int, startNode: Int, step: Int = 1) = TimeDraft(day = day.coerceIn(1, 7), startNode = startNode.coerceAtLeast(1), step = step.coerceAtLeast(1), selectedWeeks = (1..maxWeek.coerceAtLeast(1)).toSet())

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
    if (selected.size == maxWeek) return "全周"
    val values = selected.sorted()
    return if (values.size <= 4) values.joinToString("、") { "第${it}周" } else "已选 ${values.size} 周"
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
