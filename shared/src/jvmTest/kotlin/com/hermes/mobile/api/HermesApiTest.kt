package com.hermes.mobile.api

import com.hermes.mobile.models.ApprovalStatus
import com.hermes.mobile.models.RiskLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HermesApiTest {
    @Test
    fun fetchesPendingApprovals() = runTest {
        val api = HermesApi("http://test", mockClient(approvalsJson))

        val approvals = api.pendingApprovals()

        assertEquals(1, approvals.size)
        assertEquals("appr_mock_git_push", approvals.first().id)
        assertEquals(RiskLevel.High, approvals.first().risk)
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
