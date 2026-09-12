package com.start.launcher.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.start.launcher.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    /** 获取最大排序号 */
    @Query("SELECT MAX(sort_order) FROM categories")
    suspend fun maxSortOrder(): Int?

    /** 统计分类下应用数 */
    @Query("SELECT COUNT(*) FROM apps WHERE category_id = :categoryId")
    suspend fun appCount(categoryId: Long): Int

    // ── 导入/导出配置 ─────────────────────────

    /** 一次性全量获取（用于导出配置） */
    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    suspend fun getAll(): List<Category>

    /** 清空分类表（导入配置时重建） */
    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}