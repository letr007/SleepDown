package com.letr.sleepdown.ui

import com.letr.sleepdown.domain.CourseOccurrence
import com.letr.sleepdown.domain.GridPlacement
import com.letr.sleepdown.domain.ScheduleTimeTable

/** Clips course geometry to the visible periods without changing stored course times. */
internal fun visibleCoursePlacement(
    occurrence: CourseOccurrence,
    visiblePeriods: Int,
    timeTable: ScheduleTimeTable,
): GridPlacement? {
    val count = visiblePeriods.coerceIn(1, 60)
    val top: Double
    val bottom: Double
    if (occurrence.usesCustomTime) {
        val nodes = timeTable.nodes.filter { it.node in 1..count }.sortedBy { it.node }
        if (nodes.isEmpty()) return null
        fun rowPosition(minute: Int): Double {
            for (node in nodes) {
                if (minute <= node.startMinuteOfDay) return (node.node - 1).toDouble()
                if (minute < node.endMinuteOfDay) {
                    return node.node - 1 + (minute - node.startMinuteOfDay).toDouble() /
                        (node.endMinuteOfDay - node.startMinuteOfDay).coerceAtLeast(1)
                }
            }
            return nodes.last().node.toDouble()
        }
        top = rowPosition(occurrence.startMinuteOfDay)
        bottom = rowPosition(occurrence.endMinuteOfDay)
    } else {
        top = (occurrence.startNode - 1).toDouble().coerceIn(0.0, count.toDouble())
        bottom = (occurrence.startNode - 1 + occurrence.nodeCount).toDouble().coerceIn(0.0, count.toDouble())
    }
    if (bottom <= top) return null
    return GridPlacement(topFraction = top / count, heightFraction = (bottom - top) / count)
}
