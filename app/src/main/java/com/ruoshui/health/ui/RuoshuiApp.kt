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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruoshui.health.data.HealthConnectStatus
import com.ruoshui.health.data.HealthSummary
import com.ruoshui.health.health.HealthConnectManager
import com.ruoshui.health.ui.theme.DeepInk
import com.ruoshui.health.ui.theme.Rose
import com.ruoshui.health.ui.theme.Sky
import com.ruoshui.health.ui.theme.Spring
import com.ruoshui.health.ui.theme.Sun
import com.ruoshui.health.ui.theme.SurfaceGreen
import com.ruoshui.health.ui.theme.SurfaceLift
import com.ruoshui.health.ui.theme.TextMuted
import com.ruoshui.health.ui.theme.TextPrimary
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
        message = if (hasPermission) "授权完成" else "还缺少部分权限"
    }

    fun refresh() {
        scope.launch {
            status = healthConnectManager.status()
            hasPermission = healthConnectManager.hasAllPermissions()
            if (status == HealthConnectStatus.Available && hasPermission) {
                summary = healthConnectManager.readTodaySummary()
                message = "已同步"
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepInk)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // —— 顶栏 ——
        TopBar(status = status, message = message) {
            permissionLauncher.launch(healthConnectManager.permissions)
            refresh()
        }

        if (status != HealthConnectStatus.Available || !hasPermission) {
            EmptyHint(status = status, hasPermission = hasPermission)
        } else {
            // —— 主步数环 ——
            StepRing(steps = summary.steps)

            // —— 核心 2x3 网格 ——
            CoreGrid(summary)

            // —— 次要行 ——
            ExtraRow(summary)

            // —— 血压卡片 ——
            BloodPressureCard(systolic = summary.systolicBP, diastolic = summary.diastolicBP)

            // —— 趋势图 ——
            TrendCard(summary)
        }
    }
}

// ================== 组件 ==================

@Composable
private fun TopBar(
    status: HealthConnectStatus,
    message: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("若水三千", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            val dot = when (status) {
                HealthConnectStatus.Available -> "●"
                else -> "○"
            }
            val dotColor = when (status) {
                HealthConnectStatus.Available -> Spring
                HealthConnectStatus.NeedsProvider -> Sun
                HealthConnectStatus.Unavailable -> Rose
            }
            Text(
                "$dot $message",
                fontSize = 12.sp,
                color = dotColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAction) { Text("授权") }
            Button(onClick = onAction) { Text("同步") }
        }
    }
}

@Composable
private fun EmptyHint(status: HealthConnectStatus, hasPermission: Boolean) {
    val text = when {
        status == HealthConnectStatus.NeedsProvider -> "请安装或更新 Health Connect"
        status == HealthConnectStatus.Unavailable -> "当前设备不支持 Health Connect"
        !hasPermission -> "请先授权访问健康数据"
        else -> ""
    }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextMuted)
    }
}

@Composable
private fun StepRing(steps: Long) {
    val goal = 10_000
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = 14.dp.toPx()
            // 背景环
            drawCircle(color = SurfaceGreen, radius = size.minDimension / 2f, style = Stroke(stroke))
            // 进度弧
            drawArc(
                color = Spring,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(steps.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("步", fontSize = 14.sp, color = TextMuted)
            Text("目标 $goal", fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun CoreGrid(summary: HealthSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniCard("平均心率", summary.averageHeartRate?.let { "$it" } ?: "--", "bpm", Spring, Modifier.weight(1f))
            MiniCard("静息心率", summary.restingHeartRate?.let { "$it" } ?: "--", "bpm", Sky, Modifier.weight(1f))
            MiniCard("睡眠", fmtDur(summary.sleepMinutes), "", Spring, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniCard("血氧", summary.latestOxygenPercent?.let { "%.0f%%".format(it) } ?: "--", "", Rose, Modifier.weight(1f))
            MiniCard("体重", summary.weightKg?.let { "%.1f".format(it) } ?: "--", "kg", Sky, Modifier.weight(1f))
            MiniCard("体脂", summary.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "--", "", Sun, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniCard(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGreen)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = TextMuted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = accent)
                if (unit.isNotEmpty()) Spacer(Modifier.width(3.dp))
                Text(unit, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun ExtraRow(summary: HealthSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("更多数据", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExtraItem("距离", fmtDist(summary.distanceMeters), Modifier.weight(1f))
            ExtraItem("总消耗", "${summary.caloriesKcal} kcal", Modifier.weight(1f))
            ExtraItem("活动消耗", "${summary.activeCaloriesKcal} kcal", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExtraItem("运动时长", fmtDur(summary.exerciseMinutes), Modifier.weight(1f))
            ExtraItem("HRV", summary.hrvRmssd?.let { "%.0f ms".format(it) } ?: "--", Modifier.weight(1f))
            ExtraItem("呼吸率", summary.respiratoryRate?.let { "%.0f".format(it) } ?: "--", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExtraItem("血糖", summary.bloodGlucoseMmol?.let { "%.1f".format(it) } ?: "--", Modifier.weight(1f))
            ExtraItem("体温", summary.bodyTemperatureCelsius?.let { "%.1f°C".format(it) } ?: "--", Modifier.weight(1f))
            ExtraItem("饮水", "${summary.hydrationMl} ml", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExtraItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLift)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
private fun BloodPressureCard(systolic: Double?, diastolic: Double?) {
    if (systolic == null && diastolic == null) return
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGreen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("收缩压", fontSize = 11.sp, color = TextMuted)
                Text(systolic?.let { "%.0f".format(it) } ?: "--", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Rose)
            }
            Text("  /  ", fontSize = 22.sp, color = TextMuted)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("舒张压", fontSize = 11.sp, color = TextMuted)
                Text(diastolic?.let { "%.0f".format(it) } ?: "--", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Sky)
            }
            Spacer(Modifier.width(12.dp))
            Text("mmHg", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun TrendCard(summary: HealthSummary) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLift)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今日概览", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendRing("步数", summary.steps, 10_000, Spring)
                TrendRing("睡眠", summary.sleepMinutes, 480, Sky)
                TrendRing("运动", summary.exerciseMinutes, 60, Sun)
            }
        }
    }
}

@Composable
private fun TrendRing(label: String, value: Long, goal: Long, color: Color) {
    val progress = (value.toFloat() / goal).coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(72.dp)) {
            drawCircle(color = SurfaceGreen, radius = 32.dp.toPx(), style = Stroke(6.dp.toPx()))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (label == "睡眠" || label == "运动") fmtDur(value) else value.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(label, fontSize = 10.sp, color = TextMuted)
        }
    }
}

// ================== 工具函数 ==================

private fun fmtDur(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

private fun fmtDist(meters: Long): String {
    return if (meters >= 1000) "%.2f km".format(meters / 1000.0) else "${meters}m"
}
