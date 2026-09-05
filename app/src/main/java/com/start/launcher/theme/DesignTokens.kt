package com.start.launcher.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * START 设计令牌系统 —— 所有页面统一引用，保证视觉一致性。
 *
 * 调性：大胆色块风（Bold Blocks）
 *   - 大色块碰撞，高对比度，Monet 取色饱和度拉高
 *   - 粗体排版，克制留白，信息层级分明
 *   - 形状：圆角偏向 MD3E 大圆角，但保留轻微棱角以增加力度
 */

// ── 间距尺度（8dp 基准） ──────────────────────
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
    val massive = 64.dp
}

// ── 形状系统 ─────────────────────────────────
object StartShapes {
    /** 按钮/芯片/标签 */
    val pill = RoundedCornerShape(50) // 全圆角胶囊
    /** 小组件卡片 */
    val card = RoundedCornerShape(16.dp)
    /** 大卡片（hero card） */
    val heroCard = RoundedCornerShape(28.dp)
    /** 图标/头像容器 */
    val icon = RoundedCornerShape(20.dp)

    val material = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(40.dp),
    )
}

// ── 动效常量 ─────────────────────────────────
object Motion {
    /** 操作反馈：弹簧物理（点击/选中/展开） */
    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /** 操作反馈：快速弹簧（切换/按钮） */
    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** 颜色动画用弹簧（无类型参数，适配 animateColorAsState） */
    val springColor = spring<androidx.compose.ui.graphics.Color>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** 氛围呼吸：无限循环，缓慢微妙 */
    const val breatheDurationMs = 5000

    /** 展开/收起过渡 */
    const val expandDurationMs = 400

    /** 淡入淡出 */
    const val fadeDurationMs = 300
}

// ── 字号比例 ─────────────────────────────────
object TypeScale {
    /** 标签/辅助文字 */
    val label = 12
    /** 正文 */
    val body = 14
    /** 子标题 */
    val subtitle = 16
    /** 标题 */
    val title = 20
    /** 大标题 */
    val headline = 28
    /** 展示标题 */
    val display = 40
}