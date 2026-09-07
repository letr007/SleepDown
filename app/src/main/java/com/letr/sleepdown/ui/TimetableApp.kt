package com.letr.sleepdown.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.pointerInput
import com.letr.sleepdown.R
import androidx.core.os.LocaleListCompat
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.res.stringResource
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
import com.letr.sleepdown.data.ImportExportAdapter
import com.letr.sleepdown.data.ImportFormat
import com.letr.sleepdown.data.ImportOptions
import com.letr.sleepdown.data.ImportTarget
import com.letr.sleepdown.data.NodeTimeEntity
import com.letr.sleepdown.data.TableEntity
import com.letr.sleepdown.data.TableWithMeta
import com.letr.sleepdown.data.TimetableRepository
import com.letr.sleepdown.logic.CourseColors
import com.letr.sleepdown.domain.ReminderSettings
import com.letr.sleepdown.logic.Weeks
import com.letr.sleepdown.reminder.ReminderScheduler
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

@Composable
fun TimetableTheme(content: @Composable () -> Unit) {
    SleepDownAppTheme(content)
}

private enum class AppScreen {
    WEEK,
    TABLE_MANAGE,
    TABLE_SETTINGS,
    APPEARANCE,
    DISPLAY_SETTINGS,
    COURSE_MANAGE,
    COURSE_EDITOR,
    TIME_TABLE,
    IMPORT,
    REMINDERS,
    WIDGET_HELP,
}

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

private enum class CourseHoldResult {
    TAP,
    MOVED,
}

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
    onTableRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var externalImportUri by remember { mutableStateOf(initialImportUri) }
    val tables by repository.observeTables().collectAsStateWithLifecycle(initialValue = emptyList())
    var currentTableId by rememberSaveable { mutableStateOf(AppContainer.currentTableId(context)) }
    var screenStack by rememberSaveable { mutableStateOf(arrayListOf(AppScreen.WEEK.name)) }
    val screen = AppScreen.valueOf(screenStack.last())
    val screenState = rememberSaveableStateHolder()
    fun navigate(destination: AppScreen) {
        if (screen != destination) screenStack = ArrayList(screenStack + destination.name)
    }
    fun goBack() {
        if (screenStack.size > 1) screenStack = ArrayList(screenStack.dropLast(1))
    }
    fun goHome() {
        screenStack = arrayListOf(AppScreen.WEEK.name)
    }
    var editingTableId by rememberSaveable { mutableStateOf<Long?>(null) }
    var tableDraft by remember { mutableStateOf<TableEntity?>(null) }
    var editingCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorDay by rememberSaveable { mutableIntStateOf(1) }
    var editorStartNode by rememberSaveable { mutableIntStateOf(1) }
    var editorStep by rememberSaveable { mutableIntStateOf(2) }
    var exportDialog by remember { mutableStateOf(false) }
    var exportNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val id = repository.ensureDefaultTable(context.getString(R.string.default_table_name))
        if (currentTableId <= 0L) {
            currentTableId = id
            AppContainer.setCurrentTableId(context, id)
        }
    }
    fun openRequestedTable() {
        val id = initialTableId ?: return
        if (tables.any { it.id == id }) {
            currentTableId = id
            AppContainer.setCurrentTableId(context, id)
            tableDraft = null
            goHome()
        }
        onTableRequestConsumed()
    }
    LaunchedEffect(initialTableId, tables) {
        if (initialTableId != null && tables.isNotEmpty()) {
            if (tables.none { it.id == initialTableId }) onTableRequestConsumed()
            else if (screen != AppScreen.COURSE_EDITOR) openRequestedTable()
        }
    }
    LaunchedEffect(tables, currentTableId) {
        if (tables.isNotEmpty() && tables.none { it.id == currentTableId }) {
            val id = tables.first().id
            currentTableId = id
            AppContainer.setCurrentTableId(context, id)
        }
    }

    val tableId = currentTableId.takeIf { it > 0L } ?: tables.firstOrNull()?.id ?: 0L
    val table = tables.firstOrNull { it.id == tableId }
    val tableMeta by remember(repository, tableId) {
        repository.observeTableWithMeta(tableId)
    }.collectAsStateWithLifecycle(initialValue = null)
    val importExportAdapter = remember(repository, context) { ImportExportAdapter(context, repository) }
    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null && tableId > 0L) {
            scope.launch {
                runCatching { importExportAdapter.exportFile(uri, ImportFormat.JSON, tableId) }
                    .onSuccess { exportNotice = context.getString(R.string.export_json_saved) }
                    .onFailure { exportNotice = localizedUiError(context, it, R.string.export_failed) }
            }
        }
    }
    val icsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri ->
        if (uri != null && tableId > 0L) {
            scope.launch {
                runCatching { importExportAdapter.exportFile(uri, ImportFormat.ICS, tableId) }
                    .onSuccess { exportNotice = context.getString(R.string.export_ics_saved) }
                    .onFailure { exportNotice = localizedUiError(context, it, R.string.export_failed) }
            }
        }
    }

    LaunchedEffect(initialImportUri) {
        if (initialImportUri != null) {
            externalImportUri = initialImportUri
            navigate(AppScreen.IMPORT)
        }
    }
    LaunchedEffect(tables, tableMeta) {
        if (tables.isNotEmpty() && tableMeta != null) {
            refreshScheduleWidgets(context)
            ReminderScheduler(context).rebuildCurrentTable()
        }
    }

    BackHandler(enabled = screen != AppScreen.WEEK && screen != AppScreen.COURSE_EDITOR) {
        if (screen == AppScreen.TABLE_MANAGE) tableDraft = null
        goBack()
    }

    fun selectTable(id: Long) {
        if (tableDraft?.id != id) tableDraft = null
        currentTableId = id
        AppContainer.setCurrentTableId(context, id)
    }

    fun openAddCourse(day: Int = 1, startNode: Int = 1, step: Int = 2) {
        editingCourseId = null
        editorDay = day
        editorStartNode = startNode
        editorStep = step
        navigate(AppScreen.COURSE_EDITOR)
    }

    fun saveTable(tableToSave: TableEntity) {
        tableDraft = null
        scope.launch {
            val id = repository.saveTable(tableToSave)
            if (tableToSave.id == 0L) {
                repository.saveNodeTimes(TimetableRepository.defaultNodeTimes(id))
            }
            selectTable(id)
            showToast(context, context.getString(R.string.save_success))
            goBack()
        }
    }

    val updateTable: (TableEntity) -> Unit = { updated ->
        tableDraft = updated
        if (updated.id > 0L) scope.launch { repository.saveTable(updated) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        screenState.SaveableStateProvider(screen.name) {
        when (screen) {
            AppScreen.WEEK -> {
                if (tableMeta == null || table == null) {
                    LoadingView()
                } else {
                    androidx.compose.runtime.key(tableId) {
                        AggregateScheduleScreen(
                            repository = repository,
                            table = table,
                            tables = tables,
                            currentTableId = tableId,
                            onSelectTable = ::selectTable,
                            onAddCourse = ::openAddCourse,
                            onOpenManage = { navigate(AppScreen.TABLE_MANAGE) },
                            onOpenSettings = { tableDraft = null; editingTableId = tableId; navigate(AppScreen.TABLE_SETTINGS) },
                            onOpenAppearance = { editingTableId = tableId; tableDraft = null; navigate(AppScreen.APPEARANCE) },
                            onOpenCourseManage = { navigate(AppScreen.COURSE_MANAGE) },
                            onOpenTimeTable = { navigate(AppScreen.TIME_TABLE) },
                            onOpenImport = { navigate(AppScreen.IMPORT) },
                            onOpenWidgetHelp = { navigate(AppScreen.WIDGET_HELP) },
                            onShare = { exportDialog = true },
                            courses = tableMeta!!.courses,
                            onEditCourse = { course ->
                                editingCourseId = course.id
                                editorDay = 1
                                editorStartNode = 1
                                editorStep = 1
                                navigate(AppScreen.COURSE_EDITOR)
                            },
                        )
                    }
                }
            }
            AppScreen.TABLE_MANAGE -> AggregateTableManagementScreen(
                repository = repository,
                tables = tables,
                currentTableId = tableId,
                onBack = { tableDraft = null; goBack() },
                onSelect = { tableDraft = null; selectTable(it); goHome() },
                onEditSettings = { tableDraft = null; editingTableId = it; navigate(AppScreen.TABLE_SETTINGS) },
                onAppearance = { tableDraft = null; editingTableId = it; selectTable(it); navigate(AppScreen.APPEARANCE) },
                onCourses = { tableDraft = null; selectTable(it); navigate(AppScreen.COURSE_MANAGE) },
                onCopy = { source ->
                    scope.launch {
                        selectTable(repository.copyTable(source.id, context.getString(R.string.copy_name, source.name)))
                        showToast(context, context.getString(R.string.table_copied))
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
                        showToast(context, context.getString(R.string.delete_success))
                    }
                },
                onNew = { tableDraft = null; editingTableId = null; navigate(AppScreen.TABLE_SETTINGS) },
            )
            AppScreen.TABLE_SETTINGS -> {
                val editingTable = editingTableId?.let { id -> tables.firstOrNull { it.id == id } }
                val settingsTable = tableDraft?.takeIf { it.id == (editingTableId ?: 0L) } ?: editingTable
                TableSettingsScreen(
                    table = settingsTable,
                    onBack = ::goBack,
                    onSave = ::saveTable,
                    onUpdate = updateTable,
                    currentWeek = settingsTable?.let { currentWeek(it) } ?: 1,
                    onAppearance = { if (editingTable != null) { selectTable(editingTable.id); navigate(AppScreen.APPEARANCE) } },
                    onCourses = { if (editingTable != null) { selectTable(editingTable.id); navigate(AppScreen.COURSE_MANAGE) } },
                    onTimeTable = { if (editingTable != null) { selectTable(editingTable.id); navigate(AppScreen.TIME_TABLE) } },
                    onReminders = { if (editingTable != null) { selectTable(editingTable.id); navigate(AppScreen.REMINDERS) } },
                    onWidgetHelp = { navigate(AppScreen.WIDGET_HELP) },
                    onDisplaySettings = { navigate(AppScreen.DISPLAY_SETTINGS) },
                )
            }
            AppScreen.APPEARANCE -> {
                if (table == null || tableMeta == null) {
                    LoadingView()
                } else {
                    AppearanceScreen(
                        repository = repository,
                        table = tableDraft?.takeIf { it.id == tableId } ?: table,
                        onBack = ::goBack,
                        onSave = updateTable,
                    )
                }
            }
            AppScreen.COURSE_MANAGE -> {
                if (table == null || tableMeta == null) {
                    LoadingView()
                } else {
                    CourseManagementScreen(
                        table = tableDraft?.takeIf { it.id == tableId } ?: table,
                        courses = tableMeta!!.courses,
                        onBack = ::goBack,
                        onAdd = { openAddCourse() },
                        onEdit = { course ->
                            editingCourseId = course.id
                            editorDay = 1
                            editorStartNode = 1
                            editorStep = 1
                            navigate(AppScreen.COURSE_EDITOR)
                        },
                        onDelete = { course ->
                            scope.launch {
                                repository.deleteCourse(course)
                                showToast(context, context.getString(R.string.delete_success))
                            }
                        },
                        onClear = { courses ->
                            scope.launch {
                                courses.forEach { repository.deleteCourse(it) }
                                showToast(context, context.getString(R.string.courses_cleared))
                            }
                        },
                    )
                }
            }
            AppScreen.COURSE_EDITOR -> {
                if (table == null) LoadingView() else CourseEditorScreen(
                    repository = repository,
                    table = tableDraft?.takeIf { it.id == tableId } ?: table,
                    courseId = editingCourseId,
                    initialDay = editorDay,
                    initialStartNode = editorStartNode,
                    initialStep = editorStep,
                    existingCourses = tableMeta?.courses.orEmpty(),
                    nodeTimes = tableMeta?.nodeTimes.orEmpty(),
                    onBack = { goBack() },
                    onSaved = {
                        if (initialTableId != null) openRequestedTable() else goBack()
                    },
                    requestedTableId = initialTableId,
                    onOpenRequestedTable = ::openRequestedTable,
                    onCancelTableRequest = onTableRequestConsumed,
                )
            }
            AppScreen.TIME_TABLE -> {
                if (table == null) LoadingView() else AggregateTimeSettingsScreen(
                    repository = repository,
                    table = tableDraft?.takeIf { it.id == tableId } ?: table,
                    onBack = { tableDraft = null; goBack() },
                    onTableUpdated = { updated -> tableDraft = updated },
                )
            }
            AppScreen.IMPORT -> ImportScreen(
                repository = repository,
                currentTableId = tableId,
                initialUri = externalImportUri,
                onInitialUriConsumed = { externalImportUri = null },
                onBack = ::goBack,
                onImported = { id -> selectTable(id); goHome() },
            )
            AppScreen.REMINDERS -> ReminderSettingsScreen(
                repository = repository,
                tableId = tableId,
                onBack = ::goBack,
            )
            AppScreen.WIDGET_HELP -> WidgetHelpScreen(onBack = ::goBack)
            AppScreen.DISPLAY_SETTINGS -> DisplaySettingsScreen(onBack = ::goBack)
        }
        }
    }
    if (exportDialog) {
        val baseName = table?.name.orEmpty().ifBlank { context.getString(R.string.default_table_name) }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        AlertDialog(
            onDismissRequest = { exportDialog = false },
            title = { Text(stringResource(R.string.export_timetable_title)) },
            text = { Text(stringResource(R.string.export_timetable_message)) },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        exportDialog = false
                        jsonExportLauncher.launch("$baseName.sleepdown.json")
                    }) { Text(stringResource(R.string.json_backup)) }
                    TextButton(onClick = {
                        exportDialog = false
                        icsExportLauncher.launch("$baseName.ics")
                    }) { Text(stringResource(R.string.ics_calendar)) }
                }
            },
            dismissButton = { TextButton(onClick = { exportDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    exportNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { exportNotice = null },
            title = { Text(stringResource(R.string.export_result)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { exportNotice = null }) { Text(stringResource(R.string.got_it)) } },
        )
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
        week < 1 -> stringResource(R.string.term_not_started)
        week > table.maxWeek -> stringResource(R.string.term_ended_status)
        week == currentWeek -> stringResource(R.string.current_weekday_status, weekdayName(LocalDate.now().dayOfWeek.value))
        else -> stringResource(R.string.not_current_week)
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
                Text(stringResource(R.string.week_number, week), color = textColor.copy(alpha = 0.8f), fontSize = 13.sp)
                Text(status, color = textColor.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course), tint = textColor)
        }
        IconButton(onClick = onImport, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.import_timetable), tint = textColor)
        }
        Box {
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.backup_export), tint = textColor)
            }
            DropdownMenu(expanded = shareMenuExpanded, onDismissRequest = onDismissShareMenu) {
                DropdownMenuItem(text = { Text(stringResource(R.string.export_json_backup)) }, onClick = onShareBackup)
            }
        }
        Box {
            IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more), tint = textColor)
            }
            DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = onDismissMoreMenu) {
                DropdownMenuItem(text = { Text(stringResource(R.string.switch_table)) }, onClick = onSwitchTable)
                DropdownMenuItem(text = { Text(stringResource(R.string.table_management)) }, onClick = onManage)
                DropdownMenuItem(text = { Text(stringResource(R.string.table_settings)) }, onClick = onSettings)
                DropdownMenuItem(text = { Text(stringResource(R.string.table_appearance)) }, onClick = onAppearance)
                DropdownMenuItem(text = { Text(stringResource(R.string.course_management)) }, onClick = onCourseManage)
                DropdownMenuItem(text = { Text(stringResource(R.string.time_table)) }, onClick = onTimeTable)
                DropdownMenuItem(text = { Text(stringResource(R.string.widgets)) }, onClick = onWidgetHelp)
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
                Text(stringResource(R.string.month_label, month), color = headerTextColor, fontSize = table.headerTextSize.coerceIn(8, 32).sp, fontWeight = FontWeight.Bold, lineHeight = (table.headerTextSize + 1).sp)
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
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course), tint = Color.White, modifier = Modifier.size(22.dp))
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
internal fun SelectionResizeHandle(
    modifier: Modifier,
    rowHeightPx: Float,
    onDragRows: (Int) -> Unit,
) {
    val currentOnDragRows by rememberUpdatedState(onDragRows)
    Image(
        painter = painterResource(R.drawable.sd_add_course_guid_icon),
        contentDescription = stringResource(R.string.adjust_course_length),
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
                            currentOnDragRows(delta)
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
        if (!current) append(stringResource(R.string.non_current_week_marker))
        append(item.course.name)
        if (table.showLocation && time.room.isNotBlank()) {
            append("\n")
            if (table.showRoomPrefix) append("@")
            append(time.room)
        }
        if (time.weekType == CourseTimeEntity.TYPE_ODD) append(stringResource(R.string.odd_week_marker))
        if (time.weekType == CourseTimeEntity.TYPE_EVEN) append(stringResource(R.string.even_week_marker))
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
            .then(if (onMove == null) Modifier.clickable(onClick = onClick) else Modifier.pointerInput(time.id, columnWidthPx, rowHeightPx, table.nodeCount, visibleDays.size, viewportHeightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val holdResult = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var totalX = 0f
                        var totalY = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull CourseHoldResult.MOVED
                            if (!change.pressed) {
                                return@withTimeoutOrNull if (abs(totalX) <= viewConfiguration.touchSlop && abs(totalY) <= viewConfiguration.touchSlop) {
                                    CourseHoldResult.TAP
                                } else {
                                    CourseHoldResult.MOVED
                                }
                            }
                            totalX += change.position.x - change.previousPosition.x
                            totalY += change.position.y - change.previousPosition.y
                            if (abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop) {
                                return@withTimeoutOrNull CourseHoldResult.MOVED
                            }
                        }
                    }
                    when (holdResult) {
                        CourseHoldResult.TAP -> onClick()
                        CourseHoldResult.MOVED -> Unit
                        null -> {
                            down.consume()
                            dragX = 0f
                            dragY = 0f
                            dragStartScroll = scrollState.value.toFloat()
                            dragChanged = false
                            dragging = true
                            var completed = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (!change.pressed) {
                                        completed = true
                                        break
                                    }
                                    change.consume()
                                    val deltaX = change.position.x - change.previousPosition.x
                                    val deltaY = change.position.y - change.previousPosition.y
                                    dragX += deltaX
                                    dragY += deltaY
                                    if (deltaX != 0f || deltaY != 0f) dragChanged = true

                                    if (viewportHeightPx > 0 && scrollState.maxValue > 0) {
                                        val scrollDelta = scrollState.value - dragStartScroll
                                        val contentTop = rowHeightPx * (time.startNode - 1) + dragY + scrollDelta
                                        val contentBottom = contentTop + cardHeightPx
                                        val edge = with(density) { 48.dp.toPx() }
                                        val viewportTop = scrollState.value.toFloat()
                                        val viewportBottom = viewportTop + viewportHeightPx
                                        val scrollBy = when {
                                            contentTop < viewportTop + edge -> -24f
                                            contentBottom > viewportBottom - edge -> 24f
                                            else -> 0f
                                        }
                                        if (scrollBy != 0f) scrollState.dispatchRawDelta(scrollBy)
                                    }
                                }

                                if (completed && dragChanged) {
                                    val targetColumn = (column + (dragX / columnWidthPx).roundToInt())
                                        .coerceIn(0, visibleDays.lastIndex)
                                    val maxStart = (table.nodeCount - step + 1).coerceAtLeast(1)
                                    val targetNode = (time.startNode + ((dragY + (scrollState.value - dragStartScroll)) / rowHeightPx).roundToInt())
                                        .coerceIn(1, maxStart)
                                    if (targetColumn != column || targetNode != time.startNode) {
                                        onMove(visibleDays[targetColumn], targetNode)
                                    }
                                }
                            } finally {
                                dragX = 0f
                                dragY = 0f
                                dragStartScroll = 0f
                                dragChanged = false
                                dragging = false
                            }
                        }
                    }
                }
            })
            .width((columnWidth - 2.dp).coerceAtLeast(1.dp))
            .height((table.itemHeightDp.coerceIn(32, 128).dp * step.toFloat()) + (2.dp * (step - 1).toFloat()) - 2.dp)
            .alpha(displayAlpha)
            .background(baseColor.copy(alpha = table.itemAlpha.coerceIn(0f, 1f)), RoundedCornerShape(table.radius.coerceIn(0, 32).dp))
            .border(2.dp, stroke, RoundedCornerShape(table.radius.coerceIn(0, 32).dp))
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
        title = { Text(stringResource(R.string.select_week)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items((1..maxWeek.coerceAtLeast(1)).toList()) { week ->
                    TextButton(onClick = { onSelect(week) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (week == selectedWeek) stringResource(R.string.selected_week, week) else stringResource(R.string.week_number, week))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCurrent) { Text(stringResource(R.string.return_current_week)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        title = { Text(stringResource(R.string.switch_table)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(tables, key = { it.id }) { table ->
                    TextButton(onClick = { onSelect(table.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(table.name, modifier = Modifier.weight(1f))
                            if (table.id == selectedId) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.current_table))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
            if (course.teacher.isNotBlank()) Text(stringResource(R.string.teacher_value, course.teacher))
            if (course.credit > 0f) Text(stringResource(R.string.credit_value, course.credit))
            val rooms = items.map { it.time.room }.filter { it.isNotBlank() }.distinct().joinToString("、")
            if (rooms.isNotBlank()) Text(stringResource(R.string.room_value, rooms))
            items.forEach { item ->
                val time = item.time
                val clock = courseTimeLabel(time, nodeTimes)
                Text(stringResource(R.string.course_schedule_value, weekdayName(time.day), clock, time.startWeek, time.endWeek, weekTypeLabel(time.weekType)))
            }
            if (course.note.isNotBlank()) Text(stringResource(R.string.note_value, course.note))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
                OutlinedButton(onClick = { deleteDialog = true }) { Text(stringResource(R.string.delete)) }
                Spacer(Modifier.width(10.dp))
                Button(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (deleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.hint),
            text = stringResource(R.string.delete_course_confirm),
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
    containerColor: Color? = null,
    titleSize: Int = 20,
    titleWeight: FontWeight = FontWeight.SemiBold,
    titleStartPadding: Dp = 0.dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    val actualContainerColor = containerColor ?: MaterialTheme.colorScheme.background
    Scaffold(
        containerColor = actualContainerColor,
        topBar = {
            TopAppBar(
                title = { Text(title, modifier = Modifier.padding(start = titleStartPadding), color = MaterialTheme.colorScheme.onSurface, fontSize = titleSize.sp, fontWeight = titleWeight) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface) } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = actualContainerColor),
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
        Text(title, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String = "",
    tint: Color? = null,
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
        Icon(icon, contentDescription = null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (summary.isNotBlank()) Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
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
        title = stringResource(R.string.multi_table_management),
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.sort)) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_add_order)) }, onClick = { sortByName = false; sortMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_name)) }, onClick = { sortByName = true; sortMenu = false })
                }
            }
            IconButton(onClick = onNew) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_table)) }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayTables.isEmpty()) {
                EmptyState(stringResource(R.string.no_tables), stringResource(R.string.create_table_hint), onNew, padding)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(stringResource(R.string.table_management_hint), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_table))
            }
        }
    }
    deleting?.let { table ->
        if (tables.size <= 1) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text(stringResource(R.string.cannot_delete)) },
                text = { Text(stringResource(R.string.keep_one_table)) },
                confirmButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.got_it)) } },
            )
        } else {
            ConfirmDialog(
                title = stringResource(R.string.hint),
                text = stringResource(R.string.delete_table_confirm, table.name),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(96.dp).clickable(onClick = onClick)) {
                TablePreviewBackground(table)
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(table.name.ifBlank { stringResource(R.string.default_label) }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.current_table), tint = Color.White)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCourses) { Icon(Icons.Default.List, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.courses)) }
                TextButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.settings)) }
                TextButton(onClick = onAppearance) { Icon(Icons.Default.Palette, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.appearance)) }
                TextButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.copy)) }
                TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.delete)) }
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
    onReminders: () -> Unit,
    onWidgetHelp: () -> Unit,
    onDisplaySettings: () -> Unit,
) {
    val defaultTableName = stringResource(R.string.default_table_name)
    val defaultTable = remember(defaultTableName) {
        TableEntity(name = defaultTableName, startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay())
    }
    var working by remember(table) { mutableStateOf(table ?: defaultTable) }
    val context = LocalContext.current
    var nameDialog by remember { mutableStateOf(false) }
    var numberDialog by remember { mutableStateOf<String?>(null) }
    var dateDialog by remember { mutableStateOf(false) }
    var currentWeekDateDialog by remember { mutableStateOf(false) }

    fun persistWorking() {
        if (working.name.isNotBlank()) onUpdate(working)
    }
    BackHandler {
        persistWorking()
        onBack()
    }

    ScreenScaffold(
        title = stringResource(R.string.table_settings),
        onBack = {
            persistWorking()
            onBack()
        },
        actions = {
            IconButton(onClick = {
                if (working.name.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.blank_name_error), Toast.LENGTH_SHORT).show()
                } else {
                    onSave(working)
                }
            }) { Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save)) }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                SettingsGroup(stringResource(R.string.table_name)) {
                    SettingRow(Icons.Default.TableView, stringResource(R.string.table_name), working.name, tint = Color(0xFF4E7BD9), onClick = { nameDialog = true })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.table_data)) {
                    SettingRow(Icons.Default.AccessTime, stringResource(R.string.class_time), stringResource(R.string.tap_to_edit), tint = Color(0xFF2AA69B), onClick = { persistWorking(); onTimeTable() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.CalendarMonth, stringResource(R.string.first_day), LocalDate.ofEpochDay(working.startDate).toString(), tint = Color(0xFF4E7BD9), onClick = { dateDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        Icons.Default.CalendarMonth,
                        stringResource(R.string.current_week),
                        stringResource(R.string.current_week_summary, currentWeek.coerceIn(1, working.maxWeek), dateFor(working, currentWeek.coerceIn(1, working.maxWeek), if (working.sundayFirst) 7 else 1)),
                        tint = Color(0xFF4E7BD9),
                        onClick = { currentWeekDateDialog = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.List, stringResource(R.string.nodes_per_day), stringResource(R.string.node_count_summary, working.nodeCount), tint = Color(0xFF4E7BD9), onClick = { numberDialog = "nodeCount" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.CalendarMonth, stringResource(R.string.term_weeks), stringResource(R.string.term_weeks_summary, working.maxWeek), tint = Color(0xFF4E7BD9), onClick = { numberDialog = "maxWeek" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.List, stringResource(R.string.manage_courses), stringResource(R.string.manage_courses_summary), tint = Color(0xFF4E7BD9), onClick = { persistWorking(); onCourses() })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.table_appearance)) {
                    SettingRow(Icons.Default.Tune, stringResource(R.string.table_appearance), stringResource(R.string.appearance_summary), tint = Color(0xFF8D62C8), onClick = { persistWorking(); onAppearance() })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.app_display)) {
                    SettingRow(Icons.Default.Palette, stringResource(R.string.app_display), stringResource(R.string.display_settings_summary), onClick = { persistWorking(); onDisplaySettings() })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.default_config)) {
                    SettingRow(Icons.Default.Notifications, stringResource(R.string.course_reminders), stringResource(R.string.reminder_summary), tint = Color(0xFF8D62C8), onClick = { persistWorking(); onReminders() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Widgets, stringResource(R.string.widgets), stringResource(R.string.widget_summary), tint = Color(0xFF4E7BD9), onClick = { persistWorking(); onWidgetHelp() })
                }
            }
        }
    }

    if (nameDialog) {
        TextInputDialog(
            title = stringResource(R.string.table_name),
            initial = working.name,
            validation = { value -> if (value.isBlank()) context.getString(R.string.blank_name_error) else null },
            onConfirm = { value -> working = working.copy(name = value.trim()); nameDialog = false },
            onDismiss = { nameDialog = false },
        )
    }
    numberDialog?.let { field ->
        val initial = if (field == "nodeCount") working.nodeCount.toString() else working.maxWeek.toString()
        TextInputDialog(
            title = if (field == "nodeCount") stringResource(R.string.nodes_per_day) else stringResource(R.string.term_weeks),
            initial = initial,
            number = true,
            validation = { value -> if (value.toIntOrNull() == null) context.getString(R.string.invalid_number) else null },
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
        DatePickerDialogFor(
            working.startDate,
            onSelected = { date ->
                working = working.copy(startDate = date.with(java.time.DayOfWeek.MONDAY).toEpochDay())
                dateDialog = false
            },
            onDismiss = { dateDialog = false },
        )
    }
    if (currentWeekDateDialog) {
        val week = currentWeek.coerceIn(1, working.maxWeek)
        DatePickerDialogFor(
            value = dateFor(working, week, if (working.sundayFirst) 7 else 1).toEpochDay(),
            onSelected = { date ->
                val firstDay = date.minusDays((date.dayOfWeek.value - 1).toLong())
                working = working.copy(startDate = firstDay.minusWeeks((week - 1).toLong()).toEpochDay())
                currentWeekDateDialog = false
            },
            onDismiss = { currentWeekDateDialog = false },
        )
    }
}

@Composable
private fun DisplaySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uiSettings by rememberSleepDownUiSettings(context)
    var languageDialog by remember { mutableStateOf(false) }
    val languageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val languageSummary = when {
        languageTag.startsWith("en") -> stringResource(R.string.language_english)
        languageTag.startsWith("zh") -> stringResource(R.string.language_chinese)
        else -> stringResource(R.string.language_follow_system)
    }
    val emptyImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onSuccess {
                    SleepDownUiPreferences.setEmptyImageUri(context, uri.toString())
                    SleepDownUiPreferences.setShowEmptyImage(context, true)
                }
                .onFailure { showToast(context, context.getString(R.string.operation_failed)) }
        }
    }
    ScreenScaffold(title = stringResource(R.string.app_display), onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                SettingsGroup(stringResource(R.string.theme)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            SleepDownThemeMode.SYSTEM to stringResource(R.string.follow_system),
                            SleepDownThemeMode.LIGHT to stringResource(R.string.light_theme),
                            SleepDownThemeMode.DARK to stringResource(R.string.dark_theme),
                        ).forEach { (mode, label) ->
                            WeekTypeButton(label, uiSettings.themeMode == mode, { SleepDownUiPreferences.setTheme(context, mode) }, Modifier.weight(1f))
                        }
                    }
                    SettingRow(Icons.Default.Language, stringResource(R.string.language), languageSummary, onClick = { languageDialog = true })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.empty_table_image)) {
                    SettingRow(Icons.Default.Home, stringResource(R.string.empty_table_image), if (uiSettings.emptyImageUri.isBlank()) stringResource(R.string.default_original_image) else stringResource(R.string.selected_user_image), trailing = { Switch(checked = uiSettings.showEmptyImage, onCheckedChange = { SleepDownUiPreferences.setShowEmptyImage(context, it) }) })
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { emptyImagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.choose_image)) }
                        OutlinedButton(onClick = { SleepDownUiPreferences.setEmptyImageUri(context, "") }, enabled = uiSettings.emptyImageUri.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.use_default_image)) }
                    }
                }
            }
            item {
                SettingsGroup(stringResource(R.string.bottom_spacing)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to stringResource(R.string.none), 48 to stringResource(R.string.standard), 96 to stringResource(R.string.roomy)).forEach { (value, label) ->
                            WeekTypeButton(label, uiSettings.bottomSpacingDp == value, { SleepDownUiPreferences.setBottomSpacing(context, value) }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (languageDialog) {
        LanguagePickerDialog(
            selectedTag = languageTag,
            onSelect = { tag ->
                languageDialog = false
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            },
            onDismiss = { languageDialog = false },
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    selectedTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        "" to stringResource(R.string.language_follow_system),
        "zh-Hans" to stringResource(R.string.language_chinese),
        "en" to stringResource(R.string.language_english),
    )
    val selectedLanguage = selectedTag.substringBefore('-')
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { (tag, label) ->
                    val selected = if (tag.isBlank()) {
                        selectedTag.isBlank()
                    } else {
                        selectedLanguage == tag.substringBefore('-')
                    }
                    TextButton(onClick = { onSelect(tag) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (selected) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun AppearanceScreen(
    repository: TimetableRepository,
    table: TableEntity,
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

    BackHandler {
        onSave(working)
        onBack()
    }
    ScreenScaffold(
        title = stringResource(R.string.table_appearance),
        onBack = {
            onSave(working)
            showToast(context, context.getString(R.string.save_success))
            onBack()
        },
        actions = {
            IconButton(onClick = {
                onSave(working)
                showToast(context, context.getString(R.string.save_success))
            }) { Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AggregateAppearancePreview(repository = repository, table = working)
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                SettingsGroup(stringResource(R.string.overall)) {
                    SettingRow(
                        Icons.Default.Palette,
                        stringResource(R.string.timetable_background),
                        if (working.bgImageUri.isNullOrBlank()) stringResource(R.string.default_gradient_background) else stringResource(R.string.image_background_set),
                        tint = Color(0xFF4E7BD9),
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        trailing = {
                            if (!working.bgImageUri.isNullOrBlank()) {
                                TextButton(onClick = { working = working.copy(bgImageUri = null) }) { Text(stringResource(R.string.clear)) }
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.GridOn, stringResource(R.string.show_grid_guides), trailing = { Switch(checked = working.showGrid, onCheckedChange = { working = working.copy(showGrid = it) }) }, onClick = { working = working.copy(showGrid = !working.showGrid) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.TextFields, stringResource(R.string.interface_text_color), colorName(working.textColor.toInt()), tint = Color(0xFF2AA69B), onClick = { colorTarget = "text" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.TextFields, stringResource(R.string.header_text_size), stringResource(R.string.size_sp, working.headerTextSize), tint = Color(0xFF2AA69B), onClick = { inputTarget = "headerTextSize" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.AccessTime, stringResource(R.string.show_time_bar), trailing = { Switch(checked = working.showTimeBar, onCheckedChange = { working = working.copy(showTimeBar = it) }) }, onClick = { working = working.copy(showTimeBar = !working.showTimeBar) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Visibility, stringResource(R.string.show_saturday), trailing = { Switch(checked = working.showSat, onCheckedChange = { working = working.copy(showSat = it, showWeekend = it || working.showSun) }) }, onClick = { working = working.copy(showSat = !working.showSat, showWeekend = !working.showSat || working.showSun) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Visibility, stringResource(R.string.show_sunday), trailing = { Switch(checked = working.showSun, onCheckedChange = { working = working.copy(showSun = it, showWeekend = working.showSat || it) }) }, onClick = { working = working.copy(showSun = !working.showSun, showWeekend = working.showSat || !working.showSun) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Visibility, stringResource(R.string.show_other_week_courses), trailing = { Switch(checked = working.showOtherWeekCourse, onCheckedChange = { working = working.copy(showOtherWeekCourse = it) }) }, onClick = { working = working.copy(showOtherWeekCourse = !working.showOtherWeekCourse) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.CalendarMonth, stringResource(R.string.sunday_first), trailing = { Switch(checked = working.sundayFirst, onCheckedChange = { working = working.copy(sundayFirst = it) }) }, onClick = { working = working.copy(sundayFirst = !working.sundayFirst) })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.course_cells)) {
                    SettingRow(Icons.Default.TextFields, stringResource(R.string.course_text_color), colorName(working.courseTextColor), tint = Color(0xFF4E7BD9), onClick = { colorTarget = "course" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Palette, stringResource(R.string.course_text_overlay), trailing = { Switch(checked = working.textColorCompose, onCheckedChange = { working = working.copy(textColorCompose = it) }) }, onClick = { working = working.copy(textColorCompose = !working.textColorCompose) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Palette, stringResource(R.string.cell_border_color), colorName(working.strokeColor), tint = Color(0xFFFF8A00), onClick = { colorTarget = "stroke" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Palette, stringResource(R.string.border_uses_cell_color), trailing = { Switch(checked = working.strokeColorCompose, onCheckedChange = { working = working.copy(strokeColorCompose = it) }) }, onClick = { working = working.copy(strokeColorCompose = !working.strokeColorCompose) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.GridOn, stringResource(R.string.dotted_border), trailing = { Switch(checked = working.useDottedLine, onCheckedChange = { working = working.copy(useDottedLine = it) }) }, onClick = { working = working.copy(useDottedLine = !working.useDottedLine) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Tune, stringResource(R.string.cell_height), stringResource(R.string.size_dp, working.itemHeightDp), tint = Color(0xFF8D62C8), onClick = { inputTarget = "height" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Tune, stringResource(R.string.corner_radius), stringResource(R.string.size_dp, working.radius), tint = Color(0xFF8D62C8), onClick = { inputTarget = "radius" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Palette, stringResource(R.string.course_opacity), stringResource(R.string.percentage, (working.itemAlpha * 100).toInt()), tint = Color(0xFF8D62C8), onClick = { inputTarget = "alpha" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Palette, stringResource(R.string.other_week_opacity), stringResource(R.string.percentage, (working.otherWeekAlpha * 100).toInt()), tint = Color(0xFF8D62C8), onClick = { inputTarget = "otherAlpha" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.TextFields, stringResource(R.string.course_text_size), stringResource(R.string.size_sp_float, working.itemTextSize), tint = Color(0xFF4E7BD9), onClick = { inputTarget = "itemTextSize" })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.TextFields, stringResource(R.string.horizontal_center), trailing = { Switch(checked = working.itemCenterHorizontal, onCheckedChange = { working = working.copy(itemCenterHorizontal = it) }) }, onClick = { working = working.copy(itemCenterHorizontal = !working.itemCenterHorizontal) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.TextFields, stringResource(R.string.vertical_center), trailing = { Switch(checked = working.itemCenterVertical, onCheckedChange = { working = working.copy(itemCenterVertical = it) }) }, onClick = { working = working.copy(itemCenterVertical = !working.itemCenterVertical) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.AccessTime, stringResource(R.string.show_course_time), trailing = { Switch(checked = working.showTime, onCheckedChange = { working = working.copy(showTime = it) }) }, onClick = { working = working.copy(showTime = !working.showTime) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Place, stringResource(R.string.show_course_room), trailing = { Switch(checked = working.showLocation, onCheckedChange = { working = working.copy(showLocation = it) }) }, onClick = { working = working.copy(showLocation = !working.showLocation) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Place, stringResource(R.string.room_prefix_at), trailing = { Switch(checked = working.showRoomPrefix, onCheckedChange = { working = working.copy(showRoomPrefix = it) }) }, onClick = { working = working.copy(showRoomPrefix = !working.showRoomPrefix) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Person, stringResource(R.string.show_teacher), trailing = { Switch(checked = working.showTeacher, onCheckedChange = { working = working.copy(showTeacher = it) }) }, onClick = { working = working.copy(showTeacher = !working.showTeacher) })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.reset_default_appearance)) {
                    SettingRow(Icons.Default.Refresh, stringResource(R.string.reset_default_appearance), stringResource(R.string.reset_appearance_summary), onClick = {
                        working = working.copy(
                            bgImageUri = null, showWeekend = true, showSat = true, showSun = true,
                            sundayFirst = false, textColor = 0xFF000000, itemTextSize = 12f,
                            itemAlpha = 0.5f, itemHeightDp = 64, showTime = false, showLocation = true,
                            showRoomPrefix = true, showTeacher = true, showOtherWeekCourse = true,
                            otherWeekAlpha = 0.5f, showGrid = false, showTimeBar = true,
                            headerTextSize = 11, courseTextColor = 0xFFFFFFFF.toInt(),
                            strokeColor = 0x80FFFFFF.toInt(), useDottedLine = false,
                            itemCenterHorizontal = false, itemCenterVertical = false,
                            textColorCompose = false, strokeColorCompose = false, radius = 4,
                        )
                    })
                }
            }
            }
        }
    }

    colorTarget?.let { target ->
        ColorChoiceDialog(
            title = when (target) {
                "text" -> stringResource(R.string.interface_text_color)
                "course" -> stringResource(R.string.course_text_color)
                else -> stringResource(R.string.cell_border_color)
            },
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
                "headerTextSize" -> stringResource(R.string.header_text_size)
                "height" -> stringResource(R.string.cell_height)
                "radius" -> stringResource(R.string.corner_radius)
                "alpha" -> stringResource(R.string.opacity_range, stringResource(R.string.course_opacity))
                "otherAlpha" -> stringResource(R.string.opacity_range, stringResource(R.string.other_week_opacity))
                else -> stringResource(R.string.course_text_size)
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
        title = stringResource(R.string.course_management),
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.sort)) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_add_order)) }, onClick = { sortByName = false; sortMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_course_name)) }, onClick = { sortByName = true; sortMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.clear_courses)) }, onClick = { sortMenu = false; clearDialog = true })
                }
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course)) }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayCourses.isEmpty()) {
                EmptyState(stringResource(R.string.no_courses), stringResource(R.string.add_course_hint), onAdd, padding)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text(stringResource(R.string.course_list_hint), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
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
                                Text(course.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), containerColor = Color(0xFFFF2D55), contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course))
            }
        }
    }
    if (clearDialog) {
        ConfirmDialog(title = stringResource(R.string.hint), text = stringResource(R.string.clear_courses_confirm), onConfirm = { clearDialog = false; onClear(courses) }, onDismiss = { clearDialog = false })
    }
    deleteCourse?.let { course ->
        ConfirmDialog(title = stringResource(R.string.hint), text = stringResource(R.string.delete_course_confirm), onConfirm = { deleteCourse = null; onDelete(course) }, onDismiss = { deleteCourse = null })
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
    requestedTableId: Long?,
    onOpenRequestedTable: () -> Unit,
    onCancelTableRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember(courseId, table.id) { mutableStateOf("") }
    var note by remember(courseId, table.id) { mutableStateOf("") }
    var credit by remember(courseId, table.id) { mutableStateOf("") }
    var color by remember(courseId, table.id) { mutableIntStateOf(0) }
    val drafts = remember(courseId, table.id, initialDay, initialStartNode, initialStep) { mutableStateListOf<TimeDraft>() }
    var loaded by remember(courseId, table.id, initialDay, initialStartNode, initialStep) { mutableStateOf(false) }
    var error by remember(courseId, table.id, initialDay, initialStartNode, initialStep) { mutableStateOf<String?>(null) }
    var inputTarget by remember { mutableStateOf<String?>(null) }
    var colorDialog by remember { mutableStateOf(false) }
    var roomEditorIndex by remember { mutableStateOf<Int?>(null) }
    var teacherEditorIndex by remember { mutableStateOf<Int?>(null) }
    var openTimeDialogs by remember { mutableIntStateOf(0) }
    val hasEditorDialog = inputTarget != null || colorDialog || roomEditorIndex != null ||
        teacherEditorIndex != null || openTimeDialogs > 0
    val availableNodeTimes = nodeTimesFor(table, nodeTimes)
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var initialSnapshot by remember(courseId, table.id) { mutableStateOf<CourseEditorSnapshot?>(null) }
    var discardDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val title = if (courseId == null) stringResource(R.string.add_course) else stringResource(R.string.edit_course_title)
    fun leaveEditor() {
        if (requestedTableId != null) onOpenRequestedTable() else onBack()
    }
    fun requestBack() {
        if (saving) return
        val current = CourseEditorSnapshot(name, color, credit, note, drafts.toList())
        if (initialSnapshot != null && current != initialSnapshot) discardDialog = true else leaveEditor()
    }
    fun keepEditing() {
        discardDialog = false
        if (requestedTableId != null) onCancelTableRequest()
    }
    BackHandler(onBack = ::requestBack)
    LaunchedEffect(requestedTableId, loaded, saving, hasEditorDialog) {
        if (requestedTableId != null && loaded && !saving && !hasEditorDialog) requestBack()
    }

    LaunchedEffect(courseId, table.id, initialDay, initialStartNode, initialStep) {
        try {
            drafts.clear()
            if (courseId == null) {
                drafts += defaultTimeDraft(table.maxWeek, initialDay, initialStartNode, initialStep, table.nodeCount)
            } else {
                val course = repository.getCourse(courseId)
                    ?: error(context.getString(R.string.course_load_failed))
                name = course.name
                note = course.note
                credit = if (course.credit == 0f) "" else course.credit.toString()
                color = course.color
                drafts += courseTimeDrafts(course, repository.getCourseTimes(courseId))
            }
            initialSnapshot = CourseEditorSnapshot(name, color, credit, note, drafts.toList())
            loaded = true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = context.getString(R.string.course_load_failed)
            android.util.Log.e("CourseEditor", "Failed to load course", failure)
        }
    }

    if (!loaded) {
        ScreenScaffold(title = title, onBack = onBack) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    fun save() {
        if (saving) return
        val validationError = when {
            name.isBlank() -> R.string.course_name_required
            drafts.isEmpty() -> R.string.time_slot_required
            drafts.any { it.selectedWeeks.isEmpty() } -> R.string.week_required
            drafts.any { it.selectedWeeks.any { week -> week !in 1..table.maxWeek } } -> R.string.valid_week_range
            drafts.any { it.day !in 1..7 || it.startNode < 1 || it.step < 1 || it.startNode + it.step - 1 > table.nodeCount } -> R.string.day_period_required_aggregate
            !validCourseCredit(credit) -> R.string.course_credit_invalid
            drafts.any { it.ownTime && !customTimeRangeValid(it.startTime, it.endTime) } -> R.string.custom_time_invalid
            else -> null
        }
        if (validationError != null) {
            error = context.getString(validationError)
            showToast(context, context.getString(validationError))
            return
        }
        val entity = CourseEntity(
            id = courseId ?: 0L,
            tableId = table.id,
            name = name.trim(),
            color = if (color == 0) CourseColors.colorFor(name) else color,
            teacher = drafts.firstOrNull()?.teacher?.trim().orEmpty(),
            note = note.trim(),
            credit = credit.toFloatOrNull() ?: 0f,
        )
        val times = drafts.flatMap { it.toEntities(entity.id) }
        focusManager.clearFocus()
        saving = true
        error = null
        scope.launch {
            try {
                repository.saveCourse(table.id, entity, times)
                showToast(context, context.getString(R.string.save_success))
                onSaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = context.getString(R.string.aggregate_save_failed)
                showToast(context, context.getString(R.string.aggregate_save_failed))
                android.util.Log.e("CourseEditor", "Failed to save course", failure)
            } finally {
                saving = false
            }
        }
    }

    ScreenScaffold(
        title = title,
        onBack = ::requestBack,
        actions = {
            TextButton(onClick = ::save, enabled = !saving) {
                Text(stringResource(if (saving) R.string.course_saving else R.string.save))
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
                        onRemove = { drafts.removeAt(index) },
                        onTeacher = { teacherEditorIndex = index },
                        onRoom = { roomEditorIndex = index },
                        teacher = drafts[index].teacher,
                        onDialogVisibilityChanged = { visible -> openTimeDialogs += if (visible) 1 else -1 },
                    )
                }
                item { error?.let { Text(it, modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp), color = Color(0xFFD32F2F), fontSize = 13.sp) } }
            }
            FloatingActionButton(
                onClick = { if (!saving) drafts += defaultTimeDraft(table.maxWeek, 1, 1, maxNode = table.nodeCount) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 40.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_time_slot))
            }
            if (saving) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.course_saving), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (discardDialog) {
        AlertDialog(
            onDismissRequest = ::keepEditing,
            title = { Text(stringResource(R.string.discard_course_title)) },
            text = { Text(stringResource(R.string.discard_course_message)) },
            confirmButton = { TextButton(onClick = { discardDialog = false; leaveEditor() }) { Text(stringResource(R.string.discard_changes)) } },
            dismissButton = { TextButton(onClick = ::keepEditing) { Text(stringResource(R.string.keep_editing)) } },
        )
    }
    inputTarget?.let { target ->
        val initial = when (target) { "credit" -> credit; else -> note }
        TextInputDialog(
            title = if (target == "credit") stringResource(R.string.credits) else stringResource(R.string.notes),
            initial = initial,
            number = target == "credit",
            validation = { value ->
                if (target == "credit" && !validCourseCredit(value)) context.getString(R.string.course_credit_invalid) else null
            },
            multiline = target == "note",
            clearable = true,
            onConfirm = { value ->
                if (target == "credit") credit = value else note = value
                inputTarget = null
            },
            onClear = {
                if (target == "credit") credit = "" else note = ""
                inputTarget = null
            },
            onDismiss = { inputTarget = null },
        )
    }
    teacherEditorIndex?.let { index ->
        TextInputDialog(
            title = stringResource(R.string.teacher),
            initial = drafts.getOrNull(index)?.teacher.orEmpty(),
            clearable = true,
            onConfirm = { value ->
                drafts.getOrNull(index)?.let { drafts[index] = it.copy(teacher = value) }
                teacherEditorIndex = null
            },
            onClear = {
                drafts.getOrNull(index)?.let { drafts[index] = it.copy(teacher = "") }
                teacherEditorIndex = null
            },
            onDismiss = { teacherEditorIndex = null },
        )
    }
    roomEditorIndex?.let { index ->
        TextInputDialog(
            title = stringResource(R.string.room),
            initial = drafts.getOrNull(index)?.room.orEmpty(),
            clearable = true,
            onConfirm = { value -> drafts[index] = drafts[index].copy(room = value); roomEditorIndex = null },
            onClear = { drafts[index] = drafts[index].copy(room = ""); roomEditorIndex = null },
            onDismiss = { roomEditorIndex = null },
        )
    }
    if (colorDialog) {
        ColorChoiceDialog(title = stringResource(R.string.course_color), initial = color, includeAuto = true, onSelect = { color = it; colorDialog = false }, onDismiss = { colorDialog = false })
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
        SleepDownEditorRow(Icons.Default.MenuBook, MaterialTheme.colorScheme.primary) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (name.isBlank()) Text(stringResource(R.string.course_name), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
        val selectedColor = if (color == 0) MaterialTheme.colorScheme.primary else CourseColors.asColor(color)
        SleepDownEditorRow(Icons.Default.Palette, selectedColor, onClick = onColorClick) {
            Text(stringResource(R.string.change_color), color = selectedColor, fontSize = 14.sp)
        }
        SleepDownEditorRow(Icons.Default.Star, MaterialTheme.colorScheme.onSurfaceVariant, onClick = onCreditClick) {
            Text(
                if (credit.isBlank()) stringResource(R.string.credit_optional) else stringResource(R.string.credit_display, credit),
                color = if (credit.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            )
        }
        SleepDownEditorRow(Icons.Default.StickyNote2, MaterialTheme.colorScheme.onSurfaceVariant, onClick = onNoteClick) {
            Text(
                if (note.isBlank()) stringResource(R.string.note_optional) else note,
                color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SleepDownEditorRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(56.dp).padding(16.dp))
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
    onDialogVisibilityChanged: (Boolean) -> Unit,
) {
    var weekDialog by remember(draft.id, index) { mutableStateOf(false) }
    var timeDialog by remember(draft.id, index) { mutableStateOf(false) }
    var customTimeField by remember(draft.id, index) { mutableStateOf<String?>(null) }
    if (weekDialog || timeDialog || customTimeField != null) {
        DisposableEffect(Unit) {
            onDialogVisibilityChanged(true)
            onDispose { onDialogVisibilityChanged(false) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(43.dp)) {
            Text(stringResource(R.string.time_slot), modifier = Modifier.padding(start = 16.dp, top = 24.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(32.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete_time_slot), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }
        SleepDownEditorRow(Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primary, onClick = { weekDialog = true }) {
            Text(selectedWeekLabel(draft.selectedWeeks, table.maxWeek), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
        SleepDownEditorRow(Icons.Default.AccessTime, MaterialTheme.colorScheme.primary, onClick = { timeDialog = true }) {
            Text(timeRowLabel(draft), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.custom_time), color = if (draft.ownTime) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Checkbox(
                    checked = draft.ownTime,
                    onCheckedChange = { checked ->
                        onChange(draft.copy(
                            ownTime = checked,
                            startTime = draft.startTime.ifBlank { nodeTimes.firstOrNull { it.node == draft.startNode }?.start.orEmpty() },
                            endTime = draft.endTime.ifBlank { nodeTimes.firstOrNull { it.node == draft.startNode + draft.step - 1 }?.end.orEmpty() },
                        ))
                    },
                    modifier = Modifier.size(36.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        if (draft.ownTime) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 104.dp, end = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { customTimeField = "start" }, modifier = Modifier.weight(1f)) { Text(draft.startTime.ifBlank { stringResource(R.string.class_start_time) }) }
                OutlinedButton(onClick = { customTimeField = "end" }, modifier = Modifier.weight(1f)) { Text(draft.endTime.ifBlank { stringResource(R.string.class_end_time) }) }
            }
        }
        SleepDownEditorRow(Icons.Default.Person, MaterialTheme.colorScheme.onSurfaceVariant, onClick = onTeacher) {
            Text(
                if (teacher.isBlank()) stringResource(R.string.teacher_optional) else teacher,
                color = if (teacher.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
        SleepDownEditorRow(Icons.Default.Place, MaterialTheme.colorScheme.onSurfaceVariant, onClick = onRoom) {
            Text(
                if (draft.room.isBlank()) stringResource(R.string.room_optional) else draft.room,
                color = if (draft.room.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
    }

    if (weekDialog) {
        var selected by remember { mutableStateOf(draft.selectedWeeks) }
        val all = (1..table.maxWeek).toSet()
        val odd = all.filter { it % 2 == 1 }.toSet()
        val even = all.filter { it % 2 == 0 }.toSet()
        AlertDialog(
            onDismissRequest = { weekDialog = false },
            title = { Text(stringResource(R.string.select_week_count)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekNumberGrid(maxWeek = table.maxWeek, selected = selected, onChange = { selected = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeekTypeButton(stringResource(R.string.every_week), selected == all, { selected = all }, Modifier.weight(1f))
                        WeekTypeButton(stringResource(R.string.odd_week), selected == odd, { selected = odd }, Modifier.weight(1f))
                        WeekTypeButton(stringResource(R.string.even_week), selected == even, { selected = even }, Modifier.weight(1f))
                    }
                    if (selected.isEmpty()) Text(stringResource(R.string.week_required), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = selected.isNotEmpty(), onClick = {
                    onChange(draft.copy(selectedWeeks = selected))
                    weekDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { weekDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (timeDialog) {
        TimeSelectionDialog(
            draft = draft,
            maxNode = table.nodeCount.coerceIn(1, 60),
            onChange = { onChange(it); timeDialog = false },
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

@Composable
private fun timeRowLabel(draft: TimeDraft): String =
    stringResource(
        R.string.time_summary,
        stringResource(R.string.weekday_with_prefix, weekdayName(draft.day)),
        draft.startNode,
        draft.startNode + draft.step - 1,
        "",
    )

@Composable
private fun TimeSelectionDialog(
    draft: TimeDraft,
    maxNode: Int,
    onChange: (TimeDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var working by remember { mutableStateOf(draft) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_class_time)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeChoiceMenuRow(stringResource(R.string.weekday), stringResource(R.string.weekday_with_prefix, weekdayName(working.day)), (1..7).map { it to stringResource(R.string.weekday_with_prefix, weekdayName(it)) }) { working = working.copy(day = it) }
                TimeChoiceMenuRow(stringResource(R.string.start_period), stringResource(R.string.period_number, working.startNode), (1..maxNode).map { it to stringResource(R.string.period_number, it) }) { value ->
                    working = working.copy(startNode = value, step = working.step.coerceAtMost(maxNode - value + 1))
                }
                TimeChoiceMenuRow(stringResource(R.string.end_period), stringResource(R.string.period_number, working.startNode + working.step - 1), (working.startNode..maxNode).map { it to stringResource(R.string.period_number, it) }) { value ->
                    working = working.copy(step = value - working.startNode + 1)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onChange(working) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) { Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp) }
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
    LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
        gridItems((1..maxWeek.coerceIn(1, 60)).toList()) { week ->
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (week in selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .toggleable(value = week in selected, role = Role.Checkbox) { checked ->
                        onChange(if (checked) selected + week else selected - week)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("$week", color = if (week in selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
                                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        title = stringResource(R.string.time_table),
        onBack = onBack,
        actions = {
            IconButton(onClick = { addDialog = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_period)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, onClick = { menu = false; onCopy() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.save)) }, onClick = {
                        menu = false
                        scope.launch {
                            repository.saveNodeTimes(table.id, rows.map { NodeTimeEntity(it.id, table.id, it.node, it.start, it.end) })
                            showToast(context, context.getString(R.string.save_success))
                        }
                    })
                }
            }
            IconButton(onClick = {
                scope.launch {
                    repository.saveNodeTimes(table.id, rows.map { NodeTimeEntity(it.id, table.id, it.node, it.start, effectiveEnd(it.start, it.end, uniform, duration)) })
                    showToast(context, context.getString(R.string.save_success))
                }
            }) { Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save)) }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(R.string.time_table_help), modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
            }
            item {
                SettingsGroup(stringResource(R.string.current_table_time_table)) {
                    SettingRow(Icons.Default.AccessTime, stringResource(R.string.default_time_table), stringResource(R.string.time_table_edit_hint), tint = Color(0xFF2AA69B))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.ContentCopy, stringResource(R.string.copy_time_table), stringResource(R.string.copy_time_table_summary), tint = Color(0xFF4E7BD9), onClick = onCopy)
                }
            }
            item {
                SettingsGroup(stringResource(R.string.edit_time_table)) {
                    SettingRow(Icons.Default.Tune, stringResource(R.string.same_duration), trailing = { Switch(checked = uniform, onCheckedChange = { if (it) uniformConfirm = true else uniform = false }) }, onClick = { if (!uniform) uniformConfirm = true else uniform = false })
                    if (uniform) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingRow(Icons.Default.AccessTime, stringResource(R.string.duration), stringResource(R.string.duration_minutes, duration.toIntOrNull() ?: 50), tint = Color(0xFF4E7BD9), onClick = { addDialog = true })
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(stringResource(R.string.time_table_24h_hint), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            items(rows.indices.toList(), key = { rows[it].node }) { index ->
                val row = rows[index]
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${row.node}", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        OutlinedButton(onClick = { timePickerTarget = TimePickerTarget(index, isStart = true) }, modifier = Modifier.weight(1f)) { Text(row.start) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { timePickerTarget = TimePickerTarget(index, isStart = false) }, modifier = Modifier.weight(1f)) { Text(row.end) }
                        IconButton(onClick = { if (rows.size > 1) deleting = row }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_period, row.node), tint = if (rows.size > 1) Color(0xFFE53935) else Color.LightGray) }
                    }
                }
            }
        }
    }
    if (addDialog) {
        TextInputDialog(
            title = stringResource(R.string.add_period_or_uniform),
            initial = if (uniform) duration else "1",
            number = true,
            validation = { value -> if (value.toIntOrNull() == null) context.getString(R.string.invalid_number) else null },
            onConfirm = { value ->
            if (uniform) duration = value.toIntOrNull()?.coerceIn(10, 180)?.toString() ?: duration
            else {
                scope.launch {
                    if (rows.size < TimetableRepository.MAX_NODE_COUNT) {
                        val node = repository.addNode(table.id)
                        rows += NodeDraft(node.id, node.node, node.start, node.end)
                        showToast(context, context.getString(R.string.period_added, node.node))
                    } else {
                        showToast(context, context.getString(R.string.max_periods, TimetableRepository.MAX_NODE_COUNT))
                    }
                }
            }
            addDialog = false
        }, onDismiss = { addDialog = false })
    }
    if (uniformConfirm) {
        ConfirmDialog(title = stringResource(R.string.uniform_duration), text = stringResource(R.string.uniform_duration_warning), onConfirm = { uniformConfirm = false; uniform = true }, onDismiss = { uniformConfirm = false })
    }
    deleting?.let { row ->
        ConfirmDialog(title = stringResource(R.string.delete_period, row.node), text = stringResource(R.string.period_delete_warning), onConfirm = {
            deleting = null
            scope.launch {
                repository.deleteNode(table.id, row.node)
                val refreshed = nodeTimesFor(table, repository.getNodeTimes(table.id))
                rows.clear()
                rows.addAll(refreshed.map { NodeDraft(it.id, it.node, it.start, it.end) })
                showToast(context, context.getString(R.string.delete_success))
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
private fun ReminderSettingsScreen(
    repository: TimetableRepository,
    tableId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheduler = remember(context) { ReminderScheduler(context) }
    var settings by remember(tableId) { mutableStateOf(ReminderSettings()) }
    var status by remember { mutableStateOf(scheduler.permissionStatus()) }
    var startLead by remember(tableId) { mutableStateOf("10") }
    var endLead by remember(tableId) { mutableStateOf("0") }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember(tableId) { mutableStateOf(false) }
    var loaded by remember(tableId) { mutableStateOf(false) }
    var resultTitle by remember { mutableIntStateOf(R.string.course_reminders) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = scheduler.permissionStatus()
        if (granted) {
            scope.launch {
                runCatching { scheduler.rebuildCurrentTable() }
                    .onSuccess { report ->
                        status = report.permissionStatus
                        message = if (report.usedInexactFallback) {
                            context.getString(R.string.reminder_permission_allowed_fallback)
                        } else {
                            context.getString(R.string.reminder_permission_allowed_rebuilt, report.scheduledPlanIds.size)
                        }
                    }
                    .onFailure { message = localizedUiError(context, it, R.string.reminder_rebuild_failed) }
            }
        }
    }

    LaunchedEffect(tableId) {
        if (tableId > 0L) {
            settings = repository.getReminderSettings(tableId)
            startLead = settings.startLeadMinutes.toString()
            endLead = settings.endLeadMinutes.toString()
            status = scheduler.permissionStatus()
            loaded = true
        }
    }

    fun save() {
        if (saving || !loaded) return
        resultTitle = R.string.course_reminders
        val startMinutes = startLead.toIntOrNull()
        val endMinutes = endLead.toIntOrNull()
        if (startMinutes == null || startMinutes < 0 || endMinutes == null || endMinutes < 0) {
            message = context.getString(R.string.reminder_minutes_nonnegative)
            return
        }
        val updated = settings.copy(startLeadMinutes = startMinutes, endLeadMinutes = endMinutes)
        saving = true
        scope.launch {
            var persisted = false
            try {
                repository.saveReminderSettings(tableId, updated)
                persisted = true
                val report = scheduler.rebuildCurrentTable()
                status = report.permissionStatus
                resultTitle = R.string.reminder_settings_saved_title
                message = when {
                    !updated.startEnabled && !updated.endEnabled -> context.getString(R.string.reminder_saved_disabled)
                    !report.permissionStatus.notificationPermissionGranted || !report.permissionStatus.notificationsEnabled -> context.getString(R.string.reminder_saved_enable_notifications)
                    report.blockedReason != null -> context.getString(R.string.reminder_saved_unavailable)
                    report.usedInexactFallback -> context.getString(R.string.reminder_rebuilt_inexact)
                    report.scheduledPlanIds.isEmpty() -> context.getString(R.string.reminder_saved_no_upcoming)
                    else -> context.getString(R.string.reminder_rebuilt_count, report.scheduledPlanIds.size)
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                resultTitle = if (persisted) R.string.reminder_settings_saved_title else R.string.reminder_save_failed
                message = if (persisted) context.getString(R.string.reminder_saved_schedule_failed)
                    else localizedUiError(context, error, R.string.reminder_save_failed)
            } finally {
                saving = false
            }
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.course_reminders),
        onBack = onBack,
        actions = {
            TextButton(onClick = ::save, enabled = loaded && !saving) {
                Text(stringResource(if (saving) R.string.reminder_saving else R.string.save))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsGroup(stringResource(R.string.system_status)) {
                    val notificationText = when {
                        !status.notificationPermissionGranted -> stringResource(R.string.notification_permission_required)
                        !status.notificationsEnabled -> stringResource(R.string.notifications_disabled)
                        else -> stringResource(R.string.notifications_available)
                    }
                    SettingRow(
                        Icons.Default.Notifications,
                        stringResource(R.string.notifications),
                        notificationText,
                        tint = Color(0xFF8D62C8),
                        onClick = if (status.notificationPermissionGranted && !status.notificationsEnabled) {
                            {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                runCatching { context.startActivity(intent) }
                            }
                        } else {
                            null
                        },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notificationPermissionGranted) {
                        TextButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.allow_notifications)) }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        Icons.Default.AccessTime,
                        stringResource(R.string.exact_time),
                        if (status.exactAlarmAllowed) stringResource(R.string.available) else stringResource(R.string.inexact_time_description),
                        tint = Color(0xFF2AA69B),
                        onClick = {
                            ReminderScheduler.exactAlarmSettingsIntent(context)?.let { intent ->
                                runCatching { context.startActivity(intent) }
                            }
                        },
                    )
                    TextButton(onClick = { status = scheduler.permissionStatus() }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.refresh_system_status)) }
                }
            }
            item {
                SettingsGroup(stringResource(R.string.reminder_time)) {
                    SettingRow(Icons.Default.Notifications, stringResource(R.string.start_reminder), trailing = { Switch(checked = settings.startEnabled, onCheckedChange = { settings = settings.copy(startEnabled = it) }) })
                    OutlinedTextField(value = startLead, onValueChange = { startLead = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.minutes_before_start)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Notifications, stringResource(R.string.end_reminder), trailing = { Switch(checked = settings.endEnabled, onCheckedChange = { settings = settings.copy(endEnabled = it) }) })
                    OutlinedTextField(value = endLead, onValueChange = { endLead = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.minutes_before_end)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                SettingsGroup(stringResource(R.string.notification_content)) {
                    SettingRow(Icons.Default.Description, stringResource(R.string.course_name), trailing = { Switch(checked = settings.content.includeCourseName, onCheckedChange = { settings = settings.copy(content = settings.content.copy(includeCourseName = it)) }) })
                    SettingRow(Icons.Default.Person, stringResource(R.string.teacher), trailing = { Switch(checked = settings.content.includeTeacher, onCheckedChange = { settings = settings.copy(content = settings.content.copy(includeTeacher = it)) }) })
                    SettingRow(Icons.Default.Place, stringResource(R.string.location), trailing = { Switch(checked = settings.content.includeRoom, onCheckedChange = { settings = settings.copy(content = settings.content.copy(includeRoom = it)) }) })
                    SettingRow(Icons.Default.StickyNote2, stringResource(R.string.notes), trailing = { Switch(checked = settings.content.includeNote, onCheckedChange = { settings = settings.copy(content = settings.content.copy(includeNote = it)) }) })
                }
            }
            item {
                SettingsGroup(stringResource(R.string.notification_style)) {
                    SettingRow(Icons.Default.Notifications, stringResource(R.string.vibration), trailing = { Switch(checked = settings.vibrate && !settings.silent, enabled = !settings.silent, onCheckedChange = { settings = settings.copy(vibrate = it) }) })
                    SettingRow(Icons.Default.Notifications, stringResource(R.string.silent), trailing = { Switch(checked = settings.silent, onCheckedChange = { settings = settings.copy(silent = it) }) })
                }
            }
        }
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(stringResource(resultTitle)) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.got_it)) } },
        )
    }
}

@Composable
private fun ImportScreen(
    repository: TimetableRepository,
    currentTableId: Long,
    initialUri: Uri? = null,
    onInitialUriConsumed: () -> Unit = {},
    onBack: () -> Unit,
    onImported: (Long) -> Unit,
) {
    val context = LocalContext.current
    val adapter = remember(repository, context) { ImportExportAdapter(context, repository) }
    var format by rememberSaveable { mutableStateOf(ImportFormat.CSV) }
    var target by rememberSaveable { mutableStateOf(ImportTarget.CREATE_NEW) }
    val defaultImportTableName = stringResource(R.string.import_default_table_name)
    var tableName by rememberSaveable { mutableStateOf(defaultImportTableName) }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString()) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var sharedFileUri by remember { mutableStateOf<Uri?>(null) }
    var notice by remember { mutableStateOf<ImportNotice?>(null) }
    var loading by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val templateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { adapter.exportCsvTemplate(uri) }
                .onSuccess { notice = ImportNotice(context.getString(R.string.csv_template_title), context.getString(R.string.csv_template_saved)) {} }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    notice = ImportNotice(context.getString(R.string.export_failed), localizedUiError(context, error, R.string.export_failed)) {}
                }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingUri = it }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            try {
                val detected = adapter.detectFormat(initialUri)
                if (detected != null) {
                    format = detected
                    pendingUri = initialUri
                    sharedFileUri = null
                } else {
                    sharedFileUri = initialUri
                    notice = ImportNotice(context.getString(R.string.file_format), context.getString(R.string.import_choose_format)) {}
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                sharedFileUri = initialUri
                notice = ImportNotice(context.getString(R.string.import_failed), localizedUiError(context, error, R.string.file_import_failed)) {}
            }
            onInitialUriConsumed()
        }
    }

    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        val importTarget = target
        val firstDay = if (format == ImportFormat.CSV) Weeks.parseDate(startDate) else null
        if (format == ImportFormat.CSV && firstDay == null) {
            pendingUri = null
            notice = ImportNotice(context.getString(R.string.import_failed), context.getString(R.string.date_format_error)) {}
            return@LaunchedEffect
        }
        loading = true
        runCatching {
            adapter.importFile(
                uri = uri,
                format = format,
                target = importTarget,
                currentTableId = currentTableId.takeIf { it > 0L },
                options = ImportOptions(
                    name = tableName.trim().ifBlank { context.getString(R.string.import_default_table_name) },
                    firstDayEpochDay = firstDay?.toEpochDay(),
                ),
            )
        }.onSuccess { result ->
            val action = when (importTarget) {
                ImportTarget.CREATE_NEW -> context.getString(R.string.created_new_table)
                ImportTarget.MERGE_CURRENT -> context.getString(R.string.merged_current_table)
                ImportTarget.REPLACE_CURRENT -> context.getString(R.string.replaced_current_table)
            }
            notice = ImportNotice(context.getString(R.string.import_succeeded), action) { onImported(result.tableId) }
        }.onFailure { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            notice = ImportNotice(context.getString(R.string.import_failed), localizedUiError(context, error, R.string.file_import_failed)) {}
        }
        loading = false
        pendingUri = null
    }

    fun selectFile() {
        sharedFileUri?.let {
            pendingUri = it
            sharedFileUri = null
            return
        }
        val mimeTypes = when (format) {
            ImportFormat.CSV -> arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel")
            ImportFormat.JSON -> arrayOf("application/json", "text/plain")
            ImportFormat.WAKE_UP -> arrayOf("text/plain", "application/octet-stream")
            ImportFormat.ICS -> arrayOf("text/calendar", "text/plain")
        }
        launcher.launch(mimeTypes)
    }

    ScreenScaffold(title = stringResource(R.string.import_timetable), onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stringResource(R.string.file_format), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekTypeButton("CSV", format == ImportFormat.CSV, { format = ImportFormat.CSV }, Modifier.weight(1f))
                    WeekTypeButton("JSON", format == ImportFormat.JSON, { format = ImportFormat.JSON }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekTypeButton(stringResource(R.string.wakeup_backup), format == ImportFormat.WAKE_UP, { format = ImportFormat.WAKE_UP }, Modifier.weight(1f))
                    WeekTypeButton("ICS", format == ImportFormat.ICS, { format = ImportFormat.ICS }, Modifier.weight(1f))
                }
            }
            item {
                Text(stringResource(R.string.import_mode), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WeekTypeButton(stringResource(R.string.create_new_table), target == ImportTarget.CREATE_NEW, { target = ImportTarget.CREATE_NEW }, Modifier.weight(1f))
                    WeekTypeButton(stringResource(R.string.merge_current), target == ImportTarget.MERGE_CURRENT, { target = ImportTarget.MERGE_CURRENT }, Modifier.weight(1f))
                    WeekTypeButton(stringResource(R.string.replace_current), target == ImportTarget.REPLACE_CURRENT, { target = ImportTarget.REPLACE_CURRENT }, Modifier.weight(1f))
                }
            }
            if (format == ImportFormat.CSV) {
                item {
                    SettingsGroup(stringResource(R.string.csv_file)) {
                        OutlinedButton(onClick = { templateLauncher.launch("SleepDown-template.csv") }, enabled = !loading, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(stringResource(R.string.csv_template_download))
                        }
                        OutlinedTextField(value = tableName, onValueChange = { tableName = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), label = { Text(stringResource(R.string.table_name)) }, singleLine = true)
                        OutlinedTextField(value = startDate, onValueChange = { startDate = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), label = { Text(stringResource(R.string.first_monday)) }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                item {
                    SettingsGroup(
                        when (format) {
                            ImportFormat.JSON -> stringResource(R.string.sleepdown_json_backup)
                            ImportFormat.WAKE_UP -> stringResource(R.string.wakeup_five_line_backup)
                            ImportFormat.ICS -> stringResource(R.string.ics_calendar)
                            ImportFormat.CSV -> stringResource(R.string.csv_file)
                        },
                    ) {
                        val message = when (format) {
                            ImportFormat.JSON -> stringResource(R.string.json_import_description)
                            ImportFormat.WAKE_UP -> stringResource(R.string.wakeup_import_description)
                            ImportFormat.ICS -> stringResource(R.string.ics_import_description)
                            ImportFormat.CSV -> ""
                        }
                        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            item {
                Button(
                    onClick = { if (target == ImportTarget.REPLACE_CURRENT) confirmReplace = true else selectFile() },
                    enabled = !loading && (target == ImportTarget.CREATE_NEW || currentTableId > 0L),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (loading) R.string.importing else if (sharedFileUri != null) R.string.import_selected_file else R.string.choose_file))
                }
            }
        }
    }
    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text(stringResource(R.string.replace_current_title)) },
            text = { Text(stringResource(R.string.replace_current_message)) },
            confirmButton = { TextButton(onClick = { confirmReplace = false; selectFile() }) { Text(stringResource(R.string.continue_choose_file)) } },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text(stringResource(R.string.cancel)) } },
        )
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
                }) { Text(stringResource(R.string.got_it)) }
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
            pinMessage = context.getString(R.string.pin_widget_message)
        } else {
            pinMessage = context.getString(R.string.pin_widget_unsupported)
        }
    }

    ScreenScaffold(title = stringResource(R.string.widgets), onBack = onBack) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item {
                    SettingsGroup(stringResource(R.string.add_widget)) {
                        SettingRow(Icons.Default.Today, stringResource(R.string.nearby_courses), stringResource(R.string.nearby_courses_description), tint = Color(0xFF4E7BD9), onClick = { requestPin(com.letr.sleepdown.widget.ScheduleWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingRow(Icons.Default.List, stringResource(R.string.today_courses), stringResource(R.string.today_courses_description), tint = Color(0xFF2AA69B), onClick = { requestPin(com.letr.sleepdown.widget.TodayCourseWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingRow(Icons.Default.Widgets, stringResource(R.string.wide_today_courses), stringResource(R.string.wide_today_courses_description), tint = Color(0xFF8D62C8), onClick = { requestPin(com.letr.sleepdown.widget.TodayModernWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingRow(Icons.Default.DateRange, stringResource(R.string.two_column_today_courses), stringResource(R.string.two_column_today_courses_description), tint = Color(0xFFFF8A00), onClick = { requestPin(com.letr.sleepdown.widget.TodayAndNextDayWidgetReceiver::class.java) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingRow(Icons.Default.TableView, stringResource(R.string.week_courses), stringResource(R.string.week_courses_description), tint = Color(0xFFE53935), onClick = { requestPin(com.letr.sleepdown.widget.WeekScheduleWidgetReceiver::class.java) })
                    }
                }
                pinMessage?.let { message ->
                    item { Text(message, modifier = Modifier.padding(horizontal = 24.dp), color = Color(0xFF2AA69B), fontSize = 12.sp) }
                }
            }
            item {
                SettingsGroup(stringResource(R.string.usage)) {
                    SettingRow(Icons.Default.HelpOutline, stringResource(R.string.how_to_add_widget), stringResource(R.string.how_to_add_widget_description), tint = Color(0xFF2AA69B))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Tune, stringResource(R.string.how_to_resize_widget), stringResource(R.string.how_to_resize_widget_description), tint = Color(0xFF8D62C8))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Palette, stringResource(R.string.how_to_reconfigure_widget), stringResource(R.string.how_to_reconfigure_widget_description), tint = Color(0xFFFF8A00))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Refresh, stringResource(R.string.widget_update_time), stringResource(R.string.widget_update_time_description), tint = Color(0xFF4E7BD9))
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
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAction) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.new_label)) }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            Row {
                if (clearable && onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
                        Box(contentAlignment = Alignment.Center) { Text(stringResource(R.string.auto_color), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                (listOf(Color.Black.toArgbCompat(), Color.White.toArgbCompat()) + CourseColors.all()).distinct().forEach { color ->
                    Surface(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onSelect(color) }, color = CourseColors.asColor(color), border = androidx.compose.foundation.BorderStroke(if (color == initial) 3.dp else 1.dp, if (color == initial) MaterialTheme.colorScheme.onSurface else Color(0xFFDDDDDD))) {}
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
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
        title = { Text(stringResource(R.string.select_date)) },
        text = { DatePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialogFor(value: String, onSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val parsed = parseTime(value)
    val state = rememberTimePickerState(
        initialHour = parsed.hour,
        initialMinute = parsed.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onSelected("%02d:%02d".format(Locale.US, state.hour, state.minute))
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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

@Composable
private fun timeSummary(draft: TimeDraft, nodeTimes: List<NodeTimeEntity>): String {
    val day = stringResource(R.string.weekday_with_prefix, weekdayName(draft.day))
    val clock = if (draft.ownTime && draft.startTime.isNotBlank() && draft.endTime.isNotBlank()) {
        "${draft.startTime}-${draft.endTime}"
    } else {
        val start = nodeTimes.firstOrNull { it.node == draft.startNode }?.start.orEmpty()
        val end = nodeTimes.firstOrNull { it.node == draft.startNode + draft.step - 1 }?.end.orEmpty()
        if (start.isNotBlank() && end.isNotBlank()) "$start-$end" else ""
    }
    val clockSuffix = if (clock.isBlank()) "" else stringResource(R.string.time_summary_clock_suffix, clock)
    return stringResource(R.string.time_summary, day, draft.startNode, draft.startNode + draft.step - 1, clockSuffix)
}

@Composable
private fun selectedWeekLabel(selected: Set<Int>, maxWeek: Int): String {
    if (selected.isEmpty()) return stringResource(R.string.not_selected)
    val values = selected.filter { it in 1..maxWeek }.sorted()
    if (values.isEmpty()) return stringResource(R.string.not_selected)
    if (values.size == maxWeek && values.first() == 1 && values.last() == maxWeek) {
        return stringResource(R.string.full_week_range, maxWeek)
    }
    val contiguous = values.zipWithNext().all { (left, right) -> right == left + 1 }
    return if (contiguous) {
        stringResource(R.string.week_range, values.first(), values.last())
    } else if (values.size <= 4) {
        stringResource(R.string.selected_week_list, values.joinToString(stringResource(R.string.list_separator)))
    } else {
        stringResource(R.string.selected_weeks_count, values.size)
    }
}

@Composable
private fun courseTimeLabel(time: CourseTimeEntity, nodeTimes: List<NodeTimeEntity>): String {
    val start = nodeTimes.firstOrNull { it.node == time.startNode }?.start.orEmpty()
    val end = nodeTimes.firstOrNull { it.node == time.startNode + time.step - 1 }?.end.orEmpty()
    val periods = stringResource(R.string.period_range, time.startNode, time.startNode + time.step - 1)
    return if (start.isNotBlank() && end.isNotBlank()) {
        stringResource(R.string.period_range_with_clock, periods, "$start-$end")
    } else {
        periods
    }
}

private fun courseColor(course: CourseEntity): Int = if (course.color == 0) CourseColors.colorFor(course.name) else course.color

private fun currentWeek(table: TableEntity): Int {
    val days = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(table.startDate), LocalDate.now())
    return Math.floorDiv(days, 7L).toInt() + 1
}

private fun dateFor(table: TableEntity, week: Int, day: Int): LocalDate {
    val base = LocalDate.ofEpochDay(table.startDate)
    return base.plusWeeks((week - 1).toLong()).plusDays((day - 1).toLong())
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

@Composable
private fun weekdayName(day: Int): String = when (((day - 1) % 7 + 7) % 7) {
    0 -> stringResource(R.string.weekday_short_monday)
    1 -> stringResource(R.string.weekday_short_tuesday)
    2 -> stringResource(R.string.weekday_short_wednesday)
    3 -> stringResource(R.string.weekday_short_thursday)
    4 -> stringResource(R.string.weekday_short_friday)
    5 -> stringResource(R.string.weekday_short_saturday)
    else -> stringResource(R.string.weekday_short_sunday)
}

@Composable
private fun weekTypeLabel(type: Int): String = when (type) {
    CourseTimeEntity.TYPE_ODD -> stringResource(R.string.odd_week)
    CourseTimeEntity.TYPE_EVEN -> stringResource(R.string.even_week)
    else -> stringResource(R.string.every_week)
}

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

private fun customTimeRangeValid(start: String, end: String): Boolean {
    if (start.isBlank() || end.isBlank()) return false
    val startTime = runCatching { LocalTime.parse(start, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull() ?: return false
    val endTime = runCatching { LocalTime.parse(end, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull() ?: return false
    return startTime.isBefore(endTime)
}

private fun Color.toArgbCompat(): Int {
    val red = (red * 255f).toInt().coerceIn(0, 255)
    val green = (green * 255f).toInt().coerceIn(0, 255)
    val blue = (blue * 255f).toInt().coerceIn(0, 255)
    val alpha = (alpha * 255f).toInt().coerceIn(0, 255)
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
