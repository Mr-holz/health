package com.ruoshui.health.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.ruoshui.health.data.HealthConnectStatus
import com.ruoshui.health.data.HealthSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    )

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    fun status(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.NeedsProvider
            else -> HealthConnectStatus.Unavailable
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        if (status() != HealthConnectStatus.Available) return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun readTodaySummary(): HealthSummary {
        val now = Instant.now()
        val start = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val range = TimeRangeFilter.between(start, now)

        val steps = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, timeRangeFilter = range)
        ).records.sumOf { it.count }

        val heartSamples = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range)
        ).records.flatMap { it.samples }
        val averageHeartRate = heartSamples
            .map { it.beatsPerMinute }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()

        val sleepMinutes = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = range)
        ).records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }

        val latestOxygen = client.readRecords(
            ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.percentage?.value

        return HealthSummary(
            steps = steps,
            averageHeartRate = averageHeartRate,
            sleepMinutes = sleepMinutes,
            latestOxygenPercent = latestOxygen,
            updatedAt = now.toString()
        )
    }
}
