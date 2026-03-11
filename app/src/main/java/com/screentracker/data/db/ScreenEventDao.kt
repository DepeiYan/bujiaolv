package com.screentracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * 屏幕事件数据访问对象
 */
@Dao
interface ScreenEventDao {

    /** 插入一条屏幕事件记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScreenEvent): Long

    /** 批量插入屏幕事件记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ScreenEvent>)

    /** 清空所有记录 */
    @Query("DELETE FROM screen_events")
    suspend fun deleteAll()

    /** 更新屏幕事件记录 (补充screenOffTime和duration) */
    @Update
    suspend fun update(event: ScreenEvent)

    /** 获取最新的一条未关闭的屏幕事件 (screenOffTime为null) */
    @Query("SELECT * FROM screen_events WHERE screenOffTime IS NULL ORDER BY screenOnTime DESC LIMIT 1")
    suspend fun getLatestOpenEvent(): ScreenEvent?

    /** 获取指定日期的所有屏幕事件 (按时间降序) */
    @Query("SELECT * FROM screen_events WHERE dateStr = :dateStr ORDER BY screenOnTime DESC")
    fun getEventsByDate(dateStr: String): LiveData<List<ScreenEvent>>

    /** 获取指定日期的屏幕点亮次数 */
    @Query("SELECT COUNT(*) FROM screen_events WHERE dateStr = :dateStr")
    fun getScreenOnCountByDate(dateStr: String): LiveData<Int>

    /** 获取指定日期的总使用时长(毫秒) */
    @Query("SELECT COALESCE(SUM(duration), 0) FROM screen_events WHERE dateStr = :dateStr AND duration IS NOT NULL")
    fun getTotalDurationByDate(dateStr: String): LiveData<Long>

    /** 获取指定日期范围内每天的点亮次数, 用于趋势图 */
    @Query("""
        SELECT dateStr, COUNT(*) as count 
        FROM screen_events 
        WHERE dateStr BETWEEN :startDate AND :endDate 
        GROUP BY dateStr 
        ORDER BY dateStr ASC
    """)
    fun getDailyCountBetween(startDate: String, endDate: String): LiveData<List<DailyCount>>

    /** 获取指定日期范围内每天的使用时长, 用于趋势图 */
    @Query("""
        SELECT dateStr, COALESCE(SUM(duration), 0) as totalDuration 
        FROM screen_events 
        WHERE dateStr BETWEEN :startDate AND :endDate AND duration IS NOT NULL 
        GROUP BY dateStr 
        ORDER BY dateStr ASC
    """)
    fun getDailyDurationBetween(startDate: String, endDate: String): LiveData<List<DailyDuration>>

    /** 获取指定日期每小时的点亮次数, 用于时段分布图 */
    @Query("""
        SELECT CAST(strftime('%H', screenOnTime / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour, 
               COUNT(*) as count 
        FROM screen_events 
        WHERE dateStr = :dateStr 
        GROUP BY hour 
        ORDER BY hour ASC
    """)
    fun getHourlyDistribution(dateStr: String): LiveData<List<HourlyCount>>

    /** 获取指定日期范围内每天的统计 (用于周视图) */
    @Query("""
        SELECT dateStr, 
               COUNT(*) as count,
               COALESCE(SUM(duration), 0) as totalDuration
        FROM screen_events 
        WHERE dateStr BETWEEN :startDate AND :endDate 
        GROUP BY dateStr 
        ORDER BY dateStr ASC
    """)
    fun getDailyStatsBetween(startDate: String, endDate: String): LiveData<List<DailyStats>>

    /** 删除指定日期之前的所有记录 (用于清理旧数据) */
    @Query("DELETE FROM screen_events WHERE dateStr < :dateStr")
    suspend fun deleteOlderThan(dateStr: String)

    /** 获取所有记录数 */
    @Query("SELECT COUNT(*) FROM screen_events")
    suspend fun getTotalCount(): Int

    /** 获取指定日期范围内的点亮次数 (LiveData版本) */
    @Query("SELECT COUNT(*) FROM screen_events WHERE dateStr BETWEEN :startDate AND :endDate")
    fun getScreenOnCountBetween(startDate: String, endDate: String): LiveData<Int>

    /** 获取指定日期范围内的总使用时长 (LiveData版本) */
    @Query("SELECT COALESCE(SUM(duration), 0) FROM screen_events WHERE dateStr BETWEEN :startDate AND :endDate AND duration IS NOT NULL")
    fun getTotalDurationBetween(startDate: String, endDate: String): LiveData<Long>

    /** 获取指定日期范围内的所有屏幕事件 (按时间降序) */
    @Query("SELECT * FROM screen_events WHERE dateStr BETWEEN :startDate AND :endDate ORDER BY screenOnTime DESC")
    fun getEventsBetween(startDate: String, endDate: String): LiveData<List<ScreenEvent>>

    /** 获取指定日期的屏幕点亮次数 (suspend版本, 用于后台服务) */
    @Query("SELECT COUNT(*) FROM screen_events WHERE dateStr = :dateStr")
    suspend fun getScreenOnCountByDateSync(dateStr: String): Int

    /** 获取指定日期的总使用时长 (suspend版本, 用于后台服务) */
    @Query("SELECT COALESCE(SUM(duration), 0) FROM screen_events WHERE dateStr = :dateStr AND duration IS NOT NULL")
    suspend fun getTotalDurationByDateSync(dateStr: String): Long
}

/** 每日点亮次数统计结果 */
data class DailyCount(
    val dateStr: String,
    val count: Int
)

/** 每日使用时长统计结果 */
data class DailyDuration(
    val dateStr: String,
    val totalDuration: Long
)

/** 每小时点亮次数统计结果 */
data class HourlyCount(
    val hour: Int,
    val count: Int
)

/** 每日统计结果 (用于周视图) */
data class DailyStats(
    val dateStr: String,
    val count: Int,
    val totalDuration: Long
)
