package com.start.launcher.data

import android.content.Context
import androidx.room.withTransaction
import com.start.launcher.data.config.ExportApp
import com.start.launcher.data.config.ExportCategory
import com.start.launcher.data.config.ExportData
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
import com.start.launcher.data.settings.ThemeSettings
import kotlinx.coroutines.flow.Flow

/**
 * 统一数据仓库：封装 Room 操作，UI 层只通过此类访问数据
 */
class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val categoryDao = db.categoryDao()
    private val appDao = db.appDao()

    // ── 分类 ────────────────────────────────────

    /** 全部分类 */
    val categories: Flow<List<Category>> = categoryDao.observeAll()

    suspend fun insertCategory(
        name: String,
        sortType: SortType = SortType.MANUAL,
        columns: Int = 4,
        scale: Float = 1.0f,
        labelScale: Float = 1.0f,
        showTwoLine: Boolean = false,
    ): Long {
        val order = (categoryDao.maxSortOrder() ?: -1) + 1
        return categoryDao.insert(
            Category(
                name = name,
                sortOrder = order,
                sortType = sortType,
                columns = columns,
                scale = scale,
                labelScale = labelScale,
                showTwoLine = showTwoLine,
            )
        )
    }

    suspend fun renameCategory(id: Long, name: String) {
        categoryDao.getById(id)?.let { categoryDao.update(it.copy(name = name)) }
    }

    /** 更新分类设置：排序方式/列数/缩放 */
    suspend fun updateCategorySettings(
        id: Long,
        sortType: SortType? = null,
        columns: Int? = null,
        scale: Float? = null,
        labelScale: Float? = null,
        showTwoLine: Boolean? = null,
    ) {
        val cat = categoryDao.getById(id) ?: return
        categoryDao.update(
            cat.copy(
                sortType = sortType ?: cat.sortType,
                columns = columns ?: cat.columns,
                scale = scale ?: cat.scale,
                labelScale = labelScale ?: cat.labelScale,
                showTwoLine = showTwoLine ?: cat.showTwoLine,
            )
        )
    }

    suspend fun deleteCategory(id: Long) {
        categoryDao.delete(Category(id = id, name = ""))
    }

    suspend fun categoryAppCount(id: Long): Int = categoryDao.appCount(id)

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getById(id)

    // ── 应用 ─────────────────────────────────────

    /** 全量应用 */
    val allApps: Flow<List<AppEntity>> = appDao.observeAll()

    /** 按分类查应用 */
    fun appsByCategory(categoryId: Long): Flow<List<AppEntity>> =
        appDao.observeByCategory(categoryId)

    /** 未分类应用 */
    val uncategorizedApps: Flow<List<AppEntity>> = appDao.observeUncategorized()

    /** 搜索应用 */
    fun searchApps(query: String): Flow<List<AppEntity>> = appDao.search(query)

    /** 记录启动 */
    suspend fun recordLaunch(appId: Long) = appDao.incrementLaunch(appId)

    /** 移动至分类 */
    suspend fun moveToCategory(appId: Long, categoryId: Long?) =
        appDao.moveToCategory(appId, categoryId)

    /** 更新排序 */
    suspend fun updateSortOrder(appId: Long, sortOrder: Int) =
        appDao.updateSortOrder(appId, sortOrder)

    /** 置顶/取消置顶 */
    suspend fun setPinned(appId: Long, pinned: Boolean) = appDao.setPinned(appId, pinned)

    /** 批量添加应用 */
    suspend fun insertApps(apps: List<AppEntity>) = appDao.insertAll(apps)

    /** 删除应用 */
    suspend fun removeApp(app: AppEntity) = appDao.delete(app)

    /** 已添加的包名集合 */
    suspend fun addedPackageNames(): Set<String> = appDao.allPackageNames().toSet()

    /** 数据库是否为空（尚未扫描过任何应用） */
    suspend fun isEmpty(): Boolean = appDao.allPackageNames().isEmpty()

    /**
     * 刷新已安装应用：新增的应用入库，已卸载的应用移除。
     * 返回 (新增数量, 移除数量)。
     */
    suspend fun refreshInstalledApps(installed: List<AppScanner.AppItem>): Pair<Int, Int> {
        val existing = appDao.allPackageNames()
        val existingSet = existing.toSet()
        val installedPkgs = installed.map { it.packageName }.toSet()

        val newApps = installed
            .filter { it.packageName !in existingSet }
            .map { AppEntity(packageName = it.packageName, appName = it.appName) }
        if (newApps.isNotEmpty()) appDao.insertAll(newApps)

        existing.filter { it !in installedPkgs }.forEach { appDao.deleteByPackage(it) }

        return newApps.size to existing.count { it !in installedPkgs }
    }

    // ── 导入/导出配置 ──────────────────────────

    /** 导出全量数据：分类 + 分类内应用 + 未分类应用不计（配置只含用户组织的内容） */
    suspend fun exportAll(theme: ThemeSettings): ExportData {
        val categories = categoryDao.getAll()
        val apps = appDao.getAllApps()
        val byCategory = apps.groupBy { it.categoryId }

        val exportCategories = categories.map { c ->
            val catApps = (byCategory[c.id] ?: emptyList()).map { a ->
                ExportApp(
                    packageName = a.packageName,
                    appName = a.appName,
                    sortOrder = a.sortOrder,
                    isPinned = a.isPinned,
                    launchCount = a.launchCount,
                    lastLaunchTime = a.lastLaunchTime,
                )
            }
            ExportCategory(
                name = c.name,
                sortOrder = c.sortOrder,
                sortType = c.sortType,
                columns = c.columns,
                scale = c.scale,
                labelScale = c.labelScale,
                showTwoLine = c.showTwoLine,
                isSystem = c.isSystem,
                apps = catApps,
            )
        }
        return ExportData(categories = exportCategories, theme = theme)
    }

    /** 导入配置：清空现有数据后重建分类与归属关系 */
    suspend fun importAll(data: ExportData) {
        db.withTransaction {
            // 先删应用再删分类（应用外键引用分类）
            appDao.clearApps()
            categoryDao.clearCategories()

            data.categories.forEach { c ->
                val categoryId = categoryDao.insert(
                    Category(
                        name = c.name,
                        sortOrder = c.sortOrder,
                        sortType = c.sortType,
                        columns = c.columns,
                        scale = c.scale,
                        labelScale = c.labelScale,
                        showTwoLine = c.showTwoLine,
                        isSystem = c.isSystem,
                    )
                )
                // 分类 id 可能为 0 说明插入冲突（不应发生），跳过其应用
                if (categoryId > 0 && c.apps.isNotEmpty()) {
                    appDao.insertAll(
                        c.apps.map { a ->
                            AppEntity(
                                packageName = a.packageName,
                                appName = a.appName,
                                categoryId = categoryId,
                                sortOrder = a.sortOrder,
                                isPinned = a.isPinned,
                                launchCount = a.launchCount,
                                lastLaunchTime = a.lastLaunchTime,
                            )
                        }
                    )
                }
            }
        }
    }
}