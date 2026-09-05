package com.start.launcher.ui.main

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.start.launcher.data.AppRepository
import com.start.launcher.data.AppScanner
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val scanner = AppScanner(application)
    private val pm = application.packageManager

    init {
        // 启动时扫描系统可启动应用并增量入库（未分类），
        // 保证「选择应用」「应用管理」能看到设备上的应用。
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val existing = repository.addedPackageNames()
                val newApps = scanner.scanLaunchableApps()
                    .filter { it.packageName !in existing }
                    .map { AppEntity(packageName = it.packageName, appName = it.appName) }
                if (newApps.isNotEmpty()) repository.insertApps(newApps)
            }
        }
    }

    // ── 数据 ────────────────────────────────────
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allApps: StateFlow<List<AppEntity>> = repository.allApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
}