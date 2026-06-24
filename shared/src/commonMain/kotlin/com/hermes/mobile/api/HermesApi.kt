package com.hermes.mobile.api

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalDecision
import com.hermes.mobile.models.ApprovalsResponse
import com.hermes.mobile.models.GoalRequest
import com.hermes.mobile.models.GoalResponse
import com.hermes.mobile.models.NodeStatus
import com.hermes.mobile.models.SessionTimeline
import com.hermes.mobile.models.SessionSummary
import com.hermes.mobile.models.SessionsResponse
import com.hermes.mobile.ui.ApprovalActionGateway
import com.hermes.mobile.ui.GoalGateway
import com.hermes.mobile.ui.InboxGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HermesApi(
    private val baseUrl: String,
    private val client: HttpClient = defaultHttpClient(),
) : InboxGateway, ApprovalActionGateway, GoalGateway {
    override suspend fun status(): NodeStatus = client.get("$baseUrl/mobile/v1/status").body()

    suspend fun pendingApprovalsResponse(): ApprovalsResponse =
        client.get("$baseUrl/mobile/v1/approvals?status=pending").body()

    override suspend fun pendingApprovals(): List<Approval> = pendingApprovalsResponse().approvals

    suspend fun approval(id: String): Approval =
        client.get("$baseUrl/mobile/v1/approvals/$id").body()

    override suspend fun approve(id: String, decision: ApprovalDecision): Approval =
        client.post("$baseUrl/mobile/v1/approvals/$id/approve") {
            contentType(ContentType.Application.Json)
            setBody(decision)
        }.body()

    override suspend fun deny(id: String, decision: ApprovalDecision): Approval =
        client.post("$baseUrl/mobile/v1/approvals/$id/deny") {
            contentType(ContentType.Application.Json)
            setBody(decision)
        }.body()

    suspend fun sessionsResponse(): SessionsResponse =
        client.get("$baseUrl/mobile/v1/sessions").body()

    suspend fun sessions(): List<SessionSummary> = sessionsResponse().sessions

    override suspend fun appendGoal(sessionId: String, request: GoalRequest): GoalResponse =
        client.post("$baseUrl/mobile/v1/sessions/$sessionId/goals") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun startSession(request: GoalRequest): GoalResponse =
        client.post("$baseUrl/mobile/v1/sessions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun sessionTimeline(sessionId: String): SessionTimeline =
        client.get("$baseUrl/mobile/v1/sessions/$sessionId/timeline").body()
}

fun defaultHttpClient(): HttpClient = HttpClient {
    install(WebSockets)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}
