package com.start.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/**
 * 应用扫描器：在 IO 线程查询可启动应用，同时将图标转为固定尺寸 Bitmap，
 * 避免 Compose 主线程渲染 Drawable 导致的 OOM / ANR。
 * 内置 LRU 缓存，避免重复加载同一图标。
 */
class AppScanner(private val context: Context) {

    private val selfPackage = context.packageName

    /** 图标内存缓存（最多 100 个），避免快速滑动时重复加载导致掉帧 */
    private val iconCache = LruCache<String, Bitmap>(100)

    fun scanLaunchableApps(): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(intent, 0)

        return activities
            .mapNotNull { resolve ->
                val pkg = resolve.activityInfo.packageName
                if (pkg == selfPackage) return@mapNotNull null

                // 在 IO 线程完成 Drawable → Bitmap 转换，固定 96×96 避免 OOM
                val bitmap = resolve.loadIcon(pm).toBitmap(96, 96)
                iconCache.put(pkg, bitmap)

                AppItem(
                    packageName = pkg,
                    appName = resolve.loadLabel(pm).toString(),
                    bitmap = bitmap,
                )
            }
            // 同一应用可能有多个 launcher activity，按包名去重，避免 LazyColumn key 重复崩溃
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
    }

    /** 获取单个应用图标 Bitmap（优先从缓存取） */
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

    data class AppItem(
        val packageName: String,
        val appName: String,
        /** 96×96 固定尺寸 Bitmap，已在 IO 线程生成 */
        val bitmap: Bitmap,
    )
}