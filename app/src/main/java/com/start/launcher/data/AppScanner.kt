package com.start.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/**
 * 应用扫描器：在 IO 线程查询可启动应用。
 *
 * 性能设计：
 * - 扫描只取包名/名称（轻量），图标一律按需加载 + 内存缓存，
 *   避免启动时批量转图标造成的卡顿与内存峰值；
 * - 图标缓存 512 项（96×96 约 36KB/张），配合 [preloadIcons] 预热，
 *   主界面滚动时全部命中缓存，不再触发 IO。
 */
class AppScanner(private val context: Context) {

    private val selfPackage = context.packageName

    /** 图标内存缓存，避免重复加载导致掉帧 */
    private val iconCache = LruCache<String, Bitmap>(512)

    /** 扫描设备上可启动应用（不含自身），按名称排序 */
    fun scanLaunchableApps(): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = context.packageManager

        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == selfPackage) return@mapNotNull null
                AppItem(
                    packageName = pkg,
                    appName = resolve.loadLabel(pm).toString(),
                )
            }
            // 同一应用可能有多个 launcher activity，按包名去重，避免 LazyColumn key 重复崩溃
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
    }

    /** 获取单个应用图标 Bitmap（优先从缓存取，IO 线程调用） */
    fun loadIconBitmap(packageName: String): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        return try {
            val bmp = context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96)
            iconCache.put(packageName, bmp)
            bmp
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 后台预热图标缓存（IO 线程调用）。
     * 只预热分类内常用应用，上限 300 张控制内存。
     */
    fun preloadIcons(packageNames: List<String>) {
        packageNames.take(300).forEach { loadIconBitmap(it) }
    }

    data class AppItem(
        val packageName: String,
        val appName: String,
    )
}