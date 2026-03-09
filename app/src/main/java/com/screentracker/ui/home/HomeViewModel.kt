package com.screentracker.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.screentracker.data.db.HourlyCount
import com.screentracker.data.db.ScreenEvent
import com.screentracker.data.repository.ScreenEventRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenEventRepository(application)

    /** 当前选中的日期 */
    private val _selectedDate = MutableLiveData(repository.getTodayDateStr())
    val selectedDate: LiveData<String> = _selectedDate

    /** 今日点亮次数 */
    val screenOnCount: LiveData<Int> = _selectedDate.switchMap { date ->
        repository.getScreenOnCountByDate(date)
    }

    /** 今日使用总时长(毫秒) */
    val totalDuration: LiveData<Long> = _selectedDate.switchMap { date ->
        repository.getTotalDurationByDate(date)
    }

    /** 今日所有屏幕事件列表 */
    val screenEvents: LiveData<List<ScreenEvent>> = _selectedDate.switchMap { date ->
        repository.getEventsByDate(date)
    }

    /** 每小时分布 */
    val hourlyDistribution: LiveData<List<HourlyCount>> = _selectedDate.switchMap { date ->
        repository.getHourlyDistribution(date)
    }

    /** 格式化时长为可读字符串 */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟${seconds}秒"
            else -> "${seconds}秒"
        }
    }

    /** 格式化时间戳为时间字符串 */
    fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun setSelectedDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun getTodayDateStr(): String = repository.getTodayDateStr()
}
