package com.letr.sleepdown.widget

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.R
import java.io.File
import java.io.IOException
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetBackgroundTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val widgetId = 900010
    private val input = File(context.cacheDir, "widget-background-input-test.png")

    @After
    fun cleanup() {
        input.delete()
        WidgetPreferences.clear(context, intArrayOf(widgetId))
    }

    private fun image(color: Int): Uri {
        val bitmap = Bitmap.createBitmap(1800, 900, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        input.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return Uri.fromFile(input)
    }

    @Test
    fun stagedSelectionCannotChangeTheSavedImageUntilConfirmation() {
        val selected = stageWidgetBackground(context, image(Color.RED), widgetId)
        val saved = commitWidgetBackground(context, widgetId, WidgetStyle(backgroundImage = selected))
        saved.save(context, widgetId)
        input.delete()
        val bitmap = loadWidgetBackground(context, Uri.parse(saved.backgroundImage))
        assertEquals(640, bitmap.width)
        assertEquals(320, bitmap.height)
        assertTrue(bitmap.allocationByteCount <= 640 * 640 * 4)
        assertEquals(Color.RED, bitmap.getPixel(1, 1))
        bitmap.recycle()

        stageWidgetBackground(context, image(Color.BLUE), widgetId)
        discardWidgetBackgroundDraft(context, widgetId)
        assertFalse(File(Uri.parse(selected).path!!).exists())
        val unchanged = loadWidgetBackground(context, Uri.parse(WidgetStyle.read(context, widgetId).backgroundImage))
        assertEquals(Color.RED, unchanged.getPixel(1, 1))
        unchanged.recycle()
        input.writeText("not an image")
        try {
            stageWidgetBackground(context, Uri.fromFile(input), widgetId)
            fail("An invalid image must not be accepted")
        } catch (_: IOException) {
            assertEquals(saved, WidgetStyle.read(context, widgetId))
        }
        WidgetPreferences.clear(context, intArrayOf(widgetId))
        assertFalse(File(Uri.parse(saved.backgroundImage).path!!).exists())
        assertFalse(File(Uri.parse(selected).path!!).exists())
    }

    @Test
    fun roundedBackgroundCropsTheImageBeforeApplyingCorners() {
        val source = Bitmap.createBitmap(1800, 900, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        android.graphics.Canvas(source).drawRect(600f, 0f, 1200f, 900f,
            android.graphics.Paint().apply { color = Color.BLUE })
        input.outputStream().use { assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        source.recycle()
        val rendered = loadRoundedWidgetBackground(context, Uri.fromFile(input), widgetId)
        try {
            assertEquals(0, Color.alpha(rendered.getPixel(0, 0)))
            assertEquals(Color.BLUE, rendered.getPixel(rendered.width * 3 / 10, rendered.height / 2))
            assertEquals(Color.BLUE, rendered.getPixel(rendered.width / 2, rendered.height / 2))
            assertTrue(rendered.allocationByteCount <= 640 * 640 * 4)
        } finally {
            rendered.recycle()
        }
    }

    @Test
    fun photoBackgroundUsesItsOwnImageAndDoesNotApplySolidColorTransparency() {
        val staged = stageWidgetBackground(context, image(Color.RED), widgetId)
        commitWidgetBackground(context, widgetId,
            WidgetStyle(backgroundImage = staged, backgroundOpacity = 20)).save(context, widgetId)
        instrumentation.runOnMainSync {
            for (kind in listOf(WidgetKind.TODAY, WidgetKind.WEEK)) {
                for (visible in listOf(true, false)) {
                    WidgetPreferences.setShowBackground(context, widgetId, visible)
                    val views = RemoteViews(context.packageName, layoutFor(kind))
                    applyWidgetStyle(context, views, widgetId, kind)
                    val root = views.apply(context, FrameLayout(context))
                    val photo = root.findViewById<ImageView>(R.id.widget_background_image)
                    assertEquals(if (visible) View.VISIBLE else View.GONE, photo.visibility)
                    assertEquals(View.GONE, root.findViewById<View>(R.id.widget_background).visibility)
                    assertEquals(255, photo.imageAlpha)
                    assertEquals(ImageView.ScaleType.FIT_XY, photo.scaleType)
                    if (visible) {
                        val rendered = (photo.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                        assertEquals(0, Color.alpha(rendered.getPixel(0, 0)))
                        assertEquals(Color.RED, rendered.getPixel(rendered.width / 2, rendered.height / 2))
                        assertTrue(rendered.allocationByteCount <= 640 * 640 * 4)
                    } else {
                        assertNull(photo.drawable)
                    }
                }
            }
        }
    }
}
