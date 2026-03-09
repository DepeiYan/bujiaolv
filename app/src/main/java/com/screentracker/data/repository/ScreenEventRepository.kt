package com.screentracker.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.screentracker.data.db.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 屏幕事件仓库类
 * 封装数据库操作, 提供上层调用接口
 */
class ScreenEventRepository(context: Context) {

    private val dao: ScreenEventDao = AppDatabase.getInstance(context).screenEventDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 记录屏幕点亮事件 */
    suspend fun recordScreenOn(): Long {
        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))

        // 先关闭所有未关闭的事件（处理应用被kill后重启的情况）
        closeAllOpenEvents(now)

        val event = ScreenEvent(
            screenOnTime = now,
            dateStr = todayStr
        )
        return dao.insert(event)
    }

    /** 关闭所有未关闭的屏幕事件 */
    private suspend fun closeAllOpenEvents(closeTime: Long) {
        val openEvent = dao.getLatestOpenEvent() ?: return
        // 如果存在未关闭的事件，将其关闭
        val updatedEvent = openEvent.copy(
            screenOffTime = closeTime,
            duration = closeTime - openEvent.screenOnTime
        )
        dao.update(updatedEvent)
    }

    /** 记录屏幕熄灭事件 (更新最近的未关闭事件) */
    suspend fun recordScreenOff() {
        val openEvent = dao.getLatestOpenEvent() ?: return
        val now = System.currentTimeMillis()

        // 如果屏幕点亮时间大于当前时间（异常情况），忽略
        if (openEvent.screenOnTime > now) return

        // 如果持续时间超过24小时，可能是异常数据，限制为24小时
        val maxDuration = 24 * 60 * 60 * 1000L // 24小时
        val duration = now - openEvent.screenOnTime
        val finalDuration = if (duration > maxDuration) maxDuration else duration

        val updatedEvent = openEvent.copy(
            screenOffTime = now,
            duration = finalDuration
        )
        dao.update(updatedEvent)
    }

    /** 获取最新的未关闭事件 */
    suspend fun getLatestOpenEvent(): ScreenEvent? {
        return dao.getLatestOpenEvent()
    }

    /** 获取今天的日期字符串 */
    fun getTodayDateStr(): String = dateFormat.format(Date())

    /** 获取指定日期的所有屏幕事件 */
    fun getEventsByDate(dateStr: String): LiveData<List<ScreenEvent>> =
        dao.getEventsByDate(dateStr)

    /** 获取指定日期的屏幕点亮次数 */
    fun getScreenOnCountByDate(dateStr: String): LiveData<Int> =
        dao.getScreenOnCountByDate(dateStr)

    /** 获取指定日期的总使用时长 */
    fun getTotalDurationByDate(dateStr: String): LiveData<Long> =
        dao.getTotalDurationByDate(dateStr)

    /** 获取日期范围内每天的点亮次数 */
    fun getDailyCountBetween(startDate: String, endDate: String): LiveData<List<DailyCount>> =
        dao.getDailyCountBetween(startDate, endDate)

    /** 获取日期范围内每天的使用时长 */
    fun getDailyDurationBetween(startDate: String, endDate: String): LiveData<List<DailyDuration>> =
        dao.getDailyDurationBetween(startDate, endDate)

    /** 获取指定日期每小时的点亮次数分布 */
    fun getHourlyDistribution(dateStr: String): LiveData<List<HourlyCount>> =
        dao.getHourlyDistribution(dateStr)

    /** 清理超过指定天数的旧数据 */
    suspend fun cleanOldData(daysToKeep: Int = 90) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        val cutoffDate = dateFormat.format(calendar.time)
        dao.deleteOlderThan(cutoffDate)
    }

    /** 获取过去N天的日期范围 */
    fun getDateRange(days: Int): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val startDate = dateFormat.format(calendar.time)
        return Pair(startDate, endDate)
    }
}
