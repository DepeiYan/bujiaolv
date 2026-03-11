package com.screentracker.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.screentracker.data.db.DailyStats
import com.screentracker.data.db.HourlyCount
import com.screentracker.data.db.ScreenEvent
import com.screentracker.data.repository.ScreenEventRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * 视图模式: 天 / 周 / 月
 */
enum class ViewMode { DAY, WEEK, MONTH }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenEventRepository(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 当前视图模式 */
    private val _viewMode = MutableLiveData(ViewMode.DAY)
    val viewMode: LiveData<ViewMode> = _viewMode

    /** 当前锚定日期 (天=当天, 周=周一, 月=1号) */
    private val _anchorDate = MutableLiveData(Calendar.getInstance())
    val anchorDate: LiveData<Calendar> = _anchorDate

    /** 由 viewMode + anchorDate 计算出的日期范围 */
    val dateRange: LiveData<Pair<String, String>> = MediatorLiveData<Pair<String, String>>().apply {
        fun update() {
            val cal = _anchorDate.value ?: Calendar.getInstance()
            val mode = _viewMode.value ?: ViewMode.DAY
            value = computeDateRange(cal, mode)
        }
        addSource(_viewMode) { update() }
        addSource(_anchorDate) { update() }
    }

    /** 日期范围内的点亮次数 */
    val screenOnCount: LiveData<Int> = dateRange.switchMap { (start, end) ->
        repository.getScreenOnCountBetween(start, end)
    }

    /** 日期范围内的总使用时长 */
    val totalDuration: LiveData<Long> = dateRange.switchMap { (start, end) ->
        repository.getTotalDurationBetween(start, end)
    }

    /** 日期范围内的所有事件 (天模式用) */
    val screenEvents: LiveData<List<ScreenEvent>> = dateRange.switchMap { (start, end) ->
        repository.getEventsBetween(start, end)
    }

    /** 周视图: 日期范围内每天的统计 */
    val dailyStats: LiveData<List<DailyStats>> = dateRange.switchMap { (start, end) ->
        repository.getDailyStatsBetween(start, end)
    }

    /** 当天的小时分布 (天模式) / 范围内的每日分布 */
    val hourlyDistribution: LiveData<List<HourlyCount>> = dateRange.switchMap { (start, _) ->
        // 天模式用小时分布, 周/月模式也用第一天的
        repository.getHourlyDistribution(start)
    }

    /** 显示用的日期范围文本 */
    val dateRangeLabel: LiveData<String> = MediatorLiveData<String>().apply {
        fun update() {
            val cal = _anchorDate.value ?: Calendar.getInstance()
            val mode = _viewMode.value ?: ViewMode.DAY
            value = formatDateLabel(cal, mode)
        }
        addSource(_viewMode) { update() }
        addSource(_anchorDate) { update() }
    }

    // ============ 公开方法 ============

    fun setViewMode(mode: ViewMode) {
        val currentMode = _viewMode.value
        // 只有真正切换模式时才更新
        if (currentMode != mode) {
            _viewMode.value = mode
        }
    }

    fun navigatePrev() {
        val cal = (_anchorDate.value ?: Calendar.getInstance()).clone() as Calendar
        when (_viewMode.value) {
            ViewMode.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            ViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            ViewMode.MONTH -> cal.add(Calendar.MONTH, -1)
            else -> {}
        }
        _anchorDate.value = cal
    }

    fun navigateNext() {
        val cal = (_anchorDate.value ?: Calendar.getInstance()).clone() as Calendar
        when (_viewMode.value) {
            ViewMode.DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            ViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            ViewMode.MONTH -> cal.add(Calendar.MONTH, 1)
            else -> {}
        }
        _anchorDate.value = cal
    }

    fun goToToday() {
        _anchorDate.value = Calendar.getInstance()
        _viewMode.value = ViewMode.DAY
    }

    fun selectDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        _anchorDate.value = cal
    }

    /** 跳转到指定日期并切换到日视图 */
    fun jumpToDay(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month, day)
        _anchorDate.value = cal
        _viewMode.value = ViewMode.DAY
    }

    /** 跳转到指定周并切换到周视图 */
    fun jumpToWeek(year: Int, month: Int, weekOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        cal.set(Calendar.WEEK_OF_MONTH, weekOfMonth)
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        _anchorDate.value = cal
        _viewMode.value = ViewMode.WEEK
    }

    // ============ 格式化工具 ============

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}时${minutes}分"
            minutes > 0 -> "${minutes}分${seconds}秒"
            else -> "${seconds}秒"
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // ============ 内部方法 ============

    private fun realignAnchor() {
        val cal = (_anchorDate.value ?: Calendar.getInstance()).clone() as Calendar
        when (_viewMode.value) {
            ViewMode.WEEK -> {
                // 周视图: 从当前日往前倒推6天，共显示7天
                // 不需要对齐到周一，保持当前日期作为结束日期
            }
            ViewMode.MONTH -> {
                // 月视图(近7周): 不需要对齐到月初，保持当前日期作为结束日期
            }
            else -> {} // DAY 不需要对齐
        }
        _anchorDate.value = cal
    }

    private fun computeDateRange(cal: Calendar, mode: ViewMode): Pair<String, String> {
        val start = cal.clone() as Calendar
        val end = cal.clone() as Calendar
        when (mode) {
            ViewMode.DAY -> {
                // 单天
            }
            ViewMode.WEEK -> {
                // 周视图: 从当前日往前倒推6天，共显示7天
                // 结束日期为当前日期，开始日期为6天前
                start.add(Calendar.DAY_OF_YEAR, -6)
            }
            ViewMode.MONTH -> {
                // 月视图(近7周): 从当前日期往前倒推49天(7周)
                start.add(Calendar.DAY_OF_YEAR, -49)
            }
        }
        return Pair(dateFormat.format(start.time), dateFormat.format(end.time))
    }

    private fun formatDateLabel(cal: Calendar, mode: ViewMode): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return when (mode) {
            ViewMode.DAY -> "${y}年${m}月${d}日"
            ViewMode.WEEK -> {
                // 周视图: 显示最近7天的范围 (当前日往前6天)
                val endCal = cal.clone() as Calendar
                val startCal = cal.clone() as Calendar
                startCal.add(Calendar.DAY_OF_YEAR, -6)
                val sm = startCal.get(Calendar.MONTH) + 1
                val sd = startCal.get(Calendar.DAY_OF_MONTH)
                val em = endCal.get(Calendar.MONTH) + 1
                val ed = endCal.get(Calendar.DAY_OF_MONTH)
                "${sm}月${sd}日 - ${em}月${ed}日"
            }
            ViewMode.MONTH -> {
                // 月视图(近7周): 显示近7周的范围
                val endCal = cal.clone() as Calendar
                val startCal = cal.clone() as Calendar
                startCal.add(Calendar.DAY_OF_YEAR, -49) // 往前7周
                val sm = startCal.get(Calendar.MONTH) + 1
                val sd = startCal.get(Calendar.DAY_OF_MONTH)
                val em = endCal.get(Calendar.MONTH) + 1
                val ed = endCal.get(Calendar.DAY_OF_MONTH)
                "${sm}月${sd}日 - ${em}月${ed}日"
            }
        }
    }
}
