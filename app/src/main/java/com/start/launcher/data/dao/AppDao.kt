package com.start.launcher.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.start.launcher.data.model.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    /** 全量观察：按分类排序 */
    @Query("SELECT * FROM apps ORDER BY category_id, sort_order ASC")
    fun observeAll(): Flow<List<AppEntity>>

    /** 按分类查应用 */
    @Query("SELECT * FROM apps WHERE category_id = :categoryId ORDER BY sort_order ASC")
    fun observeByCategory(categoryId: Long): Flow<List<AppEntity>>

    /** 未分类应用 */
    @Query("SELECT * FROM apps WHERE category_id IS NULL ORDER BY sort_order ASC")
    fun observeUncategorized(): Flow<List<AppEntity>>

    /** 常用应用：按启动次数降序，前 10 */
    @Query("SELECT * FROM apps ORDER BY launch_count DESC LIMIT 10")
    fun observeFrequent(): Flow<List<AppEntity>>

    /** 搜索：名称或包名模糊匹配 */
    @Query("SELECT * FROM apps WHERE app_name LIKE '%' || :query || '%' OR package_name LIKE '%' || :query || '%' ORDER BY app_name ASC")
    fun search(query: String): Flow<List<AppEntity>>

    /** 启动计数 +1，更新最后启动时间 */
    @Query("UPDATE apps SET launch_count = launch_count + 1, last_launch_time = :time WHERE id = :appId")
    suspend fun incrementLaunch(appId: Long, time: Long = System.currentTimeMillis())

    /** 移动应用至指定分类 */
    @Query("UPDATE apps SET category_id = :categoryId WHERE id = :appId")
    suspend fun moveToCategory(appId: Long, categoryId: Long?)

    /** 更新排序 */
    @Query("UPDATE apps SET sort_order = :sortOrder WHERE id = :appId")
    suspend fun updateSortOrder(appId: Long, sortOrder: Int)

    /** 切换置顶状态 */
    @Query("UPDATE apps SET is_pinned = :pinned WHERE id = :appId")
    suspend fun setPinned(appId: Long, pinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: AppEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Update
    suspend fun update(app: AppEntity)

    @Delete
    suspend fun delete(app: AppEntity)

    /** 获取分类下最大排序号 */
    @Query("SELECT MAX(sort_order) FROM apps WHERE category_id = :categoryId")
    suspend fun maxSortOrder(categoryId: Long?): Int?

    /** 所有已添加的包名（用于扫描时过滤） */
    @Query("SELECT package_name FROM apps")
    suspend fun allPackageNames(): List<String>

    // ── 导入/导出配置 ─────────────────────────

    /** 一次性全量获取（用于导出配置） */
    @Query("SELECT * FROM apps")
    suspend fun getAllApps(): List<AppEntity>

    /** 按包名删除（刷新时移除已卸载应用） */
    @Query("DELETE FROM apps WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)

    /** 清空应用表（导入配置时重建） */
    @Query("DELETE FROM apps")
    suspend fun clearApps()
}