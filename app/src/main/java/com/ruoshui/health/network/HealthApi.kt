package com.ruoshui.health.network

import com.ruoshui.health.data.HealthSummary
import retrofit2.http.Body
import retrofit2.http.POST

interface HealthApi {
    @POST("health/summary")
    suspend fun uploadSummary(@Body summary: HealthSummary)
}
