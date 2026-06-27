import Foundation

final class MockURLProtocol: URLProtocol {
    static var responses: [String: (Int, String)] = [:]
    static var requests: [URLRequest] = []

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        Self.requests.append(request)
        let path = request.url?.path ?? ""
        let methodPath = "\(request.httpMethod ?? "GET") \(path)"
        let item = Self.responses[methodPath] ?? Self.responses[path] ?? (404, "{}")
        let response = HTTPURLResponse(url: request.url!, statusCode: item.0, httpVersion: nil, headerFields: ["Content-Type": "application/json"])!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(item.1.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

@main
struct MobileGatewayClientTests {
    static func main() async throws {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        let session = URLSession(configuration: config)
        let client = MobileGatewayClient(baseURL: "http://127.0.0.1:8765", session: session)

        MockURLProtocol.responses = [
            "/mobile/v1/status": (200, """
            {"node_id":"node","node_name":"Ray Hermes","status":"online","gateway_ready":true,"hermes_version":"0.x.x","api_version":"1.0","profile":"default","model":{"provider":"openai","model":"gpt"},"features":{"events":true}}
            """),
            "/mobile/v1/pair/start": (200, """
            {"pairing_id":"pair_1","code":"123456","expires_at":"2026-06-26T10:00:00Z","qr_payload":"hermes://pair?code=123456"}
            """),
            "/mobile/v1/pair/complete": (200, """
            {"device_id":"dev_1","device_token":"hmob_token","capabilities":{"sessions":true,"approvals":true}}
            """),
            "GET /mobile/v1/agents": (200, """
            {"agents":[{"id":"agent_vps","name":"VPS Hermes","base_url":"http://127.0.0.1:8765","status":"online","profile":"default","model":"gpt-5.5","created_at":"2026-06-27T10:00:00Z","last_seen_at":"2026-06-27T10:00:01Z"}]}
            """),
            "POST /mobile/v1/agents": (200, """
            {"id":"agent_local","name":"Local Hermes","base_url":"http://127.0.0.1:8766","status":"offline","profile":"default","model":"unknown","created_at":"2026-06-27T10:00:02Z","last_seen_at":null}
            """),
            "/mobile/v1/agents/agent_local": (204, ""),
            "/mobile/v1/sessions": (200, """
            {"session":{"id":"sess_new","title":"Inspect mobile UI","status":"running","created_at":"2026-06-27T10:00:00Z","updated_at":"2026-06-27T10:00:01Z"},"timeline":{"session_id":"sess_new","title":"Inspect mobile UI","items":[]}}
            """),
            "/mobile/v1/sessions/sess_new/timeline": (200, """
            {"session_id":"sess_new","title":"Inspect mobile UI","items":[{"type":"user_goal","id":"msg_1","created_at":"2026-06-27T10:00:00Z","text":"Inspect mobile UI"},{"type":"assistant_result","id":"msg_2","created_at":"2026-06-27T10:00:01Z","markdown":"Done"}]}
            """),
            "/mobile/v1/sessions/sess_new/goals": (200, """
            {"session":{"id":"sess_new","title":"Inspect mobile UI","status":"running","created_at":"2026-06-27T10:00:00Z","updated_at":"2026-06-27T10:02:00Z"},"timeline":{"session_id":"sess_new","title":"Inspect mobile UI","items":[{"type":"user_goal","id":"msg_3","created_at":"2026-06-27T10:02:00Z","text":"Continue with the next task"}]}}
            """)
        ]

        let status = try await client.status()
        if status.nodeName != "Ray Hermes" || !status.gatewayReady {
            throw TestFailure("status response was not decoded")
        }

        let pairing = try await client.startPairing()
        if pairing.code != "123456" {
            throw TestFailure("pairing code was not decoded")
        }

        let completed = try await client.completePairing(code: pairing.code, deviceName: "iPhone", platform: "ios")
        if completed.deviceId != "dev_1" || completed.deviceToken != "hmob_token" {
            throw TestFailure("pairing completion was not decoded")
        }

        let completeRequest = MockURLProtocol.requests.first { $0.url?.path == "/mobile/v1/pair/complete" }
        guard let request = completeRequest, let body = requestBody(request) else {
            throw TestFailure("pair complete request body missing")
        }
        if !body.contains("\"code\":\"123456\"") || !body.contains("\"platform\":\"ios\"") {
            throw TestFailure("pair complete request body invalid: \(body)")
        }

        let agents = try await client.agents(deviceToken: completed.deviceToken)
        if agents.count != 1 || agents[0].name != "VPS Hermes" || agents[0].status != "online" {
            throw TestFailure("agents response was not decoded")
        }

        let agent = try await client.addAgent(name: "Local Hermes", baseURL: "http://127.0.0.1:8766", deviceToken: completed.deviceToken)
        if agent.id != "agent_local" || agent.name != "Local Hermes" {
            throw TestFailure("added agent response was not decoded")
        }
        let addAgentRequest = MockURLProtocol.requests.last { $0.url?.path == "/mobile/v1/agents" && $0.httpMethod == "POST" }
        guard let request = addAgentRequest, let agentBody = requestBody(request) else {
            throw TestFailure("add agent request body missing")
        }
        if request.value(forHTTPHeaderField: "Authorization") != "Bearer hmob_token" {
            throw TestFailure("add agent request missing bearer token")
        }
        if !agentBody.contains("\"base_url\":\"http:\\/\\/127.0.0.1:8766\"") {
            throw TestFailure("add agent request body invalid: \(agentBody)")
        }

        try await client.removeAgent(id: "agent_local", deviceToken: completed.deviceToken)
        let deleteRequest = MockURLProtocol.requests.last { $0.url?.path == "/mobile/v1/agents/agent_local" }
        if deleteRequest?.httpMethod != "DELETE" {
            throw TestFailure("remove agent did not use DELETE")
        }

        let created = try await client.createSession(goal: "Inspect mobile UI", deviceToken: completed.deviceToken)
        if created.session.id != "sess_new" || created.session.title != "Inspect mobile UI" {
            throw TestFailure("created session response was not decoded")
        }

        let createRequest = MockURLProtocol.requests.last { $0.url?.path == "/mobile/v1/sessions" }
        guard let sessionRequest = createRequest, let sessionBody = requestBody(sessionRequest) else {
            throw TestFailure("create session request body missing")
        }
        if sessionRequest.value(forHTTPHeaderField: "Authorization") != "Bearer hmob_token" {
            throw TestFailure("create session request missing bearer token")
        }
        if !sessionBody.contains("\"goal\":\"Inspect mobile UI\"") {
            throw TestFailure("create session request body invalid: \(sessionBody)")
        }

        let timeline = try await client.timeline(sessionId: "sess_new", deviceToken: completed.deviceToken)
        if timeline.items.count != 2 || timeline.items[0].text != "Inspect mobile UI" || timeline.items[1].markdown != "Done" {
            throw TestFailure("session timeline response was not decoded")
        }

        let appended = try await client.appendGoal(sessionId: "sess_new", text: "Continue with the next task", deviceToken: completed.deviceToken)
        if appended.timeline.items.first?.text != "Continue with the next task" {
            throw TestFailure("append goal response was not decoded")
        }
        let appendRequest = MockURLProtocol.requests.last { $0.url?.path == "/mobile/v1/sessions/sess_new/goals" }
        guard let request = appendRequest, let appendBody = requestBody(request) else {
            throw TestFailure("append goal request body missing")
        }
        if request.value(forHTTPHeaderField: "Authorization") != "Bearer hmob_token" {
            throw TestFailure("append goal request missing bearer token")
        }
        if !appendBody.contains("\"goal\":\"Continue with the next task\"") {
            throw TestFailure("append goal request body invalid: \(appendBody)")
        }
    }

    static func requestBody(_ request: URLRequest) -> String? {
        if let data = request.httpBody {
            return String(data: data, encoding: .utf8)
        }
        guard let stream = request.httpBodyStream else {
            return nil
        }
        stream.open()
        defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 1024)
        while stream.hasBytesAvailable {
            let count = stream.read(&buffer, maxLength: buffer.count)
            if count <= 0 { break }
            data.append(buffer, count: count)
        }
        return String(data: data, encoding: .utf8)
    }
}

struct TestFailure: Error, CustomStringConvertible {
    let description: String
    init(_ description: String) { self.description = description }
}
