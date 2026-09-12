package com.start.launcher.ui.main

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.start.launcher.data.settings.BgScaleType
import com.start.launcher.data.settings.ColorSource
import com.start.launcher.data.settings.DarkStyle
import com.start.launcher.data.settings.HapticIntensity
import com.start.launcher.data.settings.SettingsRepository
import com.start.launcher.data.settings.ThemeMode
import com.start.launcher.data.settings.ThemeSettings
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes
import com.start.launcher.ui.common.rememberHaptics
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
 * 外加数据管理：刷新应用、导出配置、导入配置
 */
@Composable
fun ThemeSettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onRefreshApps: () -> Unit,
    onSnackbar: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptics()

    // 用 applicationContext 创建仓库读取设置
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = ThemeSettings())

    // 导出配置：保存到用户选择的位置
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            onDismiss()
            viewModel.exportConfig(uri) { _, msg -> onSnackbar(msg) }
        }
    }
    // 导入配置：从用户选择的位置读取
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onDismiss()
            viewModel.importConfig(uri) { _, msg -> onSnackbar(msg) }
        }
    }
    // 选择背景图（系统照片选择器）
    val pickBackgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.saveBackground(uri) { _, msg -> onSnackbar(msg) }
        }
    }

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
                                        haptic()
                                        scope.launch { settingsRepo.setSeedColor(color.toArgbLong()) }
                                    },
                            )
                        }
                    }
                }
            }

            // 触感反馈
            SettingGroup(label = "触感反馈") {
                LabeledSegment(
                    label = "",
                    items = HapticIntensity.entries.map { it.displayName() },
                    selectedIndex = settings.hapticIntensity.ordinal,
                    onSelect = { idx ->
                        scope.launch { settingsRepo.setHapticIntensity(HapticIntensity.entries[idx]) }
                    },
                )
            }

            // 收藏栏
            SettingGroup(label = "收藏栏") {
                SwitchRow(
                    label = "收藏栏按钮多层显示",
                    checked = settings.tabMultiLayer,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepo.setTabMultiLayer(checked) }
                    },
                )
            }

            // 背景
            SettingGroup(label = "背景") {
                if (settings.bgEnabled) {
                    LabeledSegment(
                        label = "显示方式",
                        items = listOf("拉伸", "自适应", "裁剪"),
                        selectedIndex = settings.bgScaleType.ordinal,
                        onSelect = { idx ->
                            scope.launch { settingsRepo.setBgScaleType(BgScaleType.entries[idx]) }
                        },
                    )
                    // 顶部面板不透明度
                    var panelOpacity by remember { mutableStateOf(settings.panelOpacity) }
                    LaunchedEffect(settings.panelOpacity) { panelOpacity = settings.panelOpacity }
                    Column {
                        Text("面板不透明度", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Slider(
                            value = panelOpacity,
                            onValueChange = { panelOpacity = it },
                            onValueChangeFinished = {
                                scope.launch { settingsRepo.setPanelOpacity(panelOpacity) }
                            },
                            valueRange = 0.1f..1f,
                        )
                    }
                    // 顶部面板模糊度
                    var panelBlur by remember { mutableStateOf(settings.panelBlur) }
                    LaunchedEffect(settings.panelBlur) { panelBlur = settings.panelBlur }
                    Column {
                        Text("面板模糊度", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Slider(
                            value = panelBlur,
                            onValueChange = { panelBlur = it },
                            onValueChangeFinished = {
                                scope.launch { settingsRepo.setPanelBlur(panelBlur) }
                            },
                            valueRange = 0f..30f,
                        )
                    }
                    // 亮度
                    var brightness by remember { mutableStateOf(settings.bgBrightness) }
                    LaunchedEffect(settings.bgBrightness) { brightness = settings.bgBrightness }
                    Column {
                        Text("亮度", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            onValueChangeFinished = {
                                scope.launch { settingsRepo.setBgBrightness(brightness) }
                            },
                            valueRange = 0.2f..1f,
                        )
                    }
                    // 模糊度
                    var blurValue by remember { mutableStateOf(settings.bgBlur) }
                    LaunchedEffect(settings.bgBlur) { blurValue = settings.bgBlur }
                    Column {
                        Text("模糊度", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                        Slider(
                            value = blurValue,
                            onValueChange = { blurValue = it },
                            onValueChangeFinished = {
                                scope.launch { settingsRepo.setBgBlur(blurValue) }
                            },
                            valueRange = 0f..25f,
                        )
                    }
                    ActionRow(
                        title = "更换背景图片",
                        description = "重新选择一张图片",
                    ) {
                        pickBackgroundLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    ActionRow(
                        title = "清除背景图片",
                        description = "恢复默认背景",
                    ) { viewModel.clearBackground { _, msg -> onSnackbar(msg) } }
                } else {
                    ActionRow(
                        title = "设置背景图片",
                        description = "选择一张图片作为主页面背景",
                    ) {
                        pickBackgroundLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                }
            }

            // 主页面
            SettingGroup(label = "主页面") {
                SwitchRow(
                    label = "隐藏搜索框",
                    checked = settings.hideSearchBar,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepo.setHideSearchBar(checked) }
                    },
                )
            }

            // 数据管理
            SettingGroup(label = "数据") {
                ActionRow(
                    title = "刷新应用",
                    description = "重新扫描已安装应用，新增/移除自动同步",
                ) { onRefreshApps() }
                ActionRow(
                    title = "导出配置",
                    description = "将分类与主题设置保存为 JSON 文件",
                ) { exportLauncher.launch("start_config_v1.json") }
                ActionRow(
                    title = "导入配置",
                    description = "从 JSON 文件恢复分类与主题设置",
                ) { importLauncher.launch(arrayOf("application/json")) }
            }

            // 关于
            SettingGroup(label = "关于") {
                Text("START v0.2.0", fontSize = 14.sp, color = scheme.onSurface)
                Text("作者：Enik", fontSize = 14.sp, color = scheme.onSurface)
                Text("QQ：1334204015", fontSize = 14.sp, color = scheme.onSurface)
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Text(
                    text = "GitHub：https://github.com/Sdbdzh/start",
                    fontSize = 14.sp,
                    color = scheme.primary,
                    modifier = Modifier.clickable {
                        haptic()
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

/** 可点击的操作行（标题 + 描述） */
@Composable
private fun ActionRow(title: String, description: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic()
                onClick()
            }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
        Text(description, fontSize = 12.sp, color = scheme.onSurfaceVariant)
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

private fun HapticIntensity.displayName(): String = when (this) {
    HapticIntensity.FOLLOW_SYSTEM -> "跟随系统"
    HapticIntensity.OFF -> "关闭"
    HapticIntensity.LIGHT -> "轻"
    HapticIntensity.MEDIUM -> "中"
    HapticIntensity.STRONG -> "强"
}

/** Color → ARGB Long（无符号），用于与 seedColor 比较/存储 */
private fun Color.toArgbLong(): Long = (this.toArgb().toLong() and 0xFFFFFFFFL)