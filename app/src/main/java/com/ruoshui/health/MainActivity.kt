package com.ruoshui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ruoshui.health.health.HealthConnectManager
import com.ruoshui.health.ui.RuoshuiApp
import com.ruoshui.health.ui.theme.RuoshuiTheme
import com.ruoshui.health.worker.SyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(applicationContext)

        val healthConnectManager = HealthConnectManager(applicationContext)
        setContent {
            RuoshuiTheme {
                RuoshuiApp(healthConnectManager = healthConnectManager)
            }
        }
    }
}
