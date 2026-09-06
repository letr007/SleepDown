package com.letr.sleepdown.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.onClick as semanticsOnClick
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import com.letr.sleepdown.R
import com.letr.sleepdown.data.CourseEntity
import com.letr.sleepdown.data.ReusableTimeTableEntity
import com.letr.sleepdown.data.TableEntity
import com.letr.sleepdown.data.TimetableRepository
import com.letr.sleepdown.domain.ConflictGroup
import com.letr.sleepdown.domain.ConflictPreference
import com.letr.sleepdown.domain.CourseOccurrence
import com.letr.sleepdown.domain.EpochDayRange
import com.letr.sleepdown.domain.GridPlacement
import com.letr.sleepdown.domain.MinuteOfDay
import com.letr.sleepdown.domain.MinuteRange
import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.TimeTableValidationCode
import com.letr.sleepdown.domain.TimeTableValidationIssue
import com.letr.sleepdown.domain.TimeTableValidator
import com.letr.sleepdown.domain.Timetable
import com.letr.sleepdown.domain.TimetableCommand
import com.letr.sleepdown.domain.TimetableCommands
import com.letr.sleepdown.domain.TimetableEngine
import com.letr.sleepdown.reminder.ReminderScheduler
import com.letr.sleepdown.widget.refreshScheduleWidgets
import coil.compose.AsyncImage
import com.letr.sleepdown.logic.CourseColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val SleepDownUiAccent = Color(0xFFFF2D55)

private data class AggregateGestureResult(val moved: Boolean)

private suspend fun PointerInputScope.detectTableReorder(
    onDragStart: (Offset) -> Unit,
    onDragCancel: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        onDragStart(held.position)
        var released = false
        try {
            while (true) {
                // Claim the held drag before the list's scroll handler sees it.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == held.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    released = true
                    break
                }
                onDrag(change, change.position - change.previousPosition)
            }
        } finally {
            if (released) onDragEnd() else onDragCancel()
        }
    }
}

internal enum class CourseMoveScope { THIS_WEEK, ALL_WEEKS }

private data class PendingCourseMove(
    val original: Timetable,
    val occurrence: CourseOccurrence,
    val targetDay: Int,
    val targetNode: Int,
)

internal data class CourseGridSelection(val day: Int, val anchorNode: Int, val cursorNode: Int) {
    val startNode: Int get() = minOf(anchorNode, cursorNode)
    val endNode: Int get() = maxOf(anchorNode, cursorNode)

    fun resizeBy(delta: Int, nodeCount: Int): CourseGridSelection =
        copy(cursorNode = (cursorNode + delta).coerceIn(1, nodeCount))
}

private data class AggregateNodeDraft(
    val node: Int,
    val start: String,
    val end: String,
)

private enum class AggregateDeleteScope {
    OCCURRENCE,
    FROM_WEEK,
    LOGICAL_SLOT,
    COURSE,
}

private data class AggregateNameDialogState(
    val mode: String,
    val initial: String,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AggregateScheduleScreen(
    repository: TimetableRepository,
    table: TableEntity,
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
    courses: List<CourseEntity>,
    onEditCourse: (CourseEntity) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var timetable by remember(table.id) { mutableStateOf<Timetable?>(null) }
    var savingMove by remember(table.id) { mutableStateOf(false) }
    var pendingMove by remember(table.id) { mutableStateOf<PendingCourseMove?>(null) }
    var tablePicker by remember { mutableStateOf(false) }
    var weekPicker by remember { mutableStateOf(false) }
    var detailsOccurrence by remember { mutableStateOf<CourseOccurrence?>(null) }
    var detailsConflictGroup by remember { mutableStateOf<ConflictGroup?>(null) }
    var selectedWeek by remember(table.id) { mutableIntStateOf(currentWeekFor(table)) }
    val maxWeek = timetable?.maxWeek ?: table.maxWeek
    val initialPage = (selectedWeek - 1).coerceIn(0, maxWeek.coerceAtLeast(1) - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { maxWeek.coerceAtLeast(1) })
    val pagerScope = rememberCoroutineScope()

    suspend fun reload() {
        timetable = repository.loadDomainTimetableOrNull(table.id)
    }

    LaunchedEffect(table.id) {
        selectedWeek = currentWeekFor(table).coerceIn(1, table.maxWeek.coerceAtLeast(1))
        reload()
    }
    LaunchedEffect(table.startDate, table.maxWeek) {
        selectedWeek = currentWeekFor(table).coerceIn(1, table.maxWeek.coerceAtLeast(1))
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedWeek = pagerState.currentPage + 1
    }
    LaunchedEffect(maxWeek) {
        if (pagerState.currentPage >= maxWeek.coerceAtLeast(1)) {
            pagerState.scrollToPage(maxWeek.coerceAtLeast(1) - 1)
        }
    }

    val domain = timetable
    if (domain == null) {
        AggregateLoadingView()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AggregateScheduleBackground(table)
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            AggregateScheduleToolbar(
                table = table,
                week = pagerState.currentPage + 1,
                currentWeek = currentWeekFor(table),
                maxWeek = domain.maxWeek,
                weekStart = dateFor(domain, pagerState.currentPage + 1),
                onWeekClick = { weekPicker = true },
                onAdd = { onAddCourse(1, 1, 2) },
                onImport = onOpenImport,
                onShare = onShare,
                onSwitchTable = { tablePicker = true },
                onManage = onOpenManage,
                onSettings = onOpenSettings,
                onAppearance = onOpenAppearance,
                onCourseManage = onOpenCourseManage,
                onTimeTable = onOpenTimeTable,
                onWidgetHelp = onOpenWidgetHelp,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 0,
            ) { page ->
                AggregateSchedulePage(
                    table = table,
                    timetable = domain,
                    week = page + 1,
                    isActivePage = page == pagerState.currentPage,
                    currentWeek = currentWeekFor(table),
                    onAddCourse = onAddCourse,
                    movingEnabled = !savingMove && pendingMove == null,
                    onOpenOccurrence = { occurrence, group ->
                        if (!savingMove && pendingMove == null) {
                            detailsOccurrence = occurrence
                            detailsConflictGroup = group
                        }
                    },
                    onMoveOccurrence = move@{ occurrence, targetDay, targetNode ->
                        if (savingMove || pendingMove != null ||
                            (targetDay == occurrence.dayOfWeek && targetNode == occurrence.startNode)) return@move
                        try {
                            val commands = aggregateMoveCommands(domain, occurrence, targetDay, targetNode, CourseMoveScope.THIS_WEEK, context)
                            timetable = commands.fold(domain, TimetableCommands::apply)
                            pendingMove = PendingCourseMove(domain, occurrence, targetDay, targetNode)
                        } catch (error: Exception) {
                            aggregateToast(context, localizedUiError(context, error, R.string.aggregate_course_move_failed))
                        }
                    },
                )
            }
        }
    }

    pendingMove?.let { move ->
        fun cancelMove() {
            timetable = move.original
            pendingMove = null
        }
        fun confirmMove(moveScope: CourseMoveScope) {
            if (savingMove) return
            val commands: List<TimetableCommand>
            try {
                commands = aggregateMoveCommands(move.original, move.occurrence, move.targetDay, move.targetNode, moveScope, context)
                timetable = commands.fold(move.original, TimetableCommands::apply)
            } catch (error: Exception) {
                cancelMove()
                aggregateToast(context, localizedUiError(context, error, R.string.aggregate_course_move_failed))
                return
            }
            pendingMove = null
            savingMove = true
            scope.launch {
                try {
                    timetable = repository.applyTimetableCommands(table.id, commands)
                } catch (error: CancellationException) {
                    timetable = move.original
                    throw error
                } catch (error: Exception) {
                    timetable = move.original
                    aggregateToast(context, localizedUiError(context, error, R.string.aggregate_course_move_failed))
                    return@launch
                } finally {
                    savingMove = false
                }
                aggregateToast(context, R.string.aggregate_course_moved)
                try {
                    ReminderScheduler(context).rebuildCurrentTable()
                    refreshScheduleWidgets(context)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    android.util.Log.e("CourseMove", "Failed to refresh reminders or widgets after moving a course", error)
                    aggregateToast(context, R.string.aggregate_course_move_refresh_failed)
                }
            }
        }
        AlertDialog(
            onDismissRequest = ::cancelMove,
            title = { Text(stringResource(R.string.course_move_scope_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(move.occurrence.courseName)
                    Button(onClick = { confirmMove(CourseMoveScope.THIS_WEEK) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.course_move_this_week))
                    }
                    TextButton(onClick = { confirmMove(CourseMoveScope.ALL_WEEKS) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.course_move_all_weeks))
                    }
                    Text(stringResource(R.string.course_move_scope_hint), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = ::cancelMove) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (weekPicker) {
        AggregateWeekPickerDialog(
            maxWeek = domain.maxWeek,
            selectedWeek = pagerState.currentPage + 1,
            currentWeek = currentWeekFor(table),
            onSelect = { week ->
                weekPicker = false
                pagerScope.launch { pagerState.animateScrollToPage(week - 1) }
            },
            onDismiss = { weekPicker = false },
        )
    }
    if (tablePicker) {
        AggregateTablePickerDialog(
            tables = tables,
            selectedId = currentTableId,
            onSelect = { id -> tablePicker = false; onSelectTable(id) },
            onDismiss = { tablePicker = false },
        )
    }
    val occurrence = detailsOccurrence
    if (occurrence != null) {
        val group = detailsConflictGroup
            ?: TimetableEngine.conflictGroups(
                TimetableEngine.expandOccurrences(domain, EpochDayRange(occurrence.epochDay, occurrence.epochDay)),
                domain.conflictPreferences,
            ).firstOrNull { it.occurrences.any { item -> item.id == occurrence.id } }
        AggregateOccurrenceDetailsSheet(
            table = table,
            timetable = domain,
            occurrence = occurrence,
            conflictGroup = group,
            onSelectOccurrence = { selected ->
                detailsOccurrence = selected
                detailsConflictGroup = group
            },
            onSetPreferred = { preferred ->
                scope.launch {
                    runCatching {
                        repository.saveConflictPreference(
                            table.id,
                            ConflictPreference(
                                courseId = preferred.courseId,
                                logicalSlotId = preferred.logicalSlotId,
                                occurrenceId = preferred.id,
                                epochDay = preferred.epochDay,
                                priority = 1,
                            ),
                            conflictingOccurrenceIds = group?.occurrences
                                ?.mapTo(mutableSetOf()) { it.id }
                                .orEmpty(),
                        )
                        repository.loadDomainTimetable(table.id)
                    }.onSuccess {
                        timetable = it
                        detailsConflictGroup = null
                        refreshScheduleWidgets(context)
                        aggregateToast(context, R.string.aggregate_preferred_set)
                    }.onFailure { error -> aggregateToast(context, localizedUiError(context, error, R.string.aggregate_preferred_save_failed)) }
                }
            },
            onDelete = { command ->
                detailsOccurrence = null
                detailsConflictGroup = null
                scope.launch {
                    runCatching {
                        repository.applyTimetableCommand(table.id, command).also {
                            ReminderScheduler(context).rebuildCurrentTable()
                            refreshScheduleWidgets(context)
                        }
                    }.onSuccess {
                        timetable = it
                        aggregateToast(context, R.string.aggregate_deleted)
                    }.onFailure { error -> aggregateToast(context, localizedUiError(context, error, R.string.aggregate_delete_failed)) }
                }
            },
            onReschedule = { command ->
                detailsOccurrence = null
                detailsConflictGroup = null
                scope.launch {
                    runCatching {
                        repository.applyTimetableCommand(table.id, command).also {
                            ReminderScheduler(context).rebuildCurrentTable()
                            refreshScheduleWidgets(context)
                        }
                    }
                        .onSuccess {
                            timetable = it
                            aggregateToast(context, R.string.aggregate_reschedule_saved)
                        }
                        .onFailure { error -> aggregateToast(context, localizedUiError(context, error, R.string.aggregate_reschedule_failed)) }
                }
            },
            onEditCourse = {
                detailsOccurrence = null
                detailsConflictGroup = null
                courses.firstOrNull { it.id.toString() == occurrence.courseId }
                    ?.let(onEditCourse)
            },
            onDismiss = {
                detailsOccurrence = null
                detailsConflictGroup = null
            },
        )
    }
}

@Composable
private fun AggregateScheduleBackground(table: TableEntity) {
    val value = table.bgImageUri.orEmpty()
    val startColor = if (value.startsWith("#")) aggregateParseColor(value, Color(0xFFE8E8F4)) else Color(0xFFE8E8F4)
    val endColor = if (value.startsWith("#")) aggregateParseColor(value, Color(0xFFBECEE5)) else Color(0xFFBECEE5)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(startColor, endColor))),
    ) {
        if (value.isNotBlank() && !value.startsWith("#")) {
            AsyncImage(model = value, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AggregateScheduleToolbar(
    table: TableEntity,
    week: Int,
    currentWeek: Int,
    maxWeek: Int,
    weekStart: LocalDate,
    onWeekClick: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onSwitchTable: () -> Unit,
    onManage: () -> Unit,
    onSettings: () -> Unit,
    onAppearance: () -> Unit,
    onCourseManage: () -> Unit,
    onTimeTable: () -> Unit,
    onWidgetHelp: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val progress = (week.toFloat() / maxWeek.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val rangeText = "${weekStart.monthValue}/${weekStart.dayOfMonth} - ${weekStart.plusDays(6).monthValue}/${weekStart.plusDays(6).dayOfMonth}"
    val currentWeekSuffix = if (week == currentWeek) stringResource(R.string.aggregate_current_week_suffix) else ""
    Column {
        TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable(onClick = onWeekClick)) {
                        Text(table.name.ifBlank { stringResource(R.string.default_table_name) }, color = Color(table.textColor.toInt()), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.aggregate_week_status, week, rangeText, currentWeekSuffix),
                            color = Color(table.textColor.toInt()).copy(alpha = 0.76f),
                            fontSize = 12.sp,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(table.textColor.toInt()).copy(alpha = 0.16f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(SleepDownUiAccent, RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onWeekClick) {
                        Icon(Icons.Default.Today, contentDescription = stringResource(R.string.select_week), tint = Color(table.textColor.toInt()))
                    }
                },
                actions = {
                    IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course), tint = Color(table.textColor.toInt())) }
                    IconButton(onClick = onImport) { Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.import_timetable), tint = Color(table.textColor.toInt())) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more), tint = Color(table.textColor.toInt())) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.switch_table)) }, onClick = { menu = false; onSwitchTable() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.backup_export)) }, onClick = { menu = false; onShare() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.table_management)) }, onClick = { menu = false; onManage() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.table_settings)) }, onClick = { menu = false; onSettings() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.table_appearance)) }, onClick = { menu = false; onAppearance() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.course_management)) }, onClick = { menu = false; onCourseManage() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.time_table)) }, onClick = { menu = false; onTimeTable() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.widgets)) }, onClick = { menu = false; onWidgetHelp() })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
    }
}

@Composable
private fun AggregateSchedulePage(
    table: TableEntity,
    timetable: Timetable,
    week: Int,
    isActivePage: Boolean,
    currentWeek: Int,
    onAddCourse: (Int, Int, Int) -> Unit,
    onOpenOccurrence: (CourseOccurrence, ConflictGroup?) -> Unit,
    onMoveOccurrence: (CourseOccurrence, Int, Int) -> Unit,
    movingEnabled: Boolean,
) {
    val days = aggregateVisibleDays(table)
    val weekRange = aggregateWeekRange(timetable, week)
    val occurrences = TimetableEngine.expandOccurrences(timetable, weekRange)
        .filter { it.dayOfWeek in days }
    val conflicts = TimetableEngine.conflictGroups(occurrences, timetable.conflictPreferences)
    val conflictByOccurrence = conflicts.flatMap { group -> group.occurrences.map { it.id to group } }.toMap()
    val placements = TimetableEngine.gridPlacements(occurrences, timetable.timeTable, timetable.conflictPreferences)
    val nodeCount = max(table.nodeCount, timetable.timeTable.nodes.maxOfOrNull { it.node } ?: 0).coerceIn(1, 60)
    val rowHeight = (table.itemHeightDp.coerceIn(32, 128) + 2).dp
    val bodyScroll = rememberScrollState()
    val gridHeight = rowHeight * nodeCount.toFloat()
    val headerColor = Color(table.textColor.toInt())
    val weekStart = LocalDate.ofEpochDay(weekRange.startEpochDay)
    val uiSettings = LocalSleepDownUiSettings.current
    val bottomSpacing = uiSettings.bottomSpacingDp.dp
    val scrollContentHeight = gridHeight + bottomSpacing
    var selection by remember(table.id, week, nodeCount, days) { mutableStateOf<CourseGridSelection?>(null) }
    LaunchedEffect(isActivePage) {
        if (!isActivePage) selection = null
    }
    BackHandler(enabled = isActivePage && selection != null) { selection = null }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 6.dp, bottom = 6.dp)) {
            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.aggregate_month_label, weekStart.monthValue), color = headerColor, fontSize = table.headerTextSize.coerceIn(8, 28).sp, fontWeight = FontWeight.Bold)
            }
            days.forEach { day ->
                val dayDate = LocalDate.ofEpochDay(weekRange.startEpochDay + day - 1L)
                val isToday = week == currentWeek && dayDate == LocalDate.now()
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.aggregate_weekday_date, aggregateWeekdayName(day), dayDate.monthValue, dayDate.dayOfMonth),
                        color = headerColor.copy(alpha = if (isToday) 1f else 0.64f),
                        fontSize = table.headerTextSize.coerceIn(8, 28).sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = (table.headerTextSize + 1).sp,
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(bodyScroll),
            ) {
                Column(modifier = Modifier.width(48.dp).height(scrollContentHeight)) {
                    (1..nodeCount).forEach { node ->
                        val time = timetable.timeTable.node(node)
                        Column(modifier = Modifier.fillMaxWidth().height(rowHeight), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("$node", color = headerColor, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                            if (table.showTimeBar && time != null) {
                                Text(formatMinuteForUi(time.startMinuteOfDay), color = headerColor.copy(alpha = 0.6f), fontSize = 9.sp, lineHeight = 12.sp)
                                Text(formatMinuteForUi(time.endMinuteOfDay), color = headerColor.copy(alpha = 0.6f), fontSize = 9.sp, lineHeight = 12.sp)
                            }
                        }
                    }
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).height(scrollContentHeight).padding(end = 8.dp)) {
                    val columnWidth = if (days.isEmpty()) 0.dp else maxWidth / days.size
                    val dayLabels = days.map { stringResource(R.string.weekday_with_prefix, aggregateWeekdayName(it)) }
                    Column(modifier = Modifier.height(gridHeight)) {
                        repeat(nodeCount) { rowIndex ->
                            val periodLabel = stringResource(R.string.period_range, rowIndex + 1, rowIndex + 1)
                            Box(modifier = Modifier.fillMaxWidth().height(rowHeight)
                                .drawBehind {
                                    if (table.showGrid) {
                                        val stroke = 0.5.dp.toPx()
                                        val ink = headerColor.copy(alpha = 0.12f)
                                        drawLine(ink, Offset.Zero, Offset(size.width, 0f), stroke)
                                        drawLine(ink, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                                        for (column in 0..days.size) {
                                            val x = size.width * column / days.size
                                            drawLine(ink, Offset(x, 0f), Offset(x, size.height), stroke)
                                        }
                                    }
                                }
                                .pointerInput(days, rowIndex) {
                                    detectTapGestures { point ->
                                        val column = (point.x / (size.width.toFloat() / days.size)).toInt().coerceIn(0, days.lastIndex)
                                        selection = CourseGridSelection(days[column], rowIndex + 1, rowIndex + 1)
                                    }
                                }
                                .semantics {
                                    contentDescription = periodLabel
                                    customActions = days.mapIndexed { index, day ->
                                        CustomAccessibilityAction(dayLabels[index]) {
                                            selection = CourseGridSelection(day, rowIndex + 1, rowIndex + 1)
                                            true
                                        }
                                    }
                                })
                        }
                    }
                    occurrences.forEach { occurrence ->
                        val dayColumn = days.indexOf(occurrence.dayOfWeek)
                        if (dayColumn >= 0) {
                            val overlapPlacement = placements[occurrence.id]
                            val nodePlacement = fallbackPlacement(occurrence, nodeCount)
                            val placement = if (occurrence.usesCustomTime) {
                                overlapPlacement ?: nodePlacement
                            } else {
                                nodePlacement.copy(
                                    leftFraction = overlapPlacement?.leftFraction ?: 0.0,
                                    widthFraction = overlapPlacement?.widthFraction ?: 1.0,
                                    column = overlapPlacement?.column ?: 0,
                                    columnCount = overlapPlacement?.columnCount ?: 1,
                                )
                            }
                            AggregateOccurrenceCard(
                                occurrence = occurrence,
                                conflictGroup = conflictByOccurrence[occurrence.id],
                                table = table,
                                columnWidth = columnWidth,
                                dayColumn = dayColumn,
                                placement = placement,
                                gridHeight = gridHeight,
                                rowHeight = rowHeight,
                                scrollState = bodyScroll,
                                nodeCount = nodeCount,
                                visibleDays = days,
                                movingEnabled = movingEnabled,
                                onClick = {
                                    selection = null
                                    onOpenOccurrence(occurrence, conflictByOccurrence[occurrence.id])
                                },
                                onMove = { day, node ->
                                    selection = null
                                    onMoveOccurrence(occurrence, day, node)
                                },
                            )
                        }
                    }
                    selection?.let { selected ->
                        val x = columnWidth * days.indexOf(selected.day).toFloat()
                        val height = rowHeight * (selected.endNode - selected.startNode + 1).toFloat()
                        val rangeLabel = stringResource(R.string.period_range, selected.startNode, selected.endNode)
                        val density = LocalDensity.current
                        val rowHeightPx = with(density) { rowHeight.toPx() }
                        val accent = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .offset(x = x + 1.dp, y = rowHeight * (selected.startNode - 1).toFloat() + 1.dp)
                                .width((columnWidth - 2.dp).coerceAtLeast(1.dp))
                                .height(height - 2.dp)
                                .zIndex(3f)
                                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                .border(2.dp, accent, RoundedCornerShape(6.dp))
                                .semantics { stateDescription = rangeLabel }
                                .clickable {
                                    selection = null
                                    onAddCourse(selected.day, selected.startNode, selected.endNode - selected.startNode + 1)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course), tint = accent)
                        }
                        val handleX = if (days.indexOf(selected.day) == days.lastIndex) {
                            x - 32.dp
                        } else {
                            x + columnWidth - 4.dp
                        }
                        val handleY = rowHeight * (selected.startNode - 1).toFloat() + (height - 72.dp) / 2
                        SelectionResizeHandle(
                            modifier = Modifier
                                .offset(x = handleX, y = handleY.coerceIn(0.dp, (gridHeight - 72.dp).coerceAtLeast(0.dp)))
                                .semantics { stateDescription = rangeLabel },
                            rowHeightPx = rowHeightPx,
                            onDragRows = { delta -> selection = selection?.resizeBy(delta, nodeCount) },
                        )
                    }
                }
            }
            if (occurrences.isEmpty() && selection == null && uiSettings.showEmptyImage) {
                if (uiSettings.emptyImageUri.isNotBlank()) {
                    AsyncImage(
                        model = uiSettings.emptyImageUri,
                        contentDescription = stringResource(R.string.empty_schedule_image),
                        modifier = Modifier.align(Alignment.Center).size(160.dp).alpha(0.72f),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(72.dp),
                        tint = headerColor.copy(alpha = 0.24f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AggregateOccurrenceCard(
    occurrence: CourseOccurrence,
    conflictGroup: ConflictGroup?,
    table: TableEntity,
    columnWidth: Dp,
    dayColumn: Int,
    placement: GridPlacement,
    gridHeight: Dp,
    rowHeight: Dp,
    scrollState: androidx.compose.foundation.ScrollState,
    nodeCount: Int,
    visibleDays: List<Int>,
    onClick: () -> Unit,
    onMove: (Int, Int) -> Unit,
    movingEnabled: Boolean,
) {
    val density = LocalDensity.current
    val widthFraction = placement.widthFraction.toFloat().coerceIn(0.05f, 1f)
    val cardWidth = (columnWidth * widthFraction - 3.dp).coerceAtLeast(18.dp)
    val cardHeight = (gridHeight * placement.heightFraction.toFloat() - 3.dp).coerceAtLeast(28.dp)
    val x = columnWidth * (dayColumn + placement.leftFraction.toFloat()) + 1.dp
    val y = gridHeight * placement.topFraction.toFloat() + 1.dp
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val gridHeightPx = with(density) { gridHeight.toPx() }
    val columnWidthPx = with(density) { columnWidth.toPx() }
    var dragging by remember(occurrence.id) { mutableStateOf(false) }
    var dragX by remember(occurrence.id) { mutableFloatStateOf(0f) }
    var dragY by remember(occurrence.id) { mutableFloatStateOf(0f) }
    var dragStartScroll by remember(occurrence.id) { mutableFloatStateOf(0f) }
    var dragChanged by remember(occurrence.id) { mutableStateOf(false) }
    val base = CourseColors.asColor(if (occurrence.color == 0) CourseColors.colorFor(occurrence.courseName) else occurrence.color)
    val textColor = Color(table.courseTextColor)
    val conflict = conflictGroup?.hasConflict == true
    val stroke = if (conflict) SleepDownUiAccent else Color(table.strokeColor)
    val clock = "${MinuteOfDay.format(occurrence.startMinuteOfDay)}-${MinuteOfDay.format(occurrence.endMinuteOfDay)}"
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentMovingEnabled by rememberUpdatedState(movingEnabled)

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .graphicsLayer {
                translationX = dragX
                translationY = dragY + if (dragging) scrollState.value - dragStartScroll else 0f
                scaleX = if (dragging) 1.05f else 1f
                scaleY = if (dragging) 1.05f else 1f
            }
            .zIndex(if (dragging || conflict) 2f else 1f)
            .semantics(mergeDescendants = true) {
                semanticsOnClick { currentOnClick(); true }
            }
            .then(
                Modifier.pointerInput(occurrence, visibleDays, dayColumn, nodeCount, columnWidthPx, rowHeightPx, gridHeightPx, y, cardHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hold: AggregateGestureResult? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            var totalX = 0f
                            var totalY = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: return@withTimeoutOrNull AggregateGestureResult(moved = true)
                                if (!change.pressed) {
                                    return@withTimeoutOrNull AggregateGestureResult(
                                        moved = abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop,
                                    )
                                }
                                totalX += change.position.x - change.previousPosition.x
                                totalY += change.position.y - change.previousPosition.y
                                if (abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop) {
                                    return@withTimeoutOrNull AggregateGestureResult(moved = true)
                                }
                            }
                            AggregateGestureResult(moved = true)
                        }
                        if (hold != null && !hold.moved) {
                            currentOnClick()
                        } else if (hold == null && currentMovingEnabled) {
                            down.consume()
                            dragging = true
                            dragStartScroll = scrollState.value.toFloat()
                            dragX = 0f
                            dragY = 0f
                            dragChanged = false
                            var completed = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        completed = true
                                        break
                                    }
                                    change.consume()
                                    val dx = change.position.x - change.previousPosition.x
                                    val dy = change.position.y - change.previousPosition.y
                                    dragX += dx
                                    dragY += dy
                                    if (dx != 0f || dy != 0f) dragChanged = true
                                    if (scrollState.maxValue > 0) {
                                        val scrollDelta = scrollState.value - dragStartScroll
                                        val top = (y.value * density.density) + dragY + scrollDelta
                                        val bottom = top + cardHeight.value * density.density
                                        val viewportTop = scrollState.value.toFloat()
                                        val viewportBottom = viewportTop + gridHeightPx
                                        val edge = with(density) { 48.dp.toPx() }
                                        val scrollBy = when {
                                            top < viewportTop + edge -> -24f
                                            bottom > viewportBottom - edge -> 24f
                                            else -> 0f
                                        }
                                        if (scrollBy != 0f) scrollState.dispatchRawDelta(scrollBy)
                                    }
                                }
                                if (completed && dragChanged && columnWidthPx > 0f && rowHeightPx > 0f) {
                                    val targetColumn = (dayColumn + (dragX / columnWidthPx).roundToInt()).coerceIn(0, visibleDays.lastIndex.coerceAtLeast(0))
                                    val targetDay = visibleDays.getOrNull(targetColumn)
                                    val maxStart = (nodeCount - occurrence.nodeCount + 1).coerceAtLeast(1)
                                    val targetNode = (occurrence.startNode + ((dragY + (scrollState.value - dragStartScroll)) / rowHeightPx).roundToInt()).coerceIn(1, maxStart)
                                    if (targetDay != null) currentOnMove(targetDay, targetNode)
                                }
                            } finally {
                                dragging = false
                                dragX = 0f
                                dragY = 0f
                                dragStartScroll = 0f
                                dragChanged = false
                            }
                        }
                    }
                },
            )
            .width(cardWidth)
            .height(cardHeight)
            .alpha(if (occurrence.isRescheduled) 0.96f else 1f)
            .background(base.copy(alpha = table.itemAlpha.coerceIn(0f, 1f)), RoundedCornerShape(table.radius.coerceIn(0, 32).dp))
            .border(if (conflict) 2.dp else 1.dp, stroke, RoundedCornerShape(table.radius.coerceIn(0, 32).dp))
            .padding(4.dp),
    ) {
        val compactConflict = placement.columnCount > 1
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
            Text(
                occurrence.courseName,
                color = textColor,
                fontSize = if (compactConflict) 10.sp else table.itemTextSize.coerceIn(8f, 24f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (compactConflict) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compactConflict && table.showTime) Text(clock, color = textColor, fontSize = 10.sp, maxLines = 1)
            if (!compactConflict && table.showTeacher && occurrence.teacher.isNotBlank()) Text(occurrence.teacher, color = textColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compactConflict && table.showLocation && occurrence.room.isNotBlank()) Text(occurrence.room, color = textColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (occurrence.isRescheduled) Text(stringResource(R.string.rescheduled), color = textColor, fontSize = 9.sp)
            if (conflictGroup?.hasConflict == true) {
                Text(stringResource(R.string.conflict_count, conflictGroup.occurrences.size), color = SleepDownUiAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AggregateOccurrenceDetailsSheet(
    table: TableEntity,
    timetable: Timetable,
    occurrence: CourseOccurrence,
    conflictGroup: ConflictGroup?,
    onSelectOccurrence: (CourseOccurrence) -> Unit,
    onSetPreferred: (CourseOccurrence) -> Unit,
    onDelete: (TimetableCommand) -> Unit,
    onReschedule: (TimetableCommand) -> Unit,
    onEditCourse: () -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteScope by remember(occurrence.id) { mutableStateOf(AggregateDeleteScope.OCCURRENCE) }
    var showDelete by remember(occurrence.id) { mutableStateOf(false) }
    var reschedule by remember(occurrence.id) { mutableStateOf(false) }
    val conflicts = conflictGroup?.occurrences.orEmpty()
    val isPreferred = conflictGroup?.preferredOccurrenceId == occurrence.id
    val date = LocalDate.ofEpochDay(occurrence.epochDay)
    val sourceWeek = aggregateSourceWeek(timetable, occurrence, LocalContext.current)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(occurrence.courseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.aggregate_date_week, date.monthValue, date.dayOfMonth, aggregateWeekdayName(occurrence.dayOfWeek)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                if (conflicts.size > 1 && isPreferred) Icon(Icons.Default.Star, contentDescription = stringResource(R.string.preferred_course), tint = SleepDownUiAccent)
            }
            Text(stringResource(R.string.occurrence_time, MinuteOfDay.format(occurrence.startMinuteOfDay), MinuteOfDay.format(occurrence.endMinuteOfDay), occurrence.startNode, occurrence.startNode + occurrence.nodeCount - 1))
            if (occurrence.teacher.isNotBlank()) Text(stringResource(R.string.teacher_value, occurrence.teacher))
            if (occurrence.room.isNotBlank()) Text(stringResource(R.string.room_value, occurrence.room))
            if (occurrence.note.isNotBlank()) Text(stringResource(R.string.note_value, occurrence.note))
            if (occurrence.isRescheduled) Text(stringResource(R.string.single_day_reschedule), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

            if (conflicts.size > 1) {
                HorizontalDivider()
                Text(stringResource(R.string.time_conflict), fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    conflicts.forEach { item ->
                        if (item.id == occurrence.id) Button(onClick = {}, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(item.courseName) }
                        else OutlinedButton(onClick = { onSelectOccurrence(item) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(item.courseName) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isPreferred) {
                        TextButton(onClick = { onSetPreferred(occurrence) }) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.set_preferred))
                        }
                    }
                }
            }

            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEditCourse, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.edit)) }
                OutlinedButton(onClick = { reschedule = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.reschedule_title)) }
                TextButton(onClick = { showDelete = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (showDelete) {
        val scope = deleteScope
        val description = when (scope) {
            AggregateDeleteScope.OCCURRENCE -> stringResource(R.string.delete_occurrence_description)
            AggregateDeleteScope.FROM_WEEK -> stringResource(R.string.delete_from_week_description, sourceWeek)
            AggregateDeleteScope.LOGICAL_SLOT -> stringResource(R.string.delete_slot_description)
            AggregateDeleteScope.COURSE -> stringResource(R.string.delete_whole_course_description)
        }
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AggregateDeleteScope.entries.forEach { choice ->
                        val label = when (choice) {
                            AggregateDeleteScope.OCCURRENCE -> R.string.this_occurrence
                            AggregateDeleteScope.FROM_WEEK -> R.string.from_this_week
                            AggregateDeleteScope.LOGICAL_SLOT -> R.string.this_time_slot
                            AggregateDeleteScope.COURSE -> R.string.whole_course
                        }
                        Row(modifier = Modifier.fillMaxWidth().selectable(selected = choice == scope, role = Role.RadioButton,
                            onClick = { deleteScope = choice }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = choice == scope, onClick = null)
                            Text(stringResource(label), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete(
                        when (scope) {
                            AggregateDeleteScope.OCCURRENCE -> TimetableCommand.DeleteOccurrence(occurrence.logicalSlotId, occurrence.sourceEpochDay, occurrence.recurrenceSegmentId)
                            AggregateDeleteScope.FROM_WEEK -> TimetableCommand.DeleteLogicalSlotFromWeek(occurrence.logicalSlotId, sourceWeek)
                            AggregateDeleteScope.LOGICAL_SLOT -> TimetableCommand.DeleteLogicalSlot(occurrence.logicalSlotId)
                            AggregateDeleteScope.COURSE -> TimetableCommand.DeleteCourse(occurrence.courseId)
                        },
                    )
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (reschedule) {
        AggregateRescheduleDialog(
            timetable = timetable,
            occurrence = occurrence,
            onConfirm = { command -> reschedule = false; onReschedule(command) },
            onDismiss = { reschedule = false },
        )
    }
}

@Composable
private fun AggregateRescheduleDialog(
    timetable: Timetable,
    occurrence: CourseOccurrence,
    onConfirm: (TimetableCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    var dateText by remember(occurrence.id) { mutableStateOf(LocalDate.ofEpochDay(occurrence.epochDay).toString()) }
    var day by remember(occurrence.id) { mutableIntStateOf(occurrence.dayOfWeek) }
    var startNode by remember(occurrence.id) { mutableIntStateOf(occurrence.startNode) }
    var targetTeacher by remember(occurrence.id) { mutableStateOf(occurrence.teacher) }
    var targetRoom by remember(occurrence.id) { mutableStateOf(occurrence.room) }
    var dayMenu by remember { mutableStateOf(false) }
    var nodeMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun targetDateForDay(value: String, targetDay: Int): String? {
        val parsed = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        val currentDay = timetableDayOf(parsed.toEpochDay(), timetable)
        return parsed.minusDays((currentDay - 1).toLong()).plusDays((targetDay - 1).toLong()).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reschedule_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.reschedule_description), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                OutlinedTextField(value = dateText, onValueChange = { dateText = it; error = null }, label = { Text(stringResource(R.string.target_date)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { dayMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.weekday_with_prefix, aggregateWeekdayName(day))) }
                        DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                            (1..7).forEach { value ->
                                DropdownMenuItem(text = { Text(stringResource(R.string.weekday_with_prefix, aggregateWeekdayName(value))) }, onClick = {
                                    targetDateForDay(dateText, value)?.let { dateText = it }
                                    day = value
                                    dayMenu = false
                                })
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { nodeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.period_number, startNode)) }
                        DropdownMenu(expanded = nodeMenu, onDismissRequest = { nodeMenu = false }) {
                            (1..timetable.timeTable.nodes.size.coerceIn(1, 60)).forEach { value ->
                                DropdownMenuItem(text = { Text(stringResource(R.string.period_number, value)) }, onClick = { startNode = value; nodeMenu = false })
                            }
                        }
                    }
                }
                Text(stringResource(R.string.single_day_replace_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                OutlinedTextField(
                    value = targetTeacher,
                    onValueChange = { targetTeacher = it },
                    label = { Text(stringResource(R.string.teacher_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetRoom,
                    onValueChange = { targetRoom = it },
                    label = { Text(stringResource(R.string.room_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = runCatching { LocalDate.parse(dateText) }.getOrNull()
                when {
                    target == null -> error = context.getString(R.string.valid_date_required)
                    target.toEpochDay() !in timetable.coverageRange -> error = context.getString(R.string.target_date_out_of_range)
                    timetableDayOf(target.toEpochDay(), timetable) != day -> error = context.getString(R.string.target_day_mismatch)
                    else -> onConfirm(
                        TimetableCommand.RescheduleOccurrence(
                            logicalSlotId = occurrence.logicalSlotId,
                            originalEpochDay = occurrence.sourceEpochDay,
                            targetEpochDay = target.toEpochDay(),
                            recurrenceSegmentId = occurrence.recurrenceSegmentId,
                            targetDayOfWeek = day,
                            targetStartNode = startNode,
                            targetNodeCount = occurrence.nodeCount,
                            targetCustomTime = occurrence.minuteRange.takeIf { occurrence.usesCustomTime },
                            targetTeacher = targetTeacher.trim(),
                            targetRoom = targetRoom.trim(),
                        ),
                    )
                }
            }) { Text(stringResource(R.string.save_success)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun AggregateWeekPickerDialog(
    maxWeek: Int,
    selectedWeek: Int,
    currentWeek: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_week)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items((1..maxWeek.coerceAtLeast(1)).toList()) { week ->
                    TextButton(onClick = { onSelect(week) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (week == selectedWeek) {
                                stringResource(R.string.aggregate_current_week_selected, week)
                            } else {
                                stringResource(
                                    R.string.aggregate_week_item,
                                    week,
                                    if (week == currentWeek) stringResource(R.string.aggregate_current_week_suffix_short) else "",
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun AggregateTablePickerDialog(
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
                items(tables, key = { it.id }) { item ->
                    TextButton(onClick = { onSelect(item.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name.ifBlank { stringResource(R.string.default_table_name) }, modifier = Modifier.weight(1f))
                            if (item.id == selectedId) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.current_table))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AggregateTableManagementScreen(
    repository: TimetableRepository,
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ordered by remember { mutableStateOf(tables) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragStartTop by remember { mutableIntStateOf(0) }
    var dragStartOrder by remember { mutableStateOf(tables) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TableEntity?>(null) }
    val orderedState by rememberUpdatedState(ordered)
    val currentSaving by rememberUpdatedState(saving)
    val listState = rememberLazyListState()
    LaunchedEffect(tables, saving, draggingId) {
        if (!saving && draggingId == null) ordered = tables
    }

    AggregateScreenScaffold(
        title = stringResource(R.string.table_management),
        onBack = onBack,
        actions = { IconButton(onClick = onNew) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_table)) } },
    ) { padding ->
        if (ordered.isEmpty()) {
            AggregateEmptyState(stringResource(R.string.aggregate_no_tables), stringResource(R.string.aggregate_create_table_hint), onNew, padding)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).pointerInput(listState) {
                    detectTableReorder(
                        onDragStart = { point ->
                            if (!currentSaving) {
                                val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    it.key is Long && point.y >= it.offset && point.y < it.offset + it.size
                                }
                                draggingId = hit?.key as? Long
                                dragStartTop = hit?.offset ?: 0
                                dragOffset = 0f
                                dragStartOrder = orderedState
                            }
                        },
                        onDragCancel = {
                            if (draggingId != null) ordered = dragStartOrder
                            draggingId = null
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            val result = orderedState.map { it.id }
                            val changed = result != dragStartOrder.map { it.id }
                            if (draggingId != null && changed) {
                                saving = true
                                scope.launch {
                                    try {
                                        repository.reorderTables(result)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        ordered = dragStartOrder
                                        aggregateToast(context, localizedUiError(context, error, R.string.order_save_failed))
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                            draggingId = null
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            val id = draggingId
                            if (id != null) {
                                change.consume()
                                dragOffset += amount.y
                                val visible = listState.layoutInfo.visibleItemsInfo
                                val dragged = visible.firstOrNull { it.key == id }
                                if (dragged != null) {
                                    val center = dragStartTop + dragOffset + dragged.size / 2f
                                    val target = visible.firstOrNull {
                                        it.key is Long && it.key != id && center >= it.offset && center < it.offset + it.size
                                    }
                                    val from = orderedState.indexOfFirst { it.id == id }
                                    val to = orderedState.indexOfFirst { it.id == target?.key }
                                    if (from >= 0 && to >= 0 && from != to && target != null &&
                                        ((to > from && center > target.offset + target.size / 2f) ||
                                            (to < from && center < target.offset + target.size / 2f))) {
                                        ordered = orderedState.toMutableList().apply { add(to, removeAt(from)) }
                                    }
                                }
                            }
                        },
                    )
                },
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(stringResource(R.string.aggregate_table_management_hint), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                if (saving) {
                    item { Text(stringResource(R.string.saving_order), modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                }
                items(ordered, key = { it.id }) { item ->
                    val isDragging = draggingId == item.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .zIndex(if (isDragging) 1f else 0f)
                            .animateItem(placementSpec = if (isDragging) null else androidx.compose.animation.core.spring())
                            .graphicsLayer {
                                alpha = if (isDragging) 0.88f else 1f
                                translationY = if (isDragging) {
                                    val top = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == item.id }?.offset ?: dragStartTop
                                    dragStartTop + dragOffset - top
                                } else 0f
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(item.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TableView, contentDescription = null, tint = SleepDownUiAccent, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name.ifBlank { stringResource(R.string.default_table_name) }, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    Text(if (item.id == currentTableId) stringResource(R.string.current_table_drag_hint) else stringResource(R.string.drag_to_sort), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                if (item.id == currentTableId) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.current_table), tint = SleepDownUiAccent)
                                IconButton(enabled = !saving, onClick = {
                                    if (tables.size > 1) deleting = item else aggregateToast(context, R.string.keep_one_table_short)
                                }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_table_title), tint = MaterialTheme.colorScheme.error) }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onCourses(item.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.courses)) }
                                TextButton(onClick = { onEditSettings(item.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.settings)) }
                                TextButton(onClick = { onAppearance(item.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.appearance)) }
                                TextButton(onClick = { onCopy(item) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.copy)) }
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_table_title)) },
            text = { Text(stringResource(R.string.delete_table_message, item.name)) },
            confirmButton = {
                TextButton(onClick = { deleting = null; onDelete(item) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AggregateScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface) } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        content = content,
    )
}

@Composable
private fun AggregateLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.aggregate_loading_timetable), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun AggregateEmptyState(title: String, message: String, onAction: () -> Unit, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.start)) }
    }
}

@Composable
internal fun AggregateTimeSettingsScreen(
    repository: TimetableRepository,
    table: TableEntity,
    onBack: () -> Unit,
    onTableUpdated: (TableEntity) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reusableTables by repository.observeReusableTimeTables().collectAsStateWithLifecycle(initialValue = emptyList())
    var editingId by remember(table.id) { mutableLongStateOf(table.timeTableId) }
    val rows = remember(table.id) { mutableStateListOf<AggregateNodeDraft>() }
    var selectedName by remember(table.id) { mutableStateOf("") }
    var uniformDurationText by remember(table.id) { mutableStateOf("50") }
    var breakMinutesText by remember(table.id) { mutableStateOf("10") }
    var breakFirstText by remember(table.id) { mutableStateOf("1") }
    var breakLastText by remember(table.id) { mutableStateOf("4") }
    var loadedId by remember(table.id) { mutableLongStateOf(0L) }
    var nodeError by remember(table.id) { mutableStateOf<List<TimeTableValidationIssue>>(emptyList()) }
    var message by remember(table.id) { mutableStateOf<String?>(null) }
    var nameDialog by remember { mutableStateOf<AggregateNameDialogState?>(null) }
    var deleteTarget by remember { mutableStateOf<ReusableTimeTableEntity?>(null) }
    val defaultTimeTableId = reusableTables
        .sortedWith(compareBy<ReusableTimeTableEntity> { it.sortOrder }.thenBy { it.id })
        .firstOrNull()
        ?.id

    suspend fun loadTimeTable(id: Long) {
        val entity = reusableTables.firstOrNull { it.id == id } ?: return
        val nodes = repository.getTimeTableNodes(id)
        editingId = id
        selectedName = entity.name
        rows.clear()
        rows.addAll(nodes.sortedBy { it.node }.map { AggregateNodeDraft(it.node, formatMinuteForUi(it.startMinuteOfDay), formatMinuteForUi(it.endMinuteOfDay)) })
        loadedId = id
        nodeError = emptyList()
    }

    LaunchedEffect(table.id, reusableTables.map { it.id }) {
        val target = reusableTables.firstOrNull { it.id == table.timeTableId } ?: reusableTables.firstOrNull()
        if (target != null && loadedId != target.id) loadTimeTable(target.id)
    }

    fun validateCurrent(): ScheduleTimeTable? {
        val candidate = ScheduleTimeTable(
            id = editingId.toString(),
            name = selectedName.trim().ifBlank { context.getString(R.string.time_table_label) },
            nodes = rows.map { TimeTableNode.fromStrings(it.node, it.start, it.end) },
        )
        nodeError = TimeTableValidator.validate(candidate).issues
        return candidate.takeIf { nodeError.isEmpty() }
    }

    fun saveCurrent() {
        val candidate = validateCurrent() ?: return
        scope.launch {
            runCatching {
                repository.saveTimeTableNodes(editingId, candidate.nodes)
                repository.getTable(table.id)?.let(onTableUpdated)
            }.onSuccess { aggregateToast(context, R.string.time_table_saved) }
                .onFailure { message = localizedUiError(context, it, R.string.time_table_save_failed) }
        }
    }

    fun selectCurrent(id: Long) {
        scope.launch {
            runCatching {
                reusableTables.first { it.id == id }
                val updated = repository.bindTimeTable(table.id, id)
                loadTimeTable(id)
                onTableUpdated(updated)
            }.onFailure { message = localizedUiError(context, it, R.string.switch_time_table_failed) }
        }
    }

    AggregateScreenScaffold(
        title = stringResource(R.string.time_table),
        onBack = onBack,
        actions = {
            IconButton(onClick = { nameDialog = AggregateNameDialogState("new", "") }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_time_table)) }
            IconButton(onClick = ::saveCurrent) { Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_time_table)) }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.time_table_shared_hint), modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
            }
            item {
                Text(stringResource(R.string.reusable_time_tables), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            items(reusableTables, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = if (item.id == editingId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).clickable { selectCurrent(item.id) }) {
                                Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        item.id == defaultTimeTableId -> stringResource(R.string.default_time_table_shared)
                                        item.id == table.timeTableId -> stringResource(R.string.current_time_table_in_use)
                                        else -> stringResource(R.string.set_current_time_table_hint)
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                            if (item.id == editingId) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.editing), tint = SleepDownUiAccent)
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { scope.launch { loadTimeTable(item.id) } }) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(3.dp)); Text(stringResource(R.string.edit)) }
                            TextButton(onClick = { nameDialog = AggregateNameDialogState("copy:${item.id}", context.getString(R.string.copy_name, item.name)) }) { Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(3.dp)); Text(stringResource(R.string.copy_time_table)) }
                            TextButton(onClick = { if (item.id != defaultTimeTableId) deleteTarget = item }, enabled = item.id != defaultTimeTableId) { Icon(Icons.Default.Delete, contentDescription = null); Spacer(Modifier.width(3.dp)); Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { nameDialog = AggregateNameDialogState("rename", selectedName) }, enabled = editingId > 0L, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.rename)) }
                    OutlinedButton(onClick = {
                        val candidate = rows.map { TimeTableNode.fromStrings(it.node, it.start, it.end) }
                        if (candidate.isNotEmpty()) nameDialog = AggregateNameDialogState("copy:$editingId", context.getString(R.string.copy_name, selectedName))
                    }, enabled = editingId > 0L, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.copy_current)) }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.uniform_duration), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.uniform_duration_warning), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = uniformDurationText,
                                onValueChange = { uniformDurationText = it.filter(Char::isDigit) },
                                label = { Text(stringResource(R.string.duration)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = {
                                val duration = uniformDurationText.toIntOrNull()
                                if (duration == null || duration !in 1..240) {
                                    message = context.getString(R.string.invalid_duration_minutes)
                                } else {
                                    rows.forEachIndexed { index, node ->
                                        rows[index] = node.copy(end = addMinutesForUi(node.start, duration))
                                    }
                                    nodeError = emptyList()
                                    message = null
                                }
                            }) { Text(stringResource(R.string.apply_uniform_duration)) }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.break_time_settings), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.break_time_hint), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = breakFirstText, onValueChange = { breakFirstText = it.filter(Char::isDigit) },
                                label = { Text(stringResource(R.string.break_first_period)) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = breakLastText, onValueChange = { breakLastText = it.filter(Char::isDigit) },
                                label = { Text(stringResource(R.string.break_last_period)) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = breakMinutesText, onValueChange = { breakMinutesText = it.filter(Char::isDigit) },
                                label = { Text(stringResource(R.string.break_minutes)) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val source = validateCurrent()
                                if (source != null) {
                                    val updated = runCatching {
                                        applyBreakMinutes(source.nodes, breakFirstText.toInt(), breakLastText.toInt(), breakMinutesText.toInt())
                                    }.getOrNull()
                                    if (updated == null) {
                                        message = context.getString(R.string.invalid_break_settings)
                                    } else {
                                        val issues = TimeTableValidator.validate(source.copy(nodes = updated)).issues
                                        if (issues.isNotEmpty()) {
                                            message = context.getString(R.string.break_time_overlap)
                                        } else {
                                            rows.clear()
                                            rows.addAll(updated.map { AggregateNodeDraft(it.node, formatMinuteForUi(it.startMinuteOfDay), formatMinuteForUi(it.endMinuteOfDay)) })
                                            nodeError = emptyList()
                                            message = null
                                        }
                                    }
                                }
                            }) { Text(stringResource(R.string.apply_uniform_duration)) }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.editing_time_table, selectedName.ifBlank { stringResource(R.string.time_table_label) }), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.period_capacity, rows.size), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    IconButton(onClick = {
                        if (rows.size < 60) {
                            val next = rows.size + 1
                            rows += AggregateNodeDraft(next, rows.lastOrNull()?.end ?: "08:00", addMinutesForUi(rows.lastOrNull()?.end ?: "08:00", 50))
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_period)) }
                }
            }
            if (nodeError.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.fix_time_table_before_save), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        nodeError.filter { it.node == 0 }.forEach { issue ->
                            Text(aggregateIssueMessage(context, issue), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }
            items(rows.indices.toList(), key = { index -> "node-${rows[index].node}" }) { index ->
                val row = rows[index]
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${row.node}", modifier = Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = row.start, onValueChange = { rows[index] = row.copy(start = it); nodeError = emptyList() }, label = { Text(stringResource(R.string.start)) }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = row.end, onValueChange = { rows[index] = row.copy(end = it); nodeError = emptyList() }, label = { Text(stringResource(R.string.end)) }, singleLine = true, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (rows.size > 1) {
                                rows.removeAt(index)
                                rows.forEachIndexed { position, node -> rows[position] = node.copy(node = position + 1) }
                                nodeError = emptyList()
                            }
                        }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_period, row.node), tint = if (rows.size > 1) MaterialTheme.colorScheme.error else Color.LightGray) }
                    }
                    val issues = nodeError.filter { it.node == row.node || it.otherNode == row.node }
                    issues.forEach { issue -> Text(aggregateIssueMessage(context, issue), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 48.dp, end = 12.dp, bottom = 4.dp)) }
                }
            }
            message?.let { text -> item { Text(text, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp)) } }
        }
    }

    nameDialog?.let { dialog ->
        AggregateNameDialog(
            initial = dialog.initial,
            title = when {
                dialog.mode == "new" -> stringResource(R.string.new_time_table)
                dialog.mode == "rename" -> stringResource(R.string.rename_time_table)
                else -> stringResource(R.string.copy_time_table)
            },
            onConfirm = { value ->
                nameDialog = null
                val cleanName = value.trim().ifBlank { context.getString(R.string.time_table_label) }
                scope.launch {
                    runCatching {
                        when {
                            dialog.mode == "new" -> {
                                val source = rows.map { TimeTableNode.fromStrings(it.node, it.start, it.end) }.takeIf { it.isNotEmpty() } ?: aggregateDefaultNodes()
                                val candidate = ScheduleTimeTable(name = cleanName, nodes = source)
                                val issues = TimeTableValidator.validate(candidate).issues
                                if (issues.isNotEmpty()) {
                                    nodeError = issues
                                    throw IllegalArgumentException(context.getString(R.string.fix_time_table_error))
                                }
                                val id = repository.saveTimeTable(candidate)
                                loadTimeTable(id)
                            }
                            dialog.mode == "rename" -> {
                                val candidate = validateCurrent() ?: throw IllegalArgumentException(context.getString(R.string.fix_time_table_error))
                                repository.saveTimeTable(candidate.copy(name = cleanName))
                                selectedName = cleanName
                            }
                            else -> {
                                val sourceId = dialog.mode.substringAfter(':').toLongOrNull() ?: editingId
                                val sourceRows = if (sourceId == editingId) rows.toList() else repository.getTimeTableNodes(sourceId).map { AggregateNodeDraft(it.node, formatMinuteForUi(it.startMinuteOfDay), formatMinuteForUi(it.endMinuteOfDay)) }
                                val source = sourceRows.map { TimeTableNode.fromStrings(it.node, it.start, it.end) }
                                val candidate = ScheduleTimeTable(name = cleanName, nodes = source)
                                val issues = TimeTableValidator.validate(candidate).issues
                                if (issues.isNotEmpty()) {
                                    nodeError = issues
                                    throw IllegalArgumentException(context.getString(R.string.fix_time_table_error))
                                }
                                val id = repository.saveTimeTable(candidate)
                                loadTimeTable(id)
                            }
                        }
                    }.onFailure { message = localizedUiError(context, it, R.string.operation_failed) }
                }
            },
            onDismiss = { nameDialog = null },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_time_table_title)) },
            text = { Text(stringResource(R.string.delete_time_table_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        runCatching { repository.deleteTimeTable(target.id) }
                            .onSuccess { message = context.getString(R.string.time_table_deleted) }
                            .onFailure { message = localizedUiError(context, it, R.string.current_time_table_cannot_delete) }
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun AggregateNameDialog(initial: String, title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun AggregateTableSettingsScreen(
    table: TableEntity,
    currentWeek: Int,
    onBack: () -> Unit,
    onSave: (TableEntity) -> Unit,
    onAppearance: () -> Unit,
    onCourses: () -> Unit,
    onTimeTable: () -> Unit,
    onWidgetHelp: () -> Unit,
) {
    var working by remember(table.id) { mutableStateOf(table) }
    var dateDialog by remember { mutableStateOf(false) }
    AggregateScreenScaffold(
        title = stringResource(R.string.table_settings),
        onBack = { onSave(working); onBack() },
        actions = { TextButton(onClick = { onSave(working) }) { Text(stringResource(R.string.save)) } },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { OutlinedTextField(value = working.name, onValueChange = { working = working.copy(name = it) }, label = { Text(stringResource(R.string.table_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        AggregateSettingButton(stringResource(R.string.first_day), LocalDate.ofEpochDay(working.startDate).toString()) { dateDialog = true }
                        HorizontalDivider()
                        AggregateSettingButton(stringResource(R.string.current_week), stringResource(R.string.current_week_adjust, currentWeek.coerceIn(1, working.maxWeek))) { dateDialog = true }
                        HorizontalDivider()
                        AggregateSettingButton(stringResource(R.string.nodes_per_day), stringResource(R.string.node_count_summary, working.nodeCount)) { }
                        HorizontalDivider()
                        AggregateSettingButton(stringResource(R.string.term_weeks), stringResource(R.string.term_weeks_summary, working.maxWeek)) { }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        AggregateSettingButton(stringResource(R.string.table_appearance), stringResource(R.string.appearance_background_summary)) { onAppearance() }
                        HorizontalDivider()
                        AggregateSettingButton(stringResource(R.string.course_management), stringResource(R.string.course_edit_summary)) { onCourses() }
                        HorizontalDivider()
                        AggregateSettingButton(stringResource(R.string.time_table), stringResource(R.string.reusable_time_table_summary)) { onTimeTable() }
                        HorizontalDivider()
                        AggregateSettingButton(stringResource(R.string.widgets), stringResource(R.string.widget_adjust_summary)) { onWidgetHelp() }
                    }
                }
            }
        }
    }
    if (dateDialog) {
        AggregateDatePickerDialog(
            value = LocalDate.ofEpochDay(working.startDate + (currentWeek - 1L) * 7L),
            onSelected = { selected ->
                val aligned = selected.with(DayOfWeek.MONDAY)
                working = working.copy(startDate = aligned.minusWeeks((currentWeek - 1).toLong()).toEpochDay())
                dateDialog = false
            },
            onDismiss = { dateDialog = false },
        )
    }
}

@Composable
private fun AggregateSettingButton(title: String, summary: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AggregateDatePickerDialog(value: LocalDate, onSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_current_week_start)) },
        text = {
            Column {
                Text(stringResource(R.string.current_week_start_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.date_format)) }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = runCatching { LocalDate.parse(text) }.getOrNull()
                if (parsed == null) error = context.getString(R.string.valid_date_required) else onSelected(parsed)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun aggregateDefaultNodes(): List<TimeTableNode> =
    (1..8).map { node ->
        val start = 8 * 60 + (node - 1) * 50
        TimeTableNode(node, start, start + 45)
    }

@Composable
private fun aggregateWeekdayName(day: Int): String = when (((day - 1) % 7 + 7) % 7) {
    0 -> stringResource(R.string.weekday_short_monday)
    1 -> stringResource(R.string.weekday_short_tuesday)
    2 -> stringResource(R.string.weekday_short_wednesday)
    3 -> stringResource(R.string.weekday_short_thursday)
    4 -> stringResource(R.string.weekday_short_friday)
    5 -> stringResource(R.string.weekday_short_saturday)
    else -> stringResource(R.string.weekday_short_sunday)
}

private fun aggregateVisibleDays(table: TableEntity): List<Int> {
    val ordered = if (table.sundayFirst) listOf(7, 1, 2, 3, 4, 5, 6) else listOf(1, 2, 3, 4, 5, 6, 7)
    return ordered.filter { day -> when (day) {
        6 -> table.showSat
        7 -> table.showSun
        else -> true
    } }
}

private fun aggregateWeekRange(timetable: Timetable, week: Int): EpochDayRange {
    val start = timetable.firstDayEpochDay + (week.coerceAtLeast(1) - 1L) * 7L
    return EpochDayRange(start, start + 6L)
}

private fun dateFor(timetable: Timetable, week: Int): LocalDate =
    LocalDate.ofEpochDay(aggregateWeekRange(timetable, week).startEpochDay)

private fun currentWeekFor(table: TableEntity): Int {
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.ofEpochDay(table.startDate), LocalDate.now())
    return Math.floorDiv(days, 7L).toInt() + 1
}

private fun timetableDayOf(epochDay: Long, timetable: Timetable): Int {
    val relative = epochDay - timetable.firstDayEpochDay
    return Math.floorMod(relative, 7L).toInt() + 1
}

internal fun aggregateSourceWeek(
    timetable: Timetable,
    occurrence: CourseOccurrence,
    context: Context? = null,
): Int {
    val relative = occurrence.sourceEpochDay - timetable.firstDayEpochDay
    if (relative < 0L) aggregateError(context, R.string.invalid_original_date)
    return Math.floorDiv(relative, 7L).toInt() + 1
}

internal fun aggregateMoveCommands(
    timetable: Timetable,
    occurrence: CourseOccurrence,
    targetDay: Int,
    targetNode: Int,
    scope: CourseMoveScope,
    context: Context? = null,
): List<TimetableCommand> {
    val single = aggregateMoveCommand(timetable, occurrence, targetDay, targetNode, context, onlyThisWeek = true)
        as TimetableCommand.RescheduleOccurrence
    if (scope == CourseMoveScope.THIS_WEEK) return listOf(single)

    val source = TimetableEngine.recurringOccurrences(timetable).single {
        it.logicalSlotId == occurrence.logicalSlotId && it.recurrenceSegmentId == occurrence.recurrenceSegmentId &&
            it.sourceEpochDay == occurrence.sourceEpochDay
    }
    val customTime = single.targetCustomTime
    val minuteDelta = if (customTime != null) {
        customTime.startMinuteOfDay - source.startMinuteOfDay
    } else {
        val targetStart = timetable.timeTable.node(targetNode)?.startMinuteOfDay
            ?: aggregateError(context, R.string.target_period_not_found, targetNode)
        val sourceStart = timetable.timeTable.node(source.startNode)?.startMinuteOfDay
            ?: aggregateError(context, R.string.original_period_not_found, source.startNode)
        targetStart - sourceStart
    }
    val all = TimetableCommand.MoveLogicalSlot(
        logicalSlotId = occurrence.logicalSlotId,
        dayDelta = targetDay - source.dayOfWeek,
        startNodeDelta = targetNode - source.startNode,
        minuteDelta = minuteDelta,
    )
    if (!occurrence.isRescheduled) return listOf(all)

    // The current date exception retains its identity when the recurring source moves.
    val moved = TimetableCommands.apply(timetable, all)
    val movedSource = TimetableEngine.recurringOccurrences(moved).single {
        it.logicalSlotId == source.logicalSlotId && it.recurrenceSegmentId == source.recurrenceSegmentId && it.week == source.week
    }
    return listOf(all, single.copy(originalEpochDay = movedSource.sourceEpochDay))
}

internal fun aggregateMoveCommand(
    timetable: Timetable,
    occurrence: CourseOccurrence,
    targetDay: Int,
    targetNode: Int,
    context: Context? = null,
    onlyThisWeek: Boolean = false,
): TimetableCommand {
    if (targetDay !in 1..7) aggregateError(context, R.string.invalid_target_day)
    if (targetNode <= 0) aggregateError(context, R.string.invalid_target_period)
    val slot = timetable.courses
        .asSequence()
        .flatMap { it.slots.asSequence() }
        .singleOrNull { it.id == occurrence.logicalSlotId }
        ?: aggregateError(context, R.string.course_slot_not_found, occurrence.logicalSlotId)
    val segment = slot.recurrenceSegments.singleOrNull { it.id == occurrence.recurrenceSegmentId }
        ?: aggregateError(context, R.string.course_segment_not_found, occurrence.recurrenceSegmentId)
    val targetCustomTime = occurrence.minuteRange.takeIf { occurrence.usesCustomTime }?.let { range ->
        val sourceNodeStart = timetable.timeTable.nodes.singleOrNull { it.node == occurrence.startNode }
            ?.startMinuteOfDay
            ?: aggregateError(context, R.string.original_period_not_found, occurrence.startNode)
        val targetNodeStart = timetable.timeTable.nodes.singleOrNull { it.node == targetNode }
            ?.startMinuteOfDay
            ?: aggregateError(context, R.string.target_period_not_found, targetNode)
        val delta = targetNodeStart - sourceNodeStart
        MinuteRange(range.startMinuteOfDay + delta, range.endMinuteOfDay + delta).also {
            if (!it.isValid) aggregateError(context, R.string.moved_custom_time_invalid)
        }
    }

    if (onlyThisWeek || occurrence.isRescheduled) {
        val effectiveWeekStart = occurrence.epochDay - (occurrence.dayOfWeek - 1L)
        return TimetableCommand.RescheduleOccurrence(
            logicalSlotId = occurrence.logicalSlotId,
            originalEpochDay = occurrence.sourceEpochDay,
            targetEpochDay = effectiveWeekStart + targetDay - 1L,
            recurrenceSegmentId = occurrence.recurrenceSegmentId,
            targetDayOfWeek = targetDay,
            targetStartNode = targetNode,
            targetNodeCount = occurrence.nodeCount,
            targetCustomTime = targetCustomTime,
            targetTeacher = occurrence.teacher,
            targetRoom = occurrence.room,
        )
    }

    val hasSegmentOverride = segment.dayOfWeek != null ||
        segment.startNode != null ||
        segment.nodeCount != null ||
        segment.teacher != null ||
        segment.room != null ||
        segment.customTime != null
    return if (hasSegmentOverride) {
        TimetableCommand.MoveRecurrenceSegment(
            logicalSlotId = occurrence.logicalSlotId,
            recurrenceSegmentId = occurrence.recurrenceSegmentId,
            targetDayOfWeek = targetDay,
            targetStartNode = targetNode,
            targetCustomTime = targetCustomTime,
        )
    } else {
        TimetableCommand.MoveLogicalSlot(
            logicalSlotId = occurrence.logicalSlotId,
            targetDayOfWeek = targetDay,
            targetStartNode = targetNode,
            targetCustomTime = targetCustomTime,
        )
    }
}

private fun fallbackPlacement(occurrence: CourseOccurrence, nodeCount: Int): GridPlacement {
    val safeCount = nodeCount.coerceAtLeast(1)
    return GridPlacement(
        topFraction = ((occurrence.startNode - 1).toDouble() / safeCount).coerceIn(0.0, 1.0),
        heightFraction = (occurrence.nodeCount.toDouble() / safeCount).coerceIn(0.02, 1.0),
    )
}

private fun formatMinuteForUi(value: Int): String =
    value.takeIf { it in 0..(24 * 60) }?.let { minute -> MinuteOfDay.format(minute) }.orEmpty()

private fun addMinutesForUi(value: String, minutes: Int): String {
    val parsed = MinuteOfDay.parse(value) ?: 0
    return MinuteOfDay.format((parsed + minutes).coerceAtMost(24 * 60))
}

private fun aggregateToast(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

private fun aggregateToast(context: Context, messageRes: Int, vararg args: Any) {
    aggregateToast(context, context.getString(messageRes, *args))
}

private fun aggregateError(context: Context?, messageRes: Int, vararg args: Any): Nothing {
    throw IllegalArgumentException(context?.getString(messageRes, *args).orEmpty())
}

private fun aggregateIssueMessage(context: Context, issue: TimeTableValidationIssue): String {
    val node = issue.node.takeIf { it > 0 }?.let { context.getString(R.string.period_number, it) }
        ?: context.getString(R.string.time_table_label)
    return when (issue.code) {
        TimeTableValidationCode.INVALID_START_MINUTE -> context.getString(R.string.period_start_invalid, node)
        TimeTableValidationCode.INVALID_END_MINUTE -> context.getString(R.string.period_end_invalid, node)
        TimeTableValidationCode.END_NOT_AFTER_START -> context.getString(R.string.period_end_not_after_start, node)
        TimeTableValidationCode.OVERLAPPING_NODES -> context.getString(R.string.period_overlap, node)
        TimeTableValidationCode.DUPLICATE_NODE_NUMBER -> context.getString(R.string.duplicate_period_number, node)
        TimeTableValidationCode.NONCONTIGUOUS_NODES -> context.getString(R.string.noncontiguous_period_numbers)
        TimeTableValidationCode.NONCHRONOLOGICAL_NODES -> context.getString(R.string.nonchronological_period_times)
        TimeTableValidationCode.INVALID_NODE_NUMBER -> context.getString(R.string.invalid_period_number, node)
        TimeTableValidationCode.INVALID_NODE_COUNT -> context.getString(R.string.time_table_needs_period)
        else -> context.getString(R.string.time_table_validation_error)
    }
}

private fun aggregateParseColor(value: String, fallback: Color): Color {
    val raw = value.removePrefix("#")
    val normalized = when (raw.length) {
        6 -> "FF$raw"
        8 -> raw
        else -> return fallback
    }
    return runCatching { Color(normalized.toLong(16).toInt()) }.getOrDefault(fallback)
}
