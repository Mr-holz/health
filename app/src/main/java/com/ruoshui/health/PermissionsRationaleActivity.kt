package com.ruoshui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruoshui.health.ui.theme.RuoshuiTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuoshuiTheme {
                RationaleScreen()
            }
        }
    }
}

@Composable
private fun RationaleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "若水三千需要读取 Health Connect 中的步数、心率、睡眠、血氧、体重、体脂、血压、运动、HRV、呼吸率、血糖、体温、饮水等健康数据，用于本地展示与按小时同步。",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
