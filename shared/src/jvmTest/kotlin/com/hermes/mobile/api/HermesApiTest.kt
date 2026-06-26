package com.hermes.mobile.api

import com.hermes.mobile.models.ApprovalStatus
import com.hermes.mobile.models.RiskLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HermesApiTest {
    @Test
    fun startsPairingAndCompletesPairing() = runTest {
        val api = HermesApi("http://test", queueMockClient(pairStartJson, pairCompleteJson))

        val start = api.startPairing()
        val complete = api.completePairing(
            code = start.code,
            deviceName = "Ray's Android",
            platform = "android",
            publicKey = "test-public-key",
        )

        assertEquals("pair_test", start.pairingId)
        assertEquals("834912", start.code)
        assertEquals("hermes://pair?url=http://test&code=834912&fingerprint=mock", start.qrPayload)
        assertEquals("dev_test", complete.deviceId)
        assertEquals("hmob_test_token", complete.deviceToken)
        assertEquals(true, complete.capabilities["approvals"])
    }

    @Test
    fun fetchesPendingApprovals() = runTest {
        val api = HermesApi("http://test", mockClient(approvalsJson))

        val approvals = api.pendingApprovals()

        assertEquals(1, approvals.size)
        assertEquals("appr_mock_git_push", approvals.first().id)
        assertEquals(RiskLevel.High, approvals.first().risk)
    }

    @Test
    fun attachesBearerTokenToResourceRequests() = runTest {
        val api = HermesApi("http://test", bearerAssertingClient(approvalsJson), deviceToken = "hmob_test_token")

        val approvals = api.pendingApprovals()

        assertEquals(1, approvals.size)
    }

    @Test
    fun fetchesPairedDevices() = runTest {
        val api = HermesApi("http://test", methodAssertingClient(HttpMethod.Get, "/mobile/v1/devices", devicesJson), deviceToken = "hmob_test_token")

        val devices = api.devices()

        assertEquals(1, devices.size)
        assertEquals("dev_test", devices.first().id)
        assertEquals("Ray's Android", devices.first().name)
    }

    @Test
    fun revokesPairedDevice() = runTest {
        val api = HermesApi("http://test", methodAssertingClient(HttpMethod.Delete, "/mobile/v1/devices/dev_test", ""), deviceToken = "hmob_test_token")

        api.revokeDevice("dev_test")
    }

    @Test
    fun approvesPendingApproval() = runTest {
        val api = HermesApi("http://test", mockClient(approvedApprovalJson))

        val approval = api.approve("appr_mock_git_push", com.hermes.mobile.models.ApprovalDecision())

        assertEquals("appr_mock_git_push", approval.id)
        assertEquals(ApprovalStatus.Approved, approval.status)
    }

    @Test
    fun fetchesSessionList() = runTest {
        val api = HermesApi("http://test", mockClient(sessionsJson))

        val sessions = api.sessions()

        assertEquals(1, sessions.size)
        assertEquals("sess_real", sessions.first().id)
        assertEquals("Ship mobile adapter", sessions.first().title)
    }

    @Test
    fun fetchesArtifacts() = runTest {
        val api = HermesApi("http://test", mockClient(artifactsJson))

        val artifacts = api.artifacts()

        assertEquals(1, artifacts.size)
        assertEquals("art_patch", artifacts.first().id)
        assertEquals("mobile-api.patch", artifacts.first().title)
    }

    @Test
    fun fetchesCronJobs() = runTest {
        val api = HermesApi("http://test", mockClient(cronJobsJson))

        val jobs = api.cronJobs()

        assertEquals(1, jobs.size)
        assertEquals("cron_report", jobs.first().id)
        assertEquals("DeFi morning report", jobs.first().name)
    }

    @Test
    fun fetchesCronJobDetail() = runTest {
        val api = HermesApi("http://test", mockClient(cronJobJson))

        val job = api.cronJob("cron_report")

        assertEquals("cron_report", job.id)
        assertEquals("DeFi morning report", job.name)
        assertEquals("Delivered concise DeFi morning report.", job.lastRun?.summary)
    }
}

private fun mockClient(responseBody: String): HttpClient = HttpClient(MockEngine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
    engine {
        addHandler {
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }
}

private fun bearerAssertingClient(responseBody: String): HttpClient = HttpClient(MockEngine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
    engine {
        addHandler { request ->
            assertEquals("Bearer hmob_test_token", request.headers[HttpHeaders.Authorization])
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }
}

private fun methodAssertingClient(method: HttpMethod, path: String, responseBody: String): HttpClient = HttpClient(MockEngine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
    engine {
        addHandler { request ->
            assertEquals(method, request.method)
            assertEquals(path, request.url.encodedPath)
            assertEquals("Bearer hmob_test_token", request.headers[HttpHeaders.Authorization])
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }
}

private fun queueMockClient(vararg responseBodies: String): HttpClient = HttpClient(MockEngine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
    engine {
        val bodies = ArrayDeque(responseBodies.toList())
        addHandler {
            respond(
                content = bodies.removeFirst(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }
}

private const val pairStartJson = """
{
  "pairing_id": "pair_test",
  "code": "834912",
  "expires_at": "2026-06-24T10:15:00Z",
  "qr_payload": "hermes://pair?url=http://test&code=834912&fingerprint=mock"
}
"""

private const val pairCompleteJson = """
{
  "device_id": "dev_test",
  "device_token": "hmob_test_token",
  "capabilities": {
    "approvals": true,
    "sessions": true,
    "cron": true,
    "artifacts": true
  }
}
"""

private const val devicesJson = """
{
  "devices": [
    {
      "id": "dev_test",
      "name": "Ray's Android",
      "platform": "android",
      "created_at": "2026-06-24T10:15:00Z"
    }
  ]
}
"""

private const val approvalsJson = """
{
  "approvals": [
    {
      "id": "appr_mock_git_push",
      "session_id": "sess_mock_contribution",
      "kind": "terminal_command",
      "risk": "high",
      "status": "pending",
      "title": "Run git push",
      "summary": "Hermes wants to push branch fix/mobile-api.",
      "reason": "The requested change passed tests and needs to be pushed for review.",
      "details": {"repo": "rayjun/hermes-agent"},
      "actions": ["approve", "deny", "ask"],
      "created_at": "2026-06-24T10:00:00Z"
    }
  ]
}
"""

private const val approvedApprovalJson = """
{
  "id": "appr_mock_git_push",
  "session_id": "sess_mock_contribution",
  "kind": "terminal_command",
  "risk": "high",
  "status": "approved",
  "title": "Run git push",
  "summary": "Hermes wants to push branch fix/mobile-api.",
  "reason": "The requested change passed tests and needs to be pushed for review.",
  "details": {"repo": "rayjun/hermes-agent"},
  "actions": ["approve", "deny", "ask"],
  "created_at": "2026-06-24T10:00:00Z"
}
"""

private const val sessionsJson = """
{
  "sessions": [
    {
      "id": "sess_real",
      "title": "Ship mobile adapter",
      "status": "running",
      "created_at": "2026-06-24T10:00:00Z",
      "updated_at": "2026-06-24T10:01:00Z"
    }
  ]
}
"""

private const val artifactsJson = """
{
  "artifacts": [
    {
      "id": "art_patch",
      "session_id": "sess_real",
      "kind": "file",
      "title": "mobile-api.patch",
      "summary": "Patch generated by Hermes.",
      "mime_type": "text/x-diff",
      "uri": "file:///tmp/mobile-api.patch",
      "size_bytes": 1024,
      "metadata": {"language": "diff"},
      "created_at": "2026-06-24T10:00:00Z"
    }
  ]
}
"""

private const val cronJobsJson = """
{
  "jobs": [
    {
      "id": "cron_report",
      "name": "DeFi morning report",
      "schedule": "0 9 * * *",
      "enabled": true,
      "next_run_at": "2026-06-25T09:00:00Z",
      "last_run": {
        "status": "success",
        "summary": "Delivered concise DeFi morning report.",
        "finished_at": "2026-06-24T09:00:00Z"
      }
    }
  ]
}
"""

private const val cronJobJson = """
{
  "id": "cron_report",
  "name": "DeFi morning report",
  "schedule": "0 9 * * *",
  "enabled": true,
  "next_run_at": "2026-06-25T09:00:00Z",
  "last_run": {
    "status": "success",
    "summary": "Delivered concise DeFi morning report.",
    "finished_at": "2026-06-24T09:00:00Z"
  }
}
"""
