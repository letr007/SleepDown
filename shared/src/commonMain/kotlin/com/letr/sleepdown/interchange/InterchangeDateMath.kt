package com.letr.sleepdown.interchange

import com.letr.sleepdown.domain.floorDiv

internal data class CivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

internal fun epochDayToCivilDate(epochDay: Long): CivilDate {
    var z = epochDay + 719468L
    val era = if (z >= 0L) z / 146097L else (z - 146096L) / 146097L
    val dayOfEra = z - era * 146097L
    val yearOfEra = (dayOfEra - dayOfEra / 1460L + dayOfEra / 36524L - dayOfEra / 146096L) / 365L
    val year = yearOfEra + era * 400L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPart = (5L * dayOfYear + 2L) / 153L
    val day = dayOfYear - (153L * monthPart + 2L) / 5L + 1L
    val month = monthPart + if (monthPart < 10L) 3L else -9L
    val adjustedYear = year + if (month <= 2L) 1L else 0L
    return CivilDate(adjustedYear.toInt(), month.toInt(), day.toInt())
}

internal fun civilDateToEpochDay(date: CivilDate): Long? {
    if (date.month !in 1..12 || date.day !in 1..31) return null
    val adjustedYear = date.year - if (date.month <= 2) 1 else 0
    val era = floorDiv(adjustedYear.toLong(), 400L)
    val yearOfEra = adjustedYear.toLong() - era * 400L
    val monthPart = date.month + if (date.month > 2) -3 else 9
    val dayOfYear = (153L * monthPart + 2L) / 5L + date.day - 1L
    val dayOfEra = yearOfEra * 365L + yearOfEra / 4L - yearOfEra / 100L + dayOfYear
    return era * 146097L + dayOfEra - 719468L
}

internal fun parseIsoDate(value: String): Long? {
    val parts = value.trim().split("-")
    if (parts.size != 3 || parts.any { it.isEmpty() }) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return civilDateToEpochDay(CivilDate(year, month, day))?.takeIf {
        epochDayToCivilDate(it) == CivilDate(year, month, day)
    }
}

internal fun formatIsoDate(epochDay: Long): String {
    val date = epochDayToCivilDate(epochDay)
    require(date.year in 1..9999) { "epochDay is outside the four-digit calendar year range" }
    return "${date.year.toString().padStart(4, '0')}-${date.month.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
}

internal fun parseBasicDate(value: String): Long? {
    if (value.length != 8 || value.any { !it.isDigit() }) return null
    val date = CivilDate(
        year = value.substring(0, 4).toIntOrNull() ?: return null,
        month = value.substring(4, 6).toIntOrNull() ?: return null,
        day = value.substring(6, 8).toIntOrNull() ?: return null,
    )
    return civilDateToEpochDay(date)?.takeIf { epochDayToCivilDate(it) == date }
}

internal fun formatBasicDate(epochDay: Long): String {
    val date = epochDayToCivilDate(epochDay)
    require(date.year in 1..9999) { "epochDay is outside the four-digit calendar year range" }
    return buildString {
        append(date.year.toString().padStart(4, '0'))
        append(date.month.toString().padStart(2, '0'))
        append(date.day.toString().padStart(2, '0'))
    }
}
