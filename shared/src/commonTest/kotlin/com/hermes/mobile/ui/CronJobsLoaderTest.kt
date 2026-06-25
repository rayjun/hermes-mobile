package com.hermes.mobile.ui

import com.hermes.mobile.models.CronJob
import com.hermes.mobile.models.CronRun
import com.hermes.mobile.models.CronRunStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class CronJobsLoaderTest {
    @Test
    fun loadsCronJobsFromGateway() = runTest {
        val loader = CronJobsLoader(FakeCronJobsGateway())

        val state = loader.load()

        assertIs<CronJobsLoadState.Ready>(state)
        assertFalse(state.usingFallback)
        assertEquals(2, state.jobs.size)
        assertEquals("DeFi morning report", state.jobs.first().name)
        assertEquals(CronRunStatus.Success, state.jobs.first().lastRun?.status)
    }

    @Test
    fun returnsOfflineFallbackWhenGatewayFails() = runTest {
        val loader = CronJobsLoader(FailingCronJobsGateway())

        val state = loader.load()

        assertIs<CronJobsLoadState.Ready>(state)
        assertEquals(true, state.usingFallback)
        assertEquals("Gateway unavailable · showing sample automations", state.notice)
        assertEquals("Sample automation", state.jobs.first().name)
    }
}

private class FakeCronJobsGateway : CronJobsGateway {
    override suspend fun cronJobs(): List<CronJob> = listOf(
        CronJob(
            id = "cron_report",
            name = "DeFi morning report",
            schedule = "0 9 * * *",
            enabled = true,
            nextRunAt = Instant.parse("2026-06-25T09:00:00Z"),
            lastRun = CronRun(
                status = CronRunStatus.Success,
                summary = "Delivered concise DeFi morning report.",
                finishedAt = Instant.parse("2026-06-24T09:00:00Z"),
            ),
        ),
        CronJob(
            id = "cron_sync",
            name = "Hermes memory sync",
            schedule = "every hour",
            enabled = true,
            nextRunAt = Instant.parse("2026-06-25T10:00:00Z"),
            lastRun = CronRun(
                status = CronRunStatus.Success,
                summary = "Pushed memory and skills.",
                finishedAt = Instant.parse("2026-06-25T08:00:00Z"),
            ),
        ),
    )
}

private class FailingCronJobsGateway : CronJobsGateway {
    override suspend fun cronJobs(): List<CronJob> = error("offline")
}
