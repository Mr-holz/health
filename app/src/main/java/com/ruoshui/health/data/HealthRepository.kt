package com.ruoshui.health.data

import com.ruoshui.health.health.HealthConnectManager
import com.ruoshui.health.network.HealthApi

class HealthRepository(
    private val healthConnectManager: HealthConnectManager,
    private val healthApi: HealthApi
) {
    suspend fun readToday(): HealthSummary {
        return healthConnectManager.readTodaySummary()
    }

    suspend fun syncToday(): HealthSummary {
        val summary = readToday()
        healthApi.uploadSummary(summary)
        return summary
    }
}
