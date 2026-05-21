package com.ruoshui.health.data

import kotlinx.serialization.Serializable

@Serializable
data class HealthSummary(
    val steps: Long = 0,
    val averageHeartRate: Long? = null,
    val sleepMinutes: Long = 0,
    val latestOxygenPercent: Double? = null,
    val distanceMeters: Long = 0,
    val caloriesKcal: Long = 0,
    val updatedAt: String = "",
    val source: String = "Health Connect"
)

enum class HealthConnectStatus {
    Available,
    NeedsProvider,
    Unavailable
}
