package com.hermes.mobile.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Artifact(
    val id: String,
    @SerialName("session_id") val sessionId: String? = null,
    val kind: ArtifactKind,
    val title: String,
    val summary: String,
    @SerialName("mime_type") val mimeType: String? = null,
    val uri: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
enum class ArtifactKind {
    @SerialName("file") File,
    @SerialName("image") Image,
    @SerialName("log") Log,
    @SerialName("link") Link,
    @SerialName("data") Data,
}

@Serializable
data class ArtifactsResponse(
    val artifacts: List<Artifact>,
)
