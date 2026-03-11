package com.screentracker.ui.statistics

import android.app.Application
import androidx.lifecycle.*
import com.screentracker.data.db.DailyCount
import com.screentracker.data.db.DailyDuration
import com.screentracker.data.repository.ScreenEventRepository
import com.screentracker.ui.home.AnxietyLevel

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenEventRepository(application)

    /** 当前选择的天数范围 */
    private val _daysRange = MutableLiveData(30)
    val daysRange: LiveData<Int> = _daysRange

    /** 日期范围 */
    private val dateRange: LiveData<Pair<String, String>> = _daysRange.map { days ->
        repository.getDateRange(days)
    }

    /** 每日点亮次数趋势 */
    val dailyCounts: LiveData<List<DailyCount>> = dateRange.switchMap { (start, end) ->
        repository.getDailyCountBetween(start, end)
    }

    /** 每日使用时长趋势 */
    val dailyDurations: LiveData<List<DailyDuration>> = dateRange.switchMap { (start, end) ->
        repository.getDailyDurationBetween(start, end)
    }

    fun setDaysRange(days: Int) {
        _daysRange.value = days
    }

    /** 格式化时长(毫秒)为小时数 */
    fun durationToHours(millis: Long): Float {
        return millis / 1000f / 3600f
    }

    /** 格式化时长 */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}小时${minutes}分"
            else -> "${minutes}分钟"
        }
    }

    /**
     * 将每日点亮次数转换为焦虑等级值 (1-5)
     */
    fun countToAnxietyLevel(count: Int): Int {
        return AnxietyLevel.fromCount(count).level
    }

    /**
     * 获取焦虑等级的颜色
     */
    fun getAnxietyColor(level: Int): Int {
        return when (level) {
            1 -> android.graphics.Color.parseColor("#81C784")  // 绿色
            2 -> android.graphics.Color.parseColor("#A5D6A7")  // 浅绿
            3 -> android.graphics.Color.parseColor("#FDD835")  // 黄色
            4 -> android.graphics.Color.parseColor("#FF8A65")  // 橙红
            5 -> android.graphics.Color.parseColor("#E57373")  // 红色
            else -> android.graphics.Color.parseColor("#E0E0E0")
        }
    }
}
