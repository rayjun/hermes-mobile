package com.hermes.mobile.models

import kotlinx.serialization.Serializable

@Serializable
data class GoalRequest(
    val goal: String,
)

@Serializable
data class GoalResponse(
    val session: SessionSummary,
    val timeline: SessionTimeline,
)
