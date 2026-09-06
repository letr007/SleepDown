package com.letr.sleepdown.ui

import com.letr.sleepdown.domain.TimeTableNode

internal fun applyBreakMinutes(nodes: List<TimeTableNode>, firstNode: Int, lastNode: Int, breakMinutes: Int): List<TimeTableNode> {
    require(breakMinutes in 0..240)
    require(firstNode < lastNode)
    val first = nodes.indexOfFirst { it.node == firstNode }
    val last = nodes.indexOfFirst { it.node == lastNode }
    require(first >= 0 && last > first)
    val updated = nodes.toMutableList()
    for (index in first + 1..last) {
        val duration = nodes[index].endMinuteOfDay - nodes[index].startMinuteOfDay
        require(duration > 0)
        val start = updated[index - 1].endMinuteOfDay + breakMinutes
        require(start >= 0 && start + duration <= 1440)
        updated[index] = nodes[index].copy(startMinuteOfDay = start, endMinuteOfDay = start + duration)
    }
    return updated
}
