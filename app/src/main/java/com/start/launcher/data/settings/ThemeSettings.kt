package com.start.launcher.data.settings

/**
 * 主题设置数据模型
 *
 * 主题逻辑：模式（跟随系统/浅色/深色）决定明暗方向，
 * 深色模式下可通过 [darkStyle] 再选择「柔和深色」或「OLED 纯黑」；
 * [colorSource] 决定配色来源：跟随壁纸的动态莫奈，或用户自定义种子色。
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/** 深色模式下的子风格 */
enum class DarkStyle {
    SOFT,  // 柔和深色：标准 M3 深色表面
    OLED   // 纯黑：表面压至纯黑，省电且对比强烈
}

/** 配色来源 */
enum class ColorSource {
    WALLPAPER, // 动态莫奈：跟随系统壁纸取色
    SEED       // 自定义：从种子色生成完整 M3 色板
}

/** 点击震动反馈强度 */
enum class HapticIntensity {
    FOLLOW_SYSTEM, // 跟随系统触感设置
    OFF,           // 关闭
    LIGHT,         // 轻
    MEDIUM,        // 中
    STRONG,        // 强
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val darkStyle: DarkStyle = DarkStyle.SOFT,
    val colorSource: ColorSource = ColorSource.WALLPAPER,
    val seedColor: Long = 0xFF4C6FFF, // 默认种子色：靛蓝
    /** 点击震动强度 */
    val hapticIntensity: HapticIntensity = HapticIntensity.FOLLOW_SYSTEM,
    /** 收藏栏（分类胶囊）按钮换行多层显示 */
    val tabMultiLayer: Boolean = false,
) {
    val isDark: Boolean
        get() = mode == ThemeMode.DARK
}
