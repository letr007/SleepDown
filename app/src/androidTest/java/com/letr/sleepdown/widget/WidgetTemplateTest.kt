package com.letr.sleepdown.widget

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetTemplateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val ids = intArrayOf(-1, -2, 900101, 900102, 900103)

    @Before
    @After
    fun clear() { WidgetPreferences.clear(context, ids) }

    @Test
    fun newWidgetsInheritOnlyTheirTypeTemplateAndExistingWidgetsStayUnchanged() {
        val source = WidgetStyle(headerTextSize = 24, courseTextSize = 20, showGrid = true, showSunday = false)
        saveWidgetTemplate(context, WidgetKind.WEEK, source, false, false)
        initializeWidgetStyle(context, 900101, WidgetKind.WEEK)
        assertEquals(source.copy(headerTextSize = 11), WidgetStyle.read(context, 900101))
        assertFalse(WidgetPreferences.showBackground(context, 900101))
        assertFalse(WidgetPreferences.showColorBlocks(context, 900101))
        assertEquals(0L, WidgetPreferences.tableId(context, 900101))
        initializeWidgetStyle(context, 900102, WidgetKind.TODAY)
        assertEquals(WidgetStyle(), WidgetStyle.read(context, 900102))
        saveWidgetTemplate(context, WidgetKind.WEEK, source.copy(courseTextSize = 32), true, true)
        initializeWidgetStyle(context, 900101, WidgetKind.WEEK)
        assertEquals(20, WidgetStyle.read(context, 900101).courseTextSize)
        WidgetPreferences.setTableId(context, 900103, 123)
        initializeWidgetStyle(context, 900103, WidgetKind.WEEK)
        assertEquals(WidgetStyle(), WidgetStyle.read(context, 900103))
        assertEquals(123L, WidgetPreferences.tableId(context, 900103))
    }

    @Test
    fun templateAndInstanceImagesSurviveDeletingTheirSource() {
        val file = File(context.cacheDir, "template-test.png")
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        try {
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val draft = stageWidgetBackground(context, Uri.fromFile(file), 900103)
            saveWidgetTemplate(context, WidgetKind.WEEK, WidgetStyle(backgroundImage = draft), true, true)
            WidgetPreferences.clear(context, intArrayOf(900103))
            file.delete()
            initializeWidgetStyle(context, 900101, WidgetKind.WEEK)
            val template = WidgetStyle.read(context, -2)
            val instance = WidgetStyle.read(context, 900101)
            assertNotEquals(template.backgroundImage, instance.backgroundImage)
            WidgetPreferences.clear(context, intArrayOf(-2))
            val copied = loadWidgetBackground(context, Uri.parse(instance.backgroundImage))
            assertEquals(Color.BLUE, copied.getPixel(0, 0))
            copied.recycle()
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }
}
