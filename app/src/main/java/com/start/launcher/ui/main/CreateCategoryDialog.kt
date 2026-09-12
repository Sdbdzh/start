package com.start.launcher.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.SortType
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes
import com.start.launcher.ui.common.rememberHaptics
import kotlinx.coroutines.launch

/**
 * 创建分类页面（全屏）：起名 → 选应用（可搜索）→ 排序方式/列数/缩放
 * 顶部固定标题栏，底部固定按钮，中间内容自由滚动。
 */
@Composable
fun CreateCategoryDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onCreated: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val allApps by viewModel.allApps.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = rememberHaptics()

    var name by remember { mutableStateOf("") }
    var selectedPkgNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortType by remember { mutableStateOf(SortType.MANUAL) }
    var columns by remember { mutableStateOf(4) }
    var scale by remember { mutableStateOf(1.0f) }
    var labelScale by remember { mutableStateOf(1.0f) }
    var showTwoLine by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var appsExpanded by remember { mutableStateOf(false) }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (appsExpanded) 180f else 0f,
        label = "arrow",
    )

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    FullScreenPage(
        title = "创建分类",
        onBack = { if (!creating) onDismiss() },
        content = {
            // 名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            // 选择应用（默认折叠）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceContainerHigh.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptic(); appsExpanded = !appsExpanded }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "选择应用 (${selectedPkgNames.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "▾",
                        fontSize = 16.sp,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation),
                    )
                }

                if (appsExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    ) {
                        // 全选
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    haptic()
                                    selectedPkgNames = if (selectedPkgNames.size == filteredApps.size && filteredApps.isNotEmpty()) {
                                        selectedPkgNames - filteredApps.map { it.packageName }.toSet()
                                    } else {
                                        selectedPkgNames + filteredApps.map { it.packageName }.toSet()
                                    }
                                },
                            ) {
                                Text(
                                    if (filteredApps.isNotEmpty() && selectedPkgNames.containsAll(filteredApps.map { it.packageName })) "取消全选" else "全选",
                                    fontSize = 13.sp,
                                    color = scheme.primary,
                                )
                            }
                        }

                        // 搜索框
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索应用...", fontSize = 13.sp, color = scheme.onSurfaceVariant) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        // 应用列表
                        filteredApps.forEach { app ->
                            val selected = app.packageName in selectedPkgNames
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) scheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                    .clickable {
                                        haptic()
                                        selectedPkgNames = selectedPkgNames.toMutableSet().apply {
                                            if (contains(app.packageName)) remove(app.packageName) else add(app.packageName)
                                        }
                                    }
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(app = app, viewModel = viewModel, size = 36.dp)
                                Spacer(Modifier.width(Spacing.md))
                                Text(app.appName, fontSize = 14.sp, color = scheme.onSurface, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) scheme.primary else Color.Transparent)
                                        .border(if (selected) 0.dp else 2.dp, scheme.outlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // 排序方式
            LabeledSegment("排序方式", SortType.entries.map { it.displayName() }, sortType.ordinal) {
                sortType = SortType.entries[it]
            }

            // 列数
            LabeledSegment("列数", (2..6).map { "$it 列" }, columns - 2) {
                columns = it + 2
            }

            // 图标缩放
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("图标缩放", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant)
                    Text("${"%.1f".format(scale)}x", fontSize = 13.sp, color = scheme.primary)
                }
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 0.6f..1.5f,
                    steps = 8,
                )
            }

            // 名字缩放
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("名字缩放", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant)
                    Text("${"%.1f".format(labelScale)}x", fontSize = 13.sp, color = scheme.primary)
                }
                Slider(
                    value = labelScale,
                    onValueChange = { labelScale = it },
                    valueRange = 0.6f..1.5f,
                    steps = 8,
                )
            }

            // 双行应用名称显示
            SwitchRow(
                label = "双行应用名称显示",
                checked = showTwoLine,
                onCheckedChange = { showTwoLine = it },
            )
        },
        bottomBar = {
            TextButton(onClick = { haptic(); onDismiss() }, enabled = !creating) { Text("取消") }
            Spacer(Modifier.width(Spacing.sm))
            Button(
                onClick = {
                    haptic()
                    if (name.isNotBlank() && selectedPkgNames.isNotEmpty() && !creating) {
                        creating = true
                        scope.launch {
                            val id = viewModel.createCategory(name, selectedPkgNames.toList(), sortType, columns, scale, labelScale, showTwoLine)
                            onCreated(id)
                        }
                    }
                },
                enabled = name.isNotBlank() && selectedPkgNames.isNotEmpty() && !creating,
                shape = StartShapes.pill,
            ) { Text(if (creating) "创建中..." else "创建") }
        },
    )
}

/** 排序方式中文名 */
fun SortType.displayName(): String = when (this) {
    SortType.MANUAL -> "手动"
    SortType.NAME -> "名称"
    SortType.FREQUENCY -> "频率"
    SortType.RECENT -> "最近"
}

/** 带标签的分段选择器 */
@Composable
fun LabeledSegment(label: String, items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StartShapes.pill)
                .background(scheme.surfaceContainerHigh),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(StartShapes.pill)
                        .background(if (selected) scheme.primary else Color.Transparent)
                        .clickable {
                            haptic()
                            onSelect(index)
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 应用图标预览（异步加载） */
@Composable
fun AppIcon(app: AppEntity, viewModel: MainViewModel, size: androidx.compose.ui.unit.Dp) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(app.packageName) {
        bitmap = viewModel.loadIconBitmap(app.packageName)?.asImageBitmap()
    }
    val bmp = bitmap
    if (bmp != null) {
        androidx.compose.foundation.Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size).clip(StartShapes.icon),
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(StartShapes.icon).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.appName.take(1), fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 全屏页面容器：顶部固定标题栏（含返回按钮），底部固定按钮，中间内容整体滚动。
 * 彻底避免弹窗内 heightIn 导致 weight 塌缩、内容与按钮消失的问题。
 */
@Composable
fun FullScreenPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .clickable { haptic(); onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                }
                Spacer(Modifier.width(Spacing.md))
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            // 内容区（整体滚动）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                content()
            }

            // 底部按钮（固定）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomBar()
            }
        }
    }
}

/** 开关行（标签 + 开关） */
@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic()
                onCheckedChange(!checked)
            }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic()
                onCheckedChange(it)
            },
        )
    }
}

/** 半透明模态弹窗容器（供主题设置等轻量弹窗使用） */
@Composable
fun ModalDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 580.dp)
                .clip(StartShapes.heroCard)
                .background(scheme.surface)
                .clickable { /* 阻止关闭 */ }
                .verticalScroll(rememberScrollState())
                .padding(Spacing.sm),
        ) {
            content()
        }
    }
}