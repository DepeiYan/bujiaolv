package com.screentracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.screentracker.MainActivity
import com.screentracker.R

/**
 * 通知帮助类
 * 管理使用提醒通知的发送
 */
object NotificationHelper {

    private const val REMINDER_CHANNEL_ID = "usage_reminder_channel"
    private const val REMINDER_NOTIFICATION_ID = 2001

    /**
     * 初始化通知渠道
     */
    fun createNotificationChannels(context: Context) {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "使用提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "当屏幕使用超过目标时发送提醒"
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 发送使用超标提醒通知
     */
    fun sendGoalExceededNotification(context: Context, title: String, message: String) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
    }
}
