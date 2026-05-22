package com.ruoshui.health.data

import kotlinx.serialization.Serializable

@Serializable
data class HealthSummary(
    val steps: Long = 0,
    val averageHeartRate: Long? = null,
    val restingHeartRate: Long? = null,
    val sleepMinutes: Long = 0,
    val latestOxygenPercent: Double? = null,
    val distanceMeters: Long = 0,
    val caloriesKcal: Long = 0,
    val activeCaloriesKcal: Long = 0,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val exerciseMinutes: Long = 0,
    val hrvRmssd: Double? = null,
    val respiratoryRate: Double? = null,
    val bloodGlucoseMmol: Double? = null,
    val bodyTemperatureCelsius: Double? = null,
    val systolicBP: Double? = null,
    val diastolicBP: Double? = null,
    val hydrationMl: Long = 0,
    val updatedAt: String = "",
    val source: String = "Health Connect"
)

enum class HealthConnectStatus {
    Available,
    NeedsProvider,
    Unavailable
}
