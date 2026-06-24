package com.hermes.mobile.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NodeStatus(
    @SerialName("node_id") val nodeId: String,
    @SerialName("node_name") val nodeName: String,
    val status: String,
    @SerialName("gateway_ready") val gatewayReady: Boolean,
    @SerialName("hermes_version") val hermesVersion: String,
    @SerialName("api_version") val apiVersion: String,
    val profile: String,
    val model: ModelInfo,
    val features: Map<String, Boolean>,
)

@Serializable
data class ModelInfo(
    val provider: String,
    val model: String,
)
