package com.letr.sleepdown.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared palette used by Android and iOS so course colors stay consistent. */
object SleepDownPalette {
    val colors: List<Int> = listOf(
        0xFFFF1744.toInt(),
        0xFFFA6278.toInt(),
        0xFF2979FF.toInt(),
        0xFF1DE9B6.toInt(),
        0xFFA375FF.toInt(),
        0xFFFF9100.toInt(),
        0xFFFF3D00.toInt(),
        0xFF2196F3.toInt(),
        0xFF005CAF.toInt(),
    )

    fun colorFor(name: String): Int {
        var hash = 0
        name.forEach { hash = hash * 31 + it.code }
        return colors[((hash % colors.size) + colors.size) % colors.size]
    }
}

data class SleepDownCourse(
    val name: String,
    val teacher: String = "",
    val room: String = "",
    val day: Int = 1,
    val startNode: Int = 1,
    val step: Int = 1,
    val color: Int = 0,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    /** 0=每周，1=单周，2=双周 */
    val weekType: Int = 0,
    val note: String = "",
    val credit: Float = 0f,
)

data class SleepDownTimeSlot(
    val node: Int,
    val start: String,
    val end: String,
)

data class SleepDownAppearance(
    val showGrid: Boolean = false,
    val showTimeBar: Boolean = true,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = true,
    val itemHeightDp: Int = 64,
    val itemRadiusDp: Int = 4,
    val itemAlpha: Float = 0.5f,
    val itemTextSize: Int = 12,
    val showLocation: Boolean = true,
    val showTeacher: Boolean = true,
)

private fun defaultSleepDownCourses(): List<SleepDownCourse> = listOf(
    SleepDownCourse("高等数学", "张老师", "A-203", day = 1, startNode = 1, step = 2),
    SleepDownCourse("大学英语", "李老师", "B-101", day = 3, startNode = 3, color = SleepDownPalette.colors[2]),
    SleepDownCourse("程序设计", "王老师", "实验楼 204", day = 5, startNode = 5, color = SleepDownPalette.colors[3]),
)

private fun defaultSleepDownTimeSlots(): List<SleepDownTimeSlot> = listOf(
    SleepDownTimeSlot(1, "08:00", "08:50"),
    SleepDownTimeSlot(2, "09:00", "09:50"),
    SleepDownTimeSlot(3, "10:10", "11:00"),
    SleepDownTimeSlot(4, "11:10", "12:00"),
    SleepDownTimeSlot(5, "13:30", "14:20"),
    SleepDownTimeSlot(6, "14:30", "15:20"),
    SleepDownTimeSlot(7, "15:40", "16:30"),
    SleepDownTimeSlot(8, "16:40", "17:30"),
)

private enum class SharedScreen {
    WEEK,
    TABLES,
    TABLE_SETTINGS,
    APPEARANCE,
    COURSES,
    COURSE_EDITOR,
    TIME_TABLE,
    IMPORT,
    WIDGET,
}

/**
 * Compose Multiplatform UI used by the iOS target and kept intentionally data-light.
 * Android continues to provide its Room-backed implementation while sharing this UI
 * vocabulary, palette, and interaction structure across platforms.
 */
@Composable
fun SleepDownSharedApp() {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFFFF2D55),
            onPrimary = Color.White,
            background = Color(0xFFF7F7F7),
            surface = Color.White,
            onSurface = Color(0xFF141414),
            onSurfaceVariant = Color(0xFF626466),
        ),
    ) {
        var screen by remember { mutableStateOf(SharedScreen.WEEK) }
        var selectedWeek by remember { mutableStateOf(1) }
        var showMore by remember { mutableStateOf(false) }
        var showWeekPicker by remember { mutableStateOf(false) }
        var selectedCourse by remember { mutableStateOf<SleepDownCourse?>(null) }
        var editingCourse by remember { mutableStateOf<SleepDownCourse?>(null) }
        var courses by remember { mutableStateOf(defaultSleepDownCourses()) }
        var tableNames by remember { mutableStateOf(listOf("我的课表", "秋季学期")) }
        var selectedTableName by remember { mutableStateOf(tableNames.first()) }
        var timeSlots by remember { mutableStateOf(defaultSleepDownTimeSlots()) }
        var appearance by remember { mutableStateOf(SleepDownAppearance()) }

        when (screen) {
            SharedScreen.WEEK -> SharedWeekScreen(
                week = selectedWeek,
                courses = courses,
                showMore = showMore,
                onMore = { showMore = true },
                onDismissMore = { showMore = false },
                onWeekClick = { showWeekPicker = true },
                onNavigate = { screen = it },
                onCourseClick = { selectedCourse = it },
                appearance = appearance,
            )
            SharedScreen.TABLES -> SharedTablesScreen(
                tableNames = tableNames,
                selectedTableName = selectedTableName,
                onSelectTable = { selectedTableName = it },
                onCopyTable = { name -> tableNames = tableNames + "$name 副本" },
                onCreateTable = {
                    val name = "新课表 ${tableNames.size + 1}"
                    tableNames = tableNames + name
                    selectedTableName = name
                },
                onBack = { screen = SharedScreen.WEEK },
                onNavigate = { screen = it },
            )
            SharedScreen.TABLE_SETTINGS -> SharedSettingsScreen(
                appearance = appearance,
                timeSlotCount = timeSlots.size,
                onAppearanceChange = { appearance = it },
                onBack = { screen = SharedScreen.TABLES },
                onNavigate = { screen = it },
            )
            SharedScreen.APPEARANCE -> SharedAppearanceScreen(
                appearance = appearance,
                onAppearanceChange = { appearance = it },
                onBack = { screen = SharedScreen.TABLE_SETTINGS },
            )
            SharedScreen.COURSES -> SharedCoursesScreen(
                courses = courses,
                onBack = { screen = SharedScreen.TABLES },
                onEdit = { editingCourse = it; screen = SharedScreen.COURSE_EDITOR },
                onDelete = { course -> courses = courses.filterNot { it == course } },
                onAdd = { editingCourse = null; screen = SharedScreen.COURSE_EDITOR },
            )
            SharedScreen.COURSE_EDITOR -> SharedCourseEditorScreen(
                course = editingCourse,
                onBack = { editingCourse = null; screen = SharedScreen.COURSES },
                onSave = { course ->
                    courses = if (editingCourse == null) {
                        courses + course
                    } else {
                        courses.map { existing -> if (existing == editingCourse) course else existing }
                    }
                    editingCourse = null
                    screen = SharedScreen.COURSES
                },
            )
            SharedScreen.TIME_TABLE -> SharedTimeTableScreen(
                slots = timeSlots,
                onSlotsChange = { timeSlots = it },
                onBack = { screen = SharedScreen.WEEK },
            )
            SharedScreen.IMPORT -> SharedImportScreen(onBack = { screen = SharedScreen.WEEK })
            SharedScreen.WIDGET -> SharedWidgetScreen(onBack = { screen = SharedScreen.WEEK })
        }

        if (showWeekPicker) {
            AlertDialog(
                onDismissRequest = { showWeekPicker = false },
                title = { Text("选择周次") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items((1..20).toList()) { week ->
                            TextButton(onClick = { selectedWeek = week; showWeekPicker = false }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (week == selectedWeek) "✓ 第${week}周" else "第${week}周")
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selectedWeek = 1; showWeekPicker = false }) { Text("回到第1周") } },
            )
        }
        selectedCourse?.let { course ->
            SharedCourseDialog(
                course = course,
                onDismiss = { selectedCourse = null },
                onEdit = { editingCourse = course; selectedCourse = null; screen = SharedScreen.COURSE_EDITOR },
            )
        }
    }
}

@Composable
private fun SharedWeekScreen(
    week: Int,
    courses: List<SleepDownCourse>,
    showMore: Boolean,
    onMore: () -> Unit,
    onDismissMore: () -> Unit,
    onWeekClick: () -> Unit,
    onNavigate: (SharedScreen) -> Unit,
    onCourseClick: (SleepDownCourse) -> Unit,
    appearance: SleepDownAppearance,
) {
    val allDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val visibleDayIndexes = allDays.indices.filter { index ->
        index < 5 || (index == 5 && appearance.showSaturday) || (index == 6 && appearance.showSunday)
    }
    val visibleCourses = courses.associateBy { it.day to it.startNode }
    val dates = listOf("31", "1", "2", "3", "4", "5", "6")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE8E8F4), Color(0xFFBECEE5))),),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onWeekClick)) {
                    Text("2026/9/3", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("第${week}周    周四", fontSize = 14.sp, color = Color(0xFF343438))
                }
                IconButton(onClick = { onNavigate(SharedScreen.COURSE_EDITOR) }) { Text("＋", fontSize = 30.sp) }
                IconButton(onClick = { onNavigate(SharedScreen.IMPORT) }) { Text("↓", fontSize = 30.sp) }
                Box {
                    IconButton(onClick = onMore) { Text("⋮", fontSize = 30.sp) }
                    DropdownMenu(expanded = showMore, onDismissRequest = onDismissMore) {
                        DropdownMenuItem(text = { Text("课表管理") }, onClick = { onDismissMore(); onNavigate(SharedScreen.TABLES) })
                        DropdownMenuItem(text = { Text("课表设置") }, onClick = { onDismissMore(); onNavigate(SharedScreen.TABLE_SETTINGS) })
                        DropdownMenuItem(text = { Text("课表外观") }, onClick = { onDismissMore(); onNavigate(SharedScreen.APPEARANCE) })
                        DropdownMenuItem(text = { Text("课程管理") }, onClick = { onDismissMore(); onNavigate(SharedScreen.COURSES) })
                        DropdownMenuItem(text = { Text("时间表") }, onClick = { onDismissMore(); onNavigate(SharedScreen.TIME_TABLE) })
                        DropdownMenuItem(text = { Text("桌面小部件") }, onClick = { onDismissMore(); onNavigate(SharedScreen.WIDGET) })
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
                Box(modifier = Modifier.width(54.dp), contentAlignment = Alignment.Center) { Text("8/9\n月", fontWeight = FontWeight.Bold) }
                visibleDayIndexes.forEach { dayIndex ->
                    val isToday = dayIndex == 3
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("${allDays[dayIndex]}\n${dates[dayIndex]}", fontSize = 12.sp, color = if (isToday) Color.Black else Color.Black.copy(alpha = .35f), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.width(54.dp)) {
                    (1..12).forEach { node ->
                        val slot = defaultSleepDownTimeSlots().getOrNull(node - 1)
                        Column(modifier = Modifier.height(appearance.itemHeightDp.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("$node", fontWeight = FontWeight.Bold)
                            if (appearance.showTimeBar && slot != null) Text("${slot.start}", fontSize = 9.sp, color = Color.Black.copy(alpha = .55f))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    (1..12).forEach { node ->
                        Row(modifier = Modifier.height(appearance.itemHeightDp.dp).fillMaxWidth()) {
                            visibleDayIndexes.forEach { day ->
                                val course = visibleCourses[day + 1 to node]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(1.dp)
                                        .then(if (appearance.showGrid) Modifier.border(1.dp, Color.White.copy(alpha = .28f)) else Modifier)
                                        .background(course?.let { Color(it.color.takeUnless { color -> color == 0 } ?: SleepDownPalette.colorFor(it.name)).copy(alpha = appearance.itemAlpha) } ?: Color.Transparent, RoundedCornerShape(appearance.itemRadiusDp.dp))
                                        .clickable(enabled = course != null, onClick = { course?.let(onCourseClick) }),
                                    contentAlignment = Alignment.TopStart,
                                ) {
                                    if (course != null) {
                                        Column(modifier = Modifier.padding(4.dp)) {
                                            Text(course.name, color = Color.White, fontSize = appearance.itemTextSize.sp, fontWeight = FontWeight.Bold)
                                            if (appearance.showTeacher && course.teacher.isNotBlank()) Text(course.teacher, color = Color.White.copy(alpha = .9f), fontSize = 9.sp)
                                            if (appearance.showLocation && course.room.isNotBlank()) Text(course.room, color = Color.White.copy(alpha = .9f), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFFF7F7F7),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", fontSize = 38.sp, color = Color(0xFF141414)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

@Composable
private fun SharedSettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(title, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp), color = Color(0xFF626466), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) { content() }
    }
}

@Composable
private fun SharedSettingRow(title: String, summary: String = "", trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            if (summary.isNotBlank()) Text(summary, color = Color(0xFF626466), fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SharedTablesScreen(
    tableNames: List<String>,
    selectedTableName: String,
    onSelectTable: (String) -> Unit,
    onCopyTable: (String) -> Unit,
    onCreateTable: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (SharedScreen) -> Unit,
) {
    SharedScreenScaffold("多课表管理", onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("点击卡片切换课表，使用下方操作管理内容", color = Color(0xFF626466))
            tableNames.forEach { name ->
                SharedTableCard(
                    name = name,
                    selected = name == selectedTableName,
                    onSelect = { onSelectTable(name) },
                    onCourses = { onNavigate(SharedScreen.COURSES) },
                    onSettings = { onNavigate(SharedScreen.TABLE_SETTINGS) },
                    onAppearance = { onNavigate(SharedScreen.APPEARANCE) },
                    onCopy = { onCopyTable(name) },
                )
            }
            Button(onClick = onCreateTable, modifier = Modifier.fillMaxWidth()) { Text("＋ 新建课表") }
        }
    }
}

@Composable
private fun SharedTableCard(
    name: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onCourses: () -> Unit,
    onSettings: () -> Unit,
    onAppearance: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF7C8ECD), Color(0xFFB8CBE1))),
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    )
                    .clickable(onClick = onSelect),
                contentAlignment = Alignment.BottomStart,
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, modifier = Modifier.weight(1f), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    if (selected) Text("✓", color = Color.White, fontSize = 28.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onCourses) { Text("☷ 课程") }
                TextButton(onClick = onSettings) { Text("⚙ 设置") }
                TextButton(onClick = onAppearance) { Text("● 外观") }
                TextButton(onClick = onCopy) { Text("□ 复制") }
            }
        }
    }
}

@Composable
private fun SharedSettingsScreen(
    appearance: SleepDownAppearance,
    timeSlotCount: Int,
    onAppearanceChange: (SleepDownAppearance) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SharedScreen) -> Unit,
) {
    SharedScreenScaffold("课表设置", onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SharedSettingsGroup("课表名称") { SharedSettingRow("课表名称", "我的课表") }
            SharedSettingsGroup("课表数据") {
                SharedSettingRow("上课时间", "点击此处更改", onClick = { onNavigate(SharedScreen.TIME_TABLE) })
                HorizontalDivider()
                SharedSettingRow("第一周的第一天", "2026-08-31")
                HorizontalDivider()
                SharedSettingRow("当前周", "第1周")
                HorizontalDivider()
                SharedSettingRow("一天课程节数", "${timeSlotCount}节")
                HorizontalDivider()
                SharedSettingRow("学期周数", "20周")
                HorizontalDivider()
                SharedSettingRow("管理已添加课程", "集中编辑或清空课程", onClick = { onNavigate(SharedScreen.COURSES) })
            }
            SharedSettingsGroup("课表外观") {
                SharedSettingRow(
                    "显示周六",
                    trailing = { Switch(checked = appearance.showSaturday, onCheckedChange = { onAppearanceChange(appearance.copy(showSaturday = it)) }) },
                )
                HorizontalDivider()
                SharedSettingRow(
                    "显示周日",
                    trailing = { Switch(checked = appearance.showSunday, onCheckedChange = { onAppearanceChange(appearance.copy(showSunday = it)) }) },
                )
                HorizontalDivider()
                SharedSettingRow("更多外观设置", "课程格子、文字与颜色", onClick = { onNavigate(SharedScreen.APPEARANCE) })
            }
            SharedSettingsGroup("默认配置") {
                SharedSettingRow("桌面小部件", "添加、调整和排查课表小部件", onClick = { onNavigate(SharedScreen.WIDGET) })
            }
        }
    }
}

@Composable
private fun SharedAppearanceScreen(
    appearance: SleepDownAppearance,
    onAppearanceChange: (SleepDownAppearance) -> Unit,
    onBack: () -> Unit,
) {
    SharedScreenScaffold("课表外观", onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(Color(0xFFCAD8EA)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    SleepDownPalette.colors.take(5).forEachIndexed { index, argb ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((82 + index * 14).dp)
                                .background(Color(argb).copy(alpha = appearance.itemAlpha), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = .55f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (index == 0) "高等数学" else "课程", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
            SharedSettingsGroup("整体") {
                SharedSettingRow("课程表背景", "默认渐变背景")
                HorizontalDivider()
                SharedSettingRow(
                    "显示网格辅助线",
                    trailing = { Switch(checked = appearance.showGrid, onCheckedChange = { onAppearanceChange(appearance.copy(showGrid = it)) }) },
                )
                HorizontalDivider()
                SharedSettingRow("界面文字颜色", "#FF000000")
                HorizontalDivider()
                SharedSettingRow("表头文字大小", "11sp")
                HorizontalDivider()
                SharedSettingRow(
                    "节数栏显示时间",
                    trailing = { Switch(checked = appearance.showTimeBar, onCheckedChange = { onAppearanceChange(appearance.copy(showTimeBar = it)) }) },
                )
                HorizontalDivider()
                SharedSettingRow(
                    "显示周六",
                    trailing = { Switch(checked = appearance.showSaturday, onCheckedChange = { onAppearanceChange(appearance.copy(showSaturday = it)) }) },
                )
                HorizontalDivider()
                SharedSettingRow(
                    "显示周日",
                    trailing = { Switch(checked = appearance.showSunday, onCheckedChange = { onAppearanceChange(appearance.copy(showSunday = it)) }) },
                )
            }
            SharedSettingsGroup("课程格子") {
                SharedSettingRow("课程文字颜色", "#FFFFFFFF")
                HorizontalDivider()
                SharedSettingRow("格子边框颜色", "#80FFFFFF")
                HorizontalDivider()
                SharedSettingRow("课程格子高度", "${appearance.itemHeightDp}dp", onClick = {
                    onAppearanceChange(appearance.copy(itemHeightDp = if (appearance.itemHeightDp >= 88) 56 else appearance.itemHeightDp + 8))
                })
                HorizontalDivider()
                SharedSettingRow("格子圆角半径", "${appearance.itemRadiusDp}dp", onClick = {
                    onAppearanceChange(appearance.copy(itemRadiusDp = if (appearance.itemRadiusDp >= 16) 0 else appearance.itemRadiusDp + 4))
                })
                HorizontalDivider()
                SharedSettingRow("课程格子不透明度", "${(appearance.itemAlpha * 100).toInt()}%", onClick = {
                    val nextAlpha = if (appearance.itemAlpha >= 0.9f) 0.3f else appearance.itemAlpha + 0.1f
                    onAppearanceChange(appearance.copy(itemAlpha = nextAlpha))
                })
                HorizontalDivider()
                SharedSettingRow("课程显示文字大小", "${appearance.itemTextSize}sp", onClick = {
                    onAppearanceChange(appearance.copy(itemTextSize = if (appearance.itemTextSize >= 18) 10 else appearance.itemTextSize + 2))
                })
                HorizontalDivider()
                SharedSettingRow(
                    "在格子内显示上课地点",
                    trailing = { Switch(checked = appearance.showLocation, onCheckedChange = { onAppearanceChange(appearance.copy(showLocation = it)) }) },
                )
                HorizontalDivider()
                SharedSettingRow(
                    "在格子内显示授课老师",
                    trailing = { Switch(checked = appearance.showTeacher, onCheckedChange = { onAppearanceChange(appearance.copy(showTeacher = it)) }) },
                )
            }
        }
    }
}

@Composable
private fun SharedCoursesScreen(
    courses: List<SleepDownCourse>,
    onBack: () -> Unit,
    onEdit: (SleepDownCourse) -> Unit,
    onDelete: (SleepDownCourse) -> Unit,
    onAdd: () -> Unit,
) {
    var deleteCourse by remember { mutableStateOf<SleepDownCourse?>(null) }
    SharedScreenScaffold("课程管理", onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("轻触课程编辑，使用删除按钮移除课程", modifier = Modifier.padding(24.dp), color = Color(0xFF626466))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(courses) { course ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(Color(SleepDownPalette.colorFor(course.name)).copy(alpha = .32f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).clickable { onEdit(course) }) {
                                Text(course.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                val details = listOf(course.teacher, course.room).filter { it.isNotBlank() }
                                if (details.isNotEmpty()) Text(details.joinToString(" · "), color = Color(0xFF626466), fontSize = 12.sp)
                            }
                            TextButton(onClick = { deleteCourse = course }) { Text("删除") }
                        }
                    }
                }
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("＋ 添加课程") }
        }
    }
    deleteCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { deleteCourse = null },
            title = { Text("删除课程") },
            text = { Text("确定要删除“${course.name}”吗？它的所有时间段都将会被删除。") },
            confirmButton = {
                TextButton(onClick = { onDelete(course); deleteCourse = null }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { deleteCourse = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SharedCourseEditorScreen(course: SleepDownCourse?, onBack: () -> Unit, onSave: (SleepDownCourse) -> Unit) {
    var name by remember(course) { mutableStateOf(course?.name.orEmpty()) }
    var teacher by remember(course) { mutableStateOf(course?.teacher.orEmpty()) }
    var room by remember(course) { mutableStateOf(course?.room.orEmpty()) }
    var note by remember(course) { mutableStateOf(course?.note.orEmpty()) }
    var credit by remember(course) { mutableStateOf(course?.credit?.takeIf { it != 0f }?.toString().orEmpty()) }
    var day by remember(course) { mutableStateOf(course?.day ?: 1) }
    var node by remember(course) { mutableStateOf(course?.startNode ?: 1) }
    var step by remember(course) { mutableStateOf(course?.step ?: 1) }
    var startWeek by remember(course) { mutableStateOf(course?.startWeek ?: 1) }
    var endWeek by remember(course) { mutableStateOf(course?.endWeek ?: 20) }
    var weekType by remember(course) { mutableStateOf(course?.weekType ?: 0) }
    var color by remember(course) { mutableStateOf(course?.color ?: 0) }
    val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val weekTypeName = when (weekType) {
        1 -> "单周"
        2 -> "双周"
        else -> "每周"
    }
    SharedScreenScaffold(if (course == null) "添加课程" else "编辑课程", onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SharedSettingsGroup("课程信息") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("课程名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("授课老师") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("上课地点") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = credit,
                        onValueChange = { value -> if (value.isEmpty() || value.all { it.isDigit() || it == '.' }) credit = value },
                        label = { Text("学分") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                HorizontalDivider()
                SharedSettingRow("颜色", if (color == 0) "自动分配" else "已选择", onClick = { color = 0 })
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SleepDownPalette.colors.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(argb), RoundedCornerShape(50))
                                .border(if (color == argb) 3.dp else 1.dp, Color.White, RoundedCornerShape(50))
                                .clickable { color = argb },
                        )
                    }
                }
            }
            SharedSettingsGroup("时间段 1") {
                SharedSettingRow("周数", "第${startWeek}-${endWeek}周 · $weekTypeName", onClick = {
                    if (startWeek == 1 && endWeek == 20) {
                        startWeek = 2
                    } else if (startWeek == 2 && endWeek == 20) {
                        startWeek = 1
                        endWeek = 12
                    } else {
                        startWeek = 1
                        endWeek = 20
                    }
                })
                HorizontalDivider()
                SharedSettingRow("星期", "周${dayNames[day - 1]}", onClick = { day = if (day == 7) 1 else day + 1 })
                HorizontalDivider()
                SharedSettingRow("开始节", "第${node}节", onClick = { node = if (node == 12) 1 else node + 1 })
                HorizontalDivider()
                SharedSettingRow("连续节数", "${step}节", onClick = { step = if (step == 4) 1 else step + 1 })
                HorizontalDivider()
                SharedSettingRow("单双周", weekTypeName, onClick = { weekType = (weekType + 1) % 3 })
                HorizontalDivider()
                SharedSettingRow("时间", "跟随时间表")
                HorizontalDivider()
                SharedSettingRow("自定义时间", trailing = { Switch(false, {}) })
            }
            Button(
                onClick = {
                    onSave(
                        SleepDownCourse(
                            name = name.ifBlank { "未命名课程" },
                            teacher = teacher,
                            room = room,
                            day = day,
                            startNode = node,
                            step = step,
                            color = color,
                            startWeek = startWeek,
                            endWeek = endWeek,
                            weekType = weekType,
                            note = note,
                            credit = credit.toFloatOrNull() ?: 0f,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) { Text("保存") }
        }
    }
}

@Composable
private fun SharedTimeTableScreen(
    slots: List<SleepDownTimeSlot>,
    onSlotsChange: (List<SleepDownTimeSlot>) -> Unit,
    onBack: () -> Unit,
) {
    var equalDuration by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingStart by remember { mutableStateOf("") }
    var editingEnd by remember { mutableStateOf("") }

    fun openEditor(index: Int) {
        val slot = slots[index]
        editingIndex = index
        editingStart = slot.start
        editingEnd = slot.end
    }

    SharedScreenScaffold("时间表", onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("要用多少节就调整多少节的时间，多余的节数忽略即可。\n使用 24 小时制。", color = Color(0xFF626466), lineHeight = 18.sp)
            }
            item {
                SharedSettingsGroup("编辑时间表") {
                    SharedSettingRow("每节课时长相同", trailing = { Switch(checked = equalDuration, onCheckedChange = { equalDuration = it }) })
                    HorizontalDivider()
                    SharedSettingRow("当前节数", "${slots.size}节")
                }
            }
            itemsIndexed(slots, key = { _, slot -> slot.node }) { index, slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(Color.White),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${index + 1}", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { openEditor(index) }) {
                            Text("${slot.start}-${slot.end}")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val remaining = slots.filterIndexed { itemIndex, _ -> itemIndex != index }
                                    .mapIndexed { itemIndex, item -> item.copy(node = itemIndex + 1) }
                                onSlotsChange(remaining)
                            },
                            enabled = slots.size > 1,
                        ) { Text("删除") }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        val nextNode = slots.size + 1
                        onSlotsChange(slots + SleepDownTimeSlot(nextNode, "00:00", "00:00"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("＋ 添加节次") }
            }
        }
    }

    editingIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("编辑第${index + 1}节") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editingStart,
                        onValueChange = { editingStart = it },
                        label = { Text("开始时间") },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = editingEnd,
                        onValueChange = { editingEnd = it },
                        label = { Text("结束时间") },
                        placeholder = { Text("08:50") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = slots.mapIndexed { itemIndex, slot ->
                        if (itemIndex == index) slot.copy(start = editingStart, end = editingEnd) else slot
                    }
                    onSlotsChange(updated)
                    editingIndex = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingIndex = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SharedImportScreen(onBack: () -> Unit) {
    SharedScreenScaffold("导入课表", onBack) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("CSV") }; OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("JSON 备份") } }
            SharedSettingsGroup("WakeUp CSV") { SharedSettingRow("列顺序", "课程名称、星期、开始节数、结束节数、老师、地点、周数"); HorizontalDivider(); SharedSettingRow("新课表名称", "导入课表"); HorizontalDivider(); SharedSettingRow("第一周的第一天", "2026-08-31") }
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("↑ 选择文件") }
        }
    }
}

@Composable
private fun SharedWidgetScreen(onBack: () -> Unit) {
    SharedScreenScaffold("桌面小部件", onBack) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SharedSettingsGroup("添加小部件") { SharedSettingRow("将课表添加到桌面", "有日视图和周视图可选哦，添加成功后可以左右滑动桌面查找。", onClick = {}) }
            SharedSettingsGroup("使用说明") {
                SharedSettingRow("如何添加小部件？", "长按桌面空白处，选择桌面小工具即可。")
                HorizontalDivider()
                SharedSettingRow("如何调整小部件大小？", "桌面长按小部件调整。")
                HorizontalDivider()
                SharedSettingRow("如何调整小部件样式？", "小部件右上角的调整按钮可以切换样式。")
                HorizontalDivider()
                SharedSettingRow("小部件刷新不及时", "请允许应用后台运行，必要时手动刷新。")
                HorizontalDivider()
                SharedSettingRow("更多问题", "根据反馈不定时更新")
            }
        }
    }
}

@Composable
private fun SharedCourseDialog(course: SleepDownCourse, onDismiss: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(course.name, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { if (course.teacher.isNotBlank()) Text("授课老师：${course.teacher}"); if (course.room.isNotBlank()) Text("上课地点：${course.room}"); Text("周${listOf("一", "二", "三", "四", "五", "六", "日")[course.day - 1]} · 第${course.startNode}-${course.startNode + course.step - 1}节 · 第1-20周") } }, confirmButton = { Button(onClick = onEdit) { Text("编辑") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}
