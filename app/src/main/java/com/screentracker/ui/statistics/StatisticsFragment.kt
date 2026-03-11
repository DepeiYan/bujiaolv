package com.screentracker.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.screentracker.R
import com.screentracker.databinding.FragmentStatisticsBinding
import com.screentracker.ui.home.HomeViewModel

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()
    // 共享 HomeViewModel，用于点击跳转天视图
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeatmap()
        setupAnxietyChart()
        observeData()

        // 固定使用90天数据
        viewModel.setDaysRange(90)
    }

    // ===== 焦虑热力图设置 =====
    private fun setupHeatmap() {
        // 点击格子跳转到对应天
        binding.heatmapAnxiety.onCellClickListener = { dateStr ->
            // 解析日期字符串 yyyy-MM-dd
            val parts = dateStr.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull()
            val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1)  // Calendar 月份从0开始
            val day = parts.getOrNull(2)?.toIntOrNull()
            if (year != null && month != null && day != null) {
                // 切换 HomeViewModel 到天视图并跳转到指定日期
                homeViewModel.jumpToDay(year, month, day)
                // 导航到首页（singleTop 避免重复创建 Fragment）
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.nav_graph, false)
                    .build()
                findNavController().navigate(R.id.navigation_home, null, navOptions)
            }
        }

        // 击碎焦虑按钮
        binding.btnBreakAnxiety.setOnClickListener {
            binding.heatmapAnxiety.startBreakAnimation()
        }
    }

    // ===== 焦虑值折线图设置 =====
    private fun setupAnxietyChart() {
        binding.chartAnxiety.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                axisMinimum = 0.5f
                axisMaximum = 5.5f
                labelCount = 5
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when (value.toInt()) {
                            1 -> "LV.1"
                            2 -> "LV.2"
                            3 -> "LV.3"
                            4 -> "LV.4"
                            5 -> "LV.5"
                            else -> ""
                        }
                    }
                }
            }
            axisRight.isEnabled = false
        }
    }

    // ===== 数据观察 =====
    private fun observeData() {
        // 每日点亮次数 -> 更新热力图和焦虑值折线图
        viewModel.dailyCounts.observe(viewLifecycleOwner) { dailyCounts ->
            // 更新热力图数据（固定90天）
            val heatmapData = dailyCounts?.associate { it.dateStr to it.count } ?: emptyMap()
            binding.heatmapAnxiety.setData(heatmapData)

            // 更新焦虑值折线图
            if (!dailyCounts.isNullOrEmpty()) {
                binding.tvNoAnxietyData.visibility = View.GONE
                binding.chartAnxiety.visibility = View.VISIBLE
                updateAnxietyChart(dailyCounts)
            } else {
                binding.tvNoAnxietyData.visibility = View.VISIBLE
                binding.chartAnxiety.visibility = View.GONE
            }
        }
    }

    // ===== 更新焦虑值折线图 =====
    private fun updateAnxietyChart(dailyCounts: List<com.screentracker.data.db.DailyCount>) {
        val entries = dailyCounts.mapIndexed { index, item ->
            val anxietyLevel = viewModel.countToAnxietyLevel(item.count).toFloat()
            Entry(index.toFloat(), anxietyLevel)
        }

        val labels = dailyCounts.map { it.dateStr.substring(5) } // MM-dd

        // 为每个点设置对应的颜色
        val colors = dailyCounts.map { item ->
            val level = viewModel.countToAnxietyLevel(item.count)
            viewModel.getAnxietyColor(level)
        }

        val dataSet = LineDataSet(entries, "焦虑值").apply {
            // 使用渐变色线条
            color = ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
            lineWidth = 2f
            circleRadius = 5f
            // 为每个圆点设置不同颜色
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(false)

            // 设置圆点颜色列表
            circleColors = colors
        }

        binding.chartAnxiety.apply {
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    // 只显示部分标签避免拥挤
                    return if (index in labels.indices && index % 5 == 0) labels[index] else ""
                }
            }
            data = LineData(dataSet)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
