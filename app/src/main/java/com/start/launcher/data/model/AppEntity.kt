package com.start.launcher.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 应用表：用户挑选加入启动面板的第三方应用 */
@Entity(
    tableName = "apps",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id"), Index("package_name", unique = true)],
)
data class AppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 包名，唯一标识，用于启动 Intent */
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** 应用显示名称 */
    @ColumnInfo(name = "app_name")
    val appName: String,

    /** 所属分类 ID，null = 未分类 */
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    /** 该分类内排序序号，越小越靠前 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** 累计启动次数，用于"常用"智能分类 */
    @ColumnInfo(name = "launch_count")
    val launchCount: Int = 0,

    /** 最近一次启动时间戳（毫秒），用于"常用"排序 */
    @ColumnInfo(name = "last_launch_time")
    val lastLaunchTime: Long = 0,

    /** 是否置顶 */
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
)