package com.start.launcher.ui.main

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.start.launcher.data.AppRepository
import com.start.launcher.data.AppScanner
import com.start.launcher.data.config.parseExportData
import com.start.launcher.data.config.toJson
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
import com.start.launcher.data.settings.SettingsRepository
import com.start.launcher.data.settings.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val scanner = AppScanner(application)
    private val settingsRepo = SettingsRepository(application)
    private val pm = application.packageManager

    init {
        // 启动不再全量扫描：仅当数据库为空（全新安装）时后台扫一次，
        // 保证「选择应用」列表非空；之后由用户手动「刷新应用」。
        // 扫描与图标预热全在 IO 线程，不再拖慢启动。
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (repository.isEmpty()) {
                    repository.refreshInstalledApps(scanner.scanLaunchableApps())
                }
                // 预热分类内应用图标，主界面滚动时命中缓存不发 IO
                val categorizedPkgs = repository.allApps.first()
                    .filter { it.categoryId != null }
                    .map { it.packageName }
                scanner.preloadIcons(categorizedPkgs)
            }
        }
    }

    // ── 数据 ────────────────────────────────────
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allApps: StateFlow<List<AppEntity>> = repository.allApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 主题/界面设置（收藏栏换行等） */
    val themeSettings: StateFlow<ThemeSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettings())

    // ── 搜索 ────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<AppEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allApps
            else repository.searchApps(query)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // ── 刷新应用 ────────────────────────────────
    /** 手动重新扫描已安装应用：新增入库、移除已卸载，回调 (新增数, 移除数) */
    fun refreshApps(onResult: (Int, Int) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val r = repository.refreshInstalledApps(scanner.scanLaunchableApps())
                val categorizedPkgs = repository.allApps.first()
                    .filter { it.categoryId != null }
                    .map { it.packageName }
                scanner.preloadIcons(categorizedPkgs)
                r
            }
            onResult(result.first, result.second)
        }
    }

    // ── 分类操作 ─────────────────────────────────
    suspend fun createCategory(
        name: String,
        appPackageNames: List<String>,
        sortType: SortType = SortType.MANUAL,
        columns: Int = 4,
        scale: Float = 1.0f,
        labelScale: Float = 1.0f,
        showTwoLine: Boolean = false,
    ): Long {
        val id = withContext(Dispatchers.IO) {
            repository.insertCategory(name, sortType, columns, scale, labelScale, showTwoLine)
        }
        appPackageNames.forEach { pkg ->
            val app = allApps.value.find { it.packageName == pkg }
            app?.let { repository.moveToCategory(it.id, id) }
        }
        return id
    }

    fun renameCategory(id: Long, name: String) {
        viewModelScope.launch { repository.renameCategory(id, name) }
    }

    fun updateCategorySettings(id: Long, sortType: SortType? = null, columns: Int? = null, scale: Float? = null, labelScale: Float? = null, showTwoLine: Boolean? = null) {
        viewModelScope.launch { repository.updateCategorySettings(id, sortType, columns, scale, labelScale, showTwoLine) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    // ── 启动 ────────────────────────────────────
    fun launchApp(app: AppEntity) {
        viewModelScope.launch {
            repository.recordLaunch(app.id)
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }
        }
    }

    // ── 应用操作 ────────────────────────────────
    fun removeApp(app: AppEntity) {
        viewModelScope.launch { repository.removeApp(app) }
    }

    fun moveToCategory(app: AppEntity, categoryId: Long?) {
        viewModelScope.launch { repository.moveToCategory(app.id, categoryId) }
    }

    fun moveUp(app: AppEntity, categoryApps: List<AppEntity>) {
        val idx = categoryApps.indexOfFirst { it.id == app.id }
        if (idx <= 0) return
        val newList = categoryApps.toMutableList()
        newList.removeAt(idx)
        newList.add(idx - 1, app)
        viewModelScope.launch {
            newList.forEachIndexed { i, a -> repository.updateSortOrder(a.id, i) }
        }
    }

    fun moveDown(app: AppEntity, categoryApps: List<AppEntity>) {
        val idx = categoryApps.indexOfFirst { it.id == app.id }
        if (idx < 0 || idx >= categoryApps.lastIndex) return
        val newList = categoryApps.toMutableList()
        newList.removeAt(idx)
        newList.add(idx + 1, app)
        viewModelScope.launch {
            newList.forEachIndexed { i, a -> repository.updateSortOrder(a.id, i) }
        }
    }

    fun togglePin(app: AppEntity) {
        viewModelScope.launch { repository.setPinned(app.id, !app.isPinned) }
    }

    // ── 图标 ────────────────────────────────────
    suspend fun loadIconBitmap(packageName: String): Bitmap? {
        return withContext(Dispatchers.IO) { scanner.loadIconBitmap(packageName) }
    }

    // ── 导入/导出配置 ───────────────────────────
    /** 导出配置到用户选择的文件，回调 (成功, 提示) */
    fun exportConfig(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val theme = settingsRepo.getCurrent()
                    val json = repository.exportAll(theme).toJson()
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: return@withContext false to "无法写入文件"
                    true to "配置已导出"
                } catch (e: Exception) {
                    false to "导出失败：${e.message}"
                }
            }
            onResult(result.first, result.second)
        }
    }

    /** 从用户选择的文件导入配置，回调 (成功, 提示) */
    fun importConfig(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = getApplication<Application>().contentResolver
                        .openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return@withContext false to "无法读取文件"
                    val data = parseExportData(json)
                        ?: return@withContext false to "解析失败：不是有效的配置文件"
                    repository.importAll(data)
                    settingsRepo.setAll(data.theme)
                    // 导入后预热图标缓存
                    val categorizedPkgs = repository.allApps.first()
                        .filter { it.categoryId != null }
                        .map { it.packageName }
                    scanner.preloadIcons(categorizedPkgs)
                    true to "配置已导入（${data.categories.size} 个分类）"
                } catch (e: Exception) {
                    false to "导入失败：${e.message}"
                }
            }
            onResult(result.first, result.second)
        }
    }
}