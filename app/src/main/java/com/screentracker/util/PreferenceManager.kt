package com.screentracker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 偏好设置管理器
 * 使用 DataStore 存储用户的设置项
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screen_tracker_prefs")

class PreferenceManager(private val context: Context) {

    companion object {
        /** 是否启用提醒 */
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        /** 每日屏幕点亮次数目标 */
        val KEY_DAILY_SCREEN_ON_GOAL = intPreferencesKey("daily_screen_on_goal")
        /** 每日屏幕使用时长目标 (分钟) */
        val KEY_DAILY_DURATION_GOAL = intPreferencesKey("daily_duration_goal")
        /** 监控服务是否已启动 */
        val KEY_SERVICE_RUNNING = booleanPreferencesKey("service_running")
    }

    // ============ 提醒开关 ============

    val reminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMINDER_ENABLED] ?: false
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun isReminderEnabled(): Boolean {
        return context.dataStore.data.first()[KEY_REMINDER_ENABLED] ?: false
    }

    // ============ 点亮次数目标 ============

    val dailyScreenOnGoalFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_SCREEN_ON_GOAL] ?: 0
    }

    suspend fun setDailyScreenOnGoal(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_SCREEN_ON_GOAL] = count
        }
    }

    suspend fun getDailyScreenOnGoal(): Int {
        return context.dataStore.data.first()[KEY_DAILY_SCREEN_ON_GOAL] ?: 0
    }

    // ============ 使用时长目标 ============

    val dailyDurationGoalFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_DURATION_GOAL] ?: 0
    }

    suspend fun setDailyDurationGoal(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_DURATION_GOAL] = minutes
        }
    }

    suspend fun getDailyDurationGoal(): Int {
        return context.dataStore.data.first()[KEY_DAILY_DURATION_GOAL] ?: 0
    }

    // ============ 服务状态 ============

    val serviceRunningFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVICE_RUNNING] ?: false
    }

    suspend fun setServiceRunning(running: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE_RUNNING] = running
        }
    }

    suspend fun isServiceRunning(): Boolean {
        return context.dataStore.data.first()[KEY_SERVICE_RUNNING] ?: false
    }
}
