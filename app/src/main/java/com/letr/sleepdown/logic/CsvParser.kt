package com.letr.sleepdown.logic

/** Parser for WakeUp's fixed-column academic CSV import templates. */
object CsvParser {
    data class WeekSegment(val start: Int, val end: Int, val weekType: Int)
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
        val records = parseRecords(csvText)
        if (records.isEmpty()) return failure()
        val delimiter = detectDelimiter(records.first())
        val rows = records.map { splitRow(it, delimiter) }
        val data = if (rows.firstOrNull()?.firstOrNull()?.contains("课程") == true ||
            rows.firstOrNull()?.firstOrNull()?.contains("名称") == true) rows.drop(1) else rows
        val result = data.mapNotNull { cells ->
            if (cells.size < 7) return@mapNotNull null
            val name = cells[0].trim()
            val day = cells[1].trim().toIntOrNull()
            val start = cells[2].trim().toIntOrNull()
            val end = cells[3].trim().toIntOrNull()
            if (name.isEmpty() || day == null || day !in 1..7 || start == null || end == null || end < start) return@mapNotNull null
            val weeks = parseWeeks(cells[6], startDate) ?: return@mapNotNull null
            if (weeks.isEmpty()) return@mapNotNull null
            Row(name, day, start, end, normalizeNone(cells[4]), normalizeNone(cells[5]), weeks)
        }
        return if (result.isEmpty()) failure() else Result.success(result)
    }

    private fun failure() = Result.failure<List<Row>>(
        IllegalArgumentException("未能从 CSV 中解析出有效课程，请检查列顺序：课程名称,星期,开始节数,结束节数,老师,地点,周数"),
    )

    private fun normalizeNone(value: String): String =
        value.trim().takeUnless { it.isEmpty() || it == "无" }.orEmpty()

    private fun parseRecords(text: String): List<String> {
        val records = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val c = text[index]
            when {
                c == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    current.append(c).append(c); index++
                }
                c == '"' -> { quoted = !quoted; current.append(c) }
                (c == '\n' || c == '\r') && !quoted -> {
                    if (current.isNotEmpty()) records += current.toString().trim('\uFEFF')
                    current.clear()
                    if (c == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                }
                else -> current.append(c)
            }
            index++
        }
        if (current.isNotEmpty()) records += current.toString().trim('\uFEFF')
        return records.filter { it.isNotBlank() }
    }

    private fun detectDelimiter(record: String): Char = listOf(',', ';', '\t')
        .maxByOrNull { delimiter -> splitRow(record, delimiter).size } ?: ','

    private fun splitRow(record: String, delimiter: Char): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < record.length) {
            val c = record[index]
            when {
                c == '"' && quoted && index + 1 < record.length && record[index + 1] == '"' -> {
                    current.append('"'); index++
                }
                c == '"' -> quoted = !quoted
                c == delimiter && !quoted -> { values += current.toString(); current.clear() }
                else -> current.append(c)
            }
            index++
        }
        values += current.toString()
        return values
    }

    fun parseWeeks(text: String, startDate: Long?): List<WeekSegment>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val date = Weeks.parseDate(trimmed) ?: Regex("^(\\d{1,2})/(\\d{1,2})$").find(trimmed)?.let { match ->
            if (startDate == null) null else runCatching {
                val base = java.time.LocalDate.ofEpochDay(startDate)
                java.time.LocalDate.of(if (match.groupValues[1].toInt() < 7) base.year + 1 else base.year,
                    match.groupValues[1].toInt(), match.groupValues[2].toInt())
            }.getOrNull()
        }
        if (date != null && startDate != null) {
            val week = Weeks.weekOf(startDate, date, Int.MAX_VALUE)
            return if (week >= 1) listOf(WeekSegment(week, week, 0)) else emptyList()
        }
        return trimmed.split('、', ',', '，', ';').mapNotNull { part ->
            val value = part.trim()
            if (value.isEmpty()) return@mapNotNull null
            val type = when { value.endsWith("单") -> 1; value.endsWith("双") -> 2; else -> 0 }
            val body = value.trimEnd('单', '双')
            val match = Regex("^(\\d+)(?:-(\\d+))?$").find(body) ?: return@mapNotNull null
            val first = match.groupValues[1].toInt()
            val last = match.groupValues[2].ifEmpty { match.groupValues[1] }.toInt()
            if (first < 1 || last < first) null else WeekSegment(first, last, type)
        }
    }
}
