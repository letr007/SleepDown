package com.letr.sleepdown

import android.content.Context
import com.letr.sleepdown.data.TimetableRepository

/** 轻量依赖容器：全 app 共享一个 Repository 实例 */
object AppContainer {
    @Volatile
    private var repository: TimetableRepository? = null

    fun repo(context: Context): TimetableRepository =
        repository ?: synchronized(this) {
            repository ?: TimetableRepository(context.applicationContext).also { repository = it }
        }

    /** 当前选中的课表 id（SharedPreferences 持久化） */
    fun currentTableId(context: Context): Long =
        context.getSharedPreferences("timetable", Context.MODE_PRIVATE)
            .getLong("current_table_id", 0L)

    fun setCurrentTableId(context: Context, id: Long) {
        context.getSharedPreferences("timetable", Context.MODE_PRIVATE)
            .edit().putLong("current_table_id", id).apply()
    }
}
