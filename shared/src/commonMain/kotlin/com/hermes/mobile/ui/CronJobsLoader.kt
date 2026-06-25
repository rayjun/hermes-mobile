package com.hermes.mobile.ui

import com.hermes.mobile.models.CronJob
import com.hermes.mobile.models.CronRun
import com.hermes.mobile.models.CronRunStatus
import kotlinx.datetime.Instant

interface CronJobsGateway {
    suspend fun cronJobs(): List<CronJob>
}

sealed interface CronJobsLoadState {
    data object Loading : CronJobsLoadState

    data class Ready(
        val jobs: List<CronJob>,
        val notice: String? = null,
        val usingFallback: Boolean = false,
    ) : CronJobsLoadState
}

class CronJobsLoader(
    private val gateway: CronJobsGateway,
) {
    suspend fun load(): CronJobsLoadState.Ready = try {
        CronJobsLoadState.Ready(jobs = gateway.cronJobs())
    } catch (_: Throwable) {
        CronJobsLoadState.Ready(
            jobs = listOf(fallbackJob()),
            notice = "Gateway unavailable · showing sample automations",
            usingFallback = true,
        )
    }
}

private fun fallbackJob(): CronJob = CronJob(
    id = "cron_sample",
    name = "Sample automation",
    schedule = "every hour",
    enabled = true,
    nextRunAt = Instant.parse("2026-06-24T13:00:00Z"),
    lastRun = CronRun(
        status = CronRunStatus.Success,
        summary = "Sample automation completed.",
        finishedAt = Instant.parse("2026-06-24T12:00:00Z"),
    ),
)
