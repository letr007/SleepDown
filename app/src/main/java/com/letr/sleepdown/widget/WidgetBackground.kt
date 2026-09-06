package com.letr.sleepdown.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.IOException
import java.io.File
import android.util.AtomicFile
import kotlin.math.max
import kotlin.math.roundToInt

/** Keep the background below 1.6 MiB so it can share a widget update with the week bitmap. */
internal fun loadWidgetBackground(context: Context, uri: Uri): Bitmap {
    val limit = 640
    if (Build.VERSION.SDK_INT >= 28) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = minOf(1f, limit.toFloat() / max(info.size.width, info.size.height))
            decoder.setTargetSize((info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    (context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open widget background"))
        .use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Invalid widget background image")
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / inSampleSize > limit * 2) inSampleSize *= 2
    }
    var decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("Cannot decode widget background")
    if (bounds.outMimeType == "image/jpeg") {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
        } ?: throw IOException("Cannot read widget background orientation")
        val matrix = android.graphics.Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }
                8 -> setRotate(270f)
            }
        }
        if (!matrix.isIdentity) {
            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (oriented !== decoded) decoded.recycle()
            decoded = oriented
        }
    }
    val scale = minOf(1f, limit.toFloat() / max(decoded.width, decoded.height))
    if (scale == 1f) return decoded
    return Bitmap.createScaledBitmap(decoded, (decoded.width * scale).roundToInt().coerceAtLeast(1),
        (decoded.height * scale).roundToInt().coerceAtLeast(1), true).also { decoded.recycle() }
}

internal fun stageWidgetBackground(context: Context, uri: Uri, widgetId: Int): String {
    val bitmap = loadWidgetBackground(context, uri)
    val file = File(context.cacheDir, "widget-background-$widgetId.png")
    val atomic = AtomicFile(file)
    try {
        val stream = atomic.startWrite()
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException("Cannot save widget background")
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    } finally {
        bitmap.recycle()
    }
    return Uri.fromFile(file).toString()
}

internal fun commitWidgetBackground(context: Context, widgetId: Int, style: WidgetStyle): WidgetStyle {
    val draft = File(context.cacheDir, "widget-background-$widgetId.png")
    if (style.backgroundImage != Uri.fromFile(draft).toString()) return style
    val file = File(context.filesDir, "widget-background-$widgetId.png")
    val atomic = AtomicFile(file)
    val stream = atomic.startWrite()
    try {
        draft.inputStream().use { it.copyTo(stream) }
        atomic.finishWrite(stream)
    } catch (error: Exception) {
        atomic.failWrite(stream)
        throw error
    }
    return style.copy(backgroundImage = Uri.fromFile(file).toString())
}

internal fun discardWidgetBackgroundDraft(context: Context, widgetId: Int) {
    AtomicFile(File(context.cacheDir, "widget-background-$widgetId.png")).delete()
}

internal fun clearWidgetBackground(context: Context, widgetId: Int) {
    AtomicFile(File(context.filesDir, "widget-background-$widgetId.png")).delete()
    discardWidgetBackgroundDraft(context, widgetId)
}
