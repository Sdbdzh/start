package com.start.launcher.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.start.launcher.data.settings.ColorSource
import com.start.launcher.data.settings.DarkStyle
import com.start.launcher.data.settings.SettingsRepository
import com.start.launcher.data.settings.ThemeMode
import com.start.launcher.data.settings.ThemeSettings
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes
import kotlinx.coroutines.launch

/** 预设种子色（莫奈风格的高级灰调色板） */
private val SEED_COLORS = listOf(
    Color(0xFF4C6FFF), // 蓝
    Color(0xFF7C4DFF), // 紫
    Color(0xFFE91E63), // 品红
    Color(0xFFFF6B6B), // 珊瑚红
    Color(0xFFFF9800), // 橙
    Color(0xFFFFC107), // 琥珀
    Color(0xFF4CAF50), // 绿
    Color(0xFF00BCD4), // 青
    Color(0xFF607D8B), // 蓝灰
    Color(0xFF9E9E9E), // 灰
    Color(0xFF795548), // 棕
    Color(0xFFF44336), // 红
)

/**
 * 主题设置对话框：明暗模式 / 深色风格 / 取色源 / 种子色
 */
@Composable
fun ThemeSettingsDialog(
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // 用 applicationContext 创建仓库读取设置
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = ThemeSettings())

    ModalDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("主题设置", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)

            // 明暗模式
            SettingGroup(label = "明暗模式") {
                LabeledSegment(
                    label = "",
                    items = ThemeMode.entries.map { it.displayName() },
                    selectedIndex = settings.mode.ordinal,
                    onSelect = { idx ->
                        scope.launch { settingsRepo.setMode(ThemeMode.entries[idx]) }
                    },
                )
            }

            // 深色风格（仅深色模式相关，浅色时显示但不生效）
            SettingGroup(label = "深色风格") {
                LabeledSegment(
                    label = "",
                    items = DarkStyle.entries.map { it.displayName() },
                    selectedIndex = settings.darkStyle.ordinal,
                    onSelect = { idx ->
                        scope.launch { settingsRepo.setDarkStyle(DarkStyle.entries[idx]) }
                    },
                )
            }

            // 取色源
            SettingGroup(label = "取色方式") {
                LabeledSegment(
                    label = "",
                    items = ColorSource.entries.map { it.displayName() },
                    selectedIndex = settings.colorSource.ordinal,
                    onSelect = { idx ->
                        scope.launch { settingsRepo.setColorSource(ColorSource.entries[idx]) }
                    },
                )
            }

            // 种子色（仅 SEED 模式可用）
            if (settings.colorSource == ColorSource.SEED) {
                SettingGroup(label = "种子色") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        SEED_COLORS.forEach { color ->
                            val selected = settings.seedColor == color.toArgbLong()
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) scheme.onSurface else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        scope.launch { settingsRepo.setSeedColor(color.toArgbLong()) }
                                    },
                            )
                        }
                    }
                }
            }

            // 关于
            SettingGroup(label = "关于") {
                Text("START v0.1.0", fontSize = 14.sp, color = scheme.onSurface)
                Text("作者：Enik", fontSize = 14.sp, color = scheme.onSurface)
                Text("QQ：1334204015", fontSize = 14.sp, color = scheme.onSurface)
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Text(
                    text = "GitHub：https://github.com/Sdbdzh/start",
                    fontSize = 14.sp,
                    color = scheme.primary,
                    modifier = Modifier.clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sdbdzh/start")))
                    },
                )
            }

            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun SettingGroup(label: String, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant)
        content()
    }
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

private fun DarkStyle.displayName(): String = when (this) {
    DarkStyle.SOFT -> "柔和黑"
    DarkStyle.OLED -> "OLED 纯黑"
}

private fun ColorSource.displayName(): String = when (this) {
    ColorSource.WALLPAPER -> "壁纸动态"
    ColorSource.SEED -> "种子色"
}

/** Color → ARGB Long（无符号），用于与 seedColor 比较/存储 */
private fun Color.toArgbLong(): Long = (this.toArgb().toLong() and 0xFFFFFFFFL)