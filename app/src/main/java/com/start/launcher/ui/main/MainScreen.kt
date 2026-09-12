package com.start.launcher.ui.main

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.start.launcher.data.BackgroundStore
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
import com.start.launcher.data.settings.BgScaleType
import com.start.launcher.data.settings.ThemeSettings
import com.start.launcher.theme.Motion
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes
import com.start.launcher.ui.common.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val themeSettings by viewModel.themeSettings.collectAsState()
    val dataReady by viewModel.dataReady.collectAsState()
    val bgVersion by viewModel.bgVersion.collectAsState()
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()

    var selectedTabId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var settingsCategoryId by remember { mutableStateOf<Long?>(null) }
    var showThemeSettings by remember { mutableStateOf(false) }
    var settingsMenuExpanded by remember { mutableStateOf(false) }
    var deleteConfirmCategory by remember { mutableStateOf<Category?>(null) }

    // 顶部面板矩形（窗口坐标）：毛玻璃覆盖层的裁剪区域
    var topPanelRect by remember { mutableStateOf(Rect.Zero) }

    // 背景位图加载（提升到此处，背景层与毛玻璃面板共用同一张图）
    val context = LocalContext.current
    var bgBitmap by remember(themeSettings.bgEnabled) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(themeSettings.bgEnabled, bgVersion) {
        bgBitmap = if (themeSettings.bgEnabled) {
            withContext(Dispatchers.IO) {
                BackgroundStore.file(context).takeIf { it.exists() }
                    ?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
            }
        } else {
            null
        }
    }
    val bgBmp = bgBitmap
    val searching = searchQuery.isNotBlank()

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

    // ── 顶栏内容：搜索框 + 合并的设置入口（分类设置/应用设置） ──
    val topBarContent: @Composable RowScope.() -> Unit = {
        if (dataReady && categories.isNotEmpty() && !themeSettings.hideSearchBar) {
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
        }
        // 统一设置入口：弹出菜单选择「分类设置」或「应用设置」
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, scheme.outlineVariant, CircleShape)
                    .clickable { haptic(); settingsMenuExpanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "设置", tint = scheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = settingsMenuExpanded,
                onDismissRequest = { settingsMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("分类设置") },
                    enabled = categories.isNotEmpty(),
                    onClick = {
                        settingsMenuExpanded = false
                        val target = selectedTabId ?: categories.firstOrNull()?.id
                        if (target != null) settingsCategoryId = target
                    },
                )
                DropdownMenuItem(
                    text = { Text("应用设置") },
                    onClick = {
                        settingsMenuExpanded = false
                        showThemeSettings = true
                    },
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface),
    ) {
        // ── 自定义背景层：铺满全屏，营造沉浸感 ──
        BackgroundLayer(bitmap = bgBmp, settings = themeSettings, modifier = Modifier.fillMaxSize())

        // ── 毛玻璃覆盖层：与主背景结构级对齐（同 bitmap/同 ContentScale/同 fillMaxSize），圆角裁剪到面板区域 ──
        bgBmp?.let {
            GlassOverlay(
                bitmap = it,
                rect = topPanelRect,
                cornerRadiusDp = 20f,
                scaleType = bgContentScale(themeSettings.bgScaleType),
                blurTotal = themeSettings.bgBlur + themeSettings.panelBlur,
                bgBrightness = themeSettings.bgBrightness,
                panelOpacity = themeSettings.panelOpacity,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── 顶部区域：顶栏 + 收藏栏连成一整块（背景模式下共用一块毛玻璃面板） ──
                if (bgBmp != null) {
                    // 面板本体：透明占位 + 上报矩形位置；毛玻璃视觉由根级 GlassOverlay 绘制
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                            .onGloballyPositioned {
                                topPanelRect = Rect(it.positionInRoot(), it.size.toSize())
                            },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                topBarContent()
                            }
                            // 收藏栏：搜索时收起，面板高度随之动画收缩
                            AnimatedVisibility(
                                visible = !searching && dataReady && categories.isNotEmpty(),
                                enter = fadeIn(tween(200)) +
                                    expandVertically(tween(240, easing = FastOutSlowInEasing)),
                                exit = fadeOut(tween(150)) +
                                    shrinkVertically(tween(200, easing = FastOutSlowInEasing)),
                            ) {
                                Column(modifier = Modifier.padding(bottom = Spacing.sm)) {
                                    CapsuleTabRow(
                                        categories = categories,
                                        selectedId = selectedTabId,
                                        multiLayer = themeSettings.tabMultiLayer,
                                        onSelect = { selectedTabId = it },
                                        onCreateClick = { showCreateDialog = true },
                                        onEdit = { settingsCategoryId = it.id },
                                        onDelete = { deleteConfirmCategory = it },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        topBarContent()
                    }
                    AnimatedVisibility(
                        visible = !searching && dataReady && categories.isNotEmpty(),
                        enter = fadeIn(tween(200)) +
                            expandVertically(tween(240, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(150)) +
                            shrinkVertically(tween(200, easing = FastOutSlowInEasing)),
                    ) {
                        Column(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md)) {
                            CapsuleTabRow(
                                categories = categories,
                                selectedId = selectedTabId,
                                multiLayer = themeSettings.tabMultiLayer,
                                onSelect = { selectedTabId = it },
                                onCreateClick = { showCreateDialog = true },
                                onEdit = { settingsCategoryId = it.id },
                                onDelete = { deleteConfirmCategory = it },
                            )
                        }
                    }
                }

                // ── 内容区 ─────────────────────────────
                Crossfade(
                    targetState = searching,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "mainContent",
                ) { s ->
                    if (s) {
                        SearchResultsList(
                            apps = searchResults,
                            viewModel = viewModel,
                            onLaunch = { viewModel.launchApp(it) },
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            when {
                                // 数据未就绪：显示加载态，避免空状态在启动瞬间闪现
                                !dataReady -> {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
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
                                            onSnackbar = { msg -> showSnackbar(msg) },
                                        )
                                    } else {
                                        // 选中分类已不存在（如导入配置后），重新选中第一个
                                        LaunchedEffect(categories) {
                                            if (categories.isNotEmpty()) selectedTabId = categories.first().id
                                        }
                                    }
                                }
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

        // ── 创建分类对话框（底部滑入过渡） ──────────
        AnimatedVisibility(
            visible = showCreateDialog,
            enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(180)),
        ) {
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

        // ── 分类设置对话框（底部滑入过渡） ───────────
        val settingsCategory = settingsCategoryId?.let { id -> categories.find { it.id == id } }
        var lastSettingsCategory by remember { mutableStateOf<Category?>(null) }
        LaunchedEffect(settingsCategory) {
            if (settingsCategory != null) lastSettingsCategory = settingsCategory
        }
        AnimatedVisibility(
            visible = settingsCategory != null,
            enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(180)),
        ) {
            lastSettingsCategory?.let { cat ->
                CategorySettingsDialog(
                    category = cat,
                    viewModel = viewModel,
                    onDismiss = { settingsCategoryId = null },
                    onSnackbar = { msg -> showSnackbar(msg) },
                )
            }
        }

        // ── 主题设置对话框（中心缩放过渡） ───────────
        AnimatedVisibility(
            visible = showThemeSettings,
            enter = fadeIn(tween(200)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(280, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(160)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(200)),
        ) {
            ThemeSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showThemeSettings = false },
                onRefreshApps = {
                    showThemeSettings = false
                    viewModel.refreshApps { added, removed ->
                        showSnackbar("已刷新：新增 $added 个，移除 $removed 个应用")
                    }
                },
                onSnackbar = { msg -> showSnackbar(msg) },
            )
        }

        // ── 删除分类确认 ─────────────────────────────
        deleteConfirmCategory?.let { cat ->
            AlertDialog(
                onDismissRequest = { deleteConfirmCategory = null },
                title = { Text("删除分类") },
                text = { Text("确定删除「${cat.name}」吗？分类内的应用不会被卸载，仅从此分类移除。") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteConfirmCategory = null
                        if (selectedTabId == cat.id) selectedTabId = null
                        viewModel.deleteCategory(cat.id)
                        showSnackbar("分类「${cat.name}」已删除")
                    }) { Text("删除", color = scheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmCategory = null }) { Text("取消") }
                },
            )
        }
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

// ── 主页面背景层 ──────────────────────────────────────────────────
/**
 * 自定义背景：私有目录里的图片 + 亮度遮罩 + 模糊。
 * 位图由 MainScreen 加载传入（背景层与毛玻璃面板共用同一张图）。
 */
@Composable
private fun BackgroundLayer(bitmap: ImageBitmap?, settings: ThemeSettings, modifier: Modifier = Modifier) {
    val bmp = bitmap ?: return
    Box(modifier) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = bgContentScale(settings.bgScaleType),
            modifier = Modifier
                .fillMaxSize()
                .then(if (settings.bgBlur > 0f) Modifier.blur(settings.bgBlur.dp) else Modifier),
        )
        // 亮度遮罩：亮度越低遮罩越重，保证前景内容可读
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - settings.bgBrightness).coerceIn(0f, 1f))),
        )
    }
}

private fun bgContentScale(type: BgScaleType): ContentScale = when (type) {
    BgScaleType.FILL_BOUNDS -> ContentScale.FillBounds
    BgScaleType.FIT -> ContentScale.Fit
    BgScaleType.CROP -> ContentScale.Crop
}

// ── 毛玻璃覆盖层 ──────────────────────────────────────────────────
/**
 * 背景图模式下的毛玻璃覆盖：绘制在根布局坐标系，与主背景是结构完全相同的
 * 兄弟节点（同 bitmap / 同 ContentScale / 同 fillMaxSize），因此像素级对齐、
 * 永不错位；再用圆角矩形 Path 把整层裁剪到面板矩形（rect 由面板本体上报）。
 * 模糊 = 主背景模糊(bgBlur) + 面板模糊(panelBlur)，玻璃感叠加在背景之上。
 */
@Composable
private fun GlassOverlay(
    bitmap: ImageBitmap,
    rect: Rect,
    cornerRadiusDp: Float,
    scaleType: ContentScale,
    blurTotal: Float,
    bgBrightness: Float,
    panelOpacity: Float,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    // 面板矩形未就绪时不绘制（onGloballyPositioned 在首帧绘制前触发，无闪烁）
    if (rect.width <= 0f || rect.height <= 0f) return

    val clipPath = remember(rect, cornerRadiusDp, density) {
        val r = with(density) { cornerRadiusDp.dp.toPx() }
        Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    topLeft = CornerRadius(r, r),
                    topRight = CornerRadius(r, r),
                    bottomLeft = CornerRadius(r, r),
                    bottomRight = CornerRadius(r, r),
                ),
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                clipPath(clipPath) { this@drawWithContent.drawContent() }
            },
    ) {
        // 与主背景结构完全一致的全屏图 → 天然对齐
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = scaleType,
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurTotal > 0f) Modifier.blur(blurTotal.dp) else Modifier),
        )
        // 亮度遮罩（与主背景一致，保证面板内图像亮度与周围相同）
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - bgBrightness).coerceIn(0f, 1f))),
        )
        // 面板底色：不透明度可调
        Box(
            Modifier
                .fillMaxSize()
                .background(scheme.surface.copy(alpha = panelOpacity.coerceIn(0f, 1f))),
        )
    }
}

// ── 空状态 ───────────────────────────────────────────────────────
@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
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
                    .clickable { haptic(); onCreateClick() }
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
    multiLayer: Boolean,
    onSelect: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    if (multiLayer) {
        // 多层显示：按钮自动换行排列
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            categories.forEach { cat ->
                CapsuleTabWithMenu(
                    category = cat,
                    selected = selectedId == cat.id,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    scheme = scheme,
                )
            }
            AddCategoryButton(onCreateClick = onCreateClick, scheme = scheme)
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(categories, key = { it.id }) { cat ->
                CapsuleTabWithMenu(
                    category = cat,
                    selected = selectedId == cat.id,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    scheme = scheme,
                )
            }
            item { AddCategoryButton(onCreateClick = onCreateClick, scheme = scheme) }
        }
    }
}

/** 分类胶囊 + 长按弹出菜单（编辑 / 删除） */
@Composable
private fun CapsuleTabWithMenu(
    category: Category,
    selected: Boolean,
    onSelect: (Long) -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    scheme: androidx.compose.material3.ColorScheme,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val haptic = rememberHaptics()

    Box {
        CapsuleTab(
            label = category.name,
            selected = selected,
            onClick = { onSelect(category.id) },
            onLongClick = { haptic(); menuExpanded = true },
            scheme = scheme,
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEdit(category)
                },
            )
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onDelete(category)
                },
            )
        }
    }
}

@Composable
private fun AddCategoryButton(onCreateClick: () -> Unit, scheme: androidx.compose.material3.ColorScheme) {
    val haptic = rememberHaptics()
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .clickable { haptic(); onCreateClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "添加分类", tint = scheme.primary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CapsuleTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
    val haptic = rememberHaptics()

    Box(
        modifier = Modifier
            .clip(StartShapes.pill)
            .background(bgColor)
            .combinedClickable(
                onClick = { haptic(); onClick() },
                onLongClick = onLongClick,
            )
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
    onSnackbar: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
    ) {
        // 分类标题行
        Column(modifier = Modifier.fillMaxWidth()) {
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

        Spacer(Modifier.height(Spacing.lg))

        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("此分类还没有应用", color = scheme.onSurfaceVariant, fontSize = 15.sp)
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "点击顶部齿轮 → 分类设置，添加应用",
                        color = scheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else {
            // 应用网格，使用分类的列数、图标缩放、文字缩放
            val baseIconSize = 72.dp
            val baseLabelSize = 14.sp
            val iconSize = baseIconSize * category.scale
            val labelSize = baseLabelSize * category.labelScale

            val columns = category.columns.coerceIn(2, 6)
            val gridState = remember(category.id) { LazyGridState() }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                contentPadding = PaddingValues(vertical = Spacing.sm, horizontal = 6.dp),
            ) {
                itemsIndexed(apps, key = { _, it -> it.id }) { index, app ->
                    AppGridItem(
                        app = app,
                        viewModel = viewModel,
                        categoryApps = apps,
                        iconSize = iconSize,
                        labelSize = labelSize,
                        showTwoLine = category.showTwoLine,
                        onSnackbar = onSnackbar,
                        index = index,
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
    index: Int,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()
    var showMenu by remember { mutableStateOf(false) }
    var iconBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        val bmp = viewModel.loadIconBitmap(app.packageName)
        iconBitmap = bmp?.asImageBitmap()
    }

    // 入场过渡：按序号错开，图标由不可见缓缓到可见
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index % 14) * 26L)
        entered = true
    }
    val itemAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "itemAlpha",
    )
    val itemScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "itemScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (iconBitmap != null) 1f else 0f,
        animationSpec = tween(500),
        label = "iconAlpha",
    )

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StartShapes.card)
                .combinedClickable(
                    onClick = { haptic(); viewModel.launchApp(app) },
                    onLongClick = { haptic(); showMenu = true },
                )
                .graphicsLayer {
                    alpha = itemAlpha
                    scaleX = itemScale
                    scaleY = itemScale
                }
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(iconSize + 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = iconBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = app.appName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(StartShapes.icon)
                            .graphicsLayer { alpha = iconAlpha },
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
    val haptic = rememberHaptics()

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
                    .clickable { haptic(); onLaunch(app) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bmp = iconBitmap
                if (bmp != null) {
                    Image(
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