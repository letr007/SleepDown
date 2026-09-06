package com.letr.sleepdown.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetUpdateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val id = 900201

    @After
    fun clear() { WidgetPreferences.clear(context, intArrayOf(id)) }

    @Test
    fun navigationAndResetAreAppliedInReceivedOrder() = runBlocking {
        withTimeout(10_000) {
            val reading = CompletableDeferred<Unit>()
            val resume = CompletableDeferred<Unit>()
            val first = launch(start = CoroutineStart.UNDISPATCHED) {
                withWidgetUpdate {
                    val week = WidgetPreferences.week(context, id)
                    reading.complete(Unit)
                    resume.await()
                    WidgetPreferences.setWeek(context, id, week + 1)
                }
            }
            reading.await()
            val second = launch(start = CoroutineStart.UNDISPATCHED) {
                withWidgetUpdate { WidgetPreferences.setWeek(context, id, WidgetPreferences.week(context, id) + 1) }
            }
            val reset = launch(start = CoroutineStart.UNDISPATCHED) {
                withWidgetUpdate { WidgetPreferences.setWeek(context, id, 0) }
            }
            val afterReset = launch(start = CoroutineStart.UNDISPATCHED) {
                withWidgetUpdate { WidgetPreferences.setWeek(context, id, WidgetPreferences.week(context, id) + 1) }
            }
            resume.complete(Unit)
            listOf(first, second, reset, afterReset).forEach { it.join() }
            assertEquals(1, WidgetPreferences.week(context, id))
        }
    }

    @Test
    fun concurrentReadModifyWriteOperationsDoNotLoseChanges() = runBlocking {
        withTimeout(10_000) {
            WidgetPreferences.setWeek(context, id, 1)
            (1..30).map {
                async {
                    withWidgetUpdate {
                        val week = WidgetPreferences.week(context, id)
                        yield()
                        WidgetPreferences.setWeek(context, id, week + 1)
                    }
                }
            }.awaitAll()
            assertEquals(31, WidgetPreferences.week(context, id))
        }
    }

    @Test
    fun cancelledRefreshReleasesTheQueueAndDeletedWidgetsAreNotRecreated() = runBlocking {
        withTimeout(10_000) {
            val entered = CompletableDeferred<Unit>()
            val blocked = launch(start = CoroutineStart.UNDISPATCHED) {
                withWidgetUpdate {
                    entered.complete(Unit)
                    CompletableDeferred<Unit>().await()
                }
            }
            entered.await()
            blocked.cancelAndJoin()
            withWidgetUpdate { WidgetPreferences.setTableId(context, id, 10) }
            assertEquals(10L, WidgetPreferences.tableId(context, id))
            withWidgetUpdate { WidgetPreferences.clear(context, intArrayOf(id)) }
            var changed = false
            assertFalse(refreshWidgetForConfiguration(context, id, WidgetKind.TODAY) {
                changed = true
                WidgetPreferences.setTableId(context, id, 10)
            })
            assertFalse(changed)
            assertEquals(0L, WidgetPreferences.tableId(context, id))
        }
    }
}
