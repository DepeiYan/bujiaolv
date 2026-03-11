package com.screentracker.ui.home

import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.data.Entry
import com.google.android.material.tabs.TabLayout
import com.screentracker.R
import com.screentracker.data.db.DailyStats
import com.screentracker.data.db.HourlyCount
import com.screentracker.databinding.FragmentHomeBinding
import com.screentracker.ui.adapter.ScreenEventAdapter
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by activityViewModels()

    private lateinit var eventAdapter: ScreenEventAdapter
    private var isDetailExpanded = false

    // 用于图表点击时记录当前数据
    private var currentDailyStats: List<DailyStats> = emptyList()

    // Tab 切换监听器（提取为字段，方便在同步时临时移除/添加）
    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            val mode = when (tab?.position) {
                0 -> ViewMode.DAY
                1 -> ViewMode.WEEK
                2 -> ViewMode.MONTH
                else -> ViewMode.DAY
            }
            viewModel.setViewMode(mode)
        }
        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupDateNavigation()
        setupAdapter()
        setupChart()
        setupCollapsibleDetail()
        observeData()
    }

    // ===== Tab 切换: 天 / 周 / 月 =====
    private fun setupTabs() {
        // 清除现有 Tab，避免重复添加
        binding.tabLayout.removeAllTabs()

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("天"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("周"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("月"))

        // 根据当前 ViewModel 的模式设置选中的 Tab（在添加监听器之前）
        val currentMode = viewModel.viewMode.value ?: ViewMode.DAY
        val tabPosition = when (currentMode) {
            ViewMode.DAY -> 0
            ViewMode.WEEK -> 1
            ViewMode.MONTH -> 2
        }
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(tabPosition), false)

        binding.tabLayout.addOnTabSelectedListener(tabSelectedListener)
    }

    // ===== 日期前后导航 =====
    private fun setupDateNavigation() {
        binding.btnPrev.setOnClickListener { viewModel.navigatePrev() }
        binding.btnNext.setOnClickListener { viewModel.navigateNext() }
        binding.btnToday.setOnClickListener {
            viewModel.goToToday()
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        }
    }

    // ===== 适配器初始化 (仅天视图使用) =====
    private fun setupAdapter() {
        eventAdapter = ScreenEventAdapter(viewModel)
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    // ===== 柱状图设置 (支持点击) =====
    private fun setupChart() {
        binding.chartHourly.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isHighlightFullBarEnabled = true

            // 禁用双击放大和捏合缩放
            setDoubleTapToZoomEnabled(false)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 10f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                axisMinimum = 0f
                textSize = 10f
            }
            axisRight.isEnabled = false
            setExtraOffsets(0f, 0f, 0f, 4f)

            // 点击监听
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let { handleChartClick(it.x.toInt()) }
                    // 取消高亮, 允许重复点击同一柱
                    highlightValue(null)
                }
                override fun onNothingSelected() {}
            })
        }
    }

    // ===== 处理柱状图点击 =====
    private fun handleChartClick(index: Int) {
        when (viewModel.viewMode.value) {
            ViewMode.WEEK -> {
                // 点击周视图的某天, 跳转到天视图
                if (index in currentDailyStats.indices) {
                    val dateStr = currentDailyStats[index].dateStr
                    val parts = dateStr.split("-")
                    if (parts.size == 3) {
                        viewModel.jumpToDay(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
                    }
                }
            }
            ViewMode.MONTH -> {
                // 点击月视图的某周, 跳转到周视图
                val weekNumber = index + 1
                val anchor = viewModel.anchorDate.value ?: Calendar.getInstance()
                viewModel.jumpToWeek(anchor.get(Calendar.YEAR), anchor.get(Calendar.MONTH), weekNumber)
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
            }
            else -> { /* 天视图不处理点击 */ }
        }
    }

    // ===== 可折叠的详细记录区域 (仅天视图显示) =====
    private fun setupCollapsibleDetail() {
        isDetailExpanded = false
        binding.layoutDetailContent.visibility = View.GONE
        binding.ivExpandArrow.rotation = 0f

        binding.layoutDetailHeader.setOnClickListener {
            isDetailExpanded = !isDetailExpanded
            if (isDetailExpanded) {
                binding.layoutDetailContent.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(binding.ivExpandArrow, "rotation", 0f, 180f)
                    .setDuration(200).start()
            } else {
                binding.layoutDetailContent.visibility = View.GONE
                ObjectAnimator.ofFloat(binding.ivExpandArrow, "rotation", 180f, 0f)
                    .setDuration(200).start()
            }
        }
    }

    // ===== 数据观察 =====
    private fun observeData() {
        // 日期范围标题
        viewModel.dateRangeLabel.observe(viewLifecycleOwner) { label ->
            binding.tvDateRange.text = label
        }

        // 点亮次数
        viewModel.screenOnCount.observe(viewLifecycleOwner) { count ->
            binding.tvScreenOnCount.text = "$count"
            // 更新焦虑等级
            val mode = viewModel.viewMode.value ?: ViewMode.DAY
            val avgCount = when (mode) {
                ViewMode.DAY -> count
                ViewMode.WEEK, ViewMode.MONTH -> {
                    // 周/月视图使用日均点击次数
                    val days = when (mode) {
                        ViewMode.WEEK -> 7
                        ViewMode.MONTH -> 30
                        else -> 1
                    }
                    if (days > 0) count / days else count
                }
            }
            updateAnxietyLevel(avgCount)
        }

        // 总使用时长
        viewModel.totalDuration.observe(viewLifecycleOwner) { duration ->
            binding.tvTotalDuration.text = viewModel.formatDuration(duration)
        }

        // 视图模式变化: 控制各视图的显示/隐藏 + 同步 Tab
        viewModel.viewMode.observe(viewLifecycleOwner) { mode ->
            updateViewVisibility(mode)
            // 同步 Tab 选中状态（避免外部改变 viewMode 时 Tab 不一致）
            val tabPos = when (mode) {
                ViewMode.DAY -> 0
                ViewMode.WEEK -> 1
                ViewMode.MONTH -> 2
            }
            if (binding.tabLayout.selectedTabPosition != tabPos) {
                binding.tabLayout.removeOnTabSelectedListener(tabSelectedListener)
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(tabPos))
                binding.tabLayout.addOnTabSelectedListener(tabSelectedListener)
            }
        }

        // 天视图: 事件列表 + 平均时长
        viewModel.screenEvents.observe(viewLifecycleOwner) { events ->
            eventAdapter.submitList(events)
            binding.tvDetailCount.text = "${events.size}条"
            binding.tvNoData.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            binding.rvEvents.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE

            val completedEvents = events.filter { it.duration != null }
            if (completedEvents.isNotEmpty()) {
                val avgDuration = completedEvents.map { it.duration!! }.average().toLong()
                binding.tvAvgDuration.text = viewModel.formatDuration(avgDuration)
            } else {
                binding.tvAvgDuration.text = "0秒"
            }
        }

        // 周/月视图: 每日统计数据
        viewModel.dailyStats.observe(viewLifecycleOwner) { stats ->
            currentDailyStats = stats
            updateChartByMode(viewModel.viewMode.value ?: ViewMode.DAY)
        }

        // 天视图: 时段分布
        viewModel.hourlyDistribution.observe(viewLifecycleOwner) { hourlyData ->
            if (viewModel.viewMode.value == ViewMode.DAY) {
                updateDayChart(hourlyData)
            }
        }
    }

    // ===== 根据视图模式更新界面显示 =====
    private fun updateViewVisibility(mode: ViewMode) {
        when (mode) {
            ViewMode.DAY -> {
                binding.cardDailyDetail.visibility = View.VISIBLE
                binding.cardAnxietyLevel.visibility = View.VISIBLE
                binding.tvChartTitle.text = "时段分布"
                binding.tvScreenOnLabel.text = "次点亮"
                binding.tvDurationLabel.text = "总使用时长"
                binding.tvAvgDurationLabel.text = "平均每次"
            }
            ViewMode.WEEK -> {
                binding.cardDailyDetail.visibility = View.GONE
                binding.cardAnxietyLevel.visibility = View.VISIBLE
                binding.tvChartTitle.text = "每日分布 (点击柱状图查看详情)"
                binding.tvScreenOnLabel.text = "本周点亮"
                binding.tvDurationLabel.text = "本周总时长"
                binding.tvAvgDurationLabel.text = "本周平均"
            }
            ViewMode.MONTH -> {
                binding.cardDailyDetail.visibility = View.GONE
                binding.cardAnxietyLevel.visibility = View.VISIBLE
                binding.tvChartTitle.text = "每周分布 (点击柱状图查看详情)"
                binding.tvScreenOnLabel.text = "本月点亮"
                binding.tvDurationLabel.text = "本月总时长"
                binding.tvAvgDurationLabel.text = "本月平均"
            }
        }
    }

    // ===== 更新焦虑等级显示 =====
    private fun updateAnxietyLevel(count: Int) {
        val level = AnxietyLevel.fromCount(count)

        binding.tvAnxietyLevel.text = "LV.${level.level}"
        binding.tvAnxietyNameCn.text = level.nameCn
        binding.tvAnxietyNameEn.text = level.nameEn
        binding.tvAnxietyEmoji.text = level.emoji
        binding.tvAnxietyDesc.text = level.description

        // 更新渐变背景
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                AnxietyLevel.getColorInt(level.gradientStart),
                AnxietyLevel.getColorInt(level.gradientEnd)
            )
        )
        gradientDrawable.cornerRadius = 16f * resources.displayMetrics.density
        binding.layoutAnxietyContent.background = gradientDrawable
    }

    // ===== 根据当前模式更新图表 =====
    private fun updateChartByMode(mode: ViewMode) {
        when (mode) {
            ViewMode.DAY -> { /* 天视图使用 hourlyDistribution 数据 */ }
            ViewMode.WEEK -> updateWeekChart(currentDailyStats)
            ViewMode.MONTH -> updateMonthChart(currentDailyStats)
        }
    }

    // ===== 天视图: 24小时时段分布 =====
    private fun updateDayChart(hourlyData: List<HourlyCount>) {
        val entries = (0..23).map { hour ->
            val count = hourlyData.find { it.hour == hour }?.count ?: 0
            BarEntry(hour.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
            setDrawValues(false)
        }

        binding.chartHourly.apply {
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}时"
                }
            }
            xAxis.labelCount = 12
            data = BarData(dataSet).apply { barWidth = 0.8f }
            invalidate()
        }
    }

    // ===== 周视图: 7天分布 =====
    private fun updateWeekChart(dailyStats: List<DailyStats>) {
        // 获取本周7天的日期
        val dateRange = viewModel.dateRange.value ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // 生成本周7天的日期列表
        val weekDates = mutableListOf<String>()
        val startDate = dateFormat.parse(dateRange.first) ?: return
        calendar.time = startDate
        for (i in 0..6) {
            weekDates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 构建数据 (没有数据的日期显示0)
        val entries = weekDates.mapIndexed { index, dateStr ->
            val count = dailyStats.find { it.dateStr == dateStr }?.count ?: 0
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(requireContext(), R.color.md_theme_secondary)
            setDrawValues(true)
            valueTextSize = 10f
        }

        binding.chartHourly.apply {
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index in weekDates.indices) {
                        val date = dateFormat.parse(weekDates[index]) ?: return ""
                        calendar.time = date
                        // 只显示日期，例如 "9日"
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        return "${day}日"
                    }
                    return ""
                }
            }
            xAxis.labelCount = 7
            data = BarData(dataSet).apply { barWidth = 0.6f }
            invalidate()
        }
    }

    // ===== 月视图: 近7周每周分布 =====
    private fun updateMonthChart(dailyStats: List<DailyStats>) {
        // 近7周，从当前周往前倒推
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        // 构建近7周的范围（当前周为第7周，往前倒推6周）
        val weekGroups = mutableMapOf<Int, Int>() // weekIndex(1-7) -> count

        // 初始化7周为0
        for (i in 1..7) {
            weekGroups[i] = 0
        }

        // 计算当前日期所属的周索引（相对于近7周的范围）
        dailyStats.forEach { stat ->
            val statCal = Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateStr) ?: return@forEach
            }

            // 计算该日期距离今天有多少周
            val daysDiff = ((calendar.timeInMillis - statCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            val weeksDiff = daysDiff / 7

            // 只统计近7周内的数据（weeksDiff: 0=本周, 1=上周, ..., 6=7周前）
            if (weeksDiff in 0..6) {
                // 倒序：本周是第7周，7周前是第1周
                val weekIndex = 7 - weeksDiff
                weekGroups[weekIndex] = (weekGroups[weekIndex] ?: 0) + stat.count
            }
        }

        // 生成7个周的条目（第1周到第7周）
        val sortedWeeks = (1..7).toList()

        val entries = sortedWeeks.mapIndexed { index, weekNum ->
            val count = weekGroups[weekNum] ?: 0
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary)
            setDrawValues(true)
            valueTextSize = 10f
        }

        binding.chartHourly.apply {
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index in sortedWeeks.indices) {
                        return "第${sortedWeeks[index]}周"
                    }
                    return ""
                }
            }
            xAxis.labelCount = 7
            data = BarData(dataSet).apply { barWidth = 0.5f }
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        // LiveData 会在 fragment 恢复 STARTED 状态时重新分发最新值
        // viewMode observer 会自动同步 Tab 状态
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
