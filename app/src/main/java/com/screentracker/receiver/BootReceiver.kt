package com.screentracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screentracker.service.ScreenMonitorService

/**
 * 开机启动广播接收器
 * 设备重启后自动启动屏幕监控服务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ScreenMonitorService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
