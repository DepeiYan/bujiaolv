package com.screentracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.screentracker.MainActivity
import com.screentracker.R
import com.screentracker.data.repository.ScreenEventRepository
import com.screentracker.receiver.ScreenReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 屏幕监控前台服务
 * 持续运行在后台, 监听屏幕点亮和熄灭事件
 * 使用前台服务保证不被系统杀掉
 */
class ScreenMonitorService : Service() {

    private var screenReceiver: ScreenReceiver? = null

    companion object {
        const val CHANNEL_ID = "screen_monitor_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerScreenReceiver()

        // 服务启动时检查屏幕状态，处理未关闭的事件
        checkAndFixScreenState()
    }

    /**
     * 检查并修复屏幕状态
     * 如果服务重启时屏幕是熄灭状态，关闭所有未关闭的事件
     */
    private fun checkAndFixScreenState() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        if (!isScreenOn) {
            // 屏幕是熄灭的，关闭所有未关闭的事件
            CoroutineScope(Dispatchers.IO).launch {
                val repository = ScreenEventRepository(this@ScreenMonitorService)
                repository.recordScreenOff()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenReceiver()

        // 服务被销毁时，尝试关闭未关闭的事件
        // 注意：这里不能保证一定执行，因为进程可能被系统强制终止
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ScreenEventRepository(this@ScreenMonitorService)
                repository.recordScreenOff()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 注册屏幕状态广播接收器
     */
    private fun registerScreenReceiver() {
        if (screenReceiver == null) {
            screenReceiver = ScreenReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenReceiver, filter)
        }
    }

    /**
     * 注销屏幕状态广播接收器
     */
    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            screenReceiver = null
        }
    }

    /**
     * 创建通知渠道 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "芭蕉绿屏幕监控",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "持续监控屏幕使用情况"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("芭蕉绿运行中")
            .setContentText("正在记录您的屏幕使用情况")
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
