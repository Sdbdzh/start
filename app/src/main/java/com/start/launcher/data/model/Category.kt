package com.start.launcher.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 分类内应用排序方式 */
enum class SortType {
    /** 手动排序 */
    MANUAL,
    /** 按名称字母 */
    NAME,
    /** 按使用频率降序 */
    FREQUENCY,
    /** 按最近使用 */
    RECENT,
}

/** 分类表 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 分类名称，如"社交""游戏""工具" */
    val name: String,

    /** 分类整体排序序号，越小越靠前 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** 分类内应用排序方式 */
    @ColumnInfo(name = "sort_type")
    val sortType: SortType = SortType.MANUAL,

    /** 列数（每行应用数），2~6 */
    @ColumnInfo(name = "columns")
    val columns: Int = 4,

    /** 缩放系数，0.6~1.5 */
    @ColumnInfo(name = "scale")
    val scale: Float = 1.0f,

    /** 应用名字缩放系数，0.6~1.5 */
    @ColumnInfo(name = "label_scale")
    val labelScale: Float = 1.0f,

    /** 应用名双行显示（否则单行截断） */
    @ColumnInfo(name = "show_two_line")
    val showTwoLine: Boolean = false,

    /** 系统预置分类，不可删除 */
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,
)