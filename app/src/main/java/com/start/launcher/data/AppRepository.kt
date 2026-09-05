package com.start.launcher.data

import android.content.Context
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType
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

    /** 检查是否首次启动（还没有添加任何应用） */
    suspend fun isFirstLaunch(): Boolean = appDao.allPackageNames().isEmpty()
}