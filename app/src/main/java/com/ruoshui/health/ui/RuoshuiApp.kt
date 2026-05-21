package com.ruoshui.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruoshui.health.data.HealthConnectStatus
import com.ruoshui.health.data.HealthSummary
import com.ruoshui.health.health.HealthConnectManager
import com.ruoshui.health.ui.theme.Sky
import com.ruoshui.health.ui.theme.Spring
import com.ruoshui.health.ui.theme.SurfaceLift
import com.ruoshui.health.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun RuoshuiApp(healthConnectManager: HealthConnectManager) {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(HealthSummary()) }
    var status by remember { mutableStateOf(healthConnectManager.status()) }
    var hasPermission by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("等待同步") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.permissionContract()
    ) { granted ->
        hasPermission = granted.containsAll(healthConnectManager.permissions)
        message = if (hasPermission) "授权完成，可以读取今日数据" else "还缺少部分健康数据权限"
    }

    fun refresh() {
        scope.launch {
            status = healthConnectManager.status()
            hasPermission = healthConnectManager.hasAllPermissions()
            if (status == HealthConnectStatus.Available && hasPermission) {
                summary = healthConnectManager.readTodaySummary()
                message = "已同步到本地"
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Header(status = status, message = message)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { permissionLauncher.launch(healthConnectManager.permissions) },
                enabled = status == HealthConnectStatus.Available
            ) {
                Text("授权")
            }
            OutlinedButton(onClick = { refresh() }) {
                Text("同步")
            }
        }

        MetricsGrid(summary)
        TrendPanel()
        SleepPanel(summary.sleepMinutes)
    }
}

@Composable
private fun Header(status: HealthConnectStatus, message: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "若水三千",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = statusLabel(status) + " · " + message,
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MetricsGrid(summary: HealthSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("步数", summary.steps.toString(), "今日累计", Modifier.weight(1f))
            MetricCard("心率", summary.averageHeartRate?.let { "$it" } ?: "--", "bpm 平均", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("睡眠", formatMinutes(summary.sleepMinutes), "昨夜与今日", Modifier.weight(1f))
            MetricCard("血氧", summary.latestOxygenPercent?.let { "%.1f%%".format(it) } ?: "--", "最近一次", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextMuted, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(caption, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TrendPanel() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLift)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("心率趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Sparkline()
        }
    }
}

@Composable
private fun Sparkline() {
    val values = listOf(62, 68, 73, 70, 76, 82, 78, 74, 71, 69, 72, 67)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val max = values.maxOrNull()?.toFloat() ?: 1f
        val min = values.minOrNull()?.toFloat() ?: 0f
        val step = size.width / (values.lastIndex.coerceAtLeast(1))
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * step
            val normalized = (value - min) / (max - min)
            val y = size.height - normalized * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(
            color = Sky.copy(alpha = 0.22f),
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f),
            strokeWidth = 2.dp.toPx()
        )
        drawPath(path, color = Spring, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun SleepPanel(minutes: Long) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Spring.copy(alpha = 0.16f))
                    drawArc(
                        color = Spring,
                        startAngle = -90f,
                        sweepAngle = ((minutes / 480f).coerceIn(0f, 1f)) * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(formatMinutes(minutes), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("睡眠分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("以 8 小时为参考目标", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun statusLabel(status: HealthConnectStatus): String {
    return when (status) {
        HealthConnectStatus.Available -> "Health Connect 可用"
        HealthConnectStatus.NeedsProvider -> "需要安装或更新 Health Connect"
        HealthConnectStatus.Unavailable -> "当前设备不可用"
    }
}

private fun formatMinutes(minutes: Long): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
}
