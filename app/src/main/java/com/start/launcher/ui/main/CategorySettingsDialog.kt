package com.start.launcher.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes
import com.start.launcher.ui.common.rememberHaptics

/**
 * 分类设置页面（全屏）：重命名、添加/移除应用、排序方式、列数、缩放、删除
 * 顶部固定标题栏，底部固定按钮，中间内容自由滚动。
 */
@Composable
fun CategorySettingsDialog(
    category: Category,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSnackbar: (String) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val allApps by viewModel.allApps.collectAsState()
    val haptic = rememberHaptics()

    var name by remember { mutableStateOf(category.name) }
    var sortType by remember { mutableStateOf(category.sortType) }
    var columns by remember { mutableStateOf(category.columns) }
    var scale by remember { mutableStateOf(category.scale) }
    var labelScale by remember { mutableStateOf(category.labelScale) }
    var searchQuery by remember { mutableStateOf("") }
    var showTwoLine by remember { mutableStateOf(category.showTwoLine) }
    var appsExpanded by remember { mutableStateOf(false) }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (appsExpanded) 180f else 0f,
        label = "arrow",
    )

    LaunchedEffect(category) {
        name = category.name
        sortType = category.sortType
        columns = category.columns
        scale = category.scale
        labelScale = category.labelScale
        showTwoLine = category.showTwoLine
    }

    val categoryApps = allApps.filter { it.categoryId == category.id }
    val categoryPkgNames = categoryApps.map { it.packageName }.toSet()

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    FullScreenPage(
        title = "分类设置",
        onBack = onDismiss,
        content = {
            // 重命名
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            // 应用管理（默认折叠）
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
                        "应用管理 (${categoryApps.size})",
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
                            val inCategory = app.packageName in categoryPkgNames
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (inCategory) scheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                    .clickable {
                                        haptic()
                                        viewModel.moveToCategory(app, if (inCategory) null else category.id)
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
                                        .background(if (inCategory) scheme.primary else Color.Transparent)
                                        .border(if (inCategory) 0.dp else 2.dp, scheme.outlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (inCategory) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // 排序方式
            LabeledSegment("排序方式", SortType.entries.map { it.displayName() }, sortType.ordinal) {
                sortType = SortType.entries[it]
                viewModel.updateCategorySettings(category.id, sortType = sortType)
            }

            // 列数
            LabeledSegment("列数", (2..6).map { "$it 列" }, columns - 2) {
                columns = it + 2
                viewModel.updateCategorySettings(category.id, columns = columns)
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
                    onValueChangeFinished = { viewModel.updateCategorySettings(category.id, scale = scale) },
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
                    onValueChangeFinished = { viewModel.updateCategorySettings(category.id, labelScale = labelScale) },
                    valueRange = 0.6f..1.5f,
                    steps = 8,
                )
            }

            // 双行应用名称显示
            SwitchRow(
                label = "双行应用名称显示",
                checked = showTwoLine,
                onCheckedChange = {
                    showTwoLine = it
                    viewModel.updateCategorySettings(category.id, showTwoLine = it)
                },
            )
        },
        bottomBar = {
            TextButton(onClick = { haptic(); viewModel.deleteCategory(category.id); onSnackbar("分类已删除"); onDismiss() }) {
                Text("删除分类", color = scheme.error)
            }
            Spacer(Modifier.width(Spacing.sm))
            Button(
                onClick = {
                    haptic()
                    if (name.isNotBlank()) {
                        viewModel.renameCategory(category.id, name)
                        onSnackbar("已保存")
                    }
                    onDismiss()
                },
                shape = StartShapes.pill,
            ) { Text("保存") }
        },
    )
}