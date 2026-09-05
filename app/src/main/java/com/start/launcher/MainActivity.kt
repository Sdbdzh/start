package com.start.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.start.launcher.data.settings.SettingsRepository
import com.start.launcher.data.settings.ThemeSettings
import com.start.launcher.theme.StartTheme
import com.start.launcher.ui.main.MainScreen
import com.start.launcher.ui.setup.SetupScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(
                initialValue = ThemeSettings(),
            )
            StartTheme(settings = settings) {
                var showSetup by remember { mutableStateOf<Boolean?>(null) }
                val scope = rememberCoroutineScope()

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val done = withContext(Dispatchers.IO) { settingsRepo.isSetupCompleted() }
                    showSetup = !done
                }

                when (showSetup) {
                    null -> { /* 加载中 */ }
                    true -> SetupScreen(onComplete = {
                        scope.launch { settingsRepo.setSetupCompleted(true) }
                        showSetup = false
                    })
                    false -> MainScreen()
                }
            }
        }
    }
}