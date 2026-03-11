package com.screentracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.screentracker.databinding.ActivityMainBinding
import com.screentracker.service.ScreenMonitorService
import com.screentracker.ui.home.HomeViewModel
import com.screentracker.util.PreferenceManager
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.navigation.ui.NavigationUI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    private val homeViewModel: HomeViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户同意或拒绝通知权限 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupNavigation()
        requestNotificationPermission()
        autoStartService()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 1. setupWithNavController 设置 destination change listener（同步底部导航选中状态）
        binding.bottomNavigation.setupWithNavController(navController)

        // 2. 覆盖 item selected listener：点击"今日"时额外调用 goToToday()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.navigation_home) {
                homeViewModel.goToToday()
            }
            // 委托 NavigationUI 处理实际导航（含 singleTop 和 popUpTo）
            NavigationUI.onNavDestinationSelected(item, navController)
        }

        // 3. 重复点击"今日"也回到今日天视图
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_home) {
                homeViewModel.goToToday()
            }
        }
    }

    /**
     * Android 13+ 需要请求通知权限
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 自动启动监控服务
     */
    private fun autoStartService() {
        lifecycleScope.launch {
            // 首次启动时自动开启服务
            val serviceIntent = Intent(this@MainActivity, ScreenMonitorService::class.java)
            startForegroundService(serviceIntent)
            prefManager.setServiceRunning(true)
        }
    }
}
