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

    /** 获取日期范围内的点亮次数 */
    fun getScreenOnCountBetween(startDate: String, endDate: String): LiveData<Int> =
        dao.getScreenOnCountBetween(startDate, endDate)

    /** 获取日期范围内的总使用时长 */
    fun getTotalDurationBetween(startDate: String, endDate: String): LiveData<Long> =
        dao.getTotalDurationBetween(startDate, endDate)

    /** 获取日期范围内的所有事件 */
    fun getEventsBetween(startDate: String, endDate: String): LiveData<List<ScreenEvent>> =
        dao.getEventsBetween(startDate, endDate)

    /** 获取日期范围内每天的统计 (用于周视图) */
    fun getDailyStatsBetween(startDate: String, endDate: String): LiveData<List<DailyStats>> =
        dao.getDailyStatsBetween(startDate, endDate)

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

    /** 生成并插入90天模拟数据 */
    suspend fun generateMockData() {
        // 清空现有数据
        dao.deleteAll()

        val events = mutableListOf<ScreenEvent>()
        val calendar = Calendar.getInstance()

        // 生成90天的数据
        for (i in 89 downTo 0) {
            val date = calendar.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(date.time)
            val dayStart = date.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // 随机生成该天的点亮次数
            val count = when ((0..99).random()) {
                in 0..29 -> 0
                in 30..69 -> (1..40).random()
                in 70..89 -> (41..70).random()
                in 90..97 -> (71..100).random()
                else -> (101..150).random()
            }

            // 为该天生成分布在一天中的事件
            if (count > 0) {
                val baseTime = dayStart + (8 * 60 * 60 * 1000) // 从早上8点开始
                for (j in 0 until count) {
                    // 随机分布在8:00-23:00之间
                    val offset = ((0..900).random() * 60 * 1000L) // 0-15小时的随机偏移
                    val screenOnTime = baseTime + offset
                    val duration = ((1..30).random() * 60 * 1000L) // 1-30分钟使用时长

                    events.add(ScreenEvent(
                        screenOnTime = screenOnTime,
                        screenOffTime = screenOnTime + duration,
                        duration = duration,
                        dateStr = dateStr
                    ))
                }
            }
        }

        // 批量插入
        dao.insertAll(events)
    }

    /** 检查是否有数据 */
    suspend fun hasData(): Boolean {
        return dao.getTotalCount() > 0
    }
}
