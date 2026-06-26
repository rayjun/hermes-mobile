package com.hermes.mobile.api

import com.hermes.mobile.models.Approval
import com.hermes.mobile.models.ApprovalDecision
import com.hermes.mobile.models.ApprovalsResponse
import com.hermes.mobile.models.Artifact
import com.hermes.mobile.models.ArtifactsResponse
import com.hermes.mobile.models.CronJob
import com.hermes.mobile.models.CronJobsResponse
import com.hermes.mobile.models.DeviceInfo
import com.hermes.mobile.models.DevicesResponse
import com.hermes.mobile.models.GoalRequest
import com.hermes.mobile.models.GoalResponse
import com.hermes.mobile.models.NodeStatus
import com.hermes.mobile.models.PairingCompleteRequest
import com.hermes.mobile.models.PairingCompleteResponse
import com.hermes.mobile.models.PairingStartResponse
import com.hermes.mobile.models.SessionTimeline
import com.hermes.mobile.models.SessionSummary
import com.hermes.mobile.models.SessionsResponse
import com.hermes.mobile.ui.ApprovalActionGateway
import com.hermes.mobile.ui.ArtifactsGateway
import com.hermes.mobile.ui.CronJobsGateway
import com.hermes.mobile.ui.GatewayStatusProbe
import com.hermes.mobile.ui.GoalGateway
import com.hermes.mobile.ui.InboxGateway
import com.hermes.mobile.ui.PairingGateway
import com.hermes.mobile.ui.SessionsGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HermesApi(
    private val baseUrl: String,
    private val client: HttpClient = defaultHttpClient(),
    private val deviceToken: String? = null,
) : InboxGateway, ApprovalActionGateway, GoalGateway, SessionsGateway, ArtifactsGateway, CronJobsGateway, GatewayStatusProbe, PairingGateway {
    override suspend fun status(): NodeStatus = client.get("$baseUrl/mobile/v1/status").body()

    override suspend fun status(baseUrl: String): NodeStatus = client.get("$baseUrl/mobile/v1/status").body()

    override suspend fun startPairing(): PairingStartResponse =
        client.post("$baseUrl/mobile/v1/pair/start").body()

    override suspend fun completePairing(
        code: String,
        deviceName: String,
        platform: String,
        publicKey: String?,
    ): PairingCompleteResponse =
        client.post("$baseUrl/mobile/v1/pair/complete") {
            contentType(ContentType.Application.Json)
            setBody(PairingCompleteRequest(code = code, deviceName = deviceName, platform = platform, publicKey = publicKey))
        }.body()

    suspend fun devicesResponse(): DevicesResponse =
        client.get("$baseUrl/mobile/v1/devices") { authorize() }.body()

    suspend fun devices(): List<DeviceInfo> = devicesResponse().devices

    suspend fun revokeDevice(deviceId: String) {
        client.delete("$baseUrl/mobile/v1/devices/$deviceId") { authorize() }
    }

    suspend fun pendingApprovalsResponse(): ApprovalsResponse =
        client.get("$baseUrl/mobile/v1/approvals?status=pending") { authorize() }.body()

    override suspend fun pendingApprovals(): List<Approval> = pendingApprovalsResponse().approvals

    suspend fun approval(id: String): Approval =
        client.get("$baseUrl/mobile/v1/approvals/$id") { authorize() }.body()

    override suspend fun approve(id: String, decision: ApprovalDecision): Approval =
        client.post("$baseUrl/mobile/v1/approvals/$id/approve") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(decision)
        }.body()

    override suspend fun deny(id: String, decision: ApprovalDecision): Approval =
        client.post("$baseUrl/mobile/v1/approvals/$id/deny") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(decision)
        }.body()

    suspend fun sessionsResponse(): SessionsResponse =
        client.get("$baseUrl/mobile/v1/sessions") { authorize() }.body()

    override suspend fun sessions(): List<SessionSummary> = sessionsResponse().sessions

    suspend fun artifactsResponse(): ArtifactsResponse =
        client.get("$baseUrl/mobile/v1/artifacts") { authorize() }.body()

    override suspend fun artifacts(): List<Artifact> = artifactsResponse().artifacts

    suspend fun cronJobsResponse(): CronJobsResponse =
        client.get("$baseUrl/mobile/v1/cron/jobs") { authorize() }.body()

    override suspend fun cronJobs(): List<CronJob> = cronJobsResponse().jobs

    override suspend fun cronJob(jobId: String): CronJob =
        client.get("$baseUrl/mobile/v1/cron/jobs/$jobId") { authorize() }.body()

    override suspend fun appendGoal(sessionId: String, request: GoalRequest): GoalResponse =
        client.post("$baseUrl/mobile/v1/sessions/$sessionId/goals") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun startSession(request: GoalRequest): GoalResponse =
        client.post("$baseUrl/mobile/v1/sessions") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun sessionTimeline(sessionId: String): SessionTimeline =
        client.get("$baseUrl/mobile/v1/sessions/$sessionId/timeline") { authorize() }.body()

    private fun HttpRequestBuilder.authorize() {
        deviceToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
    }
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
