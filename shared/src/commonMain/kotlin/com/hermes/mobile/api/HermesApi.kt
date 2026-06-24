package com.hermes.mobile.api

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalDecision
import com.hermes.mobile.models.ApprovalsResponse
import com.hermes.mobile.models.NodeStatus
import com.hermes.mobile.models.SessionTimeline
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
) {
    suspend fun status(): NodeStatus = client.get("$baseUrl/mobile/v1/status").body()

    suspend fun pendingApprovals(): ApprovalsResponse =
        client.get("$baseUrl/mobile/v1/approvals?status=pending").body()

    suspend fun approval(id: String): Approval =
        client.get("$baseUrl/mobile/v1/approvals/$id").body()

    suspend fun approve(id: String, decision: ApprovalDecision = ApprovalDecision()): Approval =
        client.post("$baseUrl/mobile/v1/approvals/$id/approve") {
            contentType(ContentType.Application.Json)
            setBody(decision)
        }.body()

    suspend fun deny(id: String, decision: ApprovalDecision): Approval =
        client.post("$baseUrl/mobile/v1/approvals/$id/deny") {
            contentType(ContentType.Application.Json)
            setBody(decision)
        }.body()

    suspend fun sessionTimeline(sessionId: String): SessionTimeline =
        client.get("$baseUrl/mobile/v1/sessions/$sessionId/timeline").body()
}

fun defaultHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}
