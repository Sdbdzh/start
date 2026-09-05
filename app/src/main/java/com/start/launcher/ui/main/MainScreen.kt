package com.start.launcher.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
import com.start.launcher.theme.Motion
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val scheme = MaterialTheme.colorScheme

    var selectedTabId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var settingsCategoryId by remember { mutableStateOf<Long?>(null) }
    var showThemeSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showSnackbar(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // 任意弹窗打开时，返回键关闭弹窗而不是退出
    val anyDialogOpen = showCreateDialog || settingsCategoryId != null || showThemeSettings
    BackHandler(enabled = anyDialogOpen) {
        when {
            showThemeSettings -> showThemeSettings = false
            settingsCategoryId != null -> settingsCategoryId = null
            showCreateDialog -> showCreateDialog = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部栏：搜索 + 设置 ──────────────────
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("搜索应用...", color = scheme.onSurfaceVariant) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { }),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    // 设置按钮
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, scheme.outlineVariant, CircleShape)
                            .clickable { showThemeSettings = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置", tint = scheme.onSurfaceVariant)
                    }
                }
            }

            if (searchQuery.isNotBlank()) {
                SearchResultsList(
                    apps = searchResults,
                    viewModel = viewModel,
                    onLaunch = { viewModel.launchApp(it) },
                )
            } else {
                // ── 分类胶囊页签 ──────────────────────
                if (categories.isNotEmpty()) {
                    CapsuleTabRow(
                        categories = categories,
                        selectedId = selectedTabId,
                        onSelect = { selectedTabId = it },
                        onCreateClick = { showCreateDialog = true },
                    )
                    Spacer(Modifier.height(Spacing.md))
                }

                // ── 内容区 ─────────────────────────────
                when {
                    categories.isEmpty() -> {
                        EmptyState(onCreateClick = { showCreateDialog = true })
                    }
                    selectedTabId == null -> {
                        LaunchedEffect(categories) {
                            if (categories.isNotEmpty() && selectedTabId == null) {
                                selectedTabId = categories.first().id
                            }
                        }
                    }
                    else -> {
                        val category = categories.find { it.id == selectedTabId }
                        if (category != null) {
                            val categoryApps = remember(allApps, category.id, category.sortType) {
                                sortedApps(allApps.filter { it.categoryId == category.id }, category.sortType)
                            }
                            CategoryCard(
                                category = category,
                                apps = categoryApps,
                                viewModel = viewModel,
                                onSettingsClick = { settingsCategoryId = category.id },
                                onSnackbar = { msg -> showSnackbar(msg) },
                            )
                        }
                    }
                }
            }
        }

        // ── Snackbar ────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.xl),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = scheme.surfaceContainerHighest,
                contentColor = scheme.onSurface,
            )
        }
    }

    // ── 创建分类对话框 ─────────────────────────
    if (showCreateDialog) {
        CreateCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
            onCreated = { id ->
                showCreateDialog = false
                selectedTabId = id
                showSnackbar("分类已创建")
            },
        )
    }

    // ── 分类设置对话框 ──────────────────────────
    settingsCategoryId?.let { id ->
        categories.find { it.id == id }?.let { cat ->
            CategorySettingsDialog(
                category = cat,
                viewModel = viewModel,
                onDismiss = { settingsCategoryId = null },
                onSnackbar = { msg -> showSnackbar(msg) },
            )
        }
    }

    // ── 主题设置对话框 ──────────────────────────
    if (showThemeSettings) {
        ThemeSettingsDialog(onDismiss = { showThemeSettings = false })
    }
}

/** 按分类的排序方式排序应用，置顶始终优先 */
private fun sortedApps(apps: List<AppEntity>, sortType: SortType): List<AppEntity> {
    val (pinned, normal) = apps.partition { it.isPinned }
    val sortedNormal = when (sortType) {
        SortType.MANUAL -> normal.sortedBy { it.sortOrder }
        SortType.NAME -> normal.sortedBy { it.appName }
        SortType.FREQUENCY -> normal.sortedByDescending { it.launchCount }
        SortType.RECENT -> normal.sortedByDescending { it.lastLaunchTime }
    }
    val sortedPinned = when (sortType) {
        SortType.MANUAL -> pinned.sortedBy { it.sortOrder }
        SortType.NAME -> pinned.sortedBy { it.appName }
        SortType.FREQUENCY -> pinned.sortedByDescending { it.launchCount }
        SortType.RECENT -> pinned.sortedByDescending { it.lastLaunchTime }
    }
    return sortedPinned + sortedNormal
}

// ── 空状态 ───────────────────────────────────────────────────────
@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("START", fontSize = 44.sp, fontWeight = FontWeight.Black, color = scheme.primary)
            Spacer(Modifier.height(Spacing.xl))
            Text("还没有分类", fontSize = 16.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xxxl))
            Box(
                modifier = Modifier
                    .clip(StartShapes.pill)
                    .background(scheme.primary)
                    .clickable { onCreateClick() }
                    .padding(horizontal = Spacing.xxxl, vertical = Spacing.lg),
            ) {
                Text("创建分类", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
            }
        }
    }
}

// ── 胶囊页签行 ───────────────────────────────────────────────────
@Composable
private fun CapsuleTabRow(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onCreateClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(categories, key = { it.id }) { cat ->
            CapsuleTab(
                label = cat.name,
                selected = selectedId == cat.id,
                onClick = { onSelect(cat.id) },
                scheme = scheme,
            )
        }
        item {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.dp, scheme.outlineVariant, CircleShape)
                    .clickable { onCreateClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加分类", tint = scheme.primary)
            }
        }
    }
}

@Composable
private fun CapsuleTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.surfaceContainerHigh,
        animationSpec = Motion.springColor,
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = Motion.springColor,
    )

    Box(
        modifier = Modifier
            .clip(StartShapes.pill)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

// ── 分类卡片 ──────────────────────────────────────────────────────
@Composable
private fun CategoryCard(
    category: Category,
    apps: List<AppEntity>,
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    onSnackbar: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
    ) {
        // 分类标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = category.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    text = "${apps.size} 个应用",
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, scheme.outlineVariant, CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "分类设置", tint = scheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("此分类还没有应用", color = scheme.onSurfaceVariant, fontSize = 15.sp)
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "点击右上角设置按钮添加",
                        color = scheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onSettingsClick() },
                    )
                }
            }
        } else {
            // 应用网格，使用分类的列数、图标缩放、文字缩放
            val baseIconSize = 72.dp
            val baseLabelSize = 14.sp
            val iconSize = baseIconSize * category.scale
            val labelSize = baseLabelSize * category.labelScale

            LazyVerticalGrid(
                columns = GridCells.Fixed(category.columns.coerceIn(2, 6)),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                contentPadding = PaddingValues(vertical = Spacing.sm),
            ) {
                items(apps, key = { it.id }) { app ->
                    AppGridItem(
                        app = app,
                        viewModel = viewModel,
                        categoryApps = apps,
                        iconSize = iconSize,
                        labelSize = labelSize,
                        showTwoLine = category.showTwoLine,
                        onSnackbar = onSnackbar,
                    )
                }
            }
        }
    }
}

// ── 应用网格项 ────────────────────────────────────────────────────
@Composable
private fun AppGridItem(
    app: AppEntity,
    viewModel: MainViewModel,
    categoryApps: List<AppEntity>,
    iconSize: androidx.compose.ui.unit.Dp,
    labelSize: androidx.compose.ui.unit.TextUnit,
    showTwoLine: Boolean = false,
    onSnackbar: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var showMenu by remember { mutableStateOf(false) }
    var iconBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        val bmp = viewModel.loadIconBitmap(app.packageName)
        iconBitmap = bmp?.asImageBitmap()
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StartShapes.card)
                .combinedClickable(
                    onClick = { viewModel.launchApp(app) },
                    onLongClick = { showMenu = true },
                )
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(iconSize + 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = iconBitmap
                if (bmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp,
                        contentDescription = app.appName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(iconSize).clip(StartShapes.icon),
                    )
                } else {
                    Box(
                        Modifier.size(iconSize).clip(StartShapes.icon).background(scheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            app.appName.take(1),
                            fontSize = labelSize * 1.5f,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xs))
            // 置顶应用用莫奈主色强调
            Text(
                text = app.appName,
                fontSize = labelSize,
                fontWeight = if (app.isPinned) FontWeight.Bold else FontWeight.Medium,
                color = if (app.isPinned) scheme.primary else scheme.onSurface,
                maxLines = if (showTwoLine) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (app.isPinned) "取消置顶" else "置顶") },
                onClick = {
                    viewModel.togglePin(app)
                    onSnackbar(if (app.isPinned) "已取消置顶" else "已置顶")
                    showMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text("上移") },
                onClick = { viewModel.moveUp(app, categoryApps); onSnackbar("已上移"); showMenu = false },
            )
            DropdownMenuItem(
                text = { Text("下移") },
                onClick = { viewModel.moveDown(app, categoryApps); onSnackbar("已下移"); showMenu = false },
            )
            DropdownMenuItem(
                text = { Text("移除") },
                onClick = { viewModel.removeApp(app); onSnackbar("已移除"); showMenu = false },
            )
        }
    }
}

// ── 搜索结果列表 ──────────────────────────────────────────────────
@Composable
private fun SearchResultsList(
    apps: List<AppEntity>,
    viewModel: MainViewModel,
    onLaunch: (AppEntity) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(Spacing.xxxl), contentAlignment = Alignment.Center) {
            Text("没有找到匹配的应用", color = scheme.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(apps, key = { it.id }) { app ->
            var iconBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
            LaunchedEffect(app.packageName) {
                val bmp = viewModel.loadIconBitmap(app.packageName)
                iconBitmap = bmp?.asImageBitmap()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onLaunch(app) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bmp = iconBitmap
                if (bmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(40.dp).clip(StartShapes.icon),
                    )
                }
                Spacer(Modifier.width(Spacing.lg))
                Text(
                    text = app.appName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text("${app.launchCount}次", fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
        }
    }
}