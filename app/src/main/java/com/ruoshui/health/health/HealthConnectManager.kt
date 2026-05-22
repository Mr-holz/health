package com.ruoshui.health.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
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
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
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

    fun todayRange(): TimeRangeFilter {
        val now = Instant.now()
        val start = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        return TimeRangeFilter.between(start, now)
    }

    suspend fun readTodaySummary(): HealthSummary {
        val range = todayRange()
        val now = Instant.now().toString()

        // 步数
        val steps = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, timeRangeFilter = range)
        ).records.sumOf { it.count }

        // 心率
        val heartSamples = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = range)
        ).records.flatMap { it.samples }
        val averageHeartRate = heartSamples
            .map { it.beatsPerMinute }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()

        // 静息心率
        val restingHr = client.readRecords(
            ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.beatsPerMinute

        // 睡眠
        val sleepMinutes = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = range)
        ).records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }

        // 血氧
        val latestOxygen = client.readRecords(
            ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.percentage?.value

        // 距离（米）
        val distanceMeters = client.readRecords(
            ReadRecordsRequest(DistanceRecord::class, timeRangeFilter = range)
        ).records.sumOf { it.distance.inMeters.toLong() }

        // 总消耗热量（千卡）
        val caloriesKcal = client.readRecords(
            ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = range)
        ).records.sumOf { it.energy.inKilocalories.toLong() }

        // 活动消耗热量
        val activeCaloriesKcal = client.readRecords(
            ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = range)
        ).records.sumOf { it.energy.inKilocalories.toLong() }

        // 体重（kg）
        val weightKg = client.readRecords(
            ReadRecordsRequest(WeightRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.weight?.inKilograms

        // 体脂率
        val bodyFatPercent = client.readRecords(
            ReadRecordsRequest(BodyFatRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.percentage?.value

        // 运动时长（分钟）
        val exerciseMinutes = client.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter = range)
        ).records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }

        // HRV
        val hrvRmssd = client.readRecords(
            ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.heartRateVariabilityMillis

        // 呼吸率
        val respiratoryRate = client.readRecords(
            ReadRecordsRequest(RespiratoryRateRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.rate

        // 血糖
        val bloodGlucose = client.readRecords(
            ReadRecordsRequest(BloodGlucoseRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.level?.inMillimolesPerLiter

        // 体温
        val bodyTemp = client.readRecords(
            ReadRecordsRequest(BodyTemperatureRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }?.temperature?.inCelsius

        // 血压
        val bp = client.readRecords(
            ReadRecordsRequest(BloodPressureRecord::class, timeRangeFilter = range)
        ).records.maxByOrNull { it.time }
        val systolic = bp?.systolic?.inMillimetersOfMercury
        val diastolic = bp?.diastolic?.inMillimetersOfMercury

        // 饮水量
        val hydration = client.readRecords(
            ReadRecordsRequest(HydrationRecord::class, timeRangeFilter = range)
        ).records.sumOf { it.volume.inMilliliters.toLong() }

        return HealthSummary(
            steps = steps,
            averageHeartRate = averageHeartRate,
            restingHeartRate = restingHr,
            sleepMinutes = sleepMinutes,
            latestOxygenPercent = latestOxygen,
            distanceMeters = distanceMeters,
            caloriesKcal = caloriesKcal,
            activeCaloriesKcal = activeCaloriesKcal,
            weightKg = weightKg,
            bodyFatPercent = bodyFatPercent,
            exerciseMinutes = exerciseMinutes,
            hrvRmssd = hrvRmssd,
            respiratoryRate = respiratoryRate,
            bloodGlucoseMmol = bloodGlucose,
            bodyTemperatureCelsius = bodyTemp,
            systolicBP = systolic,
            diastolicBP = diastolic,
            hydrationMl = hydration,
            updatedAt = now
        )
    }
}
