package com.letr.sleepdown.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letr.sleepdown.data.AppDatabase
import com.letr.sleepdown.data.TimetableRepository
import com.letr.sleepdown.domain.*
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetCollectionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val id = 900301
    private lateinit var repository: TimetableRepository

    @Before
    fun setup() {
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase("timetable.db")
        repository = TimetableRepository(context)
    }

    @After
    fun cleanup() {
        WidgetPreferences.clear(context, intArrayOf(id))
        AppDatabase.instance?.close()
        AppDatabase.instance = null
        context.deleteDatabase("timetable.db")
    }

    @Test
    fun collectionCanSupplyRowsWhileThePublisherWaitsForItsCallback() = runBlocking {
        val today = LocalDate.now()
        val monday = today.minusDays(today.dayOfWeek.value - 1L).toEpochDay()
        val tableId = repository.importDomainTimetable(Timetable(
            id = "callback", name = "Callback table", firstDayEpochDay = monday,
            timeTable = TimeTable("day", "Day", listOf(TimeTableNode(1, 480, 600))),
            courses = listOf(Course(id = "math", name = "Math", slots = listOf(LogicalCourseSlot(
                id = "math-slot", courseId = "math", dayOfWeek = today.plusDays(1).dayOfWeek.value,
                startNode = 1, customTime = MinuteRange(480, 600),
            )))),
        ))
        WidgetPreferences.setTableId(context, id, tableId)
        val factory = ScheduleWidgetFactory(context, collectionIntent(context, id, WidgetKind.TODAY, 1))
        val callbacks = Executors.newSingleThreadExecutor()
        try {
            withWidgetUpdate {
                val callback = callbacks.submit<Int> {
                    factory.onDataSetChanged()
                    factory.count
                }
                try {
                    assertEquals(1, callback.get(5, TimeUnit.SECONDS))
                    assertNotNull(factory.getViewAt(0))
                } finally {
                    callback.cancel(true)
                }
            }
            WidgetPreferences.clear(context, intArrayOf(id))
            factory.onDataSetChanged()
            assertEquals(0, factory.count)
            assertEquals(0L, WidgetPreferences.tableId(context, id))
        } finally {
            callbacks.shutdownNow()
            factory.onDestroy()
        }
    }
}
