package com.letr.sleepdown.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.letr.sleepdown.AppContainer
import com.letr.sleepdown.data.TableEntity
import com.letr.sleepdown.ui.TimetableTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetKind = WidgetKind.TODAY_COMPACT

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

        setContent {
            TimetableTheme {
                WidgetConfigScreen(
                    repository = repository,
                    kind = widgetKind,
                    appWidgetId = appWidgetId,
                    onCancel = { finish() },
                    onConfirm = ::finishConfiguration,
                )
            }
        }
    }

    private fun finishConfiguration(tableId: Long) {
        WidgetPreferences.setTableId(this, appWidgetId, tableId)
        WidgetPreferences.setWeek(this, appWidgetId, 0)
        val glanceId = androidx.glance.appwidget.GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        lifecycleScope.launch {
            runCatching {
                widgetForKind(widgetKind).update(this@WidgetConfigActivity, glanceId)
            }
            setResult(
                Activity.RESULT_OK,
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    repository: com.letr.sleepdown.data.TimetableRepository,
    kind: WidgetKind,
    appWidgetId: Int,
    onCancel: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val tables by repository.observeTables().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    var selectedTableId by remember {
        mutableLongStateOf(WidgetPreferences.tableId(context, appWidgetId))
    }

    LaunchedEffect(tables) {
        if (tables.isEmpty()) {
            repository.ensureDefaultTable()
        } else if (selectedTableId <= 0L || tables.none { it.id == selectedTableId }) {
            selectedTableId = tables.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(widgetConfigTitle(kind)) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { onConfirm(selectedTableId) },
                    enabled = selectedTableId > 0L,
                ) {
                    Text("完成")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = widgetConfigDescription(kind),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tables.isEmpty()) {
                Text(
                    text = "正在准备课表…",
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tables, key = { it.id }) { table ->
                        TableOption(
                            table = table,
                            selected = table.id == selectedTableId,
                            onClick = { selectedTableId = table.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableOption(table: TableEntity, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(table.name.ifBlank { "默认课表" })
        }
    }
}

private fun widgetConfigTitle(kind: WidgetKind): String = when (kind) {
    WidgetKind.WEEK -> "配置周课表"
    WidgetKind.TODAY -> "配置今日课程"
    WidgetKind.TODAY_COMPACT -> "配置今日课程"
    WidgetKind.TODAY_MODERN -> "配置今日课程"
    WidgetKind.TODAY_AND_NEXT_DAY -> "配置近日课程"
}

private fun widgetConfigDescription(kind: WidgetKind): String = when (kind) {
    WidgetKind.WEEK -> "选择要在周课表小部件中显示的课表。"
    WidgetKind.TODAY,
    WidgetKind.TODAY_COMPACT,
    WidgetKind.TODAY_MODERN -> "选择要在今日课程小部件中显示的课表。"
    WidgetKind.TODAY_AND_NEXT_DAY -> "选择要在近日课程小部件中显示的课表。"
}
