package com.screentracker.ui.settings

import android.app.Application
import androidx.lifecycle.*
import com.screentracker.util.PreferenceManager
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefManager = PreferenceManager(application)

    /** 是否启用提醒 */
    val reminderEnabled: LiveData<Boolean> = prefManager.reminderEnabledFlow.asLiveData()

    /** 每日点亮次数目标 */
    val dailyScreenOnGoal: LiveData<Int> = prefManager.dailyScreenOnGoalFlow.asLiveData()

    /** 每日使用时长目标(分钟) */
    val dailyDurationGoal: LiveData<Int> = prefManager.dailyDurationGoalFlow.asLiveData()

    /** 服务是否运行 */
    val serviceRunning: LiveData<Boolean> = prefManager.serviceRunningFlow.asLiveData()

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefManager.setReminderEnabled(enabled)
        }
    }

    fun setDailyScreenOnGoal(count: Int) {
        viewModelScope.launch {
            prefManager.setDailyScreenOnGoal(count)
        }
    }

    fun setDailyDurationGoal(minutes: Int) {
        viewModelScope.launch {
            prefManager.setDailyDurationGoal(minutes)
        }
    }

    fun setServiceRunning(running: Boolean) {
        viewModelScope.launch {
            prefManager.setServiceRunning(running)
        }
    }
}
