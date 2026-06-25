package com.hermes.mobile.ui

import com.hermes.mobile.models.Artifact
import com.hermes.mobile.models.ArtifactKind
import kotlinx.datetime.Instant

interface ArtifactsGateway {
    suspend fun artifacts(): List<Artifact>
}

sealed interface ArtifactsLoadState {
    data object Loading : ArtifactsLoadState

    data class Ready(
        val artifacts: List<Artifact>,
        val notice: String? = null,
        val usingFallback: Boolean = false,
    ) : ArtifactsLoadState
}

class ArtifactsLoader(
    private val gateway: ArtifactsGateway,
) {
    suspend fun load(): ArtifactsLoadState.Ready = try {
        ArtifactsLoadState.Ready(artifacts = gateway.artifacts())
    } catch (_: Throwable) {
        ArtifactsLoadState.Ready(
            artifacts = listOf(fallbackArtifact()),
            notice = "Gateway unavailable · showing sample artifacts",
            usingFallback = true,
        )
    }
}

private fun fallbackArtifact(): Artifact = Artifact(
    id = "art_sample",
    sessionId = "sess_sample",
    kind = ArtifactKind.File,
    title = "Sample artifact",
    summary = "Generated output from a Hermes session.",
    mimeType = "text/plain",
    uri = "file:///sample-artifact.txt",
    sizeBytes = 512,
    metadata = mapOf("source" to "fallback"),
    createdAt = Instant.parse("2026-06-24T12:00:00Z"),
)
