package com.start.launcher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

/**
 * 主题设置仓库：基于 DataStore Preferences 持久化
 * 枚举以 ordinal int 存储，新增枚举值时只在末尾追加，保证兼容
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val MODE = intPreferencesKey("theme_mode")
        val DARK_STYLE = intPreferencesKey("dark_style")
        val COLOR_SOURCE = intPreferencesKey("color_source")
        val SEED_COLOR = longPreferencesKey("seed_color")
        val HAPTIC = intPreferencesKey("haptic_intensity")
        val TAB_MULTI_LAYER = booleanPreferencesKey("tab_multi_layer")
    }

    val settings: Flow<ThemeSettings> = context.themeDataStore.data.map { prefs ->
        ThemeSettings(
            mode = prefs[Keys.MODE]?.let { ThemeMode.entries[it] } ?: ThemeMode.SYSTEM,
            darkStyle = prefs[Keys.DARK_STYLE]?.let { DarkStyle.entries[it] } ?: DarkStyle.SOFT,
            colorSource = prefs[Keys.COLOR_SOURCE]?.let { ColorSource.entries[it] } ?: ColorSource.WALLPAPER,
            seedColor = prefs[Keys.SEED_COLOR] ?: 0xFF4C6FFF,
            hapticIntensity = prefs[Keys.HAPTIC]?.let { HapticIntensity.entries[it] }
                ?: HapticIntensity.FOLLOW_SYSTEM,
            tabMultiLayer = prefs[Keys.TAB_MULTI_LAYER] ?: false,
        )
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[Keys.MODE] = mode.ordinal }
    }

    suspend fun setDarkStyle(style: DarkStyle) {
        context.themeDataStore.edit { it[Keys.DARK_STYLE] = style.ordinal }
    }

    suspend fun setColorSource(source: ColorSource) {
        context.themeDataStore.edit { it[Keys.COLOR_SOURCE] = source.ordinal }
    }

    suspend fun setSeedColor(color: Long) {
        context.themeDataStore.edit { it[Keys.SEED_COLOR] = color }
    }

    suspend fun setHapticIntensity(intensity: HapticIntensity) {
        context.themeDataStore.edit { it[Keys.HAPTIC] = intensity.ordinal }
    }

    suspend fun setTabMultiLayer(enabled: Boolean) {
        context.themeDataStore.edit { it[Keys.TAB_MULTI_LAYER] = enabled }
    }

    // ── 导入配置 ────────────────────────────────

    /** 读取当前主题设置（导出配置用） */
    suspend fun getCurrent(): ThemeSettings = settings.first()

    /** 一次性写入全部主题设置（导入配置用） */
    suspend fun setAll(s: ThemeSettings) {
        context.themeDataStore.edit {
            it[Keys.MODE] = s.mode.ordinal
            it[Keys.DARK_STYLE] = s.darkStyle.ordinal
            it[Keys.COLOR_SOURCE] = s.colorSource.ordinal
            it[Keys.SEED_COLOR] = s.seedColor
            it[Keys.HAPTIC] = s.hapticIntensity.ordinal
            it[Keys.TAB_MULTI_LAYER] = s.tabMultiLayer
        }
    }
}
