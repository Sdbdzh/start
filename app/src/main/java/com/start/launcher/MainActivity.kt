package com.start.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.start.launcher.data.settings.SettingsRepository
import com.start.launcher.data.settings.ThemeSettings
import com.start.launcher.theme.StartTheme
import com.start.launcher.ui.common.Haptics
import com.start.launcher.ui.main.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 启动画面保持到主题设置就绪，避免主题闪变；引导页已移除，直接进入主界面
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(
                initialValue = cachedSettings,
            )
            // 冷启动缓存为 null：等 DataStore 首个值再放行启动画面；热重建直接用缓存
            splashScreen.setKeepOnScreenCondition { settings == null }
            LaunchedEffect(settings) {
                settings?.let {
                    cachedSettings = it
                    Haptics.intensity = it.hapticIntensity
                }
            }
            settings?.let { s ->
                StartTheme(settings = s) { MainScreen() }
            }
        }
    }

    companion object {
        /** 进程内缓存主题设置：Activity 重建免等待免闪烁，null 表示尚未读取 */
        private var cachedSettings: ThemeSettings? = null
    }
}
