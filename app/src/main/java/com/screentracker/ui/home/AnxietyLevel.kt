package com.screentracker.ui.home

import android.graphics.Color

/**
 * 焦虑等级数据模型
 * 基于每日屏幕点亮次数划分
 */
enum class AnxietyLevel(
    val level: Int,
    val minCount: Int,
    val maxCount: Int,
    val nameCn: String,
    val nameEn: String,
    val emoji: String,
    val description: String,
    val colorHex: String,
    val gradientStart: String,
    val gradientEnd: String
) {
    LV1(
        level = 1,
        minCount = 0,
        maxCount = 40,
        nameCn = "人间清醒",
        nameEn = "Zen Master",
        emoji = "🧘",
        description = "手机只是工具，你才是主人。拥有极强的专注力，生活充实，几乎不存在数字焦虑。",
        colorHex = "#8DA88F",
        gradientStart = "#A8C5AA",
        gradientEnd = "#8DA88F"
    ),
    LV2(
        level = 2,
        minCount = 41,
        maxCount = 70,
        nameCn = "松弛玩家",
        nameEn = "Chill Zone",
        emoji = "😌",
        description = "适度使用，有急有缓。会查看消息但不会被消息绑架，心态平稳，略带睡意。",
        colorHex = "#7A9B7C",
        gradientStart = "#8DA88F",
        gradientEnd = "#6B8B6D"
    ),
    LV3(
        level = 3,
        minCount = 71,
        maxCount = 100,
        nameCn = "微蕉青年",
        nameEn = "Mild Green",
        emoji = "😐",
        description = "大多数人的状态。习惯性亮屏，偶尔无意识解锁，处于'蕉绿'的临界点，需要警惕。",
        colorHex = "#F5D65D",
        gradientStart = "#F9E79F",
        gradientEnd = "#F5D65D"
    ),
    LV4(
        level = 4,
        minCount = 101,
        maxCount = 140,
        nameCn = "指尖卷王",
        nameEn = "Finger Gym",
        emoji = "😰",
        description = "手指运动量过大。频繁检查通知，害怕错过信息(FOMO)，焦虑值开始明显上升。",
        colorHex = "#E57373",
        gradientStart = "#EF9A9A",
        gradientEnd = "#E57373"
    ),
    LV5(
        level = 5,
        minCount = 141,
        maxCount = Int.MAX_VALUE,
        nameCn = "赛博囚徒",
        nameEn = "System Overload",
        emoji = "🚨",
        description = "被算法锁死。亮屏已成条件反射，注意力碎片化严重，急需'数字排毒'，悬崖勒马！",
        colorHex = "#D32F2F",
        gradientStart = "#E57373",
        gradientEnd = "#B71C1C"
    );

    companion object {
        fun fromCount(count: Int): AnxietyLevel {
            return values().find { count in it.minCount..it.maxCount } ?: LV3
        }

        fun getColorInt(hex: String): Int {
            return Color.parseColor(hex)
        }
    }
}
