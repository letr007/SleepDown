package com.letr.sleepdown.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.letr.sleepdown.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.letr.sleepdown.AppContainer
import com.letr.sleepdown.data.TableEntity
import com.letr.sleepdown.ui.TimetableTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.util.Log
import kotlin.math.roundToInt

private data class WidgetConfiguration(
    val tableId: Long,
    val showBackground: Boolean,
    val showColorBlocks: Boolean,
    val style: WidgetStyle,
)

class WidgetConfigActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetKind = WidgetKind.NEXT
    private var saving by mutableStateOf(false)
    private var saveError by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val providerName = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
            .orEmpty()
        widgetKind = widgetKindForProvider(providerName)
        val repository = AppContainer.repo(this)

        lifecycleScope.launch {
            try {
                withWidgetUpdate { initializeWidgetStyle(this@WidgetConfigActivity, appWidgetId, widgetKind) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WidgetConfiguration", "Failed to initialize default style", error)
                saveError = true
            }
            setContent {
                TimetableTheme {
                    WidgetConfigScreen(
                        repository = repository,
                        kind = widgetKind,
                        appWidgetId = appWidgetId,
                        saving = saving,
                        saveError = saveError,
                        onCancel = { if (!saving) finish() },
                        onConfirm = ::finishConfiguration,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) discardWidgetBackgroundDraft(this, appWidgetId)
        super.onDestroy()
    }

    private fun finishConfiguration(configuration: WidgetConfiguration) {
        if (saving) return
        saving = true
        saveError = false
        lifecycleScope.launch {
            try {
                val refreshed = refreshWidgetForConfiguration(this@WidgetConfigActivity, appWidgetId, widgetKind) {
                    val style = commitWidgetBackground(this@WidgetConfigActivity, appWidgetId, configuration.style)
                    WidgetPreferences.setTableId(this@WidgetConfigActivity, appWidgetId, configuration.tableId)
                    WidgetPreferences.setShowBackground(this@WidgetConfigActivity, appWidgetId, configuration.showBackground)
                    WidgetPreferences.setShowColorBlocks(this@WidgetConfigActivity, appWidgetId, configuration.showColorBlocks)
                    style.save(this@WidgetConfigActivity, appWidgetId)
                    WidgetPreferences.setWeek(this@WidgetConfigActivity, appWidgetId, 0)
                    WidgetPreferences.setDayOffset(this@WidgetConfigActivity, appWidgetId, 0)
                    if (configuration.style.backgroundImage.isBlank()) clearWidgetBackground(this@WidgetConfigActivity, appWidgetId)
                }
                if (!refreshed) {
                    finish()
                    return@launch
                }
                setResult(
                    Activity.RESULT_OK,
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                )
                finish()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WidgetConfiguration", "Failed to refresh configured widget", error)
                saveError = true
            } finally {
                saving = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    repository: com.letr.sleepdown.data.TimetableRepository,
    kind: WidgetKind,
    appWidgetId: Int,
    saving: Boolean,
    saveError: Boolean,
    onCancel: () -> Unit,
    onConfirm: (WidgetConfiguration) -> Unit,
) {
    val tables by repository.observeTables().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    var selectedTableId by rememberSaveable {
        mutableLongStateOf(WidgetPreferences.tableId(context, appWidgetId).takeIf { it > 0L }
            ?: AppContainer.currentTableId(context))
    }
    var showBackground by rememberSaveable {
        mutableStateOf(WidgetPreferences.showBackground(context, appWidgetId))
    }
    var showColorBlocks by rememberSaveable {
        mutableStateOf(WidgetPreferences.showColorBlocks(context, appWidgetId))
    }
    var style by rememberSaveable { mutableStateOf(WidgetStyle.read(context, appWidgetId)) }
    var imageLoading by androidx.compose.runtime.remember { mutableStateOf(false) }
    var imageError by androidx.compose.runtime.remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var templateSaving by androidx.compose.runtime.remember { mutableStateOf(false) }
    var templateError by androidx.compose.runtime.remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            imageLoading = true
            imageError = false
            try {
                val image = withContext(Dispatchers.IO) { stageWidgetBackground(context, uri, appWidgetId) }
                style = style.copy(backgroundImage = image)
                showBackground = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WidgetConfiguration", "Failed to read background image", error)
                imageError = true
            } finally {
                imageLoading = false
            }
        }
    }

    LaunchedEffect(tables) {
        if (tables.isEmpty()) {
            repository.ensureDefaultTable(context.getString(R.string.default_table_name))
        } else if (selectedTableId <= 0L || tables.none { it.id == selectedTableId }) {
            selectedTableId = tables.first().id
        }
    }

    BackHandler(enabled = saving || imageLoading || templateSaving) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(widgetConfigTitle(kind)) },
                navigationIcon = { TextButton(onClick = onCancel, enabled = !saving && !imageLoading && !templateSaving) { Text(stringResource(R.string.cancel)) } },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (kind == WidgetKind.TODAY || kind == WidgetKind.WEEK) {
                    TextButton(enabled = !saving && !imageLoading && !templateSaving, onClick = {
                        val draft = style
                        val background = showBackground
                        val colors = showColorBlocks
                        templateSaving = true
                        templateError = false
                        scope.launch {
                            try {
                                withWidgetUpdate { saveWidgetTemplate(context, kind, draft, background, colors) }
                                android.widget.Toast.makeText(context, R.string.widget_template_saved, android.widget.Toast.LENGTH_SHORT).show()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                Log.e("WidgetConfiguration", "Failed to save default style", error)
                                templateError = true
                            } finally {
                                templateSaving = false
                            }
                        }
                    }) { Text(stringResource(R.string.widget_save_template)) }
                }
                Button(
                    onClick = {
                        onConfirm(
                            WidgetConfiguration(
                                tableId = selectedTableId,
                                showBackground = showBackground,
                                showColorBlocks = showColorBlocks,
                                style = style,
                            ),
                        )
                    },
                    enabled = !saving && !imageLoading && !templateSaving && tables.any { it.id == selectedTableId },
                ) {
                    Text(stringResource(if (imageLoading) R.string.widget_image_loading else if (saving) R.string.widget_saving else R.string.done))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (templateError) {
                item { Text(stringResource(R.string.widget_template_failed), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
            if (imageError) {
                item { Text(stringResource(R.string.widget_image_failed), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
            if (saveError) {
                item {
                    Text(stringResource(R.string.widget_configuration_refresh_failed),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
            item {
                Text(
                    text = widgetConfigDescription(kind),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (kind == WidgetKind.TODAY || kind == WidgetKind.WEEK) {
                item {
                    WidgetStyleSection(
                        kind = kind,
                        showBackground = showBackground,
                        onShowBackgroundChange = { if (!saving) showBackground = it },
                        showColorBlocks = showColorBlocks,
                        onShowColorBlocksChange = { if (!saving) showColorBlocks = it },
                        style = style,
                        imageLoading = imageLoading || saving || templateSaving,
                        onChooseImage = { imagePicker.launch(arrayOf("image/*")) },
                        onStyleChange = { if (!saving) style = it },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.table_name),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tables.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.widget_preparing_timetable),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            } else {
                items(tables, key = { it.id }) { table ->
                    TableOption(
                        table = table,
                        selected = table.id == selectedTableId,
                        onClick = { if (!saving) selectedTableId = table.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStyleSection(
    kind: WidgetKind,
    showBackground: Boolean,
    onShowBackgroundChange: (Boolean) -> Unit,
    showColorBlocks: Boolean,
    onShowColorBlocksChange: (Boolean) -> Unit,
    style: WidgetStyle,
    imageLoading: Boolean,
    onChooseImage: () -> Unit,
    onStyleChange: (WidgetStyle) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        StyleToggleRow(stringResource(R.string.widget_show_header), style.showHeader) { onStyleChange(style.copy(showHeader = it)) }
        if (style.showHeader) {
            StyleToggleRow(stringResource(R.string.widget_show_buttons), style.showButtons) { onStyleChange(style.copy(showButtons = it)) }
            StyleToggleRow(stringResource(R.string.widget_show_date), style.showDate) { onStyleChange(style.copy(showDate = it)) }
            StyleSlider(stringResource(R.string.widget_header_size), style.headerTextSize, 8..32, "sp") { onStyleChange(style.copy(headerTextSize = it)) }
            StyleColor(stringResource(R.string.widget_header_color), style.textColor, minAlpha = 60) { onStyleChange(style.copy(textColor = it)) }
        }
        StyleToggleRow(stringResource(R.string.widget_show_background), showBackground, onShowBackgroundChange)
        if (showBackground) {
            TextButton(onClick = onChooseImage, enabled = !imageLoading) {
                Text(stringResource(if (style.backgroundImage.isBlank()) R.string.widget_choose_image else R.string.widget_replace_image))
            }
            if (style.backgroundImage.isNotBlank()) {
                TextButton(onClick = { onStyleChange(style.copy(backgroundImage = "")) }, enabled = !imageLoading) {
                    Text(stringResource(R.string.widget_use_solid_background))
                }
            }
            StyleColor(stringResource(R.string.widget_background_color), androidx.core.graphics.ColorUtils.setAlphaComponent(style.backgroundColor, style.backgroundOpacity * 255 / 100)) {
                onStyleChange(style.copy(backgroundColor = it or (0xFF shl 24), backgroundOpacity = android.graphics.Color.alpha(it) * 100 / 255, backgroundImage = ""))
            }
            if (style.backgroundImage.isBlank()) {
                StyleSlider(stringResource(R.string.widget_background_transparency), 100 - style.backgroundOpacity, 0..100, "%") { onStyleChange(style.copy(backgroundOpacity = 100 - it)) }
            }
        }
        StyleToggleRow(stringResource(R.string.widget_show_color_blocks), showColorBlocks, onShowColorBlocksChange)
        StyleSlider(stringResource(R.string.widget_course_transparency), 100 - style.courseOpacity, 0..100, "%") { onStyleChange(style.copy(courseOpacity = 100 - it)) }
        StyleSlider(stringResource(R.string.widget_course_size), style.courseTextSize, 8..32, "sp") { onStyleChange(style.copy(courseTextSize = it)) }
        StyleColor(stringResource(R.string.widget_course_text_color), style.courseTextColor, minAlpha = 60) { onStyleChange(style.copy(courseTextColor = it)) }
        StyleToggleRow(stringResource(R.string.widget_show_location), style.showLocation) { onStyleChange(style.copy(showLocation = it)) }
        StyleToggleRow(stringResource(R.string.widget_show_teacher), style.showTeacher) { onStyleChange(style.copy(showTeacher = it)) }
        StyleToggleRow(stringResource(R.string.widget_text_compose), style.textColorCompose) { onStyleChange(style.copy(textColorCompose = it)) }
        if (kind == WidgetKind.WEEK) {
            StyleToggleRow(stringResource(R.string.widget_show_saturday), style.showSaturday) { onStyleChange(style.copy(showSaturday = it)) }
            StyleToggleRow(stringResource(R.string.widget_show_sunday), style.showSunday) { onStyleChange(style.copy(showSunday = it)) }
            StyleToggleRow(stringResource(R.string.widget_show_other_week), style.showOtherWeekCourses) { onStyleChange(style.copy(showOtherWeekCourses = it)) }
            if (style.showOtherWeekCourses) {
                StyleSlider(stringResource(R.string.widget_other_week_opacity), style.otherWeekOpacity, 0..100, "%") { onStyleChange(style.copy(otherWeekOpacity = it)) }
            }
            StyleToggleRow(stringResource(R.string.widget_show_grid), style.showGrid) { onStyleChange(style.copy(showGrid = it)) }
            StyleToggleRow(stringResource(R.string.widget_dotted_border), style.dottedBorder) { onStyleChange(style.copy(dottedBorder = it)) }
            StyleColor(stringResource(R.string.widget_border_color), style.strokeColor) { onStyleChange(style.copy(strokeColor = it)) }
            StyleSlider(stringResource(R.string.widget_border_opacity), android.graphics.Color.alpha(style.strokeColor) * 100 / 255, 0..100, "%") {
                onStyleChange(style.copy(strokeColor = androidx.core.graphics.ColorUtils.setAlphaComponent(style.strokeColor, it * 255 / 100)))
            }
            StyleToggleRow(stringResource(R.string.widget_stroke_compose), style.strokeColorCompose) { onStyleChange(style.copy(strokeColorCompose = it)) }
            StyleToggleRow(stringResource(R.string.widget_show_time), style.showTime) { onStyleChange(style.copy(showTime = it)) }
            StyleToggleRow(stringResource(R.string.widget_show_time_bar), style.showTimeBar) { onStyleChange(style.copy(showTimeBar = it)) }
            StyleSlider(stringResource(R.string.widget_row_height), style.rowHeight, 32..128, "dp") { onStyleChange(style.copy(rowHeight = it)) }
            StyleSlider(stringResource(R.string.widget_course_radius), style.radius, 0..32, "dp") { onStyleChange(style.copy(radius = it)) }
            StyleToggleRow(stringResource(R.string.widget_center_horizontal), style.centerHorizontal) { onStyleChange(style.copy(centerHorizontal = it)) }
            StyleToggleRow(stringResource(R.string.widget_center_vertical), style.centerVertical) { onStyleChange(style.copy(centerVertical = it)) }
        }
    }
}

@Composable
private fun StyleSlider(label: String, value: Int, range: IntRange, unit: String, onChange: (Int) -> Unit) {
    Text("$label · $value $unit", modifier = Modifier.padding(top = 8.dp))
    Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1)
}

@Composable
private fun StyleColor(label: String, value: Int, minAlpha: Int = 0, onChange: (Int) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { choosing = true }) { Text(label, color = androidx.compose.material3.MaterialTheme.colorScheme.primary) }
    if (choosing) WidgetColorDialog(label, value, minAlpha,
        onSelect = { onChange(it); choosing = false }, onDismiss = { choosing = false })
}

@Composable
private fun StyleToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TableOption(table: TableEntity, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(table.name.ifBlank { stringResource(R.string.default_table_name) })
        }
    }
}

@Composable
private fun widgetConfigTitle(kind: WidgetKind): String = when (kind) {
    WidgetKind.NEXT -> stringResource(R.string.widget_config_next_title)
    WidgetKind.WEEK -> stringResource(R.string.widget_config_week_title)
    WidgetKind.TODAY -> stringResource(R.string.widget_config_today_title)
    WidgetKind.TODAY_MODERN -> stringResource(R.string.widget_config_today_modern_title)
    WidgetKind.TODAY_AND_NEXT_DAY -> stringResource(R.string.widget_config_today_next_title)
}

@Composable
private fun widgetConfigDescription(kind: WidgetKind): String = when (kind) {
    WidgetKind.NEXT -> stringResource(R.string.widget_config_next_description)
    WidgetKind.WEEK -> stringResource(R.string.widget_config_week_description)
    WidgetKind.TODAY -> stringResource(R.string.widget_config_today_description)
    WidgetKind.TODAY_MODERN -> stringResource(R.string.widget_config_today_modern_description)
    WidgetKind.TODAY_AND_NEXT_DAY -> stringResource(R.string.widget_config_today_next_description)
}
