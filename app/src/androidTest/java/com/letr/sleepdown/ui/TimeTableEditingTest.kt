package com.letr.sleepdown.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letr.sleepdown.domain.TimeTableNode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeTableEditingTest {
    private val nodes = listOf(
        TimeTableNode(1, 480, 530), TimeTableNode(2, 550, 595),
        TimeTableNode(3, 615, 665), TimeTableNode(4, 810, 860),
    )

    @Test
    fun breaksApplyWithinTheSelectedRangeAndPreserveDurations() {
        val result = applyBreakMinutes(nodes, 1, 3, 10)
        assertEquals(listOf(480, 540, 595, 810), result.map { it.startMinuteOfDay })
        assertEquals(listOf(530, 585, 645, 860), result.map { it.endMinuteOfDay })
        assertEquals(nodes.first(), result.first())
        assertEquals(nodes.last(), result.last())
        assertEquals(550, nodes[1].startMinuteOfDay)
        assertEquals(530, applyBreakMinutes(nodes, 1, 2, 0)[1].startMinuteOfDay)
    }

    @Test
    fun invalidRangesAndTimesFailWithoutChangingTheDraft() {
        for ((first, last, gap) in listOf(Triple(1, 1, 10), Triple(3, 2, 10), Triple(1, 9, 10), Triple(1, 3, -1))) {
            assertTrue(runCatching { applyBreakMinutes(nodes, first, last, gap) }.isFailure)
        }
        val late = listOf(TimeTableNode(1, 1350, 1400), TimeTableNode(2, 1400, 1440))
        assertTrue(runCatching { applyBreakMinutes(late, 1, 2, 10) }.isFailure)
        assertEquals(1400, late[1].startMinuteOfDay)
    }
}
