package com.start.launcher.ui.common

import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.start.launcher.data.settings.HapticIntensity

/**
 * 全局点击震动控制器。
 * 强度由设置驱动（MainActivity 观察设置流后写入 [intensity]）：
 * - 跟随系统：走 View.performHapticFeedback，是否震动由系统触感设置决定；
 * - 自定义强度：直接驱动振动器，按时长/振幅区分轻、中、强。
 */
object Haptics {

    @Volatile
    var intensity: HapticIntensity = HapticIntensity.FOLLOW_SYSTEM

    /** 按当前强度执行一次点击震动 */
    fun click(view: View) {
        when (intensity) {
            HapticIntensity.FOLLOW_SYSTEM ->
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            HapticIntensity.OFF -> Unit
            HapticIntensity.LIGHT -> vibrate(view, 10, 72)
            HapticIntensity.MEDIUM -> vibrate(view, 15, 160)
            HapticIntensity.STRONG -> vibrate(view, 20, 255)
        }
    }

    private fun vibrate(view: View, millis: Long, amplitude: Int) {
        val vibrator = view.context
            .getSystemService(VibratorManager::class.java)
            ?.defaultVibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(millis, amplitude))
    }
}

/** 取当前界面的震动触发器：`val haptic = rememberHaptics()`，在点击处理里调用 `haptic()` */
@Composable
fun rememberHaptics(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { Haptics.click(view) } }
}
