package com.screentracker.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.screentracker.databinding.FragmentSettingsBinding
import com.screentracker.service.ScreenMonitorService

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupServiceToggle()
        setupReminderToggle()
        setupGoalSettings()
        observeData()
    }

    private fun setupServiceToggle() {
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(requireContext(), ScreenMonitorService::class.java)
            if (isChecked) {
                requireContext().startForegroundService(serviceIntent)
                viewModel.setServiceRunning(true)
            } else {
                requireContext().stopService(serviceIntent)
                viewModel.setServiceRunning(false)
            }
        }
    }

    private fun setupReminderToggle() {
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setReminderEnabled(isChecked)
            binding.layoutGoalSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupGoalSettings() {
        // 点亮次数目标滑块: 0-200次, 步进10
        binding.seekbarScreenOnGoal.apply {
            max = 20 // 实际值 = progress * 10
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val goal = progress * 10
                    binding.tvScreenOnGoalValue.text = if (goal == 0) "未设置" else "${goal}次"
                    if (fromUser) viewModel.setDailyScreenOnGoal(goal)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        // 使用时长目标滑块: 0-480分钟 (8小时), 步进30
        binding.seekbarDurationGoal.apply {
            max = 16 // 实际值 = progress * 30 分钟
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val minutes = progress * 30
                    binding.tvDurationGoalValue.text = when {
                        minutes == 0 -> "未设置"
                        minutes >= 60 -> "${minutes / 60}小时${if (minutes % 60 > 0) "${minutes % 60}分钟" else ""}"
                        else -> "${minutes}分钟"
                    }
                    if (fromUser) viewModel.setDailyDurationGoal(minutes)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun observeData() {
        viewModel.serviceRunning.observe(viewLifecycleOwner) { running ->
            binding.switchService.isChecked = running
            binding.tvServiceStatus.text = if (running) "监控服务运行中" else "监控服务已停止"
        }

        viewModel.reminderEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchReminder.isChecked = enabled
            binding.layoutGoalSettings.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        viewModel.dailyScreenOnGoal.observe(viewLifecycleOwner) { goal ->
            binding.seekbarScreenOnGoal.progress = goal / 10
            binding.tvScreenOnGoalValue.text = if (goal == 0) "未设置" else "${goal}次"
        }

        viewModel.dailyDurationGoal.observe(viewLifecycleOwner) { minutes ->
            binding.seekbarDurationGoal.progress = minutes / 30
            binding.tvDurationGoalValue.text = when {
                minutes == 0 -> "未设置"
                minutes >= 60 -> "${minutes / 60}小时${if (minutes % 60 > 0) "${minutes % 60}分钟" else ""}"
                else -> "${minutes}分钟"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
