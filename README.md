# START

Android 16+ 专属的应用启动面板。打开软件即可一键启动你常用的应用，告别在桌面翻找图标的体验。

## 特性

- 一键启动第三方应用
- 自定义分类，支持排序方式（手动 / 名称 / 频率 / 最近）、列数、图标缩放、文字缩放、双行名称显示
- 长按应用：置顶 / 上移 / 下移 / 移除
- 应用搜索
- 启动频率统计
- 动态莫奈取色：跟随壁纸动态取色，或自定义种子色
- 主题模式：跟随系统 / 浅色 / 深色，深色可选 OLED 纯黑
- Material 3 Expressive 设计，弹簧动效

## 技术栈

- Kotlin + Jetpack Compose
- Material 3 Expressive（Compose Material3 1.4.0）
- Room（应用 / 分类 / 启动统计）
- DataStore（主题设置）
- materialkolor（莫奈取色 / 种子色生成 M3 色板）

## 环境要求

- Android 16+（minSdk 36）
- JDK 21

## 构建

```bash
./gradlew assembleDebug
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

> Windows 下使用 `gradlew.bat`。

## 许可证

本项目采用 [MIT License](LICENSE)。

## 作者

- Enik
- QQ：1334204015

## 仓库

https://github.com/Sdbdzh/start