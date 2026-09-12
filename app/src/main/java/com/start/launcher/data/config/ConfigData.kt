package com.start.launcher.data.config

import com.start.launcher.data.model.SortType
import com.start.launcher.data.settings.ColorSource
import com.start.launcher.data.settings.DarkStyle
import com.start.launcher.data.settings.HapticIntensity
import com.start.launcher.data.settings.ThemeMode
import com.start.launcher.data.settings.ThemeSettings
import org.json.JSONArray
import org.json.JSONObject

/**
 * 配置导入/导出数据模型 + JSON 序列化。
 * 覆盖范围：分类（含排序/列数/缩放等设置）、分类内应用（含置顶/启动统计）、主题设置。
 */
data class ExportData(
    val version: Int = 1,
    val categories: List<ExportCategory>,
    val theme: ThemeSettings,
)

data class ExportCategory(
    val name: String,
    val sortOrder: Int,
    val sortType: SortType,
    val columns: Int,
    val scale: Float,
    val labelScale: Float,
    val showTwoLine: Boolean,
    val isSystem: Boolean,
    val apps: List<ExportApp>,
)

data class ExportApp(
    val packageName: String,
    val appName: String,
    val sortOrder: Int,
    val isPinned: Boolean,
    val launchCount: Int,
    val lastLaunchTime: Long,
)

/** 序列化为 JSON（UTF-8） */
fun ExportData.toJson(): String {
    val root = JSONObject()
    root.put("version", version)

    val catsJson = JSONArray()
    categories.forEach { c ->
        val catJson = JSONObject()
        catJson.put("name", c.name)
        catJson.put("sortOrder", c.sortOrder)
        catJson.put("sortType", c.sortType.name)
        catJson.put("columns", c.columns)
        catJson.put("scale", c.scale.toDouble())
        catJson.put("labelScale", c.labelScale.toDouble())
        catJson.put("showTwoLine", c.showTwoLine)
        catJson.put("isSystem", c.isSystem)

        val appsJson = JSONArray()
        c.apps.forEach { a ->
            val appJson = JSONObject()
            appJson.put("packageName", a.packageName)
            appJson.put("appName", a.appName)
            appJson.put("sortOrder", a.sortOrder)
            appJson.put("isPinned", a.isPinned)
            appJson.put("launchCount", a.launchCount)
            appJson.put("lastLaunchTime", a.lastLaunchTime)
            appsJson.put(appJson)
        }
        catJson.put("apps", appsJson)
        catsJson.put(catJson)
    }
    root.put("categories", catsJson)

    val themeJson = JSONObject()
        themeJson.put("mode", theme.mode.name)
        themeJson.put("darkStyle", theme.darkStyle.name)
        themeJson.put("colorSource", theme.colorSource.name)
        themeJson.put("seedColor", theme.seedColor)
        themeJson.put("hapticIntensity", theme.hapticIntensity.name)
        themeJson.put("tabMultiLayer", theme.tabMultiLayer)
        root.put("theme", themeJson)

    return root.toString(2)
}

/** 从 JSON 反序列化，解析失败返回 null */
fun parseExportData(json: String): ExportData? {
    return try {
        val root = JSONObject(json)
        val catsArr = root.optJSONArray("categories") ?: JSONArray()

        val categories = buildList {
            for (i in 0 until catsArr.length()) {
                val c = catsArr.getJSONObject(i)
                val appsArr = c.optJSONArray("apps") ?: JSONArray()
                val apps = buildList {
                    for (j in 0 until appsArr.length()) {
                        val a = appsArr.getJSONObject(j)
                        add(
                            ExportApp(
                                packageName = a.getString("packageName"),
                                appName = a.optString("appName", a.getString("packageName")),
                                sortOrder = a.optInt("sortOrder", 0),
                                isPinned = a.optBoolean("isPinned", false),
                                launchCount = a.optInt("launchCount", 0),
                                lastLaunchTime = a.optLong("lastLaunchTime", 0),
                            )
                        )
                    }
                }
                add(
                    ExportCategory(
                        name = c.optString("name", "分类${i + 1}"),
                        sortOrder = c.optInt("sortOrder", i),
                        sortType = enumValueOrDefault(c.optString("sortType"), SortType.MANUAL),
                        columns = c.optInt("columns", 4).coerceIn(2, 6),
                        scale = c.optDouble("scale", 1.0).toFloat().coerceIn(0.6f, 1.5f),
                        labelScale = c.optDouble("labelScale", 1.0).toFloat().coerceIn(0.6f, 1.5f),
                        showTwoLine = c.optBoolean("showTwoLine", false),
                        isSystem = c.optBoolean("isSystem", false),
                        apps = apps,
                    )
                )
            }
        }

        val themeJson = root.optJSONObject("theme")
        val theme = if (themeJson != null) {
            ThemeSettings(
                mode = enumValueOrDefault(themeJson.optString("mode"), ThemeMode.SYSTEM),
                darkStyle = enumValueOrDefault(themeJson.optString("darkStyle"), DarkStyle.SOFT),
                colorSource = enumValueOrDefault(themeJson.optString("colorSource"), ColorSource.WALLPAPER),
                seedColor = themeJson.optLong("seedColor", 0xFF4C6FFF),
                hapticIntensity = enumValueOrDefault(
                    themeJson.optString("hapticIntensity"),
                    HapticIntensity.FOLLOW_SYSTEM,
                ),
                tabMultiLayer = themeJson.optBoolean("tabMultiLayer", false),
            )
        } else {
            ThemeSettings()
        }

        ExportData(
            version = root.optInt("version", 1),
            categories = categories,
            theme = theme,
        )
    } catch (_: Exception) {
        null
    }
}

/** 按名称解析枚举，未知值回退默认 */
private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default