package com.start.launcher.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.materialkolor.dynamicColorScheme
import com.start.launcher.data.settings.ColorSource
import com.start.launcher.data.settings.DarkStyle
import com.start.launcher.data.settings.ThemeMode
import com.start.launcher.data.settings.ThemeSettings

/**
 * 饱和度增强：大胆色块风的核心——将主色系饱和度拉高 1.5x，
 * 表面色不变，保持可读性。深色模式下增强因子略低，避免刺眼。
 */
private fun Color.boostSaturation(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * factor).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun ColorScheme.boldBlock(dark: Boolean): ColorScheme {
    val factor = if (dark) 1.3f else 1.5f
    return copy(
        primary = primary.boostSaturation(factor),
        primaryContainer = primaryContainer.boostSaturation(factor),
        onPrimaryContainer = onPrimaryContainer.boostSaturation(factor * 0.8f),
        secondary = secondary.boostSaturation(factor),
        secondaryContainer = secondaryContainer.boostSaturation(factor),
        tertiary = tertiary.boostSaturation(factor),
        tertiaryContainer = tertiaryContainer.boostSaturation(factor),
    )
}

/**
 * OLED 纯黑：表面色阶压至纯黑系，主色不受影响
 */
private fun ColorScheme.toOled(): ColorScheme = copy(
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1A1A1A),
)

/**
 * START 主题引擎
 *
 * 解析顺序：
 * 1. 明暗方向：ThemeMode
 * 2. 配色来源：WALLPAPER 动态莫奈 / SEED 种子色
 * 3. 饱和度增强：bold block 风格，拉高主色系饱和度
 * 4. 深色子风格：OLED 纯黑
 * 5. 挂载 MD3E expressive 动效 + 设计令牌形状
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StartTheme(
    settings: ThemeSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val oled = dark && settings.darkStyle == DarkStyle.OLED

    val scheme: ColorScheme = when (settings.colorSource) {
        ColorSource.WALLPAPER -> {
            val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val boosted = base.boldBlock(dark)
            if (oled) boosted.toOled() else boosted
        }
        ColorSource.SEED -> {
            dynamicColorScheme(
                seedColor = Color(settings.seedColor),
                isDark = dark,
                isAmoled = oled,
            ).boldBlock(dark)
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        shapes = StartShapes.material,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}