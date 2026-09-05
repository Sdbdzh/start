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
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val settings: Flow<ThemeSettings> = context.themeDataStore.data.map { prefs ->
        ThemeSettings(
            mode = prefs[Keys.MODE]?.let { ThemeMode.entries[it] } ?: ThemeMode.SYSTEM,
            darkStyle = prefs[Keys.DARK_STYLE]?.let { DarkStyle.entries[it] } ?: DarkStyle.SOFT,
            colorSource = prefs[Keys.COLOR_SOURCE]?.let { ColorSource.entries[it] } ?: ColorSource.WALLPAPER,
            seedColor = prefs[Keys.SEED_COLOR] ?: 0xFF4C6FFF,
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

    // ── 首启标记 ────────────────────────────────

    /** 是否已完成首启向导 */
    suspend fun isSetupCompleted(): Boolean =
        context.themeDataStore.data.first()[Keys.SETUP_COMPLETED] ?: false

    suspend fun setSetupCompleted(completed: Boolean) {
        context.themeDataStore.edit { it[Keys.SETUP_COMPLETED] = completed }
    }
}
