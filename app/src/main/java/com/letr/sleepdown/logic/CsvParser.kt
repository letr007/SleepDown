package com.letr.sleepdown.logic

/**
 * WakeUp 课程表兼容的 CSV 解析。
 * 列：课程名称, 星期, 开始节数, 结束节数, 老师, 地点, 周数
 * 周数支持 "1-16"、"1-5、7-11单"、"1-5双"，多个段用中文顿号分隔；
 * Excel 日期化导致的 "2024/9/2" 等值按日期换算周次（需要 startDate）。
 */
object CsvParser {

    data class WeekSegment(
        val start: Int,
        val end: Int,
        val weekType: Int,
    )

    data class Row(
        val name: String,
        val day: Int,
        val startNode: Int,
        val endNode: Int,
        val teacher: String,
        val room: String,
        val weekSegments: List<WeekSegment>,
    )

    fun parse(csvText: String, startDate: Long? = null): Result<List<Row>> {
        val lines = csvText.lineSequence()
            .map { it.trim('\uFEFF', '\r') }
            .filter { it.isNotBlank() }
            .toList()

        val rows = mutableListOf<Row>()
        var dataStarted = false

        for (line in lines) {
            val cells = splitCsv(line)
            if (cells.size < 7) {
                if (dataStarted) continue else continue
            }
            // 跳过表头行（第一列为"课程名称"等）
            if (!dataStarted) {
                if (cells[0].contains("课程") || cells[0].contains("名称")) {
                    dataStarted = true
                    continue
                }
                dataStarted = true
            }
            val name = cells[0].trim()
            val day = cells[1].trim().toIntOrNull()
            val startNode = cells[2].trim().toIntOrNull()
            val endNode = cells[3].trim().toIntOrNull()
            val teacher = normalizeNone(cells[4])
            val room = normalizeNone(cells[5])
            val weekText = cells[6].trim()

            if (name.isEmpty() || day == null || startNode == null || endNode == null) continue
            if (day !in 1..7 || startNode < 1 || endNode < startNode) continue

            val segments = parseWeeks(weekText, startDate) ?: continue
            if (segments.isEmpty()) continue

            rows.add(Row(name, day, startNode, endNode, teacher, room, segments))
        }

        return if (rows.isEmpty()) {
            Result.failure(IllegalArgumentException("未能从 CSV 中解析出有效课程，请检查列顺序：课程名称,星期,开始节数,结束节数,老师,地点,周数"))
        } else {
            Result.success(rows)
        }
    }

    private fun normalizeNone(s: String): String =
        if (s.trim().isEmpty() || s.trim() == "无") "" else s.trim()

    /** 处理引号包裹的简单 CSV 分列 */
    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuote && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuote = !inQuote
                c == ',' && !inQuote -> {
                    out.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    fun parseWeeks(text: String, startDate: Long?): List<WeekSegment>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // Excel 日期化："2024/9/2" 或 "9/2" —— 按日期换算周次，全周无单双
        val date = Weeks.parseDate(trimmed) ?: run {
            val m = Regex("^(\\d{1,2})/(\\d{1,2})$").find(trimmed)
            if (m != null && startDate != null) {
                val year = java.time.LocalDate.ofEpochDay(startDate).let {
                    if (m.groupValues[1].toInt() < 7) it.year + 1 else it.year
                }
                runCatching {
                    java.time.LocalDate.of(year, m.groupValues[1].toInt(), m.groupValues[2].toInt())
                }.getOrNull()
            } else null
        }
        if (date != null && startDate != null) {
            val week = Weeks.weekOf(startDate, date, Int.MAX_VALUE)
            return if (week >= 1) listOf(WeekSegment(week, week, 0)) else emptyList()
        }

        val segments = mutableListOf<WeekSegment>()
        for (part in trimmed.split('、', ',', '，')) {
            val p = part.trim()
            if (p.isEmpty()) continue
            val weekType = when {
                p.endsWith("单") -> 1
                p.endsWith("双") -> 2
                else -> 0
            }
            val body = p.trimEnd('单', '双')
            val range = Regex("^(\\d+)(?:-(\\d+))?$").find(body) ?: continue
            val start = range.groupValues[1].toInt()
            val end = range.groupValues[2].ifEmpty { range.groupValues[1] }.toInt()
            if (start < 1 || end < start) continue
            segments.add(WeekSegment(start, end, weekType))
        }
        return segments
    }
}
