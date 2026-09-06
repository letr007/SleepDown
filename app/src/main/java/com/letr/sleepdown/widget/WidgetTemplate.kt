package com.letr.sleepdown.widget

import android.content.Context
import android.net.Uri

private fun templateId(kind: WidgetKind): Int? = when (kind) {
    WidgetKind.TODAY -> -1
    WidgetKind.WEEK -> -2
    else -> null
}

private fun hasStyle(context: Context, id: Int): Boolean =
    context.getSharedPreferences("widget_style_$id", Context.MODE_PRIVATE).contains("courseTextSize")

internal fun saveWidgetTemplate(context: Context, kind: WidgetKind, style: WidgetStyle, background: Boolean, colors: Boolean) {
    val id = requireNotNull(templateId(kind))
    copyStyle(context, style, id)
    WidgetPreferences.setShowBackground(context, id, background)
    WidgetPreferences.setShowColorBlocks(context, id, colors)
}

internal fun initializeWidgetStyle(context: Context, widgetId: Int, kind: WidgetKind) {
    val source = templateId(kind) ?: return
    if (hasStyle(context, widgetId) || WidgetPreferences.tableId(context, widgetId) > 0 || !hasStyle(context, source)) return
    copyStyle(context, WidgetStyle.read(context, source), widgetId)
    WidgetPreferences.setShowBackground(context, widgetId, WidgetPreferences.showBackground(context, source))
    WidgetPreferences.setShowColorBlocks(context, widgetId, WidgetPreferences.showColorBlocks(context, source))
}

private fun copyStyle(context: Context, source: WidgetStyle, targetId: Int) {
    var copy = source.copy(headerTextSize = WidgetStyle.read(context, targetId).headerTextSize)
    try {
        if (copy.backgroundImage.isNotBlank()) {
            copy = copy.copy(backgroundImage = stageWidgetBackground(context, Uri.parse(copy.backgroundImage), targetId))
            copy = commitWidgetBackground(context, targetId, copy)
        }
        copy.save(context, targetId)
        if (copy.backgroundImage.isBlank()) clearWidgetBackground(context, targetId)
    } finally {
        discardWidgetBackgroundDraft(context, targetId)
    }
}
