package com.letr.sleepdown.widget

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.letr.sleepdown.R
import kotlin.math.roundToInt
import java.util.Locale

@Composable
internal fun WidgetColorDialog(title: String, initial: Int, minAlpha: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    var hex by rememberSaveable { mutableStateOf(String.format(Locale.ROOT, "%08X", initial)) }
    val selected = hex.toLongOrNull(16)?.takeIf { hex.length == 8 }?.toInt()
    val color = selected ?: initial
    val initialHsv = remember(initial) { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    var alpha by rememberSaveable { mutableIntStateOf(AndroidColor.alpha(initial)) }
    fun choose(h: Float = hue, s: Float = saturation, v: Float = brightness, a: Int = alpha) {
        hue = h; saturation = s; brightness = v; alpha = a
        hex = String.format(Locale.ROOT, "%08X", AndroidColor.HSVToColor(a, floatArrayOf(h, s, v)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorCanvas(Modifier.weight(1f).height(200.dp), stringResource(R.string.widget_color_saturation),
                        onPosition = { x, y -> choose(s = x, v = 1f - y) }) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))))))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        drawCircle(Color.White, 6.dp.toPx(), Offset(saturation * size.width, (1f - brightness) * size.height),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                    ColorCanvas(Modifier.width(24.dp).height(200.dp), stringResource(R.string.widget_color_hue),
                        onPosition = { _, y -> choose(h = y * 360f) }) {
                        drawRect(Brush.verticalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }))
                        val y = hue / 360f * size.height
                        drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                    }
                }
                ColorCanvas(Modifier.fillMaxWidth().height(28.dp), stringResource(R.string.widget_color_alpha),
                    onPosition = { x, _ -> choose(a = (x * 255).roundToInt().coerceAtLeast(minAlpha)) }) {
                    val side = 7.dp.toPx()
                    for (row in 0..(size.height / side).toInt()) for (column in 0..(size.width / side).toInt()) {
                        drawRect(if ((row + column) % 2 == 0) Color.LightGray else Color.White,
                            Offset(column * side, row * side), Size(side, side))
                    }
                    drawRect(Brush.horizontalGradient(listOf(Color(color).copy(alpha = 0f), Color(color).copy(alpha = 1f))))
                    val x = AndroidColor.alpha(color) / 255f * size.width
                    drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(48.dp).height(56.dp).background(Color(color)))
                    OutlinedTextField(value = hex, onValueChange = { value ->
                        if (value.length <= 8 && value.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) {
                            hex = value.uppercase(Locale.ROOT)
                            value.toLongOrNull(16)?.takeIf { value.length == 8 }?.toInt()?.let { parsed ->
                                val channels = FloatArray(3).also { AndroidColor.colorToHSV(parsed, it) }
                                hue = channels[0]; saturation = channels[1]; brightness = channels[2]; alpha = AndroidColor.alpha(parsed)
                            }
                        }
                    }, label = { Text("# AARRGGBB") }, singleLine = true, isError = selected == null, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { TextButton(enabled = selected != null, onClick = {
            selected?.let { onSelect(androidx.core.graphics.ColorUtils.setAlphaComponent(it, AndroidColor.alpha(it).coerceAtLeast(minAlpha))) }
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ColorCanvas(modifier: Modifier, label: String, onPosition: (Float, Float) -> Unit, draw: DrawScope.() -> Unit) {
    val update by rememberUpdatedState(onPosition)
    Canvas(modifier.semantics { contentDescription = label }
        .pointerInput(Unit) {
            detectTapGestures { update((it.x / size.width).coerceIn(0f, 1f), (it.y / size.height).coerceIn(0f, 1f)) }
        }.pointerInput(Unit) {
            detectDragGestures(onDragStart = { update((it.x / size.width).coerceIn(0f, 1f), (it.y / size.height).coerceIn(0f, 1f)) }) { change, _ ->
                change.consume()
                update((change.position.x / size.width).coerceIn(0f, 1f), (change.position.y / size.height).coerceIn(0f, 1f))
            }
        }, onDraw = draw)
}
