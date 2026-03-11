package com.screentracker.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
        setupAutostartHint()
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
                // 显示自启动提示
                checkAndShowAutostartHint()
            } else {
                requireContext().stopService(serviceIntent)
                viewModel.setServiceRunning(false)
            }
        }
    }

    private fun setupAutostartHint() {
        binding.btnAutostartSettings.setOnClickListener {
            openAutostartSettings()
        }
        
        binding.btnNotificationSettings.setOnClickListener {
            openNotificationSettings()
        }
    }
    
    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // Android 8.0+ 跳转到通知渠道设置
                    action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, ScreenMonitorService.CHANNEL_ID)
                }
                else -> {
                    // 低版本跳转到应用通知设置
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${requireContext().packageName}")
                }
            }
        }
        startActivity(intent)
    }

    private fun checkAndShowAutostartHint() {
        // 检查是否需要显示自启动提示
        if (!isAutostartEnabled(requireContext())) {
            binding.layoutAutostartHint.visibility = View.VISIBLE
        } else {
            binding.layoutAutostartHint.visibility = View.GONE
        }
    }

    private fun isAutostartEnabled(context: Context): Boolean {
        // 由于 Android 没有标准 API 检查自启动权限，
        // 我们使用一种启发式方法：检查是否曾经引导过用户
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("autostart_checked", false)
    }

    private fun openAutostartSettings() {
        val context = requireContext()
        val intent = Intent()

        try {
            // 尝试不同厂商的自启动设置页面
            when {
                // 小米
                Build.BRAND.equals("xiaomi", ignoreCase = true) -> {
                    intent.component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
                // 华为
                Build.BRAND.equals("huawei", ignoreCase = true) -> {
                    intent.component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
                // OPPO
                Build.BRAND.equals("oppo", ignoreCase = true) -> {
                    intent.component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
                // VIVO
                Build.BRAND.equals("vivo", ignoreCase = true) -> {
                    intent.component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
                // 三星
                Build.BRAND.equals("samsung", ignoreCase = true) -> {
                    intent.component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
                // 其他品牌跳转到应用详情页
                else -> {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.parse("package:${context.packageName}")
                }
            }

            startActivity(intent)

            // 标记已引导用户
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("autostart_checked", true)
                .apply()

        } catch (e: Exception) {
            // 如果特定页面打不开，跳转到应用详情页
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            startActivity(fallbackIntent)
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
            // 服务开启时检查自启动提示
            if (running) {
                checkAndShowAutostartHint()
            } else {
                binding.layoutAutostartHint.visibility = View.GONE
            }
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
