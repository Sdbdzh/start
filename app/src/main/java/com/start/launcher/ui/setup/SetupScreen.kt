package com.start.launcher.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.start.launcher.theme.Spacing
import com.start.launcher.theme.StartShapes

/**
 * 首启页面：仅一个欢迎卡片，点击「开始设置」直接进入主界面。
 * 应用与分类的添加都在主界面内通过「创建分类」完成。
 */
@Composable
fun SetupScreen(onComplete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxl)
                .clip(StartShapes.heroCard)
                .background(scheme.surfaceContainerLow)
                .border(1.dp, scheme.outlineVariant, StartShapes.heroCard)
                .padding(horizontal = Spacing.xxxl, vertical = Spacing.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("START", fontSize = 44.sp, fontWeight = FontWeight.Black, color = scheme.primary)
            Spacer(Modifier.height(Spacing.sm))
            Text("应用启动面板", fontSize = 15.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                text = "创建分类并添加常用应用\n一键启动，告别桌面翻找",
                fontSize = 14.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(Spacing.xxxl))
            Button(
                onClick = onComplete,
                shape = StartShapes.pill,
                modifier = Modifier.width(220.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
            ) {
                Text("开始设置", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}