package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CronJob(
    val id: String,
    val name: String,
    val schedule: String,
    val enabled: Boolean,
    @SerialName("next_run_at") val nextRunAt: Instant? = null,
    @SerialName("last_run") val lastRun: CronRun? = null,
)

@Serializable
data class CronRun(
    val status: CronRunStatus,
    val summary: String,
    @SerialName("finished_at") val finishedAt: Instant? = null,
)

@Serializable
enum class CronRunStatus {
    @SerialName("success") Success,
    @SerialName("failed") Failed,
    @SerialName("running") Running,
    @SerialName("skipped") Skipped,
}

@Serializable
data class CronJobsResponse(
    val jobs: List<CronJob>,
)
