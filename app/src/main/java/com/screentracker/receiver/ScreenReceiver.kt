package com.screentracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screentracker.data.db.AppDatabase
import com.screentracker.data.repository.ScreenEventRepository
import com.screentracker.util.NotificationHelper
import com.screentracker.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 屏幕状态广播接收器
 * 监听 ACTION_SCREEN_ON 和 ACTION_SCREEN_OFF 事件
 */
class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = ScreenEventRepository(context)

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.recordScreenOn()
                    checkUsageGoal(context, repository)
                }
            }
            Intent.ACTION_SCREEN_OFF -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.recordScreenOff()
                }
            }
        }
    }

    /**
     * 检查今日使用情况是否超过设定的目标
     * 如果超过, 发送提醒通知
     */
    private suspend fun checkUsageGoal(context: Context, repository: ScreenEventRepository) {
        val prefManager = PreferenceManager(context)

        if (!prefManager.isReminderEnabled()) return

        val todayStr = repository.getTodayDateStr()
        val dao = AppDatabase.getInstance(context).screenEventDao()

        // 检查点亮次数目标
        val maxCount = prefManager.getDailyScreenOnGoal()
        if (maxCount > 0) {
            val currentCount = dao.getScreenOnCountByDateSync(todayStr)
            if (currentCount >= maxCount) {
                NotificationHelper.sendGoalExceededNotification(
                    context,
                    "屏幕点亮次数已达 $currentCount 次",
                    "已超过您设定的每日 $maxCount 次目标，请注意控制手机使用。"
                )
            }
        }

        // 检查使用时长目标 (分钟)
        val maxMinutes = prefManager.getDailyDurationGoal()
        if (maxMinutes > 0) {
            val currentDuration = dao.getTotalDurationByDateSync(todayStr)
            val currentMinutes = currentDuration / 1000 / 60
            if (currentMinutes >= maxMinutes) {
                NotificationHelper.sendGoalExceededNotification(
                    context,
                    "今日屏幕使用时长已达 ${currentMinutes} 分钟",
                    "已超过您设定的每日 $maxMinutes 分钟目标，请放下手机休息一下。"
                )
            }
        }
    }
}
