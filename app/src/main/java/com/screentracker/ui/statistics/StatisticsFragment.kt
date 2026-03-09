package com.screentracker.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.screentracker.R
import com.screentracker.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

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

        setupCharts()
        setupRangeSelector()
        observeData()
    }

    private fun setupCharts() {
        // 点亮次数折线图
        binding.chartCount.apply {
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
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
        }

        // 使用时长折线图
        binding.chartDuration.apply {
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
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1f时", value)
                    }
                }
            }
            axisRight.isEnabled = false
        }
    }

    private fun setupRangeSelector() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val days = when {
                checkedIds.contains(R.id.chip7days) -> 7
                checkedIds.contains(R.id.chip14days) -> 14
                checkedIds.contains(R.id.chip30days) -> 30
                else -> 7
            }
            viewModel.setDaysRange(days)
        }
    }

    private fun observeData() {
        viewModel.dailyCounts.observe(viewLifecycleOwner) { dailyCounts ->
            if (dailyCounts.isEmpty()) {
                binding.tvNoCountData.visibility = View.VISIBLE
                binding.chartCount.visibility = View.GONE
                return@observe
            }
            binding.tvNoCountData.visibility = View.GONE
            binding.chartCount.visibility = View.VISIBLE

            val entries = dailyCounts.mapIndexed { index, item ->
                Entry(index.toFloat(), item.count.toFloat())
            }

            val labels = dailyCounts.map { it.dateStr.substring(5) } // 只显示 MM-dd

            val dataSet = LineDataSet(entries, "点亮次数").apply {
                color = ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(true)
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(requireContext(), R.color.md_theme_primary_container)
            }

            binding.chartCount.apply {
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index in labels.indices) labels[index] else ""
                    }
                }
                data = LineData(dataSet)
                invalidate()
            }

            // 更新统计摘要
            val totalCount = dailyCounts.sumOf { it.count }
            val avgCount = totalCount / dailyCounts.size
            val maxCount = dailyCounts.maxOf { it.count }
            binding.tvCountSummary.text =
                "总计 ${totalCount} 次 · 日均 ${avgCount} 次 · 最高 ${maxCount} 次"
        }

        viewModel.dailyDurations.observe(viewLifecycleOwner) { dailyDurations ->
            if (dailyDurations.isEmpty()) {
                binding.tvNoDurationData.visibility = View.VISIBLE
                binding.chartDuration.visibility = View.GONE
                return@observe
            }
            binding.tvNoDurationData.visibility = View.GONE
            binding.chartDuration.visibility = View.VISIBLE

            val entries = dailyDurations.mapIndexed { index, item ->
                Entry(index.toFloat(), viewModel.durationToHours(item.totalDuration))
            }

            val labels = dailyDurations.map { it.dateStr.substring(5) }

            val dataSet = LineDataSet(entries, "使用时长").apply {
                color = ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(true)
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1f", value)
                    }
                }
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary_container)
            }

            binding.chartDuration.apply {
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index in labels.indices) labels[index] else ""
                    }
                }
                data = LineData(dataSet)
                invalidate()
            }

            // 更新时长摘要
            val totalDuration = dailyDurations.sumOf { it.totalDuration }
            val avgDuration = totalDuration / dailyDurations.size
            binding.tvDurationSummary.text =
                "总计 ${viewModel.formatDuration(totalDuration)} · 日均 ${viewModel.formatDuration(avgDuration)}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
