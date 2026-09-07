package com.letr.sleepdown.data

import com.letr.sleepdown.domain.ScheduleTimeTable
import com.letr.sleepdown.domain.TimeTableNode
import com.letr.sleepdown.domain.Timetable
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayPeriodCountTest {
    private val timetable = Timetable(
        id = "1", name = "Term", firstDayEpochDay = 0,
        timeTable = ScheduleTimeTable(id = "2", nodes = listOf(
            TimeTableNode(1, 480, 530), TimeTableNode(2, 540, 590), TimeTableNode(3, 600, 650),
        )),
    )

    @Test
    fun updatingCoursesPreservesTheChosenDisplayPeriodCount() {
        val rows = DomainMappers.toEntities(timetable,
            existingTable = TableEntity(id = 1, name = "Term", startDate = 0, nodeCount = 2, timeTableId = 2))
        assertEquals(2, rows.table.nodeCount)
        assertEquals(3, rows.timeTableNodes.size)
    }

    @Test
    fun newTimetablesStartWithAllConfiguredPeriodsVisible() {
        val rows = DomainMappers.toEntities(timetable)
        assertEquals(3, rows.table.nodeCount)
    }
}
