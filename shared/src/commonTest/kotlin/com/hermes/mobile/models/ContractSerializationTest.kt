package com.hermes.mobile.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class ContractSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesApprovalContract() {
        val approval = json.decodeFromString<Approval>(approvalJson)

        assertEquals("appr_mock_git_push", approval.id)
        assertEquals("sess_mock_contribution", approval.sessionId)
        assertEquals(ApprovalKind.TerminalCommand, approval.kind)
        assertEquals(RiskLevel.High, approval.risk)
        assertEquals(ApprovalStatus.Pending, approval.status)
        assertEquals("rayjun/hermes-agent", approval.details["repo"].toString().trim('"'))
    }

    @Test
    fun decodesTimelineContract() {
        val timeline = json.decodeFromString<SessionTimeline>(timelineJson)

        assertEquals("sess_mock_contribution", timeline.sessionId)
        assertEquals(3, timeline.items.size)
        assertEquals(TimelineItemType.UserGoal, timeline.items[0].type)
        assertEquals(TimelineItemType.ThinkingBlock, timeline.items[1].type)
        assertEquals(ToolStatus.Completed, timeline.items[1].toolCalls?.first()?.status)
    }

    @Test
    fun decodesNodeStatusContract() {
        val status = json.decodeFromString<NodeStatus>(statusJson)

        assertEquals("node_mock_vps", status.nodeId)
        assertEquals(true, status.gatewayReady)
        assertEquals("openai-codex", status.model.provider)
        assertEquals(true, status.features["approvals"])
    }
}

private const val approvalJson = """
{
  "id": "appr_mock_git_push",
  "session_id": "sess_mock_contribution",
  "kind": "terminal_command",
  "risk": "high",
  "status": "pending",
  "title": "Run git push",
  "summary": "Hermes wants to push branch fix/mobile-api.",
  "reason": "The requested change passed tests and needs to be pushed for review.",
  "details": {
    "command": "git push origin fix/mobile-api",
    "cwd": "/home/ubuntu/projects/hermes-agent",
    "repo": "rayjun/hermes-agent",
    "branch": "fix/mobile-api",
    "files_changed": 3,
    "tests": "128 passed"
  },
  "actions": ["approve", "deny", "ask"],
  "created_at": "2026-06-24T10:00:00Z",
  "expires_at": "2026-06-24T10:15:00Z"
}
"""

private const val timelineJson = """
{
  "session_id": "sess_mock_contribution",
  "title": "Hermes-Agent contribution #2",
  "items": [
    {
      "type": "user_goal",
      "id": "msg_1",
      "created_at": "2026-06-24T09:00:00Z",
      "text": "继续找 pr"
    },
    {
      "type": "thinking_block",
      "id": "think_1",
      "created_at": "2026-06-24T09:00:05Z",
      "title": "Thinking",
      "tool_calls": [
        {
          "id": "tool_1",
          "name": "search_files",
          "summary": "Searched files",
          "status": "completed",
          "duration_ms": 458
        }
      ]
    },
    {
      "type": "assistant_result",
      "id": "msg_2",
      "created_at": "2026-06-24T09:01:00Z",
      "markdown": "找到一个适合的小问题..."
    }
  ]
}
"""

private const val statusJson = """
{
  "node_id": "node_mock_vps",
  "node_name": "VPS Hermes",
  "status": "online",
  "gateway_ready": true,
  "hermes_version": "0.x.x",
  "api_version": "1.0",
  "profile": "default",
  "model": {"provider": "openai-codex", "model": "gpt-5.5"},
  "features": {
    "events": true,
    "approvals": true,
    "session_timeline": true
  }
}
"""
