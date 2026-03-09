package com.screentracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 屏幕事件实体类
 * 记录每次屏幕点亮和熄灭的事件
 */
@Entity(tableName = "screen_events")
data class ScreenEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 屏幕点亮的时间戳 (毫秒) */
    val screenOnTime: Long,

    /** 屏幕熄灭的时间戳 (毫秒), 如果屏幕还亮着则为null */
    val screenOffTime: Long? = null,

    /** 本次屏幕点亮的持续时长 (毫秒), 如果屏幕还亮着则为null */
    val duration: Long? = null,

    /** 日期字符串, 格式: yyyy-MM-dd, 用于按日期查询 */
    val dateStr: String
)
