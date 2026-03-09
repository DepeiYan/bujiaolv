package com.screentracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.screentracker.R
import com.screentracker.databinding.FragmentHomeBinding
import com.screentracker.ui.adapter.ScreenEventAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var eventAdapter: ScreenEventAdapter

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

        setupRecyclerView()
        setupHourlyChart()
        observeData()

        // 刷新到今天
        binding.btnToday.setOnClickListener {
            viewModel.setSelectedDate(viewModel.getTodayDateStr())
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = ScreenEventAdapter(viewModel)
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun setupHourlyChart() {
        binding.chartHourly.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 12
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}:00"
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }
    }

    private fun observeData() {
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        viewModel.screenOnCount.observe(viewLifecycleOwner) { count ->
            binding.tvScreenOnCount.text = "$count"
            binding.tvScreenOnLabel.text = "次点亮"
        }

        viewModel.totalDuration.observe(viewLifecycleOwner) { duration ->
            binding.tvTotalDuration.text = viewModel.formatDuration(duration)
            binding.tvDurationLabel.text = "总使用时长"
        }

        viewModel.screenEvents.observe(viewLifecycleOwner) { events ->
            eventAdapter.submitList(events)
            binding.tvNoData.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            binding.rvEvents.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE

            // 计算平均每次使用时长
            val completedEvents = events.filter { it.duration != null }
            if (completedEvents.isNotEmpty()) {
                val avgDuration = completedEvents.map { it.duration!! }.average().toLong()
                binding.tvAvgDuration.text = viewModel.formatDuration(avgDuration)
            } else {
                binding.tvAvgDuration.text = "0秒"
            }
            binding.tvAvgDurationLabel.text = "平均每次时长"
        }

        viewModel.hourlyDistribution.observe(viewLifecycleOwner) { hourlyData ->
            updateHourlyChart(hourlyData)
        }
    }

    private fun updateHourlyChart(hourlyData: List<com.screentracker.data.db.HourlyCount>) {
        val entries = (0..23).map { hour ->
            val count = hourlyData.find { it.hour == hour }?.count ?: 0
            BarEntry(hour.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "每小时点亮次数").apply {
            color = ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
            setDrawValues(false)
        }

        binding.chartHourly.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.8f
            }
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次回到首页时刷新为今日数据
        viewModel.setSelectedDate(viewModel.getTodayDateStr())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
