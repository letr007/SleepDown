package com.letr.sleepdown.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 本应用自有的 JSON 备份格式（单张课表） */
@Serializable
data class BackupData(
    val version: Int = 1,
    val table: TableEntity,
    val nodeTimes: List<NodeTimeEntity>,
    val courses: List<CourseEntity>,
    val times: List<CourseTimeEntity>,
)

object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(backup: BackupData): String = json.encodeToString(backup)

    fun decode(text: String): BackupData = json.decodeFromString<BackupData>(text)
}
